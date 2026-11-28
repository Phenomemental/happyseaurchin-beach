package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sundial 0.12, the plain chat (spine:pocket-beach 1.111178719993): files
 * read into the conversation as text, a block in pscale structure as text,
 * the text files of his folder, his conversations kept and shared out.
 */
class ChatsTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val nowMs = 1_795_000_000_000L

    @Test
    fun filesAreReadInBeneathHisWords() {
        val turn = Chats.withFiles("What do you make of these?", listOf(
            Chats.Attached("garden", "The garden, through the year.\n  1  Autumn: plant the bulbs", block = true),
            Chats.Attached("notes.txt", "Bring the seeds.", block = false),
        ))
        assertEquals(
            "What do you make of these?\n\n(His block garden: its opening line, then each part beneath it:)\nThe garden, through the year.\n  1  Autumn: plant the bulbs\n\n(His file notes.txt:)\nBring the seeds.",
            turn,
        )
        assertEquals("Just my words.", Chats.withFiles("Just my words.", emptyList()))
        val long = "x".repeat(Chats.MOST_TEXT + 500)
        assertTrue(Chats.clip(long).endsWith("… (cut here: the file runs on)"))
        assertTrue(Chats.clip(long).length < long.length)
    }

    @Test
    fun aBlockReadsAsTextInItsStructure() {
        val (_, pages) = Bench.fixture(shelf, nowMs)
        check(pages.carryOut(pages.planKeep("orchard", JsObj.of(
            "_" to str("The garden, through the year."),
            "1" to JsObj.of("_" to str("Autumn"), "1" to str("plant the bulbs"), "2" to str("rake the leaves")),
            "2" to str("Winter: prune the roses"),
        ))).ok)
        val outline = pages.outline(Region.POCKET, "orchard")!!
        assertEquals(listOf("" to 0, "1" to 1, "1.1" to 2, "1.2" to 2, "2" to 1), outline.map { it.address to it.depth })
        assertEquals("The garden, through the year.\n  1  Autumn\n    1.1  plant the bulbs\n    1.2  rake the leaves\n  2  Winter: prune the roses", pages.outlineText(Region.POCKET, "orchard"))
        assertNull(pages.outline(Region.POCKET, "nowhere"))
    }

    @Test
    fun aWrappedBlockReadsWhole() {
        val (_, pages) = Bench.fixture(shelf, nowMs)
        check(pages.carryOut(pages.planNewBlock("log")).ok)
        for (i in 1..10) check(pages.carryOut(pages.planAdd("log", "", "entry $i")).ok) { "entry $i" }
        val outline = pages.outline(Region.POCKET, "log")!!
        val texts = outline.mapNotNull { it.text }
        for (i in 1..10) assertTrue(texts.any { it == "entry $i" }, "entry $i in:\n${outlineAsText(outline)}")
    }

    /** Revised 22 November: a block too big for her window is given as its root and each part's opening line, the disc at pscale 0, and marked so. */
    @Test
    fun aBlockTooBigForHerWindowIsGivenAsItsOpeningLines() {
        val (_, pages) = Bench.fixture(shelf, nowMs)
        val long = "words ".repeat(60).trim()
        val big = JsObj.of("_" to str("The book, chapter by chapter."))
        for (i in 1..9) big["$i"] = JsObj.of("_" to str("Chapter $i"), *(1..9).map { j -> "$j" to str("Scene $i.$j: $long") }.toTypedArray())
        check(pages.carryOut(pages.planKeep("book", big)).ok)
        val read = Chats.blockFile(pages, Region.POCKET, "book")!!
        assertTrue(read.block)
        assertTrue(!read.whole, "too big to read whole")
        assertEquals("The book, chapter by chapter.\n" + (1..9).joinToString("\n") { "  $it  Chapter $it  (9 beneath)" }, read.text)
        assertTrue(!read.text.contains("Scene"))
        val turn = Chats.withFiles("What do you make of it?", listOf(read))
        assertTrue(turn.contains("(His block book, too big to read whole here, as bsp() reads it: the start of its opening line and of each part's, and how many lines stand beneath each part:)"), turn)
        // A small block is read whole.
        check(pages.carryOut(pages.planKeep("orchard", JsObj.of("_" to str("The orchard."), "1" to str("Prune the apples")))).ok)
        assertTrue(Chats.blockFile(pages, Region.POCKET, "orchard")!!.whole)
        assertNull(Chats.blockFile(pages, Region.POCKET, "nowhere"))
        // Kept and read back, the mark kept with it.
        val kept = Chats.Kept(nowMs, listOf(Chats.Said("Look", listOf(read), "", nowMs)))
        assertEquals(kept, Chats.fromJson(Chats.toJson(kept)))
    }

    /** A block chosen from elsewhere, a JSON file, is never handed her as JSON: readable text, as his own blocks are. */
    /**
     * Sundial 0.13.6: a block's top lines as bsp() reads them. In his blocks an
     * opening line is often a paragraph, and the app gave each whole: his
     * passport's top came to about 1,500 tokens against bsp's 400.
     */
    @Test
    fun topLinesAreCutWhereBspCutsThem() {
        val (_, pages) = Bench.fixture(shelf, nowMs)
        val long = "IF YOU ARE AN AI WORKING WITH ME — " + "you're here to help me do with my information what must be done. ".repeat(8)
        val block = JsObj.of(
            "_" to str("Matthew — " + "a long opening line about who he is. ".repeat(12)),
            "1" to str("Short."),
            "2" to JsObj.of("_" to str(long), "1" to str("Beneath it, the detail.")),
        )
        check(pages.carryOut(pages.planKeep("passport:Phenomemental", block)).ok)
        val top = Chats.discText(pages, Region.POCKET, "passport:Phenomemental")!!.lines()
        val bsp = Reach.walk(shelf, "passport:Phenomemental", block, null, null).lines()
        val ours = top[2].substringAfter("2  ").substringBefore("  (1 beneath)")
        val theirs = bsp.first { "[2]" in it }.substringAfter("]: ").substringBefore(" · ")
        assertEquals(theirs, ours, "the same cut as bsp()'s own disc")
        assertTrue(ours.endsWith("...") && ours.length == 153, ours)
        assertTrue(top[0].endsWith("...") && top[0].length == 153, top[0])
        assertEquals("  1  Short.", top[1])
        assertTrue(top.none { "Beneath it, the detail." in it }, "the detail stays beneath, for a walk")
        assertTrue(top.joinToString("\n").length < 400, "a few hundred characters, however long his lines: $top")
    }

    @Test
    fun aBlockChosenFromElsewhereIsReadableTextNeverJson() {
        val json = stringify(JsObj.of("_" to str("Letters I mean to write."), "1" to JsObj.of("_" to str("To Jo"), "1" to str("say thank you first"))))!!
        val read = Chats.picked("letters.json", json, shelf)
        assertTrue(read.block && read.whole)
        assertEquals("letters", read.name)
        assertEquals("Letters I mean to write.\n  1  To Jo\n    1.1  say thank you first", read.text)
        assertTrue(!read.text.contains("{") && !read.text.contains("\"_\""))
        // Anything else is its text.
        assertEquals(Chats.Attached("notes.json", """{"type":"list"}""", block = false), Chats.picked("notes.json", """{"type":"list"}""", shelf))
        assertEquals(false, Chats.picked("notes.txt", "Bring the seeds.", shelf).block)
    }

    @Test
    fun theFolderOffersItsTextFilesOnly() {
        val dir = Files.createTempDirectory("sundial-chat").toFile()
        File(dir, "notes.txt").writeText("a")
        File(dir, "plan.md").writeText("b")
        File(dir, "garden.json").writeText("{}")
        File(dir, ".hidden.txt").writeText("c")
        File(dir, ".sundial").mkdirs()
        File(dir, "sub").mkdirs()
        assertEquals(setOf("notes.txt", "plan.md"), Chats.textFiles(dir).map { it.name }.toSet())
        assertEquals(emptyList(), Chats.textFiles(File(dir, "missing")))
    }

    @Test
    fun conversationsAreKeptListedAndSharedAsText() {
        val dir = Files.createTempDirectory("sundial-talks").toFile()
        val first = Chats.Kept(nowMs, listOf(
            Chats.Said("Hello, here's my garden.", listOf(Chats.Attached("garden", "The garden.", block = true)), "A garden through the year. What's first?", nowMs),
            Chats.Said("The bulbs.", emptyList(), "", nowMs + 60_000),
        ))
        Chats.keep(dir, first)
        Chats.keep(dir, Chats.Kept(nowMs + 3_600_000, listOf(Chats.Said("Later.", emptyList(), "Yes?", nowMs + 3_600_000))))
        File(dir, "talk-broken.json").writeText("not json")
        val all = Chats.all(dir)
        assertEquals(listOf(nowMs + 3_600_000, nowMs), all.map { it.startedMs }, "newest first; the unreadable passed over")
        val again = all.last()
        assertEquals(first, again)
        assertEquals("Hello, here's my garden.", again.opening)
        assertEquals(
            "Matthew: Hello, here's my garden.\n(with garden)\n\nGemma: A garden through the year. What's first?\n\nMatthew: The bulbs.",
            Chats.asText(again),
        )
        // Kept again after another reply: the same file, written whole.
        Chats.keep(dir, Chats.Kept(nowMs, first.exchanges + Chats.Said("And then?", emptyList(), "The roses.", nowMs + 120_000)))
        assertEquals(3, Chats.all(dir).first { it.startedMs == nowMs }.exchanges.size)
        assertNotNull(Chats.fromJson(Chats.toJson(first)))
    }
}
