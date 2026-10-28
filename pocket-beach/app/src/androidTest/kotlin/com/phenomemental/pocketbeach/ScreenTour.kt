package com.phenomemental.pocketbeach

import android.graphics.Bitmap
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every screen, light and dark, photographed as the phone shows it, so the
 * look is checked by eye before a release. The pictures are kept in the app's
 * files (files/screens); CI copies them off the emulator.
 */
@RunWith(AndroidJUnit4::class)
class ScreenTour {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val dir = File(Fresh.app.filesDir, "screens")

    private fun night(on: Boolean) {
        instrumentation.uiAutomation.executeShellCommand("cmd uimode night ${if (on) "yes" else "no"}").close()
        Thread.sleep(3000)
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(400)
        val picture = instrumentation.uiAutomation.takeScreenshot() ?: return
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { picture.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tour(theme: String) {
        Fresh.open().use {
            compose.waitFor("block:sunstone")
            shot("$theme-01-shore-empty")
            compose.tap("new-block")
            compose.waitFor("block-name")
            compose.onNodeWithTag("block-name").performTextInput("garden")
            compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("make-block") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
            shot("$theme-02-new-block")
            compose.tap("make-block")
            compose.waitFor("standing", "garden at pocket.")
            shot("$theme-03-block-new")
            compose.save("change", "The garden, through the year")
            compose.write("add", "Autumn: plant the bulbs before the first frost", landing = "A new line at 1,")
            shot("$theme-04-write-add")
            compose.tap("save")
            compose.waitFor("line:1")
            compose.save("add", "Winter: prune the roses and mulch the beds")
            compose.save("add", "Spring: sow the sweet peas along the fence")
            compose.tap("line:1")
            compose.waitFor("standing", "Autumn")
            compose.save("add", "Tulips by the gate, alliums behind the bench")
            compose.save("add", "Daffodils under the apple tree")
            shot("$theme-05-position")
            compose.tap("trail:")
            compose.waitFor("standing", "The garden")
            shot("$theme-06-block")
            for (i in 4..9) compose.save("add", "Line $i of the garden")
            compose.write("add", "The tenth line, which wraps the block", landing = "wraps it first")
            shot("$theme-07-write-wraps")
            compose.tap("save")
            compose.waitFor("wrapped")
            shot("$theme-08-wrapped")
            compose.tap("back")
            compose.waitFor("block:garden")
            shot("$theme-09-shore")
            compose.tap("block:whetstone")
            compose.waitFor("line:2")
            shot("$theme-10-teaching")
            compose.tap("line:2")
            compose.waitFor("standing", "Selection-shape")
            shot("$theme-11-teaching-position")
            compose.tap("back")
            compose.tap("back")
            compose.waitFor("about")
            compose.tap("about")
            compose.waitFor("thanks")
            shot("$theme-12-about")
        }
        companion(theme)
    }

    /** Piece 3's screens, with a stand-in for the companion and a wire that goes nowhere. */
    private fun companion(theme: String) {
        val app = Fresh.app
        val wire = NowhereWire()
        try {
            app.mind = app.gemma
            Fresh.open().use {
                compose.tap("ask-companion")
                compose.waitUntil(15_000) {
                    listOf("too-little-memory", "too-little-room", "get-model").any { compose.onAllNodes(hasTestTag(it), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
                }
                shot("$theme-13-ask-model")
            }
            app.mind = StandIn()
            app.wire = wire
            Fresh.open().use {
                compose.tap("ask-companion")
                compose.waitFor("make-shell")
                shot("$theme-14-ask-no-shell")
                compose.tap("Make the companion's shell")
                compose.waitFor("wearing", "Wearing pocket-companion")
                compose.onNodeWithTag("question").performTextInput("Do the hello world. My secret is purple walrus.")
                shot("$theme-15-ask-ready")
                compose.tap("ask")
                compose.waitFor("offered-post")
                shot("$theme-16-ask-proposal")
                compose.tap("not-now")
                compose.waitFor("wrong-place")
                shot("$theme-17-ask-not-now")
                compose.tap("back-to-proposal")
                compose.tap("yes")
                compose.waitFor("post-message")
                shot("$theme-18-ask-post")
                compose.tap("post")
                compose.waitFor("posted")
                shot("$theme-19-ask-posted")
                compose.tap("back")
                compose.tap("about")
                compose.tap("calls")
                compose.waitFor("hello-post")
                shot("$theme-20-calls-hello")
                compose.tap("hello-post")
                compose.waitFor("hello-message")
                shot("$theme-21-calls-confirm")
            }
            Fresh.clear()
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).setClass(app, MainActivity::class.java).setType("text/plain")
                .putExtra(android.content.Intent.EXTRA_TEXT, """{"_":"Notes from a friend","1":"The ferry leaves at nine","2":{"_":"Bring","1":"a coat","2":"the map"}}""")
                .putExtra(android.content.Intent.EXTRA_SUBJECT, "friend")
            androidx.test.core.app.ActivityScenario.launch<MainActivity>(send).use {
                compose.waitFor("received-opening")
                shot("$theme-22-received")
            }
        } finally {
            app.wire = com.phenomemental.pocketbeach.net.HttpsWire
            app.mind = app.gemma
        }
    }

    @Test
    fun everyScreenLightAndDark() {
        dir.deleteRecursively()
        try {
            night(false)
            tour("light")
            night(true)
            tour("dark")
        } finally {
            night(false)
        }
    }
}
