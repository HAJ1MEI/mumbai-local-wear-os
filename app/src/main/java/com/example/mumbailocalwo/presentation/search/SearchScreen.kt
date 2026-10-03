package com.example.mumbailocalwo.presentation.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.model.Station
import com.example.mumbailocalwo.data.repository.TimetableRepository
import com.example.mumbailocalwo.presentation.components.LineDot
import com.example.mumbailocalwo.presentation.components.MessageState
import com.example.mumbailocalwo.presentation.theme.PrimaryAccent
import kotlinx.coroutines.launch

/**
 * Screen 3: Search (From / To).
 * Centered, non-scrolling layout fitted to round Wear OS displays.
 * All controls including the Search button are 100% visible without scrolling.
 */
@Composable
fun SearchScreen(
    fromStation: Station?,
    toStation: Station?,
    onSelectFrom: () -> Unit,
    onSelectTo: () -> Unit,
    onSearch: (fromId: Int, toId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { TimetableRepository.getInstance(context) }

    var currentFrom by remember { mutableStateOf(fromStation) }
    var currentTo by remember { mutableStateOf(toStation) }
    var rotationAngle by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(targetValue = rotationAngle, label = "swap")

    var noDirectTrainsMessage by remember { mutableStateOf<String?>(null) }

    // Prefill from preferences if not already set
    LaunchedEffect(Unit) {
        if (currentFrom == null) {
            val lastFromId = repo.preferences.lastFromStationId?.toInt()
            if (lastFromId != null) {
                currentFrom = repo.getStationById(lastFromId)
            }
        }
        if (currentTo == null) {
            val lastToId = repo.preferences.lastToStationId?.toInt()
            if (lastToId != null) {
                currentTo = repo.getStationById(lastToId)
            }
        }
    }

    LaunchedEffect(fromStation) {
        if (fromStation != null) {
            currentFrom = fromStation
            noDirectTrainsMessage = null
        }
    }
    LaunchedEffect(toStation) {
        if (toStation != null) {
            currentTo = toStation
            noDirectTrainsMessage = null
        }
    }

    if (noDirectTrainsMessage != null) {
        MessageState(
            iconText = "ℹ",
            title = "No direct trains found",
            detail = noDirectTrainsMessage,
            primaryButtonText = "OK",
            onPrimaryClick = { noDirectTrainsMessage = null },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val isSearchEnabled = currentFrom != null && currentTo != null && currentFrom?.id != currentTo?.id

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.84f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "Search Trains",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // From Chip
            StationCompactChip(
                label = "From",
                station = currentFrom,
                placeholder = "Select origin",
                onClick = onSelectFrom
            )

            // Swap Button
            Box(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262626))
                    .clickable {
                        val temp = currentFrom
                        currentFrom = currentTo
                        currentTo = temp
                        rotationAngle += 180f
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⇅",
                    fontSize = 12.sp,
                    color = PrimaryAccent,
                    modifier = Modifier.rotate(animatedRotation)
                )
            }

            // To Chip
            StationCompactChip(
                label = "To",
                station = currentTo,
                placeholder = "Select destination",
                onClick = onSelectTo
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Search Button
            Button(
                onClick = {
                    val from = currentFrom ?: return@Button
                    val to = currentTo ?: return@Button
                    scope.launch {
                        val hasDirect = repo.hasDirectTrains(from.id, to.id)
                        if (hasDirect) {
                            repo.preferences.lastFromStationId = from.id.toLong()
                            repo.preferences.lastToStationId = to.id.toLong()
                            onSearch(from.id, to.id)
                        } else {
                            val interchange = repo.findInterchangeStation(from.id, to.id)
                            if (interchange != null) {
                                repo.preferences.lastFromStationId = from.id.toLong()
                                repo.preferences.lastToStationId = to.id.toLong()
                                onSearch(from.id, to.id)
                            } else {
                                noDirectTrainsMessage = "No train route found between ${from.name} and ${to.name}."
                            }
                        }
                    }
                },
                enabled = isSearchEnabled,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSearchEnabled) PrimaryAccent else Color(0xFF242424),
                    contentColor = if (isSearchEnabled) Color.Black else Color(0xFF888888)
                )
            ) {
                Text(
                    text = if (isSearchEnabled) "🔍 Search" else "Pick From & To",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StationCompactChip(
    label: String,
    station: Station?,
    placeholder: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(shape)
            .background(Color(0xFF1F1F1F))
            .border(1.dp, if (station != null) Color(0xFF444444) else Color(0xFF2C2C2C), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "$label: ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF888888)
            )
            Text(
                text = station?.name ?: placeholder,
                fontSize = 12.sp,
                fontWeight = if (station != null) FontWeight.Medium else FontWeight.Normal,
                color = if (station != null) Color.White else Color(0xFF666666),
                maxLines = 1
            )
        }

        if (station != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (line in station.lineCodes) {
                    LineDot(lineCode = line, size = 7.dp)
                }
            }
        }
    }
}
