package com.phenomemental.pocketbeach

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.floorDepth
import com.phenomemental.pocketbeach.bsp.parseJson
import java.io.File
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** What Matthew tries in Piece 2: a block of his own from nothing, by tapping alone; walked; ten lines at its opening, wrapping it. */
@RunWith(AndroidJUnit4::class)
class TappingTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @get:Rule
    val microphone: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun aBlockIsBuiltWalkedAndWrappedByTapping() {
        Fresh.open().use {
            compose.makeBlock("garden")
            compose.save("change", "My garden")
            compose.waitFor("standing", "My garden")

            compose.save("add", "autumn", landing = "A new line at 1,")
            compose.waitFor("line:1", "autumn")
            compose.tap("line:1")
            compose.waitFor("standing", "autumn")
            compose.save("add", "plant the bulbs", landing = "A new line at 1.1, beneath “autumn”")
            compose.waitFor("line:1.1", "plant the bulbs")
            compose.tap("trail:")
            compose.waitFor("standing", "My garden")

            for (i in 2..9) compose.save("add", "line $i", landing = "A new line at $i,")
            compose.waitFor("full", "Nine lines at the opening")
            compose.write("add", "line 10", landing = "wraps it first").performClick()
            compose.waitFor("wrapped", "This block has wrapped")
            compose.waitFor("owed", "A summary is owed at 10")
            compose.waitFor("line:11", "line 10")
            compose.waitFor("line:01", "autumn")

            val stored = parseJson(File(Fresh.app.filesDir, "regions/pocket/garden.json").readText()) as JsObj
            assertEquals(2, floorDepth(stored))
        }
    }

    /**
     * A block saved as a file is the block whole: shared and copied from the
     * menu, 0.4.1's bench came through cut short, at about 19,000 characters.
     * The phone's file picker is stood in for, answering with a file of the test's.
     */
    @Test
    fun aBlockIsSavedAsAFileWhole() {
        Fresh.clear()
        val pages = Fresh.app.pages
        pages.carryOut(pages.planNewBlock("long"))
        repeat(9) { i -> pages.carryOut(pages.planAdd("long", "", "line ${i + 1}: " + "words ".repeat(400))) }
        val file = File(Fresh.app.cacheDir, "saved-long.json").also { it.delete() }
        Intents.init()
        try {
            Intents.intending(hasAction(Intent.ACTION_CREATE_DOCUMENT))
                .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file))))
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("block:long")
                compose.waitFor("standing", "long at pocket.")
                compose.tap("save-file")
                compose.waitFor("slip", "Saved long as a file, whole.")
            }
            Intents.intended(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT), hasExtra(Intent.EXTRA_TITLE, "long.json")))
            val whole = Fresh.app.storage.read("long")!!
            assertTrue("longer than the share that came through cut short: ${whole.length}", whole.length > 20_000)
            assertEquals(whole, file.readText())
        } finally {
            Intents.release()
        }
    }

    @Test
    fun aBlockIsRenamedThenRemovedFromTheShore() {
        Fresh.clear()
        val pages = Fresh.app.pages
        for (name in listOf("garden", "notes")) pages.carryOut(pages.planNewBlock(name))
        pages.carryOut(pages.planAdd("garden", "", "autumn"))
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("tidy")
            compose.tap("rename:garden")
            compose.waitFor("rename-name")
            compose.onNodeWithTag("rename-name").performTextReplacement("notes")
            compose.waitFor("refusal", "There's already a block called notes.")
            compose.onNodeWithTag("rename-block").assertIsNotEnabled()
            compose.onNodeWithTag("rename-name").performTextReplacement("allotment")
            compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("rename-block") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
            compose.tap("rename-block")
            compose.waitFor("tidy:allotment")
            assertEquals(false, pages.exists("garden"))
            assertEquals("autumn", pages.position(com.phenomemental.pocketbeach.bsp.Region.POCKET, "allotment")!!.lines.single().text)

            compose.tap("remove:notes")
            compose.waitFor("remove-block")
            compose.tap("remove-block")
            compose.waitUntil(10_000) { !pages.exists("notes") }
            compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("tidy:notes")).fetchSemanticsNodes().isEmpty() }
            compose.tap("tidy")
            compose.tap("block:allotment")
            compose.waitFor("standing", "garden at pocket.")
        }
    }

    @Test
    fun aFullLineTakesNoMore() {
        Fresh.open().use {
            compose.makeBlock("box")
            compose.save("add", "shelf")
            compose.tap("line:1")
            compose.waitFor("standing", "shelf")
            for (i in 1..8) compose.save("add", "item $i")
            compose.waitFor("full", "Every place beneath this line is taken")
            compose.onNodeWithTag("add").assertIsNotEnabled()
        }
    }

    @Test
    fun speakingEndsInAPlainSentenceAndNeverAServer() {
        Fresh.open().use {
            compose.makeBlock("spoken")
            compose.tap("add")
            compose.waitFor("speak")
            compose.tap("speak")
            // On a phone with on-device recognition this listens, and either writes what it heard
            // into the line or says it heard nothing; on one without, it says speaking is off.
            // Either way it ends in words, never in silence and never at a server.
            compose.waitUntil(30_000) {
                compose.onAllNodes(hasTestTag("speech-note")).fetchSemanticsNodes().isNotEmpty() ||
                    compose.onNodeWithTag("line-text").fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text?.isNotEmpty() == true
            }
        }
    }

    @Test
    fun aTeachingBlockIsWalkedAndReadOnly() {
        Fresh.open().use {
            compose.tap("block:whetstone")
            compose.waitFor("line:2", "Selection-shape derivation")
            compose.tap("line:2")
            compose.waitFor("standing", "Selection-shape derivation")
            compose.onNodeWithTag("trail:").assertTextContains("whetstone")
            assertEquals(0, compose.onAllNodes(hasTestTag("add")).fetchSemanticsNodes().size)
        }
    }
}
