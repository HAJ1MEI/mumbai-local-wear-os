package com.example.mumbailocalwo.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text

/**
 * C4. Badge: 12 sp text in a rounded outline.
 * Examples: F, S, SF, AC, 12C, 15C, MEMU.
 */
@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFB0B0B0)
) {
    Box(
        modifier = modifier
            .border(width = 1.dp, color = color.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
