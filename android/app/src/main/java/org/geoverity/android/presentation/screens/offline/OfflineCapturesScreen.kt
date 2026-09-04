package org.geoverity.android.presentation.screens.offline

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
import androidx.compose.material.icons.outlined.CloudOff
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Sync Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch { db.offlineCaptureDao().purgeCompletedAndRejected() }
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Purge Cleaned")
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
            
            // 1. Automatic Sync Architecture Visualizer Pipeline Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(24.dp)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUTOMATIC SYNC PIPELINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandIndigo,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Box(
                            modifier = Modifier
                                .background(
                                    if (serverHealth.isConnected) EmeraldLight else AmberLight,
                                    RoundedCornerShape(50.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (serverHealth.isConnected) "ONLINE" else "OFFLINE",
                                color = if (serverHealth.isConnected) EmeraldDark else AmberDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 4-Step Diagram
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SyncStepItem(icon = Icons.Default.CameraAlt, label = "Capture", active = true)
                        Text("➔", color = Slate400, fontWeight = FontWeight.Bold)
                        SyncStepItem(icon = Icons.Default.Lock, label = "AES-256", active = true)
                        Text("➔", color = Slate400, fontWeight = FontWeight.Bold)
                        SyncStepItem(icon = Icons.Default.CloudSync, label = "WorkManager", active = serverHealth.isConnected)
                        Text("➔", color = Slate400, fontWeight = FontWeight.Bold)
                        SyncStepItem(icon = Icons.Default.CloudDone, label = "Server DB", active = serverHealth.isConnected)
                    }
                }
            }

            // 2. Sync Action Bar
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${captures.size} Captures in Queue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = if (captures.isEmpty()) "All evidence synchronized" else "Auto-syncs when online",
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Sync Now", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. Queue List
            if (captures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(EmeraldLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(32.dp))
                        }
                        Text(
                            text = "All captures synchronized!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "No pending items in the offline queue",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(captures) { capture ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(20.dp)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = capture.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (capture.status == "PENDING") AmberDark else RoseDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "📍 ${capture.locationName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate700
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "AES-256 Keystore Protected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandIndigo,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(capture.deviceCaptureTime)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate500
                                    )
                                }

                                capture.rejectionReason?.let { reason ->
                                    Text(
                                        text = "⚠️ Anomaly: $reason",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BrandRose,
                                        fontWeight = FontWeight.SemiBold
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

@Composable
private fun SyncStepItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (active) IndigoLight else Slate100, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) BrandIndigo else Slate400,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (active) Slate900 else Slate400,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
