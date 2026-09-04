package org.geoverity.android.image

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ImageSaver {
    private const val TAG = "ImageSaver"

    /**
     * Saves captured JPEG image bytes to the phone's public MediaStore and Gallery
     * so it immediately appears in Google Photos, Gallery apps, and File Managers.
     */
    fun saveToGallery(
        context: Context,
        imageBytes: ByteArray,
        verificationId: String,
        locationName: String = "",
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        trustedTimestamp: Long = System.currentTimeMillis(),
        prefix: String = "GeoVerity"
    ): Uri? {
        val fileName = "${prefix}_${verificationId}.jpg"
        var resultUri: Uri? = null

        // 1. Save internal cache file in context.filesDir
        val internalFile = File(context.filesDir, "$verificationId.jpg")
        try {
            FileOutputStream(internalFile).use { it.write(imageBytes) }
            injectExifMetadata(internalFile.absolutePath, verificationId, locationName, latitude, longitude, trustedTimestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing internal cache file: ${e.message}")
        }

        // 2. Primary: MediaStore Scoped Storage insert for Android 10+ (Q+)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/GeoVerity")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(imageBytes)
                        outputStream.flush()
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    Log.i(TAG, "Image saved via MediaStore: $uri")
                    resultUri = uri
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore insert error: ${e.message}", e)
        }

        // 3. Direct Public Storage write (Pictures/GeoVerity)
        try {
            val publicPicturesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "GeoVerity"
            )
            if (!publicPicturesDir.exists()) publicPicturesDir.mkdirs()

            val publicFile = File(publicPicturesDir, fileName)
            FileOutputStream(publicFile).use { it.write(imageBytes) }
            injectExifMetadata(publicFile.absolutePath, verificationId, locationName, latitude, longitude, trustedTimestamp)

            // Trigger immediate media scanner index
            MediaScannerConnection.scanFile(
                context,
                arrayOf(publicFile.absolutePath),
                arrayOf("image/jpeg")
            ) { path, scannedUri ->
                Log.i(TAG, "Indexed public picture: $path -> $scannedUri")
                if (resultUri == null) resultUri = scannedUri
            }

            // Broadcast scan intent
            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                data = Uri.fromFile(publicFile)
            }
            context.sendBroadcast(scanIntent)

        } catch (e: Exception) {
            Log.e(TAG, "Public storage write error: ${e.message}", e)
        }

        // 4. Fallback: App External Storage directory
        try {
            val appPicturesDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir,
                "GeoVerity"
            )
            if (!appPicturesDir.exists()) appPicturesDir.mkdirs()

            val appFile = File(appPicturesDir, fileName)
            FileOutputStream(appFile).use { it.write(imageBytes) }
            injectExifMetadata(appFile.absolutePath, verificationId, locationName, latitude, longitude, trustedTimestamp)

            MediaScannerConnection.scanFile(
                context,
                arrayOf(appFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
        } catch (e: Exception) {
            Log.e(TAG, "App external storage error: ${e.message}", e)
        }

        // 5. Toast feedback on Main Thread
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(context, "📸 Saved to Local Device (Pictures/GeoVerity)", Toast.LENGTH_SHORT).show()
            } catch (ignored: Exception) {}
        }

        return resultUri
    }

    private fun injectExifMetadata(
        filePath: String,
        verificationId: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        timestamp: Long
    ) {
        try {
            val exif = ExifInterface(filePath)
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "GeoVerity Authenticated Digital Evidence: $verificationId")
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "{\"verificationId\":\"$verificationId\",\"location\":\"$locationName\",\"timestamp\":$timestamp}")
            exif.setAttribute(ExifInterface.TAG_DATETIME, SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date(timestamp)))
            
            if (latitude != 0.0 || longitude != 0.0) {
                val latRef = if (latitude >= 0) "N" else "S"
                val lonRef = if (longitude >= 0) "E" else "W"
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latRef)
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, decimalToDms(Math.abs(latitude)))
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lonRef)
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, decimalToDms(Math.abs(longitude)))
            }
            
            exif.saveAttributes()
        } catch (e: Exception) {
            Log.w(TAG, "Could not inject EXIF metadata: ${e.message}")
        }
    }

    private fun decimalToDms(coordinate: Double): String {
        val degrees = coordinate.toInt()
        val minutesDouble = (coordinate - degrees) * 60.0
        val minutes = minutesDouble.toInt()
        val seconds = ((minutesDouble - minutes) * 60.0 * 1000).toInt()
        return "$degrees/1,$minutes/1,$seconds/1000"
    }
}
