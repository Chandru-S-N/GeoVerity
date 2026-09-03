package org.geoverity.android.presentation.screens.capture

import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import java.text.SimpleDateFormat
import java.util.*

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
    var authStepMessage by remember { mutableStateOf("Initializing cryptographic engine...") }
    var isProcessing by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Automatic Authentication Pipeline Triggered Immediately Upon Taking Image
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                authStepMessage = "Composing evidence with detailed location & pincode..."
                val rawBitmap = BitmapFactory.decodeFile(rawPhotoPath) ?: throw IllegalStateException("Failed to decode raw capture")

                val api = RetrofitClient.getApi(secureStorage.getServerUrl())
                val apiKey = secureStorage.getApiKey()
                val deviceId = secureStorage.getDeviceId()

                // Step 1: Obtain Trusted Authoritative Server Time Token
                authStepMessage = "Obtaining authoritative server timestamp..."
                val timeRes = try {
                    api.getTimeToken(apiKey, TimeTokenRequestDto(deviceId, System.currentTimeMillis()))
                } catch (e: Exception) {
                    null
                }

                if (timeRes != null && timeRes.isSuccessful && timeRes.body() != null) {
                    val tokenBody = timeRes.body()!!
                    val trustedServerTime = tokenBody.serverTime
                    val timeToken = tokenBody.token

                    // Update baseline for monotonic offline reconciliation
                    secureStorage.saveTrustedTimeSync(trustedServerTime, SystemClock.elapsedRealtime())

                    // Step 2: Compose final image with separate Date and Time lines
                    authStepMessage = "Rendering separate Date & Time lines and QR identifier..."
                    val finalImageBytes = ImageComposer.composeFinalImageBytes(
                        photoBitmap = rawBitmap,
                        locationName = locationName,
                        latitude = latitude,
                        longitude = longitude,
                        trustedTimestamp = trustedServerTime,
                        verificationId = verificationId
                    )
                    composedFinalBytes = finalImageBytes

                    // Step 3: Serialize Canonical Metadata
                    authStepMessage = "Serializing canonical metadata & binding composite SHA-256..."
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

                    // Step 4: Calculate Composite SHA-256 Hash
                    val sha256 = Sha256Hasher.calculateCompositeHash(finalImageBytes, canonicalBytes)

                    // Step 5: Transmit to Backend for Server-Authority ECDSA P-256 Signing
                    authStepMessage = "Requesting Server ECDSA P-256 digital signature..."
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
                        // Step 6: Save original authenticated image to local device storage (for Gallery)
                        authStepMessage = "Saving image to local device storage..."
                        val savedFile = File(context.filesDir, "$verificationId.jpg")
                        FileOutputStream(savedFile).use { it.write(finalImageBytes) }

                        // Step 7: Inject Verification ID into standard EXIF headers as well
                        try {
                            val exif = ExifInterface(savedFile.absolutePath)
                            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "GeoVerity Authenticated Digital Evidence: $verificationId")
                            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "GEOVERITY_ID:$verificationId; LOCATION:$locationName; TRUSTED_EPOCH:$trustedServerTime")
                            exif.setAttribute(ExifInterface.TAG_DATETIME, SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date(trustedServerTime)))
                            exif.saveAttributes()
                        } catch (e: Exception) {
                            // EXIF tag injection is supplementary; COM marker and footer already embedded
                        }

                        // Step 8: Persist to Room Database
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

                        authStepMessage = "Authentication Complete!"
                        delay(600)
                        withContext(Dispatchers.Main) {
                            onAuthenticationComplete(verificationId)
                        }
                    } else {
                        errorMessage = "Server rejected authentication: ${captureRes.errorBody()?.string()}"
                        isProcessing = false
                    }
                } else {
                    // Fallback: Offline Temporary Storage with Keystore AES-256-GCM
                    authStepMessage = "Offline Mode: Encrypting raw capture with Keystore AES-256-GCM..."
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

                    authStepMessage = "Encrypted locally. Auto-sync will run when internet connects."
                    delay(800)
                    withContext(Dispatchers.Main) {
                        onAuthenticationComplete(verificationId)
                    }
                }
            } catch (e: Exception) {
                // Offline fallback on network error
                try {
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
                    delay(800)
                    withContext(Dispatchers.Main) {
                        onAuthenticationComplete(verificationId)
                    }
                } catch (ex: Exception) {
                    errorMessage = "Error during capture processing: ${e.message}"
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authenticating Evidence", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            
            // 1. Live Authentication Progress HUD
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(IndigoLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = BrandPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    } else if (errorMessage == null) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(EmeraldLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(36.dp))
                        }
                    }

                    Text(
                        text = "Automatic Digital Authentication",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Text(
                        text = authStepMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (errorMessage != null) BrandRose else Slate600,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // 2. Composed Image Preview
            composedFinalBytes?.let { bytes ->
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(24.dp)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Composed Evidence Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    }
                }
            }

            // 3. Metadata Specifications Summary Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier.fillMaxWidth(),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Captured Evidence Metadata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Verification ID", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        Text(text = verificationId.take(16) + "...", style = MaterialTheme.typography.bodySmall, color = BrandIndigo, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Location & Pincode", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        Text(text = locationName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(max = 200.dp))
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "GPS Coordinates", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        Text(text = String.format(Locale.US, "%.6f, %.6f", latitude, longitude), style = MaterialTheme.typography.bodySmall)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Storage Target", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        Text(text = "Local Device Gallery (Server Stores Crypto Proof)", style = MaterialTheme.typography.bodySmall, color = BrandEmerald, fontWeight = FontWeight.Bold)
                    }
                }
            }

            errorMessage?.let { err ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RoseLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = err, color = RoseDark, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
