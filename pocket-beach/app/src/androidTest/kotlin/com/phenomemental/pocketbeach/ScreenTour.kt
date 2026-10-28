package com.phenomemental.pocketbeach

import android.graphics.Bitmap
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
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
            // A download kept part way, then one that failed, as the screen says them.
            val part = java.io.File(app.getExternalFilesDir(null), "models/${com.phenomemental.pocketbeach.model.ModelStore.FILE}.part")
            part.parentFile!!.mkdirs()
            part.writeBytes(ByteArray(1 shl 20))
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("ask-companion")
                compose.waitFor("paused")
                shot("$theme-13a-ask-paused")
            }
            app.getSharedPreferences("model", 0).edit().putString("failed", "Hugging Face sent the download on to eu.example.hf.co, a host the pocket doesn't trust, so it stopped there.").commit()
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("ask-companion")
                compose.waitFor("model-failed")
                shot("$theme-13b-ask-failed")
            }
            Fresh.clear()
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
                // The companion's post is this same message, so it shows as posted, and goes again only on purpose.
                compose.waitFor("hello-said")
                shot("$theme-20-calls-hello")
                compose.tap("hello-post-again")
                compose.waitFor("hello-message")
                shot("$theme-21-calls-confirm")
            }
            // The hello world again, with the block there: the app's own seal and fingerprint shown with the companion's step.
            app.asking.reset()
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("ask-companion")
                compose.waitFor("question")
                compose.onNodeWithTag("question").performTextInput("Do the hello world again. My secret is otters hold hands.")
                compose.tap("ask")
                compose.waitFor("by-app")
                shot("$theme-23-ask-again")
                compose.tap("back")
                compose.tap("tidy")
                compose.waitFor("tidy:hello-world")
                shot("$theme-24-shore-tidy")
                compose.tap("rename:hello-world")
                compose.waitFor("rename-name")
                shot("$theme-25-rename")
                compose.tap("Not now")
                compose.tap("remove:pocket-companion")
                compose.waitFor("remove-block")
                shot("$theme-26-remove")
                compose.tap("Keep it")
            }
            // A post the beach couldn't be reached for, kept on the Shore: offline, then back online.
            val kept = com.phenomemental.pocketbeach.bsp.PoolPost.prepare(
                com.phenomemental.pocketbeach.bsp.HelloWorld.POOL,
                com.phenomemental.pocketbeach.bsp.HelloWorld.messageFor(app.storage.read(com.phenomemental.pocketbeach.bsp.HelloWorld.BLOCK)) ?: "Hello world from the pocket.",
            )
            app.unsent.keep(kept, "The beach couldn't be reached, so nothing was posted (Unable to resolve host \"beach.happyseaurchin.com\").", false, System.currentTimeMillis())
            app.connectedForTests = false
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
                compose.waitFor("waiting-why", "Waiting for a connection")
                shot("$theme-27-shore-waiting-offline")
                app.connectedForTests = true
                compose.waitFor("waiting-why", "only when you tap Post now")
                shot("$theme-28-shore-waiting-online")
            }
            app.connectedForTests = null
            app.unsent.clear()
            // The bench, run with the stand-in, and the block it keeps; it is offered between asks, so the Ask still waiting on Yes is let go.
            app.asking.reset()
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("ask-companion")
                compose.tap("open-bench")
                compose.waitFor("run-bench")
                shot("$theme-29-bench")
                compose.tap("run-bench")
                compose.waitFor("bench-saved", timeoutMs = 120_000)
                shot("$theme-30-bench-saved")
                compose.tap("open-bench-block")
                compose.waitFor("standing", "Bench, ")
                shot("$theme-31-bench-block")
            }
            // 0.4's conversation: a plain answer, then an act whose quote matches two lines, put to Matthew, then planned.
            app.asking.reset()
            app.pages.carryOut(app.pages.planNewBlock("orchard"))
            app.pages.carryOut(app.pages.planAdd("orchard", "", "Apples: pick in the autumn"))
            app.pages.carryOut(app.pages.planAdd("orchard", "", "Pears: prune in the winter"))
            (app.mind as StandIn).script += listOf(
                "talk\nI'm the pocket companion. I read your blocks freely, and write only when you say yes.",
                "act\nadd orchard under \"in the\": net the trees against the birds",
                "talk\nThat sounds like a good plan.",
            )
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("ask-companion")
                compose.waitFor("question")
                compose.onNodeWithTag("question").performTextInput("What's your name?")
                compose.tap("ask")
                compose.waitFor("over", "She talked.")
                shot("$theme-32-ask-talk")
                compose.onNodeWithTag("question").performTextClearance()
                compose.onNodeWithTag("question").performTextInput("In orchard, under the line about the season, note: net the trees against the birds.")
                compose.tap("ask")
                compose.waitFor("which-line")
                shot("$theme-33-ask-which-line")
                compose.tap("choose:1")
                compose.waitFor("writes")
                shot("$theme-34-ask-chosen")
                // Something told that names a block, which she only talks about: the app offers to keep it there.
                compose.tap("not-now")
                compose.tap("just-not-now")
                compose.waitFor("question")
                compose.onNodeWithTag("question").performTextClearance()
                compose.onNodeWithTag("question").performTextInput("The orchard needs netting before June.")
                compose.tap("ask")
                compose.waitFor("keep-in", "Keep this in orchard?")
                shot("$theme-36-ask-keep")
            }
            app.asking.reset()
            // Settings: whether each answer shows its time.
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("settings")
                compose.waitFor("show-timings")
                shot("$theme-35-settings")
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
            app.connectedForTests = null
            app.unsent.clear()
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
