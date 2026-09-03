package org.geoverity;

import org.geoverity.crypto.CanonicalMetadataSerializer;
import org.geoverity.crypto.EcdsaSigner;
import org.geoverity.crypto.HashUtils;
import org.geoverity.crypto.JpegMarkerExtractor;
import org.geoverity.dto.CanonicalMetadataDto;
import org.geoverity.dto.VerificationResponse;
import org.geoverity.entity.ApiClient;
import org.geoverity.entity.VerificationRecord;
import org.geoverity.repository.ApiClientRepository;
import org.geoverity.repository.VerificationRecordRepository;
import org.geoverity.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class VerificationIntegrationTest {

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private VerificationRecordRepository verificationRecordRepository;

    @Autowired
    private ApiClientRepository apiClientRepository;

    @Autowired
    private CanonicalMetadataSerializer metadataSerializer;

    @Autowired
    private JpegMarkerExtractor jpegMarkerExtractor;

    @Autowired
    private EcdsaSigner ecdsaSigner;

    private ApiClient testClient;

    @BeforeEach
    void setup() {
        verificationRecordRepository.deleteAll();
        apiClientRepository.deleteAll();

        String rawKey = "gv_live_test_integration_key_" + UUID.randomUUID();
        testClient = apiClientRepository.save(ApiClient.builder()
                .clientName("Test Integration Client")
                .apiKeyHash(HashUtils.sha256Hex(rawKey))
                .apiKeyPrefix("gv_live_test_int")
                .permissions("CAPTURE,VERIFY,TIME_TOKEN")
                .status("ACTIVE")
                .build());
    }

    @Test
    @DisplayName("Should successfully authenticate genuine GeoVerity image with embedded COM marker")
    void testVerifyAuthenticImage() throws IOException {
        String verificationId = "SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E";

        // 1. Create simulated JPEG bytes with embedded COM marker
        byte[] rawPhotoBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, 0x10, 0x20, 0x30, 0x40, (byte) 0xFF, (byte) 0xD9};
        byte[] finalImageBytes = jpegMarkerExtractor.embedVerificationId(rawPhotoBytes, verificationId);

        // 2. Canonical metadata
        CanonicalMetadataDto metadata = CanonicalMetadataDto.builder()
                .verificationId(verificationId)
                .trustedTimestamp(1788440712000L)
                .latitude(10.785234)
                .longitude(78.125432)
                .locationName("Karur, Tamil Nadu")
                .deviceId("dev_pixel8_gv_984128")
                .appVersion("1.0.0")
                .build();

        String canonicalJson = metadataSerializer.serializeToCanonicalJson(metadata);
        byte[] canonicalBytes = metadataSerializer.serializeToCanonicalBytes(metadata);

        // 3. Compute Composite SHA-256 over: finalImageBytes + canonicalBytes
        String compositeHash = HashUtils.calculateCompositeHash(finalImageBytes, canonicalBytes);

        // 4. Server signs with ECDSA P-256
        byte[] payloadToSign = (compositeHash + ":" + canonicalJson).getBytes(StandardCharsets.UTF_8);
        String ecdsaSig = ecdsaSigner.sign(payloadToSign);

        // 5. Store in DB
        VerificationRecord record = VerificationRecord.builder()
                .verificationId(verificationId)
                .sha256Hash(compositeHash)
                .canonicalMetadata(canonicalJson)
                .trustedServerTimestamp(Instant.ofEpochMilli(1788440712000L))
                .ecdsaSignature(ecdsaSig)
                .deviceId("dev_pixel8_gv_984128")
                .apiClient(testClient)
                .status("AUTHENTICATED")
                .build();
        verificationRecordRepository.save(record);

        // 6. Third party submits ONLY the final image bytes
        VerificationResponse result = verificationService.verifyImage(finalImageBytes, "127.0.0.1");

        assertNotNull(result);
        assertEquals("AUTHENTIC", result.getStatus());
        assertTrue(result.isSignatureValid());
        assertTrue(result.isHashMatched());
        assertEquals("Karur, Tamil Nadu", result.getLocation());
        assertEquals("10.785234, 78.125432", result.getGps());
    }

    @Test
    @DisplayName("Should REJECT image if 1 pixel/byte is altered (Hash Mismatch)")
    void testVerifyTamperedImageRejection() throws IOException {
        String verificationId = "SGA-99AA88BB-C34E-4621-91AB-5369A18DF50E";

        byte[] rawPhotoBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, 0x11, 0x22, 0x33, 0x44, (byte) 0xFF, (byte) 0xD9};
        byte[] finalImageBytes = jpegMarkerExtractor.embedVerificationId(rawPhotoBytes, verificationId);

        CanonicalMetadataDto metadata = CanonicalMetadataDto.builder()
                .verificationId(verificationId)
                .trustedTimestamp(1788440712000L)
                .latitude(10.785234)
                .longitude(78.125432)
                .locationName("Karur, Tamil Nadu")
                .deviceId("dev_pixel8_gv_984128")
                .appVersion("1.0.0")
                .build();

        String canonicalJson = metadataSerializer.serializeToCanonicalJson(metadata);
        byte[] canonicalBytes = metadataSerializer.serializeToCanonicalBytes(metadata);
        String compositeHash = HashUtils.calculateCompositeHash(finalImageBytes, canonicalBytes);

        byte[] payloadToSign = (compositeHash + ":" + canonicalJson).getBytes(StandardCharsets.UTF_8);
        String ecdsaSig = ecdsaSigner.sign(payloadToSign);

        VerificationRecord record = VerificationRecord.builder()
                .verificationId(verificationId)
                .sha256Hash(compositeHash)
                .canonicalMetadata(canonicalJson)
                .trustedServerTimestamp(Instant.ofEpochMilli(1788440712000L))
                .ecdsaSignature(ecdsaSig)
                .deviceId("dev_pixel8_gv_984128")
                .apiClient(testClient)
                .status("AUTHENTICATED")
                .build();
        verificationRecordRepository.save(record);

        // Tamper: Modify 1 byte of image (e.g. Photoshop, cropping, WhatsApp re-encoding)
        byte[] tamperedBytes = finalImageBytes.clone();
        tamperedBytes[tamperedBytes.length - 1] = 0x00;

        VerificationResponse result = verificationService.verifyImage(tamperedBytes, "127.0.0.1");

        assertNotNull(result);
        assertEquals("NOT_AUTHENTIC", result.getStatus());
        assertTrue(result.isSignatureValid()); // Stored record signature is valid
        assertFalse(result.isHashMatched());  // But the image bytes DO NOT match the authenticated hash!
        assertTrue(result.getFailureReason().contains("Hash mismatch"));
    }
}
