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
import com.phenomemental.pocketbeach.ui.Received
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sundial 0.12, the plain chat (spine:pocket-beach 1.111178719993): a block
 * added with the paperclip is read into her turn as text in pscale structure
 * and shown as a chip on his message; nothing beneath her reply but her words;
 * the conversation kept and listed; text shared in lands as a file added.
 */
@RunWith(AndroidJUnit4::class)
class ChatDeviceTest {
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

    private fun say(words: String) {
        compose.waitFor("question")
        compose.onNodeWithTag("question").performTextInput(words)
        compose.tap("ask")
    }

    private fun count(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun aBlockAddedWithThePaperclipIsReadInAsTextAndShownAsAChip() {
        Fresh.hatched()
        check(app.pages.carryOut(app.pages.planKeep("orchard", JsObj.of("_" to str("The orchard."), "1" to str("Prune the apples")))).ok)
        mind.talkScript += "An orchard with apples to prune. When will you start?"
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            compose.tap("paperclip")
            compose.waitFor("attach-sheet")
            compose.tap("attach-block:orchard")
            compose.waitUntil(10_000) { app.asking.attached.any { it.name == "orchard" } }
            compose.tap("attach-done")
            say("What do you make of this?")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.isNotEmpty() }
            val turn = mind.talkMessages.last()
            assertTrue(turn, turn.contains("What do you make of this?\n\n(His block orchard: its opening line, then each part beneath it:)\nThe orchard.\n  1  Prune the apples"))
            assertTrue(turn, turn.startsWith("(It is "))
            compose.waitFor("say", "An orchard with apples to prune.")
            assertEquals("the chip on his message", 1, count("file-chip"))
            assertTrue("no file waits for his next message", app.asking.attached.isEmpty())
            // Nothing set out beneath her reply: the plain chat walks no recipe.
            assertEquals(0, count("yes"))
            // Kept, and listed.
            val kept = app.asking.keptTalks()
            assertTrue(kept.isNotEmpty())
            assertEquals("What do you make of this?", kept.first().exchanges.last().his)
            assertEquals("orchard", kept.first().exchanges.last().files.single().name)
            compose.tap("talks")
            compose.waitFor("talk:${kept.first().startedMs}")
        }
    }

    /** Revised 22 November: a block too big for her window is handed her as its opening lines, and the chip says so. */
    @Test
    fun aBlockTooBigIsGivenAsItsOpeningLinesAndTheChipSaysSo() {
        Fresh.hatched()
        val long = "words ".repeat(60).trim()
        val big = JsObj.of("_" to str("The book, chapter by chapter."))
        for (i in 1..9) big["$i"] = JsObj.of("_" to str("Chapter $i"), *(1..9).map { j -> "$j" to str("Scene $i.$j: $long") }.toTypedArray())
        check(app.pages.carryOut(app.pages.planKeep("book", big)).ok)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            compose.tap("paperclip")
            compose.tap("attach-block:book")
            compose.waitUntil(10_000) { app.asking.attached.any { it.name == "book" } }
            compose.waitFor("file-chip-partial", "its opening lines only")
            compose.tap("attach-done")
            say("What do you make of it?")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.isNotEmpty() }
            val turn = mind.talkMessages.last()
            assertTrue(turn, turn.contains("(His block book, too big to read whole here: its opening line, then each part's opening line beneath it:)\nThe book, chapter by chapter.\n  1  Chapter 1  (9 beneath)"))
            assertTrue("no scene, and never JSON", !turn.contains("Scene") && !turn.contains("{"))
            compose.waitFor("file-chip-partial", "its opening lines only")
        }
    }

    @Test
    fun textSharedInLandsAsAFileAdded() {
        Fresh.hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            app.inbox = Received("Bring the seeds and the twine.", "list.txt")
            compose.waitUntil(10_000) { app.asking.attached.any { it.name == "list.txt" } }
            compose.waitFor("file-chip")
            assertTrue(app.asking.attached.single().text.contains("Bring the seeds"))
            assertTrue("the inbox is emptied", app.inbox == null)
        }
    }
}
