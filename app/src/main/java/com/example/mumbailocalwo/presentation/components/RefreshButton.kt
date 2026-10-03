package com.example.mumbailocalwo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Text
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * C3. RefreshButton: 40 dp circular icon button (⟳) with debounced tap and status caption.
 */
@Composable
fun RefreshButton(
    isLoading: Boolean,
    lastFetchedMillis: Long?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lastTapMillis by remember { mutableLongStateOf(0L) }

    val caption = when {
        isLoading -> "Updating…"
        lastFetchedMillis != null && lastFetchedMillis > 0 -> {
            val timeStr = Instant.ofEpochMilli(lastFetchedMillis)
                .atZone(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("HH:mm"))
            "Live $timeStr"
        }
        else -> "Not loaded"
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF262626))
                .clickable(enabled = !isLoading) {
                    val now = System.currentTimeMillis()
                    // 5-second debounce
                    if (now - lastTapMillis > 5000L) {
                        lastTapMillis = now
                        onRefresh()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "⟳",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = caption,
            fontSize = 11.sp,
            color = Color(0xFF9E9E9E)
        )
    }
}
