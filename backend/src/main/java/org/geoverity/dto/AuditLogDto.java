package org.geoverity.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {
    private UUID id;
    private String eventType;
    private String clientIp;
    private UUID apiClientId;
    private String verificationId;
    private String status;
    private String details;
    private Instant createdAt;
}
