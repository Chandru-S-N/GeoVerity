package org.geoverity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoverity.crypto.CanonicalMetadataSerializer;
import org.geoverity.crypto.EcdsaSigner;
import org.geoverity.dto.CaptureRequest;
import org.geoverity.dto.CaptureResponse;
import org.geoverity.entity.ApiClient;
import org.geoverity.entity.Device;
import org.geoverity.entity.VerificationRecord;
import org.geoverity.repository.DeviceRepository;
import org.geoverity.repository.VerificationRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptureService {

    private final VerificationRecordRepository verificationRecordRepository;
    private final DeviceRepository deviceRepository;
    private final TimeService timeService;
    private final CanonicalMetadataSerializer metadataSerializer;
    private final EcdsaSigner ecdsaSigner;
    private final AuditService auditService;

    @Transactional
    public CaptureResponse processCapture(CaptureRequest request, ApiClient apiClient, String clientIp) {
        String verificationId = request.getVerificationId().trim();

        // 1. Replay / Duplication Check
        Optional<VerificationRecord> existing = verificationRecordRepository.findByVerificationId(verificationId);
        if (existing.isPresent()) {
            auditService.logEvent("CAPTURE_DUPLICATE_ATTEMPT", clientIp, apiClient.getId(), verificationId, "FAILURE",
                    "Verification ID already registered: " + verificationId);
            throw new IllegalArgumentException("Verification ID already registered: " + verificationId);
        }

        // 2. Validate Trusted Server Time Token
        String deviceId = request.getCanonicalMetadata().getDeviceId();
        long trustedServerTime = timeService.validateAndExtractServerTime(request.getTimeToken(), deviceId);

        // Ensure canonical metadata has accurate server timestamp
        request.getCanonicalMetadata().setTrustedTimestamp(trustedServerTime);
        request.getCanonicalMetadata().setVerificationId(verificationId);

        // 3. Register or Update Device
        registerOrUpdateDevice(deviceId, request.getCanonicalMetadata().getAppVersion());

        // 4. Serialize Canonical Metadata Deterministically
        String canonicalJson = metadataSerializer.serializeToCanonicalJson(request.getCanonicalMetadata());

        // 5. Generate Server-Side ECDSA P-256 Signature over the SHA-256 hash + canonical metadata
        byte[] payloadToSign = (request.getSha256Hash() + ":" + canonicalJson).getBytes(StandardCharsets.UTF_8);
        String ecdsaSignature = ecdsaSigner.sign(payloadToSign);

        // 6. Persist Verification Record (NO images stored!)
        VerificationRecord record = VerificationRecord.builder()
                .verificationId(verificationId)
                .sha256Hash(request.getSha256Hash().toLowerCase().trim())
                .canonicalMetadata(canonicalJson)
                .trustedServerTimestamp(Instant.ofEpochMilli(trustedServerTime))
                .ecdsaSignature(ecdsaSignature)
                .deviceId(deviceId)
                .apiClient(apiClient)
                .status("AUTHENTICATED")
                .build();

        verificationRecordRepository.save(record);

        // 7. Audit Log
        auditService.logEvent("AUTHENTICATION_SUCCESS", clientIp, apiClient.getId(), verificationId, "SUCCESS",
                "Authenticated record for device: " + deviceId);

        return CaptureResponse.builder()
                .verificationId(verificationId)
                .status("AUTHENTICATED")
                .trustedTimestamp(record.getTrustedServerTimestamp().toString())
                .sha256(record.getSha256Hash())
                .signatureStatus("VALID")
                .ecdsaSignature(ecdsaSignature)
                .build();
    }

    private void registerOrUpdateDevice(String deviceId, String appVersion) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        Optional<Device> devOpt = deviceRepository.findByDeviceId(deviceId);
        if (devOpt.isPresent()) {
            Device dev = devOpt.get();
            dev.setLastSeenAt(Instant.now());
            if (appVersion != null) {
                dev.setAppVersion(appVersion);
            }
            deviceRepository.save(dev);
        } else {
            Device newDev = Device.builder()
                    .deviceId(deviceId)
                    .appVersion(appVersion != null ? appVersion : "1.0.0")
                    .status("ACTIVE")
                    .build();
            deviceRepository.save(newDev);
        }
    }
}
