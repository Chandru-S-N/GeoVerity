package org.geoverity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoverity.crypto.CanonicalMetadataSerializer;
import org.geoverity.crypto.EcdsaSigner;
import org.geoverity.dto.CaptureResponse;
import org.geoverity.dto.OfflineSyncRequest;
import org.geoverity.entity.ApiClient;
import org.geoverity.entity.VerificationRecord;
import org.geoverity.repository.VerificationRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineSyncService {

    private final VerificationRecordRepository verificationRecordRepository;
    private final CanonicalMetadataSerializer metadataSerializer;
    private final EcdsaSigner ecdsaSigner;
    private final AuditService auditService;

    @Value("${geoverity.security.offline-max-time-deviation-ms:120000}")
    private long maxDeviationMs; // 120,000 ms (2 minutes)

    @Transactional
    public CaptureResponse processOfflineSync(OfflineSyncRequest request, ApiClient apiClient, String clientIp) {
        String verificationId = request.getVerificationId().trim();

        // 1. Replay / Duplication Check
        Optional<VerificationRecord> existing = verificationRecordRepository.findByVerificationId(verificationId);
        if (existing.isPresent()) {
            auditService.logEvent("OFFLINE_SYNC_DUPLICATE", clientIp, apiClient.getId(), verificationId, "FAILURE",
                    "Verification ID already registered during offline sync: " + verificationId);
            throw new IllegalArgumentException("Verification ID already registered: " + verificationId);
        }

        // 2. Monotonic Elapsed Time Reconciliation
        long lastTrustedServerTime = request.getLastTrustedServerTimestamp();
        long lastTrustedElapsed = request.getLastTrustedElapsedRealtime();
        long captureElapsed = request.getCaptureElapsedRealtime();
        long deviceCaptureTime = request.getDeviceCaptureTime();

        // Monotonic sanity check: elapsed time must move forward
        if (captureElapsed < lastTrustedElapsed) {
            auditService.logEvent("TIME_ANOMALY", clientIp, apiClient.getId(), verificationId, "ANOMALY",
                    "Negative elapsed monotonic interval detected. Device may have rebooted or manipulated monotonic timer.");
            throw new SecurityException("TIME_ANOMALY: Monotonic elapsed time regression detected. Offline capture rejected.");
        }

        long monotonicElapsedDelta = captureElapsed - lastTrustedElapsed;
        long expectedDeviceTime = lastTrustedServerTime + monotonicElapsedDelta;
        long deviation = Math.abs(deviceCaptureTime - expectedDeviceTime);

        log.info("Offline Time Reconciliation for {}: expected={}, reported={}, deviation={}ms, maxAllowed={}ms",
                verificationId, expectedDeviceTime, deviceCaptureTime, deviation, maxDeviationMs);

        if (deviation > maxDeviationMs) {
            auditService.logEvent("TIME_ANOMALY", clientIp, apiClient.getId(), verificationId, "ANOMALY",
                    String.format("Time anomaly detected! Deviation %d ms exceeded threshold of %d ms. Clock manipulation suspected.",
                            deviation, maxDeviationMs));
            throw new SecurityException(String.format("TIME_ANOMALY: Time deviation (%d ms) exceeds allowed tolerance (%d ms). Authentication rejected.",
                    deviation, maxDeviationMs));
        }

        // 3. Reconciled Trusted Capture Timestamp
        // Use the mathematically reconciled expectedDeviceTime as the authoritative timestamp
        long authoritativeTimestamp = expectedDeviceTime;
        request.getCanonicalMetadata().setTrustedTimestamp(authoritativeTimestamp);
        request.getCanonicalMetadata().setVerificationId(verificationId);

        // 4. Deterministic Canonical Metadata Serialization
        String canonicalJson = metadataSerializer.serializeToCanonicalJson(request.getCanonicalMetadata());

        // 5. Server ECDSA Signature Generation
        byte[] payloadToSign = (request.getSha256Hash() + ":" + canonicalJson).getBytes(StandardCharsets.UTF_8);
        String ecdsaSignature = ecdsaSigner.sign(payloadToSign);

        // 6. Save Verification Record
        VerificationRecord record = VerificationRecord.builder()
                .verificationId(verificationId)
                .sha256Hash(request.getSha256Hash().toLowerCase().trim())
                .canonicalMetadata(canonicalJson)
                .trustedServerTimestamp(Instant.ofEpochMilli(authoritativeTimestamp))
                .ecdsaSignature(ecdsaSignature)
                .deviceId(request.getCanonicalMetadata().getDeviceId())
                .apiClient(apiClient)
                .status("AUTHENTICATED")
                .build();

        verificationRecordRepository.save(record);

        // 7. Audit Log
        auditService.logEvent("OFFLINE_SYNC_SUCCESS", clientIp, apiClient.getId(), verificationId, "SUCCESS",
                String.format("Offline capture reconciled & authenticated with deviation %d ms", deviation));

        return CaptureResponse.builder()
                .verificationId(verificationId)
                .status("AUTHENTICATED")
                .trustedTimestamp(record.getTrustedServerTimestamp().toString())
                .sha256(record.getSha256Hash())
                .signatureStatus("VALID")
                .ecdsaSignature(ecdsaSignature)
                .build();
    }
}
