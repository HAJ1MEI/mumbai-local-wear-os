package com.example.mumbailocalwo.presentation.error

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mumbailocalwo.presentation.components.MessageState

/**
 * Screen 12: Error / Recovery.
 * Shown when no usable timetable database can be loaded.
 */
@Composable
fun ErrorRecoveryScreen(
    reason: String,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (reason) {
        "DB_CORRUPT" -> "Timetable damaged"
        "SCHEMA" -> "Timetable incompatible"
        else -> "Timetable unavailable"
    }

    val detail = when (reason) {
        "DB_CORRUPT" -> "The saved timetable is unreadable. Try restoring or downloading."
        "SCHEMA" -> "This timetable needs a different app version."
        else -> "No timetable found. Tap retry to restore bundled data."
    }

    MessageState(
        iconText = "⚠",
        title = title,
        detail = detail,
        primaryButtonText = "Retry",
        onPrimaryClick = onRetry,
        secondaryButtonText = "Download timetable",
        onSecondaryClick = onDownload,
        modifier = modifier.fillMaxSize()
    )
}
