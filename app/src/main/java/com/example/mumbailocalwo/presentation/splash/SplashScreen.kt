package com.example.mumbailocalwo.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Text
import com.example.mumbailocalwo.data.repository.TimetableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Screen 1: Splash / Database Initialization.
 * Checks and prepares timetable DB before any screen reads it.
 */
@Composable
fun SplashScreen(
    onReady: () -> Unit,
    onError: (reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val repo = TimetableRepository.getInstance(context)
                val db = repo.getDatabaseInstance()

                if (!db.databaseFileExists()) {
                    statusText = "Setting up timetable…"
                }

                val readableDb = db.getReadableDatabase()
                if (readableDb != null && readableDb.isOpen) {
                    val count = repo.getTrainCount()
                    if (count > 0) {
                        // Keep splash briefly for smooth visual transition
                        delay(200)
                        withContext(Dispatchers.Main) {
                            onReady()
                        }
                        return@withContext
                    }
                }
                withContext(Dispatchers.Main) {
                    onError("NO_DB")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("DB_CORRUPT")
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Thin circular progress arc along screen edge
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize().padding(2.dp),
            strokeWidth = 3.dp
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🚆",
                fontSize = 38.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Mumbai Local",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (statusText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = Color(0xFFB0B0B0)
                )
            }
        }
    }
}
