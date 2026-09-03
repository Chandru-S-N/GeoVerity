package org.geoverity.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtils {

    private HashUtils() {}

    public static MessageDigest getSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public static byte[] sha256Bytes(byte[] data) {
        return getSha256Digest().digest(data);
    }

    public static String sha256Hex(byte[] data) {
        return bytesToHex(sha256Bytes(data));
    }

    public static String sha256Hex(String text) {
        return sha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Calculates SHA-256 over: finalImageBytes + canonicalMetadataBytes
     */
    public static String calculateCompositeHash(byte[] finalImageBytes, byte[] canonicalMetadataBytes) {
        MessageDigest digest = getSha256Digest();
        digest.update(finalImageBytes);
        digest.update(canonicalMetadataBytes);
        return bytesToHex(digest.digest());
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
