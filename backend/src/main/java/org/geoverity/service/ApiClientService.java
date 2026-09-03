package org.geoverity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoverity.crypto.HashUtils;
import org.geoverity.dto.ApiClientResponse;
import org.geoverity.dto.CreateApiClientRequest;
import org.geoverity.entity.ApiClient;
import org.geoverity.repository.ApiClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiClientService {

    private final ApiClientRepository apiClientRepository;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String KEY_PREFIX = "gv_live_";

    @Transactional
    public ApiClientResponse createApiClient(CreateApiClientRequest request, String adminIp) {
        String rawKey = generateSecureApiKey();
        String hash = HashUtils.sha256Hex(rawKey);
        String prefix = rawKey.substring(0, 16);
        String permissions = request.getPermissions() != null && !request.getPermissions().isBlank()
                ? request.getPermissions()
                : "CAPTURE,VERIFY,TIME_TOKEN";

        ApiClient client = ApiClient.builder()
                .clientName(request.getClientName().trim())
                .apiKeyHash(hash)
                .apiKeyPrefix(prefix)
                .permissions(permissions)
                .status("ACTIVE")
                .build();

        ApiClient saved = apiClientRepository.save(client);

        auditService.logEvent("API_KEY_CREATED", adminIp, saved.getId(), null, "SUCCESS",
                "Created API client: " + saved.getClientName());

        return mapToResponse(saved, rawKey);
    }

    @Transactional
    public ApiClientResponse rotateApiKey(UUID clientId, String adminIp) {
        ApiClient client = apiClientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("API client not found: " + clientId));

        String rawKey = generateSecureApiKey();
        String hash = HashUtils.sha256Hex(rawKey);
        String prefix = rawKey.substring(0, 16);

        client.setApiKeyHash(hash);
        client.setApiKeyPrefix(prefix);
        client.setStatus("ACTIVE");
        ApiClient updated = apiClientRepository.save(client);

        auditService.logEvent("API_KEY_ROTATED", adminIp, client.getId(), null, "SUCCESS",
                "Rotated API key for client: " + client.getClientName());

        return mapToResponse(updated, rawKey);
    }

    @Transactional
    public ApiClientResponse revokeApiKey(UUID clientId, String adminIp) {
        ApiClient client = apiClientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("API client not found: " + clientId));

        client.setStatus("REVOKED");
        ApiClient updated = apiClientRepository.save(client);

        auditService.logEvent("API_KEY_REVOKED", adminIp, client.getId(), null, "SUCCESS",
                "Revoked API key for client: " + client.getClientName());

        return mapToResponse(updated, null);
    }

    @Transactional
    public ApiClientResponse toggleStatus(UUID clientId, String newStatus, String adminIp) {
        ApiClient client = apiClientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("API client not found: " + clientId));

        client.setStatus(newStatus.toUpperCase());
        ApiClient updated = apiClientRepository.save(client);

        auditService.logEvent("API_KEY_STATUS_CHANGED", adminIp, client.getId(), null, "SUCCESS",
                "Changed status to " + newStatus + " for client: " + client.getClientName());

        return mapToResponse(updated, null);
    }

    public List<ApiClientResponse> getAllClients() {
        return apiClientRepository.findAll().stream()
                .map(client -> mapToResponse(client, null))
                .collect(Collectors.toList());
    }

    public Optional<ApiClient> authenticateApiKey(String rawApiKey) {
        if (rawApiKey == null || !rawApiKey.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }
        String hash = HashUtils.sha256Hex(rawApiKey);
        Optional<ApiClient> clientOpt = apiClientRepository.findByApiKeyHash(hash);
        if (clientOpt.isPresent()) {
            ApiClient client = clientOpt.get();
            if ("ACTIVE".equalsIgnoreCase(client.getStatus())) {
                client.setLastUsedAt(Instant.now());
                apiClientRepository.save(client);
                return Optional.of(client);
            }
        }
        return Optional.empty();
    }

    private String generateSecureApiKey() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return KEY_PREFIX + HashUtils.bytesToHex(randomBytes);
    }

    private ApiClientResponse mapToResponse(ApiClient client, String rawKey) {
        return ApiClientResponse.builder()
                .id(client.getId())
                .clientName(client.getClientName())
                .apiKeyPrefix(client.getApiKeyPrefix() + "************")
                .rawApiKey(rawKey)
                .permissions(client.getPermissions())
                .status(client.getStatus())
                .createdAt(client.getCreatedAt())
                .lastUsedAt(client.getLastUsedAt())
                .build();
    }
}
