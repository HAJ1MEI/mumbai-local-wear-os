package com.example.mumbailocalwo.presentation.search

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.model.Station
import com.example.mumbailocalwo.data.repository.TimetableRepository
import com.example.mumbailocalwo.presentation.components.LineDot
import kotlinx.coroutines.launch

/**
 * Screen 4: Station Picker (shared for FROM, TO, and BOARD).
 * Lists recent stations, all stations alphabetically, and line indicators.
 */
@Composable
fun StationPickerScreen(
    purpose: String, // "FROM", "TO", "BOARD"
    onStationChosen: (Station) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { TimetableRepository.getInstance(context) }

    var allStations by remember { mutableStateOf<List<Station>>(emptyList()) }
    var recentStations by remember { mutableStateOf<List<Station>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    val screenTitle = when (purpose.uppercase()) {
        "FROM" -> "Select From"
        "TO" -> "Select To"
        "BOARD" -> "Board at"
        else -> "Select Station"
    }

    LaunchedEffect(Unit) {
        val stations = repo.getAllStations()
        allStations = stations

        val recentIds = repo.preferences.getRecentStationIds().map { it.toInt() }
        if (recentIds.isNotEmpty()) {
            recentStations = repo.getStationsByIds(recentIds)
        }
    }

    val filteredStations = remember(searchQuery, allStations) {
        if (searchQuery.isBlank()) {
            allStations
        } else {
            allStations.filter { st ->
                st.name.contains(searchQuery, ignoreCase = true) ||
                        st.code.contains(searchQuery, ignoreCase = true) ||
                        (st.marathiName != null && st.marathiName.contains(searchQuery))
            }
        }
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                text = screenTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Recent Section (only when not searching and recent stations exist)
        if (searchQuery.isBlank() && recentStations.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Recent",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4FC3F7)
                    )
                }
            }

            items(recentStations, key = { "recent_${it.id}" }) { station ->
                StationPickerRow(
                    station = station,
                    onClick = {
                        scope.launch {
                            repo.preferences.addRecentStation(station.id.toLong())
                            onStationChosen(station)
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "All Stations",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0B0B0)
                    )
                }
            }
        }

        // All stations list
        items(filteredStations, key = { "station_${it.id}" }) { station ->
            StationPickerRow(
                station = station,
                onClick = {
                    scope.launch {
                        repo.preferences.addRecentStation(station.id.toLong())
                        onStationChosen(station)
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StationPickerRow(
    station: Station,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .clip(shape)
            .background(Color(0xFF1F1F1F))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            if (!station.marathiName.isNullOrBlank()) {
                Text(
                    text = station.marathiName,
                    fontSize = 10.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (line in station.lineCodes) {
                LineDot(lineCode = line, size = 8.dp)
            }
        }
    }
}
