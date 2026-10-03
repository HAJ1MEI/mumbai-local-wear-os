package com.example.mumbailocalwo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.live.model.LiveKind
import com.example.mumbailocalwo.live.model.LiveStatus
import com.example.mumbailocalwo.presentation.theme.LiveCancelledColor
import com.example.mumbailocalwo.presentation.theme.LiveLateHighColor
import com.example.mumbailocalwo.presentation.theme.LiveLateLowColor
import com.example.mumbailocalwo.presentation.theme.LiveOnTimeColor

sealed class LiveChipState {
    object Hidden : LiveChipState()
    object Loading : LiveChipState()
    data class Live(val status: LiveStatus) : LiveChipState()
    object NoData : LiveChipState()
    object Failed : LiveChipState()
}

/**
 * C2. LiveChip: One-line pill, 12 sp, height 20 dp.
 */
@Composable
fun LiveChip(
    state: LiveChipState,
    modifier: Modifier = Modifier
) {
    if (state is LiveChipState.Hidden) return

    val backgroundColor = Color(0xFF262626)

    Box(
        modifier = modifier
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is LiveChipState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Live…",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
            is LiveChipState.NoData -> {
                Text(
                    text = "No live data",
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
            is LiveChipState.Failed -> {
                Text(
                    text = "Live unavailable",
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
            is LiveChipState.Live -> {
                val status = state.status
                val dotColor = when {
                    status.kind == LiveKind.CANCELLED -> LiveCancelledColor
                    status.delayMinutes != null && status.delayMinutes == 0 -> LiveOnTimeColor
                    status.delayMinutes != null && status.delayMinutes in 1..5 -> LiveLateLowColor
                    status.delayMinutes != null && status.delayMinutes > 5 -> LiveLateHighColor
                    else -> LiveOnTimeColor
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (status.kind != LiveKind.CANCELLED) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = status.getChipText().removePrefix("● "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (status.kind == LiveKind.CANCELLED) LiveCancelledColor else Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            LiveChipState.Hidden -> {}
        }
    }
}
