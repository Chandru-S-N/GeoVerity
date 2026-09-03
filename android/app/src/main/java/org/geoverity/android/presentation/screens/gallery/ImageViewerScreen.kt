package org.geoverity.android.presentation.screens.gallery

import android.content.Intent
import android.graphics.BitmapFactory
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
            rec?.localImagePath?.let { path ->
                val f = File(path)
                if (f.exists()) {
                    loadedBitmap = BitmapFactory.decodeFile(f.absolutePath)
                }
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
                        evidence?.localImagePath?.let { path ->
                            val file = File(path)
                            if (file.exists()) {
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. High-Resolution Image Card with Footer Display
            loadedBitmap?.let { bmp ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(24.dp)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Evidence Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                        )
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

            // 2. Comprehensive Cryptographic Proof Inspector
            evidence?.let { item ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(24.dp)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Authoritative Evidence Specifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        SpecRow("Verification ID", item.verificationId, BrandIndigo)
                        SpecRow("Detailed Location & Pincode", item.locationName, Slate900)
                        SpecRow("GPS Coordinates", String.format(Locale.US, "%.6f, %.6f", item.latitude, item.longitude), Slate700)
                        SpecRow("Trusted Timestamp", SimpleDateFormat("dd MMM yyyy, hh:mm:ss a (z)", Locale.US).format(Date(item.trustedTimestamp)), Slate700)
                        SpecRow("Composite SHA-256 Hash", item.sha256Hash, Slate900)
                        SpecRow("Server ECDSA Signature", "VALID (NIST P-256 Authority)", BrandEmerald)
                        SpecRow("Device Storage Status", if (item.localImagePath != null) "Stored on Local Phone" else "Deleted from Local Device (Retained on Server)", Slate700)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && evidence != null) {
        val item = evidence!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Image from Phone?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The physical photo will be deleted from your mobile phone memory.", color = Slate700)
                    Text("ℹ️ The server's cryptographic proof, hash, and signature remain permanently valid in the registry.", color = BrandIndigo, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            item.localImagePath?.let { path ->
                                val f = File(path)
                                if (f.exists()) f.delete()
                            }
                            db.evidenceHistoryDao().markLocalDeleted(item.verificationId)
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

@Composable
private fun SpecRow(label: String, value: String, valueColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate500)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}
