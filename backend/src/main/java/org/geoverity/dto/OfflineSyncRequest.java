package org.geoverity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineSyncRequest {

    @NotBlank(message = "Verification ID is required")
    private String verificationId;

    @NotNull(message = "Canonical metadata is required")
    private CanonicalMetadataDto canonicalMetadata;

    @NotBlank(message = "SHA-256 hash is required")
    private String sha256Hash;

    @NotNull(message = "lastTrustedServerTimestamp is required")
    private Long lastTrustedServerTimestamp;

    @NotNull(message = "lastTrustedElapsedRealtime is required")
    private Long lastTrustedElapsedRealtime;

    @NotNull(message = "captureElapsedRealtime is required")
    private Long captureElapsedRealtime;

    @NotNull(message = "deviceCaptureTime is required")
    private Long deviceCaptureTime;
}
