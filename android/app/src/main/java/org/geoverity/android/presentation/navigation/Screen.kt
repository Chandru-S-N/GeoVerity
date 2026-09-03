package org.geoverity.android.presentation.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object SecureCapture : Screen("capture", "Capture")
    object Gallery : Screen("gallery", "Gallery")
    object ImageViewer : Screen("viewer/{verificationId}", "Evidence Viewer")
    object CapturePreview : Screen("capture_preview", "Preview & Footer")
    object AuthResult : Screen("auth_result/{verificationId}", "Authentication Result")
    object VerifyImage : Screen("verify", "Verify")
    object OfflineCaptures : Screen("offline_captures", "Offline Captures")
    object EvidenceHistory : Screen("evidence_history", "Evidence History")
    object EvidenceDetails : Screen("evidence_details/{verificationId}", "Evidence Details")
    object ShareEvidence : Screen("share", "Share Evidence")
    object Settings : Screen("settings", "Settings")
    object About : Screen("about", "About GeoVerity")
}
