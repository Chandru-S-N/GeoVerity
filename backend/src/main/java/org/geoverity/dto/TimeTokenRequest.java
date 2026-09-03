package org.geoverity.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeTokenRequest {
    private String deviceId;
    private Long clientTimestamp;
}
