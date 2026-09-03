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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Transactional(readOnly = true)
    public VerificationResponse verifyImage(byte[] imageBytes, String clientIp) {
        List<String> steps = new ArrayList<>();
        steps.add("Received original image payload (" + imageBytes.length + " bytes)");

        // 1. Extract Verification ID from JPEG COM Marker or QR Code
        Optional<String> markerIdOpt = jpegMarkerExtractor.extractVerificationId(imageBytes);
        String verificationId = null;

        if (markerIdOpt.isPresent()) {
            verificationId = markerIdOpt.get();
            steps.add("Extracted Verification ID from embedded JPEG COM segment: " + verificationId);
        } else {
            // Fallback: Scan QR Code
            Optional<String> qrIdOpt = qrCodeService.decodeQrCode(imageBytes);
            if (qrIdOpt.isPresent()) {
                verificationId = qrIdOpt.get();
                steps.add("Extracted Verification ID from visible QR Code: " + verificationId);
            }
        }

        if (verificationId == null || verificationId.isBlank()) {
            steps.add("FAILED: No Verification ID embedded in image markers or decodable QR code");
            auditService.logEvent("VERIFY_FAILURE", clientIp, null, null, "FAILURE",
                    "Image verification failed: Missing Verification ID");
            return VerificationResponse.builder()
                    .status("NOT_AUTHENTIC")
                    .signatureValid(false)
                    .hashMatched(false)
                    .failureReason("Verification ID missing: The submitted image does not contain a valid GeoVerity embedded marker or QR identifier.")
                    .verificationSteps(steps)
                    .build();
        }

        // 2. Direct Indexed Database Lookup
        Optional<VerificationRecord> recordOpt = verificationRecordRepository.findByVerificationId(verificationId);
        if (recordOpt.isEmpty()) {
            steps.add("FAILED: Verification ID " + verificationId + " not found in database registry");
            auditService.logEvent("VERIFY_FAILURE", clientIp, null, verificationId, "FAILURE",
                    "Verification ID not found in registry");
            return VerificationResponse.builder()
                    .verificationId(verificationId)
                    .status("NOT_AUTHENTIC")
                    .signatureValid(false)
                    .hashMatched(false)
                    .failureReason("Unknown Verification ID: No cryptographic evidence record exists for " + verificationId)
                    .verificationSteps(steps)
                    .build();
        }

        VerificationRecord record = recordOpt.get();
        steps.add("Retrieved trusted authority record from PostgreSQL (Created at " + record.getCreatedAt() + ")");

        // 3. Verify Server-Side ECDSA P-256 Digital Signature
        byte[] signedPayload = (record.getSha256Hash() + ":" + record.getCanonicalMetadata()).getBytes(StandardCharsets.UTF_8);
        boolean signatureValid = ecdsaSigner.verify(signedPayload, record.getEcdsaSignature());

        if (!signatureValid) {
            steps.add("FAILED: ECDSA P-256 signature verification failed");
            auditService.logEvent("VERIFY_FAILURE", clientIp, null, verificationId, "FAILURE",
                    "Invalid digital signature for verification ID: " + verificationId);
            return VerificationResponse.builder()
                    .verificationId(verificationId)
                    .status("NOT_AUTHENTIC")
                    .signatureValid(false)
                    .hashMatched(false)
                    .failureReason("Digital signature invalid: The server-issued cryptographic signature could not be verified.")
                    .verificationSteps(steps)
                    .build();
        }
        steps.add("Validated server ECDSA P-256 digital signature: VALID");

        // 4. Retrieve Trusted Stored Canonical Metadata
        CanonicalMetadataDto canonicalMetadata = metadataSerializer.deserializeFromJson(record.getCanonicalMetadata());
        byte[] canonicalBytes = metadataSerializer.serializeToCanonicalBytes(canonicalMetadata);

        // 5. Recalculate Cryptographic SHA-256 over: uploadedImageBytes + storedCanonicalMetadataBytes
        String computedHash = HashUtils.calculateCompositeHash(imageBytes, canonicalBytes);
        boolean hashMatched = computedHash.equalsIgnoreCase(record.getSha256Hash());

        if (!hashMatched) {
            steps.add(String.format("FAILED: Hash mismatch (Computed: %s..., Stored: %s...)",
                    computedHash.substring(0, 12), record.getSha256Hash().substring(0, 12)));
            auditService.logEvent("VERIFY_FAILURE", clientIp, null, verificationId, "FAILURE",
                    "Hash mismatch: Image modified, cropped, or re-encoded");
            return VerificationResponse.builder()
                    .verificationId(verificationId)
                    .status("NOT_AUTHENTIC")
                    .signatureValid(true)
                    .hashMatched(false)
                    .failureReason("Hash mismatch: The photograph content, metadata footer, or pixel bytes have been modified, cropped, compressed, or re-encoded.")
                    .verificationSteps(steps)
                    .build();
        }

        steps.add("Recalculated SHA-256 hash matches authoritative database record: EXACT MATCH");
        steps.add("SUCCESS: Image is verified AUTHENTIC digital evidence");

        auditService.logEvent("VERIFY_SUCCESS", clientIp, null, verificationId, "SUCCESS",
                "Authentic image verified successfully");

        return VerificationResponse.builder()
                .verificationId(verificationId)
                .status("AUTHENTIC")
                .signatureValid(true)
                .hashMatched(true)
                .location(canonicalMetadata.getLocationName())
                .gps(String.format("%.6f, %.6f", canonicalMetadata.getLatitude(), canonicalMetadata.getLongitude()))
                .trustedTimestamp(record.getTrustedServerTimestamp().toString())
                .deviceId(record.getDeviceId())
                .sha256Hash(record.getSha256Hash())
                .canonicalMetadata(canonicalMetadata)
                .verificationSteps(steps)
                .build();
    }
}
