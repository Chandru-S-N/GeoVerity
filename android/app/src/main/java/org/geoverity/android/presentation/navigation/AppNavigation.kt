package org.geoverity.android.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import org.geoverity.android.presentation.screens.about.AboutScreen
import org.geoverity.android.presentation.screens.capture.AuthenticationResultScreen
import org.geoverity.android.presentation.screens.capture.CapturePreviewScreen
import org.geoverity.android.presentation.screens.capture.SecureCaptureScreen
import org.geoverity.android.presentation.screens.gallery.GalleryScreen
import org.geoverity.android.presentation.screens.gallery.ImageViewerScreen
import org.geoverity.android.presentation.screens.history.EvidenceDetailsScreen
import org.geoverity.android.presentation.screens.history.EvidenceHistoryScreen
import org.geoverity.android.presentation.screens.home.HomeScreen
import org.geoverity.android.presentation.screens.offline.OfflineCapturesScreen
import org.geoverity.android.presentation.screens.settings.SettingsScreen
import org.geoverity.android.presentation.screens.share.ShareEvidenceScreen
import org.geoverity.android.presentation.screens.verify.VerifyImageScreen
import org.geoverity.android.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(navController: NavHostController) {
    var showMoreSheet by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelRoute = currentRoute in listOf(
        Screen.Home.route,
        Screen.SecureCapture.route,
        Screen.Gallery.route,
        Screen.VerifyImage.route
    )

    Scaffold(
        bottomBar = {
            if (isTopLevelRoute || currentRoute == null) {
                NavigationBar(
                    containerColor = WhiteBackground,
                    tonalElevation = 8.dp,
                    modifier = Modifier.shadow(8.dp)
                ) {
                    // 1. Home
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = { navController.navigate(Screen.Home.route) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPrimary,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = IndigoLight
                        )
                    )

                    // 2. Secure Capture
                    NavigationBarItem(
                        selected = currentRoute == Screen.SecureCapture.route,
                        onClick = { navController.navigate(Screen.SecureCapture.route) },
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Capture") },
                        label = { Text("Capture", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPrimary,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = IndigoLight
                        )
                    )

                    // 3. Local Gallery
                    NavigationBarItem(
                        selected = currentRoute == Screen.Gallery.route,
                        onClick = { navController.navigate(Screen.Gallery.route) },
                        icon = { Icon(Icons.Outlined.Collections, contentDescription = "Gallery") },
                        label = { Text("Gallery", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPrimary,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = IndigoLight
                        )
                    )

                    // 4. Verify Image
                    NavigationBarItem(
                        selected = currentRoute == Screen.VerifyImage.route,
                        onClick = { navController.navigate(Screen.VerifyImage.route) },
                        icon = { Icon(Icons.Default.Verified, contentDescription = "Verify") },
                        label = { Text("Verify", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPrimary,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = IndigoLight
                        )
                    )

                    // 5. More
                    NavigationBarItem(
                        selected = showMoreSheet,
                        onClick = { showMoreSheet = true },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                        label = { Text("More", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPrimary,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = IndigoLight
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToCapture = { navController.navigate(Screen.SecureCapture.route) },
                    onNavigateToGallery = { navController.navigate(Screen.Gallery.route) },
                    onNavigateToVerify = { navController.navigate(Screen.VerifyImage.route) },
                    onNavigateToOffline = { navController.navigate(Screen.OfflineCaptures.route) },
                    onNavigateToHistory = { navController.navigate(Screen.EvidenceHistory.route) },
                    onNavigateToDetails = { vId -> navController.navigate("viewer/$vId") }
                )
            }

            composable(Screen.SecureCapture.route) {
                SecureCaptureScreen(
                    onPhotoCaptured = { photoPath, locName, lat, lon ->
                        navController.navigate("capture_preview?path=$photoPath&loc=$locName&lat=$lat&lon=$lon")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Gallery.route) {
                GalleryScreen(
                    onNavigateToViewer = { vId -> navController.navigate("viewer/$vId") },
                    onNavigateToCapture = { navController.navigate(Screen.SecureCapture.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "viewer/{verificationId}",
                arguments = listOf(navArgument("verificationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val vId = backStackEntry.arguments?.getString("verificationId") ?: ""
                ImageViewerScreen(
                    verificationId = vId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "capture_preview?path={path}&loc={loc}&lat={lat}&lon={lon}",
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType },
                    navArgument("loc") { type = NavType.StringType },
                    navArgument("lat") { type = NavType.FloatType },
                    navArgument("lon") { type = NavType.FloatType }
                )
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString("path") ?: ""
                val loc = backStackEntry.arguments?.getString("loc") ?: "Thanthonimalai, Karur - 639005, Tamil Nadu, India"
                val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 10.785234
                val lon = backStackEntry.arguments?.getFloat("lon")?.toDouble() ?: 78.125432

                CapturePreviewScreen(
                    rawPhotoPath = path,
                    locationName = loc,
                    latitude = lat,
                    longitude = lon,
                    onAuthenticationComplete = { vId ->
                        navController.navigate("auth_result/$vId") {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "auth_result/{verificationId}",
                arguments = listOf(navArgument("verificationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val vId = backStackEntry.arguments?.getString("verificationId") ?: ""
                AuthenticationResultScreen(
                    verificationId = vId,
                    onNavigateHome = { navController.navigate(Screen.Home.route) },
                    onNavigateToHistory = { navController.navigate(Screen.Gallery.route) }
                )
            }

            composable(Screen.VerifyImage.route) {
                VerifyImageScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.OfflineCaptures.route) {
                OfflineCapturesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.EvidenceHistory.route) {
                EvidenceHistoryScreen(
                    onNavigateToDetails = { vId -> navController.navigate("viewer/$vId") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "evidence_details/{verificationId}",
                arguments = listOf(navArgument("verificationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val vId = backStackEntry.arguments?.getString("verificationId") ?: ""
                EvidenceDetailsScreen(
                    verificationId = vId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ShareEvidence.route) {
                ShareEvidenceScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // More Menu Bottom Sheet
        if (showMoreSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMoreSheet = false },
                containerColor = WhiteBackground,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Application Menu",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )

                    MoreMenuItem("Local Image Gallery", Icons.Outlined.Collections, IndigoLight, BrandIndigo) {
                        showMoreSheet = false
                        navController.navigate(Screen.Gallery.route)
                    }

                    MoreMenuItem("Offline Captures & Auto-Sync", Icons.Outlined.CloudQueue, AmberLight, BrandAmber) {
                        showMoreSheet = false
                        navController.navigate(Screen.OfflineCaptures.route)
                    }

                    MoreMenuItem("Evidence History Registry", Icons.Outlined.History, IndigoLight, BrandIndigo) {
                        showMoreSheet = false
                        navController.navigate(Screen.EvidenceHistory.route)
                    }

                    MoreMenuItem("Share Evidence Guide", Icons.Outlined.Share, EmeraldLight, BrandEmerald) {
                        showMoreSheet = false
                        navController.navigate(Screen.ShareEvidence.route)
                    }

                    MoreMenuItem("Settings & Server URL", Icons.Outlined.Settings, Slate100, Slate700) {
                        showMoreSheet = false
                        navController.navigate(Screen.Settings.route)
                    }

                    MoreMenuItem("About GeoVerity", Icons.Outlined.Info, IndigoLight, BrandPrimary) {
                        showMoreSheet = false
                        navController.navigate(Screen.About.route)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    tintColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(bgColor, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(22.dp))
        }
        Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Slate900)
    }
}
