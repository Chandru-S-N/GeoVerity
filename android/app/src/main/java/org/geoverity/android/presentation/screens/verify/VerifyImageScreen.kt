package org.geoverity.android.presentation.screens.verify

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                title = { Text("Third-Party Verification", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
            
            // 1. Image Selector Box
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pickerLauncher.launch("image/*") }
                    .shadow(2.dp, RoundedCornerShape(24.dp)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(IndigoLight, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = if (selectedUri == null) "Select GeoVerity Evidence Image" else "Image Selected (Click to change)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "No login or separate ID required. Auto-extracts verification marker.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500
                    )
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
                                    errorMsg = "Verification request failed: ${res.errorBody()?.string()}"
                                }
                            } catch (e: Exception) {
                                errorMsg = "Verification network error: ${e.message}"
                            } finally {
                                isVerifying = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(text = "VERIFY DIGITAL INTEGRITY", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. Result Card (Clean, without raw verification ID text)
            verificationResult?.let { res ->
                val isAuthentic = res.status == "AUTHENTIC"

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isAuthentic) EmeraldLight else RoseLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(24.dp)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isAuthentic) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isAuthentic) BrandEmerald else BrandRose,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = if (isAuthentic) "AUTHENTIC EVIDENCE" else "NOT AUTHENTIC",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAuthentic) EmeraldDark else RoseDark
                            )
                        }

                        if (isAuthentic) {
                            Text(text = "Location: ${res.location}", style = MaterialTheme.typography.bodyMedium, color = EmeraldDark)
                            Text(text = "GPS: ${res.gps}", style = MaterialTheme.typography.bodyMedium, color = EmeraldDark)
                            Text(text = "Attestation: Validated by Server Authority", style = MaterialTheme.typography.labelSmall, color = EmeraldDark, fontWeight = FontWeight.Bold)
                            Text(text = "Digital Signature: VALID (ECDSA NIST P-256)", style = MaterialTheme.typography.labelSmall, color = EmeraldDark)
                            Text(text = "Composite SHA-256: MATCHED", style = MaterialTheme.typography.labelSmall, color = EmeraldDark)
                        } else {
                            Text(
                                text = res.failureReason ?: "Cryptographic mismatch or image alteration detected.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RoseDark
                            )
                        }
                    }
                }
            }

            errorMsg?.let { err ->
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
