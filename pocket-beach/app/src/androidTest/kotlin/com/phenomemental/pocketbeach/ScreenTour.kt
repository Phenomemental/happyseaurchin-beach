package com.phenomemental.pocketbeach

import android.graphics.Bitmap
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
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
            val standIn = StandIn()
            app.mind = standIn
            app.wire = wire
            // 0.5: the new Ask shows only the conversation; its workings are behind the Details switch.
            app.showDetails = false
            standIn.talkScript += listOf(
                "Here's the hello world with your secret. Say yes below, and then I'll offer the post.",
                "Again, then: the same block with your new secret, and the post after it.",
            )
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("ask-companion")
                compose.waitFor("make-shell")
                shot("$theme-14-ask-no-shell")
                compose.tap("Make the companion's shell")
                compose.waitFor("question")
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
            app.showDetails = true
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
            // 0.6.3, a plain chat: his walk of 31 October, four turns, the Write job's proposal waiting beneath her second reply while he carries on;
            // then with Details on. His first two lines are as the phone heard him, her replies as the laptop drew them (E2B, her instruction as released, seed 0); the last two turns are written for the picture.
            app.asking.reset()
            app.showDetails = false
            app.pages.carryOut(app.pages.planNewBlock("book"))
            app.pages.carryOut(app.pages.planChange("book", "", "The book"))
            standIn.talkScript.clear()
            standIn.script.clear()
            standIn.talkScript += listOf(
                "That sounds like a lot of thoughts swirling around right now, Matthew. It's good that you're finding time for walks. What kind of things are you thinking about for the book right now?",
                "Okay.",
                "Slow going is still going, and liking it counts for a lot.",
                "Enjoy the river. I'm here when you want to pick it up again.",
            )
            standIn.script += listOf(
                "talk\nNothing to keep.",
                "act\nadd book: I've started to learn video editing tools and have written half of the book.",
                "talk\nNothing to keep.",
                "talk\nNothing to keep.",
            )
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("ask-companion")
                for ((i, line) in listOf(
                    "Hey Gemma Matthew here I'm just walking along the canal on the way up to the river NEN it's pretty chilly I'm wrapped up warm enough there's no rain and I'm just " +
                        "if I'm honest my mind is a wash I want to write this book but also want to make a film and I also want to keep working on the beach and create apps and things " +
                        "it's there's a lot in my mind at the moment I know what it's about really it's about somebody watching something or reading something or experiencing " +
                        "something that just goes wow you know what you created this my god that's so beautiful you know like poets do you know I can film documentary makers do " +
                        "I've got that kind of thing in me and I really want to I want your support Gemma to just sort of keep keep oppressed of the things I say and hopefully we can help shape them over time",
                    "Yeah they are buzzing with ideas but there's also a practical side to this as well of started to learn video editing tools of written half of the book you know this " +
                        "so it's not just full hardiness and yes I would like you to to set some of this down for me please",
                    "Resolve mostly. It's slow going but I like it.",
                    "Not sure yet. Nearly at the river now.",
                ).withIndex()) {
                    compose.waitFor("question")
                    compose.onNodeWithTag("question").performTextInput(line)
                    compose.tap("ask")
                    compose.waitUntil(10_000) { app.asking.history.size == i + 1 && app.asking.settled }
                }
                compose.waitFor("yes")
                shot("$theme-32-ask-chat")
                compose.onNodeWithTag("yes").performScrollTo()
                shot("$theme-32b-ask-chat-proposal")
                app.showDetails = true
                compose.waitFor("wearing")
                compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("took")).fetchSemanticsNodes().size >= 4 }
                compose.onNodeWithTag("yes").performScrollTo()
                shot("$theme-32c-ask-chat-details-proposal")
                compose.onNodeWithTag("question").performTextInput("Let's keep the walk too.")
                shot("$theme-32d-ask-chat-details-typing")
            }
            // The conversation: a plain answer, then a proposal whose quote matches two lines, put to Matthew, then planned.
            app.asking.reset()
            app.showDetails = false
            app.pages.carryOut(app.pages.planNewBlock("orchard"))
            app.pages.carryOut(app.pages.planAdd("orchard", "", "Apples: pick in the autumn"))
            app.pages.carryOut(app.pages.planAdd("orchard", "", "Pears: prune in the winter"))
            standIn.talkScript.clear()
            standIn.talkScript += listOf(
                "I'm the pocket companion. I read your blocks freely, and write only when you say yes.",
                "I'd note it under the line about the season. Two lines speak of one, so tap the one you mean.",
                "Before June, then: the blossom will have set by then, so netting soon is wise.",
            )
            standIn.script += listOf(
                "act\nadd orchard under \"in the\": net the trees against the birds",
                "talk\nThat sounds like a good plan.",
            )
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("ask-companion")
                compose.waitFor("question")
                compose.onNodeWithTag("question").performTextInput("What's your name?")
                compose.tap("ask")
                compose.waitFor("say", "I'm the pocket companion.")
                compose.waitUntil(10_000) { app.asking.settled }
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
                // The same conversation with its workings shown: her shell, how each ask ended, and how long it took.
                app.showDetails = true
                compose.waitFor("wearing")
                shot("$theme-37-ask-details")
                compose.tap("keep-in")
                compose.waitFor("writes")
                shot("$theme-38-ask-details-proposal")
            }
            app.asking.reset()
            // 0.6, his day told: her reply; 0.6.1, "Sort as my day" under it, then the day sorted as one proposal; a line moved, a word put right, and the move kept.
            app.showDetails = false
            standIn.talkScript.clear()
            standIn.talkScript += "That sounds like a hard morning, and a lighter walk home."
            standIn.sortWords += mapOf(
                "Felt rotten about the morning" to "felt", "that hot-faced thing" to "felt", "felt lighter by the end of it" to "felt",
                "I've been putting that car off for weeks" to "thought", "that I'm always the one apologising" to "thought",
                "The thing I keep coming back to is the kids seeing me calm" to "matters", "I've decided I'm ringing the garage tomorrow" to "chose",
            )
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("ask-companion")
                compose.waitFor("question")
                compose.onNodeWithTag("question").performTextInput(com.phenomemental.pocketbeach.bsp.Telling.WRITTEN)
                compose.tap("ask")
                compose.waitFor("sort-as-day")
                compose.onNodeWithTag("sort-as-day").performScrollTo()
                shot("$theme-38b-ask-sort-as-day")
                compose.tap("sort-as-day")
                compose.waitFor("day-yes", timeoutMs = 30_000)
                shot("$theme-39-ask-day")
                val day = app.asking.waiting as com.phenomemental.pocketbeach.ui.Asking.Stage.Day
                val sam = day.lines.indexOfFirst { it.words.contains("Sam said something that stuck with me") }.takeIf { it >= 0 }
                    ?: day.lines.indexOfFirst { it.place != null }
                compose.tap("day-move:$sam")
                compose.waitFor("day-to:1.3")
                shot("$theme-40-ask-day-move")
                compose.tap("day-to:1.3")
                compose.waitFor("day-move:$sam")
                shot("$theme-41-ask-day-moved")
                // A word the phone misheard, put right by a tap on the line.
                val car = day.lines.indexOfFirst { it.words.contains("car wouldn't start") }.takeIf { it >= 0 } ?: 0
                compose.tap("day-words:$car")
                compose.waitFor("day-correct-field")
                shot("$theme-41b-ask-day-correct")
                compose.tap("day-correct-not-now")
                compose.waitFor("day-yes")
                compose.tap("day-yes")
                compose.waitFor("over", "kept as an example")
                shot("$theme-42-ask-day-kept")
            }
            app.asking.reset()
            // Settings: each job's settings, as Edge shows them, and the bigger model to fetch.
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("settings")
                compose.waitFor("job-talk")
                shot("$theme-35-settings")
                compose.tap("job-write-reply-less")
                compose.waitFor("job-write-reset")
                shot("$theme-35a-settings-write")
                compose.tap("job-write-reset")
                compose.tap("job-sort-reply")
                shot("$theme-35b-settings-sort")
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
