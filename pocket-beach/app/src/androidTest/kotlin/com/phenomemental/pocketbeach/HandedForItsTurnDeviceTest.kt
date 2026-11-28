package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.net.HttpsWire
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sundial 0.13.6, after his phone on 28 November: every block handed her
 * stayed in her conversation, turn on turn, until four blocks' lines came to
 * about 9,700 of its 10,900 tokens and her words broke down past about 4,000.
 * What she is handed (a block's top lines as bsp() reads them, a walk, a
 * file) is hers for its turn only: the turn stays as his words and her reply,
 * and a block his words name again is handed again. Within a turn, what she
 * was handed isn't handed twice. How full the conversation is still shows
 * beneath it (0.13.5).
 */
@RunWith(AndroidJUnit4::class)
class HandedForItsTurnDeviceTest {
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

    private val block = "(His block passport:Phenomemental, which his words named, as bsp() reads it:"

    @Test
    fun aBlockHisWordsNameIsHandedForItsTurnAndLetGoAfter() {
        Fresh.hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say("what does my passport tell you about how to work with me")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 1 }
            assertTrue(mind.talkMessages[0], mind.talkMessages[0].contains(block))
            compose.waitFor("file-chip-read", "read by Sundial: its top lines")
            // Let go after the turn: kept as his words alone.
            assertEquals(1, mind.forgotten.size)
            assertTrue(mind.forgotten[0], mind.forgotten[0].contains("what does my passport tell you about how to work with me"))
            assertFalse(mind.forgotten[0], mind.forgotten[0].contains("(His block"))
            assertTrue(app.asking.transcript(), app.asking.transcript().contains("What she was handed on this turn was let go after it: her conversation keeps his words and her reply."))
            // Named again, as on his phone: handed again, as she no longer holds it.
            say("Stop reading the passport")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 2 }
            assertTrue(mind.talkMessages[1], mind.talkMessages[1].contains(block))
            assertEquals(2, mind.forgotten.size)
            // Nothing handed: nothing to let go.
            say("Good morning")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 3 }
            assertFalse(mind.talkMessages[2], mind.talkMessages[2].contains("(His block"))
            assertEquals(2, mind.forgotten.size)
            assertEquals("no chip says given earlier any more", 0, count("file-chip-given"))
        }
    }

    @Test
    fun aWalkOfWhatWasHandedOnItsTurnIsNotHandedTwiceButOnTheNextItIs() {
        Fresh.hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            // On the turn his words name it, she walks the passport's top, which the app has just handed her.
            mind.walkArgs = mapOf("block" to "passport:Phenomemental")
            say("what does my passport tell you about how to work with me")
            compose.waitUntil(20_000) { app.asking.settled && mind.walkResults.size == 1 }
            assertTrue(mind.walkResults[0], mind.walkResults[0].startsWith("His block passport:Phenomemental was handed you with his words on this turn; it is above."))
            compose.waitFor("walked-chip", "already handed: passport:Phenomemental")
            // The next turn, the walker still offered: she no longer holds it, so the walk hands it her.
            say("do what you think is best")
            compose.waitUntil(20_000) { app.asking.settled && mind.walkResults.size == 2 }
            assertTrue(mind.walkResults[1], mind.walkResults[1].startsWith("(His block passport:Phenomemental, as bsp() reads it:"))
            assertEquals("each turn's handing let go after it", 2, mind.forgotten.size)
            // A place of it is walked whole.
            mind.walkArgs = mapOf("block" to "passport:Phenomemental", "spindle" to "1")
            say("read my passport at 1")
            compose.waitUntil(20_000) { app.asking.settled && mind.walkResults.size == 3 }
            assertTrue(mind.walkResults[2], mind.walkResults[2].contains("1  The heart."))
        }
    }

    @Test
    fun howFullTheConversationIsShowsAndNearlyFullOffersANewChat() {
        Fresh.hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            mind.heldTokens = 3277
            say("Good morning")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 1 }
            compose.waitFor("fullness", "This conversation is 40% full.")
            assertEquals(0, count("fullness-new-chat"))
            val opened = mind.talkInstructions.size
            // Past three quarters when his next message comes, her conversation is opened afresh with the last exchanges, as it
            // always was; the engine's count is what tells it, kept now after every reply.
            mind.heldTokens = 6800
            say("Tell me more")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 2 }
            assertEquals("opened afresh", opened + 1, mind.talkInstructions.size)
            compose.waitFor("fullness", "This conversation is 83% full. As it fills, her replies slow and can break down.")
            assertEquals(1, count("fullness-new-chat"))
            compose.tap("fullness-new-chat")
            compose.waitUntil(10_000) { app.asking.history.isEmpty() }
            assertNull(app.asking.held)
        }
    }
}
