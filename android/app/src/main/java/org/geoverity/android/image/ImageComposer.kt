package org.geoverity.android.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
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
     * 1. Keeps original photo area clean on top (untouched & uncompressed).
     * 2. Renders dedicated metadata footer at bottom with:
     *    - Detailed location & pincode
     *    - GPS Coordinates
     *    - Date on one line
     *    - Time on the next line
     *    - Verification ID
     * 3. Renders dedicated QR code in the footer with high-contrast white container.
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
        // Allocate generous footer height (at least 320px or 27% of photo) to fit separate Date and Time lines
        val footerHeight = (photoHeight * 0.27f).toInt().coerceAtLeast(320)
        val totalHeight = photoHeight + footerHeight

        val compositeBitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(compositeBitmap)

        // 1. Draw original photo at top (unobstructed)
        canvas.drawBitmap(photoBitmap, 0f, 0f, null)

        // 2. Draw dedicated footer background at bottom (Sleek Slate 950)
        val footerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, photoHeight.toFloat(), width.toFloat(), totalHeight.toFloat(), footerPaint)

        // Top border accent line on footer (Vibrant Indigo accent)
        val accentLinePaint = Paint().apply {
            color = Color.rgb(99, 102, 241) // Indigo 500
            strokeWidth = (footerHeight * 0.015f).coerceAtLeast(4f)
        }
        canvas.drawLine(0f, photoHeight.toFloat(), width.toFloat(), photoHeight.toFloat(), accentLinePaint)

        // 3. Format Date & Time strictly on separate lines from trusted timestamp
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm:ss a (z)", Locale.US)
        val dateStr = dateFormat.format(Date(trustedTimestamp))
        val timeStr = timeFormat.format(Date(trustedTimestamp))

        // 4. Calculate responsive typography sizes
        val baseTextSize = (footerHeight * 0.078f).coerceAtLeast(18f)
        val lineSpacing = baseTextSize * 1.40f
        val leftMargin = width * 0.04f
        val qrSize = (footerHeight * 0.72f).toInt()
        val textMaxRight = width - qrSize - (width * 0.08f)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = baseTextSize * 1.15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225) // Slate 300
            textSize = baseTextSize
            typeface = Typeface.DEFAULT
        }

        val idPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(129, 140, 248) // Indigo 400
            textSize = baseTextSize * 1.05f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var currentY = photoHeight + (footerHeight * 0.15f)

        // Line 1: Detailed Location with Pincode
        val locationLines = wrapText("📍 Location: $locationName", titlePaint, textMaxRight - leftMargin)
        for (line in locationLines.take(2)) {
            canvas.drawText(line, leftMargin, currentY, titlePaint)
            currentY += lineSpacing
        }

        // Line 2: GPS Coordinates
        canvas.drawText(String.format(Locale.US, "🌐 GPS: %.6f, %.6f", latitude, longitude), leftMargin, currentY, textPaint)
        currentY += lineSpacing

        // Line 3: Date in ONE line
        canvas.drawText("📅 Date: $dateStr", leftMargin, currentY, textPaint)
        currentY += lineSpacing

        // Line 4: Time in the NEXT line
        canvas.drawText("⏰ Time: $timeStr", leftMargin, currentY, textPaint)
        currentY += lineSpacing

        // Line 5: Digital Evidence Status (Clean, no explicit ID)
        canvas.drawText("🛡️ Authenticated Digital Evidence", leftMargin, currentY, idPaint)

        // 5. Draw QR Code on right side of footer with white rounded background
        val qrBitmap = generateQrCodeBitmap(verificationId, qrSize, qrSize)
        if (qrBitmap != null) {
            val qrLeft = width - qrSize - (width * 0.04f)
            val qrTop = photoHeight + (footerHeight - qrSize) / 2f

            // White container background for maximum contrast
            val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val padding = 12f
            canvas.drawRoundRect(
                RectF(qrLeft - padding, qrTop - padding, qrLeft + qrSize + padding, qrTop + qrSize + padding),
                16f,
                16f,
                qrBgPaint
            )

            canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
        }

        // 6. Compress composite bitmap to JPEG (high quality 94)
        val baos = ByteArrayOutputStream()
        compositeBitmap.compress(Bitmap.CompressFormat.JPEG, 94, baos)
        val rawJpegBytes = baos.toByteArray()

        // 7. Inject Verification ID into JPEG COM Segment (0xFF, 0xFE)
        return embedVerificationIdIntoJpeg(rawJpegBytes, verificationId)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
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
