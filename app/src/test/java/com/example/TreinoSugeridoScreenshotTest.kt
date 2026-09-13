package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.WorkoutRepository
import com.example.data.ciclo.CicloDeTreinos
import com.example.data.ciclo.DataCivil
import com.example.model.SwimSetStopwatchState
import com.example.model.TrainingLevel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.WorkoutsScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

// Tela alta para caber o treino inteiro na imagem.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h2200dp-420dpi", sdk = [36])
class TreinoSugeridoScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val dia = DataCivil.deIso("2026-09-13")
    private val ciclo = CicloDeTreinos.deJson(File("src/main/assets/treinos_ciclo.json").readText())

    @Test
    fun treino_sugerido_screenshot() {
        val treino = ciclo.sugestao(dia, TrainingLevel.INTERMEDIARIO)!!
        composeTestRule.setContent {
            MyApplicationTheme {
                WorkoutsScreen(workout = treino, onStartWorkoutClick = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/treino_sugerido.png")
    }

    @Test
    fun home_tres_niveis_screenshot() {
        val treino = ciclo.sugestao(dia, TrainingLevel.INICIANTE)!!
        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(
                    workout = treino,
                    selectedLevel = TrainingLevel.INICIANTE,
                    calendarDays = WorkoutRepository.semanaDoCalendario(dia, dia),
                    stopwatchState = SwimSetStopwatchState(),
                    onDayClick = {},
                    onLevelChange = {},
                    onStartWorkoutClick = {},
                    onViewWorkoutDetails = {},
                    onToggleStopwatch = {},
                    onLapStopwatch = {},
                    onResetStopwatch = {},
                    onStopwatchModeChange = {},
                    onStopwatchPrevSet = {},
                    onStopwatchNextSet = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/home_tres_niveis.png")
    }
}
