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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import org.geoverity.android.GeoVerityApp
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
    val db = GeoVerityApp.instance.database
    val pendingCount by db.offlineCaptureDao().getPendingCount().collectAsState(initial = 0)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelRoute = currentRoute in listOf(
        Screen.Home.route,
        Screen.SecureCapture.route,
        Screen.Gallery.route,
        Screen.OfflineCaptures.route,
        Screen.Settings.route
    )

    val navigateToTab: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(Screen.Home.route) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (isTopLevelRoute || currentRoute == null) {
                NavigationBar(
                    containerColor = WhiteBackground,
                    tonalElevation = 10.dp,
                    modifier = Modifier.shadow(12.dp)
                ) {
                    // 1. Home
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = { navigateToTab(Screen.Home.route) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPrimary,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = IndigoLight
                        )
                    )

                    // 2. Secure Capture
                    NavigationBarItem(
                        selected = currentRoute == Screen.SecureCapture.route,
                        onClick = { navigateToTab(Screen.SecureCapture.route) },
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Capture") },
                        label = { Text("Capture", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPrimary,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = IndigoLight
                        )
                    )

                    // 3. Local Gallery
                    NavigationBarItem(
                        selected = currentRoute == Screen.Gallery.route,
                        onClick = { navigateToTab(Screen.Gallery.route) },
                        icon = { Icon(Icons.Outlined.Collections, contentDescription = "Gallery") },
                        label = { Text("Gallery", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPrimary,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = IndigoLight
                        )
                    )

                    // 4. Auto-Sync Queue (with live badge!)
                    NavigationBarItem(
                        selected = currentRoute == Screen.OfflineCaptures.route,
                        onClick = { navigateToTab(Screen.OfflineCaptures.route) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingCount > 0) {
                                        Badge(containerColor = BrandAmber) {
                                            Text("$pendingCount", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = "Auto-Sync")
                            }
                        },
                        label = { Text("Auto-Sync", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPrimary,
                            selectedTextColor = BrandPrimary,
                            indicatorColor = IndigoLight
                        )
                    )

                    // 5. Security & Settings
                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = { navigateToTab(Screen.Settings.route) },
                        icon = { Icon(Icons.Default.Shield, contentDescription = "Security") },
                        label = { Text("Security", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
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
                    onNavigateToGallery = { navigateToTab(Screen.Gallery.route) },
                    onNavigateToVerify = { navController.navigate(Screen.VerifyImage.route) },
                    onNavigateToOffline = { navigateToTab(Screen.OfflineCaptures.route) },
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
                    onNavigateBack = { navigateToTab(Screen.Home.route) }
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
                    onNavigateBack = { navigateToTab(Screen.Home.route) }
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
                    onNavigateBack = { navigateToTab(Screen.Home.route) }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
