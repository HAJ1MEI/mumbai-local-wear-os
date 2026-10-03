package com.example.mumbailocalwo.presentation.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.repository.TimetableRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen 11: Timetable Info.
 * Read-only diagnostics screen displaying metadata, train counts, and source attribution.
 */
@Composable
fun TimetableInfoScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { TimetableRepository.getInstance(context) }

    var metadata by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var lastCheckText by remember { mutableStateOf("Never") }

    LaunchedEffect(Unit) {
        metadata = repo.getTimetableMetadata()
        val lastCheck = repo.preferences.lastUpdateCheckMillis
        if (lastCheck > 0) {
            val zdt = Instant.ofEpochMilli(lastCheck).atZone(ZoneId.of("Asia/Kolkata"))
            lastCheckText = zdt.format(DateTimeFormatter.ofPattern("d MMM HH:mm"))
        }
    }

    val rawVersion = metadata["version"] ?: "20260928"
    val formattedVersion = if (rawVersion.length == 8) {
        try {
            LocalDate.parse(rawVersion, DateTimeFormatter.ofPattern("yyyyMMdd"))
                .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        } catch (_: Exception) { rawVersion }
    } else rawVersion

    TransformingLazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 36.dp,
            bottom = 44.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                text = "Timetable Info",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        item { InfoRow("App version", "1.0.0") }
        item { InfoRow("Timetable", formattedVersion) }
        item { InfoRow("Schema", metadata["schemaVersion"] ?: "1") }
        item { InfoRow("Trains", metadata["trainCount"] ?: "3,187") }
        item { InfoRow("Stations", metadata["stationCount"] ?: "142") }
        item { InfoRow("Stop events", metadata["stopEventCount"] ?: "53,387") }
        item { InfoRow("Last check", lastCheckText) }
        item { InfoRow("Source", "m-Indicator") }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFFB0B0B0))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}
