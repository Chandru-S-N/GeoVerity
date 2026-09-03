package org.geoverity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificationResponse {
    
    // Core Boolean status for all client apps
    @JsonProperty("verified")
    @Builder.Default
    private boolean verified = false;

    private String verificationId;
    private String status; // AUTHENTIC, VERIFIED, or NOT_AUTHENTIC

    @Builder.Default
    private int confidenceScore = 0;

    @JsonProperty("hashMatch")
    private boolean hashMatch;

    @JsonProperty("signatureValid")
    private boolean signatureValid;

    private String location;
    private String gps;
    private String trustedTimestamp;
    private String deviceId;
    private String sha256Hash;
    private CanonicalMetadataDto canonicalMetadata;
    private String failureReason;
    private String reason;
    private List<String> verificationSteps;
    private String auditLogId;

    // Detailed metadata object for third-party portals (College ERP, Govt, etc.)
    private Map<String, Object> metadata;

    // Cryptographic security proof breakdown
    private Map<String, Object> securityProof;

    public boolean isHashMatched() {
        return this.hashMatch;
    }

    public void setHashMatched(boolean hashMatched) {
        this.hashMatch = hashMatched;
    }
}
