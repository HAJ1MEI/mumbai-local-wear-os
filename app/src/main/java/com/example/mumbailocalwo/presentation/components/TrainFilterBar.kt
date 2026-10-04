package com.example.mumbailocalwo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.model.Train

/**
 * Filter modes for train lists.
 */
enum class TrainFilter {
    SLOW,
    FAST,
    AC;

    /**
     * Checks if a train matches the filter criteria:
     * - SLOW: matches Slow trains (including Slow AC trains).
     * - FAST: matches Fast / Semi-Fast trains (including Fast AC trains).
     * - AC: matches all AC trains (both Slow and Fast).
     */
    fun matches(train: Train): Boolean {
        return when (this) {
            SLOW -> (train.trainType.equals("SLOW", ignoreCase = true) ||
                    train.trainType.startsWith("S", ignoreCase = true)) &&
                    !train.trainType.contains("FAST", ignoreCase = true)
            FAST -> train.trainType.contains("FAST", ignoreCase = true) ||
                    train.trainType.startsWith("F", ignoreCase = true) ||
                    train.trainType.equals("EXPRESS", ignoreCase = true)
            AC -> train.isAC
        }
    }
}

/**
 * Floating 3-button filter bar pinned at the bottom of train list screens.
 * Options: Slow, Fast, AC. Only one can be active at a time.
 * Tapping the active option toggles it off back to all trains.
 */
@Composable
fun TrainFilterBar(
    selectedFilter: TrainFilter?,
    onFilterSelected: (TrainFilter?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xEE1C1C1E))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterButton(
                label = "Slow",
                isSelected = selectedFilter == TrainFilter.SLOW,
                selectedBg = Color(0xFF0284C7), // Blue
                onClick = {
                    onFilterSelected(if (selectedFilter == TrainFilter.SLOW) null else TrainFilter.SLOW)
                }
            )
            FilterButton(
                label = "Fast",
                isSelected = selectedFilter == TrainFilter.FAST,
                selectedBg = Color(0xFFF59E0B), // Amber
                onClick = {
                    onFilterSelected(if (selectedFilter == TrainFilter.FAST) null else TrainFilter.FAST)
                }
            )
            FilterButton(
                label = "AC",
                isSelected = selectedFilter == TrainFilter.AC,
                selectedBg = Color(0xFF10B981), // Emerald
                onClick = {
                    onFilterSelected(if (selectedFilter == TrainFilter.AC) null else TrainFilter.AC)
                }
            )
        }
    }
}

@Composable
private fun FilterButton(
    label: String,
    isSelected: Boolean,
    selectedBg: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(50.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (isSelected) selectedBg else Color(0xFF2C2C2E))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else Color(0xFFCCCCCC),
            textAlign = TextAlign.Center
        )
    }
}
