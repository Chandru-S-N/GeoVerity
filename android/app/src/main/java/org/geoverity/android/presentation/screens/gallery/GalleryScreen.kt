package org.geoverity.android.presentation.screens.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.geoverity.android.GeoVerityApp
import org.geoverity.android.data.db.EvidenceHistoryEntity
import org.geoverity.android.presentation.theme.*
import java.io.File

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

    var selectedTab by remember { mutableIntStateOf(0) }
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
                            text = "${activeEvidence.size} authenticated photos",
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
        ) {
            // Tab Selector Pill Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .background(WhiteBackground, RoundedCornerShape(18.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                            text = "My Photos (${activeEvidence.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) Color.White else Slate700
                        )
                    }
                }

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

            if (selectedTab == 0) {
                if (activeEvidence.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(26.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(IndigoLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.PhotoLibrary,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Text(
                                    text = "No Evidence Photos Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Capture geotagged photos using the Controlled Camera to build your authenticated evidence gallery.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onNavigateToCapture,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Capture Evidence", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(activeEvidence, key = { it.verificationId }) { item ->
                            EvidenceGridTile(
                                evidence = item,
                                onTap = { onNavigateToViewer(item.verificationId) },
                                onLongPress = {
                                    evidenceToDelete = item
                                    showDeleteDialog = true
                                }
                            )
                        }
                        item(span = { GridItemSpan(4) }) {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            } else {
                if (pendingCaptures.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(EmeraldLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.CloudDone, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(36.dp))
                                }
                                Text(
                                    text = "All Synchronized!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "All evidence is authenticated and secured on the authority server.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(pendingCaptures, key = { it.verificationId }) { offline ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AmberLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = BrandAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = offline.locationName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberDark,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                        item(span = { GridItemSpan(4) }) {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && evidenceToDelete != null) {
        val item = evidenceToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = BrandRose, modifier = Modifier.size(34.dp))
            },
            title = {
                Text(text = "Delete Photo?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Text(
                    text = "This will permanently delete the photo from your device. The server verification record remains intact.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val file = resolveImageFile(context, item.localImagePath, item.verificationId)
                            if (file != null && file.exists()) file.delete()
                            db.evidenceHistoryDao().markLocalDeleted(item.verificationId)
                            showDeleteDialog = false
                            evidenceToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EvidenceGridTile(
    evidence: EvidenceHistoryEntity,
    onTap: () -> Unit,
    onLongPress: () -> Unit
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

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(Slate100)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        contentAlignment = Alignment.Center
    ) {
        loadedBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Evidence Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = Slate300,
            strokeWidth = 2.dp
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(16.dp)
                .background(
                    if (isPendingSync) BrandAmber else BrandEmerald,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPendingSync) Icons.Default.CloudSync else Icons.Default.Verified,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}
