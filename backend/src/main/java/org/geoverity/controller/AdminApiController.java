package org.geoverity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.geoverity.dto.*;
import org.geoverity.entity.AuditLog;
import org.geoverity.entity.VerificationRecord;
import org.geoverity.repository.ApiClientRepository;
import org.geoverity.repository.AuditLogRepository;
import org.geoverity.repository.VerificationRecordRepository;
import org.geoverity.service.ApiClientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "Administrator endpoints for managing API clients, reviewing verification records, and inspecting audit logs")
public class AdminApiController {

    private final ApiClientService apiClientService;
    private final ApiClientRepository apiClientRepository;
    private final VerificationRecordRepository verificationRecordRepository;
    private final AuditLogRepository auditLogRepository;

    @PostMapping("/api-clients")
    @Operation(summary = "Create new API client", description = "Generates a cryptographically secure random API key, stores its hash, and returns the raw key once.")
    public ResponseEntity<ApiClientResponse> createApiClient(
            @Valid @RequestBody CreateApiClientRequest request,
            HttpServletRequest httpRequest) {
        ApiClientResponse response = apiClientService.createApiClient(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api-clients")
    @Operation(summary = "List all API clients", description = "Returns all registered API clients with masked key prefixes and status.")
    public ResponseEntity<List<ApiClientResponse>> getAllApiClients() {
        return ResponseEntity.ok(apiClientService.getAllClients());
    }

    @PostMapping("/api-clients/{id}/rotate")
    @Operation(summary = "Rotate API key", description = "Generates a new key and invalidates the previous key for the specified client.")
    public ResponseEntity<ApiClientResponse> rotateApiKey(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        ApiClientResponse response = apiClientService.rotateApiKey(id, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api-clients/{id}/revoke")
    @Operation(summary = "Revoke API key", description = "Permanently revokes the API key for the specified client.")
    public ResponseEntity<ApiClientResponse> revokeApiKey(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        ApiClientResponse response = apiClientService.revokeApiKey(id, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api-clients/{id}/status")
    @Operation(summary = "Toggle API client status", description = "Sets client status to ACTIVE or DISABLED.")
    public ResponseEntity<ApiClientResponse> updateClientStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            HttpServletRequest httpRequest) {
        ApiClientResponse response = apiClientService.toggleStatus(id, status, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verification-records")
    @Operation(summary = "Query verification records", description = "Returns paginated list of authenticated cryptographic evidence records.")
    public ResponseEntity<Page<VerificationRecord>> getVerificationRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<VerificationRecord> records = verificationRecordRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(records);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Query security audit logs", description = "Returns paginated stream of security and authentication events.")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        
        Page<AuditLogDto> dtos = logs.map(log -> AuditLogDto.builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .clientIp(log.getClientIp())
                .apiClientId(log.getApiClientId())
                .verificationId(log.getVerificationId())
                .status(log.getStatus())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/stats")
    @Operation(summary = "System statistics dashboard", description = "Provides real-time metrics on authenticated captures, anomalies, and active API clients.")
    public ResponseEntity<AdminStatsDto> getSystemStats() {
        long totalRecords = verificationRecordRepository.count();
        long authenticatedRecords = verificationRecordRepository.countByStatus("AUTHENTICATED");
        long totalClients = apiClientRepository.count();
        long activeClients = apiClientRepository.findAll().stream().filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus())).count();
        long totalAudits = auditLogRepository.count();
        long anomalies = auditLogRepository.countByEventType("TIME_ANOMALY");

        AdminStatsDto stats = AdminStatsDto.builder()
                .totalRecords(totalRecords)
                .authenticatedRecords(authenticatedRecords)
                .totalApiClients(totalClients)
                .activeApiClients(activeClients)
                .totalAuditEvents(totalAudits)
                .timeAnomalies(anomalies)
                .build();

        return ResponseEntity.ok(stats);
    }
}
