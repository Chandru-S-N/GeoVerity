package org.geoverity;

import org.geoverity.crypto.EcdsaSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class EcdsaSignerTest {

    private EcdsaSigner signer;

    @BeforeEach
    void setUp() {
        signer = new EcdsaSigner();
        File tempDir = new File("./target/test-keys-" + System.currentTimeMillis());
        tempDir.mkdirs();
        ReflectionTestUtils.setField(signer, "keyDirectory", tempDir.getAbsolutePath());
        signer.init();
    }

    @Test
    @DisplayName("Should generate valid ECDSA P-256 signature and verify successfully")
    void testSignAndVerifySuccess() {
        byte[] payload = "SHA256_HASH_SAMPLE:CANONICAL_METADATA_PAYLOAD".getBytes(StandardCharsets.UTF_8);
        String signature = signer.sign(payload);

        assertNotNull(signature, "Signature should not be null");
        assertFalse(signature.isBlank(), "Signature should not be empty");

        boolean verified = signer.verify(payload, signature);
        assertTrue(verified, "Signature verification must succeed for identical payload");
    }

    @Test
    @DisplayName("Should reject verification if payload was tampered")
    void testTamperedPayloadRejection() {
        byte[] payload = "AUTHENTIC_PAYLOAD".getBytes(StandardCharsets.UTF_8);
        String signature = signer.sign(payload);

        byte[] tamperedPayload = "TAMPERED_PAYLOAD".getBytes(StandardCharsets.UTF_8);
        boolean verified = signer.verify(tamperedPayload, signature);
        assertFalse(verified, "Signature verification must fail when payload is tampered");
    }

    @Test
    @DisplayName("Should reject verification if signature was corrupted")
    void testCorruptedSignatureRejection() {
        byte[] payload = "AUTHENTIC_PAYLOAD".getBytes(StandardCharsets.UTF_8);
        String signature = signer.sign(payload);

        String corruptedSignature = signature.substring(0, signature.length() - 4) + "AAAA";
        boolean verified = signer.verify(payload, corruptedSignature);
        assertFalse(verified, "Signature verification must fail for corrupted signature string");
    }
}
