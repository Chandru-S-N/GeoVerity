package org.geoverity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoverity.entity.Device;
import org.geoverity.repository.DeviceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Device Management", description = "Device hardware registration and attestation")
public class DeviceController {

    private final DeviceRepository deviceRepository;

    @PostMapping(value = {"/register", ""})
    @Operation(summary = "Register or refresh device identity", description = "Registers hardware device parameters and returns device session status.")
    @Transactional
    public ResponseEntity<Map<String, Object>> registerDevice(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String deviceId = (String) body.getOrDefault("deviceId", "dev_" + UUID.randomUUID().toString().substring(0, 8));
        String deviceModel = (String) body.getOrDefault("deviceModel", "Android Mobile Device");
        String osVersion = (String) body.getOrDefault("osVersion", "Android 14 (API 34)");
        String appVersion = (String) body.getOrDefault("appVersion", "1.0.0");

        Optional<Device> existingOpt = deviceRepository.findByDeviceId(deviceId);
        Device device;

        if (existingOpt.isPresent()) {
            device = existingOpt.get();
            device.setDeviceModel(deviceModel);
            device.setOsVersion(osVersion);
            device.setAppVersion(appVersion);
            device.setLastSeenAt(Instant.now());
            device = deviceRepository.save(device);
            log.info("Refreshed existing device registration: {}", deviceId);
        } else {
            device = Device.builder()
                    .deviceId(deviceId)
                    .deviceModel(deviceModel)
                    .osVersion(osVersion)
                    .appVersion(appVersion)
                    .status("ACTIVE")
                    .firstSeenAt(Instant.now())
                    .lastSeenAt(Instant.now())
                    .build();
            device = deviceRepository.save(device);
            log.info("Registered new device: {}", deviceId);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "deviceId", device.getDeviceId(),
                "deviceBearerToken", "gv_dev_token_" + UUID.randomUUID().toString().replace("-", ""),
                "status", device.getStatus(),
                "registeredAt", device.getFirstSeenAt().toString()
        ));
    }
}
