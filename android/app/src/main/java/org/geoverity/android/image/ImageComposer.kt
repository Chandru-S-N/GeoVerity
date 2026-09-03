package org.geoverity.android.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumMap
import java.util.Locale

object ImageComposer {

    private const val MARKER_HEADER = "GEOVERITY:"

    /**
     * Composes the final authenticated image:
     * 1. Keeps original photo area clean on top.
     * 2. Renders dedicated metadata footer at bottom.
     * 3. Renders dedicated QR code in the footer.
     * 4. Embeds Verification ID in JPEG COM marker (0xFF, 0xFE).
     * 5. Returns final authenticated image bytes ready for SHA-256 calculation.
     */
    fun composeFinalImageBytes(
        photoBitmap: Bitmap,
        locationName: String,
        latitude: Double,
        longitude: Double,
        trustedTimestamp: Long,
        verificationId: String
    ): ByteArray {
        val width = photoBitmap.width
        val photoHeight = photoBitmap.height
        val footerHeight = (photoHeight * 0.22f).toInt().coerceAtLeast(240)
        val totalHeight = photoHeight + footerHeight

        val compositeBitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(compositeBitmap)

        // 1. Draw original photo at top (unobstructed)
        canvas.drawBitmap(photoBitmap, 0f, 0f, null)

        // 2. Draw dedicated footer background at bottom
        val footerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, photoHeight.toFloat(), width.toFloat(), totalHeight.toFloat(), footerPaint)

        // 3. Format Date & Time from trusted timestamp
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)
        val dateStr = dateFormat.format(Date(trustedTimestamp))
        val timeStr = timeFormat.format(Date(trustedTimestamp))

        // 4. Draw Metadata Text on left side of footer
        val baseTextSize = (footerHeight * 0.10f).coerceAtLeast(22f)
        val lineSpacing = baseTextSize * 1.35f
        var currentY = photoHeight + (footerHeight * 0.18f)
        val leftMargin = width * 0.04f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = baseTextSize * 1.15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184) // Slate 400
            textSize = baseTextSize
            typeface = Typeface.DEFAULT
        }

        val idPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(129, 140, 248) // Indigo 400
            textSize = baseTextSize * 1.05f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("Location: $locationName", leftMargin, currentY, titlePaint)
        currentY += lineSpacing

        canvas.drawText(String.format(Locale.US, "GPS: %.6f, %.6f", latitude, longitude), leftMargin, currentY, textPaint)
        currentY += lineSpacing

        canvas.drawText("Date: $dateStr", leftMargin, currentY, textPaint)
        currentY += lineSpacing

        canvas.drawText("Time: $timeStr", leftMargin, currentY, textPaint)
        currentY += lineSpacing

        canvas.drawText("Verification ID: $verificationId", leftMargin, currentY, idPaint)

        // 5. Draw QR Code on right side of footer
        val qrSize = (footerHeight * 0.75f).toInt()
        val qrBitmap = generateQrCodeBitmap(verificationId, qrSize, qrSize)
        if (qrBitmap != null) {
            val qrLeft = width - qrSize - (width * 0.04f)
            val qrTop = photoHeight + (footerHeight - qrSize) / 2f
            canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
        }

        // 6. Compress composite bitmap to JPEG
        val baos = ByteArrayOutputStream()
        compositeBitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
        val rawJpegBytes = baos.toByteArray()

        // 7. Inject Verification ID into JPEG COM Segment (0xFF, 0xFE)
        return embedVerificationIdIntoJpeg(rawJpegBytes, verificationId)
    }

    private fun generateQrCodeBitmap(content: String, width: Int, height: Int): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
            }
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }

    private fun embedVerificationIdIntoJpeg(jpegBytes: ByteArray, verificationId: String): ByteArray {
        if (jpegBytes.size < 4 || jpegBytes[0] != 0xFF.toByte() || jpegBytes[1] != 0xD8.toByte()) {
            return jpegBytes
        }

        val comment = (MARKER_HEADER + verificationId).toByteArray(StandardCharsets.UTF_8)
        val segmentLength = 2 + comment.size

        val baos = ByteArrayOutputStream(jpegBytes.size + segmentLength + 2)
        // Write SOI
        baos.write(0xFF)
        baos.write(0xD8)

        // Write COM marker
        baos.write(0xFF)
        baos.write(0xFE)
        baos.write((segmentLength shr 8) and 0xFF)
        baos.write(segmentLength and 0xFF)
        baos.write(comment)

        // Write remainder of JPEG
        baos.write(jpegBytes, 2, jpegBytes.size - 2)
        return baos.toByteArray()
    }
}
