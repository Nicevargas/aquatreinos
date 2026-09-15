package com.example.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WorkoutRepository
import com.example.data.ciclo.DataCivil
import com.example.data.ciclo.TreinosSugeridosRepository
import com.example.data.supabase.SupabaseRepository
import com.example.data.supabase.SupabaseStatus
import com.example.model.AppNavTab
import com.example.model.CalendarDay
import com.example.model.CompletedSetRecord
import com.example.model.StopwatchMode
import com.example.model.SwimSetStopwatchState
import com.example.model.TrainingLevel
import com.example.model.Workout
import com.example.model.WorkoutSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class AquagendaUiState(
    val selectedTab: AppNavTab = AppNavTab.HOME,
    val selectedLevel: TrainingLevel = TrainingLevel.INTERMEDIARIO,
    val selectedEpochDay: Long = DataCivil.hoje(),
    val calendarDays: List<CalendarDay> = WorkoutRepository.semanaDoCalendario(DataCivil.hoje()),
    val currentWorkout: Workout = WorkoutRepository.getWorkoutForLevel(TrainingLevel.INTERMEDIARIO),
    val stopwatch: SwimSetStopwatchState = SwimSetStopwatchState(),
    val userNotification: String? = null,
    val supabaseStatus: SupabaseStatus = SupabaseRepository.getInitialStatus(),
    val isSyncingWithSupabase: Boolean = false
)

// Mesmos ritmos médios que scripts/carrossel_para_supabase.py usa para estimar o tempo.
private val RITMO_S_POR_100M = mapOf(
    TrainingLevel.INICIANTE to 150,
    TrainingLevel.INTERMEDIARIO to 130,
    TrainingLevel.AVANCADO to 115
)

private fun repeticoes(set: WorkoutSet): Int =
    set.repsDistance.lowercase().substringBefore("x", "1").trim().toIntOrNull() ?: 1

/** A série que o cronômetro acompanha: a primeira série repetida da parte principal. */
private fun serieDoCronometro(workout: Workout): WorkoutSet? {
    // "Desenvolvimento" no Método NC; "Principal" nos treinos antigos.
    val principal = workout.phases.firstOrNull {
        it.title.equals("Desenvolvimento", ignoreCase = true) || it.title.equals("Principal", ignoreCase = true)
    }?.sets.orEmpty()
    val todas = workout.phases.flatMap { it.sets }
    return principal.firstOrNull { repeticoes(it) > 1 && it.restSeconds > 0 }
        ?: principal.firstOrNull { repeticoes(it) > 1 }
        ?: todas.firstOrNull { repeticoes(it) > 1 }
        ?: principal.firstOrNull()
}

private fun AquagendaUiState.comTreino(workout: Workout): AquagendaUiState {
    val serie = serieDoCronometro(workout)
    val cronometro = if (serie == null || stopwatch.isRunning) {
        stopwatch
    } else {
        val reps = repeticoes(serie)
        val metros = if (serie.distanceMeters > 0) serie.distanceMeters / reps else 100
        val descanso = serie.restSeconds.takeIf { it > 0 } ?: stopwatch.restDurationSeconds
        val nado = metros * (RITMO_S_POR_100M[workout.level] ?: 130) / 100
        stopwatch.copy(
            currentSetNumber = 1,
            totalSets = reps,
            setRepDescription = serie.header.ifBlank { "${serie.repsDistance}m ${serie.description}" },
            setDistanceMeters = metros,
            restDurationSeconds = descanso,
            targetIntervalSeconds = ((nado + descanso + 4) / 5) * 5
        )
    }
    return copy(currentWorkout = workout, stopwatch = cronometro)
}

class AquagendaViewModel(application: Application) : AndroidViewModel(application) {

    // Os dois programas embarcados; o do Método NC vale a partir de 28/09/2026.
    private val treinos = TreinosSugeridosRepository {
        listOf("treinos_ciclo.json", "programa_nc.json").map { nome ->
            application.assets.open(nome).bufferedReader().use { it.readText() }
        }
    }

    // O treino de hoje já sai da cópia embarcada no primeiro quadro, sem esperar rede.
    private val _uiState = MutableStateFlow(
        AquagendaUiState().let { it.comTreino(treinos.embarcado(it.selectedEpochDay, it.selectedLevel)) }
    )
    val uiState: StateFlow<AquagendaUiState> = _uiState.asStateFlow()

    private var remoteWorkoutJob: Job? = null

