package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.net.HttpsWire
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sundial 0.13, the walk offered when his words name a block
 * (spine:pocket-beach 1.1111787199994): on that turn only she is given the
 * walker; what she walks is handed her as readable text, never JSON; a chip
 * on her reply names it. A turn naming no block has no tool.
 */
@RunWith(AndroidJUnit4::class)
class WalkTurnDeviceTest {
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
        app.walkCheck = null
    }

    private fun membership() = check(app.pages.carryOut(app.pages.planKeep("membership", JsObj.of(
        "_" to str("The membership form, step by step."),
        "1" to str("Agree the categories with David"),
        "2" to JsObj.of("_" to str("ID checks"), "1" to str("Finish the remaining checks"), "2" to str("Log each one in the register")),
    ))).ok)

    private fun say(words: String) {
        compose.waitFor("question")
        compose.onNodeWithTag("question").performTextInput(words)
        compose.tap("ask")
    }

    private fun count(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun aBlockHisWordsNameIsWalkedAndHerReplyCarriesAChip() {
        Fresh.hatched()
        membership()
        mind.walkArgs = mapOf("block" to "membership", "spindle" to "2.2")
        mind.talkScript += "At 2.2 it says to log each one in the register."
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say("What's at 2.2 in my membership block?")
            compose.waitUntil(20_000) { app.asking.settled && mind.toolTurns.isNotEmpty() }
            assertEquals("offered on this turn", 1, mind.toolTurns.size)
            assertEquals("on the GPU, as her talk runs", listOf(false), mind.toolOnCpu.toList())
            assertTrue("the walker named for the block his words named", mind.toolJsons.single().contains("his words named membership. Read it,") && mind.toolJsons.single().contains("\"enum\":[\"membership\"]"))
            val handed = mind.walkResults.single()
            assertTrue(handed, handed.contains("(His block membership, walked to 2.2") && handed.contains("    2.2  Log each one in the register"))
            assertTrue("never JSON", !handed.contains("{") && !handed.contains("\"_\""))
            compose.waitFor("say", "log each one in the register")
            compose.waitFor("walked-chip", "walked membership at 2.2")
            assertTrue(app.asking.transcript(), app.asking.transcript().contains("His words named membership: the walker offered; she walked membership at 2.2."))
        }
    }

    @Test
    fun aTurnNamingNoBlockHasNoTool() {
        Fresh.hatched()
        membership()
        mind.walkArgs = mapOf("block" to "membership", "spindle" to "2.2")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say("I walked Haven this morning and it was cold.")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.isNotEmpty() }
            assertTrue("no tool offered", mind.toolTurns.isEmpty())
            assertEquals(0, count("walked-chip"))
            // The next turn naming it is offered the walker, and only that one.
            say("And what does my membership block say?")
            compose.waitUntil(20_000) { app.asking.settled && mind.toolTurns.size == 1 }
            assertEquals(2, mind.talkMessages.size)
        }
    }

    @Test
    fun aPlaceThatIsntThereIsNotWalkedAndSheIsToldPlainly() {
        Fresh.hatched()
        membership()
        mind.walkArgs = mapOf("block" to "membership", "spindle" to "2.9")
        mind.talkScript += "There's no 2.9 in it; it has two ID checks."
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say("What's at 2.9 in my membership block?")
            compose.waitUntil(20_000) { app.asking.settled && mind.walkResults.isNotEmpty() }
            assertEquals("There is no place 2.9 in his block membership. Answer him without it.", mind.walkResults.single())
            compose.waitFor("say", "two ID checks")
            compose.waitFor("walked-chip", "looked for 2.9 in membership")
        }
    }

    @Test
    fun whenTheWalkCheckFoundAddressesAlteredHerToolTurnsRunOnTheCpu() {
        Fresh.hatched()
        membership()
        app.walkCheck = PocketApp.WalkChecked(17, 20, 3, System.currentTimeMillis(), "Gemma 4 E4B")
        mind.walkArgs = mapOf("block" to "membership", "spindle" to "1")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say("Read me the first step of my membership block.")
            compose.waitUntil(20_000) { app.asking.settled && mind.toolTurns.isNotEmpty() }
            assertEquals(listOf(true), mind.toolOnCpu.toList())
            assertTrue(app.asking.transcript(), app.asking.transcript().contains("the walker offered on the CPU, as the walk check found addresses altered on the GPU"))
            // A turn naming no block is untouched: her talk as it was, no tool.
            say("Thanks.")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 2 }
            assertEquals(1, mind.toolTurns.size)
        }
        // None altered (one call that never came isn't an altered address): on the GPU.
        app.walkCheck = PocketApp.WalkChecked(19, 20, 0, System.currentTimeMillis(), "Gemma 4 E4B")
        mind.toolOnCpu.clear()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say("And the second step of my membership block?")
            compose.waitUntil(20_000) { app.asking.settled && mind.toolOnCpu.isNotEmpty() }
            assertEquals(listOf(false), mind.toolOnCpu.toList())
        }
    }
}
