package org.geoverity.android.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "offline_captures",
    indices = [Index(value = ["verificationId"], unique = true)]
)
data class OfflineCaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verificationId: String,
    val encryptedImageData: ByteArray,
    val lastTrustedServerTimestamp: Long,
    val lastTrustedElapsedRealtime: Long,
    val captureElapsedRealtime: Long,
    val deviceCaptureTime: Long,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val status: String = "PENDING", // PENDING, REJECTED_ANOMALY, SYNCED
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "evidence_history",
    indices = [Index(value = ["verificationId"], unique = true)]
)
data class EvidenceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verificationId: String,
    val sha256Hash: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val trustedTimestamp: Long,
    val signatureStatus: String = "VALID",
    val localImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
