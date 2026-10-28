package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** "Save a copy" and "Restore": one file holding every block, brought back whole or not at all. */
class CopiesTest {
    private val shelfDir = File(System.getProperty("pocket.shelf") ?: "../shelf")
    private val shelf = Shelf(DirShelfSource(shelfDir))
    private val roots = mutableListOf<File>()

    private fun folder() = Files.createTempDirectory("pocket-copies").toFile().also { roots += it }

    @AfterTest
    fun cleanUp() = roots.forEach { it.deleteRecursively() }

    private val day = 24L * 60 * 60 * 1000
    private val t0 = 1_793_178_000_000

    /** A pocket with blocks of every kind: made by bsp(), wrapped, and every teaching block as a block of its own. */
    private fun filled(root: File): FileStorage {
        val storage = FileStorage(root)
        val bsp = PocketBsp(storage, shelf) { t0 }
        bsp.call("""{"block":"garden","spindle":"1","content":"plant the bulbs"}""")
        bsp.call("""{"block":"shell:matthew","content":{"_":"a colon, and \"quotes\", and ünïcode 🌊"}}""")
        for (i in 1..12) bsp.call("""{"block":"log","content":"line $i","append":true}""")
        for (name in shelf.shelved()) storage.write("copy of $name", stringify(shelf.load(name)!!)!!)
        return storage
    }

    @Test
    fun aRestoredCopyIsByteForByteWhatWasSaved() {
        val from = filled(folder())
        val text = Copies.make(from, t0 + day)
        val copy = Copies.read(text)
        assertEquals(isoString(t0 + day), copy.saved)
        assertEquals(from.names().sorted(), copy.blocks.keys.sorted())

        val to = FileStorage(folder())
        PocketBsp(to, shelf) { t0 }.call("""{"block":"stray","spindle":"1","content":"made after the copy"}""")
        Copies.restore(to, copy)
        assertEquals(from.names().sorted(), to.names().sorted(), "a block not in the copy is gone")
        for (name in from.names()) assertEquals(from.read(name), to.read(name), name)
        assertEquals(from.stamps(), to.stamps())
        assertEquals(text, Copies.make(to, t0 + day), "saving the restored pocket gives the same file")
    }

    @Test
    fun whatIsNotACopyIsRefusedInPlainWords() {
        fun refusal(text: String) = assertFailsWith<Copies.Unreadable> { Copies.read(text) }.message!!
        val good = Copies.make(filled(folder()), t0)
        assertTrue(refusal("not json").startsWith("This file isn't a copy saved by Pocket beach"))
        assertEquals("This file isn't a copy saved by Pocket beach.", refusal("""{"blocks":{}}"""))
        assertTrue(refusal(good.replace("\"version\": 1", "\"version\": 2")).startsWith("This copy was saved by a newer Pocket beach"))
        fun withBlocks(blocks: String) = """{"format":"pocket-beach copy","version":1,"saved":"2026-10-28T09:00:00.000Z","blocks":$blocks}"""
        assertTrue(refusal(withBlocks("""{"sed:club":{"_":"x"}}""")).contains("belongs to the beach"))
        assertEquals("This copy is damaged: \"a\" isn't a block.", refusal(withBlocks("""{"a":{"1":"no opening line"}}""")))
        assertTrue(refusal(withBlocks("""{"a":{"_":"x","10":"y"}}""")).startsWith("This copy is damaged: \"a\" has an invalid key \"10\""))
        assertEquals(0, Copies.read(withBlocks("{}")).blocks.size)
    }

    @Test
    fun aRestoreStoppedPartWayLeavesOneWholeOrTheOther() {
        val root = folder()
        val storage = filled(root)
        val before = storage.names().associateWith { storage.read(it) }
        val regions = File(root, "regions")

        // Stopped while the new blocks were still being written: the old ones stand.
        File(regions, "pocket.incoming").mkdirs()
        File(regions, "pocket.incoming/half.json").writeText("""{"_":"half"}""")
        File(regions, "pocket.meta.incoming.json").writeText("""{"touched":{},"born":{}}""")
        val again = FileStorage(root)
        assertEquals(before.keys, again.names().toSet())
        assertFalse(File(regions, "pocket.incoming").exists())
        assertFalse(File(regions, "pocket.meta.incoming.json").exists())

        // Stopped after the old folder was moved aside: the new blocks go in.
        File(regions, "pocket").renameTo(File(regions, "pocket.outgoing"))
        File(regions, "pocket.incoming").mkdirs()
        File(regions, "pocket.incoming/${fileNameOf("new")}").writeText("""{"_":"new"}""")
        File(regions, "pocket.meta.incoming.json").writeText("""{"touched":{"new":"2026-10-28T09:00:00.000Z"},"born":{}}""")
        val after = FileStorage(root)
        assertEquals(listOf("new"), after.names())
        assertEquals(mapOf("new" to "2026-10-28T09:00:00.000Z"), after.stamps().touched)
        assertFalse(File(regions, "pocket.outgoing").exists())
    }

    @Test
    fun theReminderComesThreeDaysAfterTheLastCopyWhenSomethingChanged() {
        fun stamps(touched: Long?, born: Long?) = Stamps(
            touched?.let { mapOf("garden" to isoString(it)) } ?: emptyMap(),
            born?.let { mapOf("garden" to isoString(it)) } ?: emptyMap(),
        )
        assertFalse(Copies.reminderDue(null, Stamps(), t0), "no blocks, nothing to save")
        assertFalse(Copies.reminderDue(null, stamps(t0, t0), t0 + 2 * day))
        assertTrue(Copies.reminderDue(null, stamps(t0, t0), t0 + 3 * day), "never saved, the first block three days old")
        assertFalse(Copies.reminderDue(t0, stamps(t0 - day, t0 - day), t0 + 10 * day), "nothing changed since the copy")
        assertFalse(Copies.reminderDue(t0, stamps(t0 + day, t0 - day), t0 + 2 * day))
        assertTrue(Copies.reminderDue(t0, stamps(t0 + day, t0 - day), t0 + 3 * day))
        assertTrue(Copies.changedSince(t0, stamps(t0 + 1, t0)))
        assertFalse(Copies.changedSince(t0, stamps(t0, t0)))
    }
}
