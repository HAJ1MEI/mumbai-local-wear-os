package com.example.mumbailocalwo.presentation.direction

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.model.Station
import com.example.mumbailocalwo.data.model.StationDirectionOption
import com.example.mumbailocalwo.data.repository.TimetableRepository
import com.example.mumbailocalwo.presentation.components.LineDot
import com.example.mumbailocalwo.presentation.components.MessageState

/**
 * Screen 6: Direction Selector (Board Train, step 2).
 * Grouped by railway line with commuter-friendly direction labels ("Towards CSMT").
 */
@Composable
fun DirectionScreen(
    stationId: Int,
    onDirectionSelected: (stationId: Int, lineCode: String, direction: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { TimetableRepository.getInstance(context) }

    var station by remember { mutableStateOf<Station?>(null) }
    var directions by remember { mutableStateOf<List<StationDirectionOption>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(stationId) {
        station = repo.getStationById(stationId)
        directions = repo.getStationDirections(stationId)
        isLoading = false
    }

    if (!isLoading && directions.isEmpty()) {
        MessageState(
            iconText = "ℹ",
            title = "No trains found",
            detail = "This station has no departing trains in the timetable.",
            modifier = modifier.fillMaxSize()
        )
        return
    }

    // Group direction options by lineCode
    val grouped = directions.groupBy { it.lineCode }
    val showLineHeaders = grouped.size > 1

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
                text = station?.name ?: "Select Direction",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        grouped.forEach { (lineCode, options) ->
            if (showLineHeaders) {
                item {
                    val lineName = options.firstOrNull()?.lineName ?: lineCode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .padding(top = 4.dp, bottom = 2.dp, start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LineDot(lineCode = lineCode, size = 8.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = lineName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                }
            }

            items(options, key = { "${it.lineCode}_${it.directionFlag}" }) { option ->
                DirectionChip(
                    option = option,
                    onClick = {
                        onDirectionSelected(stationId, option.lineCode, option.directionFlag)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DirectionChip(
    option: StationDirectionOption,
    onClick: () -> Unit
) {
    val arrowSymbol = when (option.arrow) {
        "UP" -> "◀"
        "DOWN" -> "▶"
        else -> "↔"
    }

    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .clip(shape)
            .background(Color(0xFF1F1F1F))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = arrowSymbol,
                fontSize = 14.sp,
                color = Color(0xFF4FC3F7)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = option.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                lineHeight = 16.sp
            )
        }
    }
}
