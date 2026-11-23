package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.Looping
import com.phenomemental.pocketbeach.model.Job
import com.phenomemental.pocketbeach.model.JobPrefs
import com.phenomemental.pocketbeach.model.JobSettings
import com.phenomemental.pocketbeach.net.HttpsWire
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sundial 0.13.1 (spine:pocket-beach 1.1111787199993), after she looped on
 * his phone: the check in Settings runs the same short conversation on the
 * GPU and on the CPU, side by side; a reply that repeats the same word or
 * phrase many times running is stopped by the app and marked so; Talk's
 * temperature back to its default, 1.0.
 */
@RunWith(AndroidJUnit4::class)
class LoopAndSideBySideDeviceTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = Fresh.app
    private val mind = StandIn()

    @Before
    fun standIns() {
        app.wire = NowhereWire()
        app.mind = mind
        app.walkCheck = null
    }

    @After
    fun backAsTheyWere() {
        app.wire = HttpsWire
        app.mind = app.gemma
        app.walkCheck = null
        app.set(Job.TALK, JobSettings.of(Job.TALK))
    }

    private fun count(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun theSameConversationRunsOnTheGpuAndOnTheCpuSideBySide() {
        Fresh.hatched()
        mind.sideLoops = "GPU"
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            compose.menu("menu-settings")
            compose.tap("walk-check")
            compose.waitFor("walk-check-result", "20 of 20 arrived intact.", timeoutMs = 20_000)
            compose.waitUntil(20_000) { count("side-cpu") == 1 }
            assertEquals("the GPU, then the CPU", listOf("GPU", "CPU"), mind.conversedOn.map { it.processor })
            assertTrue("his Talk settings, its default temperature", mind.conversedOn.all { it.temperature == 1.0 })
            assertEquals(1, count("side-gpu"))
            // The GPU's last reply looped: cut where it began, and marked; the CPU's didn't.
            compose.waitFor("side-looped", "\"the\" 20 times running")
            assertEquals(1, count("side-looped"))
        }
    }

    @Test
    fun aReplyThatRepeatsOneWordOverAndOverIsStoppedAndMarked() {
        Fresh.hatched()
        mind.talkScript += "Walking by the sea in November is " + "the ".repeat(40)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            compose.waitFor("question")
            compose.onNodeWithTag("question").performTextInput("Tell me about the sea in November.")
            compose.tap("ask")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.isNotEmpty() }
            compose.waitFor("unfinished", Looping.STOPPED)
            compose.waitFor("say", "Walking by the sea in November is the …")
            assertTrue(app.asking.transcript(), app.asking.transcript().contains(Looping.STOPPED))
        }
    }

    @Test
    fun talksTemperatureGoesBackToItsDefaultOnce() {
        val prefs = app.getSharedPreferences("pocket", 0)
        prefs.edit().putFloat("job.TALK.temperature", 0.7f).remove(JobPrefs.TALK_BACK_TO_DEFAULT).commit()
        assertTrue("0.70, as on his phone, goes back to 1.0", JobPrefs.talkTemperatureBackToDefault(prefs))
        assertEquals(1.0, JobPrefs.read(prefs, Job.TALK).temperature, 0.0)
        // Once only: what he sets after is his.
        prefs.edit().putFloat("job.TALK.temperature", 0.5f).commit()
        assertFalse(JobPrefs.talkTemperatureBackToDefault(prefs))
        assertEquals(0.5, JobPrefs.read(prefs, Job.TALK).temperature, 0.0)
    }
}
