package org.geoverity.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsDto {
    private long totalRecords;
    private long authenticatedRecords;
    private long totalApiClients;
    private long activeApiClients;
    private long totalAuditEvents;
    private long timeAnomalies;
}
