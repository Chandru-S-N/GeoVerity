package org.geoverity;

import org.geoverity.crypto.CanonicalMetadataSerializer;
import org.geoverity.dto.CanonicalMetadataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalMetadataTest {

    private final CanonicalMetadataSerializer serializer = new CanonicalMetadataSerializer();

    @Test
    @DisplayName("Should serialize canonical metadata with strictly ordered keys and normalized floats")
    void testDeterministicSerialization() {
        CanonicalMetadataDto dto1 = CanonicalMetadataDto.builder()
                .verificationId("SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E")
                .trustedTimestamp(1788440712000L)
                .latitude(10.785234123) // extra decimals
                .longitude(78.125432888)
                .locationName("Karur, Tamil Nadu")
                .deviceId("dev_pixel8_gv_984128")
                .appVersion("1.0.0")
                .build();

        CanonicalMetadataDto dto2 = CanonicalMetadataDto.builder()
                .verificationId("SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E")
                .trustedTimestamp(1788440712000L)
                .latitude(10.785234)
                .longitude(78.125433) // rounded up
                .locationName("Karur, Tamil Nadu")
                .deviceId("dev_pixel8_gv_984128")
                .appVersion("1.0.0")
                .build();

        String json1 = serializer.serializeToCanonicalJson(dto1);
        String json2 = serializer.serializeToCanonicalJson(dto2);

        assertEquals(json1, json2, "Both metadata representations must normalize to the exact same canonical JSON");
        assertTrue(json1.startsWith("{\"appVersion\":"), "Keys must be strictly alphabetical: appVersion must be first");
    }
}
