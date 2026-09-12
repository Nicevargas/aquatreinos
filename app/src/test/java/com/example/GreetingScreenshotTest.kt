package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.supabase.SupabaseStatus
import com.example.model.SwimSetStopwatchState
import com.example.ui.components.SupabaseSyncBar
import com.example.ui.components.SwimSetHistoryChartCard
import com.example.ui.components.SwimSetStopwatchCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun stopwatch_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        SwimSetStopwatchCard(
          stopwatchState = SwimSetStopwatchState(),
          onToggleStartPause = {},
          onLapSet = {},
          onReset = {},
          onModeChange = {},
          onPreviousSet = {},
          onNextSet = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun chart_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        SwimSetHistoryChartCard(
          completedLaps = SwimSetStopwatchState().completedLaps
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/chart.png")
  }

  @Test
  fun supabase_sync_bar_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        SupabaseSyncBar(
          status = SupabaseStatus.CONFIG_NEEDED,
          isSyncing = false,
          onSyncClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/supabase_sync_bar.png")
  }
}
