package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sundial 0.12, the editor (spine:pocket-beach 1.111178719994): Your pocket
 * finds a block by its words; a block opens in pscale structure; his words
 * change in place; removing and moving a part are set out and written only
 * on his yes; a new block starts as its opening line.
 */
@RunWith(AndroidJUnit4::class)
class EditorDeviceTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = Fresh.app

    private fun orchard() = check(app.pages.carryOut(app.pages.planKeep("orchard", JsObj.of(
        "_" to str("The orchard, through the year."),
        "1" to JsObj.of("_" to str("Autumn"), "1" to str("pick the apples"), "2" to str("rake the leaves")),
        "2" to str("Winter: prune the pears"),
        "3" to str("Spring: net the trees"),
    ))).ok)

    private fun text(address: String) = app.pages.position(Region.POCKET, "orchard", address)?.text

    private fun count(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun aBlockOpensInItsStructureAndHisWordsChangeInPlace() {
        Fresh.clear()
        orchard()
        check(app.pages.carryOut(app.pages.planNewBlock("letters")).ok)
        ActivityScenario.launch(MainActivity::class.java).use {
            // Found by its words.
            compose.waitFor("pocket-search")
            compose.onNodeWithTag("pocket-search").performTextInput("through the year")
            compose.waitUntil(5_000) { count("block:letters") == 0 }
            compose.tap("block:orchard")
            compose.waitFor("outline-line:", "The orchard, through the year.")
            compose.waitFor("outline-line:1.1", "pick the apples")
            compose.waitFor("outline-line:3", "Spring: net the trees")
            compose.tap("outline-line:2")
            compose.tap("change-words")
            compose.waitFor("edit-field")
            compose.onNodeWithTag("edit-field").performTextReplacement("Winter: prune the pears and the plums")
            compose.tap("keep-words")
            compose.waitUntil(10_000) { text("2") == "Winter: prune the pears and the plums" }
            compose.waitFor("outline-line:2", "and the plums")
        }
    }

    @Test
    fun removingOrMovingAPartWaitsForHisYes() {
        Fresh.clear()
        orchard()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("block:orchard")
            compose.waitFor("outline-line:1", "Autumn")
            // Removed: set out with what goes, nothing written before his yes.
            compose.tap("outline-line:3")
            compose.tap("remove-part")
            compose.waitFor("change-said", "Remove 3, “Spring: net the trees”.")
            assertEquals("Spring: net the trees", text("3"))
            compose.tap("change-not-now")
            assertEquals("Spring: net the trees", text("3"))
            compose.tap("outline-line:3")
            compose.tap("remove-part")
            compose.tap("change-yes")
            compose.waitUntil(10_000) { text("3") == null }
            // Moved, with everything beneath it: autumn beneath winter.
            compose.tap("outline-line:1")
            compose.tap("move-part")
            compose.waitFor("move-ask")
            compose.tap("outline-line:2")
            compose.waitFor("change-said", "Move 1, “Autumn”, with two lines beneath it, to stand beneath 2, at 2.1.")
            assertEquals("Autumn", text("1"))
            compose.tap("change-yes")
            compose.waitUntil(10_000) { text("2.1") == "Autumn" }
            assertEquals("pick the apples", text("2.11"))
            assertNull(text("1"))
            compose.waitFor("outline-line:2.12", "rake the leaves")
            // Added beneath: set out with where it lands.
            compose.tap("outline-line:2")
            compose.tap("add-part")
            compose.waitFor("part-field")
            compose.onNodeWithTag("part-field").performTextInput("mulch the beds")
            compose.tap("set-out-part")
            compose.waitFor("change-said", "Add “mulch the beds” beneath 2, at 2.2.")
            compose.tap("change-yes")
            compose.waitUntil(10_000) { text("2.2") == "mulch the beds" }
        }
    }

    @Test
    fun aNewBlockStartsAsItsOpeningLine() {
        Fresh.clear()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("new-block")
            compose.waitFor("block-name")
            compose.onNodeWithTag("block-name").performTextInput("letters")
            compose.onNodeWithTag("block-root").performTextInput("Letters I mean to write.")
            compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("make-block") and androidx.compose.ui.test.isEnabled()).fetchSemanticsNodes().isNotEmpty() }
            compose.tap("make-block")
            compose.waitFor("outline-line:", "Letters I mean to write.")
            assertEquals("Letters I mean to write.", app.pages.position(Region.POCKET, "letters")!!.text)
            assertTrue("one line, its opening", app.pages.outline(Region.POCKET, "letters")!!.size == 1)
        }
    }
}
