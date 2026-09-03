package org.geoverity.android.presentation.screens.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import org.geoverity.android.GeoVerityApp
import org.geoverity.android.offline.OfflineSyncWorker
import org.geoverity.android.presentation.theme.*

@Composable
fun OfflineCapturesScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = GeoVerityApp.instance.database
    val captures by db.offlineCaptureDao().getAllOfflineCaptures().collectAsState(initial = emptyList())
    var isSyncing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Captures", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch { db.offlineCaptureDao().purgeCompletedAndRejected() }
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Purge")
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Sync Action Bar
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier.fillMaxWidth(),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "${captures.size} Total Offline Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Encrypted locally with AES-256-GCM", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                    }

                    Button(
                        onClick = {
                            isSyncing = true
                            val syncRequest = OneTimeWorkRequestBuilder<OfflineSyncWorker>().build()
                            WorkManager.getInstance(context).enqueue(syncRequest)
                            isSyncing = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Sync Now", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (captures.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = Slate300, modifier = Modifier.size(48.dp))
                        Text(text = "No pending offline captures", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                        text = capture.verificationId.take(16) + "...",
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

                                Text(text = capture.locationName, style = MaterialTheme.typography.bodyMedium, color = Slate700)
                                Text(
                                    text = "Recorded: ${java.util.Date(capture.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500
                                )

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
