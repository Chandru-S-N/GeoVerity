package org.geoverity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificationResponse {
    private String verificationId;
    private String status; // AUTHENTIC or NOT_AUTHENTIC
    private boolean signatureValid;
    private boolean hashMatched;
    private String location;
    private String gps;
    private String trustedTimestamp;
    private String deviceId;
    private String sha256Hash;
    private CanonicalMetadataDto canonicalMetadata;
    private String failureReason;
    private List<String> verificationSteps;
}
