package org.geoverity.android.presentation.screens.capture

import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.geoverity.android.GeoVerityApp
import org.geoverity.android.crypto.*
import org.geoverity.android.data.db.EvidenceHistoryEntity
import org.geoverity.android.data.db.OfflineCaptureEntity
import org.geoverity.android.data.network.*
import org.geoverity.android.image.ImageComposer
import org.geoverity.android.presentation.theme.*
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturePreviewScreen(
    rawPhotoPath: String,
    locationName: String,
    latitude: Double,
    longitude: Double,
    onAuthenticationComplete: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val secureStorage = GeoVerityApp.instance.secureStorage
    val db = GeoVerityApp.instance.database
    val keyStoreManager = remember { AndroidKeyStoreManager() }

    val verificationId = remember { "SGA-" + UUID.randomUUID().toString().uppercase() }
    var composedFinalBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Initial composition of preview image with dedicated footer
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val rawBitmap = BitmapFactory.decodeFile(rawPhotoPath)
            if (rawBitmap != null) {
                val finalBytes = ImageComposer.composeFinalImageBytes(
                    photoBitmap = rawBitmap,
                    locationName = locationName,
                    latitude = latitude,
                    longitude = longitude,
                    trustedTimestamp = System.currentTimeMillis(),
                    verificationId = verificationId
                )
                composedFinalBytes = finalBytes
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evidence Composition", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WhiteBackground)
            )
        },
        containerColor = Slate50
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            
            // 1. Image Preview with Dedicated Metadata Footer Card
            composedFinalBytes?.let { bytes ->
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(24.dp)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Composed Evidence",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    }
                }
            } ?: Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Slate100, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimary)
            }

            // 2. Metadata Inspection Card (White Card with Colorful Accents)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier.fillMaxWidth(),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Evidence Identity & Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Verification ID", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        Text(text = verificationId.take(14) + "...", style = MaterialTheme.typography.labelSmall, color = BrandIndigo, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Location", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        Text(text = locationName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "GPS Coordinates", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        Text(text = String.format("%.6f, %.6f", latitude, longitude), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Error banner if any
            errorMessage?.let { err ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RoseLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = err, color = RoseDark, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                }
            }

            // 3. Authenticate Action CTA
            Button(
                onClick = {
                    if (isSubmitting || composedFinalBytes == null) return@Button
                    isSubmitting = true
                    errorMessage = null

                    coroutineScope.launch {
                        try {
                            val api = RetrofitClient.getApi(secureStorage.getServerUrl())
                            val apiKey = secureStorage.getApiKey()
                            val deviceId = secureStorage.getDeviceId()

                            // Step 1: Request Trusted Server Time Token
                            val timeRes = api.getTimeToken(
                                apiKey,
                                TimeTokenRequestDto(deviceId, System.currentTimeMillis())
                            )

                            if (timeRes.isSuccessful && timeRes.body() != null) {
                                val tokenBody = timeRes.body()!!
                                val trustedServerTime = tokenBody.serverTime
                                val timeToken = tokenBody.token

                                // Update local time sync reference
                                secureStorage.saveTrustedTimeSync(trustedServerTime, SystemClock.elapsedRealtime())

                                // Re-compose final image with authoritative server timestamp
                                val rawBitmap = BitmapFactory.decodeFile(rawPhotoPath)
                                val finalImageBytes = ImageComposer.composeFinalImageBytes(
                                    photoBitmap = rawBitmap,
                                    locationName = locationName,
                                    latitude = latitude,
                                    longitude = longitude,
                                    trustedTimestamp = trustedServerTime,
                                    verificationId = verificationId
                                )

                                // Serialize Canonical Metadata
                                val metadata = CanonicalMetadata(
                                    appVersion = "1.0.0",
                                    deviceId = deviceId,
                                    latitude = latitude,
                                    locationName = locationName,
                                    longitude = longitude,
                                    trustedTimestamp = trustedServerTime,
                                    verificationId = verificationId
                                )
                                val canonicalBytes = CanonicalMetadataSerializer.serializeToCanonicalBytes(metadata)

                                // Compute Composite SHA-256 over: finalImageBytes + canonicalBytes
                                val sha256 = Sha256Hasher.calculateCompositeHash(finalImageBytes, canonicalBytes)

                                // Transmit to Backend for ECDSA P-256 Signing & Storage
                                val captureRes = api.authenticateCapture(
                                    apiKey,
                                    CaptureRequestDto(
                                        verificationId = verificationId,
                                        timeToken = timeToken,
                                        canonicalMetadata = CanonicalMetadataRequestDto(
                                            appVersion = metadata.appVersion,
                                            deviceId = metadata.deviceId,
                                            latitude = metadata.latitude,
                                            locationName = metadata.locationName,
                                            longitude = metadata.longitude,
                                            trustedTimestamp = metadata.trustedTimestamp,
                                            verificationId = metadata.verificationId
                                        ),
                                        sha256Hash = sha256
                                    )
                                )

                                if (captureRes.isSuccessful && captureRes.body()?.status == "AUTHENTICATED") {
                                    // Save original authenticated file to private app storage
                                    val savedFile = File(context.filesDir, "$verificationId.jpg")
                                    FileOutputStream(savedFile).use { it.write(finalImageBytes) }

                                    // Save to local evidence history
                                    db.evidenceHistoryDao().insert(
                                        EvidenceHistoryEntity(
                                            verificationId = verificationId,
                                            sha256Hash = sha256,
                                            locationName = locationName,
                                            latitude = latitude,
                                            longitude = longitude,
                                            trustedTimestamp = trustedServerTime,
                                            signatureStatus = "VALID",
                                            localImagePath = savedFile.absolutePath
                                        )
                                    )

                                    onAuthenticationComplete(verificationId)
                                } else {
                                    errorMessage = "Server authentication rejected: ${captureRes.errorBody()?.string()}"
                                }
                            } else {
                                // Fallback: Offline Temporary Storage with Keystore AES-256-GCM
                                val rawBytes = File(rawPhotoPath).readBytes()
                                val encryptedBytes = keyStoreManager.encrypt(rawBytes)

                                db.offlineCaptureDao().insert(
                                    OfflineCaptureEntity(
                                        verificationId = verificationId,
                                        encryptedImageData = encryptedBytes,
                                        lastTrustedServerTimestamp = secureStorage.getLastTrustedServerTimestamp().takeIf { it > 0 } ?: System.currentTimeMillis(),
                                        lastTrustedElapsedRealtime = secureStorage.getLastTrustedElapsedRealtime().takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
                                        captureElapsedRealtime = SystemClock.elapsedRealtime(),
                                        deviceCaptureTime = System.currentTimeMillis(),
                                        locationName = locationName,
                                        latitude = latitude,
                                        longitude = longitude,
                                        status = "PENDING"
                                    )
                                )

                                onAuthenticationComplete(verificationId)
                            }
                        } catch (e: Exception) {
                            // Offline fallback on network error
                            val rawBytes = File(rawPhotoPath).readBytes()
                            val encryptedBytes = keyStoreManager.encrypt(rawBytes)

                            db.offlineCaptureDao().insert(
                                OfflineCaptureEntity(
                                    verificationId = verificationId,
                                    encryptedImageData = encryptedBytes,
                                    lastTrustedServerTimestamp = secureStorage.getLastTrustedServerTimestamp().takeIf { it > 0 } ?: System.currentTimeMillis(),
                                    lastTrustedElapsedRealtime = secureStorage.getLastTrustedElapsedRealtime().takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
                                    captureElapsedRealtime = SystemClock.elapsedRealtime(),
                                    deviceCaptureTime = System.currentTimeMillis(),
                                    locationName = locationName,
                                    latitude = latitude,
                                    longitude = longitude,
                                    status = "PENDING"
                                )
                            )

                            onAuthenticationComplete(verificationId)
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = BrandPrimary.copy(alpha = 0.3f))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("AUTHENTICATE DIGITAL EVIDENCE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
