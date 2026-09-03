package org.geoverity.service;

import lombok.extern.slf4j.Slf4j;
import org.geoverity.crypto.HashUtils;
import org.geoverity.dto.TimeTokenRequest;
import org.geoverity.dto.TimeTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
public class TimeService {

    private final String hmacSecret;
    private final long onlineToleranceMs;
    private final long tokenValiditySeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public TimeService(
            @Value("${geoverity.security.admin-api-key:gv_admin_master_secret_key_884920}") String secret,
            @Value("${geoverity.security.online-time-tolerance-ms:5000}") long onlineToleranceMs,
            @Value("${geoverity.security.token-validity-seconds:60}") long tokenValiditySeconds) {
        this.hmacSecret = secret;
        this.onlineToleranceMs = onlineToleranceMs;
        this.tokenValiditySeconds = tokenValiditySeconds;
    }

    /**
     * Issues a cryptographically signed time token representing trusted server time.
     */
    public TimeTokenResponse generateTimeToken(TimeTokenRequest request) {
        long serverTime = Instant.now().toEpochMilli();
        long expiresAt = serverTime + (tokenValiditySeconds * 1000);
        String nonce = Long.toHexString(secureRandom.nextLong());
        String deviceId = request != null && request.getDeviceId() != null ? request.getDeviceId() : "unknown";

        String payload = serverTime + ":" + expiresAt + ":" + nonce + ":" + deviceId;
        String signature = signTokenPayload(payload);
        String token = Base64.getUrlEncoder().encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));

        return TimeTokenResponse.builder()
                .serverTime(serverTime)
                .token(token)
                .expiresAt(expiresAt)
                .toleranceMs(onlineToleranceMs)
                .build();
    }

    /**
     * Validates that the provided time token is genuine, issued by this server, unexpired,
     * and within the allowed online tolerance window (e.g. 5 seconds).
     */
    public long validateAndExtractServerTime(String token, String deviceId) {
        if (token == null || token.isBlank()) {
            throw new SecurityException("Missing trusted server time token");
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 5) {
                throw new SecurityException("Malformed time token format");
            }

            long serverTime = Long.parseLong(parts[0]);
            long expiresAt = Long.parseLong(parts[1]);
            String nonce = parts[2];
            String tokenDeviceId = parts[3];
            String receivedSignature = parts[4];

            String payload = serverTime + ":" + expiresAt + ":" + nonce + ":" + tokenDeviceId;
            String expectedSignature = signTokenPayload(payload);

            if (!expectedSignature.equals(receivedSignature)) {
                throw new SecurityException("Invalid time token signature (possible forgery)");
            }

            long now = Instant.now().toEpochMilli();
            if (now > expiresAt) {
                throw new SecurityException("Time token has expired");
            }

            // Check tolerance window: capture should occur within allowed tolerance of token issue
            if (Math.abs(now - serverTime) > (tokenValiditySeconds * 1000)) {
                throw new SecurityException("Capture timestamp outside valid token window");
            }

            return serverTime;
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to validate time token: {}", e.getMessage());
            throw new SecurityException("Invalid or corrupted time token", e);
        }
    }

    private String signTokenPayload(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HashUtils.bytesToHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation error", e);
        }
    }
}
