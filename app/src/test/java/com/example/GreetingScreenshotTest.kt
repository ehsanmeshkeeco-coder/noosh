package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.presentation.components.CircularWaterProgress
import com.example.presentation.theme.NooshTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun water_progress_rendered_successfully() {
        composeTestRule.setContent {
            NooshTheme {
                CircularWaterProgress(
                    percentage = 65,
                    consumedMl = 1300,
                    goalMl = 2000
                )
            }
        }
        // Successfully rendered in Compose hierarchy
        composeTestRule.waitForIdle()
    }
}
