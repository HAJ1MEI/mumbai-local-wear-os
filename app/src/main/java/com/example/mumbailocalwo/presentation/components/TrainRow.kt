package com.example.mumbailocalwo.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.model.Train
import com.example.mumbailocalwo.presentation.theme.NotRunningAmberColor
import com.example.mumbailocalwo.presentation.theme.PrimaryAccent
import com.example.mumbailocalwo.presentation.theme.trainTypeColor

/**
 * C1. TrainRow: Wear OS Material 3 transformed card component for train lists.
 * Styled like the native Wear OS app drawer with dynamic center scaling and curved bezel morphing.
 */
@Composable
fun TrainRow(
    departureTime: String,
    train: Train,
    destinationText: String,
    platform: String?,
    isNext: Boolean,
    isPast: Boolean,
    isRunningToday: Boolean,
    countdownText: String?,
    liveState: LiveChipState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null
) {
    val isCancelled = (liveState as? LiveChipState.Live)?.status?.isCancelled == true
    val opacity = if (!isRunningToday || isCancelled) 0.55f else 1.0f
    val cardBackground = if (isNext) Color(0xFF162534) else Color(0xFF1E2024)
    val cardBorder = if (isNext) BorderStroke(1.5.dp, PrimaryAccent) else null

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(opacity),
        transformation = transformation,
        colors = CardDefaults.cardColors(
            containerColor = cardBackground,
            contentColor = Color.White
        ),
        border = cardBorder,
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Next banner header if isNext
            if (isNext && countdownText != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEXT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryAccent
                    )
                    Text(
                        text = countdownText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryAccent
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Line 1: Time (bold) + Type badge + Car count + AC
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = departureTime,
                        fontSize = if (isNext) 17.sp else 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LineDot(lineCode = train.lineCode, size = 8.dp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(
                        text = train.trainTypeDisplay,
                        color = trainTypeColor(train.trainType, train.isAC)
                    )
                    if (train.isAC) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Badge(text = "AC", color = Color(0xFF4FC3F7))
                    }
                    if (train.is15Car) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Badge(text = "15C", color = Color(0xFFFFA000))
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Line 2: Destination + Platform
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = destinationText,
                    fontSize = 12.sp,
                    color = Color(0xFFD6D6D6),
                    maxLines = 1
                )

                if (!platform.isNullOrBlank()) {
                    val cleanPf = if (platform.startsWith("PF", ignoreCase = true)) platform else "PF $platform"
                    Text(
                        text = cleanPf,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFB0B0B0)
                    )
                }
            }

            // Line 3: Live chip OR "Not running today" label
            if (!isRunningToday) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .border(1.dp, NotRunningAmberColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "Not running today",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = NotRunningAmberColor
                    )
                }
            } else if (liveState !is LiveChipState.Hidden) {
                Spacer(modifier = Modifier.height(3.dp))
                LiveChip(state = liveState)
            }
        }
    }
}

