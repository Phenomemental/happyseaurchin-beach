package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.Cook
import com.phenomemental.pocketbeach.bsp.Days
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Landing
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.Telling
import com.phenomemental.pocketbeach.bsp.jsGet
import com.phenomemental.pocketbeach.bsp.jsString
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.bsp.stringify
import com.phenomemental.pocketbeach.net.HttpsWire
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Rockpool, pscale as procedure (spine:pocket-beach 1.111178714), on the
 * phone with a stand-in for the model: his cook and kernel brought into his
 * pocket at opening, his days moved out of telling; his day told the beach's
 * way; and the name and the mark. Sundial 0.12's plain chat walks no recipe,
 * so the checks of his message walked by his cook went with it.
 */
@RunWith(AndroidJUnit4::class)
class RockpoolDeviceTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = Fresh.app
    private val wire = NowhereWire()
    private val mind = StandIn()
    private val h = Hatch.MATTHEW_HANDLE

    @Before
    fun standIns() {
        app.wire = wire
        app.mind = mind
    }

    @After
    fun backAsTheyWere() {
        app.wire = HttpsWire
        app.mind = app.gemma
        app.showDetails = false
    }

    private fun hatched() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        check(app.pages.carryOut(app.pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
        check(app.pages.carryOut(app.pages.planNewBlock("films")).ok)
        val passport = JsObj.of("_" to str("Matthew Bushell — digital social innovator."), "1" to str("The heart."))
        val planned = Hatch.plan(h, passport, app.pages, app.storage, System.currentTimeMillis(), "Adopted on this phone for the test.")
        Hatch.carryOut(planned, app.pages, app.storage).problem?.let { error("The hatch: $it") }
    }

    private fun block(name: String): JsObj? = app.storage.read(name)?.let { parseJson(it) as? JsObj }

    private fun say(words: String) {
        compose.waitFor("question")
        compose.onNodeWithTag("question").performTextInput(words)
        compose.tap("ask")
    }

    @Test
    fun atOpeningHisCookComesInAndHisDaysMoveOutOfTelling() {
        hatched()
        // As 0.8.1 left his days: a telling block, the six inside a day of his now, her window's hand on the telling.
        val old = Telling.shape()
        (old["2"] as JsObj)["2"] = JsObj.of("_" to str("This week"), "1" to str("Cut the second film"))
        check(app.pages.carryOut(app.pages.planWhole(Landing.TELLING, old)).ok)
        check(app.pages.carryOut(app.pages.planWhole(Landing.NOW, Before012.withDay(null, Before012.dayAddress(System.currentTimeMillis()), "Walked the dog.", mapOf("1.2" to listOf("Calm"))).first)).ok)
        check(app.pages.carryOut(app.pages.planChange(Hatch.shellOf(h), "${Hatch.WINDOW}.5", Hatch.OLD_HAND)).ok)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("corrected", "your cook and kernel are in your pocket", timeoutMs = 20_000)
            compose.waitFor("corrected", "your days moved the beach's way")
            assertNotNull(block(Cook.cookOf(h)))
            assertNotNull(block(Cook.kernelOf(h)))
            assertTrue(stringify(block(Days.p6Of(h)))!!.contains("Calm"))
            assertTrue(stringify(block(Days.aheadOf(h)))!!.contains("Cut the second film"))
            assertNull("telling retired", app.storage.read(Landing.TELLING))
            assertNotNull("kept, retired, in the hidden region", app.storage.read(Days.RETIRED_TELLING))
            val shell = block(Hatch.shellOf(h))!!
            assertTrue(stringify(shell["3"])!!.contains(Cook.cookOf(h)))
            assertTrue(stringify(shell["3"])!!.contains(Days.aheadOf(h)))
            assertTrue(jsString(jsGet(shell[Hatch.WINDOW], "5")) == Days.aheadOf(h))
        }
    }

    /**
     * Sundial 0.10: Keep today sets his day out on the keep card, and his yes keeps it in Sundial's form, in his now, with no P6.
     * Sundial 0.10.1 (item 11): Keep today is the one way: no Sort as my day after her reply.
     */
    @Test
    fun hisDayIsKeptInSundialsForm() {
        hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("top-bar")
            say("Took the dog out with Sam this afternoon by the river and it was lovely and crisp. I felt calm for the first time in a while, which surprised me. I need to ring Jo tomorrow about the music for the pilot.")
            compose.waitFor("say", timeoutMs = 20_000)
            compose.waitUntil(20_000) { app.asking.settled }
            assertTrue("no card after her reply", compose.onAllNodes(hasTestTag("sort-as-day"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
            compose.tap("keep-today")
            compose.waitFor("keep-card", timeoutMs = 20_000)
            compose.waitFor("keep-title", "A walk by the river")
            compose.tap("day-yes")
            compose.waitUntil(20_000) { app.storage.read(Days.NOW)?.contains("Took the dog out with Sam") == true }
            val now = app.storage.read(Days.NOW)!!
            assertTrue(now, now.contains("A walk by the river"))
            assertTrue(now, now.contains("I need to ring Jo tomorrow"))
            assertTrue("no P6 written", app.storage.read(Days.p6Of(h))?.contains("Took the dog out") != true)
            compose.waitUntil(10_000) { app.waiting.all.isEmpty() }
        }
    }

    @Test
    fun theNameAndTheMark() {
        hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("wordmark", "SUNDIAL")
            compose.menu("menu-about")
            compose.waitFor("mark")
            // The label is set in capitals, as every label is; About opens with Sundial's own first line.
            compose.waitUntil(5_000) { compose.onAllNodes(hasText("SUNDIAL")).fetchSemanticsNodes().isNotEmpty() }
            compose.waitFor("about-opening", "Most AI that people turn to for support")
            compose.waitUntil(5_000) { compose.onAllNodes(hasText("This was made by Matthew Bushell", substring = true)).fetchSemanticsNodes().isNotEmpty() }
            // How the pocket works, under Show the workings only.
            compose.waitFor("about-workings", "Sundial keeps pscale blocks")
        }
    }
}
