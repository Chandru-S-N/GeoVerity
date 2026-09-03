package org.geoverity.android.presentation.screens.gallery

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
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
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = GeoVerityApp.instance.database

    val activeEvidence by db.evidenceHistoryDao().getActiveLocalEvidence().collectAsState(initial = emptyList())
    val pendingCaptures by db.offlineCaptureDao().getAllOfflineCaptures().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(0) } // 0: Local Images, 1: Pending Offline
    var evidenceToDelete by remember { mutableStateOf<EvidenceHistoryEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Captured Digital Evidence",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${activeEvidence.size} stored on local device",
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
            
            // Tab Selector (Clean White Card with Colorful Active Pills)
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
                        text = "Local Images (${activeEvidence.size})",
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
                        text = "Pending Offline (${pendingCaptures.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) Color.White else Slate600
                    )
                }
            }

            if (selectedTab == 0) {
                if (activeEvidence.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Slate300, modifier = Modifier.size(56.dp))
                            Text(text = "No images stored on device", style = MaterialTheme.typography.bodyMedium, color = Slate500)
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
                                    item.localImagePath?.let { path ->
                                        val file = File(path)
                                        if (file.exists()) {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/jpeg"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, "GeoVerity Evidence ${item.verificationId}")
                                                putExtra(Intent.EXTRA_TEXT, "GeoVerity Authenticated Digital Evidence\nVerification ID: ${item.verificationId}\nLocation: ${item.locationName}\nGPS: ${item.latitude}, ${item.longitude}\n(Note: Share as Document/File to preserve exact cryptographic bytes).")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Original Evidence File"))
                                        }
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
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Outlined.CloudDone, contentDescription = null, tint = Slate300, modifier = Modifier.size(56.dp))
                            Text(text = "No pending offline captures. All synced!", style = MaterialTheme.typography.bodyMedium, color = Slate500)
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate700
                                    )

                                    Text(
                                        text = "🔒 Encrypted with Keystore AES-256-GCM. Will automatically reconcile and authenticate when internet connects.",
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

    // Delete Confirmation Dialog (Explains local deletion semantics)
    if (showDeleteDialog && evidenceToDelete != null) {
        val item = evidenceToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Local Image?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This will permanently remove the physical image file from your mobile device storage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate700
                    )
                    Text(
                        text = "ℹ️ Note: The server's authoritative verification proof (ECDSA signature, SHA-256 hash, and audit log) remains intact and valid on the GeoVerity authority registry.",
                        style = MaterialTheme.typography.bodySmall,
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
                            item.localImagePath?.let { path ->
                                val file = File(path)
                                if (file.exists()) {
                                    file.delete()
                                }
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

@Composable
private fun EvidenceImageCard(
    evidence: EvidenceHistoryEntity,
    onView: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var loadedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(evidence.localImagePath) {
        withContext(Dispatchers.IO) {
            evidence.localImagePath?.let { path ->
                val f = File(path)
                if (f.exists()) {
                    loadedBitmap = BitmapFactory.decodeFile(f.absolutePath)
                }
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
                    .height(200.dp)
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
                } ?: Icon(Icons.Outlined.Image, contentDescription = null, tint = Slate300, modifier = Modifier.size(40.dp))

                // Status Badge Overlay on top-right of image
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(EmeraldLight, RoundedCornerShape(50.dp))
                        .border(1.dp, BrandEmerald.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(14.dp))
                        Text(text = "AUTHENTICATED", style = MaterialTheme.typography.labelSmall, color = EmeraldDark, fontWeight = FontWeight.Bold)
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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandEmerald)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Share", fontWeight = FontWeight.SemiBold)
                }

                // Delete Button
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRose)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Delete", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
