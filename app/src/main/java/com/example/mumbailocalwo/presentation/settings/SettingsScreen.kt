package com.example.mumbailocalwo.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.repository.TimetableRepository
import com.example.mumbailocalwo.presentation.components.MessageState
import com.example.mumbailocalwo.presentation.theme.PrimaryAccent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Screen 9: Settings.
 * Displays timetable date, update trigger, auto-load toggle, timetable info, and clear recents.
 */
@Composable
fun SettingsScreen(
    onCheckForUpdates: () -> Unit,
    onTimetableInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { TimetableRepository.getInstance(context) }

    var timetableDateText by remember { mutableStateOf("28 Sep 2026") }
    var autoLiveEnabled by remember { mutableStateOf(repo.preferences.autoLiveEnabled) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val raw = repo.preferences.timetableVersion.ifBlank { "20260928" }
                timetableDateText = if (raw.length == 8) {
                    try {
                        val date = LocalDate.parse(raw, DateTimeFormatter.ofPattern("yyyyMMdd"))
                        date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
                    } catch (_: Exception) {
                        raw
                    }
                } else raw
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showClearConfirm) {
        MessageState(
            iconText = "🗑",
            title = "Clear recent stations?",
            detail = "This will reset your recent search history.",
            primaryButtonText = "Clear",
            onPrimaryClick = {
                repo.preferences.clearRecents()
                showClearConfirm = false
            },
            secondaryButtonText = "Cancel",
            onSecondaryClick = { showClearConfirm = false },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    TransformingLazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 10.dp,
            end = 10.dp,
            top = 48.dp,
            bottom = 52.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text(
                text = "Settings",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Timetable version info card (not clickable)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1F1F1F))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "Timetable",
                        fontSize = 11.sp,
                        color = Color(0xFFB0B0B0)
                    )
                    Text(
                        text = timetableDateText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        // Check for updates
        item {
            Button(
                onClick = onCheckForUpdates,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF262626),
                    contentColor = Color.White
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔄", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Check for updates", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Auto-load live status toggle
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1F1F1F))
                    .clickable {
                        val newValue = !autoLiveEnabled
                        autoLiveEnabled = newValue
                        repo.preferences.autoLiveEnabled = newValue
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-load live status",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "Fetch once on list open",
                            fontSize = 10.sp,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (autoLiveEnabled) Color(0xFF004D6B) else Color(0xFF333333))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (autoLiveEnabled) "ON" else "OFF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (autoLiveEnabled) Color(0xFF4FC3F7) else Color(0xFF9E9E9E)
                        )
                    }
                }
            }
        }

        // Timetable info
        item {
            Button(
                onClick = onTimetableInfo,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F),
                    contentColor = Color.White
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "ℹ", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Timetable info", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Clear recents
        item {
            Button(
                onClick = { showClearConfirm = true },
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F),
                    contentColor = Color(0xFFE57373)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🗑", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clear recents",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE57373)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
