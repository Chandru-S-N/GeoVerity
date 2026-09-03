package org.geoverity.android.offline

import android.content.Context
import android.graphics.BitmapFactory
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.geoverity.android.GeoVerityApp
import org.geoverity.android.crypto.AndroidKeyStoreManager
import org.geoverity.android.crypto.CanonicalMetadata
import org.geoverity.android.crypto.CanonicalMetadataSerializer
import org.geoverity.android.crypto.Sha256Hasher
import org.geoverity.android.data.db.EvidenceHistoryEntity
import org.geoverity.android.data.network.CanonicalMetadataRequestDto
import org.geoverity.android.data.network.OfflineSyncRequestDto
import org.geoverity.android.data.network.RetrofitClient
import org.geoverity.android.image.ImageComposer
import java.io.File
import java.io.FileOutputStream

class OfflineSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = GeoVerityApp.instance.database
    private val secureStorage = GeoVerityApp.instance.secureStorage
    private val keyStoreManager = AndroidKeyStoreManager()

    override suspend fun doWork(): Result {
        val pendingCaptures = db.offlineCaptureDao().getPendingCaptures()
        if (pendingCaptures.isEmpty()) {
            return Result.success()
        }

        val api = RetrofitClient.getApi(secureStorage.getServerUrl())
        val apiKey = secureStorage.getApiKey()

        for (capture in pendingCaptures) {
            try {
                // 1. Monotonic Time Reconciliation Check
                val reconciliation = TimeAnomalyDetector.reconcile(
                    lastTrustedServerTimestamp = capture.lastTrustedServerTimestamp,
                    lastTrustedElapsedRealtime = capture.lastTrustedElapsedRealtime,
                    captureElapsedRealtime = capture.captureElapsedRealtime,
                    deviceCaptureTime = capture.deviceCaptureTime
                )

                if (reconciliation is TimeReconciliationResult.Anomaly) {
                    // Clock manipulation detected! Mark as REJECTED and do not sign
                    db.offlineCaptureDao().update(
                        capture.copy(
                            status = "REJECTED_ANOMALY",
                            rejectionReason = reconciliation.reason
                        )
                    )
                    continue
                }

                val validReconciliation = reconciliation as TimeReconciliationResult.Valid
                val authoritativeTimestamp = validReconciliation.reconciledTimestamp

                // 2. Decrypt temporary image bytes using Keystore
                val decryptedRawBytes = keyStoreManager.decrypt(capture.encryptedImageData)
                val photoBitmap = BitmapFactory.decodeByteArray(decryptedRawBytes, 0, decryptedRawBytes.size)

                // 3. Compose final image with dedicated metadata footer and QR code
                val finalImageBytes = ImageComposer.composeFinalImageBytes(
                    photoBitmap = photoBitmap,
                    locationName = capture.locationName,
                    latitude = capture.latitude,
                    longitude = capture.longitude,
                    trustedTimestamp = authoritativeTimestamp,
                    verificationId = capture.verificationId
                )

                // 4. Serialize canonical metadata
                val canonicalMetadata = CanonicalMetadata(
                    appVersion = "1.0.0",
                    deviceId = secureStorage.getDeviceId(),
                    latitude = capture.latitude,
                    locationName = capture.locationName,
                    longitude = capture.longitude,
                    trustedTimestamp = authoritativeTimestamp,
                    verificationId = capture.verificationId
                )
                val canonicalBytes = CanonicalMetadataSerializer.serializeToCanonicalBytes(canonicalMetadata)

                // 5. Compute composite SHA-256 over: finalImageBytes + canonicalBytes
                val sha256Hash = Sha256Hasher.calculateCompositeHash(finalImageBytes, canonicalBytes)

                // 6. Transmit offline reconciliation payload to server
                val syncRequest = OfflineSyncRequestDto(
                    verificationId = capture.verificationId,
                    canonicalMetadata = CanonicalMetadataRequestDto(
                        appVersion = canonicalMetadata.appVersion,
                        deviceId = canonicalMetadata.deviceId,
                        latitude = canonicalMetadata.latitude,
                        locationName = canonicalMetadata.locationName,
                        longitude = canonicalMetadata.longitude,
                        trustedTimestamp = canonicalMetadata.trustedTimestamp,
                        verificationId = canonicalMetadata.verificationId
                    ),
                    sha256Hash = sha256Hash,
                    lastTrustedServerTimestamp = capture.lastTrustedServerTimestamp,
                    lastTrustedElapsedRealtime = capture.lastTrustedElapsedRealtime,
                    captureElapsedRealtime = capture.captureElapsedRealtime,
                    deviceCaptureTime = capture.deviceCaptureTime
                )

                val response = api.authenticateOfflineSync(apiKey, syncRequest)
                if (response.isSuccessful && response.body()?.status == "AUTHENTICATED") {
                    // Save final authenticated JPEG to local device storage for Gallery display
                    val savedFile = File(context.filesDir, "${capture.verificationId}.jpg")
                    FileOutputStream(savedFile).use { it.write(finalImageBytes) }

                    // Persist to Evidence History and Gallery
                    db.evidenceHistoryDao().insert(
                        EvidenceHistoryEntity(
                            verificationId = capture.verificationId,
                            sha256Hash = sha256Hash,
                            locationName = capture.locationName,
                            latitude = capture.latitude,
                            longitude = capture.longitude,
                            trustedTimestamp = authoritativeTimestamp,
                            signatureStatus = "VALID",
                            localImagePath = savedFile.absolutePath
                        )
                    )
                    // Delete temporary offline record
                    db.offlineCaptureDao().delete(capture)
                }
            } catch (e: Exception) {
                // Network failed or server temporarily unreachable; will retry automatically
            }
        }

        return Result.success()
    }
}
