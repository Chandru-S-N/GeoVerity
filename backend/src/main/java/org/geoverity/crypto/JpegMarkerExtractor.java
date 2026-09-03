package org.geoverity.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class JpegMarkerExtractor {

    private static final byte MARKER_PREFIX = (byte) 0xFF;
    private static final byte SOI = (byte) 0xD8; // Start of Image
    private static final byte COM = (byte) 0xFE; // Comment Marker
    private static final byte SOS = (byte) 0xDA; // Start of Scan
    private static final byte EOI = (byte) 0xD9; // End of Image

    private static final String MARKER_HEADER = "GEOVERITY:";
    private static final Pattern VERIFICATION_ID_PATTERN = Pattern.compile("SGA-[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}");

    /**
     * Embeds a Verification ID as a JPEG COM segment (0xFF, 0xFE) right after SOI.
     * Must be done BEFORE SHA-256 calculation.
     */
    public byte[] embedVerificationId(byte[] jpegBytes, String verificationId) throws IOException {
        if (jpegBytes == null || jpegBytes.length < 4) {
            throw new IllegalArgumentException("Invalid JPEG byte stream");
        }
        if (jpegBytes[0] != MARKER_PREFIX || jpegBytes[1] != SOI) {
            throw new IllegalArgumentException("Data is not a valid JPEG (Missing SOI 0xFFD8)");
        }

        String commentContent = MARKER_HEADER + verificationId;
        byte[] commentBytes = commentContent.getBytes(StandardCharsets.UTF_8);
        int segmentLength = 2 + commentBytes.length; // 2 bytes for length + comment bytes

        ByteArrayOutputStream baos = new ByteArrayOutputStream(jpegBytes.length + segmentLength + 2);
        // Write SOI
        baos.write(MARKER_PREFIX);
        baos.write(SOI);

        // Write COM marker
        baos.write(MARKER_PREFIX);
        baos.write(COM);
        baos.write((segmentLength >> 8) & 0xFF);
        baos.write(segmentLength & 0xFF);
        baos.write(commentBytes);

        // Write remainder of original JPEG (from index 2 onwards)
        baos.write(jpegBytes, 2, jpegBytes.length - 2);

        return baos.toByteArray();
    }

    /**
     * Extracts Verification ID from JPEG COM markers and custom APP segments.
     */
    public Optional<String> extractVerificationId(byte[] jpegBytes) {
        if (jpegBytes == null || jpegBytes.length < 4) {
            return Optional.empty();
        }
        if (jpegBytes[0] != MARKER_PREFIX || jpegBytes[1] != SOI) {
            // Check fallback regex scan across first 64KB for embedded marker
            return fallbackPatternScan(jpegBytes);
        }

        int index = 2;
        while (index < jpegBytes.length - 1) {
            if (jpegBytes[index] == MARKER_PREFIX) {
                byte marker = jpegBytes[index + 1];
                if (marker == SOS || marker == EOI) {
                    break; // Reached image scan data or end
                }

                if (index + 3 >= jpegBytes.length) {
                    break;
                }

                int length = ((jpegBytes[index + 2] & 0xFF) << 8) | (jpegBytes[index + 3] & 0xFF);
                if (length < 2 || index + 2 + length > jpegBytes.length) {
                    break;
                }

                if (marker == COM || (marker >= (byte) 0xE0 && marker <= (byte) 0xEF)) { // COM or APP0-APP15
                    int payloadLength = length - 2;
                    String segmentString = new String(jpegBytes, index + 4, payloadLength, StandardCharsets.UTF_8);
                    
                    if (segmentString.startsWith(MARKER_HEADER)) {
                        String id = segmentString.substring(MARKER_HEADER.length()).trim();
                        return Optional.of(id);
                    }

                    Matcher matcher = VERIFICATION_ID_PATTERN.matcher(segmentString);
                    if (matcher.find()) {
                        return Optional.of(matcher.group());
                    }
                }

                index += 2 + length;
            } else {
                index++;
            }
        }

        return fallbackPatternScan(jpegBytes);
    }

    private Optional<String> fallbackPatternScan(byte[] bytes) {
        int scanLength = Math.min(bytes.length, 65536);
        String headerChunk = new String(bytes, 0, scanLength, StandardCharsets.ISO_8859_1);
        Matcher matcher = VERIFICATION_ID_PATTERN.matcher(headerChunk);
        if (matcher.find()) {
            return Optional.of(matcher.group());
        }
        return Optional.empty();
    }
}
