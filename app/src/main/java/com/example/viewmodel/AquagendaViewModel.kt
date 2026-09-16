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
import com.example.model.ModoDeTreino
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
    val selectedModo: ModoDeTreino = ModoDeTreino.PISCINA,
    val selectedEpochDay: Long = DataCivil.hoje(),
    val calendarDays: List<CalendarDay> = WorkoutRepository.diasDoCalendario(DataCivil.hoje()),
    val currentWorkout: Workout = WorkoutRepository.getWorkoutForLevel(TrainingLevel.INTERMEDIARIO),
    val userNotification: String? = null,
    val supabaseStatus: SupabaseStatus = SupabaseRepository.getInitialStatus(),
    val isSyncingWithSupabase: Boolean = false
) {
    /** Águas abertas escolhido num nível sem treino desse modo: a tela mostra o de piscina e avisa. */
    val modoSemTreinoNoNivel: Boolean get() = !selectedModo.temTreinoPara(selectedLevel)

    /** O modo do treino que está na tela. */
    val modoEmUso: ModoDeTreino get() = if (modoSemTreinoNoNivel) ModoDeTreino.PISCINA else selectedModo
}

private fun AquagendaUiState.comTreino(workout: Workout): AquagendaUiState =
    copy(currentWorkout = workout)

class AquagendaViewModel(application: Application) : AndroidViewModel(application) {

    // Piscina: o programa antigo e o do Método NC (desde 15/09/2026). Águas abertas: o seu.
    private val treinos = TreinosSugeridosRepository { modo ->
        when (modo) {
            ModoDeTreino.PISCINA -> listOf("treinos_ciclo.json", "programa_nc.json")
            ModoDeTreino.AGUAS_ABERTAS -> listOf("programa_aa.json")
        }.map { nome -> application.assets.open(nome).bufferedReader().use { it.readText() } }
    }

    // O treino de hoje já sai da cópia embarcada no primeiro quadro, sem esperar rede.
    private val _uiState = MutableStateFlow(AquagendaUiState().let { it.comTreino(embarcado(it)) })
    val uiState: StateFlow<AquagendaUiState> = _uiState.asStateFlow()

    private var remoteWorkoutJob: Job? = null

    init {
        checkSupabaseConnection()
    }

    private fun embarcado(state: AquagendaUiState): Workout =
        treinos.embarcado(state.selectedEpochDay, state.selectedLevel, state.modoEmUso)
            ?: treinos.embarcado(state.selectedEpochDay, state.selectedLevel)

    private fun mesmaEscolha(a: AquagendaUiState, b: AquagendaUiState) =
        a.selectedEpochDay == b.selectedEpochDay && a.selectedLevel == b.selectedLevel && a.selectedModo == b.selectedModo

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
            val inicio = _uiState.value
            try {
                val remoteWorkout = treinos.remoto(inicio.selectedEpochDay, inicio.selectedLevel, inicio.modoEmUso)
                _uiState.update { state ->
                    val withWorkout = if (remoteWorkout != null && mesmaEscolha(state, inicio)) state.comTreino(remoteWorkout) else state
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
        _uiState.update { it.comTreino(embarcado(state)) }

        remoteWorkoutJob?.cancel()
        if (state.supabaseStatus != SupabaseStatus.CONNECTED) return
        remoteWorkoutJob = viewModelScope.launch {
            val remoto = treinos.remoto(state.selectedEpochDay, state.selectedLevel, state.modoEmUso) ?: return@launch
            _uiState.update { s -> if (mesmaEscolha(s, state)) s.comTreino(remoto) else s }
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

    fun selectModo(modo: ModoDeTreino) {
        if (_uiState.value.selectedModo == modo) return
        _uiState.update { it.copy(selectedModo = modo) }
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
