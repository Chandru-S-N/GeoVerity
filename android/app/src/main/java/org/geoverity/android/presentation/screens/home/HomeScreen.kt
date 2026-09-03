package org.geoverity.android.presentation.screens.home

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.geoverity.android.GeoVerityApp
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
    val db = GeoVerityApp.instance.database
    val totalCount by db.evidenceHistoryDao().getTotalAuthenticatedCount().collectAsState(initial = 0)
    val localCount by db.evidenceHistoryDao().getLocalEvidenceCount().collectAsState(initial = 0)
    val pendingCount by db.offlineCaptureDao().getPendingCount().collectAsState(initial = 0)
    val recentEvidence by db.evidenceHistoryDao().getActiveLocalEvidence().collectAsState(initial = emptyList())

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
                        .shadow(4.dp, RoundedCornerShape(28.dp), spotColor = BrandIndigo.copy(alpha = 0.12f))
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

                        // Live Status Pills Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Server Connection Status
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .background(EmeraldLight, RoundedCornerShape(50.dp))
                                    .border(1.dp, BrandEmerald.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(BrandEmerald, CircleShape))
                                Text(
                                    text = "Server Online",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Auto-Sync Ready Status
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .background(IndigoLight, RoundedCornerShape(50.dp))
                                    .border(1.dp, BrandIndigo.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(BrandIndigo, CircleShape))
                                Text(
                                    text = "Auto-Sync Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IndigoDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Primary Hero CTA: [ CAPTURE DIGITAL EVIDENCE ]
            item {
                Button(
                    onClick = onNavigateToCapture,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = BrandPrimary.copy(alpha = 0.45f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "SECURE CAPTURE (CAMERA)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // 3. Three Modern Stats Cards (Authenticated, Local Images, Pending Offline)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Authenticated
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToHistory() }
                            .shadow(2.dp, RoundedCornerShape(22.dp)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier.size(34.dp).background(EmeraldLight, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "$totalCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Slate900)
                            Text(text = "Server Proofs", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        }
                    }

                    // Local Stored on Phone
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToGallery() }
                            .shadow(2.dp, RoundedCornerShape(22.dp)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier.size(34.dp).background(IndigoLight, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "$localCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Slate900)
                            Text(text = "Local Gallery", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        }
                    }

                    // Pending Offline
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToOffline() }
                            .shadow(2.dp, RoundedCornerShape(22.dp)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier.size(34.dp).background(AmberLight, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "$pendingCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = if (pendingCount > 0) BrandAmber else Slate900)
                            Text(text = "Auto-Syncing", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        }
                    }
                }
            }

            // 4. Quick Access to Local Gallery Banner
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = IndigoLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToGallery() },
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BrandIndigo.copy(alpha = 0.3f), BrandIndigo.copy(alpha = 0.1f))))
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp).background(BrandIndigo, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Collections, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text(text = "View Stored Images", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IndigoDark)
                                Text(text = "View, share original file, or delete from device", style = MaterialTheme.typography.bodySmall, color = BrandIndigo)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandIndigo)
                    }
                }
            }

            // 5. Recent Captured Evidence
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Captures on Phone",
                        style = MaterialTheme.typography.titleLarge,
                        color = Slate900,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToGallery) {
                        Text(text = "View All ($localCount)", color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (recentEvidence.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier.fillMaxWidth(),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Slate300, modifier = Modifier.size(48.dp))
                            Text(text = "No images captured yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate700)
                            Text(text = "Tap 'SECURE CAPTURE' above to capture your first evidence photograph with detailed location and pincode.", style = MaterialTheme.typography.bodySmall, color = Slate500, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                items(recentEvidence.take(4)) { evidence ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDetails(evidence.verificationId) }
                            .shadow(2.dp, RoundedCornerShape(20.dp)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(EmeraldLight, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text(
                                        text = evidence.verificationId.take(16) + "...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Slate900,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = evidence.locationName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(evidence.trustedTimestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(EmeraldLight, RoundedCornerShape(50.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "VALID",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
