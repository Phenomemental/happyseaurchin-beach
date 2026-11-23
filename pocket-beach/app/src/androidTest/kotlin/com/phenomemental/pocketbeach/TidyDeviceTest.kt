package com.phenomemental.pocketbeach

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.model.Job
import com.phenomemental.pocketbeach.model.JobSettings
import com.phenomemental.pocketbeach.net.HttpsWire
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sundial 0.12.1, the tidy-up (spine:pocket-beach 1.1111787199992): her talk
 * mind loads as Sundial opens, after the hatch and before he says anything,
 * the model his talk runs on; before the hatch, nothing loads. The log
 * header says her window is the time alone.
 */
@RunWith(AndroidJUnit4::class)
class TidyDeviceTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = Fresh.app
    private val mind = StandIn()

    @Before
    fun standIns() {
        app.wire = NowhereWire()
        app.mind = mind
    }

    @After
    fun backAsTheyWere() {
        app.wire = HttpsWire
        app.mind = app.gemma
        app.set(Job.TALK, JobSettings.of(Job.TALK))
    }

    @Test
    fun herTalkMindLoadsAsSundialOpensAfterTheHatch() {
        Fresh.hatched()
        app.set(Job.TALK, JobSettings.of(Job.TALK).copy(model = JobSettings.GEMMA_E4B))
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(10_000) { mind.warmed.isNotEmpty() }
            assertEquals("her talk's own model, before he says anything", JobSettings.GEMMA_E4B, mind.warmed.first().model)
            assertTrue("nothing said yet", mind.talkMessages.isEmpty())
        }
        assertTrue(app.asking.transcript().contains("her window is the time alone."))
    }

    @Test
    fun beforeTheHatchNothingLoads() {
        Fresh.clear()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            Thread.sleep(1500)
            assertTrue("no model loaded before the hatch", mind.warmed.isEmpty())
        }
    }
}
