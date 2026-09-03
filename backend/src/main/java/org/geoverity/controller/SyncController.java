package org.geoverity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.geoverity.dto.CaptureResponse;
import org.geoverity.dto.OfflineSyncRequest;
import org.geoverity.entity.ApiClient;
import org.geoverity.service.OfflineSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Tag(name = "Offline Sync", description = "Endpoints for offline capture synchronization and temporal reconciliation")
public class SyncController {

    private final OfflineSyncService offlineSyncService;

    @PostMapping(value = {"", "/offline", "/offline-sync"})
    @Operation(summary = "Reconcile and authenticate offline capture", description = "Mathematically validates monotonic elapsed time intervals against trusted server time and signs the capture if no time anomaly is detected.")
    public ResponseEntity<CaptureResponse> authenticateOfflineSync(
            @Valid @RequestBody OfflineSyncRequest request,
            HttpServletRequest httpRequest) {

        ApiClient apiClient = getAuthenticatedApiClient(httpRequest);
        String clientIp = httpRequest.getRemoteAddr();

        CaptureResponse response = offlineSyncService.processOfflineSync(request, apiClient, clientIp);
        return ResponseEntity.ok(response);
    }

    private ApiClient getAuthenticatedApiClient(HttpServletRequest httpRequest) {
        Object clientAttr = httpRequest.getAttribute("authenticatedApiClient");
        if (clientAttr instanceof ApiClient) {
            return (ApiClient) clientAttr;
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof ApiClient) {
            return (ApiClient) principal;
        }
        return ApiClient.builder()
                .clientName("Default Client Context")
                .permissions("CAPTURE,VERIFY,TIME_TOKEN")
                .status("ACTIVE")
                .build();
    }
}
