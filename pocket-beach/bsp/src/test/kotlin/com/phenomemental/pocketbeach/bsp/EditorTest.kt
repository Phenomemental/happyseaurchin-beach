package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sundial 0.12, the editor (spine:pocket-beach 1.111178719994): a block in
 * pscale structure; his words changed in place; a part added, moved or
 * removed worked out on a copy and written whole on his yes, held when the
 * block changed while it was open (0.11's guard).
 */
class EditorTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val nowMs = 1_795_000_000_000L

    private fun garden(): Pages {
        val (_, pages) = Bench.fixture(shelf, nowMs)
        check(pages.carryOut(pages.planKeep("orchard", JsObj.of(
            "_" to str("The orchard, through the year."),
            "1" to JsObj.of("_" to str("Autumn"), "1" to str("pick the apples"), "2" to str("rake the leaves")),
            "2" to str("Winter: prune the pears"),
            "3" to str("Spring: net the trees"),
        ))).ok)
        return pages
    }

    private fun shape(pages: Pages) = pages.outline(Region.POCKET, "orchard")!!.map { it.address to it.text }

    @Test
    fun aPartIsRemovedWithItsLinesAndTheRestKeepTheirPlaces() {
        val pages = garden()
        assertEquals(2, pages.linesBeneath("orchard", "1"))
        val plan = pages.planRemove("orchard", "1")
        assertNull(plan.refusal)
        assertEquals(Kind.WHOLE, plan.kind)
        assertTrue(pages.carryOut(plan, pages.seen("orchard")).ok)
        assertEquals(listOf("" to "The orchard, through the year.", "2" to "Winter: prune the pears", "3" to "Spring: net the trees"), shape(pages))
        // A line left with nothing beneath it is kept as the store keeps such a line.
        assertTrue(pages.carryOut(pages.planRemove("orchard", "3")).ok)
        assertEquals(listOf("" to "The orchard, through the year.", "2" to "Winter: prune the pears"), shape(pages))
        assertNotNull(pages.planRemove("orchard", "").refusal, "the opening line is the block's: removed from Your pocket")
        assertNotNull(pages.planRemove("orchard", "7").refusal)
    }

    @Test
    fun aPartMovesBeneathAnotherLineWithEverythingBeneathIt() {
        val pages = garden()
        assertEquals("2.1", pages.moveLandsAt("orchard", "1", "2"))
        val plan = pages.planMove("orchard", "1", "2")
        assertNull(plan.refusal)
        assertTrue(pages.carryOut(plan, pages.seen("orchard")).ok)
        assertEquals(
            listOf(
                "" to "The orchard, through the year.",
                "2" to "Winter: prune the pears",
                "2.1" to "Autumn", "2.11" to "pick the apples", "2.12" to "rake the leaves",
                "3" to "Spring: net the trees",
            ),
            shape(pages),
        )
        // Back to the opening line, at its next free place.
        assertTrue(pages.carryOut(pages.planMove("orchard", "2.12", "")).ok)
        assertEquals("rake the leaves", pages.position(Region.POCKET, "orchard", "1")!!.text)
        // Never beneath itself, nor where it stands already.
        assertNotNull(pages.planMove("orchard", "2", "2.1").refusal)
        assertNotNull(pages.planMove("orchard", "2.1", "2").refusal)
        assertNotNull(pages.planMove("orchard", "3", "8").refusal)
    }

    @Test
    fun aStructureChangeIsHeldWhenTheBlockChangedWhileItWasOpen() {
        val pages = garden()
        val seen = pages.seen("orchard")
        val plan = pages.planRemove("orchard", "2")
        // The block changes while the card waits for his yes: the whole is written only while it stands as planned.
        check(pages.carryOut(pages.planChange("orchard", "3", "Spring: net the cherries")).ok)
        val done = pages.carryOut(plan, seen)
        assertTrue(done.held, done.problem)
        assertEquals("Winter: prune the pears", pages.position(Region.POCKET, "orchard", "2")!!.text)
    }

    @Test
    fun hisWordsAreHeldWhenAnotherHandChangedTheBlock() {
        // His blocks in his Sundial folder, where another hand (a laptop) can write them too.
        val dir = java.nio.file.Files.createTempDirectory("sundial-editor").toFile()
        val storage = FolderStorage(dir)
        val pages = Pages(storage, shelf, PocketBsp(storage, shelf) { nowMs }) { nowMs }
        check(pages.carryOut(pages.planKeep("orchard", JsObj.of("_" to str("The orchard."), "1" to str("Autumn: pick the apples"), "2" to str("Winter: prune the pears")))).ok)
        val seen = pages.seen("orchard")
        val words = pages.planChange("orchard", "2", "Winter: prune the pears and the plums")
        // Another hand writes the block's file while his words wait.
        val file = dir.listFiles()!!.single { it.name.startsWith("orchard") }
        file.writeText(stringify(JsObj.of("_" to str("The orchard."), "1" to str("Autumn: pick the apples"), "2" to str("Winter: prune the pears"), "3" to str("Spring: net the trees")))!!)
        assertTrue(pages.carryOut(words, seen).held)
        assertEquals("Winter: prune the pears", pages.position(Region.POCKET, "orchard", "2")!!.text)
        // Seen again, his words land.
        assertTrue(pages.carryOut(pages.planChange("orchard", "2", "Winter: prune the pears and the plums"), pages.seen("orchard")).ok)
    }

    @Test
    fun aNewBlockStartsAsItsRootLine() {
        val (_, pages) = Bench.fixture(shelf, nowMs)
        assertTrue(pages.carryOut(pages.planKeep("letters", JsObj.of("_" to str("Letters I mean to write.")))).ok)
        assertEquals(listOf("" to "Letters I mean to write."), pages.outline(Region.POCKET, "letters")!!.map { it.address to it.text })
        assertTrue(pages.carryOut(pages.planAdd("letters", "", "To Jo, about the music")).ok)
        assertTrue(pages.carryOut(pages.planAdd("letters", "1", "say thank you first")).ok)
        assertEquals(listOf("" to 0, "1" to 1, "1.1" to 2), pages.outline(Region.POCKET, "letters")!!.map { it.address to it.depth })
    }
}
