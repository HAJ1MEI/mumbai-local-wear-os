package com.example.mumbailocalwo.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

// Core Colors per Section 2.3 of Mumbai Local Wear UI Design
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF1F1F1F)
val DarkSurfaceCard = Color(0xFF262626)
val PrimaryAccent = Color(0xFF4FC3F7)

// Line colors
val CentralLineColor = Color(0xFF1E88E5)
val WesternLineColor = Color(0xFFE53935)
val HarbourLineColor = Color(0xFF43A047)
val TransHarbourLineColor = Color(0xFF8E24AA)
val UranLineColor = Color(0xFFFB8C00)
val DvpLineColor = Color(0xFF00ACC1)

// Live status colors
val LiveOnTimeColor = Color(0xFF66BB6A)
val LiveLateLowColor = Color(0xFFFFCA28)
val LiveLateHighColor = Color(0xFFFF7043)
val LiveCancelledColor = Color(0xFFEF5350)
val UnavailableDimColor = Color(0x999E9E9E) // 60% opacity grey
val NotRunningAmberColor = Color(0xFFFFB300)

// Train type colors
val FastTrainColor = Color(0xFFE53935)
val SlowTrainColor = Color(0xFF4CAF50)
val SemiFastTrainColor = Color(0xFFFFA000)
val ACTrainColor = Color(0xFF4FC3F7)
val ExpressTrainColor = Color(0xFF9C27B0)

fun lineColor(lineCode: String): Color {
    return when (lineCode.uppercase()) {
        "C", "CENTRAL" -> CentralLineColor
        "W", "WESTERN" -> WesternLineColor
        "H", "HARBOUR" -> HarbourLineColor
        "T", "TRANS_HARBOUR" -> TransHarbourLineColor
        "U", "URAN" -> UranLineColor
        "DVP", "DIVA_VASAI_PANVEL" -> DvpLineColor
        else -> CentralLineColor
    }
}

fun trainTypeColor(trainType: String, isAC: Boolean): Color {
    if (isAC) return ACTrainColor
    return when (trainType.uppercase()) {
        "FAST" -> FastTrainColor
        "SLOW" -> SlowTrainColor
        "SEMI_FAST" -> SemiFastTrainColor
        "EXPRESS" -> ExpressTrainColor
        "MEMU" -> Color(0xFF8D6E63)
        "PASSENGER" -> Color(0xFF78909C)
        else -> SlowTrainColor
    }
}

@Composable
fun MumbaiLocalWOTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = ColorScheme(
        primary = PrimaryAccent,
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF004D6B),
        onPrimaryContainer = Color.White,
        secondary = CentralLineColor,
        onSecondary = Color.White,
        secondaryContainer = DarkSurface,
        onSecondaryContainer = Color.White,
        tertiary = LiveOnTimeColor,
        onTertiary = Color.Black,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFB0B0B0),
        error = LiveCancelledColor,
        onError = Color.White,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}