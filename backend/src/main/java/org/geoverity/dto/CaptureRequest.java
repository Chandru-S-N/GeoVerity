package org.geoverity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaptureRequest {

    @NotBlank(message = "Verification ID is required")
    private String verificationId;

    @NotBlank(message = "Time token is required")
    private String timeToken;

    @NotNull(message = "Canonical metadata is required")
    private CanonicalMetadataDto canonicalMetadata;

    @NotBlank(message = "SHA-256 hash is required")
    private String sha256Hash;
}
