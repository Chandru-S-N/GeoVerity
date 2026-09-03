package org.geoverity.android.data.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

data class TimeTokenRequestDto(
    val deviceId: String,
    val clientTimestamp: Long
)

data class TimeTokenResponseDto(
    val serverTime: Long,
    val token: String,
    val expiresAt: Long,
    val toleranceMs: Long
)

data class CanonicalMetadataRequestDto(
    val appVersion: String,
    val deviceId: String,
    val latitude: Double,
    val locationName: String,
    val longitude: Double,
    val trustedTimestamp: Long,
    val verificationId: String
)

data class CaptureRequestDto(
    val verificationId: String,
    val timeToken: String,
    val canonicalMetadata: CanonicalMetadataRequestDto,
    val sha256Hash: String
)

data class OfflineSyncRequestDto(
    val verificationId: String,
    val canonicalMetadata: CanonicalMetadataRequestDto,
    val sha256Hash: String,
    val lastTrustedServerTimestamp: Long,
    val lastTrustedElapsedRealtime: Long,
    val captureElapsedRealtime: Long,
    val deviceCaptureTime: Long
)

data class CaptureResponseDto(
    val verificationId: String,
    val status: String,
    val trustedTimestamp: String,
    val sha256: String,
    val signatureStatus: String,
    val ecdsaSignature: String
)

data class VerificationResponseDto(
    val verificationId: String?,
    val status: String,
    val signatureValid: Boolean,
    val hashMatched: Boolean,
    val location: String?,
    val gps: String?,
    val trustedTimestamp: String?,
    val deviceId: String?,
    val sha256Hash: String?,
    val failureReason: String?,
    val verificationSteps: List<String>?
)

interface GeoVerityApi {

    @POST("/api/v1/time/token")
    suspend fun getTimeToken(
        @Header("X-API-Key") apiKey: String,
        @Body request: TimeTokenRequestDto
    ): Response<TimeTokenResponseDto>

    @POST("/api/v1/capture")
    suspend fun authenticateCapture(
        @Header("X-API-Key") apiKey: String,
        @Body request: CaptureRequestDto
    ): Response<CaptureResponseDto>

    @POST("/api/v1/capture/offline-sync")
    suspend fun authenticateOfflineSync(
        @Header("X-API-Key") apiKey: String,
        @Body request: OfflineSyncRequestDto
    ): Response<CaptureResponseDto>

    @Multipart
    @POST("/api/v1/verify")
    suspend fun verifyImage(
        @Part file: MultipartBody.Part
    ): Response<VerificationResponseDto>
}
