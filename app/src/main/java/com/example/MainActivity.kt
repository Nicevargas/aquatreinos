package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppNavTab
import com.example.ui.components.AppTopBar
import com.example.ui.components.BottomNavBar
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveWorkoutExecutionScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WorkoutsScreen
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AquagendaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AquagendaApp()
            }
        }
    }
}

@Composable
fun AquagendaApp(
    viewModel: AquagendaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show user notification snackbar when needed
    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    if (uiState.liveWorkout.isOpen) {
        // Full screen Live Workout Execution Mode
        LiveWorkoutExecutionScreen(
            workout = uiState.currentWorkout,
            liveState = uiState.liveWorkout,
            onCloseClick = { viewModel.closeLiveWorkout() },
            onTogglePauseClick = { viewModel.togglePauseTimer() },
            onNextSetClick = { viewModel.advanceToNextSet() }
        )
    } else {
        // Main App Layout with TopBar, Content and BottomNav
        Scaffold(
            topBar = {
                AppTopBar(
                    isExecutionMode = false,
                    showUserAvatar = uiState.selectedTab == AppNavTab.PROFILE,
                    onNotificationClick = {
                        viewModel.selectToday()
                    }
                )
            },
            bottomBar = {
                BottomNavBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = AquaBackground,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = uiState.selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                        AppNavTab.HOME -> {
                            HomeScreen(
                                workout = uiState.currentWorkout,
                                selectedLevel = uiState.selectedLevel,
                                calendarDays = uiState.calendarDays,
                                stopwatchState = uiState.stopwatch,
                                onDayClick = { viewModel.selectDay(it) },
                                onLevelChange = { viewModel.selectLevel(it) },
                                onStartWorkoutClick = { viewModel.startLiveWorkout() },
                                onViewWorkoutDetails = { viewModel.selectTab(AppNavTab.WORKOUTS) },
                                onToggleStopwatch = { viewModel.toggleStopwatch() },
                                onLapStopwatch = { viewModel.recordSetLap() },
                                onResetStopwatch = { viewModel.resetStopwatch() },
                                onStopwatchModeChange = { viewModel.setStopwatchMode(it) },
                                onStopwatchPrevSet = { viewModel.decrementStopwatchSet() },
                                onStopwatchNextSet = { viewModel.incrementStopwatchSet() }
                            )
                        }

                        AppNavTab.WORKOUTS -> {
                            WorkoutsScreen(
                                workout = uiState.currentWorkout,
                                onStartWorkoutClick = { viewModel.startLiveWorkout() }
                            )
                        }

                        AppNavTab.PROFILE -> {
                            ProfileScreen(
                                onDownloadWorkoutClick = { viewModel.downloadWorkout() }
                            )
                        }
                    }
                }
            }
        }
    }
}

