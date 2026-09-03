package org.geoverity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoverity.entity.AuditLog;
import org.geoverity.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void logEvent(String eventType, String clientIp, UUID apiClientId, String verificationId, String status, String details) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .eventType(eventType)
                    .clientIp(clientIp)
                    .apiClientId(apiClientId)
                    .verificationId(verificationId)
                    .status(status)
                    .details(details)
                    .build();
            auditLogRepository.save(logEntry);
            log.info("AUDIT [{}]: status={}, verificationId={}, clientIp={}", eventType, status, verificationId, clientIp);
        } catch (Exception e) {
            log.error("Failed to write audit log for event {}: {}", eventType, e.getMessage());
        }
    }
}
