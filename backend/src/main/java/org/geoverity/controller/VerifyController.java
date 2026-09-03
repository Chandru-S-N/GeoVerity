package org.geoverity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoverity.dto.VerificationResponse;
import org.geoverity.service.VerificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/verify")
@RequiredArgsConstructor
@Tag(name = "Third-Party Verification", description = "Public, zero-login endpoint for verifying digital photographic evidence")
public class VerifyController {

    private final VerificationService verificationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Verify original GeoVerity image",
        description = "Accepts ONLY the original image file. Extracts embedded Verification ID, verifies server ECDSA P-256 signature, and re-computes composite SHA-256 hash to prove absolute digital authenticity without login."
    )
    public ResponseEntity<VerificationResponse> verifyImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(VerificationResponse.builder()
                    .status("NOT_AUTHENTIC")
                    .failureReason("Uploaded file is empty.")
                    .build());
        }

        byte[] imageBytes = file.getBytes();
        String clientIp = request.getRemoteAddr();

        log.info("Received verification request for file '{}' ({} bytes) from IP {}",
                file.getOriginalFilename(), imageBytes.length, clientIp);

        VerificationResponse response = verificationService.verifyImage(imageBytes, clientIp);
        return ResponseEntity.ok(response);
    }
}
