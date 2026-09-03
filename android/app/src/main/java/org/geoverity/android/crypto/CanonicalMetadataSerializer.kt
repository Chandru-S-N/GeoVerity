package org.geoverity.android.crypto

import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.util.Locale

data class CanonicalMetadata(
    val appVersion: String = "1.0.0",
    val deviceId: String,
    val latitude: Double,
    val locationName: String,
    val longitude: Double,
    val trustedTimestamp: Long,
    val verificationId: String
)

object CanonicalMetadataSerializer {

    /**
     * Serializes CanonicalMetadata into deterministic UTF-8 bytes matching backend format exactly.
     */
    fun serializeToCanonicalBytes(metadata: CanonicalMetadata): ByteArray {
        return serializeToCanonicalJson(metadata).toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * Serializes CanonicalMetadata into deterministic JSON string with alphabetically sorted keys
     * and standardized 6-decimal floating-point coordinates.
     */
    fun serializeToCanonicalJson(metadata: CanonicalMetadata): String {
        val lat = BigDecimal.valueOf(metadata.latitude).setScale(6, RoundingMode.HALF_UP).toDouble()
        val lon = BigDecimal.valueOf(metadata.longitude).setScale(6, RoundingMode.HALF_UP).toDouble()

        // Construct sorted JSON keys manually to ensure 100% byte-for-byte determinism
        return String.format(
            Locale.US,
            "{\"appVersion\":%s,\"deviceId\":%s,\"latitude\":%.6f,\"locationName\":%s,\"longitude\":%.6f,\"trustedTimestamp\":%d,\"verificationId\":%s}",
            JSONObject.quote(metadata.appVersion.trim()),
            JSONObject.quote(metadata.deviceId.trim()),
            lat,
            JSONObject.quote(metadata.locationName.trim()),
            lon,
            metadata.trustedTimestamp,
            JSONObject.quote(metadata.verificationId.trim())
        )
    }
}
