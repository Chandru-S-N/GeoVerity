package org.geoverity.crypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.geoverity.dto.CanonicalMetadataDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

@Component
public class CanonicalMetadataSerializer {

    private final ObjectMapper objectMapper;

    public CanonicalMetadataSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    /**
     * Serializes CanonicalMetadataDto into a deterministic canonical UTF-8 byte representation.
     */
    public byte[] serializeToCanonicalBytes(CanonicalMetadataDto dto) {
        return serializeToCanonicalJson(dto).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Serializes CanonicalMetadataDto into a deterministic canonical JSON string.
     */
    public String serializeToCanonicalJson(CanonicalMetadataDto dto) {
        // Normalize coordinates to 6 decimal places
        BigDecimal lat = BigDecimal.valueOf(dto.getLatitude()).setScale(6, RoundingMode.HALF_UP);
        BigDecimal lon = BigDecimal.valueOf(dto.getLongitude()).setScale(6, RoundingMode.HALF_UP);

        CanonicalMetadataDto normalized = CanonicalMetadataDto.builder()
                .appVersion(dto.getAppVersion() != null ? dto.getAppVersion().trim() : "1.0.0")
                .deviceId(dto.getDeviceId() != null ? dto.getDeviceId().trim() : "")
                .latitude(lat.doubleValue())
                .longitude(lon.doubleValue())
                .locationName(dto.getLocationName() != null ? dto.getLocationName().trim() : "")
                .trustedTimestamp(dto.getTrustedTimestamp())
                .verificationId(dto.getVerificationId() != null ? dto.getVerificationId().trim() : "")
                .build();

        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize canonical metadata", e);
        }
    }

    public CanonicalMetadataDto deserializeFromJson(String json) {
        try {
            return objectMapper.readValue(json, CanonicalMetadataDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse canonical metadata JSON", e);
        }
    }
}