    // Cronômetro de série da tela inicial. Relógio monotônico: não pula quando o
    // celular acerta a hora.
    private var stopwatchJob: Job? = null
    private var stopwatchStartTimestamp: Long = 0L
    private var stopwatchBaseAccumulatedMillis: Long = 48500L

    private fun agora(): Long = SystemClock.elapsedRealtime()

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
            val dia = _uiState.value.selectedEpochDay
            val level = _uiState.value.selectedLevel
            try {
                val remoteWorkout = treinos.remoto(dia, level)
                val remoteLaps = SupabaseRepository.getSwimSetRecords()
                _uiState.update { state ->
                    val mesmaEscolha = state.selectedEpochDay == dia && state.selectedLevel == level
                    val withWorkout = if (remoteWorkout != null && mesmaEscolha) state.comTreino(remoteWorkout) else state
                    val lapsToUse = if (remoteLaps.isNotEmpty()) remoteLaps.sortedByDescending { it.setNumber } else withWorkout.stopwatch.completedLaps
                    withWorkout.copy(
                        stopwatch = withWorkout.stopwatch.copy(completedLaps = lapsToUse),
                        isSyncingWithSupabase = false,
                        userNotification = "Sincronizado com Supabase Cloud com sucesso!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncingWithSupabase = false) }
            }
        }
    }

    /** Mostra na hora o treino embarcado e, com Supabase conectado, troca pelo do banco. */
    private fun loadSuggestedWorkout() {
        val state = _uiState.value
        val dia = state.selectedEpochDay
        val level = state.selectedLevel
        _uiState.update { it.comTreino(treinos.embarcado(dia, level)) }

        remoteWorkoutJob?.cancel()
        if (state.supabaseStatus != SupabaseStatus.CONNECTED) return
        remoteWorkoutJob = viewModelScope.launch {
            val remoto = treinos.remoto(dia, level) ?: return@launch
            _uiState.update { s ->
                if (s.selectedEpochDay == dia && s.selectedLevel == level) s.comTreino(remoto) else s
            }
        }
    }

    /** Um treino de "Meus treinos" passa a ser o treino da tela inicial. */
    fun usarTreino(workout: Workout) {
        remoteWorkoutJob?.cancel()
        _uiState.update { state ->
            state.comTreino(workout).copy(selectedLevel = workout.level, selectedTab = AppNavTab.HOME)
        }
    }

    fun selectTab(tab: AppNavTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectDay(epochDay: Long) {
        _uiState.update { state ->
            state.copy(
                selectedEpochDay = epochDay,
                calendarDays = WorkoutRepository.semanaDoCalendario(epochDay)
            )
        }
        loadSuggestedWorkout()
    }

    fun selectToday() {
        selectDay(DataCivil.hoje())
    }

    fun selectLevel(level: TrainingLevel) {
        if (_uiState.value.selectedLevel == level) return
        _uiState.update { it.copy(selectedLevel = level) }
        loadSuggestedWorkout()
    }

    fun downloadWorkout() {
        _uiState.update { it.copy(userNotification = "Treino sincronizado e salvo no dispositivo com sucesso!") }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(userNotification = null) }
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
        stopwatchStartTimestamp = agora()
        stopwatchBaseAccumulatedMillis = _uiState.value.stopwatch.elapsedMillis
        _uiState.update { it.copy(stopwatch = it.stopwatch.copy(isRunning = true)) }
        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (true) {
                delay(50L) // 20 updates per second for smooth real-time deciseconds
                val currentElapsed = stopwatchBaseAccumulatedMillis + (agora() - stopwatchStartTimestamp)
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
        val currentElapsed = stopwatchBaseAccumulatedMillis + (agora() - stopwatchStartTimestamp)
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
            stopwatchBaseAccumulatedMillis + (agora() - stopwatchStartTimestamp)
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
        stopwatchStartTimestamp = agora()
        stopwatchBaseAccumulatedMillis = 0L

        // Asynchronously persist to Supabase if configured
        viewModelScope.launch {
            val workout = _uiState.value.currentWorkout
            // Treino sugerido não é linha de public.workouts: a FK recusaria o id.
            SupabaseRepository.recordSwimSet(
                record = newRecord,
                workoutId = workout.id.takeUnless { workout.isSuggestion },
                repDescription = current.setRepDescription,
                distanceMeters = current.setDistanceMeters
            )
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
        remoteWorkoutJob?.cancel()
        stopwatchJob?.cancel()
    }
}
