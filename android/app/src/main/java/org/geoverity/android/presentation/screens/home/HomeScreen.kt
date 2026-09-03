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
import org.geoverity.android.data.db.EvidenceHistoryEntity
import org.geoverity.android.presentation.theme.*

@Composable
fun HomeScreen(
    onNavigateToCapture: () -> Unit,
    onNavigateToVerify: () -> Unit,
    onNavigateToOffline: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val db = GeoVerityApp.instance.database
    val totalCount by db.evidenceHistoryDao().getTotalAuthenticatedCount().collectAsState(initial = 0)
    val pendingCount by db.offlineCaptureDao().getPendingCount().collectAsState(initial = 0)
    val recentEvidence by db.evidenceHistoryDao().getRecentEvidence().collectAsState(initial = emptyList())

    Scaffold(
        containerColor = Slate50
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            // 1. App Header & Brand Banner (Pure White Card with Indigo Gradient Accent)
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate200, Slate100))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(28.dp), spotColor = BrandIndigo.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
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
                                    contentContent = "Logo",
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
                                    text = "Secure Digital Evidence Authentication",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate500
                                )
                            }
                        }

                        // Status Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Server Status Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .background(EmeraldLight, RoundedCornerShape(50.dp))
                                    .border(1.dp, BrandEmerald.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(BrandEmerald, CircleShape)
                                )
                                Text(
                                    text = "Server Connected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // GPS Status Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .background(IndigoLight, RoundedCornerShape(50.dp))
                                    .border(1.dp, BrandIndigo.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(BrandIndigo, CircleShape)
                                )
                                Text(
                                    text = "GPS Available",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IndigoDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Primary CTA: [ CAPTURE IMAGE ] (Vibrant Gradient Card Button)
            item {
                Button(
                    onClick = onNavigateToCapture,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = BrandPrimary.copy(alpha = 0.4f))
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
                            text = "SECURE CAPTURE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // 3. Stats Cards Grid (Colorful White Cards)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Authenticated Records Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToHistory() }
                            .shadow(2.dp, RoundedCornerShape(24.dp)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate200, Slate100)))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(EmeraldLight, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "$totalCount", style = MaterialTheme.typography.headlineMedium, color = Slate900)
                            Text(text = "Authenticated", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        }
                    }

                    // Pending Offline Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToOffline() }
                            .shadow(2.dp, RoundedCornerShape(24.dp)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate200, Slate100)))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(AmberLight, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "$pendingCount", style = MaterialTheme.typography.headlineMedium, color = if (pendingCount > 0) BrandAmber else Slate900)
                            Text(text = "Pending Offline", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        }
                    }
                }
            }

            // 4. Recent Evidence Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Evidence",
                        style = MaterialTheme.typography.titleLarge,
                        color = Slate900,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToHistory) {
                        Text(text = "View All", color = BrandPrimary, fontWeight = FontWeight.SemiBold)
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Slate300, modifier = Modifier.size(40.dp))
                            Text(text = "No evidence captured yet", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        }
                    }
                }
            } else {
                items(recentEvidence) { evidence ->
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(IndigoLight, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text(
                                        text = evidence.verificationId.take(14) + "...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Slate900,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = evidence.locationName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate500
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(EmeraldLight, RoundedCornerShape(50.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Authenticated",
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
