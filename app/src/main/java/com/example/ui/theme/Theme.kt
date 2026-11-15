package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ChatQivoLightColorScheme = lightColorScheme(
    primary = ChatQivoTeal,
    onPrimary = ChatQivoWhite,
    primaryContainer = ChatQivoMintSurface,
    onPrimaryContainer = ChatQivoTealDark,
    secondary = ChatQivoBlueAccent,
    onSecondary = ChatQivoWhite,
    secondaryContainer = ChatQivoBlueSurface,
    onSecondaryContainer = ChatQivoBlueInk,
    tertiary = ChatQivoPurpleInk,
    tertiaryContainer = ChatQivoPurpleSurface,
    background = ChatQivoWhite,
    onBackground = ChatQivoInk,
    surface = ChatQivoWhite,
    onSurface = ChatQivoInk,
    surfaceVariant = ChatQivoSoft,
    onSurfaceVariant = ChatQivoSubtle,
    outline = ChatQivoLine,
    outlineVariant = ChatQivoInputBorder,
    error = ChatQivoErrorInk,
    errorContainer = ChatQivoErrorSurface,
    onErrorContainer = ChatQivoErrorInk
)

@Composable
fun ChatQivoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChatQivoLightColorScheme,
        typography = ChatQivoTypography,
        content = content
    )
}
