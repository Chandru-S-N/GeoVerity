package org.geoverity.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoverity.entity.ApiClient;
import org.geoverity.entity.Device;
import org.geoverity.repository.ApiClientRepository;
import org.geoverity.repository.DeviceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ApiClientRepository apiClientRepository;
    private final DeviceRepository deviceRepository;

    @Override
    public void run(String... args) {
        if (apiClientRepository.count() == 0) {
            log.info("Seeding initial demo API client into database...");
            ApiClient defaultClient = ApiClient.builder()
                    .id(UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"))
                    .clientName("GeoVerity Official Android App")
                    .apiKeyHash("c759a224a9a084c5689da6d4002636c0a0c9a41df08f61546ea48d88e0f3fe67")
                    .apiKeyPrefix("gv_live_demo")
                    .permissions("CAPTURE,VERIFY,TIME_TOKEN")
                    .status("ACTIVE")
                    .createdAt(Instant.now())
                    .lastUsedAt(Instant.now())
                    .build();
            apiClientRepository.save(defaultClient);
            log.info("Default API Client seeded successfully: key prefix=gv_live_demo");
        }

        if (deviceRepository.count() == 0) {
            Device defaultDevice = Device.builder()
                    .id(UUID.fromString("b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e"))
                    .deviceId("dev_pixel8_gv_984128")
                    .deviceModel("Google Pixel 8 Pro")
                    .osVersion("Android 14 (API 34)")
                    .appVersion("1.0.0")
                    .status("ACTIVE")
                    .firstSeenAt(Instant.now())
                    .lastSeenAt(Instant.now())
                    .build();
            deviceRepository.save(defaultDevice);
            log.info("Default demo device seeded successfully: dev_pixel8_gv_984128");
        }
    }
}
