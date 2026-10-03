package com.example.mumbailocalwo.presentation.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.repository.TimetableRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class UpdateState {
    CHECKING,
    UP_TO_DATE,
    UPDATE_FOUND,
    DOWNLOADING,
    VALIDATING,
    SWAPPING,
    UPDATED,
    NO_NETWORK,
    BAD_DOWNLOAD
}

/**
 * Screen 10: Update Status.
 * Renders the timetable update state machine with progress and status messages.
 */
@Composable
fun UpdateStatusScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { TimetableRepository.getInstance(context) }

    var currentState by remember { mutableStateOf(UpdateState.CHECKING) }
    var versionText by remember { mutableStateOf("28 Sep 2026") }

    LaunchedEffect(Unit) {
        val raw = repo.getTimetableVersion()
        if (raw.length == 8) {
            try {
                val date = LocalDate.parse(raw, DateTimeFormatter.ofPattern("yyyyMMdd"))
                versionText = date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
            } catch (_: Exception) {
                versionText = raw
            }
        }

        // Simulate update check
        delay(1200)
        currentState = UpdateState.UP_TO_DATE
        repo.preferences.lastUpdateCheckMillis = System.currentTimeMillis()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (currentState) {
                UpdateState.CHECKING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Checking for updates…",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                UpdateState.UP_TO_DATE -> {
                    Text(text = "✓", fontSize = 34.sp, color = Color(0xFF66BB6A))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Already up to date",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Version $versionText",
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0B0)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(0.8f).height(40.dp)
                    ) {
                        Text(text = "Done", fontSize = 13.sp)
                    }
                }

                UpdateState.NO_NETWORK -> {
                    Text(text = "⚠", fontSize = 34.sp, color = Color(0xFFFFCA28))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No Connection",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Your current timetable is unchanged.",
                        fontSize = 11.sp,
                        color = Color(0xFFB0B0B0),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth(0.8f).height(40.dp)) {
                        Text(text = "Close", fontSize = 13.sp)
                    }
                }

                else -> {
                    Text(text = "✓", fontSize = 34.sp, color = Color(0xFF66BB6A))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Timetable updated", fontSize = 14.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth(0.8f).height(40.dp)) {
                        Text(text = "Done", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
