package org.geoverity.android.presentation.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object SecureCapture : Screen("capture", "Secure Capture")
    object CapturePreview : Screen("capture_preview", "Preview & Footer")
    object AuthResult : Screen("auth_result", "Authentication Result")
    object VerifyImage : Screen("verify", "Verify Evidence")
    object VerificationResult : Screen("verify_result", "Verification Result")
    object OfflineCaptures : Screen("offline_captures", "Offline Captures")
    object EvidenceHistory : Screen("evidence_history", "Evidence History")
    object EvidenceDetails : Screen("evidence_details", "Evidence Details")
    object ShareEvidence : Screen("share", "Share Evidence")
    object Settings : Screen("settings", "Settings")
    object About : Screen("about", "About GeoVerity")
}
