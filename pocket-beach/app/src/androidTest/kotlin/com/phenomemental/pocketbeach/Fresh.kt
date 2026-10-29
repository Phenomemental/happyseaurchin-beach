package com.phenomemental.pocketbeach

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Helpers for the screen tests: an empty pocket to start from, and the taps Matthew makes. */
object Fresh {
    val app: PocketApp get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PocketApp

    /** Clear every block and the last-saved time, then open the app on the Shore. */
    fun open(): ActivityScenario<MainActivity> {
        clear()
        return ActivityScenario.launch(MainActivity::class.java)
    }

    /** Ask's stage and the bench's, in a line, for a wait that times out. */
    fun state(): String {
        val a = app.asking
        val stage = a.stage
        val note = (stage as? com.phenomemental.pocketbeach.ui.Asking.Stage.Over)?.note?.let { ": $it" }.orEmpty()
        val bench = app.benching.stage.let { b -> b::class.simpleName + ((b as? com.phenomemental.pocketbeach.ui.Benching.Stage.Failed)?.why?.let { ": $it" }.orEmpty()) }
        return "Ask: ${stage::class.simpleName}$note; checking ${a.checking}, sorting ${a.sorting}, its lock ${if (a.lockedForTests) "held" else "free"}; " +
            "the last exchange: ${a.history.lastOrNull()?.let { "${it.question.take(60)} / ${it.note}" }}. The bench: $bench."
    }

    fun clear() {
        File(app.filesDir, "regions").deleteRecursively()
        app.lastSavedMs = null
        app.asking.reset()
        app.benching.reset()
        app.inbox = null
        app.getSharedPreferences("posts", 0).edit().clear().commit()
        app.unsent.clear()
        app.connectedForTests = null
        // The tests read the workings; the pictures of the new Ask turn them off.
        app.showDetails = true
        for (job in com.phenomemental.pocketbeach.model.Job.values()) app.set(job, com.phenomemental.pocketbeach.model.JobSettings.of(job))
        // A download kept part way, or a failure recorded, but never the model itself.
        File(app.getExternalFilesDir(null), "models/${com.phenomemental.pocketbeach.model.ModelStore.FILE}.part").delete()
        app.getSharedPreferences("model", 0).edit().remove("failed").commit()
    }
}

/**
 * Tap what carries [tag], scrolling it into view first when it stands on a
 * scrolling page (the foot of a page and a dialog don't scroll). The page
 * resizes as the keyboard comes or goes, so the tap waits until what it
 * taps has stopped moving, and lands on it.
 */
fun ComposeTestRule.tap(tag: String) {
    waitUntil(10_000) { onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    val node = onNodeWithTag(tag, useUnmergedTree = true)
    fun scroll() = try {
        node.performScrollTo()
    } catch (_: AssertionError) {
    }
    scroll()
    var last: androidx.compose.ui.geometry.Rect? = null
    var still = 0
    val until = System.currentTimeMillis() + 5_000
    while (still < 3 && System.currentTimeMillis() < until) {
        Thread.sleep(100)
        waitForIdle()
        val now = node.fetchSemanticsNode().boundsInRoot
        if (now == last) still++ else {
            still = 0
            last = now
        }
    }
    scroll()
    node.performClick()
}

fun ComposeTestRule.waitFor(tag: String, text: String? = null, timeoutMs: Long = 10_000) {
    val matcher = if (text == null) hasTestTag(tag) else hasTestTag(tag) and hasText(text, substring = true)
    try {
        waitUntil(timeoutMs) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
        // What the app was doing, so a wait that never ends says why.
        throw AssertionError("No \"$tag\"${text?.let { " saying \"$it\"" }.orEmpty()} after $timeoutMs ms. ${Fresh.state()}", e)
    }
}

/** Make a block by tapping "Make a new block" and typing its name. */
fun ComposeTestRule.makeBlock(name: String) {
    tap("new-block")
    waitFor("block-name")
    onNodeWithTag("block-name").performTextInput(name)
    waitUntil(10_000) { onAllNodes(hasTestTag("make-block") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
    onNodeWithTag("make-block").performClick()
    waitFor("standing", "$name at pocket.")
}

/** From where Matthew stands: "Add beneath" (or "Change this line"), type the line, wait for its landing, save. */
fun ComposeTestRule.write(action: String, line: String, landing: String? = null): SemanticsNodeInteraction {
    tap(action)
    waitFor("line-text")
    onNodeWithTag("line-text").performTextReplacement(line)
    waitUntil(10_000) { onAllNodes(hasTestTag("save") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
    if (landing != null) waitFor("landing", landing)
    return onNodeWithTag("save")
}

fun ComposeTestRule.save(action: String, line: String, landing: String? = null) {
    write(action, line, landing).performClick()
    waitFor("standing")
}
