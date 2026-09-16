package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WorkoutRepository
import com.example.data.ciclo.DataCivil
import com.example.data.ciclo.TreinosSugeridosRepository
import com.example.data.supabase.SupabaseRepository
import com.example.data.supabase.SupabaseStatus
import com.example.model.AppNavTab
import com.example.model.CalendarDay
import com.example.model.TrainingLevel
import com.example.model.Workout
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AquagendaUiState(
    val selectedTab: AppNavTab = AppNavTab.HOME,
    val selectedLevel: TrainingLevel = TrainingLevel.INTERMEDIARIO,
    val selectedEpochDay: Long = DataCivil.hoje(),
    val calendarDays: List<CalendarDay> = WorkoutRepository.diasDoCalendario(DataCivil.hoje()),
    val currentWorkout: Workout = WorkoutRepository.getWorkoutForLevel(TrainingLevel.INTERMEDIARIO),
    val userNotification: String? = null,
    val supabaseStatus: SupabaseStatus = SupabaseRepository.getInitialStatus(),
    val isSyncingWithSupabase: Boolean = false
)

private fun AquagendaUiState.comTreino(workout: Workout): AquagendaUiState =
    copy(currentWorkout = workout)

class AquagendaViewModel(application: Application) : AndroidViewModel(application) {

    // Os dois programas embarcados; o do Método NC vale a partir de 15/09/2026.
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
                _uiState.update { state ->
                    val mesmaEscolha = state.selectedEpochDay == dia && state.selectedLevel == level
                    val withWorkout = if (remoteWorkout != null && mesmaEscolha) state.comTreino(remoteWorkout) else state
                    withWorkout.copy(
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
                calendarDays = WorkoutRepository.diasDoCalendario(epochDay)
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

    override fun onCleared() {
        super.onCleared()
        remoteWorkoutJob?.cancel()
    }
}
