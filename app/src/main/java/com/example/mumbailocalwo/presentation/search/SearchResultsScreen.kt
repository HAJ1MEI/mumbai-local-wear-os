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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.mumbailocalwo.presentation.theme.PrimaryAccent
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
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.model.SearchResult
import com.example.mumbailocalwo.data.model.Station
import com.example.mumbailocalwo.data.repository.TimetableRepository
import com.example.mumbailocalwo.live.LiveCache
import com.example.mumbailocalwo.live.model.LiveStatus
import com.example.mumbailocalwo.presentation.components.FilterSheet
import com.example.mumbailocalwo.presentation.components.LiveChipState
import com.example.mumbailocalwo.presentation.components.MessageState
import com.example.mumbailocalwo.presentation.components.RefreshButton
import com.example.mumbailocalwo.presentation.components.TrainFilter
import com.example.mumbailocalwo.presentation.components.TrainRow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

/**
 * Screen 5: Search Results (live).
 * Lists all direct trains between two stations for the service day, auto-scrolled to the next train.
 */
@Composable
fun SearchResultsScreen(
    fromStationId: Int,
    toStationId: Int,
    onTrainClick: (trainId: Int, fromId: Int, toId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { TimetableRepository.getInstance(context) }
    val listState = rememberTransformingLazyColumnState()

    var fromStation by remember { mutableStateOf<Station?>(null) }
    var toStation by remember { mutableStateOf<Station?>(null) }
    var interchangeStation by remember { mutableStateOf<Station?>(null) }
    var selectedLeg by remember { mutableIntStateOf(1) } // 1: Origin -> Interchange, 2: Interchange -> Destination
    var allResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var filter by remember { mutableStateOf(TrainFilter.ALL) }
    var showFilterSheet by remember { mutableStateOf(false) }

    var isLiveLoading by remember { mutableStateOf(false) }
    var lastLiveFetchMillis by remember { mutableLongStateOf(0L) }
    var liveStatuses by remember { mutableStateOf<Map<String, LiveStatus>>(emptyMap()) }
    var liveFailed by remember { mutableStateOf(false) }

    fun refreshLive(fromSt: Station?, lines: Set<String>) {
        val stationCode = fromSt?.liveCode ?: fromSt?.code ?: return
        val supportedLines = lines.filter { repo.liveClient.isLineSupported(it) }
        if (supportedLines.isEmpty()) return

        scope.launch {
            isLiveLoading = true
            liveFailed = false
            try {
                // Fetch in parallel for each line passing through this station
                val combined = mutableMapOf<String, LiveStatus>()
                val jobs = supportedLines.map { line ->
                    async {
                        val res = repo.liveClient.fetchAllLiveTrains(line, stationCode)
                        res.getOrNull() ?: emptyMap()
                    }
                }
                val results = jobs.awaitAll()
                for (res in results) {
                    combined.putAll(res)
                }
                liveStatuses = combined
                lastLiveFetchMillis = System.currentTimeMillis()
            } catch (e: Exception) {
                liveFailed = true
            }
            isLiveLoading = false
        }
    }

    LaunchedEffect(fromStationId, toStationId, selectedLeg) {
        val from = repo.getStationById(fromStationId)
        val to = repo.getStationById(toStationId)
        fromStation = from
        toStation = to

        val hasDirect = repo.hasDirectTrains(fromStationId, toStationId)
        val via = if (!hasDirect) repo.findInterchangeStation(fromStationId, toStationId) else null
        interchangeStation = via

        val currentFromId = if (via != null && selectedLeg == 2) via.id else fromStationId
        val currentToId = if (via != null && selectedLeg == 1) via.id else toStationId
        val activeOrigin = if (via != null && selectedLeg == 2) via else from

        val list = repo.findDirectTrains(currentFromId, currentToId)
        allResults = list

        val lines = list.map { it.train.lineCode }.toSet()
        if (repo.preferences.autoLiveEnabled) {
            refreshLive(activeOrigin, lines)
        }

        // Auto-scroll to next train
        val nextIdx = list.indexOfFirst { it.isNext }
        if (nextIdx != -1) {
            scope.launch {
                val headerOffset = if (via != null) 3 else 2
                listState.animateScrollToItem(nextIdx + headerOffset)
            }
        }
    }

    if (showFilterSheet) {
        FilterSheet(
            currentFilter = filter,
            onFilterSelected = {
                filter = it
                showFilterSheet = false
            }
        )
        return
    }

    val filteredList = remember(allResults, filter) {
        when (filter) {
            TrainFilter.ALL -> allResults
            TrainFilter.AC_ONLY -> allResults.filter { it.train.isAC }
            TrainFilter.FAST_ONLY -> allResults.filter { it.train.trainType.equals("FAST", ignoreCase = true) }
            TrainFilter.SLOW_ONLY -> allResults.filter { it.train.trainType.equals("SLOW", ignoreCase = true) }
        }
    }

    val anyLineLiveSupported = allResults.any { repo.liveClient.isLineSupported(it.train.lineCode) }

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
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${fromStation?.name ?: ""} → ${toStation?.name ?: ""}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (interchangeStation != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Change at ${interchangeStation?.name ?: "Dadar"}",
                        fontSize = 11.sp,
                        color = Color(0xFF4FC3F7),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Connecting Leg Tabs (if interchange)
        if (interchangeStation != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val viaName = interchangeStation?.name ?: "Dadar"
                    val destName = toStation?.name ?: ""

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedLeg == 1) PrimaryAccent else Color(0xFF242424))
                            .clickable { selectedLeg = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1: To $viaName",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedLeg == 1) Color.Black else Color(0xFFB0B0B0),
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedLeg == 2) PrimaryAccent else Color(0xFF242424))
                            .clickable { selectedLeg = 2 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "2: To $destName",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedLeg == 2) Color.Black else Color(0xFFB0B0B0),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Controls
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (anyLineLiveSupported) {
                    RefreshButton(
                        isLoading = isLiveLoading,
                        lastFetchedMillis = lastLiveFetchMillis,
                        onRefresh = {
                            val lines = allResults.map { it.train.lineCode }.toSet()
                            refreshLive(fromStation, lines)
                        }
                    )
                } else {
                    Text(text = "Schedule only", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF262626))
                        .clickable { showFilterSheet = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val filterLabel = when (filter) {
                        TrainFilter.ALL -> "All ▾"
                        TrainFilter.AC_ONLY -> "AC ▾"
                        TrainFilter.FAST_ONLY -> "Fast ▾"
                        TrainFilter.SLOW_ONLY -> "Slow ▾"
                    }
                    Text(
                        text = filterLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4FC3F7)
                    )
                }
            }
        }

        // Empty state
        if (filteredList.isEmpty()) {
            item {
                MessageState(
                    iconText = "🔍",
                    title = "No trains found",
                    detail = if (filter != TrainFilter.ALL) "No trains match the filter." else "No direct trains scheduled today."
                )
            }
        }

        // Train rows
        items(filteredList, key = { "result_${it.train.id}_${it.departureMinutes}" }) { item ->
            val trainNumber = item.train.trainNumber
            val status = if (trainNumber != null) {
                liveStatuses[trainNumber]
                    ?: liveStatuses[trainNumber.trim()]
                    ?: liveStatuses[trainNumber.trim().trimStart('0')]
            } else null
            val isSupported = repo.liveClient.isLineSupported(item.train.lineCode)

            val liveState = when {
                !isSupported -> LiveChipState.Hidden
                isLiveLoading -> LiveChipState.Loading
                liveFailed -> LiveChipState.Failed
                status != null -> LiveChipState.Live(status)
                lastLiveFetchMillis > 0 -> LiveChipState.NoData
                else -> LiveChipState.Hidden
            }

            TrainRow(
                departureTime = item.departureTime,
                train = item.train,
                destinationText = "Arr ${item.arrivalTime} · ${item.durationMinutes}m",
                platform = item.fromPlatform,
                isNext = item.isNext,
                isPast = item.isPast,
                isRunningToday = item.isRunningToday,
                countdownText = item.countdownText,
                liveState = liveState,
                onClick = {
                    val actualFrom = if (interchangeStation != null && selectedLeg == 2) interchangeStation!!.id else fromStationId
                    val actualTo = if (interchangeStation != null && selectedLeg == 1) interchangeStation!!.id else toStationId
                    onTrainClick(item.train.id, actualFrom, actualTo)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
