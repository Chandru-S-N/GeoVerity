package org.geoverity.android.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = WhiteBackground,
    primaryContainer = IndigoLight,
    onPrimaryContainer = IndigoDark,
    secondary = BrandPurple,
    onSecondary = WhiteBackground,
    background = WhiteBackground,
    onBackground = Slate900,
    surface = WhiteBackground,
    onSurface = Slate900,
    surfaceVariant = Slate50,
    onSurfaceVariant = Slate700,
    outline = Slate200,
    error = BrandRose,
    onError = WhiteBackground
)

@Composable
fun GeoVerityTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
