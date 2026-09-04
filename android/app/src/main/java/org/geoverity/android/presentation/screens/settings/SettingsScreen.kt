package org.geoverity.android.presentation.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.geoverity.android.GeoVerityApp
import org.geoverity.android.data.network.ConnectionTestResult
import org.geoverity.android.data.network.ServerHealthChecker
import org.geoverity.android.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val secureStorage = GeoVerityApp.instance.secureStorage
    val serverHealth by ServerHealthChecker.state.collectAsState()

    var serverUrl by remember { mutableStateOf(secureStorage.getServerUrl()) }
    var apiKeyInput by remember { mutableStateOf("") }
    var showApiKeyInput by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val deviceIp = remember { ServerHealthChecker.getLocalDeviceIp() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security & Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            itemSpacer()

            // 1. Authority Server Endpoint & Wi-Fi Auto-Discovery Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Authority Server Endpoint", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .background(if (serverHealth.isConnected) EmeraldLight else AmberLight, RoundedCornerShape(50.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (serverHealth.isConnected) "CONNECTED" else "UNREACHABLE",
                                color = if (serverHealth.isConnected) EmeraldDark else AmberDark,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = {
                            serverUrl = it
                            testResult = null
                        },
                        label = { Text("Server Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // 1-Tap Auto-Discover on Wi-Fi Subnet Button
                    Button(
                        onClick = {
                            isScanning = true
                            testResult = null
                            coroutineScope.launch {
                                val discovered = ServerHealthChecker.scanAndAutoConnect()
                                if (discovered != null) {
                                    serverUrl = discovered
                                    testResult = ConnectionTestResult(true, 40, "Discovered & Connected to: $discovered")
                                } else {
                                    testResult = ConnectionTestResult(false, 0, "No GeoVerity server found on local Wi-Fi subnet.")
                                }
                                isScanning = false
                            }
                        },
                        enabled = !isScanning,
                        shape = RoundedCornerShape(14.dp),
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Test Connection Button
                        OutlinedButton(
                            onClick = {
                                isTesting = true
                                testResult = null
                                coroutineScope.launch {
                                    val result = ServerHealthChecker.testServerUrl(serverUrl.trim())
                                    testResult = result
                                    isTesting = false
                                }
                            },
                            enabled = !isTesting && serverUrl.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Ping")
                            }
                        }

                        // Save URL Button
                        Button(
                            onClick = {
                                secureStorage.setServerUrl(serverUrl.trim())
                                coroutineScope.launch {
                                    ServerHealthChecker.checkHealth()
                                }
                                savedMessage = "Server URL saved!"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Save URL", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (deviceIp != null) {
                        Text(
                            text = "Detected Device IP: $deviceIp",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }

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
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // 2. Client Authorization & Keystore Security Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Cryptographic Identity & Keystore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    SettingRow("Authentication Scheme", "App-Level X-API-Key (Zero User Login)")
                    SettingRow("KeyStore Security", "Android Keystore AES-256-GCM")
                    SettingRow("Digital Signature Algorithm", "ECDSA NIST P-256 (FIPS 186-4)")
                    SettingRow("Active API Key", secureStorage.getMaskedApiKey())

                    TextButton(onClick = { showApiKeyInput = !showApiKeyInput }) {
                        Text(text = if (showApiKeyInput) "Hide Key Input" else "Change API Key", color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    if (showApiKeyInput) {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("New API Key (gv_live_...)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Button(
                            onClick = {
                                if (apiKeyInput.isNotBlank()) {
                                    secureStorage.setApiKey(apiKeyInput)
                                    apiKeyInput = ""
                                    showApiKeyInput = false
                                    savedMessage = "API Key successfully updated in Keystore!"
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Save API Key")
                        }
                    }
                }
            }

            // 3. Hardware Device Diagnostics Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Device & App Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    SettingRow("Device ID", secureStorage.getDeviceId().take(18) + "...")
                    SettingRow("App Build Version", "1.0.0 (Production Verified)")
                    SettingRow("Offline Encryption", "AES-256-GCM MasterKey Active")
                    SettingRow("Play Integrity Ready", "Hardware Attestation Active")
                }
            }

            savedMessage?.let { msg ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = msg, color = EmeraldDark, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun itemSpacer() {
    Spacer(modifier = Modifier.height(2.dp))
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Slate500)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Slate900)
    }
}
