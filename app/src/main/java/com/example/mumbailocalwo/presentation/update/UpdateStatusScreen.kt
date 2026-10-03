package com.example.mumbailocalwo.presentation.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.mumbailocalwo.update.UpdateManager
import com.example.mumbailocalwo.update.UpdateState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Screen 10: Update Status.
 * Renders the timetable OTA update state machine with progress and status messages.
 */
@Composable
fun UpdateStatusScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager.getInstance(context) }
    val updateState by updateManager.state.collectAsState()

    fun formatVersionDate(raw: String): String {
        return if (raw.length == 8) {
            try {
                val date = LocalDate.parse(raw, DateTimeFormatter.ofPattern("yyyyMMdd"))
                date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
            } catch (_: Exception) {
                raw
            }
        } else raw
    }

    LaunchedEffect(Unit) {
        updateManager.checkForUpdatesAndApply()
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            updateManager.resetState()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = updateState) {
                is UpdateState.Idle, is UpdateState.Checking -> {
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

                is UpdateState.UpdateFound -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "New Timetable Found",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4FC3F7)
                    )
                    Text(
                        text = "v${formatVersionDate(state.newVersion)}",
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0B0)
                    )
                }

                is UpdateState.Downloading -> {
                    CircularProgressIndicator(
                        progress = { state.progressPercent / 100f },
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Downloading ${state.progressPercent}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    val mb = state.bytesDownloaded / (1024f * 1024f)
                    val totalMb = state.totalBytes / (1024f * 1024f)
                    Text(
                        text = String.format("%.1f / %.1f MB", mb, totalMb),
                        fontSize = 11.sp,
                        color = Color(0xFFB0B0B0)
                    )
                }

                is UpdateState.Validating -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Validating…",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                is UpdateState.Swapping -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Installing timetable…",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                is UpdateState.Success -> {
                    Text(text = "✓", fontSize = 34.sp, color = Color(0xFF66BB6A))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Timetable Updated",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Version ${formatVersionDate(state.version)}",
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0B0)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            updateManager.resetState()
                            onDone()
                        },
                        modifier = Modifier.fillMaxWidth(0.8f).height(40.dp)
                    ) {
                        Text(text = "Done", fontSize = 13.sp)
                    }
                }

                is UpdateState.UpToDate -> {
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
                        text = "Version ${formatVersionDate(state.currentVersion)}",
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0B0)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            updateManager.resetState()
                            onDone()
                        },
                        modifier = Modifier.fillMaxWidth(0.8f).height(40.dp)
                    ) {
                        Text(text = "Done", fontSize = 13.sp)
                    }
                }

                is UpdateState.NoNetwork -> {
                    Text(text = "⚠", fontSize = 30.sp, color = Color(0xFFFFCA28))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No Connection",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Current timetable active (${formatVersionDate(state.currentVersion)})",
                        fontSize = 11.sp,
                        color = Color(0xFFB0B0B0),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(0.88f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch { updateManager.checkForUpdatesAndApply() }
                            },
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text(text = "Retry", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                updateManager.resetState()
                                onDone()
                            },
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text(text = "Close", fontSize = 12.sp)
                        }
                    }
                }

                is UpdateState.Error -> {
                    Text(text = "⚠", fontSize = 30.sp, color = Color(0xFFE57373))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Update Failed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = state.message,
                        fontSize = 10.sp,
                        color = Color(0xFFB0B0B0),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(0.88f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (state.canRetry) {
                            Button(
                                onClick = {
                                    scope.launch { updateManager.checkForUpdatesAndApply() }
                                },
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text(text = "Retry", fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = {
                                updateManager.resetState()
                                onDone()
                            },
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text(text = "Close", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
