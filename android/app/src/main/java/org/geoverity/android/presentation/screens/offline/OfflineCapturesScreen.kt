package org.geoverity.android.presentation.screens.offline

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import org.geoverity.android.GeoVerityApp
import org.geoverity.android.data.network.ServerHealthChecker
import org.geoverity.android.offline.OfflineSyncWorker
import org.geoverity.android.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineCapturesScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = GeoVerityApp.instance.database
    val captures by db.offlineCaptureDao().getAllOfflineCaptures().collectAsState(initial = emptyList())
    val serverHealth by ServerHealthChecker.state.collectAsState()
    var isSyncing by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Auto-Sync Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Slate900)
                        Text("Android Keystore AES-256 Protected Queue", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate800)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch { db.offlineCaptureDao().purgeCompletedAndRejected() }
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Purge Cleaned", tint = Slate600)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WhiteBackground)
            )
        },
        containerColor = Slate50
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. Automatic Sync Architecture Visualizer Pipeline Card
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(26.dp), spotColor = BrandIndigo.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(IndigoLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "SECURE AUTO-SYNC PIPELINE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandIndigo,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (serverHealth.isConnected) EmeraldLight else AmberLight,
                                        RoundedCornerShape(50.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (serverHealth.isConnected) BrandEmerald.copy(alpha = 0.4f) else BrandAmber.copy(alpha = 0.4f),
                                        RoundedCornerShape(50.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (serverHealth.isConnected) "ONLINE" else "OFFLINE",
                                    color = if (serverHealth.isConnected) EmeraldDark else AmberDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // 4-Step Interactive Diagram
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SyncStepItem(icon = Icons.Default.CameraAlt, label = "Capture", active = true, isComplete = true)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                            SyncStepItem(icon = Icons.Default.Lock, label = "AES-256", active = true, isComplete = true)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = if (serverHealth.isConnected) BrandPrimary else Slate400, modifier = Modifier.size(16.dp))
                            SyncStepItem(icon = Icons.Default.CloudSync, label = "Worker", active = serverHealth.isConnected, isComplete = serverHealth.isConnected)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = if (serverHealth.isConnected) BrandEmerald else Slate400, modifier = Modifier.size(16.dp))
                            SyncStepItem(icon = Icons.Default.CloudDone, label = "Server DB", active = serverHealth.isConnected, isComplete = serverHealth.isConnected)
                        }
                    }
                }
            }

            // 2. Server Connectivity & 1-Tap Subnet Scanner Card
            if (!serverHealth.isConnected) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberLight.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, BrandAmber.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.WifiOff, contentDescription = null, tint = AmberDark, modifier = Modifier.size(22.dp))
                                Column {
                                    Text(
                                        text = "Offline Mode Active",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberDark
                                    )
                                    Text(
                                        text = "Captures are AES-256 encrypted on device and will automatically sync when connected.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AmberDark
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    isScanning = true
                                    scanMessage = "Scanning Wi-Fi subnet for server..."
                                    coroutineScope.launch {
                                        val discovered = ServerHealthChecker.scanAndAutoConnect { status ->
                                            scanMessage = status
                                        }
                                        isScanning = false
                                        scanMessage = if (discovered != null) "Connected: $discovered" else "No server found on subnet. Check PC IP."
                                    }
                                },
                                enabled = !isScanning,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scanning Wi-Fi Subnet...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.WifiFind, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("1-Tap Auto-Discover Server on Wi-Fi", fontWeight = FontWeight.Bold)
                                }
                            }

                            scanMessage?.let { msg ->
                                Text(text = msg, style = MaterialTheme.typography.labelSmall, color = Slate700)
                            }
                        }
                    }
                }
            }

            // 3. Sync Action Bar
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(22.dp)),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${captures.size} Captures in Queue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            Text(
                                text = if (captures.isEmpty()) "All evidence synchronized" else "Auto-sync runs automatically in background",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }

                        Button(
                            onClick = {
                                isSyncing = true
                                val syncRequest = OneTimeWorkRequestBuilder<OfflineSyncWorker>().build()
                                WorkManager.getInstance(context).enqueue(syncRequest)
                                isSyncing = false
                            },
                            enabled = captures.isNotEmpty() && !isSyncing,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Sync Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Queue List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Encrypted Local Queue (${captures.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
            }

            // 5. Queue Items or Empty State
            if (captures.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .background(EmeraldLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(36.dp))
                            }
                            Text(
                                text = "All Captures Synchronized!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            Text(
                                text = "There are no pending captures waiting in the local encrypted queue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(captures, key = { it.verificationId }) { capture ->
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
                                    text = "Encrypted Geotag Capture",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Slate900,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (capture.status == "PENDING") AmberLight else RoseLight,
                                            RoundedCornerShape(50.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (capture.status == "PENDING") BrandAmber.copy(alpha = 0.4f) else BrandRose.copy(alpha = 0.4f),
                                            RoundedCornerShape(50.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = capture.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (capture.status == "PENDING") AmberDark else RoseDark,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Text(
                                text = "📍 ${capture.locationName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate700,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "AES-256 Keystore Protected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandIndigo,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(capture.deviceCaptureTime)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500
                                )
                            }

                            capture.rejectionReason?.let { reason ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = RoseLight),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⚠️ Anomaly: $reason",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RoseDark,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SyncStepItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    isComplete: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (isComplete) EmeraldLight else if (active) IndigoLight else Slate100,
                    CircleShape
                )
                .border(
                    1.dp,
                    if (isComplete) BrandEmerald.copy(alpha = 0.4f) else if (active) BrandIndigo.copy(alpha = 0.4f) else Slate200,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isComplete) BrandEmerald else if (active) BrandIndigo else Slate400,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            label,
            color = if (active || isComplete) Slate900 else Slate400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
