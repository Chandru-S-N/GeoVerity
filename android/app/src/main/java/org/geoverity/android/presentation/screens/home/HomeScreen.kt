package org.geoverity.android.presentation.screens.home

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.geoverity.android.GeoVerityApp
import org.geoverity.android.data.network.ConnectionTestResult
import org.geoverity.android.data.network.ServerHealthChecker
import org.geoverity.android.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onNavigateToCapture: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToVerify: () -> Unit,
    onNavigateToOffline: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = GeoVerityApp.instance.database
    val secureStorage = GeoVerityApp.instance.secureStorage

    val serverHealth by ServerHealthChecker.state.collectAsState()
    val totalCount by db.evidenceHistoryDao().getTotalAuthenticatedCount().collectAsState(initial = 0)
    val localCount by db.evidenceHistoryDao().getLocalEvidenceCount().collectAsState(initial = 0)
    val pendingCount by db.offlineCaptureDao().getPendingCount().collectAsState(initial = 0)
    val recentEvidence by db.evidenceHistoryDao().getActiveLocalEvidence().collectAsState(initial = emptyList())

    var showServerConfigDialog by remember { mutableStateOf(false) }

    // Periodically check server connectivity every 10 seconds
    LaunchedEffect(Unit) {
        while (true) {
            ServerHealthChecker.checkHealth()
            delay(10000)
        }
    }

    Scaffold(
        containerColor = Slate50
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            // 1. App Header & Brand Banner
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate200, Slate100))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(28.dp), spotColor = BrandIndigo.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(listOf(BrandIndigo, BrandPurple))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "GeoVerity",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Slate900,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Secure Digital Evidence Platform",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate500
                                )
                            }
                        }

                        // 2. Interactive Server Connection Status Card (Tap to Configure IP)
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (serverHealth.isConnected) EmeraldLight else RoseLight
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (serverHealth.isConnected) BrandEmerald.copy(alpha = 0.3f) else BrandRose.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showServerConfigDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                if (serverHealth.isConnected) BrandEmerald else BrandRose,
                                                CircleShape
                                            )
                                    )
                                    Column {
                                        Text(
                                            text = if (serverHealth.isConnected) "Authority Online" else "Server Unreachable",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (serverHealth.isConnected) EmeraldDark else RoseDark
                                        )
                                        Text(
                                            text = secureStorage.getServerUrl(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate600
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (serverHealth.isConnected) {
                                        Text(
                                            text = "${serverHealth.latencyMs}ms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Configure Server IP",
                                        tint = if (serverHealth.isConnected) EmeraldDark else RoseDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Auto-Sync Ready Status Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .background(IndigoLight, RoundedCornerShape(50.dp))
                                .border(1.dp, BrandIndigo.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(BrandIndigo, CircleShape))
                            Text(
                                text = "Background Auto-Sync: Real-time network listener active",
                                style = MaterialTheme.typography.labelSmall,
                                color = IndigoDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 3. Primary Hero Action Card: Capture Authenticated Digital Evidence
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = BrandPrimary.copy(alpha = 0.35f))
                        .clickable { onNavigateToCapture() }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BrandPrimary, BrandIndigo, BrandPurple)
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Controlled Camera",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Capture Evidence",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "1-Tap automatic binding with GPS pincode, server time & ECDSA signature.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Quick Access Gallery Banner Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(22.dp))
                        .clickable { onNavigateToGallery() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(EmeraldLight, RoundedCornerShape(16.dp))
                                    .border(1.dp, BrandEmerald.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(26.dp))
                            }
                            Column {
                                Text(
                                    text = "Device Evidence Gallery",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "$localCount photographs saved locally on phone",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                            }
                        }

                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
                    }
                }
            }

            // 5. Quick Stats 3-Card Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Authenticated
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(20.dp)),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(IndigoLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                            }
                            Text(text = "$totalCount", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Slate900)
                            Text(text = "Total Verified", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        }
                    }

                    // Stored Locally
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(20.dp)),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(EmeraldLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(20.dp))
                            }
                            Text(text = "$localCount", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Slate900)
                            Text(text = "Local Photos", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        }
                    }

                    // Pending Offline Sync
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(20.dp)),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AmberLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(20.dp))
                            }
                            Text(text = "$pendingCount", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Slate900)
                            Text(text = "Pending Sync", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        }
                    }
                }
            }

            // 6. Recent Evidence Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Evidence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "View All ($localCount)",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToGallery() }
                    )
                }
            }

            // 7. Recent Evidence List Items
            if (recentEvidence.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.Camera, contentDescription = null, tint = Slate400, modifier = Modifier.size(36.dp))
                            Text(text = "No evidence captured yet", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                    }
                }
            } else {
                items(recentEvidence.take(4)) { item ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .clickable { onNavigateToDetails(item.verificationId) },
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            if (item.signatureStatus == "VALID") EmeraldLight else AmberLight,
                                            RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (item.signatureStatus == "VALID") Icons.Default.Verified else Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = if (item.signatureStatus == "VALID") BrandEmerald else BrandAmber,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = item.locationName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.verificationId.take(16) + "...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandIndigo,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                    Text(
                                        text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(item.trustedTimestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate500
                                    )
                                }
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    // 8. Server Authority Configuration Modal Dialog
    if (showServerConfigDialog) {
        var inputUrl by remember { mutableStateOf(secureStorage.getServerUrl()) }
        var isTesting by remember { mutableStateOf(false) }
        var testResult by remember { mutableStateOf<ConnectionTestResult?>(null) }

        AlertDialog(
            onDismissRequest = { showServerConfigDialog = false },
            icon = { Icon(Icons.Default.Dns, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = "Server Authority Address",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "When testing on a physical mobile phone, enter your PC's Wi-Fi LAN IP address (e.g. http://192.168.1.10:8080):",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Server Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Presets
                    Text(text = "Quick Presets:", style = MaterialTheme.typography.labelSmall, color = Slate500, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { inputUrl = "http://10.0.2.2:8080" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("Emulator (10.0.2.2)", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = { inputUrl = "http://127.0.0.1:8080" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("Localhost (127.0.0.1)", fontSize = 10.sp)
                        }
                    }

                    // Test Connection Button
                    Button(
                        onClick = {
                            isTesting = true
                            testResult = null
                            coroutineScope.launch {
                                val result = ServerHealthChecker.testServerUrl(inputUrl.trim())
                                testResult = result
                                isTesting = false
                            }
                        },
                        enabled = !isTesting && inputUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pinging Server...")
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TEST CONNECTION NOW")
                        }
                    }

                    // Test Result Banner
                    testResult?.let { res ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (res.isSuccess) EmeraldLight else RoseLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = res.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (res.isSuccess) EmeraldDark else RoseDark,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        secureStorage.setServerUrl(inputUrl.trim())
                        coroutineScope.launch {
                            ServerHealthChecker.checkHealth()
                        }
                        showServerConfigDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Save & Apply", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerConfigDialog = false }) {
                    Text("Cancel", color = Slate600)
                }
            },
            containerColor = WhiteBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
