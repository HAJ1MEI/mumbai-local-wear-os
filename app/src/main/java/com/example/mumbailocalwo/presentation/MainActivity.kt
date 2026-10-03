package com.example.mumbailocalwo.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.mumbailocalwo.data.model.Station
import com.example.mumbailocalwo.presentation.board.BoardListScreen
import com.example.mumbailocalwo.presentation.detail.TrainDetailScreen
import com.example.mumbailocalwo.presentation.direction.DirectionScreen
import com.example.mumbailocalwo.presentation.error.ErrorRecoveryScreen
import com.example.mumbailocalwo.presentation.home.HomeScreen
import com.example.mumbailocalwo.presentation.info.TimetableInfoScreen
import com.example.mumbailocalwo.presentation.search.SearchResultsScreen
import com.example.mumbailocalwo.presentation.search.SearchScreen
import com.example.mumbailocalwo.presentation.search.StationPickerScreen
import com.example.mumbailocalwo.presentation.settings.SettingsScreen
import com.example.mumbailocalwo.presentation.splash.SplashScreen
import com.example.mumbailocalwo.presentation.theme.MumbaiLocalWOTheme
import com.example.mumbailocalwo.presentation.update.UpdateStatusScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MumbaiLocalApp()
        }
    }
}

@Composable
fun MumbaiLocalApp() {
    MumbaiLocalWOTheme {
        AppScaffold {
            val navController = rememberSwipeDismissableNavController()

            // State shared between Search and Station Picker
            var selectedFromStation by remember { mutableStateOf<Station?>(null) }
            var selectedToStation by remember { mutableStateOf<Station?>(null) }

            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "splash"
            ) {
                // Screen 1: Splash / DB Init
                composable("splash") {
                    SplashScreen(
                        onReady = {
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        },
                        onError = { reason ->
                            navController.navigate("error/$reason") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    )
                }

                // Screen 2: Home
                composable("home") {
                    HomeScreen(
                        onSearchClick = { navController.navigate("search") },
                        onBoardTrainClick = { navController.navigate("picker/BOARD") },
                        onSettingsClick = { navController.navigate("settings") },
                        onUpdateClick = { navController.navigate("update") }
                    )
                }

                // Screen 3: Search (From / To)
                composable("search") {
                    SearchScreen(
                        fromStation = selectedFromStation,
                        toStation = selectedToStation,
                        onSelectFrom = { navController.navigate("picker/FROM") },
                        onSelectTo = { navController.navigate("picker/TO") },
                        onSearch = { fromId, toId ->
                            navController.navigate("results/$fromId/$toId")
                        }
                    )
                }

                // Screen 4: Station Picker
                composable("picker/{purpose}") { backStackEntry ->
                    val purpose = backStackEntry.arguments?.getString("purpose") ?: "FROM"
                    StationPickerScreen(
                        purpose = purpose,
                        onStationChosen = { station ->
                            when (purpose.uppercase()) {
                                "FROM" -> {
                                    selectedFromStation = station
                                    navController.popBackStack()
                                }
                                "TO" -> {
                                    selectedToStation = station
                                    navController.popBackStack()
                                }
                                "BOARD" -> {
                                    navController.navigate("direction/${station.id}")
                                }
                                else -> navController.popBackStack()
                            }
                        }
                    )
                }

                // Screen 5: Search Results
                composable("results/{fromId}/{toId}") { backStackEntry ->
                    val fromId = backStackEntry.arguments?.getString("fromId")?.toIntOrNull() ?: 0
                    val toId = backStackEntry.arguments?.getString("toId")?.toIntOrNull() ?: 0
                    SearchResultsScreen(
                        fromStationId = fromId,
                        toStationId = toId,
                        onTrainClick = { trainId, from, to ->
                            navController.navigate("train/$trainId?board=$from&alight=$to")
                        }
                    )
                }

                // Screen 6: Direction Selector
                composable("direction/{stationId}") { backStackEntry ->
                    val stationId = backStackEntry.arguments?.getString("stationId")?.toIntOrNull() ?: 0
                    DirectionScreen(
                        stationId = stationId,
                        onDirectionSelected = { sId, line, dir ->
                            navController.navigate("board/$sId/$line/$dir")
                        }
                    )
                }

                // Screen 7: Board List
                composable("board/{stationId}/{lineCode}/{direction}") { backStackEntry ->
                    val stationId = backStackEntry.arguments?.getString("stationId")?.toIntOrNull() ?: 0
                    val lineCode = backStackEntry.arguments?.getString("lineCode") ?: "C"
                    val direction = backStackEntry.arguments?.getString("direction") ?: "UP"
                    BoardListScreen(
                        stationId = stationId,
                        lineCode = lineCode,
                        direction = direction,
                        onTrainClick = { trainId, boardStationId ->
                            navController.navigate("train/$trainId?board=$boardStationId")
                        }
                    )
                }

                // Screen 8: Train Details
                composable(
                    route = "train/{trainId}?board={boardStationId}&alight={alightStationId}",
                    arguments = listOf(
                        androidx.navigation.navArgument("trainId") { type = androidx.navigation.NavType.IntType },
                        androidx.navigation.navArgument("boardStationId") {
                            type = androidx.navigation.NavType.StringType
                            defaultValue = "-1"
                        },
                        androidx.navigation.navArgument("alightStationId") {
                            type = androidx.navigation.NavType.StringType
                            defaultValue = "-1"
                        }
                    )
                ) { backStackEntry ->
                    val trainId = backStackEntry.arguments?.getInt("trainId") ?: 0
                    val boardId = backStackEntry.arguments?.getString("boardStationId")?.toIntOrNull()?.takeIf { it != -1 }
                    val alightId = backStackEntry.arguments?.getString("alightStationId")?.toIntOrNull()?.takeIf { it != -1 }
                    TrainDetailScreen(
                        trainId = trainId,
                        boardStationId = boardId,
                        alightStationId = alightId
                    )
                }

                // Screen 9: Settings
                composable("settings") {
                    SettingsScreen(
                        onCheckForUpdates = { navController.navigate("update") },
                        onTimetableInfo = { navController.navigate("info") }
                    )
                }

                // Screen 10: Update Status
                composable("update") {
                    UpdateStatusScreen(
                        onDone = { navController.popBackStack() }
                    )
                }

                // Screen 11: Timetable Info
                composable("info") {
                    TimetableInfoScreen()
                }

                // Screen 12: Error / Recovery
                composable("error/{reason}") { backStackEntry ->
                    val reason = backStackEntry.arguments?.getString("reason") ?: "NO_DB"
                    ErrorRecoveryScreen(
                        reason = reason,
                        onRetry = {
                            navController.navigate("splash") {
                                popUpTo("error/$reason") { inclusive = true }
                            }
                        },
                        onDownload = {
                            navController.navigate("update")
                        }
                    )
                }
            }
        }
    }
}