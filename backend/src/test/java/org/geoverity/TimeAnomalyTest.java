package org.geoverity;

import org.geoverity.crypto.CanonicalMetadataSerializer;
import org.geoverity.crypto.EcdsaSigner;
import org.geoverity.dto.CanonicalMetadataDto;
import org.geoverity.dto.OfflineSyncRequest;
import org.geoverity.entity.ApiClient;
import org.geoverity.repository.VerificationRecordRepository;
import org.geoverity.service.AuditService;
import org.geoverity.service.OfflineSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeAnomalyTest {

    @Mock
    private VerificationRecordRepository verificationRecordRepository;

    @Mock
    private EcdsaSigner ecdsaSigner;

    @Mock
    private AuditService auditService;

    private OfflineSyncService offlineSyncService;
    private final CanonicalMetadataSerializer metadataSerializer = new CanonicalMetadataSerializer();

    @BeforeEach
    void setUp() {
        offlineSyncService = new OfflineSyncService(
                verificationRecordRepository,
                metadataSerializer,
                ecdsaSigner,
                auditService
        );
        ReflectionTestUtils.setField(offlineSyncService, "maxDeviationMs", 120000L); // 2 minutes
    }

    @Test
    @DisplayName("Should detect and REJECT Clock Rewind Tampering Attack (TIME_ANOMALY)")
    void testClockRewindAttackRejection() {
        // Scenario:
        // 1. Online at 2:00 PM (1788444000000 ms), elapsedRealtime = 10,000,000 ms
        // 2. Network disconnected
        // 3. Attacker rolls back device wall clock to 12:00 PM (1788436800000 ms)
        // 4. Captures at 12:05 PM (1788437100000 ms), elapsedRealtime = 10,300,000 ms (5 minutes elapsed)
        // 5. Attacker rolls clock back forward to 2:05 PM and reconnects

        long lastTrustedServerTime = 1788444000000L; // 2:00 PM
        long lastTrustedElapsed = 10_000_000L;
        long captureElapsed = 10_300_000L; // 300,000 ms elapsed = 5 mins later
        long reportedDeviceCaptureTime = 1788437100000L; // 12:05 PM (Forged 2 hours earlier!)

        // Expected legitimate time should be: 2:00 PM + 5 mins = 2:05 PM (1788444300000 ms)
        // Deviation = | 1788437100000 - 1788444300000 | = 7,200,000 ms (2 hours) >> 120,000 ms threshold

        OfflineSyncRequest request = OfflineSyncRequest.builder()
                .verificationId("SGA-TAMPERED-001")
                .canonicalMetadata(CanonicalMetadataDto.builder()
                        .deviceId("dev_attacker_phone")
                        .appVersion("1.0.0")
                        .latitude(10.785234)
                        .longitude(78.125432)
                        .locationName("Karur, Tamil Nadu")
                        .build())
                .sha256Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .lastTrustedServerTimestamp(lastTrustedServerTime)
                .lastTrustedElapsedRealtime(lastTrustedElapsed)
                .captureElapsedRealtime(captureElapsed)
                .deviceCaptureTime(reportedDeviceCaptureTime)
                .build();

        ApiClient apiClient = ApiClient.builder().clientName("Test Client").status("ACTIVE").build();

        when(verificationRecordRepository.findByVerificationId("SGA-TAMPERED-001")).thenReturn(Optional.empty());

        SecurityException ex = assertThrows(SecurityException.class, () ->
                offlineSyncService.processOfflineSync(request, apiClient, "192.168.1.100"));

        assertTrue(ex.getMessage().contains("TIME_ANOMALY"), "Exception must explicitly flag TIME_ANOMALY");

        // Verify that NO ECDSA signature was generated and NO record was saved
        verify(ecdsaSigner, never()).sign(any());
        verify(verificationRecordRepository, never()).save(any());
        verify(auditService, times(1)).logEvent(eq("TIME_ANOMALY"), any(), any(), eq("SGA-TAMPERED-001"), eq("ANOMALY"), any());
    }

    @Test
    @DisplayName("Should accept valid offline capture when monotonic elapsed time is within tolerance")
    void testValidOfflineCaptureReconciliation() {
        long lastTrustedServerTime = 1788444000000L;
        long lastTrustedElapsed = 10_000_000L;
        long captureElapsed = 10_060_000L; // 60 seconds later
        long deviceCaptureTime = 1788444059500L; // reported 59.5 seconds later (500ms jitter)

        OfflineSyncRequest request = OfflineSyncRequest.builder()
                .verificationId("SGA-LEGIT-002")
                .canonicalMetadata(CanonicalMetadataDto.builder()
                        .deviceId("dev_legit_phone")
                        .appVersion("1.0.0")
                        .latitude(10.785234)
                        .longitude(78.125432)
                        .locationName("Karur, Tamil Nadu")
                        .build())
                .sha256Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .lastTrustedServerTimestamp(lastTrustedServerTime)
                .lastTrustedElapsedRealtime(lastTrustedElapsed)
                .captureElapsedRealtime(captureElapsed)
                .deviceCaptureTime(deviceCaptureTime)
                .build();

        ApiClient apiClient = ApiClient.builder().clientName("Test Client").status("ACTIVE").build();

        when(verificationRecordRepository.findByVerificationId("SGA-LEGIT-002")).thenReturn(Optional.empty());
        when(ecdsaSigner.sign(any())).thenReturn("MEYCIQDemoxxxValidEcdsaSig...");

        var response = offlineSyncService.processOfflineSync(request, apiClient, "192.168.1.100");

        assertNotNull(response);
        assertEquals("AUTHENTICATED", response.getStatus());
        assertEquals("VALID", response.getSignatureStatus());
        verify(ecdsaSigner, times(1)).sign(any());
        verify(verificationRecordRepository, times(1)).save(any());
    }
}
