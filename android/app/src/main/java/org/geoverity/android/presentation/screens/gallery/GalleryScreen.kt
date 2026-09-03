package org.geoverity.android.presentation.screens.gallery

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateToViewer: (String) -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = GeoVerityApp.instance.database

    val activeEvidence by db.evidenceHistoryDao().getActiveLocalEvidence().collectAsState(initial = emptyList())
    val pendingCaptures by db.offlineCaptureDao().getAllOfflineCaptures().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Local Images, 1: Pending Offline
    var showDeleteDialog by remember { mutableStateOf(false) }
    var evidenceToDelete by remember { mutableStateOf<EvidenceHistoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Local Evidence Gallery",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Stored on device • Cryptographically verifiable",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToCapture,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(IndigoLight, CircleShape)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = BrandPrimary)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Tab Selector (Clean White Card with Modern Colorful Active Pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WhiteBackground, RoundedCornerShape(16.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab 0: Authenticated Local Images
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedTab == 0) BrandPrimary else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Local Gallery (${activeEvidence.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) Color.White else Slate600
                    )
                }

                // Tab 1: Pending Offline Images
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedTab == 1) BrandAmber else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pending Queue (${pendingCaptures.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) Color.White else Slate600
                    )
                }
            }

            if (selectedTab == 0) {
                if (activeEvidence.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(IndigoLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.PhotoLibrary,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Text(
                                    text = "No Evidence Photos Stored Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Take a photograph using the GeoVerity Controlled Camera to capture cryptographically authenticated evidence.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = onNavigateToCapture,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Capture Digital Evidence", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(activeEvidence) { item ->
                            EvidenceImageCard(
                                evidence = item,
                                onView = { onNavigateToViewer(item.verificationId) },
                                onShare = {
                                    val file = resolveImageFile(context, item.localImagePath, item.verificationId)
                                    if (file != null && file.exists()) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/jpeg"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_SUBJECT, "GeoVerity Evidence ${item.verificationId}")
                                            putExtra(Intent.EXTRA_TEXT, "GeoVerity Authenticated Digital Evidence\nVerification ID: ${item.verificationId}\nLocation: ${item.locationName}\nGPS: ${item.latitude}, ${item.longitude}\n(Note: Share as Document/File to preserve exact cryptographic bits).")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Original Evidence File"))
                                    }
                                },
                                onDelete = {
                                    evidenceToDelete = item
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            } else {
                // Pending Offline Captures list
                if (pendingCaptures.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(EmeraldLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.CloudDone, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(32.dp))
                            }
                            Text(text = "All captures are fully synchronized with server authority!", style = MaterialTheme.typography.bodyMedium, color = Slate600)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(pendingCaptures) { offline ->
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = offline.verificationId.take(16) + "...",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )

                                        Box(
                                            modifier = Modifier
                                                .background(AmberLight, RoundedCornerShape(50.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "AUTO-SYNC PENDING",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AmberDark,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = "📍 ${offline.locationName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate700
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Encrypted in Android Keystore",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandIndigo,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(offline.deviceCaptureTime)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate500
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    // Delete Confirmation Dialog (Explaining Device Only Deletion)
    if (showDeleteDialog && evidenceToDelete != null) {
        val item = evidenceToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = BrandRose, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = "Delete Local Photo?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This will permanently delete the photograph file from your mobile device storage to free up space.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                    Text(
                        text = "🔒 Security Note: The server's cryptographic verification record, SHA-256 hash, and ECDSA signature will remain permanently intact on the authority server.",
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
                            // 1. Delete physical JPEG file from storage
                            val file = resolveImageFile(context, item.localImagePath, item.verificationId)
                            if (file != null && file.exists()) {
                                file.delete()
                            }
                            // 2. Mark local deleted in Room DB
                            db.evidenceHistoryDao().markLocalDeleted(item.verificationId)
                            showDeleteDialog = false
                            evidenceToDelete = null
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

private fun resolveImageFile(context: android.content.Context, localImagePath: String?, verificationId: String): File? {
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
private fun EvidenceImageCard(
    evidence: EvidenceHistoryEntity,
    onView: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var loadedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val isPendingSync = evidence.signatureStatus == "PENDING_SYNC"

    LaunchedEffect(evidence.localImagePath, evidence.verificationId) {
        withContext(Dispatchers.IO) {
            val file = resolveImageFile(context, evidence.localImagePath, evidence.verificationId)
            if (file != null && file.exists()) {
                loadedBitmap = BitmapFactory.decodeFile(file.absolutePath)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(24.dp)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            
            // Image Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Slate100)
                    .clickable { onView() },
                contentAlignment = Alignment.Center
            ) {
                loadedBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Evidence Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = Slate400, modifier = Modifier.size(44.dp))
                    Text(text = "Loading Local Image...", style = MaterialTheme.typography.labelSmall, color = Slate500)
                }

                // Status Badge Overlay on top-right of image
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(if (isPendingSync) AmberLight else EmeraldLight, RoundedCornerShape(50.dp))
                        .border(
                            1.dp,
                            if (isPendingSync) BrandAmber.copy(alpha = 0.4f) else BrandEmerald.copy(alpha = 0.4f),
                            RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (isPendingSync) Icons.Default.CloudSync else Icons.Default.Verified,
                            contentDescription = null,
                            tint = if (isPendingSync) BrandAmber else BrandEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isPendingSync) "PENDING SYNC" else "AUTHENTICATED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPendingSync) AmberDark else EmeraldDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Location & Pincode info
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = evidence.locationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GPS: ${String.format(Locale.US, "%.5f, %.5f", evidence.latitude, evidence.longitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(evidence.trustedTimestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }
            }

            HorizontalDivider(color = Slate100)

            // Action Buttons Row: [ VIEW ] [ SHARE ] [ DELETE ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View Button
                OutlinedButton(
                    onClick = onView,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary)
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = "View", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "View", fontWeight = FontWeight.SemiBold)
                }

                // Share Button
                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandIndigo)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Share", fontWeight = FontWeight.SemiBold)
                }

                // Delete Button
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRose)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
