package org.geoverity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiClientResponse {
    private UUID id;
    private String clientName;
    private String apiKeyPrefix;
    private String rawApiKey; // Only returned on creation or rotation
    private String permissions;
    private String status;
    private Instant createdAt;
    private Instant lastUsedAt;
}
