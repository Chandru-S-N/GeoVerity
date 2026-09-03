package org.geoverity.android.crypto

import java.security.MessageDigest

object Sha256Hasher {

    fun hashHex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return bytesToHex(hashBytes)
    }

    /**
     * Calculates SHA-256 over: finalImageBytes + canonicalMetadataBytes
     */
    fun calculateCompositeHash(finalImageBytes: ByteArray, canonicalMetadataBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(finalImageBytes)
        digest.update(canonicalMetadataBytes)
        return bytesToHex(digest.digest())
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i ushr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }
}
