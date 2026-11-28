package com.phenomemental.pocketbeach

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.net.HttpsWire
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sundial 0.13.5, talk about this: from the editor and the walker, what he
 * is looking at and the block's shape worked out by the app, handed her as
 * readable text beside his next words.
 */
@RunWith(AndroidJUnit4::class)
class TalkAboutThisDeviceTest {
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
    }

    private fun orchard() = check(app.pages.carryOut(app.pages.planKeep("orchard", JsObj.of(
        "_" to str("The orchard, through the year."),
        "1" to JsObj.of("_" to str("Autumn"), "1" to str("pick the apples")),
        "2" to str("Winter: prune the pears"),
    ))).ok)

    private fun send(words: String) {
        compose.waitFor("question")
        compose.onNodeWithTag("question").performTextInput(words)
        compose.tap("ask")
        compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.isNotEmpty() }
    }

    @Test
    fun fromTheEditorSheIsHandedWhatHeSeesAndItsShape() {
        Fresh.hatched()
        orchard()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            compose.menu("menu-blocks")
            compose.tap("block:orchard")
            compose.waitFor("outline-line:1.1", "pick the apples")
            compose.tap("talk-about-this")
            compose.waitFor("file-chip-view", "what you were looking at, and its shape")
            send("What do you see in it?")
            val turn = mind.talkMessages.last()
            assertTrue(turn, turn.contains("What do you see in it?\n\n(What he is looking at: the block orchard, then its shape, worked out by the app:)\nThe orchard, through the year."))
            assertTrue(turn, turn.contains("(The shape of his block orchard, worked out by the app:)\nIt goes 2 levels deep beneath its opening line, with 3 parts in all."))
            assertTrue(turn, turn.contains("It has grown most at 1: 1 part beneath it, 1 level deep."))
        }
    }

    /**
     * 0.13.6, from his phone on 28 November: a block with a long name squeezed
     * the chip's words into a column a letter or two wide. Now the words stand
     * beneath the name, as a line of words.
     */
    @Test
    fun aLongNameLeavesTheChipsWordsReadable() {
        Fresh.hatched()
        val long = "soft-llm-conversations-kept-through-the-year:Phenomemental"
        check(app.pages.carryOut(app.pages.planKeep(long, JsObj.of("_" to str("Conversations with soft LLMs."), "1" to str("The first one.")))).ok)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            compose.menu("menu-blocks")
            compose.tap("block:$long")
            compose.waitFor("outline-line:1", "The first one.")
            compose.tap("talk-about-this")
            compose.waitFor("file-chip-view", "what you were looking at, and its shape")
            val name = compose.onNodeWithTag("file-chip-name", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val words = compose.onNodeWithTag("file-chip-view", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("the words stand beneath the name: $name, $words", words.top >= name.bottom - 1f)
            assertTrue("a line of words, not a column of letters: $words", words.width > words.height * 4)
        }
    }

    @Test
    fun fromTheWalkerSheIsHandedTheWalkAndTheShape() {
        Fresh.hatched()
        orchard()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            compose.menu("menu-blocks")
            compose.tap("open-walker")
            compose.waitFor("walk-block-name")
            compose.onNodeWithTag("walk-block-name").performTextReplacement("orchard")
            compose.tap("walk-go")
            compose.waitFor("walk-out", "[disc @ pscale 0")
            compose.tap("walker-talk-about")
            compose.waitFor("file-chip-view")
            send("What patterns do you see?")
            val turn = mind.talkMessages.last()
            assertTrue(turn, turn.contains("(What he is looking at: the block orchard, then its shape, worked out by the app:)\n[disc @ pscale 0"))
            assertTrue(turn, turn.contains("(The shape of his block orchard, worked out by the app:)"))
        }
    }
}
