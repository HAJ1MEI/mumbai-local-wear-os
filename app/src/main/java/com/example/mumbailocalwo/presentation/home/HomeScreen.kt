package com.example.mumbailocalwo.presentation.home

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
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.repository.TimetableRepository
import com.example.mumbailocalwo.presentation.theme.NotRunningAmberColor
import com.example.mumbailocalwo.presentation.theme.PrimaryAccent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Screen 2: Home Screen.
 * Three full-width tiles: Search, Board Train, and Settings, plus timetable version footer.
 */
@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onBoardTrainClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var timetableVersion by remember { mutableStateOf("28 Sep 2026") }
    var isOutdated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val repo = TimetableRepository.getInstance(context)
        val rawVer = repo.getTimetableVersion()
        // Format YYYYMMDD into "28 Sep 2026"
        if (rawVer.length == 8) {
            try {
                val date = LocalDate.parse(rawVer, DateTimeFormatter.ofPattern("yyyyMMdd"))
                timetableVersion = date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
                val daysOld = ChronoUnit.DAYS.between(date, LocalDate.now())
                isOutdated = daysOld > 45
            } catch (_: Exception) {
                timetableVersion = rawVer
            }
        } else {
            timetableVersion = rawVer
        }
    }

    TransformingLazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 48.dp,
            bottom = 52.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Chip 1: Search
        item {
            Button(
                onClick = onSearchClick,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAccent,
                    contentColor = Color.Black
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔍", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Search",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // Chip 2: Board Train
        item {
            Button(
                onClick = onBoardTrainClick,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F),
                    contentColor = Color.White
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🚉", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Board Train",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        // Chip 3: Settings
        item {
            Button(
                onClick = onSettingsClick,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F),
                    contentColor = Color.White
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⚙", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Settings",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        // Footer Caption
        item {
            Spacer(modifier = Modifier.height(4.dp))
            val footerText = if (isOutdated) {
                "Timetable $timetableVersion · Check for update"
            } else {
                "Timetable $timetableVersion"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isOutdated, onClick = onUpdateClick)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = footerText,
                    fontSize = 11.sp,
                    color = if (isOutdated) NotRunningAmberColor else Color(0x99FFFFFF),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
