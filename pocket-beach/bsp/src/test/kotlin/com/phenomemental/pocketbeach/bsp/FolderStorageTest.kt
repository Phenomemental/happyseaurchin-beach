package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sundial 0.11: his blocks in a folder he can see (spine:pocket-beach
 * 1.111178719942, FINDINGS-local-first.md 2a). One test for each kind of
 * file the folder may hold: his blocks, files he made on the laptop, files
 * that aren't blocks, files that aren't JSON, hidden files, a sync's second
 * versions and temporary files, and Sundial's own housekeeping.
 */
class FolderStorageTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val roots = mutableListOf<File>()

    private fun folder(): File = Files.createTempDirectory("sundial-folder").toFile().also { roots += it }

    @AfterTest
    fun cleanUp() = roots.forEach { it.deleteRecursively() }

    private fun block(opening: String, vararg lines: String): String =
        stringify(JsObj.of("_" to str(opening), *lines.mapIndexed { i, l -> "${i + 1}" to str(l) }.toTypedArray()))!!

    /** The files a person sees in the folder: everything but the hidden housekeeping. */
    private fun seen(dir: File): List<String> = (dir.listFiles() ?: emptyArray()).map { it.name }.filter { !it.startsWith(".") }.sorted()

    // ── his blocks ──

    @Test
    fun aBlockIsOneReadableFileDirectlyInTheFolder() {
        val dir = folder()
        val storage = FolderStorage(dir)
        storage.write("Garden", block("Garden at pocket.", "plant the bulbs"))
        storage.write("passport:Phenomemental", block("Phenomemental"))
        assertEquals(listOf("Garden.json", "passport%3APhenomemental.json"), seen(dir))
        assertEquals(block("Garden at pocket.", "plant the bulbs"), File(dir, "Garden.json").readText())
        assertEquals(listOf("Garden", "passport:Phenomemental"), storage.names().sorted())
        assertEquals(block("Garden at pocket.", "plant the bulbs"), storage.read("Garden"))
        assertNull(storage.read("garden"), "a name is matched exactly: garden isn't Garden")
    }

    @Test
    fun everyNameComesBackFromItsFileAndNoFileNameHoldsWhatFileSystemsRefuse() {
        val names = listOf(
            "garden", "Garden", "passport:Phenomemental", "a/b\\c", "what? *now*", "\"quoted\" <angle> |pipe|",
            "50%off", "%41", "100%", "%%41", "con", "CON.txt", "nul", "com1", "notes.", "trailing ", ".hidden", " leading",
            "ünïcode 🌊", "tab\there", "line\nbreak", "日記",
        )
        for (n in names) {
            val file = folderFileNameOf(n)
            assertEquals(n, folderBlockNameOf(file), "$n comes back from $file")
            val stem = file.removeSuffix(".json")
            assertTrue(stem.none { it in "/\\:*?\"<>|" || it.code < 0x20 }, "$file holds nothing a file system refuses")
            assertFalse(stem.startsWith(".") || stem.startsWith(" ") || stem.endsWith(".") || stem.endsWith(" "), "$file neither hides nor loses an end")
            assertFalse(Regex("^(con|prn|aux|nul|com[0-9]|lpt[0-9])$", RegexOption.IGNORE_CASE).matches(stem.substringBefore('.')), "$file isn't a device's name")
        }
        assertEquals("Garden.json", folderFileNameOf("Garden"))
        assertEquals("50%off.json", folderFileNameOf("50%off"), "a % that can't be misread stands as it is")
        assertEquals("ünïcode 🌊.json", folderFileNameOf("ünïcode 🌊"))
    }

    @Test
    fun everyBlockNameTheAppAllowsIsWrittenAndReadBack() {
        val dir = folder()
        val storage = FolderStorage(dir)
        // A file name beyond ASCII is written only where the JVM names files in UTF-8 (the phone, Windows, CI; not a bare POSIX shell).
        val jnu = java.nio.charset.Charset.forName(System.getProperty("sun.jnu.encoding") ?: "UTF-8").newEncoder()
        val names = listOf("garden", "Garden2", "passport:Phenomemental", "a/b", "what?", "50%off", "con", "notes.", "ünïcode 🌊")
            .filter { jnu.canEncode(folderFileNameOf(it)) }
        for (n in names) storage.write(n, block("$n at pocket."))
        assertEquals(names.sorted(), storage.names().sorted())
        for (n in names) assertEquals(block("$n at pocket."), storage.read(n))
    }

    @Test
    fun aFileHeMakesOnTheLaptopIsABlockOfThatName() {
        val dir = folder()
        File(dir, "My notes.json").writeText(block("My notes", "call the garage"))
        File(dir, "50%off.json").writeText(block("Sale"))
        File(dir, "%zz.json").writeText(block("Odd"))
        File(dir, "Plans.JSON").writeText(block("Plans"))
        val storage = FolderStorage(dir)
        assertEquals(listOf("%zz", "50%off", "My notes", "Plans"), storage.names().sorted())
        assertEquals(block("My notes", "call the garage"), storage.read("My notes"))
        // Written by Sundial, it stays the file he made: no second file appears beside it.
        storage.write("My notes", block("My notes", "call the garage", "and the bank"))
        storage.write("Plans", block("Plans", "one"))
        assertEquals(listOf("%zz.json", "50%off.json", "My notes.json", "Plans.JSON"), seen(dir))
        assertEquals(block("My notes", "call the garage", "and the bank"), File(dir, "My notes.json").readText())
        val bsp = PocketBsp(storage, shelf)
        val walked = bsp.call("""{"block":"My notes","spindle":"2"}""")
        assertTrue(walked.contains("and the bank"), walked)
    }

    // ── files that aren't blocks ──

    @Test
    fun aFileThatIsntJsonIsSetAsideListedAndLeftAsItIs() {
        val dir = folder()
        val storage = FolderStorage(dir)
        storage.write("garden", block("garden"))
        File(dir, "broken.json").writeText("{\"_\": \"half writ")
        assertEquals(listOf("garden"), storage.names())
        assertEquals(listOf(SetAside("broken.json", "broken", "isn't readable JSON")), storage.setAside())
        assertNull(storage.read("broken"))
        assertFailsWith<SetAsideFile> { storage.write("broken", block("broken")) }
        storage.delete("broken")
        assertEquals("{\"_\": \"half writ", File(dir, "broken.json").readText(), "never written over, never removed")
        // The pages a screen shows go on round it, and her listing too.
        val pages = Pages(storage, shelf, PocketBsp(storage, shelf))
        val shore = pages.shore()
        assertEquals(listOf("garden"), shore.mine.map { it.name })
        assertEquals(listOf("broken.json"), shore.setAside.map { it.file })
        assertTrue(Copies.read(Copies.make(storage, 0)).blocks.keys == setOf("garden"))
    }

    @Test
    fun jsonThatIsntABlockIsSetAsideWithWhy() {
        val dir = folder()
        File(dir, "package.json").writeText("""{"name": "sundial", "version": "1"}""")
        File(dir, "list.json").writeText("""["a", "b"]""")
        File(dir, "empty.json").writeText("")
        File(dir, "headless.json").writeText("""{"1": "a line with no opening above it"}""")
        File(dir, "null.json").writeText("null")
        val storage = FolderStorage(dir)
        assertEquals(emptyList(), storage.names())
        val why = storage.setAside().associate { it.file to it.why }
        assertEquals("is JSON, but not a block: it holds \"name\", where a block holds only _ and 1 to 9", why["package.json"])
        assertEquals("is JSON, but not a block: a block is an object", why["list.json"])
        assertEquals("is empty", why["empty.json"])
        assertEquals("is JSON, but not a block: it has no opening line", why["headless.json"])
        assertEquals("is JSON, but not a block: a block is an object", why["null.json"])
    }

    @Test
    fun aFileChangedOutsideIntoSomethingElseIsSetAsideFromThenOn() {
        val dir = folder()
        val storage = FolderStorage(dir)
        storage.write("garden", block("garden", "plant the bulbs"))
        assertEquals(listOf("garden"), storage.names())
        File(dir, "garden.json").writeText("garden: plant the bulbs")
        assertNull(storage.read("garden"))
        assertEquals(listOf("garden.json"), storage.setAside().map { it.file })
        File(dir, "garden.json").writeText(block("garden", "mended"))
        assertEquals(block("garden", "mended"), storage.read("garden"), "made a block again, it is read again")
    }

    @Test
    fun filesThatArentJsonAndHiddenFilesAreNeverListedNorTouched() {
        val dir = folder()
        val others = mapOf(
            "notes.md" to "# notes", "photo.jpg" to "jpeg", ".DS_Store" to "mac", "._garden.json" to "\u0000\u0005",
            ".syncthing.garden.json.tmp" to "partial", ".~lock.garden.json#" to "lock", "garden.json.tmp" to "partial",
        )
        for ((n, t) in others) File(dir, n).writeText(t)
        File(dir, ".stfolder").mkdirs()
        val storage = FolderStorage(dir)
        storage.write("garden", block("garden"))
        assertEquals(listOf("garden"), storage.names())
        assertEquals(emptyList(), storage.setAside())
        storage.replaceAll(mapOf("tide" to block("tide")), Stamps())
        for ((n, t) in others) assertEquals(t, File(dir, n).readText(), "$n stays as it was")
        assertTrue(File(dir, ".stfolder").isDirectory, "a sync's marker stays")
    }

    // ── a sync's second versions ──

    @Test
    fun aSyncsSecondVersionsAreNamedForWhatTheyAreAndNeverBlocks() {
        val dir = folder()
        File(dir, "garden.json").writeText(block("garden", "on the phone"))
        val seconds = listOf(
            "garden.sync-conflict-20261121-120000-ABCDEFG.json",
            "garden (conflicted copy 2026-11-21 120000).json",
            "garden (Matthew's conflicted copy 2026-11-21).json",
            "garden (SFConflict matthew 2026-11-21-12-00-00).json",
            "garden (Case Conflict).json",
            "garden (1).json",
        )
        for (s in seconds) File(dir, s).writeText(block("garden", "on the laptop"))
        File(dir, "plan (2).json").writeText(block("plan, the second"))
        val storage = FolderStorage(dir)
        assertEquals(listOf("garden", "plan (2)"), storage.names().sorted(), "a numbered name is a block of its own when nothing stands under the first")
        val aside = storage.setAside()
        assertEquals(seconds.sorted(), aside.map { it.file }.sorted())
        assertTrue(aside.all { it.why == "is a second version of garden, kept by your sync app" })
        assertNull(storage.read("garden (1)"))
        assertFailsWith<SetAsideFile> { storage.write("garden (1)", block("x")) }
        assertEquals("garden", secondVersionOf("garden.sync-conflict-20261121-120000-ABCDEFG") { false })
        assertNull(secondVersionOf("garden (1)") { false })
    }

    @Test
    fun aSecondFileForTheSameBlockIsSetAside() {
        val dir = folder()
        File(dir, "garden.json").writeText(block("garden"))
        File(dir, "%67arden.json").writeText(block("garden, again"))
        val storage = FolderStorage(dir)
        assertEquals(listOf("garden"), storage.names())
        assertEquals(block("garden"), storage.read("garden"), "its own name's file holds it")
        assertEquals(listOf(SetAside("%67arden.json", "garden", "is a second file for the block garden, which garden.json holds")), storage.setAside())
    }

    @Test
    fun aNameTakenInOtherCapitalsIsFoundForTheMove() {
        val dir = folder()
        File(dir, "Garden.json").writeText(block("Garden"))
        File(dir, "broken.json").writeText("{")
        val storage = FolderStorage(dir)
        assertEquals("Garden.json", storage.taken("garden"))
        assertEquals("broken.json", storage.taken("Broken"), "a file set aside takes its name too")
        assertNull(storage.taken("tide"))
    }

    // ── housekeeping ──

    @Test
    fun theStampsAndEveryTemporaryFileStayInTheHiddenFolder() {
        val dir = folder()
        val storage = FolderStorage(dir)
        val bsp = PocketBsp(storage, shelf) { 1_793_178_000_000 }
        bsp.call("""{"block":"garden","spindle":"1","content":"plant the bulbs"}""")
        for (i in 1..12) bsp.call("""{"block":"log","content":"line $i","append":true}""")
        assertEquals(listOf("garden.json", "log.json"), seen(dir))
        assertTrue(File(dir, ".sundial/stamps.json").isFile)
        assertEquals(emptyList(), File(dir, ".sundial/tmp").listFiles()?.toList().orEmpty())
        assertEquals(setOf("garden", "log"), storage.stamps().touched.keys)
        assertEquals(setOf("garden", "log"), FolderStorage(dir).stamps().born.keys, "the stamps are read back by a new start")
    }

    @Test
    fun theFingerprintMovesWhenAnythingInTheFolderDoes() {
        val dir = folder()
        val storage = FolderStorage(dir)
        storage.write("garden", block("garden"))
        val a = storage.fingerprint()
        assertEquals(a, storage.fingerprint())
        File(dir, "notes.json").writeText(block("notes"))
        val b = storage.fingerprint()
        assertNotEquals(a, b)
        File(dir, "garden.json").writeText(block("garden", "a longer line, from the laptop"))
        assertNotEquals(b, storage.fingerprint())
    }

    @Test
    fun aFolderThatIsntThereReadsAsNothingAndNeverStops() {
        val storage = FolderStorage(File(folder(), "not yet"))
        assertEquals(emptyList(), storage.names())
        assertEquals(emptyList(), storage.setAside())
        assertNull(storage.read("garden"))
        assertEquals(Stamps(), storage.stamps())
    }

    // ── a restore ──

    @Test
    fun aRestoreWritesFileByFileAndLeavesEveryOtherFile() {
        val dir = folder()
        val storage = FolderStorage(dir)
        storage.write("a", block("a, old"))
        storage.write("b", block("b"))
        File(dir, "broken.json").writeText("{")
        File(dir, "notes.md").writeText("mine")
        storage.setStamps(Stamps(mapOf("a" to "2026-11-01T00:00:00.000Z", "b" to "2026-11-01T00:00:00.000Z")))
        val stamps = Stamps(mapOf("a" to "2026-11-20T00:00:00.000Z"), mapOf("c" to "2026-11-20T00:00:00.000Z"))
        storage.replaceAll(mapOf("a" to block("a, restored"), "c" to block("c")), stamps)
        assertEquals(listOf("a", "c"), storage.names().sorted())
        assertEquals(block("a, restored"), storage.read("a"))
        assertEquals(stamps, storage.stamps())
        assertEquals("{", File(dir, "broken.json").readText())
        assertEquals("mine", File(dir, "notes.md").readText())
        assertFalse(File(dir, ".sundial/incoming").exists())
        assertFalse(File(dir, ".sundial/restore.json").exists())
    }

    @Test
    fun aRestoreOverAFileSetAsideIsRefusedWhole() {
        val dir = folder()
        val storage = FolderStorage(dir)
        storage.write("a", block("a"))
        File(dir, "broken.json").writeText("{")
        assertFailsWith<SetAsideFile> { storage.replaceAll(mapOf("a" to block("a, new"), "broken" to block("broken")), Stamps()) }
        assertEquals(block("a"), storage.read("a"), "nothing was restored")
        assertEquals("{", File(dir, "broken.json").readText())
    }

    @Test
    fun aRestoreStoppedPartWayIsFinishedOrAbandonedAtTheNextStart() {
        // The journal written: the restore had been decided, so the next start finishes it.
        val dir = folder()
        FolderStorage(dir).apply { write("old", block("old")); write("kept", block("kept")) }
        File(dir, ".sundial/incoming").mkdirs()
        File(dir, ".sundial/incoming/new.json").writeText(block("new"))
        File(dir, ".sundial/incoming-stamps.json").writeText("""{"touched":{"new":"2026-11-21T00:00:00.000Z"},"born":{}}""")
        File(dir, ".sundial/restore.json").writeText("""{"remove":["old.json"]}""")
        val finished = FolderStorage(dir)
        assertEquals(listOf("kept", "new"), finished.names().sorted())
        assertEquals(setOf("new"), finished.stamps().touched.keys)
        assertFalse(File(dir, ".sundial/restore.json").exists())

        // No journal: the restore had not been decided, so the next start leaves the blocks as they were.
        val dir2 = folder()
        FolderStorage(dir2).write("old", block("old"))
        File(dir2, ".sundial/incoming").mkdirs()
        File(dir2, ".sundial/incoming/new.json").writeText(block("new"))
        val abandoned = FolderStorage(dir2)
        assertEquals(listOf("old"), abandoned.names())
        assertFalse(File(dir2, ".sundial/incoming").exists())
    }

    @Test
    fun aCopyRestoredIntoTheFolderIsByteForByteWhatWasSaved() {
        val from = FolderStorage(folder())
        val bsp = PocketBsp(from, shelf) { 1_793_178_000_000 }
        bsp.call("""{"block":"garden","spindle":"1","content":"plant the bulbs"}""")
        bsp.call("""{"block":"passport:matthew","content":{"_":"a colon, and \"quotes\", and ünïcode 🌊"}}""")
        for (i in 1..12) bsp.call("""{"block":"log","content":"line $i","append":true}""")
        val copy = Copies.read(Copies.make(from, 1_793_178_000_000))
        val to = FolderStorage(folder())
        to.write("stale", block("stale"))
        Copies.restore(to, copy)
        assertEquals(from.names().sorted(), to.names().sorted())
        for (n in from.names()) assertEquals(from.read(n), to.read(n))
        assertEquals(from.stamps(), to.stamps())
    }
}
