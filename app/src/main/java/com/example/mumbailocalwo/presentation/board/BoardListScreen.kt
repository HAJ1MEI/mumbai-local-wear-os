package com.example.mumbailocalwo.presentation.board

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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.model.BoardingTrain
import com.example.mumbailocalwo.data.model.Station
import com.example.mumbailocalwo.data.repository.TimetableRepository
import com.example.mumbailocalwo.live.LiveCache
import com.example.mumbailocalwo.live.model.LiveStatus
import com.example.mumbailocalwo.presentation.components.LineDot
import com.example.mumbailocalwo.presentation.components.LiveChipState
import com.example.mumbailocalwo.presentation.components.MessageState
import com.example.mumbailocalwo.presentation.components.RefreshButton
import com.example.mumbailocalwo.presentation.components.TrainFilter
import com.example.mumbailocalwo.presentation.components.TrainFilterBar
import com.example.mumbailocalwo.presentation.components.TrainRow
import kotlinx.coroutines.launch

/**
 * Screen 7: Board List.
 * Shows all trains departing a station in a specific direction on a specific line, with live overlays.
 */
@Composable
fun BoardListScreen(
    stationId: Int,
    lineCode: String,
    direction: String,
    onTrainClick: (trainId: Int, boardStationId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { TimetableRepository.getInstance(context) }
    val listState = rememberTransformingLazyColumnState()

    var station by remember { mutableStateOf<Station?>(null) }
    var allTrains by remember { mutableStateOf<List<BoardingTrain>>(emptyList()) }
    var filter by remember { mutableStateOf<TrainFilter?>(null) }

    var isLiveLoading by remember { mutableStateOf(false) }
    var lastLiveFetchMillis by remember { mutableLongStateOf(0L) }
    var liveStatuses by remember { mutableStateOf<Map<String, LiveStatus>>(emptyMap()) }
    var liveFailed by remember { mutableStateOf(false) }

    fun refreshLive(st: Station?) {
        val stationCode = st?.liveCode ?: st?.code ?: return
        if (!repo.liveClient.isLineSupported(lineCode)) return

        scope.launch {
            isLiveLoading = true
            liveFailed = false
            val result = repo.liveClient.fetchAllLiveTrains(lineCode, stationCode)
            if (result.isSuccess) {
                liveStatuses = result.getOrNull() ?: emptyMap()
                lastLiveFetchMillis = System.currentTimeMillis()
            } else {
                liveFailed = true
            }
            isLiveLoading = false
        }
    }

    LaunchedEffect(stationId, lineCode, direction) {
        val st = repo.getStationById(stationId)
        station = st
        val list = repo.getBoardingTrains(stationId, lineCode, direction)
        allTrains = list

        // Check if cache already has statuses
        val cached = LiveCache.getBulk(lineCode, st?.liveCode ?: st?.code ?: "")
        if (cached != null) {
            liveStatuses = cached.statuses
            lastLiveFetchMillis = cached.fetchedAtMillis
        } else if (repo.preferences.autoLiveEnabled) {
            // One automatic live fetch
            refreshLive(st)
        }

        // Auto-scroll to the next train
        val nextIdx = list.indexOfFirst { it.isNext }
        if (nextIdx != -1) {
            // Offset for header items (title + refresh row)
            scope.launch {
                listState.animateScrollToItem(nextIdx + 2)
            }
        }
    }

    val filteredList = remember(allTrains, filter) {
        if (filter == null) {
            allTrains
        } else {
            allTrains.filter { filter!!.matches(it.train) }
        }
    }

    val isLineLiveSupported = repo.liveClient.isLineSupported(lineCode)

    Box(modifier = modifier.fillMaxSize()) {
        TransformingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 10.dp,
                end = 10.dp,
                top = 44.dp,
                bottom = 58.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Station + Direction + LineDot
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${station?.name ?: ""} Board",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        LineDot(lineCode = lineCode, size = 8.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$lineCode Line · $direction",
                            fontSize = 11.sp,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                }
            }

            // Controls: RefreshButton + Count
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLineLiveSupported) {
                        RefreshButton(
                            isLoading = isLiveLoading,
                            lastFetchedMillis = lastLiveFetchMillis,
                            onRefresh = { refreshLive(station) }
                        )
                    } else {
                        Text(
                            text = "Schedule only",
                            fontSize = 11.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }

                    Text(
                        text = "${filteredList.size} trains",
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }

            // Empty state
            if (filteredList.isEmpty()) {
                item {
                    MessageState(
                        iconText = "🚉",
                        title = "No trains found",
                        detail = if (filter != null) "No ${filter?.name?.lowercase()} trains match." else "No more trains scheduled today."
                    )
                }
            }

            // Train rows
            items(filteredList, key = { "train_${it.train.id}_${it.departureMinutes}" }) { item ->
                val trainNumber = item.train.trainNumber
                val status = if (trainNumber != null) {
                    liveStatuses[trainNumber]
                        ?: liveStatuses[trainNumber.trim()]
                        ?: liveStatuses[trainNumber.trim().trimStart('0')]
                } else null

                val liveState = when {
                    !isLineLiveSupported -> LiveChipState.Hidden
                    isLiveLoading -> LiveChipState.Loading
                    liveFailed -> LiveChipState.Failed
                    status != null -> LiveChipState.Live(status)
                    lastLiveFetchMillis > 0 -> LiveChipState.NoData
                    else -> LiveChipState.Hidden
                }

                TrainRow(
                    departureTime = item.departureTime,
                    train = item.train,
                    destinationText = "→ ${item.destinationName}",
                    platform = item.platform,
                    isNext = item.isNext,
                    isPast = item.isPast,
                    isRunningToday = item.isRunningToday,
                    countdownText = item.countdownText,
                    liveState = liveState,
                    onClick = {
                        onTrainClick(item.train.id, stationId)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        TrainFilterBar(
            selectedFilter = filter,
            onFilterSelected = { filter = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

