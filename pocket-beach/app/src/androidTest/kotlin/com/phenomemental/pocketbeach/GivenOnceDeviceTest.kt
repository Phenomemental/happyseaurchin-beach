package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.Chats
import com.phenomemental.pocketbeach.bsp.Region
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
 * Sundial 0.13.5, after a conversation on his phone broke down as it filled:
 * each turn naming his passport handed it her again (even "Stop reading the
 * passport") until her conversation held 7,904 of 8,192 tokens. A block given
 * her in a conversation is never given again in it, named, walked or added
 * with the paperclip; and how full the conversation is shows beneath it,
 * nearly full said plainly, with a new chat offered.
 */
@RunWith(AndroidJUnit4::class)
class GivenOnceDeviceTest {
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

    private val block = "(His block passport:Phenomemental"

    @Test
    fun hisPassportIsGivenHerOnceInAConversationNamedOrAdded() {
        Fresh.hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say("what does my passport tell you about how to work with me")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 1 }
            assertTrue(mind.talkMessages[0], mind.talkMessages[0].contains(block))
            // Named again, as on his phone: not given again.
            say("Stop reading the passport")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 2 }
            assertFalse(mind.talkMessages[1], mind.talkMessages[1].contains(block))
            compose.waitFor("file-chip-given", "given earlier in this chat")
            assertTrue(app.asking.transcript(), app.asking.transcript().contains("Given her earlier in this conversation, so not again: passport:Phenomemental."))
            // Added with the paperclip: not given again either.
            app.asking.attach(Chats.blockFile(app.pages, Region.POCKET, "passport:Phenomemental")!!)
            say("and this?")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 3 }
            assertFalse(mind.talkMessages[2], mind.talkMessages[2].contains("His block passport:Phenomemental"))
            // A new chat: given her again, once.
            compose.tap("new-conversation")
            compose.waitUntil(10_000) { app.asking.history.isEmpty() }
            say("what does my passport say?")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 4 }
            assertTrue(mind.talkMessages[3], mind.talkMessages[3].contains(block))
        }
    }

    @Test
    fun aWalkOfWhatWasGivenIsNotHandedAgainButANewPlaceIs() {
        Fresh.hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say("what does my passport tell you about how to work with me")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 1 }
            // The next turn, the walker still offered: she walks the passport's opening again.
            mind.walkArgs = mapOf("block" to "passport:Phenomemental")
            say("do what you think is best")
            compose.waitUntil(20_000) { app.asking.settled && mind.walkResults.size == 1 }
            assertTrue(mind.walkResults[0], mind.walkResults[0].startsWith("His block passport:Phenomemental was given you earlier in this conversation; it is above."))
            compose.waitFor("walked-chip", "given earlier: passport:Phenomemental")
            // A place not yet given is walked.
            mind.walkArgs = mapOf("block" to "passport:Phenomemental", "spindle" to "1")
            say("read my passport at 1")
            compose.waitUntil(20_000) { app.asking.settled && mind.walkResults.size == 2 }
            assertTrue(mind.walkResults[1], mind.walkResults[1].contains("1  The heart."))
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
