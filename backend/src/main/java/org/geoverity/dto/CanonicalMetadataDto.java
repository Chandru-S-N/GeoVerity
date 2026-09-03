package org.geoverity.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonPropertyOrder({"appVersion", "deviceId", "latitude", "locationName", "longitude", "trustedTimestamp", "verificationId"})
public class CanonicalMetadataDto {
    private String appVersion;
    private String deviceId;
    private double latitude;
    private String locationName;
    private double longitude;
    private long trustedTimestamp;
    private String verificationId;
}
