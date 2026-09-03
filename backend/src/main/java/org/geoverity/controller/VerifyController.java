package org.geoverity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoverity.crypto.QrCodeService;
import org.geoverity.dto.ApiClientResponse;
import org.geoverity.dto.CreateApiClientRequest;
import org.geoverity.dto.VerificationResponse;
import org.geoverity.service.ApiClientService;
import org.geoverity.service.VerificationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Third-Party Verification", description = "Public, zero-login endpoint for verifying digital photographic evidence")
public class VerifyController {

    private final VerificationService verificationService;
    private final QrCodeService qrCodeService;
    private final ApiClientService apiClientService;

    @GetMapping("/ping")
    @Operation(summary = "Public server health ping", description = "Lightweight ping to verify connection with the GeoVerity Cryptographic Authority.")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "GeoVerity Cryptographic Authority",
            "version", "1.0.0",
            "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Verify original GeoVerity image",
        description = "Accepts the original image file (and optional verificationId). Extracts embedded Verification ID, verifies server ECDSA P-256 signature, and re-computes composite SHA-256 hash to prove absolute digital authenticity without login."
    )
    public ResponseEntity<VerificationResponse> verifyImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "verificationId", required = false) String verificationId,
            HttpServletRequest request) throws IOException {

        MultipartFile targetFile = file != null ? file : image;

        if (targetFile == null || targetFile.isEmpty()) {
            return ResponseEntity.badRequest().body(VerificationResponse.builder()
                    .verified(false)
                    .status("NOT_AUTHENTIC")
                    .failureReason("Uploaded image file is missing or empty.")
                    .reason("EMPTY_IMAGE_FILE")
                    .build());
        }

        byte[] imageBytes = targetFile.getBytes();
        String clientIp = request.getRemoteAddr();
        String apiKeyHeader = request.getHeader("X-API-Key");
        String organization = apiKeyHeader != null ? "API Client (" + apiKeyHeader.substring(0, Math.min(12, apiKeyHeader.length())) + "...)" : "Public Verifier";

        log.info("Received verification request for file '{}' ({} bytes, vId={}) from IP {}",
                targetFile.getOriginalFilename(), imageBytes.length, verificationId, clientIp);

        VerificationResponse response = verificationService.verifyImage(imageBytes, verificationId, clientIp, organization);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/records/{verificationId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get QR code PNG for a verification record", description = "Generates and returns the PNG QR code badge encoding the record verification ID.")
    public ResponseEntity<byte[]> getRecordQrCode(@PathVariable String verificationId) {
        try {
            byte[] qrBytes = qrCodeService.generateQrCodePngBytes(verificationId, 300, 300);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .body(qrBytes);
        } catch (Exception e) {
            log.error("Failed generating QR code for {}: {}", verificationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/keys/generate")
    @Operation(summary = "Generate third-party API Key", description = "Generates a new API Key and Secret for third-party institutional integrations (College ERP, Gov inspection systems).")
    public ResponseEntity<Map<String, String>> generateThirdPartyKey(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {

        String orgName = (body != null && body.containsKey("organizationName")) 
                ? body.get("organizationName") 
                : "Third-Party Integration Portal";

        CreateApiClientRequest createReq = CreateApiClientRequest.builder()
                .clientName(orgName)
                .permissions("CAPTURE,VERIFY,TIME_TOKEN")
                .build();

        ApiClientResponse clientRes = apiClientService.createApiClient(createReq, request.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "apiKey", clientRes.getRawApiKey(),
                "apiSecret", UUID.randomUUID().toString().replace("-", ""),
                "organizationName", orgName,
                "status", "ACTIVE"
        ));
    }
}
