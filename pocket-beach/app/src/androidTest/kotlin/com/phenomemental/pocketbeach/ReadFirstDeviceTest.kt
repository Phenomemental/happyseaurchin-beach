package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.NothingRead
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
 * Sundial 0.13.3 (spine:pocket-beach 1.1111787199994, revised 23 November),
 * his two turns on his phone at 14:58: offered the walker for his passport,
 * she asked his leave and walked nothing; on his next words no block was
 * named, no walker was offered, and she wrote a pretend bsp call and an
 * invented passport as if read. Now the app reads a block his words name
 * first, its disc at pscale 0, beside his words with a chip; the walker is
 * offered on that turn and the next; and a reply that writes a tool call or
 * "data received" with nothing read is marked so, plainly.
 */
@RunWith(AndroidJUnit4::class)
class ReadFirstDeviceTest {
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
    fun hisTwoTurnsOnHisPhone() {
        Fresh.hatched()
        mind.talkScript += "Would you like me to read your passport to find out?"
        mind.talkScript += "```tool_code\nprint(bsp(block=\"passport:Phenomemental\"))\n```\nData received. Your passport says you belong to the Temporal Weavers Guild, as a Sovereign Observer."
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say("what does my passport tell you about how to work with me")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 1 }
            // The app read his passport first, its opening lines, and handed it her beside his words; the chip on his message says so.
            val first = mind.talkMessages[0]
            assertTrue(
                first,
                first.contains(
                    "what does my passport tell you about how to work with me\n\n" +
                        "(His block passport:Phenomemental, which his words named, read by the app: its opening line, then each part's opening line beneath it:)\n" +
                        "Matthew Bushell — digital social innovator.\n  1  The heart.",
                ),
            )
            assertEquals("the walker offered", 1, mind.toolTurns.size)
            assertTrue(mind.toolJsons[0], mind.toolJsons[0].contains("his words named passport:Phenomemental. Read it,"))
            compose.waitFor("file-chip-read", "read by Sundial")
            assertEquals("she asked his leave: nothing to mark", 0, count("nothing-read"))

            say("do what you think is best")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 2 }
            val second = mind.talkMessages[1]
            assertFalse("no block named, nothing read this turn: $second", second.contains("(His block"))
            assertTrue(second, second.endsWith("\ndo what you think is best"))
            // The walker still offered, on the turn after the one that named it.
            assertEquals(2, mind.toolTurns.size)
            assertTrue(mind.toolJsons[1], mind.toolJsons[1].contains("passport:Phenomemental"))
            // She wrote a pretend call and data as if read, and walked nothing: marked so, plainly, her words as she wrote them.
            compose.waitFor("nothing-read", NothingRead.SAID)
            compose.waitFor("say", "Temporal Weavers Guild")
            assertEquals(1, count("nothing-read"))
            assertTrue(app.asking.transcript(), app.asking.transcript().contains(NothingRead.SAID))

            // A third turn naming none: the walker is offered on that turn and the next only.
            mind.talkScript += "Of course."
            say("thanks")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 3 }
            assertEquals(2, mind.toolTurns.size)
        }
    }

    @Test
    fun aRealWalkOnTheNextTurnIsNotMarked() {
        Fresh.hatched()
        mind.talkScript += "Shall I read it?"
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say("what does my passport tell you about how to work with me")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 1 }
            mind.walkArgs = mapOf("block" to "passport:Phenomemental", "spindle" to "1")
            mind.talkScript += "Data received: at 1 it says The heart."
            say("do what you think is best")
            compose.waitUntil(20_000) { app.asking.settled && mind.walkResults.isNotEmpty() }
            assertTrue(mind.walkResults.single(), mind.walkResults.single().contains("1  The heart."))
            compose.waitFor("walked-chip", "walked passport:Phenomemental at 1")
            assertEquals("a real walk: nothing to mark", 0, count("nothing-read"))
        }
    }

    @Test
    fun aReplyAnsweringFromAFileHeAddedIsNotMarked() {
        // His phone, 19:21: "Read the attached. Learn it.", with a file added with the paperclip; her summary named the
        // protocol ("BSP (Block-Spindle-Point)") and the mark fired, though she answered from the file he gave her.
        Fresh.hatched()
        mind.talkScript += "I have read and fully learned the content of His file 02-bsp-walker.md. The documentation describes the **BSP (Block-Spindle-Point)** protocol. Data received and understood."
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            app.asking.attach(com.phenomemental.pocketbeach.bsp.Chats.Attached("02-bsp-walker.md", "# The BSP walker\n\nBSP is a pure resolution algorithm for pscale JSON blocks.", block = false))
            say("Read the attached. Learn it.")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 1 }
            compose.waitFor("say", "Block-Spindle-Point")
            assertEquals("a file he added: nothing to mark", 0, count("nothing-read"))
            assertFalse(app.asking.transcript(), app.asking.transcript().contains(NothingRead.SAID))
        }
    }
}
