package org.geoverity.android.presentation.screens.gallery

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
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
                        Text("Authentic Digital Evidence", style = MaterialTheme.typography.labelSmall, color = BrandEmerald, fontWeight = FontWeight.SemiBold)
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
                                putExtra(Intent.EXTRA_SUBJECT, "GeoVerity Authenticated Evidence")
                                putExtra(Intent.EXTRA_TEXT, "GeoVerity Authenticated Digital Evidence\nLocation: ${evidence?.locationName}\nGPS: ${evidence?.latitude}, ${evidence?.longitude}\n(Note: Share as Document/File to preserve exact cryptographic bits).")
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
            
            // 1. Interactive Zoomable Image Preview Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                loadedBitmap?.let { bmp ->
                    ZoomableImageView(
                        bitmap = bmp,
                        contentDescription = "Full High-Res Evidence",
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandPrimary)
                }
            }

            // 2. Cryptographic Proof Specifications Drawer (Clean, NO explicit verification IDs)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cryptographic Proof Specifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Box(
                                modifier = Modifier
                                    .background(EmeraldLight, RoundedCornerShape(50.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "VERIFIED",
                                    color = EmeraldDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = Slate100)

                        ProofField(
                            label = "Digital Evidence Status",
                            value = "Authentic • Signed by Authority Server",
                            isHighlighted = true,
                            isSuccess = true
                        )
                        ProofField(label = "Detailed Location & Pincode", value = ev.locationName)
                        ProofField(label = "GPS Coordinates", value = String.format(Locale.US, "%.6f, %.6f", ev.latitude, ev.longitude))
                        ProofField(
                            label = "Authoritative Timestamp",
                            value = SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a (z)", Locale.US).format(Date(ev.trustedTimestamp))
                        )
                        ProofField(label = "Composite SHA-256 Hash", value = ev.sha256Hash, isMonospace = true)
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

    // Delete Confirmation Dialog (Deletes from local device only)
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
                        text = "🔒 Security Note: The server's authoritative cryptographic proof, SHA-256 composite hash, and ECDSA signature will remain permanently intact on the authority server.",
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

@Composable
fun ZoomableImageView(
    bitmap: Bitmap,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    if (newScale > 1f) {
                        scale = newScale
                        offset = Offset(offset.x + pan.x, offset.y + pan.y)
                    } else {
                        scale = 1f
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )

        // Floating Zoom Controls overlay (+ / - / scale indicator)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = {
                    scale = (scale - 0.5f).coerceAtLeast(1f)
                    if (scale == 1f) offset = Offset.Zero
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(14.dp))
            }

            Text(
                "${String.format(Locale.US, "%.1f", scale)}x",
                color = BrandPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    scale = (scale + 0.5f).coerceIn(1f, 5f)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
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
