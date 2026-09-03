package org.geoverity;

import org.geoverity.crypto.HashUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class Sha256HasherTest {

    @Test
    @DisplayName("Should compute accurate deterministic SHA-256 hash")
    void testDeterministicSha256() {
        String input = "GeoVerity-Secure-Digital-Evidence-2026";
        String hash1 = HashUtils.sha256Hex(input);
        String hash2 = HashUtils.sha256Hex(input);

        assertEquals(64, hash1.length(), "SHA-256 hex string must be 64 characters");
        assertEquals(hash1, hash2, "Hashing identical input must produce identical hash");
    }

    @Test
    @DisplayName("Should compute composite hash over image bytes + canonical metadata")
    void testCompositeHash() {
        byte[] imageBytes = new byte[]{0x10, 0x20, 0x30, 0x40, 0x50};
        byte[] canonicalMetadataBytes = "{\"verificationId\":\"SGA-1234\"}".getBytes(StandardCharsets.UTF_8);

        String compositeHash = HashUtils.calculateCompositeHash(imageBytes, canonicalMetadataBytes);
        assertNotNull(compositeHash);
        assertEquals(64, compositeHash.length());

        // Modifying 1 byte in image bytes must change the composite hash completely
        byte[] tamperedImage = new byte[]{0x10, 0x20, 0x30, 0x40, 0x51};
        String tamperedHash = HashUtils.calculateCompositeHash(tamperedImage, canonicalMetadataBytes);

        assertNotEquals(compositeHash, tamperedHash, "1-bit alteration in image bytes must change composite hash");
    }
}
