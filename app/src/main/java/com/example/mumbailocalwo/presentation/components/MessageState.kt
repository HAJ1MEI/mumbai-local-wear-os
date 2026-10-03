package com.example.mumbailocalwo.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text

/**
 * C6. MessageState: Centered icon, title, detail, and optional action buttons.
 * Used for empty, error, and recovery states across screens.
 */
@Composable
fun MessageState(
    iconText: String,
    title: String,
    detail: String? = null,
    primaryButtonText: String? = null,
    onPrimaryClick: (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = iconText,
            fontSize = 32.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        if (!detail.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail,
                fontSize = 12.sp,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )
        }

        if (primaryButtonText != null && onPrimaryClick != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onPrimaryClick,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(text = primaryButtonText, fontSize = 13.sp)
            }
        }

        if (secondaryButtonText != null && onSecondaryClick != null) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onSecondaryClick,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(text = secondaryButtonText, fontSize = 13.sp)
            }
        }
    }
}
