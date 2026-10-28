package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The test screen, as Matthew will use it: fill the fields, tap Run, read the result. */
@RunWith(AndroidJUnit4::class)
class ScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun runningAReadShowsTheWhetstone() {
        compose.onNodeWithTag("agent_id").performTextReplacement("pscale")
        compose.onNodeWithTag("block").performTextReplacement("whetstone")
        compose.onNodeWithTag("spindle").performTextReplacement("2")
        compose.onNodeWithTag("run").performScrollTo().performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("result") and hasText("Selection-shape derivation", substring = true), 10_000)
        compose.onNodeWithTag("took").performScrollTo()
    }
}
