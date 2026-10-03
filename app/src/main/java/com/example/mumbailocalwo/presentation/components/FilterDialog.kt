package com.example.mumbailocalwo.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text

enum class TrainFilter {
    ALL,
    AC_ONLY,
    FAST_ONLY,
    SLOW_ONLY
}

/**
 * Filter selection sheet for train lists.
 */
@Composable
fun FilterSheet(
    currentFilter: TrainFilter,
    onFilterSelected: (TrainFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Filter Trains",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        val options = listOf(
            TrainFilter.ALL to "All trains",
            TrainFilter.FAST_ONLY to "Fast only",
            TrainFilter.SLOW_ONLY to "Slow only",
            TrainFilter.AC_ONLY to "AC only"
        )

        for ((filter, label) in options) {
            val isSelected = filter == currentFilter
            Button(
                onClick = { onFilterSelected(filter) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF004D6B) else Color(0xFF262626),
                    contentColor = if (isSelected) Color(0xFF4FC3F7) else Color.White
                )
            ) {
                Text(
                    text = if (isSelected) "✓ $label" else label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
