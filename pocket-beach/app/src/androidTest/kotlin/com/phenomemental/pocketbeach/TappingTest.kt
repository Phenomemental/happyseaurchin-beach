package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.floorDepth
import com.phenomemental.pocketbeach.bsp.parseJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** What Matthew tries in Piece 2: a block of his own from nothing, by tapping alone; walked; ten lines at its opening, wrapping it. */
@RunWith(AndroidJUnit4::class)
class TappingTest {
    @get:Rule
    val compose = createEmptyComposeRule()

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
