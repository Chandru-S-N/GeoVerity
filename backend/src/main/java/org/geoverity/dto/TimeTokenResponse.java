package org.geoverity.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeTokenResponse {
    private long serverTime;
    private String token;
    private long expiresAt;
    private long toleranceMs;
}
