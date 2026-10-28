package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The pages a front end shows, and the writes it offers, on a pocket held in memory. */
class PagesTest {
    private val storage = MemoryStorage()
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private var now = 1_793_178_000_000
    private val bsp = PocketBsp(storage, shelf) { now }
    private val pages = Pages(storage, shelf, bsp) { now }

    private fun done(plan: Plan): Done {
        assertNull(plan.refusal, plan.refusal)
        now += 1000
        return pages.carryOut(plan).also { assertTrue(it.ok, "${it.problem}\n${it.reply}") }
    }

    private fun root(name: String) = assertNotNull(pages.position(Region.POCKET, name))

    /** On a disk, as the phone keeps blocks, and as a Windows laptop would, where a name's capitals may share one file. */
    @Test
    fun aRenameOfCapitalsOnlyKeepsTheBlockOnDisk() {
        val dir = kotlin.io.path.createTempDirectory("pocket").toFile()
        try {
            val disk = FileStorage(dir)
            val onDisk = Pages(disk, shelf, PocketBsp(disk, shelf) { now }) { now }
            assertTrue(onDisk.carryOut(onDisk.planNewBlock("million")).ok)
            assertTrue(onDisk.carryOut(onDisk.planAdd("million", "", "a million pound by 2027")).ok)
            val before = disk.read("million")
            assertTrue(onDisk.rename("million", "Million").ok)
            assertEquals(listOf("Million"), disk.names())
            assertEquals(before, disk.read("Million"))
            assertEquals("a million pound by 2027", onDisk.position(Region.POCKET, "Million")!!.lines.single().text)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun aBlockIsRenamedOrRemovedWhole() {
        done(pages.planNewBlock("garden"))
        done(pages.planAdd("garden", "", "autumn"))
        done(pages.planNewBlock("notes"))
        val stored = storage.read("garden")
        val born = storage.stamps().born["garden"]
        assertEquals("That's its name already.", pages.renameRefusal("garden", " garden "))
        assertEquals("There's already a block called notes.", pages.renameRefusal("garden", "notes"))
        assertEquals("Give the block a name.", pages.renameRefusal("garden", "  "))
        assertEquals("There's no block called nowhere.", pages.renameRefusal("nowhere", "x"))
        assertNotNull(pages.renameRefusal("garden", "sed:garden"))
        assertFalse(pages.rename("garden", "notes").ok)
        assertEquals(stored, storage.read("garden"), "a refused rename changes nothing")

        now += 1000
        assertTrue(pages.rename("garden", " allotment ").ok)
        assertNull(storage.read("garden"))
        assertEquals(stored, storage.read("allotment"), "the same block, line for line")
        assertEquals(born, storage.stamps().born["allotment"])
        assertEquals(isoString(now), storage.stamps().touched["allotment"])
        assertFalse("garden" in storage.stamps().touched || "garden" in storage.stamps().born)
        assertEquals(setOf("allotment", "notes"), pages.shore().mine.map { it.name }.toSet())
        assertEquals("autumn", root("allotment").lines.single().text)

        assertTrue(pages.remove("notes").ok)
        assertFalse(pages.exists("notes"))
        assertFalse("notes" in storage.stamps().born || "notes" in storage.stamps().touched)
        assertEquals("There's no block called notes.", pages.remove("notes").problem)
        assertEquals(listOf("allotment"), pages.shore().mine.map { it.name })
        done(pages.planNewBlock("notes"))

        // A name differing only in its capitals is the same block's: no second one, but a block may change its own.
        assertEquals("There's already a block called notes.", pages.planNewBlock("Notes").refusal)
        assertEquals("There's already a block called allotment.", pages.renameRefusal("notes", "Allotment"))
        assertEquals("notes", pages.named("NOTES"))
        assertEquals("nowhere", pages.named("nowhere"))
        now += 1000
        assertTrue(pages.rename("notes", "Notes").ok)
        assertEquals("Notes", pages.named("notes"))
    }

    @Test
    fun aBlockIsBuiltByTappingAlone() {
        assertTrue(pages.shore().mine.isEmpty())
        done(pages.planNewBlock("garden"))
        assertEquals(listOf("garden"), pages.shore().mine.map { it.name })
        assertEquals("garden at pocket.", pages.shore().mine[0].opening)

        val renamed = pages.planChange("garden", "", "My garden")
        assertEquals(str("0"), renamed.call!!["spindle"])
        done(renamed)
        assertEquals("My garden", root("garden").text)

        val autumn = pages.planAdd("garden", "", "autumn")
        assertEquals("1", autumn.landsAt)
        done(autumn)
        val bulbs = pages.planAdd("garden", "1", "plant the bulbs")
        assertEquals("1.1", bulbs.landsAt)
        done(bulbs)

        assertEquals(listOf("1" to "autumn"), root("garden").lines.map { it.address to it.text })
        assertEquals(1, root("garden").lines[0].beneath)
        val at1 = assertNotNull(pages.position(Region.POCKET, "garden", "1"))
        assertEquals("autumn", at1.text)
        assertNotNull(at1.stamp, "an added line carries its arrival stamp")
        assertEquals(listOf("1.1" to "plant the bulbs"), at1.lines.map { it.address to it.text }, "the stamp at 3 is not a line")
        assertTrue(at1.trail.isEmpty())

        done(pages.planChange("garden", "1.1", "plant the tulips"))
        assertEquals("plant the tulips", pages.position(Region.POCKET, "garden", "1.1")!!.text)
    }

    @Test
    fun tenLinesAtTheRootWrapIt() {
        done(pages.planNewBlock("log"))
        for (i in 1..9) done(pages.planAdd("log", "", "line $i"))
        assertTrue(root("log").full, "nine lines: the next one wraps")
        assertEquals(0, root("log").wrapped)

        val tenth = pages.planAdd("log", "", "line 10")
        assertTrue(tenth.wraps)
        assertEquals("11", tenth.landsAt)
        assertTrue(done(tenth).wraps)

        val view = root("log")
        assertEquals(1, view.wrapped)
        assertFalse(view.full)
        assertEquals((1..9).map { "0$it" } + "11", view.lines.map { it.address })
        assertEquals((1..10).map { "line $it" }, view.lines.map { it.text })
        assertEquals(listOf("10" to "1-9"), view.owed.map { it.at to it.over })
        assertEquals("log at pocket.", view.text)

        assertEquals("12", pages.planAdd("log", "", "line 11").landsAt)
        val at11 = pages.position(Region.POCKET, "log", "11")!!
        assertEquals(listOf("10"), at11.trail.map { it.address }, "the second nine's container, its summary not yet written")
        assertNull(at11.trail[0].text)

        done(pages.planChange("log", "10", "lines one to nine"))
        assertTrue(root("log").owed.isEmpty())
        done(pages.planChange("log", "", "the log"))
        assertEquals("the log", root("log").text)
        assertEquals(10, root("log").lines.size)
    }

    @Test
    fun theOpeningLineOfABlockWrappedTwiceIsChangedWhole() {
        done(pages.planNewBlock("long"))
        repeat(91) { bsp.call(JsObj.of("block" to str("long"), "content" to str("entry ${it + 1}"), "append" to JsBool(true))) }
        assertEquals(3, floorDepth(parseJson(storage.read("long")!!)))
        val plan = pages.planChange("long", "", "a long log")
        assertNull(plan.call!!["spindle"], "re-voicing through \"0\" would reach a node too deep, so the block goes whole")
        assertTrue(plan.call!!["content"] is JsObj)
        done(plan)
        val view = root("long")
        assertEquals("a long log", view.text)
        assertEquals((1..91).map { "entry $it" }, view.lines.map { it.text })
    }

    @Test
    fun aFullLineTakesNoTenth() {
        done(pages.planNewBlock("box"))
        done(pages.planAdd("box", "", "shelf"))
        for (i in 1..8) done(pages.planAdd("box", "1", "item $i"))
        val at1 = pages.position(Region.POCKET, "box", "1")!!
        assertTrue(at1.full, "eight lines and the arrival stamp fill the nine digits")
        assertEquals(8, at1.lines.size)
        val ninth = pages.planAdd("box", "1", "item 9")
        assertEquals(Pages.FULL, ninth.refusal)
        assertNull(ninth.call)
    }

    @Test
    fun aPlanThatWouldNowLandElsewhereWritesNothing() {
        done(pages.planNewBlock("tide"))
        val first = pages.planAdd("tide", "", "high")
        val second = pages.planAdd("tide", "", "low")
        assertEquals("1", first.landsAt)
        assertEquals("1", second.landsAt)
        done(first)
        val stale = pages.carryOut(second)
        assertFalse(stale.ok)
        assertTrue(stale.problem!!.startsWith("The block changed while this was open"))
        assertEquals(listOf("high"), root("tide").lines.map { it.text })
    }

    @Test
    fun aWriteThatWouldDisturbOtherLinesIsRefused() {
        bsp.call("""{"block":"h","content":{"_":"h","1":{"_":{"_":"a hidden line","1":"x"}}}}""")
        val plan = pages.planChange("h", "1", "new")
        assertNotNull(plan.refusal)
        assertNull(plan.call)
        assertEquals("""{"1":{"_":{"1":"x","_":"a hidden line"}},"_":"h"}""", storage.read("h"))
    }

    @Test
    fun plainRefusals() {
        assertEquals("Give the block a name.", pages.planNewBlock("   ").refusal)
        assertTrue(pages.planNewBlock("sed:club").refusal!!.startsWith("Names starting sed: or grain:"))
        assertEquals("A name fits on one line.", pages.planNewBlock("two\nlines").refusal)
        done(pages.planNewBlock("garden"))
        assertEquals("There's already a block called garden.", pages.planNewBlock("garden").refusal)
        assertEquals("Write the line first.", pages.planAdd("garden", "", " ").refusal)
        assertEquals("The line already says that.", pages.planChange("garden", "", "garden at pocket.").refusal)
        assertEquals("There's no block called nowhere.", pages.planAdd("nowhere", "", "x").refusal)
        assertNull(pages.position(Region.POCKET, "garden", "7"))
    }

    @Test
    fun theTeachingShelfReadsLikeAnyBlock() {
        val teaching = pages.shore().teaching
        assertEquals(27, teaching.size, "28 names, one of them a second name for payway")
        assertEquals("sunstone", teaching[0].name)
        val whetstone = assertNotNull(pages.position(Region.TEACHING, "whetstone"))
        assertEquals(9, whetstone.lines.size)
        val two = assertNotNull(pages.position(Region.TEACHING, "whetstone", "2"))
        assertTrue(two.text!!.startsWith("Selection-shape derivation"), two.text)
        assertTrue(two.lines.isNotEmpty())
    }

    @Test
    fun everyTeachingBlockOpensAndEveryLineInItOpens() {
        var opened = 0
        for (b in pages.shore().teaching) {
            val view = assertNotNull(pages.position(Region.TEACHING, b.name), b.name)
            assertNotNull(view.text, "${b.name} has an opening line")
            for (line in view.lines) {
                val at = assertNotNull(pages.position(Region.TEACHING, b.name, line.address), "${b.name} ${line.address}")
                assertEquals(line.text, at.text, "${b.name} ${line.address}")
                for (deeper in at.lines) assertNotNull(pages.position(Region.TEACHING, b.name, deeper.address), "${b.name} ${deeper.address}")
                opened++
            }
        }
        assertTrue(opened > 100, "opened $opened lines")
    }

    @Test
    fun theShoreListsTheMostRecentlyChangedFirst() {
        done(pages.planNewBlock("a"))
        done(pages.planNewBlock("b"))
        done(pages.planAdd("a", "", "again"))
        assertEquals(listOf("a", "b"), pages.shore().mine.map { it.name })
    }
}
