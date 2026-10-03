package com.example.mumbailocalwo.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.model.StopEvent
import com.example.mumbailocalwo.data.model.Train
import com.example.mumbailocalwo.data.repository.TimetableRepository
import com.example.mumbailocalwo.live.LiveCache
import com.example.mumbailocalwo.live.model.LiveStatus
import com.example.mumbailocalwo.live.model.SingleTrainLiveInfo
import com.example.mumbailocalwo.presentation.components.Badge
import com.example.mumbailocalwo.presentation.components.LineDot
import com.example.mumbailocalwo.presentation.components.RefreshButton
import com.example.mumbailocalwo.presentation.theme.NotRunningAmberColor
import com.example.mumbailocalwo.presentation.theme.PrimaryAccent
import com.example.mumbailocalwo.presentation.theme.trainTypeColor
import com.example.mumbailocalwo.presentation.util.ServiceDayUtil
import kotlinx.coroutines.launch

import com.example.mumbailocalwo.live.LiveStatusParser
import com.example.mumbailocalwo.live.model.LiveKind
import java.util.Locale

/**
 * Screen 8: Train Details.
 * Full stop schedule timeline with boarding/alighting highlights and live tracking banner.
 */
@Composable
fun TrainDetailScreen(
    trainId: Int,
    boardStationId: Int? = null,
    alightStationId: Int? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { TimetableRepository.getInstance(context) }
    val listState = rememberTransformingLazyColumnState()

    var train by remember { mutableStateOf<Train?>(null) }
    var stops by remember { mutableStateOf<List<StopEvent>>(emptyList()) }
    var cachedStatus by remember { mutableStateOf<LiveStatus?>(null) }
    var detailedLiveInfo by remember { mutableStateOf<SingleTrainLiveInfo?>(null) }
    var isLiveRefreshing by remember { mutableStateOf(false) }

    fun refreshSingleTrain() {
        val tn = train?.trainNumber ?: return
        scope.launch {
            isLiveRefreshing = true
            val res = repo.liveClient.fetchSingleTrainInfo(tn)
            if (res.isSuccess) {
                detailedLiveInfo = res.getOrNull()
            }
            isLiveRefreshing = false
        }
    }

    LaunchedEffect(trainId) {
        val t = repo.getTrainById(trainId)
        train = t
        val s = repo.getTrainStops(trainId)
        stops = s

        if (t?.trainNumber != null) {
            cachedStatus = LiveCache.getStatusForTrain(t.trainNumber)
            detailedLiveInfo = LiveCache.getSingleTrainInfo(t.trainNumber)

            // Auto-fetch fresh live tracking if enabled
            if (repo.preferences.autoLiveEnabled && repo.liveClient.isLineSupported(t.lineCode)) {
                refreshSingleTrain()
            }
        }

        // Auto-scroll so board station is centered
        val boardIdx = s.indexOfFirst { it.stationId == boardStationId }
        if (boardIdx != -1) {
            scope.launch {
                listState.animateScrollToItem(boardIdx + 2)
            }
        }
    }

    val currentTrain = train ?: return
    val runsToday = ServiceDayUtil.runsToday(currentTrain.operatingDays)
    val isLiveSupported = repo.liveClient.isLineSupported(currentTrain.lineCode)

    val livePosition = remember(stops, cachedStatus, detailedLiveInfo) {
        resolveLiveTrainPosition(stops, cachedStatus, detailedLiveInfo)
    }

    // Auto-scroll to train's live location if not focused on a specific board station
    LaunchedEffect(livePosition) {
        if (boardStationId == null && livePosition != null) {
            val targetIdx = when (livePosition) {
                is LiveTrainPosition.AtStation -> livePosition.stopIndex
                is LiveTrainPosition.BetweenStations -> livePosition.fromStopIndex
            }
            listState.animateScrollToItem((targetIdx + 2).coerceAtLeast(0))
        }
    }

    TransformingLazyColumn(
        state = listState,
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
        // Header Block
        item {
            Column(
                modifier = Modifier.fillMaxWidth(0.88f).padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentTrain.trainNumber ?: "Local",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LineDot(lineCode = currentTrain.lineCode, size = 10.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Badge(
                        text = currentTrain.trainTypeDisplay,
                        color = trainTypeColor(currentTrain.trainType, currentTrain.isAC)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentTrain.isAC) Badge(text = "AC", color = Color(0xFF4FC3F7))
                    if (currentTrain.is15Car) Badge(text = "15C", color = Color(0xFFFFA000))
                    Badge(text = currentTrain.direction, color = Color(0xFFB0B0B0))
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${currentTrain.sourceStationName ?: ""} → ${currentTrain.destStationName ?: ""}",
                    fontSize = 13.sp,
                    color = Color.White
                )

                Text(
                    text = "${currentTrain.departureTime ?: ""} → ${currentTrain.arrivalTime ?: ""}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFB0B0B0)
                )

                if (!runsToday) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, NotRunningAmberColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Not running today",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NotRunningAmberColor
                        )
                    }
                }
            }
        }

        // Live Banner (if live supported and train runs today)
        if (isLiveSupported && runsToday) {
            item {
                LiveBanner(
                    cachedStatus = cachedStatus,
                    detailedInfo = detailedLiveInfo,
                    isRefreshing = isLiveRefreshing,
                    onRefresh = { refreshSingleTrain() }
                )
            }
        }

        // Timeline Stops with Live Position highlighting
        itemsIndexed(stops, key = { _, stop -> "stop_${stop.id}" }) { index, stop ->
            val isBoard = stop.stationId == boardStationId
            val isAlight = stop.stationId == alightStationId
            val isFirst = index == 0
            val isLast = index == stops.size - 1

            val isTrainHere = livePosition is LiveTrainPosition.AtStation && livePosition.stopIndex == index

            TimelineRow(
                stop = stop,
                isBoard = isBoard,
                isAlight = isAlight,
                isTrainHere = isTrainHere,
                isFirst = isFirst,
                isLast = isLast
            )

            // If train is between this station and the next, render live transit marker
            if (livePosition is LiveTrainPosition.BetweenStations && livePosition.fromStopIndex == index) {
                BetweenLiveMarker(description = livePosition.description)
            }
        }

        // Notes block at bottom
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Runs: ${currentTrain.operatingDays}",
                    fontSize = 11.sp,
                    color = Color(0xFF888888)
                )
                if (currentTrain.isLadiesSpecial) {
                    Text(
                        text = "Ladies Special",
                        fontSize = 11.sp,
                        color = Color(0xFFFF80AB)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

sealed class LiveTrainPosition {
    data class AtStation(val stopIndex: Int, val stationName: String) : LiveTrainPosition()
    data class BetweenStations(
        val fromStopIndex: Int,
        val toStopIndex: Int,
        val description: String
    ) : LiveTrainPosition()
}

private fun findStopIndex(stops: List<StopEvent>, rawName: String): Int {
    if (rawName.isBlank()) return -1
    val clean = rawName.trim().lowercase(Locale.ROOT)
        .replace(" junction", "")
        .replace(" jn", "")
        .trim()

    // 1. Exact match on name or code
    val exact = stops.indexOfFirst { stop ->
        val sName = stop.stationName?.trim()?.lowercase(Locale.ROOT) ?: ""
        val sCode = stop.stationCode?.trim()?.lowercase(Locale.ROOT) ?: ""
        sName == clean || sCode == clean ||
        sName.replace(" junction", "").replace(" jn", "").trim() == clean
    }
    if (exact != -1) return exact

    // 2. Starts with / prefix match
    val prefix = stops.indexOfFirst { stop ->
        val sName = stop.stationName?.trim()?.lowercase(Locale.ROOT) ?: ""
        (sName.isNotBlank() && clean.startsWith(sName)) || (clean.isNotBlank() && sName.startsWith(clean))
    }
    if (prefix != -1) return prefix

    // 3. Contains match
    return stops.indexOfFirst { stop ->
        val sName = stop.stationName?.trim()?.lowercase(Locale.ROOT) ?: ""
        (sName.isNotBlank() && clean.contains(sName)) || (clean.isNotBlank() && sName.contains(clean))
    }
}

private fun resolveLiveTrainPosition(
    stops: List<StopEvent>,
    status: LiveStatus?,
    singleInfo: SingleTrainLiveInfo?
): LiveTrainPosition? {
    if (stops.isEmpty()) return null

    val effectiveStatus = if (singleInfo != null && !singleInfo.message.isNullOrBlank()) {
        LiveStatusParser.parse(singleInfo.message)
    } else {
        status
    }

    val rawLocation = effectiveStatus?.location ?: singleInfo?.station
    if (rawLocation.isNullOrBlank()) return null

    val kind = effectiveStatus?.kind ?: LiveKind.AT

    when (kind) {
        LiveKind.AT, LiveKind.REACHING -> {
            val idx = findStopIndex(stops, rawLocation)
            if (idx != -1) {
                return LiveTrainPosition.AtStation(idx, stops[idx].stationName ?: rawLocation)
            }
        }
        LiveKind.BETWEEN -> {
            val parts = rawLocation.split("-")
            if (parts.size >= 2) {
                val idx1 = findStopIndex(stops, parts[0])
                val idx2 = findStopIndex(stops, parts[1])
                if (idx1 != -1 && idx2 != -1) {
                    val fromIdx = minOf(idx1, idx2)
                    val toIdx = maxOf(idx1, idx2)
                    val name1 = stops[fromIdx].stationName ?: parts[0].trim()
                    val name2 = stops[toIdx].stationName ?: parts[1].trim()
                    return LiveTrainPosition.BetweenStations(fromIdx, toIdx, "$name1 & $name2")
                } else if (idx1 != -1) {
                    val nextIdx = (idx1 + 1).coerceAtMost(stops.size - 1)
                    val name1 = stops[idx1].stationName ?: parts[0].trim()
                    return LiveTrainPosition.BetweenStations(idx1, nextIdx, "$name1 & ${parts[1].trim()}")
                } else if (idx2 != -1) {
                    val prevIdx = (idx2 - 1).coerceAtLeast(0)
                    val name2 = stops[idx2].stationName ?: parts[1].trim()
                    return LiveTrainPosition.BetweenStations(prevIdx, idx2, "${parts[0].trim()} & $name2")
                }
            } else {
                val idx = findStopIndex(stops, rawLocation)
                if (idx != -1) {
                    val nextIdx = (idx + 1).coerceAtMost(stops.size - 1)
                    return LiveTrainPosition.BetweenStations(idx, nextIdx, "Near ${stops[idx].stationName ?: rawLocation}")
                }
            }
        }
        LiveKind.CROSSED -> {
            val idx = findStopIndex(stops, rawLocation)
            if (idx != -1) {
                val nextIdx = (idx + 1).coerceAtMost(stops.size - 1)
                return LiveTrainPosition.BetweenStations(idx, nextIdx, "Past ${stops[idx].stationName ?: rawLocation}")
            }
        }
        else -> {
            val idx = findStopIndex(stops, rawLocation)
            if (idx != -1) {
                return LiveTrainPosition.AtStation(idx, stops[idx].stationName ?: rawLocation)
            }
        }
    }
    return null
}

@Composable
private fun BetweenLiveMarker(
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.88f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2E2200))
            .border(1.dp, Color(0xFFFFA000), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFA000)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🚆", fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = description,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD54F),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LiveBanner(
    cachedStatus: LiveStatus?,
    detailedInfo: SingleTrainLiveInfo?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .clip(shape)
            .background(Color(0xFF1E2830))
            .border(1.dp, Color(0xFF004D6B), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val msg = detailedInfo?.message
                    ?: cachedStatus?.getChipText()
                    ?: "No live tracking data yet"

                Text(
                    text = msg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                if (detailedInfo != null && detailedInfo.passengerCount > 0) {
                    Text(
                        text = "${detailedInfo.passengerCount} passengers tracking",
                        fontSize = 10.sp,
                        color = Color(0xFF4FC3F7)
                    )
                } else {
                    Text(
                        text = "Tap ⟳ to refresh live position",
                        fontSize = 10.sp,
                        color = Color(0xFF888888)
                    )
                }
            }

            RefreshButton(
                isLoading = isRefreshing,
                lastFetchedMillis = detailedInfo?.fetchedAtMillis ?: cachedStatus?.fetchedAtMillis,
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
private fun TimelineRow(
    stop: StopEvent,
    isBoard: Boolean,
    isAlight: Boolean,
    isTrainHere: Boolean,
    isFirst: Boolean,
    isLast: Boolean
) {
    val isHighlighted = isBoard || isAlight
    val textColor = when {
        isTrainHere -> Color(0xFF00E676)
        isHighlighted -> PrimaryAccent
        else -> Color.White
    }
    val textWeight = if (isHighlighted || isTrainHere) FontWeight.Bold else FontWeight.Normal

    val rowModifier = if (isTrainHere) {
        Modifier
            .fillMaxWidth(0.88f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D3322))
            .border(1.dp, Color(0xFF00E676), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp)
    } else {
        Modifier
            .fillMaxWidth(0.88f)
            .height(38.dp)
            .padding(horizontal = 4.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicator dot / train marker
        Box(
            modifier = Modifier.width(26.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isTrainHere -> {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚆", fontSize = 12.sp)
                    }
                }
                isHighlighted -> {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(PrimaryAccent)
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0xFF888888), CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Station name
        Text(
            text = stop.stationName ?: stop.stationCode ?: "",
            fontSize = 13.sp,
            fontWeight = textWeight,
            color = textColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Time
        val timeDisplay = stop.departure ?: stop.arrival ?: ""
        Text(
            text = timeDisplay,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = textWeight,
            color = textColor
        )

        // Platform
        if (!stop.platform.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            val cleanPf = if (stop.platform.startsWith("PF", ignoreCase = true)) stop.platform else "PF ${stop.platform}"
            Text(
                text = cleanPf,
                fontSize = 11.sp,
                color = if (isTrainHere) Color(0xFFB9F6CA) else Color(0xFFB0B0B0)
            )
        }
    }
}
