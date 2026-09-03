package org.geoverity.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaptureResponse {
    private String verificationId;
    private String status; // AUTHENTICATED
    private String trustedTimestamp;
    private String sha256;
    private String signatureStatus; // VALID
    private String ecdsaSignature;
}
