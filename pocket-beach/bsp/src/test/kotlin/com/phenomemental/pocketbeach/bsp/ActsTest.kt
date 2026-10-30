package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The action language: read, quotes found, and planned as the same guarded writes, on the bench's small pocket. */
class ActsTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val now = 1_793_178_000_000L
    private val seal = "0123456789abcdef0123456789abcdef"
    private val pocket = Bench.fixture(shelf, now)
    private val storage = pocket.first
    private val pages = pocket.second

    private fun plan(text: String, chosen: Map<Int, String> = emptyMap()) =
        Acts.propose("", Acts.parse(text), storage, shelf, HelloWorld.SHELL_OPENING, seal, now, chosen)

    @Test
    fun everyActionIsReadFromItsLine() {
        val a = Acts.parse(
            """
            ```
            new recipes
            new books: Books to read
            - add garden: buy more compost
            2. add garden under "Autumn: plant the bulbs": tulips came from Holland
            add garden beneath “Winter”: mulch the beds
            change garden: My garden
            change garden "Winter: prune the roses": Winter: prune the roses and the apple tree
            read garden
            read pocket/garden "autumn"
            post: Hello world
            ```
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                Acts.Action.New("recipes", null),
                Acts.Action.New("books", "Books to read"),
                Acts.Action.Add("garden", null, "buy more compost"),
                Acts.Action.Add("garden", "Autumn: plant the bulbs", "tulips came from Holland"),
                Acts.Action.Add("garden", "Winter", "mulch the beds"),
                Acts.Action.Change("garden", null, "My garden"),
                Acts.Action.Change("garden", "Winter: prune the roses", "Winter: prune the roses and the apple tree"),
                Acts.Action.Read("garden", null),
                Acts.Action.Read("garden", "autumn"),
                Acts.Action.Post("Hello world"),
            ),
            a,
        )
        assertContains(assertFailsWith<Acts.Unreadable> { Acts.parse("add garden\nplease") }.message!!, "isn't one of the actions")
        assertFailsWith<Acts.Unreadable> { Acts.parse("  \n ") }
    }

    @Test
    fun aQuoteFindsItsLineOrAsksWhich() {
        assertEquals("1", assertIs<Acts.Found.One>(Acts.find(pages, "garden", "Autumn: plant the bulbs")).line.address)
        assertEquals("1", assertIs<Acts.Found.One>(Acts.find(pages, "garden", "autumn: plant the bulbs.")).line.address, "case and the stop at its end")
        assertEquals("2", assertIs<Acts.Found.One>(Acts.find(pages, "garden", "prune")).line.address, "a line holding the quote")
        val both = assertIs<Acts.Found.Many>(Acts.find(pages, "garden", "the"))
        assertEquals(listOf("1", "2"), both.lines.map { it.address })
        assertContains(assertIs<Acts.Found.None>(Acts.find(pages, "garden", "spring")).why, "No line in garden says “spring”.")
        assertContains(assertIs<Acts.Found.None>(Acts.find(pages, "nowhere", "x")).why, "There's no block called nowhere.")
    }

    @Test
    fun aLineGoesBeneathAPlainLineTheAppOpensUp() {
        val p = assertNotNull(plan("add garden under \"Autumn: plant the bulbs\": tulips came from Holland").proposal)
        assertNull(p.refusal, p.refusal)
        assertEquals(listOf("1.1"), p.writes.map { it.plan.landsAt })
        assertEquals("tulips came from Holland", p.writes.single().text)
        val carried = Asks.carryOut(p, pages, storage, HelloWorld.SHELL_OPENING)
        assertNull(carried.problem)
        assertEquals("Autumn: plant the bulbs", pages.position(Region.POCKET, "garden", "1")!!.text, "the line it went beneath is as it was")
        assertEquals("tulips came from Holland", pages.position(Region.POCKET, "garden", "1.1")!!.text)
    }

    @Test
    fun aQuoteThatFindsTwoLinesIsPutToMatthew() {
        val first = plan("add garden under \"the\": a note")
        assertNull(first.proposal)
        val choice = assertNotNull(first.choose)
        assertEquals(0, choice.action)
        assertEquals(listOf("Autumn: plant the bulbs", "Winter: prune the roses"), choice.lines.map { it.text })
        val second = plan("add garden under \"the\": a note", chosen = mapOf(0 to "2"))
        assertEquals(listOf("2.1"), second.proposal!!.writes.map { it.plan.landsAt })
        assertContains(plan("change garden \"spring\": x").problem!!, "Action 1: No line in garden says “spring”.")
    }

    @Test
    fun theBenchsActsComeOutRight() {
        fun writes(text: String) = plan(text).proposal!!.writes.map { "${it.plan.block} ${it.plan.kind} ${it.plan.landsAt}: ${it.text}" }
        assertEquals(listOf("garden ADD 3: buy more compost"), writes("add garden: buy more compost"))
        assertEquals(listOf("garden CHANGE 2: Winter: prune the roses and the apple tree"), writes("change garden \"Winter\": Winter: prune the roses and the apple tree"))
        // As she wrote it on the laptop: the colon left out, the new line quoted.
        assertEquals(listOf("garden CHANGE 2: Winter: prune the roses and the apple tree"), writes("change garden \"Winter: prune the roses\" \"Winter: prune the roses and the apple tree\""))
        assertEquals(listOf("garden CHANGE 2: Winter: prune the roses and the apple tree"), writes("change garden \"Winter: prune the roses\"\nchange garden \"Winter: prune the roses and the apple tree\""), "a change split over two lines")
        assertEquals(listOf("garden CHANGE 2: Winter: mulch"), writes("change garden \"Winter\" to \"Winter: mulch\""))
        // A line restated before the action that quotes it is let be.
        assertEquals(listOf("garden ADD 2.1: mulch the beds"), writes("add garden: Winter: prune the roses\nadd garden under \"Winter: prune the roses\": mulch the beds"))
        // 0.6.2, one home per fact: asked for alone, a line the block already holds isn't proposed again.
        assertTrue(plan("add garden: Winter: prune the roses").nothing, "a line already in its block")
        assertContains(plan("add garden: winter: prune the roses.").dropped.single(), "garden already says")
        // A block named in other capitals is that block: speech writes "million", and the block is Million.
        assertEquals(listOf("Million ADD 1: a million pound by 2027"), writes("add million: a million pound by 2027"))
        assertContains(plan("new million").problem!!, "There's already a block called Million.")
        // As she answered on the laptop: a new block, with his words, of the name he already has. His words go in it.
        assertEquals(listOf("Million ADD 1: I'm going to make a million pound"), writes("new million: I'm going to make a million pound"))
        // Under the block's opening is in the block itself.
        assertEquals(listOf("notes ADD 2: call the plumber"), writes("add notes under \"Notes\": call the plumber"))
        assertEquals(listOf("recipes NEW_BLOCK : null"), writes("new recipes"))
        assertEquals(listOf("books NEW_BLOCK : null", "books CHANGE : Books to read"), writes("new books: Books to read"))
        assertEquals(listOf("garden ADD 3: water the roses", "garden ADD 4: cut the hedge"), writes("add garden: water the roses\nadd garden: cut the hedge"))
        val reading = plan("read garden \"autumn\"")
        assertEquals(listOf("garden 1"), reading.reads.map { "${it.block} ${it.at}" })
        assertTrue(reading.proposal!!.writes.isEmpty())
    }

    @Test
    fun theHelloWorldInActionsGetsTheAppsSealFingerprintAndPost() {
        assertContains(plan("new hello-world\nadd hello-world: Secret: HIS SECRET").problem!!, "leaves HIS SECRET")
        val filled = Acts.propose("", Acts.parse("new hello-world\nadd hello-world: Secret: HIS SECRET"), storage, shelf, HelloWorld.SHELL_OPENING, seal, now, secret = "purple walrus")
        assertEquals("Secret: purple walrus", filled.proposal!!.writes.single { it.text?.startsWith("Secret: ") == true }.text, "the secret he gave, written in by the app")
        val p = assertNotNull(
            plan("new hello-world\nadd hello-world: {{shell}}\nadd hello-world: Secret: ${Bench.SECRET}\npost: Hello world").proposal,
        )
        assertNull(p.refusal, p.refusal)
        assertEquals(listOf(false, false, false, true, true), p.writes.map { it.step.byApp })
        // As the shell now says it: the block, the secret and the post; the app writes the shell's line first.
        val short = assertNotNull(plan("new hello-world\nadd hello-world: Secret: ${Bench.SECRET}\npost: Hello world").proposal)
        assertNull(short.refusal, short.refusal)
        assertEquals(listOf(false, true, false, true, true), short.writes.map { it.step.byApp })
        assertEquals(p.writes.map { it.text }, short.writes.map { it.text })
        // The shell's opening written out where {{shell}} goes is the line the app fills.
        val out = plan("new hello-world\nadd hello-world: pocket-companion: \"${HelloWorld.SHELL_OPENING}\"\nadd hello-world: Secret: ${Bench.SECRET}\npost: Hello world").proposal!!
        assertEquals(p.writes.map { it.text }, out.writes.map { it.text })
        val carried = Asks.carryOut(p, pages, storage, HelloWorld.SHELL_OPENING)
        val stored = storage.read(HelloWorld.BLOCK)
        assertEquals(emptyList(), HelloWorld.check(stored, HelloWorld.SHELL_OPENING, seal))
        assertEquals(HelloWorld.messageFor(stored), carried.post!!.message)
    }
}
