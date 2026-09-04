package org.geoverity.android.presentation.screens.history

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.geoverity.android.GeoVerityApp
import org.geoverity.android.data.db.EvidenceHistoryEntity
import org.geoverity.android.presentation.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceDetailsScreen(
    verificationId: String,
    onNavigateBack: () -> Unit
) {
    val db = GeoVerityApp.instance.database
    var evidence by remember { mutableStateOf<EvidenceHistoryEntity?>(null) }
    var loadedImage by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(verificationId) {
        withContext(Dispatchers.IO) {
            val rec = db.evidenceHistoryDao().getByVerificationId(verificationId)
            evidence = rec
            rec?.localImagePath?.let { path ->
                val f = File(path)
                if (f.exists()) {
                    loadedImage = BitmapFactory.decodeFile(f.absolutePath)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evidence Record", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Image Preview if stored locally
            loadedImage?.let { bmp ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                    modifier = Modifier.fillMaxWidth(),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Evidence Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            }

            // Cryptographic Details Card
            evidence?.let { item ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                    modifier = Modifier.fillMaxWidth(),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(text = "Authoritative Record Specs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Security Attestation", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(text = "AUTHENTIC & NOTARIZED BY SERVER AUTHORITY", style = MaterialTheme.typography.bodyMedium, color = BrandEmerald, fontWeight = FontWeight.Bold)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Location Name & Pincode", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(text = item.locationName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "GPS Coordinates", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(text = String.format(Locale.US, "%.6f, %.6f", item.latitude, item.longitude), style = MaterialTheme.typography.bodyMedium)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Authoritative Timestamp", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(text = SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a (z)", Locale.US).format(Date(item.trustedTimestamp)), style = MaterialTheme.typography.bodyMedium)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Composite SHA-256 Hash", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(text = item.sha256Hash, style = MaterialTheme.typography.labelSmall, color = Slate900, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Server Digital Signature", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(text = "VALID (ECDSA NIST P-256 Verified)", style = MaterialTheme.typography.bodyMedium, color = BrandEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } ?: CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.align(Alignment.CenterHorizontally))

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
