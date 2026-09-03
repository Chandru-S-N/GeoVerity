package org.geoverity;

import org.geoverity.crypto.JpegMarkerExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JpegMarkerTest {

    private final JpegMarkerExtractor extractor = new JpegMarkerExtractor();

    @Test
    @DisplayName("Should embed Verification ID into JPEG COM marker and extract it successfully")
    void testEmbedAndExtractMarker() throws IOException {
        // Mock minimal valid JPEG byte stream with SOI (0xFF, 0xD8) and EOI (0xFF, 0xD9)
        byte[] mockJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
        String verificationId = "SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E";

        byte[] embeddedJpeg = extractor.embedVerificationId(mockJpeg, verificationId);
        assertNotNull(embeddedJpeg);
        assertTrue(embeddedJpeg.length > mockJpeg.length);

        // Extract
        Optional<String> extractedId = extractor.extractVerificationId(embeddedJpeg);
        assertTrue(extractedId.isPresent(), "Extracted Verification ID must be present");
        assertEquals(verificationId, extractedId.get(), "Extracted Verification ID must match embedded ID exactly");
    }
}
