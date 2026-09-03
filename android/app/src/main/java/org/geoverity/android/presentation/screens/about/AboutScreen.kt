package org.geoverity.android.presentation.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.geoverity.android.presentation.theme.*

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About GeoVerity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // App Banner
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(IndigoLight, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(34.dp))
                    }

                    Text(text = "GeoVerity Mobile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(text = "Version 1.0.0 (Release)", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    Text(
                        text = "A Secure Geolocation & Image Authentication Platform powered by server-side ECDSA P-256 and composite SHA-256 cryptographic binding.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate700,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Security Specifications Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                modifier = Modifier.fillMaxWidth(),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Cryptographic Architecture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text(text = "• Server Authority: ECDSA NIST P-256 (SHA256withECDSA)", style = MaterialTheme.typography.bodyMedium, color = Slate700)
                    Text(text = "• Integrity Binding: SHA-256 over image bytes + canonical metadata", style = MaterialTheme.typography.bodyMedium, color = Slate700)
                    Text(text = "• Offline Security: Monotonic elapsedRealtime reconciliation (120s tolerance)", style = MaterialTheme.typography.bodyMedium, color = Slate700)
                    Text(text = "• Local Storage: Android Keystore AES-256-GCM encrypted", style = MaterialTheme.typography.bodyMedium, color = Slate700)
                    Text(text = "• Zero Server Image Retention: Proof records only (no image blobs)", style = MaterialTheme.typography.bodyMedium, color = Slate700)
                }
            }

            // Realistic Security Notice
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "GeoVerity never claims '100% secure'. Cryptographic guarantees protect digital evidence against unauthorized modification, re-compression, EXIF tampering, and device clock rollbacks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
