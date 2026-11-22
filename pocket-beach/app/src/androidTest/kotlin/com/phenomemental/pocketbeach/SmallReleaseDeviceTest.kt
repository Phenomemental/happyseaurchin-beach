package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.str
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
 * 0.8.1, the small release after 0.8 (spine:pocket-beach 1.1111787136), on
 * the phone with a stand-in for the model: the standard chat on the screen,
 * opened on once he has hatched. Sundial 0.12 took the chat's posts and
 * reads out (the recipes are held), and their checks with them.
 */
@RunWith(AndroidJUnit4::class)
class SmallReleaseDeviceTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = Fresh.app
    private val wire = NowhereWire()
    private val mind = StandIn()

    @Before
    fun standIns() {
        app.wire = wire
        app.mind = mind
    }

    @After
    fun backAsTheyWere() {
        app.wire = HttpsWire
        app.mind = app.gemma
    }

    /** His passport as long as the one on the beach: places well past what she is read of a block. */
    private val passport = JsObj.of(
        "_" to str("Matthew Bushell — digital social innovator, aspiring writer."),
        "1" to str("The heart. bi-polar · one axis, two ends. " + "whole-less, as in boundless. ".repeat(60)),
        "2" to str("The other side of the &: people who will meet me in the middle and build with, not at. " + "LEROs. ".repeat(120)),
        "6" to str("WHERE THINGS GO — for an AI working with me. " + "Confirmed in conversation. ".repeat(60)),
    )

    /** A pocket hatched as Phenomemental, his passport adopted, before the app opens. */
    private fun hatched() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        check(app.pages.carryOut(app.pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
        val planned = Hatch.plan(Hatch.MATTHEW_HANDLE, passport, app.pages, app.storage, System.currentTimeMillis(), "Adopted on this phone for the test.")
        check(Hatch.carryOut(planned, app.pages, app.storage).problem == null)
    }

    private fun say(words: String) {
        compose.waitFor("question")
        compose.onNodeWithTag("question").performTextInput(words)
        compose.tap("ask")
    }

    @Test
    fun theStandardChatIsOnTheScreenAndOpensOnceHeHasHatched() {
        hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            // Opened on the chat, not the Shore.
            compose.waitFor("top-bar")
            compose.waitFor("her-name", "Gemma")
            compose.waitFor("wearing", "wearing your shell")
            for (tag in listOf("menu", "new-conversation", "input-bar", "question", "speak", "ask")) compose.waitFor(tag)
            assertTrue("the Shore isn't shown first", compose.onAllNodes(hasTestTag("ask-companion")).fetchSemanticsNodes().isEmpty())
            say("Hello")
            compose.waitFor("his", "Hello")
            compose.waitFor("say", timeoutMs = 20_000)
            // The menu holds the rest of the app.
            compose.tap("menu")
            for (tag in listOf("menu-blocks", "menu-teaching", "open-bench", "save-conversation", "details", "menu-settings", "menu-about")) compose.waitFor(tag)
            androidx.test.espresso.Espresso.pressBack()
            // A new conversation: the thread begins empty, the one before kept in his history.
            compose.tap("new-conversation")
            compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("his")).fetchSemanticsNodes().isEmpty() }
            compose.waitUntil(20_000) { app.pages.position(com.phenomemental.pocketbeach.bsp.Region.POCKET, "history:${Hatch.MATTHEW_HANDLE}")?.lines?.isNotEmpty() == true }
            // Your blocks: the Shore, beneath the chat.
            compose.menu("menu-blocks")
            compose.waitFor("ask-companion")
            assertFalse(app.asking.stage is com.phenomemental.pocketbeach.ui.Asking.Stage.Thinking)
        }
    }
}
