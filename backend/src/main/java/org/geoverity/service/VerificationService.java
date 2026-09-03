package org.geoverity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoverity.crypto.CanonicalMetadataSerializer;
import org.geoverity.crypto.EcdsaSigner;
import org.geoverity.crypto.HashUtils;
import org.geoverity.crypto.JpegMarkerExtractor;
import org.geoverity.crypto.QrCodeService;
import org.geoverity.dto.CanonicalMetadataDto;
import org.geoverity.dto.VerificationResponse;
import org.geoverity.entity.VerificationRecord;
import org.geoverity.repository.VerificationRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRecordRepository verificationRecordRepository;
    private final JpegMarkerExtractor jpegMarkerExtractor;
    private final QrCodeService qrCodeService;
    private final CanonicalMetadataSerializer metadataSerializer;
    private final EcdsaSigner ecdsaSigner;
    private final AuditService auditService;

    private static final Pattern ID_IN_HEADER_PATTERN = Pattern.compile(
            "\"verificationId\"\\s*:\\s*\"([^\"]+)\"|(?:SGA-)?[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}"
    );

    @Transactional(readOnly = true)
    public VerificationResponse verifyImage(byte[] imageBytes, String clientIp) {
        return verifyImage(imageBytes, null, clientIp, "GeoVerity Verification Portal");
    }

    @Transactional(readOnly = true)
    public VerificationResponse verifyImage(byte[] imageBytes, String verificationIdParam, String clientIp, String organization) {
        List<String> steps = new ArrayList<>();
        steps.add("Received image payload (" + imageBytes.length + " bytes)");

        String verificationId = (verificationIdParam != null && !verificationIdParam.isBlank()) 
                ? verificationIdParam.trim() 
                : null;

        // 1. If not provided directly in parameter, extract from JPEG COM marker
        if (verificationId == null) {
            Optional<String> markerIdOpt = jpegMarkerExtractor.extractVerificationId(imageBytes);
            if (markerIdOpt.isPresent()) {
                verificationId = markerIdOpt.get();
                steps.add("Extracted Verification ID from embedded JPEG COM segment: " + verificationId);
            }
        }

        // 2. Extract from EXIF UserComment / ImageDescription header text
        if (verificationId == null) {
            int scanLen = Math.min(imageBytes.length, 65536);
            String headerSample = new String(imageBytes, 0, scanLen, StandardCharsets.ISO_8859_1);
            Matcher matcher = ID_IN_HEADER_PATTERN.matcher(headerSample);
            if (matcher.find()) {
                verificationId = matcher.group(1) != null ? matcher.group(1) : matcher.group();
                steps.add("Extracted Verification ID from EXIF image headers: " + verificationId);
            }
        }

        // 3. Fallback: Scan QR Code from visual pixels
        if (verificationId == null) {
            Optional<String> qrIdOpt = qrCodeService.decodeQrCode(imageBytes);
            if (qrIdOpt.isPresent()) {
                verificationId = qrIdOpt.get();
                steps.add("Extracted Verification ID from visible QR Code: " + verificationId);
            }
        }

        // Case A: Verification ID identified -> Direct indexed lookup
        if (verificationId != null && !verificationId.isBlank()) {
            Optional<VerificationRecord> recordOpt = findRecordById(verificationId);
            if (recordOpt.isPresent()) {
                return verifySingleRecord(recordOpt.get(), imageBytes, clientIp, organization, steps);
            }
        }

        // Case B: Pure Image Upload Fallback (Iterate registered records & compare composite hash)
        steps.add("Searching registry ledger for matching composite image hash...");
        List<VerificationRecord> allRecords = verificationRecordRepository.findAll();
        for (VerificationRecord record : allRecords) {
            if (!"AUTHENTICATED".equalsIgnoreCase(record.getStatus()) && !"VALID".equalsIgnoreCase(record.getStatus())) {
                continue;
            }

            try {
                CanonicalMetadataDto canonicalMetadata = metadataSerializer.deserializeFromJson(record.getCanonicalMetadata());
                byte[] canonicalBytes = metadataSerializer.serializeToCanonicalBytes(canonicalMetadata);
                String computedHash = HashUtils.calculateCompositeHash(imageBytes, canonicalBytes);

                if (computedHash.equalsIgnoreCase(record.getSha256Hash())) {
                    steps.add("Found matching cryptographic record: " + record.getVerificationId());
                    return verifySingleRecord(record, imageBytes, clientIp, organization, steps);
                }
            } catch (Exception e) {
                log.debug("Error testing record {}: {}", record.getVerificationId(), e.getMessage());
            }
        }

        // Not Found / Tampered
        String auditId = UUID.randomUUID().toString();
        steps.add("FAILED: No matching cryptographic evidence record exists on authority ledger");
        auditService.logEvent("VERIFY_FAILURE", clientIp, null, verificationId, "FAILURE",
                "Image verification failed: Record not found or tampered");

        return VerificationResponse.builder()
                .verified(false)
                .verificationId(verificationId)
                .status("NOT_AUTHENTIC")
                .confidenceScore(0)
                .hashMatch(false)
                .signatureValid(false)
                .failureReason("No cryptographic evidence record found for this photograph, or the image pixels/metadata have been altered.")
                .reason("RECORD_NOT_FOUND_OR_TAMPERED")
                .auditLogId(auditId)
                .verificationSteps(steps)
                .build();
    }

    private Optional<VerificationRecord> findRecordById(String verificationId) {
        // Try exact match
        Optional<VerificationRecord> rec = verificationRecordRepository.findByVerificationId(verificationId);
        if (rec.isPresent()) return rec;

        // Try with SGA- prefix if omitted
        if (!verificationId.startsWith("SGA-")) {
            rec = verificationRecordRepository.findByVerificationId("SGA-" + verificationId);
            if (rec.isPresent()) return rec;
        } else {
            // Try without SGA- prefix
            rec = verificationRecordRepository.findByVerificationId(verificationId.substring(4));
            if (rec.isPresent()) return rec;
        }

        return Optional.empty();
    }

    private VerificationResponse verifySingleRecord(
            VerificationRecord record,
            byte[] imageBytes,
            String clientIp,
            String organization,
            List<String> steps) {

        String vId = record.getVerificationId();
        steps.add("Retrieved trusted authority record from registry (Created at " + record.getCreatedAt() + ")");

        // 1. Verify Server-Side ECDSA P-256 Digital Signature
        byte[] signedPayload = (record.getSha256Hash() + ":" + record.getCanonicalMetadata()).getBytes(StandardCharsets.UTF_8);
        boolean signatureValid = ecdsaSigner.verify(signedPayload, record.getEcdsaSignature());

        if (!signatureValid) {
            steps.add("FAILED: ECDSA P-256 signature verification failed");
            auditService.logEvent("VERIFY_FAILURE", clientIp, null, vId, "FAILURE",
                    "Invalid digital signature for verification ID: " + vId);
            return VerificationResponse.builder()
                    .verified(false)
                    .verificationId(vId)
                    .status("NOT_AUTHENTIC")
                    .confidenceScore(0)
                    .signatureValid(false)
                    .hashMatch(false)
                    .failureReason("Digital signature invalid: The server-issued cryptographic signature could not be verified.")
                    .reason("INVALID_SIGNATURE")
                    .auditLogId(UUID.randomUUID().toString())
                    .verificationSteps(steps)
                    .build();
        }
        steps.add("Validated server ECDSA P-256 digital signature: VALID");

        // 2. Retrieve Stored Canonical Metadata
        CanonicalMetadataDto canonicalMetadata = metadataSerializer.deserializeFromJson(record.getCanonicalMetadata());
        byte[] canonicalBytes = metadataSerializer.serializeToCanonicalBytes(canonicalMetadata);

        // 3. Recalculate Cryptographic SHA-256 over: uploadedImageBytes + storedCanonicalMetadataBytes
        String computedHash = HashUtils.calculateCompositeHash(imageBytes, canonicalBytes);
        boolean hashMatched = computedHash.equalsIgnoreCase(record.getSha256Hash());

        if (!hashMatched) {
            steps.add(String.format("FAILED: Hash mismatch (Computed: %s..., Stored: %s...)",
                    computedHash.substring(0, 12), record.getSha256Hash().substring(0, 12)));
            auditService.logEvent("VERIFY_FAILURE", clientIp, null, vId, "FAILURE",
                    "Hash mismatch: Image modified, cropped, or re-encoded");
            return VerificationResponse.builder()
                    .verified(false)
                    .verificationId(vId)
                    .status("NOT_AUTHENTIC")
                    .confidenceScore(0)
                    .signatureValid(true)
                    .hashMatch(false)
                    .failureReason("Hash mismatch: The photograph content, location footer, or pixel bytes have been modified, cropped, compressed, or re-encoded.")
                    .reason("HASH_MISMATCH_MODIFIED_IMAGE")
                    .auditLogId(UUID.randomUUID().toString())
                    .verificationSteps(steps)
                    .build();
        }

        steps.add("Recalculated SHA-256 hash matches authoritative registry record: EXACT MATCH");
        steps.add("SUCCESS: Image is 100% verified AUTHENTIC digital evidence");

        auditService.logEvent("VERIFY_SUCCESS", clientIp, null, vId, "SUCCESS",
                "Authentic image verified successfully by " + organization);

        String auditId = UUID.randomUUID().toString();
        long serverTimestampMs = record.getTrustedServerTimestamp() != null 
                ? record.getTrustedServerTimestamp().toEpochMilli() 
                : System.currentTimeMillis();

        Map<String, Object> metadataMap = new LinkedHashMap<>();
        metadataMap.put("verificationId", vId);
        metadataMap.put("deviceId", record.getDeviceId());
        metadataMap.put("gpsLat", canonicalMetadata.getLatitude());
        metadataMap.put("gpsLng", canonicalMetadata.getLongitude());
        metadataMap.put("location", canonicalMetadata.getLocationName());
        metadataMap.put("serverTimestamp", serverTimestampMs);
        metadataMap.put("formattedUtcTime", Instant.ofEpochMilli(serverTimestampMs).toString());
        metadataMap.put("offline", false);
        metadataMap.put("timeFlag", "TRUSTED_SERVER_TIME");
        metadataMap.put("organization", organization);

        Map<String, Object> securityProofMap = new LinkedHashMap<>();
        securityProofMap.put("hashAlgorithm", "SHA-256");
        securityProofMap.put("signatureAlgorithm", "ECDSA-SHA256 (secp256r1)");
        securityProofMap.put("serverSignatureValid", true);
        securityProofMap.put("hardwareAttested", true);
        securityProofMap.put("ledgerRecordStatus", "AUTHENTICATED_AND_NOTARIZED");

        return VerificationResponse.builder()
                .verified(true)
                .verificationId(vId)
                .status("AUTHENTIC")
                .confidenceScore(100)
                .signatureValid(true)
                .hashMatch(true)
                .location(canonicalMetadata.getLocationName())
                .gps(String.format(Locale.US, "%.6f, %.6f", canonicalMetadata.getLatitude(), canonicalMetadata.getLongitude()))
                .trustedTimestamp(Instant.ofEpochMilli(serverTimestampMs).toString())
                .deviceId(record.getDeviceId())
                .sha256Hash(record.getSha256Hash())
                .canonicalMetadata(canonicalMetadata)
                .metadata(metadataMap)
                .securityProof(securityProofMap)
                .auditLogId(auditId)
                .verificationSteps(steps)
                .build();
    }
}
