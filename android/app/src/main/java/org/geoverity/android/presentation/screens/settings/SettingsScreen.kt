package org.geoverity.android.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.geoverity.android.GeoVerityApp
import org.geoverity.android.presentation.theme.*

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val secureStorage = GeoVerityApp.instance.secureStorage
    var serverUrl by remember { mutableStateOf(secureStorage.getServerUrl()) }
    var apiKeyInput by remember { mutableStateOf("") }
    var showApiKeyInput by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            
            // 1. Client Authorization & API Key Card (Keystore Protected)
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
                    Text(text = "Client Authorization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Authorization Method", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        Text(text = "App X-API-Key (Zero User Login)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Configured API Key", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        Text(text = secureStorage.getMaskedApiKey(), style = MaterialTheme.typography.labelSmall, color = BrandIndigo, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Key Storage", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        Text(text = "Android Keystore AES-256-SIV", style = MaterialTheme.typography.labelSmall, color = BrandEmerald, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { showApiKeyInput = !showApiKeyInput }) {
                        Text(text = if (showApiKeyInput) "Hide Key Input" else "Update API Key", color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    if (showApiKeyInput) {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("New API Key (gv_live_...)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
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
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Save API Key")
                        }
                    }
                }
            }

            // 2. Server Configuration Card
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
                    Text(text = "Authority Server Endpoint", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Server Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Button(
                        onClick = {
                            secureStorage.setServerUrl(serverUrl)
                            savedMessage = "Server URL saved!"
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Text("Update Server URL")
                    }
                }
            }

            // 3. Device Diagnostics Card
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

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Device ID", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        Text(text = secureStorage.getDeviceId(), style = MaterialTheme.typography.labelSmall)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "App Version", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        Text(text = "1.0.0 (Production)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Offline Encryption", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        Text(text = "AES-256-GCM Keystore Master", style = MaterialTheme.typography.labelSmall, color = BrandEmerald)
                    }
                }
            }

            savedMessage?.let { msg ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = msg, color = EmeraldDark, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
