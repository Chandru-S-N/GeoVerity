package org.geoverity.android.presentation.screens.gallery

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Local Photos, 1: Pending Sync
    var showDeleteDialog by remember { mutableStateOf(false) }
    var evidenceToDelete by remember { mutableStateOf<EvidenceHistoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Evidence Gallery",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Text(
                            text = "Locally stored • Verifiable digital proof",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate800)
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
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // Tab Selector Pill Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WhiteBackground, RoundedCornerShape(18.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tab 0: Local Evidence Photos
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selectedTab == 0) Brush.horizontalGradient(listOf(BrandPrimary, BrandIndigo))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color.White else Slate500,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Local Photos (${activeEvidence.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) Color.White else Slate700
                        )
                    }
                }

                // Tab 1: Pending Auto-Sync Queue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selectedTab == 1) Brush.horizontalGradient(listOf(BrandAmber, BrandAmber.copy(alpha = 0.85f)))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color.White else Slate500,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Pending (${pendingCaptures.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) Color.White else Slate700
                        )
                    }
                }
            }

            // Tab Content
            if (selectedTab == 0) {
                if (activeEvidence.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .shadow(4.dp, RoundedCornerShape(26.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .background(IndigoLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.PhotoLibrary,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                                Text(
                                    text = "No Evidence Photos Stored Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Take a photograph using the Controlled Camera to capture cryptographically authenticated evidence with tamper-proof geolocation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = onNavigateToCapture,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Capture Digital Evidence", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(activeEvidence, key = { it.verificationId }) { item ->
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
                                            putExtra(Intent.EXTRA_SUBJECT, "GeoVerity Authenticated Evidence")
                                            putExtra(Intent.EXTRA_TEXT, "GeoVerity Authenticated Digital Evidence\nLocation: ${item.locationName}\nGPS: ${item.latitude}, ${item.longitude}\n(Note: Share as Document/File to preserve exact cryptographic bits).")
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

                        item { Spacer(modifier = Modifier.height(16.dp)) }
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
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .background(EmeraldLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.CloudDone, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(36.dp))
                                }
                                Text(
                                    text = "All Captures Synchronized!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "All evidence is fully authenticated and notarized on the authority server ledger.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(pendingCaptures, key = { it.verificationId }) { offline ->
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(22.dp)),
                                border = BorderStroke(1.dp, Slate200)
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
                                            text = "Encrypted Offline Geotag",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )

                                        Box(
                                            modifier = Modifier
                                                .background(AmberLight, RoundedCornerShape(50.dp))
                                                .border(1.dp, BrandAmber.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
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

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog (Device Only Deletion)
    if (showDeleteDialog && evidenceToDelete != null) {
        val item = evidenceToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = BrandRose, modifier = Modifier.size(34.dp)) },
            title = {
                Text(
                    text = "Delete Local Photograph?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This will permanently delete the photograph file from your mobile device storage to free up space.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = IndigoLight.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "🔒 Security Note: The server's cryptographic verification record, SHA-256 hash, and ECDSA signature remain permanently intact on the authority server ledger.",
                            style = MaterialTheme.typography.labelSmall,
                            color = IndigoDark,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val file = resolveImageFile(context, item.localImagePath, item.verificationId)
                            if (file != null && file.exists()) {
                                file.delete()
                            }
                            db.evidenceHistoryDao().markLocalDeleted(item.verificationId)
                            showDeleteDialog = false
                            evidenceToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete from Device", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Slate600, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = WhiteBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

private fun resolveImageFile(context: Context, localImagePath: String?, verificationId: String): File? {
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
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
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
            .shadow(3.dp, RoundedCornerShape(24.dp), spotColor = BrandIndigo.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            
            // Image Thumbnail Container with overlay
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

                // Bottom gradient scrim showing tap to zoom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Tap to view full resolution & proof",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
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
                Button(
                    onClick = onView,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = "View", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "View", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Share Button
                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandIndigo),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Share", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Delete Button
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRose),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
