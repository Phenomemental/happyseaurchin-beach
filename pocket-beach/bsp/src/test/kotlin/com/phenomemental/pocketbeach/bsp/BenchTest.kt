package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The bench's asks, its scratch pocket, and how it judges an answer, with answers written by hand. */
class BenchTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val now = 1_793_178_000_000L
    private val timing = Bench.Timing(100, 2000, 2000, 1, 1300, 60)

    /** Today's companion, answering every ask with [json]. */
    private fun plan(json: (Bench.Ask) -> String) = { ask: Bench.Ask, pages: Pages, storage: MemoryStorage ->
        val asked = Asks.ask(ask.question, { json(ask) }, pages, storage, shelf, HelloWorld.SHELL_OPENING, "0".repeat(32)) { now }
        Bench.didOfPlan(asked, timing)
    }

    @Test
    fun twentyAsksOfThreeKinds() {
        assertEquals(23, Bench.ASKS.size)
        assertEquals((1..23).toList(), Bench.ASKS.map { it.n })
        assertEquals(mapOf(Bench.Route.TALK to 7, Bench.Route.READ to 6, Bench.Route.ACT to 8, Bench.Route.KEEP to 2), Bench.ASKS.groupingBy { it.route }.eachCount())
        assertTrue(Bench.ASKS.any { it.hello }, "a fresh hello world")
        assertTrue(Bench.ASKS.any { a -> a.writes.any { it.at.contains('.') } }, "a line added beneath a plain line")
    }

    @Test
    fun theScratchPocketIsSmallAndKnown() {
        val (storage, pages) = Bench.fixture(shelf, now)
        assertEquals(setOf(HelloWorld.SHELL, "garden", "notes", "Million"), storage.names().toSet())
        assertEquals("Million at pocket.", pages.position(Region.POCKET, "Million")!!.text)
        assertTrue(pages.position(Region.POCKET, "Million")!!.lines.isEmpty(), "as Matthew made it, empty")
        assertEquals(listOf("Autumn: plant the bulbs", "Winter: prune the roses"), pages.position(Region.POCKET, "garden")!!.lines.map { it.text })
        assertEquals("The garden, through the year", pages.position(Region.POCKET, "garden")!!.text)
    }

    @Test
    fun rightAnswersAreJudgedRight() {
        val answers = mapOf(
            1 to """{"say": "I'm the pocket companion.", "steps": []}""",
            5 to """{"say": "A block is a page of lines, each line in its own place.", "steps": []}""",
            6 to """{"say": "You're welcome. Bye for now!", "steps": []}""",
            21 to """{"say": "Keeping it.", "steps": [{"do": "add", "block": "Million", "at": "", "text": "I'm going to make a million pound by creating tech that changes the world, before the end of 2027."}]}""",
            22 to """{"say": "A new block.", "steps": [{"do": "make", "block": "sailing", "at": "", "text": "My plan is to learn to sail next summer."}]}""",
            23 to """{"say": "Good night!", "steps": []}""",
            7 to """{"say": "garden holds: Autumn: plant the bulbs, and Winter: prune the roses.", "steps": []}""",
            // As 0.4 answered on Matthew's Pixel (the bench of 0.4.1), first judged wrong for want of "prune".
            12 to """{"say": "Yes, the garden block has a line about winter pruning roses.", "steps": []}""",
            13 to """{"say": "Adding it.", "steps": [{"do": "add", "block": "garden", "at": "", "text": "buy more compost"}]}""",
            14 to """{"say": "Under autumn.", "steps": [{"do": "add", "block": "garden", "at": "1", "text": "tulips came from Holland"}]}""",
            17 to """{"say": "Making it.", "steps": [{"do": "make", "block": "recipes", "at": "", "text": ""}]}""",
            19 to """{"say": "Two.", "steps": [{"do": "add", "block": "garden", "at": "", "text": "water the roses"}, {"do": "add", "block": "garden", "at": "", "text": "cut the hedge"}]}""",
            20 to "{\"say\": \"Here.\", " + HelloWorld.SHELL_RECIPE.substringAfter("changing nothing else: {").replace("HIS SECRET", Bench.SECRET),
        )
        for ((n, json) in answers) {
            val r = Bench.one(Bench.ASKS[n - 1], shelf, now, answer = plan { json })
            assertTrue(r.right, "ask $n: ${r.problems}")
        }
    }

    @Test
    fun wrongAnswersSayWhy() {
        fun problems(n: Int, json: String) = Bench.one(Bench.ASKS[n - 1], shelf, now, answer = plan { json }).problems.joinToString(" ")
        assertContains(problems(1, """{"say": "", "steps": []}"""), "It said nothing.")
        assertContains(problems(1, """{"say": "Hello.", "steps": []}"""), "didn't say")
        assertContains(problems(4, """{"say": "Sure.", "steps": [{"do": "add", "block": "garden", "at": "", "text": "spring plan"}]}"""), "for an ask that needs none")
        // The repeated line under "autumn", as the model wrote it on the laptop: beneath it, but the wrong line.
        assertContains(problems(14, """{"say": "x", "steps": [{"do": "add", "block": "garden", "at": "1", "text": "Autumn: plant the bulbs"}]}"""), "didn't plan a line \"tulips came from Holland\" in garden at 1.1")
        assertContains(problems(13, """{"say": "x", "steps": [{"do": "add", "block": "garden", "at": "1", "text": "buy more compost"}]}"""), "at 3")
        assertContains(problems(20, """{"say": "No.", "steps": []}"""), "planned nothing")
        // As 0.3 answered on Matthew's Pixel, and the bench first judged right.
        assertContains(problems(5, """{"say": "I'm sorry, Matthew, I don't see any block named 'block' in the pocket or on the teaching shelf.", "steps": []}"""), "looking for a block nobody asked about")
        assertContains(problems(6, """{"say": "I apologize, Matthew, but I don't see any block named 'hello-world' in the pocket or on the teaching shelf.", "steps": []}"""), "looking for a block nobody asked about")
        assertContains(problems(6, """{"say": "Thanks.", "steps": [{"do": "read", "block": "pocket-companion", "at": "", "text": ""}]}"""), "needs no reading")
        assertContains(problems(1, """{"say": "I'm the pocket companion.", "steps": [{"do": "read", "block": "pocket-companion", "at": "", "text": ""}]}"""), "needs no reading")
        assertContains(problems(20, "{\"say\": \"Here.\", " + HelloWorld.SHELL_RECIPE.substringAfter("changing nothing else: {").replace("HIS SECRET", "42")), "word for word")
    }

    @Test
    fun theRunIsOneBlockWithNineLinesOrFewerAtEachLevel() {
        val results = Bench.ASKS.map { ask -> Bench.one(ask, shelf, now, answer = plan { """{"say": "I'm the pocket companion.", "steps": []}""" }) }
        val block = Bench.block(listOf(Bench.Run("0.3", results), Bench.Run("0.4", results)), listOf("The phone: a laptop", "The engine: none"), now)
        val opening = collectUnderscore(block)!!
        assertContains(opening, "0.3: 1 of 23 right.")
        assertContains(opening, "0.4: 1 of 23 right.")
        assertEquals("How it was run", collectUnderscore(block["1"]))
        val before = block["2"] as JsObj
        assertContains(collectUnderscore(before)!!, "0.3: 1 of 23 right.")
        assertEquals("Talk: 1 of 7 right.", collectUnderscore(before["1"]))
        assertEquals("Keep: 0 of 2 right.", collectUnderscore(before["4"]))
        assertTrue((before["3"] as JsObj).keys().all { it == "_" || it.toInt() in 1..9 })
        assertContains(collectUnderscore(block["3"])!!, "0.4: 1 of 23 right.")
        assertTrue(Bench.line(results[0]).startsWith("✓ 1. What's your name? (first token 0.1 s, shown 2.0 s, total 2.0 s, 1 exchange, read 1300, wrote 60 tokens)."))
        assertTrue(Bench.line(results[12]).startsWith("✗ 13."))
        assertContains(Bench.line(results[12]), "Taken as talk.")
        // It is kept as any block from elsewhere is: whole, as one write.
        val (_, pages) = Bench.fixture(shelf, now)
        val plan = pages.planKeep("bench-1", block)
        assertEquals(null, plan.refusal)
        assertTrue(pages.carryOut(plan).ok)
        assertFalse(pages.position(Region.POCKET, "bench-1")!!.lines.isEmpty())
    }
}
