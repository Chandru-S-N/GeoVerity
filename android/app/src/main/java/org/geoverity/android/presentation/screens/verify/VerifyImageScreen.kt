package org.geoverity.android.presentation.screens.verify

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.geoverity.android.GeoVerityApp
import org.geoverity.android.data.network.RetrofitClient
import org.geoverity.android.data.network.VerificationResponseDto
import org.geoverity.android.presentation.theme.*
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyImageScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val secureStorage = GeoVerityApp.instance.secureStorage

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationResult by remember { mutableStateOf<VerificationResponseDto?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
        verificationResult = null
        errorMsg = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Third-Party Verification", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Slate900)
                        Text("Zero-Login Public Authenticity Check", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate800)
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
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 1. Image Selector Card
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pickerLauncher.launch("image/*") }
                    .shadow(3.dp, RoundedCornerShape(26.dp), spotColor = BrandIndigo.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(IndigoLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(32.dp))
                    }
                    Text(
                        text = if (selectedUri == null) "Select GeoVerity Evidence Photo" else "Image Selected (Tap to change)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "Upload any unmodified JPEG captured via GeoVerity. Cryptographic verification auto-evaluates the byte hash with the authority server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (selectedUri != null) {
                        Box(
                            modifier = Modifier
                                .background(IndigoLight, RoundedCornerShape(50.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Ready for Verification",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandIndigo,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Action Button
            if (selectedUri != null) {
                Button(
                    onClick = {
                        if (isVerifying) return@Button
                        isVerifying = true
                        errorMsg = null

                        coroutineScope.launch {
                            try {
                                val inputStream = context.contentResolver.openInputStream(selectedUri!!)
                                val tempFile = File(context.cacheDir, "verify_temp.jpg")
                                FileOutputStream(tempFile).use { out ->
                                    inputStream?.copyTo(out)
                                }

                                val reqFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                val bodyPart = MultipartBody.Part.createFormData("file", tempFile.name, reqFile)

                                val api = RetrofitClient.getApi(secureStorage.getServerUrl())
                                val res = api.verifyImage(bodyPart)

                                if (res.isSuccessful && res.body() != null) {
                                    verificationResult = res.body()
                                } else {
                                    errorMsg = "Verification request failed: ${res.errorBody()?.string() ?: "Server error"}"
                                }
                            } catch (e: Exception) {
                                errorMsg = "Verification network error: ${e.message}"
                            } finally {
                                isVerifying = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Verifying Cryptographic Ledger...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "VERIFY DIGITAL INTEGRITY", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // 3. Result Card
            verificationResult?.let { res ->
                val isAuthentic = res.status == "AUTHENTIC"

                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isAuthentic) EmeraldLight else RoseLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(26.dp)),
                    border = BorderStroke(1.dp, if (isAuthentic) BrandEmerald.copy(alpha = 0.4f) else BrandRose.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isAuthentic) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isAuthentic) BrandEmerald else BrandRose,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = if (isAuthentic) "AUTHENTIC EVIDENCE" else "NOT AUTHENTIC",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isAuthentic) EmeraldDark else RoseDark
                                )
                                Text(
                                    text = if (isAuthentic) "Validated by GeoVerity Authority Server" else "Cryptographic signature or hash mismatch",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAuthentic) EmeraldDark else RoseDark
                                )
                            }
                        }

                        HorizontalDivider(color = if (isAuthentic) BrandEmerald.copy(alpha = 0.2f) else BrandRose.copy(alpha = 0.2f))

                        if (isAuthentic) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Location & Pincode", style = MaterialTheme.typography.labelSmall, color = EmeraldDark, fontWeight = FontWeight.Bold)
                                    Text(res.location ?: "Unknown Location", style = MaterialTheme.typography.bodySmall, color = EmeraldDark, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("GPS Coordinates", style = MaterialTheme.typography.labelSmall, color = EmeraldDark, fontWeight = FontWeight.Bold)
                                    Text(res.gps ?: "N/A", style = MaterialTheme.typography.bodySmall, color = EmeraldDark)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Server Timestamp", style = MaterialTheme.typography.labelSmall, color = EmeraldDark, fontWeight = FontWeight.Bold)
                                    Text(res.trustedTimestamp ?: "N/A", style = MaterialTheme.typography.bodySmall, color = EmeraldDark)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Digital Signature", style = MaterialTheme.typography.labelSmall, color = EmeraldDark, fontWeight = FontWeight.Bold)
                                    Text("ECDSA NIST P-256 (VALID)", style = MaterialTheme.typography.bodySmall, color = EmeraldDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = res.failureReason ?: "Cryptographic mismatch or image alteration detected.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RoseDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            errorMsg?.let { err ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = RoseLight),
                    border = BorderStroke(1.dp, BrandRose.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = err, color = RoseDark, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
