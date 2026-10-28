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

    fun clear() {
        File(app.filesDir, "regions").deleteRecursively()
        app.lastSavedMs = null
        app.asking.reset()
        app.benching.reset()
        app.inbox = null
        app.getSharedPreferences("posts", 0).edit().clear().commit()
        app.unsent.clear()
        app.connectedForTests = null
        // A download kept part way, or a failure recorded, but never the model itself.
        File(app.getExternalFilesDir(null), "models/${com.phenomemental.pocketbeach.model.ModelStore.FILE}.part").delete()
        app.getSharedPreferences("model", 0).edit().remove("failed").commit()
    }
}

/** Tap what carries [tag], scrolling it into view first when it stands on a scrolling page (the foot of a page and a dialog don't scroll). */
fun ComposeTestRule.tap(tag: String) {
    waitUntil(10_000) { onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    val node = onNodeWithTag(tag, useUnmergedTree = true)
    try {
        node.performScrollTo()
    } catch (_: AssertionError) {
    }
    node.performClick()
}

fun ComposeTestRule.waitFor(tag: String, text: String? = null, timeoutMs: Long = 10_000) {
    val matcher = if (text == null) hasTestTag(tag) else hasTestTag(tag) and hasText(text, substring = true)
    waitUntil(timeoutMs) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
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
