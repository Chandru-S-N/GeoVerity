package org.geoverity.android.image

import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object JpegMarkerReader {

    private val VERIFICATION_ID_PATTERN = Pattern.compile("SGA-[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")
    private const val MARKER_HEADER = "GEOVERITY:"

    fun extractVerificationId(jpegBytes: ByteArray): String? {
        if (jpegBytes.size < 4) return null
        if (jpegBytes[0] != 0xFF.toByte() || jpegBytes[1] != 0xD8.toByte()) {
            return fallbackRegexScan(jpegBytes)
        }

        var i = 2
        while (i < jpegBytes.size - 1) {
            if (jpegBytes[i] == 0xFF.toByte()) {
                val marker = jpegBytes[i + 1]
                if (marker == 0xDA.toByte() || marker == 0xD9.toByte()) break
                if (i + 3 >= jpegBytes.size) break

                val length = ((jpegBytes[i + 2].toInt() and 0xFF) shl 8) or (jpegBytes[i + 3].toInt() and 0xFF)
                if (length < 2 || i + 2 + length > jpegBytes.size) break

                if (marker == 0xFE.toByte() || (marker.toInt() and 0xFF in 0xE0..0xEF)) {
                    val payloadLength = length - 2
                    val segmentStr = String(jpegBytes, i + 4, payloadLength, StandardCharsets.UTF_8)

                    if (segmentStr.startsWith(MARKER_HEADER)) {
                        return segmentStr.substring(MARKER_HEADER.length).trim()
                    }

                    val matcher = VERIFICATION_ID_PATTERN.matcher(segmentStr)
                    if (matcher.find()) {
                        return matcher.group()
                    }
                }
                i += 2 + length
            } else {
                i++
            }
        }

        return fallbackRegexScan(jpegBytes)
    }

    private fun fallbackRegexScan(bytes: ByteArray): String? {
        val scanLen = bytes.size.coerceAtMost(65536)
        val chunk = String(bytes, 0, scanLen, StandardCharsets.ISO_8859_1)
        val matcher = VERIFICATION_ID_PATTERN.matcher(chunk)
        return if (matcher.find()) matcher.group() else null
    }
}
