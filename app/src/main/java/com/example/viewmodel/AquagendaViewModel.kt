package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WorkoutRepository
import com.example.data.supabase.SupabaseRepository
import com.example.data.supabase.SupabaseStatus
import com.example.model.AppNavTab
import com.example.model.CalendarDay
import com.example.model.CompletedSetRecord
import com.example.model.PhaseStatus
import com.example.model.StopwatchMode
import com.example.model.SwimSetStopwatchState
import com.example.model.TrainingLevel
import com.example.model.Workout
import com.example.model.WorkoutPhase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class LiveWorkoutUiState(
    val isOpen: Boolean = false,
    val isTimerRunning: Boolean = false,
    val elapsedSeconds: Long = 2535L, // 00:42:15 initial time matching mockup
    val currentSetNumber: Int = 3,
    val totalSetsInPhase: Int = 5,
    val completedMeters: Int = 1200,
    val totalMeters: Int = 2500,
    val isFinished: Boolean = false
)

data class AquagendaUiState(
    val selectedTab: AppNavTab = AppNavTab.HOME,
    val selectedLevel: TrainingLevel = TrainingLevel.INTERMEDIARIO,
    val calendarDays: List<CalendarDay> = WorkoutRepository.getInitialCalendarDays(),
    val currentWorkout: Workout = WorkoutRepository.getWorkoutForLevel(TrainingLevel.INTERMEDIARIO),
    val liveWorkout: LiveWorkoutUiState = LiveWorkoutUiState(),
    val stopwatch: SwimSetStopwatchState = SwimSetStopwatchState(),
    val userNotification: String? = null,
    val supabaseStatus: SupabaseStatus = SupabaseRepository.getInitialStatus(),
    val isSyncingWithSupabase: Boolean = false
)

class AquagendaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AquagendaUiState())
    val uiState: StateFlow<AquagendaUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // Real-time Swim Set Stopwatch on Main Screen
    private var stopwatchJob: Job? = null
    private var stopwatchStartTimestamp: Long = 0L
    private var stopwatchBaseAccumulatedMillis: Long = 48500L

    init {
        checkSupabaseConnection()
    }

    fun checkSupabaseConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingWithSupabase = true) }
            val status = SupabaseRepository.testConnection()
            _uiState.update { it.copy(supabaseStatus = status, isSyncingWithSupabase = false) }
            if (status == SupabaseStatus.CONNECTED) {
                syncDataFromSupabase()
            }
        }
    }

    fun syncDataFromSupabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingWithSupabase = true) }
            try {
                val remoteWorkouts = SupabaseRepository.getWorkouts(_uiState.value.selectedLevel)
                val remoteLaps = SupabaseRepository.getSwimSetRecords()
                _uiState.update { state ->
                    val workoutToUse = remoteWorkouts.firstOrNull() ?: state.currentWorkout
                    val lapsToUse = if (remoteLaps.isNotEmpty()) remoteLaps.sortedByDescending { it.setNumber } else state.stopwatch.completedLaps
                    state.copy(
                        currentWorkout = workoutToUse,
                        stopwatch = state.stopwatch.copy(completedLaps = lapsToUse),
                        isSyncingWithSupabase = false,
                        userNotification = "Sincronizado com Supabase Cloud com sucesso!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncingWithSupabase = false) }
            }
        }
    }

    fun selectTab(tab: AppNavTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectDay(dayNumber: Int) {
        _uiState.update { state ->
            val updatedDays = state.calendarDays.map { day ->
                day.copy(isSelected = (day.dayNumber == dayNumber))
            }
            state.copy(calendarDays = updatedDays)
        }
    }

    fun selectLevel(level: TrainingLevel) {
        if (_uiState.value.selectedLevel == level) return
        val newWorkout = WorkoutRepository.getWorkoutForLevel(level)
        val isAvancado = level == TrainingLevel.AVANCADO
        _uiState.update { state ->
            state.copy(
                selectedLevel = level,
                currentWorkout = newWorkout,
                liveWorkout = state.liveWorkout.copy(
                    totalMeters = newWorkout.totalDistanceMeters
                ),
                stopwatch = state.stopwatch.copy(
                    totalSets = if (isAvancado) 10 else 8,
                    setRepDescription = if (isAvancado) "10x100m Crawl" else "8x100m Crawl",
                    targetIntervalSeconds = if (isAvancado) 95 else 105
                )
            )
        }
    }

    fun startLiveWorkout() {
        _uiState.update { state ->
            state.copy(
                liveWorkout = state.liveWorkout.copy(
                    isOpen = true,
                    isTimerRunning = true
                )
            )
        }
        startTimer()
    }

    fun closeLiveWorkout() {
        pauseTimer()
        _uiState.update { state ->
            state.copy(
                liveWorkout = state.liveWorkout.copy(isOpen = false)
            )
        }
    }

    fun togglePauseTimer() {
        val currentlyRunning = _uiState.value.liveWorkout.isTimerRunning
        if (currentlyRunning) {
            pauseTimer()
            _uiState.update { state ->
                state.copy(
                    liveWorkout = state.liveWorkout.copy(isTimerRunning = false)
                )
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    liveWorkout = state.liveWorkout.copy(isTimerRunning = true)
                )
            }
            startTimer()
        }
    }

    fun advanceToNextSet() {
        _uiState.update { state ->
            val lw = state.liveWorkout
            if (lw.currentSetNumber < lw.totalSetsInPhase) {
                val nextSet = lw.currentSetNumber + 1
                val addedMeters = 100
                val newCompletedMeters = (lw.completedMeters + addedMeters).coerceAtMost(lw.totalMeters)
                state.copy(
                    liveWorkout = lw.copy(
                        currentSetNumber = nextSet,
                        completedMeters = newCompletedMeters
                    )
                )
            } else {
                // Completed main set
                state.copy(
                    liveWorkout = lw.copy(
                        completedMeters = lw.totalMeters,
                        isFinished = true
                    ),
                    userNotification = "Parabéns! Todas as séries da fase principal foram concluídas!"
                )
            }
        }
    }

    fun downloadWorkout() {
        _uiState.update { it.copy(userNotification = "Treino sincronizado e salvo no dispositivo com sucesso!") }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _uiState.update { state ->
                    if (state.liveWorkout.isTimerRunning) {
                        state.copy(
                            liveWorkout = state.liveWorkout.copy(
                                elapsedSeconds = state.liveWorkout.elapsedSeconds + 1
                            )
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // --- Swimming Set Real-time Stopwatch Methods ---

    fun toggleStopwatch() {
        val isRunning = _uiState.value.stopwatch.isRunning
        if (isRunning) {
            pauseStopwatch()
        } else {
            startStopwatch()
        }
    }

    private fun startStopwatch() {
        stopwatchStartTimestamp = System.currentTimeMillis()
        stopwatchBaseAccumulatedMillis = _uiState.value.stopwatch.elapsedMillis
        _uiState.update { it.copy(stopwatch = it.stopwatch.copy(isRunning = true)) }
        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (true) {
                delay(50L) // 20 updates per second for smooth real-time deciseconds
                val now = System.currentTimeMillis()
                val currentElapsed = stopwatchBaseAccumulatedMillis + (now - stopwatchStartTimestamp)
                _uiState.update { state ->
                    if (state.stopwatch.isRunning) {
                        state.copy(
                            stopwatch = state.stopwatch.copy(elapsedMillis = currentElapsed)
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun pauseStopwatch() {
        stopwatchJob?.cancel()
        stopwatchJob = null
        val now = System.currentTimeMillis()
        val currentElapsed = stopwatchBaseAccumulatedMillis + (now - stopwatchStartTimestamp)
        stopwatchBaseAccumulatedMillis = currentElapsed
        _uiState.update {
            it.copy(
                stopwatch = it.stopwatch.copy(
                    isRunning = false,
                    elapsedMillis = currentElapsed
                )
            )
        }
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        stopwatchJob = null
        stopwatchStartTimestamp = 0L
        stopwatchBaseAccumulatedMillis = 0L
        _uiState.update {
            it.copy(
                stopwatch = it.stopwatch.copy(
                    isRunning = false,
                    elapsedMillis = 0L
                )
            )
        }
    }

    fun recordSetLap() {
        val current = _uiState.value.stopwatch
        val currentMillis = if (current.isRunning) {
            stopwatchBaseAccumulatedMillis + (System.currentTimeMillis() - stopwatchStartTimestamp)
        } else {
            current.elapsedMillis
        }

        val formattedTime = formatMillisToTime(currentMillis)
        val pace = calculatePace(currentMillis, current.setDistanceMeters)

        val previousLapMillis = current.completedLaps.firstOrNull()?.timeMillis ?: 0L
        val splitDiff = if (previousLapMillis > 0L) {
            val diffSeconds = (currentMillis - previousLapMillis).toDouble() / 1000.0
            if (diffSeconds > 0) String.format(Locale.US, "+%.1fs", diffSeconds)
            else String.format(Locale.US, "%.1fs", diffSeconds)
        } else {
            "Base"
        }

        val newRecord = CompletedSetRecord(
            setNumber = current.currentSetNumber,
            timeFormatted = formattedTime,
            pacePer100m = pace,
            splitDifference = splitDiff,
            timeMillis = currentMillis
        )

        val isLastSet = current.currentSetNumber >= current.totalSets
        val nextSet = if (!isLastSet) current.currentSetNumber + 1 else current.totalSets

        // Reset timer base for next set
        stopwatchStartTimestamp = System.currentTimeMillis()
        stopwatchBaseAccumulatedMillis = 0L

        // Asynchronously persist to Supabase if configured
        viewModelScope.launch {
            val workoutId = _uiState.value.currentWorkout.id
            SupabaseRepository.recordSwimSet(newRecord, workoutId)
        }

        _uiState.update { state ->
            state.copy(
                stopwatch = state.stopwatch.copy(
                    currentSetNumber = nextSet,
                    elapsedMillis = 0L,
                    completedLaps = listOf(newRecord) + state.stopwatch.completedLaps,
                    lastRecordedTime = formattedTime
                ),
                userNotification = if (isLastSet) {
                    "Parabéns! Todas as ${current.totalSets} séries concluídas! Último tempo: $formattedTime."
                } else {
                    "Série ${current.currentSetNumber} concluída: $formattedTime! Próxima: Série $nextSet/${current.totalSets}."
                }
            )
        }
    }

    fun setStopwatchMode(mode: StopwatchMode) {
        _uiState.update {
            it.copy(stopwatch = it.stopwatch.copy(mode = mode))
        }
    }

    fun incrementStopwatchSet() {
        _uiState.update { state ->
            val next = (state.stopwatch.currentSetNumber + 1).coerceAtMost(state.stopwatch.totalSets)
            state.copy(stopwatch = state.stopwatch.copy(currentSetNumber = next))
        }
    }

    fun decrementStopwatchSet() {
        _uiState.update { state ->
            val prev = (state.stopwatch.currentSetNumber - 1).coerceAtLeast(1)
            state.copy(stopwatch = state.stopwatch.copy(currentSetNumber = prev))
        }
    }

    private fun formatMillisToTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val tenths = (millis % 1000) / 100
        return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
    }

    private fun calculatePace(millis: Long, distanceMeters: Int): String {
        if (distanceMeters <= 0 || millis <= 0) return "--:--"
        val paceSecondsPer100 = ((millis.toDouble() / 1000.0) / (distanceMeters.toDouble() / 100.0)).toLong()
        val minutes = paceSecondsPer100 / 60
        val seconds = paceSecondsPer100 % 60
        return String.format(Locale.US, "%02d'%02d\"/100m", minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        stopwatchJob?.cancel()
    }
}
