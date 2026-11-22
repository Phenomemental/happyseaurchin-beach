package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sundial 0.11, the move (spine:pocket-beach 1.111178719933, 1.111178719942):
 * copy, check, switch; refused when a name clashes; his shell and history
 * kept private with the app's own blocks; the private pocket left whole.
 */
class MoveTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val roots = mutableListOf<File>()
    private val now = 1_793_178_000_000
    private val h = Hatch.MATTHEW_HANDLE
    private val passport = JsObj.of("_" to str("Matthew Bushell — digital social innovator, aspiring writer."), "1" to str("The heart."))

    private fun temp(): File = Files.createTempDirectory("sundial-move").toFile().also { roots += it }

    @AfterTest
    fun cleanUp() = roots.forEach { it.deleteRecursively() }

    private class Phone(val root: File, val pocket: FileStorage, val hidden: FileStorage) {
        /** The phone as 0.11 opens it: his shell and history private, the region open by what it holds. */
        fun routed(over: BlockStorage = pocket) = RoutedStorage(over, hidden, Private::routes, Private.open(over, hidden))
    }

    /** A phone hatched under 0.10.5: his blocks, the app's own blocks in the private region, his shell and history still in the pocket. */
    private fun hatchedUnder0105(): Phone {
        val root = temp()
        val pocket = FileStorage(root)
        val hidden = FileStorage(root, "app")
        val routed = RoutedStorage(pocket, hidden, Hidden::routes, openNow = false)
        val pages = Pages(routed, shelf, PocketBsp(routed, shelf) { now }) { now }
        check(pages.carryOut(pages.planNewBlock("garden")).ok)
        check(pages.carryOut(pages.planAdd("garden", "", "plant the bulbs")).ok)
        check(pages.carryOut(pages.planNewBlock(Asks.LOG)).ok)
        check(pages.carryOut(pages.planKeep(Talking.SAFETY_BLOCK, JsObj.of("_" to JsStr("Safety words, kept before Sundial 0.12."), "1" to JsObj.of("_" to JsStr("Not feeling safe"), "1" to JsStr("not safe"))))).ok)
        val planned = Hatch.plan(h, passport, Hatch.MATTHEW, pages, routed, now, "Hatched for the move's test.")
        check(planned.ok) { planned.refusal!! }
        check(Hatch.carryOut(planned, pages, routed).problem == null)
        check(pocket.read(Hatch.shellOf(h)) != null && pocket.read(Hatch.historyOf(h)) != null) { "0.10.5 kept his shell and history in the pocket" }
        return Phone(root, pocket, hidden)
    }

    private fun contents(dir: File): Map<String, String> =
        dir.walkTopDown().filter { it.isFile }.associate { it.relativeTo(dir).path to it.readText() }

    @Test
    fun theMoveCopiesChecksAndKeepsHisShellAndHistoryPrivate() {
        val phone = hatchedUnder0105()
        val before = phone.routed()
        val names = before.names().sorted()
        val texts = names.associateWith { before.read(it) }
        val pocketFiles = contents(File(phone.root, "regions/pocket"))
        val folder = FolderStorage(File(temp(), "Documents/Sundial"))

        val check = Move.check(phone.pocket, folder, Private::routes)
        assertTrue(check.ok, check.refusal)
        assertEquals(listOf("garden", Hatch.passportOf(h), Hatch.stashOf(h)), check.moving)
        assertEquals(listOf(Hatch.historyOf(h), Hatch.shellOf(h)), check.keptPrivate)

        val copy = File(phone.root, "copies-before-folder/copy.json")
        val moved = Move.carryOut(phone.pocket, phone.hidden, folder, Private::routes, before, copy, now)
        assertTrue(moved.ok, moved.problem)
        assertEquals(check.moving, folder.names().sorted())
        for (n in check.moving) assertEquals(phone.pocket.read(n), folder.read(n), "$n byte for byte")
        assertEquals(phone.pocket.stamps().born.filterKeys { it in check.moving }, folder.stamps().born)
        for (n in check.keptPrivate) assertEquals(phone.pocket.read(n), phone.hidden.read(n), "$n kept in the app's own files")
        assertEquals(pocketFiles, contents(File(phone.root, "regions/pocket")), "the private pocket is left whole")
        assertEquals(names.toSet(), Copies.read(copy.readText()).blocks.keys, "the whole copy holds every block")

        // The switch: every block reads as it did, his shell from the private region, and he is still hatched.
        val after = phone.routed(SwitchingStorage(folder))
        assertEquals(names, after.names().sorted())
        for (n in names) assertEquals(texts[n], after.read(n), n)
        assertEquals(h, Hatch.hatched(after))
        assertNull(folder.read(Hatch.shellOf(h)))
        assertNull(folder.read(Talking.SAFETY_BLOCK))

        // His history goes on growing in the private region, never in the folder.
        val pages = Pages(after, shelf, PocketBsp(after, shelf) { now }) { now }
        assertTrue(pages.carryOut(pages.planEntry(Hatch.historyOf(h), JsObj.of("_" to str("Talked with him.")))).ok)
        assertNull(folder.read(Hatch.historyOf(h)))
        assertTrue(phone.hidden.read(Hatch.historyOf(h))!!.contains("Talked with him."))
        assertFalse(File(folder.dir, "history%3A$h.json").exists())
    }

    @Test
    fun aNameTakenInTheFolderStopsTheMoveBeforeAnythingIsWritten() {
        val phone = hatchedUnder0105()
        val dir = File(temp(), "Sundial").apply { mkdirs() }
        File(dir, "Garden.json").writeText("""{"_":"Garden, on the laptop"}""")
        File(dir, "${folderFileNameOf(Hatch.passportOf(h))}").writeText("{")
        val folder = FolderStorage(dir)
        val check = Move.check(phone.pocket, folder, Private::routes)
        assertFalse(check.ok)
        assertEquals(listOf("Garden.json (for garden)", folderFileNameOf(Hatch.passportOf(h))), check.clashes)
        val copy = File(phone.root, "copy.json")
        val moved = Move.carryOut(phone.pocket, phone.hidden, folder, Private::routes, phone.routed(), copy, now)
        assertFalse(moved.ok)
        assertTrue(moved.problem!!.startsWith("Documents/Sundial already holds Garden.json (for garden) and"), moved.problem)
        assertEquals(setOf("Garden.json", folderFileNameOf(Hatch.passportOf(h))), dir.list()!!.toSet(), "nothing written into the folder")
        assertFalse(copy.exists(), "nothing saved either: the move never began")
    }

    @Test
    fun aBlockThatCantBeReadStopsTheMove() {
        val phone = hatchedUnder0105()
        File(phone.root, "regions/pocket/${fileNameOf("odd")}").writeText("""{"name":"odd"}""")
        val check = Move.check(phone.pocket, FolderStorage(File(temp(), "Sundial")), Private::routes)
        assertFalse(check.ok)
        assertEquals(listOf("odd is JSON, but not a block: it holds \"name\", where a block holds only _ and 1 to 9"), check.unreadable)
    }

    @Test
    fun aBlockGoneFromTheFolderNeverShutsHerSafetyAway() {
        val phone = hatchedUnder0105()
        val folder = FolderStorage(File(temp(), "Sundial"))
        assertTrue(Move.carryOut(phone.pocket, phone.hidden, folder, Private::routes, phone.routed(), File(phone.root, "c.json"), now).ok)
        File(folder.dir, folderFileNameOf(Hatch.passportOf(h))).delete()
        val after = phone.routed(folder)
        assertTrue(after.isOpen, "decided by the private region, not by the folder")
        assertNotNull(after.read(Talking.SAFETY_BLOCK))
        assertNotNull(after.read(Hatch.shellOf(h)))
    }

    @Test
    fun beforeAnyHatchThePrivateRegionStaysShut() {
        val root = temp()
        val pocket = FileStorage(root)
        val hidden = FileStorage(root, "app")
        pocket.write("garden", """{"_":"garden"}""")
        assertFalse(Private.open(pocket, hidden))
    }

    @Test
    fun aSwitchedPocketRefusesEveryWriteWhileBarred() {
        var why: String? = null
        val mem = MemoryStorage()
        val switching = SwitchingStorage(mem) { why }
        switching.write("a", """{"_":"a"}""")
        why = "Sundial can't reach Documents/Sundial."
        assertEquals("""{"_":"a"}""", switching.read("a"), "reading goes on")
        val refused = runCatching { switching.write("b", """{"_":"b"}""") }.exceptionOrNull()
        assertTrue(refused is Barred && refused.message == why)
        assertTrue(runCatching { switching.delete("a") }.isFailure)
        assertTrue(runCatching { switching.setStamps(Stamps()) }.isFailure)
        assertEquals(listOf("a"), mem.names(), "nothing was written or removed")
        val pages = Pages(switching, shelf, PocketBsp(switching, shelf) { now }) { now }
        assertFalse(pages.remove("a").ok, "a refused write is said, never thrown")
    }
}
