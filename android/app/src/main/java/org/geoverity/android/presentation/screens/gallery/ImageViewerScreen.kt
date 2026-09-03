package org.geoverity.android.presentation.screens.gallery

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.geoverity.android.GeoVerityApp
import org.geoverity.android.data.db.EvidenceHistoryEntity
import org.geoverity.android.presentation.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    verificationId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = GeoVerityApp.instance.database

    var evidence by remember { mutableStateOf<EvidenceHistoryEntity?>(null) }
    var loadedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(verificationId) {
        withContext(Dispatchers.IO) {
            val rec = db.evidenceHistoryDao().getByVerificationId(verificationId)
            evidence = rec
            val file = resolveViewerImageFile(context, rec?.localImagePath, verificationId)
            if (file != null && file.exists()) {
                loadedBitmap = BitmapFactory.decodeFile(file.absolutePath)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Evidence Viewer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(verificationId.take(16) + "...", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Share Action
                    IconButton(onClick = {
                        val file = resolveViewerImageFile(context, evidence?.localImagePath, verificationId)
                        if (file != null && file.exists()) {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "GeoVerity Evidence $verificationId")
                                putExtra(Intent.EXTRA_TEXT, "GeoVerity Authenticated Digital Evidence\nVerification ID: $verificationId\nLocation: ${evidence?.locationName}\n(Share as Document/File to preserve exact cryptographic bytes).")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Original Evidence File"))
                        }
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = BrandEmerald)
                    }

                    // Delete Action
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = BrandRose)
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Fullscreen Preview Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    loadedBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Full High-Res Evidence",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(Slate100, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandPrimary)
                    }
                }
            }

            // 2. Cryptographic Proof Specifications Drawer
            evidence?.let { ev ->
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
                        Text(
                            text = "Authoritative Cryptographic Proof",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        HorizontalDivider(color = Slate100)

                        ProofField(label = "Verification ID", value = ev.verificationId, isMonospace = true, isHighlighted = true)
                        ProofField(label = "Detailed Location", value = ev.locationName)
                        ProofField(label = "GPS Coordinates", value = String.format(Locale.US, "%.6f, %.6f", ev.latitude, ev.longitude))
                        ProofField(
                            label = "Authoritative Timestamp",
                            value = SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a (z)", Locale.US).format(Date(ev.trustedTimestamp))
                        )
                        ProofField(label = "Composite SHA-256", value = ev.sha256Hash, isMonospace = true)
                        ProofField(
                            label = "Digital Signature Status",
                            value = if (ev.signatureStatus == "VALID") "VALID (ECDSA NIST P-256 Verified)" else "PENDING_SYNC (Offline Queue)",
                            isSuccess = ev.signatureStatus == "VALID"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && evidence != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = BrandRose, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = "Delete Local Photograph?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This will delete the high-resolution photograph file from your mobile device memory.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                    Text(
                        text = "The server's authoritative cryptographic proof, SHA-256 composite hash, and ECDSA signature will remain permanently intact on the authority server.",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandIndigo,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val file = resolveViewerImageFile(context, evidence?.localImagePath, verificationId)
                            if (file != null && file.exists()) {
                                file.delete()
                            }
                            db.evidenceHistoryDao().markLocalDeleted(verificationId)
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose)
                ) {
                    Text("Delete from Device", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Slate600)
                }
            },
            containerColor = WhiteBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

private fun resolveViewerImageFile(context: android.content.Context, localImagePath: String?, verificationId: String): File? {
    if (localImagePath != null) {
        val f = File(localImagePath)
        if (f.exists()) return f
    }
    val internalFile = File(context.filesDir, "$verificationId.jpg")
    if (internalFile.exists()) return internalFile

    val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    if (picturesDir != null) {
        val externalFile = File(picturesDir, "$verificationId.jpg")
        if (externalFile.exists()) return externalFile
    }
    return internalFile
}

@Composable
private fun ProofField(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    isHighlighted: Boolean = false,
    isSuccess: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate500, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            style = if (isMonospace) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
            color = when {
                isSuccess -> EmeraldDark
                isHighlighted -> BrandIndigo
                else -> Slate900
            },
            fontWeight = if (isHighlighted || isSuccess) FontWeight.Bold else FontWeight.Medium,
            fontFamily = if (isMonospace) androidx.compose.ui.text.font.FontFamily.Monospace else null
        )
    }
}
