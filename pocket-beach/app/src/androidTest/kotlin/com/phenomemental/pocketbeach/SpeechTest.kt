package com.phenomemental.pocketbeach

import android.Manifest
import android.content.Context
import android.speech.SpeechRecognizer
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.phenomemental.pocketbeach.ui.busy
import com.phenomemental.pocketbeach.ui.WAITING
import com.phenomemental.pocketbeach.ui.Heard
import com.phenomemental.pocketbeach.ui.Recogniser
import com.phenomemental.pocketbeach.ui.Speech
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A recogniser that hears what the test says it heard, and remembers being let go. */
class StandInRecogniser : Recogniser {
    var heard: Heard? = null
    var destroyed = false
    override fun listen(heard: Heard) {
        this.heard = heard
        heard.ready()
    }
    override fun destroy() { destroyed = true }
}

/**
 * Speaking a line holds the phone's speech recogniser only while it listens
 * (Matthew's Pixel, 0.3.2: error 10, and Gboard's voice typing stopped while
 * the app ran). With a stand-in recogniser, since the emulator recognises no
 * speech: one at a time, let go on a tap, at the end, on an error, on leaving
 * the screen and in the background; never tried again by itself.
 */
@RunWith(AndroidJUnit4::class)
class SpeechTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @get:Rule
    val microphone: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val app get() = Fresh.app
    private val made = mutableListOf<StandInRecogniser>()
    private lateinit var make: (Context) -> Recogniser
    private lateinit var available: (Context) -> Boolean

    @Before
    fun standIn() {
        make = app.speech.make
        available = app.speech.available
        app.speech.make = { StandInRecogniser().also { made += it } }
        app.speech.available = { true }
    }

    @After
    fun asItWas() {
        app.speech.letGo()
        app.speech.make = make
        app.speech.available = available
    }

    private fun main(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    /** Never more than one recogniser held, and every one let go but the one listening. */
    private fun oneAtMost() {
        val held = made.count { !it.destroyed }
        assertTrue("one recogniser at most, $held held", held <= 1)
        assertEquals(app.speech.holding, held == 1)
    }

    /**
     * Tap Speak and wait until the recogniser it makes is listening: with the
     * keyboard up, the app hides it first and listens a moment later.
     */
    private fun listen() {
        val before = made.size
        compose.tap("speak")
        compose.waitUntil(5_000) { made.size == before + 1 }
        compose.waitFor("speak", "Listening")
    }

    private fun lineText(): String =
        compose.onNodeWithTag("line-text").fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()

    private fun openALine() {
        compose.makeBlock("spoken")
        compose.tap("add")
        compose.waitFor("speak")
    }

    @Test
    fun aTapToStopLetsItGoAndKeepsWhatItHeard() {
        Fresh.open().use {
            openALine()
            listen()
            assertEquals(1, made.size)
            assertTrue(app.speech.holding)
            main { made[0].heard!!.partial("plant the bulbs") }
            compose.waitFor("speech-heard", "plant the bulbs")
            compose.tap("speak")
            compose.waitFor("speak", "Speak the line")
            assertTrue("let go at once", made[0].destroyed)
            oneAtMost()
            assertEquals("plant the bulbs", lineText())
        }
    }

    @Test
    fun theEndOfListeningLetsItGo() {
        Fresh.open().use {
            openALine()
            listen()
            main { made[0].heard!!.done("prune the roses") }
            compose.waitFor("speak", "Speak the line")
            assertTrue(made[0].destroyed)
            oneAtMost()
            assertEquals("prune the roses", lineText())

            // Again: a new recogniser, the last one long gone.
            listen()
            assertEquals(2, made.size)
            oneAtMost()
            main { made[1].heard!!.failed(SpeechRecognizer.ERROR_NO_MATCH) }
            compose.waitFor("speech-note", "Nothing was heard")
            assertTrue("an error lets it go", made[1].destroyed)
            oneAtMost()
        }
    }

    @Test
    fun busySpeechIsTriedOnceMoreWhereHeSeesItThenSaidPlainly() {
        Fresh.open().use {
            openALine()
            listen()
            main { made[0].heard!!.failed(SpeechRecognizer.ERROR_TOO_MANY_REQUESTS) }
            // 0.7: busy once, one try more, and he sees it waiting.
            compose.waitFor("speech-note", WAITING)
            assertTrue(made[0].destroyed)
            compose.waitUntil(5_000) { made.size == 2 }
            oneAtMost()
            // Busy again: which error, by its number, so Matthew's test says which; nothing more is tried by itself.
            main { made[1].heard!!.failed(SpeechRecognizer.ERROR_TOO_MANY_REQUESTS) }
            compose.waitFor("speech-note", busy(SpeechRecognizer.ERROR_TOO_MANY_REQUESTS))
            compose.waitFor("speech-note", "the phone's error 10")
            assertTrue(made[1].destroyed)
            Thread.sleep(Speech.RETRY_MS + 500)
            assertEquals("busy twice: nothing more tried by itself", 2, made.size)

            // A tap while it's busy makes nothing.
            compose.tap("speak")
            compose.waitFor("speech-note", busy(SpeechRecognizer.ERROR_TOO_MANY_REQUESTS))
            Thread.sleep(1_500)
            assertEquals("no recogniser made while speech is busy", 2, made.size)

            // After the wait, a tap listens again.
            Thread.sleep(Speech.BUSY_MS)
            listen()
            assertEquals(3, made.size)
            oneAtMost()
        }
    }

    @Test
    fun theKeyboardGoesBeforeListening() {
        Fresh.open().use { scenario ->
            openALine()
            compose.onNodeWithTag("line-text").performClick()
            fun keyboardUp(): Boolean {
                var up = false
                scenario.onActivity { a -> up = ViewCompat.getRootWindowInsets(a.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true }
                return up
            }
            val shown = runCatching { compose.waitUntil(5_000) { keyboardUp() } }.isSuccess
            assumeTrue("this emulator shows no keyboard", shown)
            compose.tap("speak")
            compose.waitUntil(5_000) { !keyboardUp() }
            compose.waitUntil(5_000) { made.size == 1 }
            oneAtMost()
        }
    }

    @Test
    fun leavingTheScreenLetsItGo() {
        Fresh.open().use {
            openALine()
            listen()
            compose.tap("back")
            compose.waitUntil(10_000) { made[0].destroyed }
            assertFalse(app.speech.holding)
        }
    }

    @Test
    fun theAppInTheBackgroundLetsItGo() {
        Fresh.open().use { scenario ->
            openALine()
            listen()
            // As when Matthew goes to another app, where Gboard's voice typing must work.
            scenario.moveToState(Lifecycle.State.CREATED)
            compose.waitUntil(10_000) { made[0].destroyed }
            assertFalse(app.speech.holding)
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.waitFor("speak", "Speak the line")
        }
    }
}
