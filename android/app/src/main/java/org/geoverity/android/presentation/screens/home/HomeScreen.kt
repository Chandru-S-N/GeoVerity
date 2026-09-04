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
    var isAutoScanning by remember { mutableStateOf(false) }
    var scanStatusMessage by remember { mutableStateOf<String?>(null) }

    // Periodically check server connectivity every 8 seconds
    LaunchedEffect(Unit) {
        while (true) {
            ServerHealthChecker.checkHealth()
            delay(8000)
        }
    }

    Scaffold(
        containerColor = Slate50
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. App Header & Brand Banner
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(28.dp), spotColor = BrandIndigo.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(listOf(BrandIndigo, BrandPurple))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GeoVerity",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Slate900,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Authenticated Evidence Platform",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                            }

                            // Live Server Status Indicator Pill
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
                                    .clickable { showServerConfigDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (serverHealth.isConnected) BrandEmerald else BrandAmber,
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = if (serverHealth.isConnected) "Online" else "Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (serverHealth.isConnected) EmeraldDark else AmberDark
                                    )
                                }
                            }
                        }

                        // Interactive Server Connection Status Card with 1-Tap Auto-Discover
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (serverHealth.isConnected) EmeraldLight.copy(alpha = 0.6f) else AmberLight.copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (serverHealth.isConnected) BrandEmerald.copy(alpha = 0.3f) else BrandAmber.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            if (serverHealth.isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = if (serverHealth.isConnected) BrandEmerald else BrandAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (serverHealth.isConnected) "Authority Server Connected" else "Authority Server Unreachable",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (serverHealth.isConnected) EmeraldDark else AmberDark
                                            )
                                            Text(
                                                text = secureStorage.getServerUrl(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Slate600
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { showServerConfigDialog = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "Config", tint = Slate700, modifier = Modifier.size(16.dp))
                                    }
                                }

                                if (!serverHealth.isConnected) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                isAutoScanning = true
                                                scanStatusMessage = "Scanning Wi-Fi subnet..."
                                                coroutineScope.launch {
                                                    val found = ServerHealthChecker.scanAndAutoConnect { status ->
                                                        scanStatusMessage = status
                                                    }
                                                    isAutoScanning = false
                                                    scanStatusMessage = if (found != null) "Connected: $found" else "Server not found on Wi-Fi subnet. Tap ⚙️ to enter PC IP manually."
                                                }
                                            },
                                            enabled = !isAutoScanning,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            if (isAutoScanning) {
                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Scanning...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(Icons.Default.WifiFind, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("1-Tap Auto-Discover on Wi-Fi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { showServerConfigDialog = true },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("Configure IP", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    scanStatusMessage?.let { msg ->
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate600
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Primary Hero Action Card: Capture Authenticated Digital Evidence
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(26.dp), spotColor = BrandPrimary.copy(alpha = 0.3f))
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
                            .padding(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "CONTROLLED CAMERA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Capture & Sign Geotag",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Direct server notarization with automatic offline queue.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Quick Stats 3-Card Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Authenticated
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .clickable { onNavigateToGallery() },
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Verified", style = MaterialTheme.typography.labelSmall, color = Slate500, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.Verified, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                            }
                            Text(text = "$totalCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Slate900)
                            Text(text = "Total Authenticated", style = MaterialTheme.typography.labelSmall, color = BrandEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Local Photos on Phone
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .clickable { onNavigateToGallery() },
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Gallery", style = MaterialTheme.typography.labelSmall, color = Slate500, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(16.dp))
                            }
                            Text(text = "$localCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Slate900)
                            Text(text = "Local Storage", style = MaterialTheme.typography.labelSmall, color = Slate600, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Pending Offline Queue
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .clickable { onNavigateToOffline() },
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Auto-Sync", style = MaterialTheme.typography.labelSmall, color = Slate500, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = if (pendingCount > 0) BrandAmber else BrandEmerald, modifier = Modifier.size(16.dp))
                            }
                            Text(text = if (pendingCount > 0) "$pendingCount" else "0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Slate900)
                            Text(text = if (pendingCount > 0) "Pending Sync" else "Up to date", style = MaterialTheme.typography.labelSmall, color = if (pendingCount > 0) AmberDark else BrandEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. Quick Action Row: Verify Image & View Gallery
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Third-party verification button
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .clickable { onNavigateToVerify() },
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(IndigoLight, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(text = "Verify Photo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
                                Text(text = "Third-party check", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            }
                        }
                    }

                    // Open gallery button
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .clickable { onNavigateToGallery() },
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(EmeraldLight, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Collections, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(text = "Local Gallery", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
                                Text(text = "View all photos", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            }
                        }
                    }
                }
            }

            // 5. Recent Authentications Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Authentications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    if (recentEvidence.isNotEmpty()) {
                        Text(
                            text = "View All (${recentEvidence.size}) ↗",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToGallery() }
                        )
                    }
                }
            }

            // 6. Recent Evidence List Items
            if (recentEvidence.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.Camera, contentDescription = null, tint = Slate400, modifier = Modifier.size(40.dp))
                            Text(text = "No evidence captured yet", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Slate600)
                            Text(text = "Tap 'Capture & Sign Geotag' above to take your first photo", style = MaterialTheme.typography.labelSmall, color = Slate400)
                        }
                    }
                }
            } else {
                items(recentEvidence.take(4), key = { it.verificationId }) { item ->
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(22.dp))
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
                                        .size(46.dp)
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
                                        modifier = Modifier.size(24.dp)
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
                                        text = if (item.signatureStatus == "VALID") "🛡️ Authentic • Signed by Server" else "📦 Saved on Device • Auto-Sync Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.signatureStatus == "VALID") EmeraldDark else AmberDark,
                                        fontWeight = FontWeight.Bold
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

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Server Authority Configuration Modal Dialog
    if (showServerConfigDialog) {
        var inputUrl by remember { mutableStateOf(secureStorage.getServerUrl()) }
        var isTesting by remember { mutableStateOf(false) }
        var testResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
        val deviceIp = remember { ServerHealthChecker.getLocalDeviceIp() }

        AlertDialog(
            onDismissRequest = { showServerConfigDialog = false },
            icon = { Icon(Icons.Default.Dns, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(34.dp)) },
            title = {
                Text(
                    text = "Authority Server Address",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "When running on a mobile phone on Wi-Fi, enter your PC's IP address (e.g. http://192.168.1.15:8080):",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Server Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // 1-Tap Wi-Fi Auto-Scan Button
                    Button(
                        onClick = {
                            isTesting = true
                            testResult = null
                            coroutineScope.launch {
                                val discovered = ServerHealthChecker.scanAndAutoConnect()
                                if (discovered != null) {
                                    inputUrl = discovered
                                    testResult = ConnectionTestResult(true, 50, "Discovered & Connected: $discovered")
                                } else {
                                    testResult = ConnectionTestResult(false, 0, "No server responded on local subnet.")
                                }
                                isTesting = false
                            }
                        },
                        enabled = !isTesting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo)
                    ) {
                        Icon(Icons.Default.WifiFind, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AUTO-SCAN LOCAL WI-FI SUBNET", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Test Connection Button
                    OutlinedButton(
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
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pinging Server...")
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TEST CONNECTION NOW", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Quick Presets
                    if (deviceIp != null) {
                        val subnet = deviceIp.substringBeforeLast(".") + ".1"
                        Text(
                            text = "Detected Phone IP: $deviceIp (Subnet $subnet)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }

                    // Test Result Banner
                    testResult?.let { res ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
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
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Save & Apply", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerConfigDialog = false }) {
                    Text("Cancel", color = Slate600, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = WhiteBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
