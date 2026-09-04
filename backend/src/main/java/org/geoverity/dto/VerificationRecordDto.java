package org.geoverity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRecordDto {
    private UUID id;
    private String verificationId;
    private String sha256Hash;
    private String canonicalMetadata;
    private Instant trustedServerTimestamp;
    private String ecdsaSignature;
    private String deviceId;
    private String apiClientName;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
