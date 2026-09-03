package org.geoverity.android.presentation.screens.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.geoverity.android.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareEvidenceScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Evidence", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
            
            // Critical Security Notice Banner
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AmberLight),
                modifier = Modifier.fillMaxWidth(),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = BrandAmber)
                        Text(text = "Exact Digital Byte Preservation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AmberDark)
                    }
                    Text(
                        text = "GeoVerity verifies exact authenticated image bytes. Any re-compression, social media photo filter, cropping, or screenshot will alter the SHA-256 hash and invalidate third-party verification.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmberDark
                    )
                }
            }

            // Recommended Sharing Methods
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
                    Text(text = "Recommended Sharing Channels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    ShareMethodRow("Original File Transfer", "Preserves exact bit stream over USB / Nearby Share", Icons.Outlined.Devices)
                    ShareMethodRow("WhatsApp / Telegram (Document Mode)", "MUST share as 'Document / File', NOT 'Photo'", Icons.Outlined.AttachFile)
                    ShareMethodRow("Email Attachment", "Send original unmodified JPEG attachment", Icons.Outlined.Mail)
                    ShareMethodRow("Cloud Storage Drive", "Google Drive / OneDrive binary download", Icons.Outlined.CloudUpload)
                }
            }

            // Prohibited / Invalidation Channels
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
                    Text(text = "Channels That Alter Image Bytes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandRose)

                    Text(text = "❌ Taking a screenshot of the photo", style = MaterialTheme.typography.bodyMedium, color = Slate700)
                    Text(text = "❌ Photo editing, cropping, or filtering apps", style = MaterialTheme.typography.bodyMedium, color = Slate700)
                    Text(text = "❌ WhatsApp / Social Media 'Standard Photo' sharing (re-compresses pixels)", style = MaterialTheme.typography.bodyMedium, color = Slate700)
                    Text(text = "❌ Photographing a printed paper copy", style = MaterialTheme.typography.bodyMedium, color = Slate700)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ShareMethodRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(EmeraldLight, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Slate900)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Slate500)
        }
    }
}
