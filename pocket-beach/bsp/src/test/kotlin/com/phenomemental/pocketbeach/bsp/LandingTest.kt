package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 0.6: a told day landed for one Yes, his corrections kept as the examples she is shown next time. */
class LandingTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val nowMs = Instant.parse("2026-10-29T14:00:00Z").toEpochMilli()

    private fun pocket(): Pair<MemoryStorage, Pages> {
        val storage = MemoryStorage()
        return storage to Pages(storage, shelf, PocketBsp(storage, shelf) { nowMs }) { nowMs }
    }

    /** The key's own answer to his written telling, as the sort would give it. */
    private val right = Telling.placed(
        """
        1.1: the car wouldn't start again this morning
        1.1: The meeting went on forever but Sam said something that stuck with me
        1.2: Felt rotten about the morning, that hot-faced thing
        1.3: that I'm always the one apologising
        1.4: The thing I keep coming back to is the kids seeing me calm, not this
        1.5: I've decided I'm ringing the garage tomorrow, not fiddling with it myself again
        1.6: Dentist on Thursday, I keep forgetting
        2.1: I'm ringing the garage tomorrow
        2.2: Dentist on Thursday
        3.1: I ring the garage and get to the dentist.
        """.trimIndent(),
    )

    private fun read(storage: MemoryStorage): (String) -> Js? = { name -> storage.read(name)?.let(::parseJson) }

    private fun land(storage: MemoryStorage, pages: Pages, lines: List<Landing.Line>, said: String = Telling.WRITTEN): Landing.Landed {
        val landed = Landing.landed(lines, said, nowMs, read(storage))
        for (plan in Landing.plans(landed, pages)) assertTrue(pages.carryOut(plan).ok, "${plan.block}: ${plan.refusal}")
        return landed
    }

    @Test
    fun theDayGoesOnHisClockWithTheSixInItsHiddenDirectory() {
        val (storage, pages) = pocket()
        val sorting = Landing.sorting(Telling.WRITTEN, right)
        assertTrue(sorting.lines.any { it.place == null && !it.ticked }, "his sentences that landed nowhere are offered, unticked, to place himself")
        val landed = land(storage, pages, sorting.lines)
        assertEquals(listOf(Whens.BLOCK, Landing.TELLING, Landing.NOW), landed.blocks.map { it.first }, "his time words are written with the first Yes")

        val day = Landing.dayAddress(nowMs)
        assertEquals("2026415100", day)
        val view = pages.position(Region.POCKET, Landing.NOW, day)!!
        assertEquals(Telling.WRITTEN, view.text, "the telling whole is the day's line")
        assertTrue(view.lines.isEmpty(), "nothing on the day's digits, its day-parts: ${view.lines.map { it.address }}")
        val dir = dayNode(storage)["_"] as JsObj
        assertEquals("What happened", ((dir["1"] as JsObj)["_"] as JsStr).value)
        assertEquals("the car wouldn't start again this morning", ((dir["1"] as JsObj)["1"] as JsStr).value)
        assertEquals("that I'm always the one apologising", ((dir["3"] as JsObj)["1"] as JsStr).value)

        // The hand and the rungs, in his structure block.
        val hand = parseJson(storage.read(Landing.TELLING)!!) as JsObj
        assertEquals("I'm ringing the garage tomorrow", Telling.cards(hand)["2.1.1"])
        assertEquals("I ring the garage and get to the dentist.", Telling.rungs(hand)["3.1.1"])
        assertTrue(deepEqual(Whens.first(), parseJson(storage.read(Whens.BLOCK)!!)))
    }

    @Test
    fun aBeatAlreadyOnTheDayStaysAndASecondTellingFollowsTheFirst() {
        val (storage, pages) = pocket()
        assertTrue(pages.carryOut(pages.planKeep(Landing.NOW, Landing.emptyClock())).ok)
        PocketBsp(storage, shelf) { nowMs }.call(parseJson("""{"agent_id":"pocket","block":"now","spindle":"2026415165","content":"An afternoon beat."}""") as JsObj, nowMs)
        land(storage, pages, Landing.sorting(Telling.WRITTEN, right).lines)
        assertEquals("An afternoon beat.", pages.position(Region.POCKET, Landing.NOW, "2026415165")?.text, "the day-parts stay as they were")

        val second = Landing.sorting(Telling.SECOND, Telling.placed("1.1: Rang the garage first thing\n1.6: they're coming Monday\n2.1.1: Rang the garage first thing, they're coming Monday"))
        val landed = land(storage, pages, second.lines, Telling.SECOND)
        assertFalse(landed.blocks.any { it.first == Whens.BLOCK }, "his time words are his once written")
        assertEquals(Telling.WRITTEN + "\n\n" + Telling.SECOND, pages.position(Region.POCKET, Landing.NOW, "2026415100")!!.text)
        val dir = dayNode(storage)["_"] as JsObj
        assertEquals("Rang the garage first thing", ((dir["1"] as JsObj)["3"] as JsStr).value, "after the first telling's lines")
        val hand = Telling.cards(parseJson(storage.read(Landing.TELLING)!!) as JsObj)
        assertEquals("Rang the garage first thing, they're coming Monday", hand["2.1.1"], "the card updated, not doubled")
        assertEquals(2, hand.size)
    }

    @Test
    fun hisMovesAreKeptAsExamplesAndShownToHerNextTime() {
        val (storage, pages) = pocket()
        val lines = Landing.sorting(Telling.WRITTEN, right).lines.map { l ->
            when {
                l.words == "that I'm always the one apologising" -> l.movedTo("1.1")
                l.words == "Dentist on Thursday, I keep forgetting" -> l.copy(ticked = false)
                l.place == null && l.words.startsWith("Walked home") -> l.movedTo("1.1")
                else -> l
            }
        }
        val landed = land(storage, pages, lines)
        assertEquals(listOf("that I'm always the one apologising" to "happened", "Walked home the long way by the water and felt lighter by the end of it." to "happened"), landed.examples)
        val examples = Landing.examples(parseJson(storage.read(Landing.EXAMPLES)!!))
        assertEquals(landed.examples, examples)
        val told = Telling.sortInstruction(Telling.BENCH_DAY, examples)
        assertContains(told, "Matthew placed these himself:\n“that I'm always the one apologising”: happened")
        val dir = dayNode(storage)["_"] as JsObj
        assertFalse((dir["6"]?.let(::stringify) ?: "").contains("Dentist on Thursday, I keep forgetting"), "an unticked line isn't written")
        assertEquals("that I'm always the one apologising", ((dir["1"] as JsObj)["3"] as JsStr).value)

        // Moved back where she had it, a line is no example; the examples keep the newest nine.
        val back = Landing.Line("1.3", null, "x").movedTo("1.1").movedTo("1.3")
        assertFalse(back.moved)
        assertNull(Landing.example(back))
        val many = (1..12).map { "piece $it" to "felt" }
        assertEquals((4..12).map { "piece $it" to "felt" }, Landing.examples(Landing.withExamples(null, many)))
        assertEquals("ahead", Landing.example(Landing.Line("1.1", null, "the dentist").movedTo("2.2"))?.second, "a move to the hand is ahead, to her")
    }

    @Test
    fun aWholeWriteGoesOnlyWhileTheBlockStandsAsItWasPlanned() {
        val (storage, pages) = pocket()
        land(storage, pages, Landing.sorting(Telling.WRITTEN, right).lines)
        val second = Telling.placed("1.1: Rang the garage first thing\n2.1.1: Rang the garage first thing, they're coming Monday")
        val landed = Landing.landed(Landing.sorting(Telling.SECOND, second).lines, Telling.SECOND, nowMs, read(storage))
        val plans = Landing.plans(landed, pages)
        assertTrue(pages.carryOut(pages.planAdd(Landing.TELLING, "", "A line of his, written meanwhile")).ok)
        val done = plans.map { pages.carryOut(it) }
        assertFalse(done.first { it.problem != null }.ok)
        assertEquals(Pages.CHANGED, done.first { !it.ok }.problem)
    }

    /** A clock opens at once on its page, and her instructions name it without reading its days (0.6's first CI: it never opened). */
    @Test
    fun hisClockOpensAtOnceAndHerInstructionsNameIt() {
        val (storage, pages) = pocket()
        HelloWorld.makeShell(pages, Talks.SHELL_RECIPE)
        land(storage, pages, Landing.sorting(Telling.WRITTEN, right).lines)
        val done = java.util.concurrent.CompletableFuture.supplyAsync {
            listOf(
                pages.position(Region.POCKET, Landing.NOW)?.text.orEmpty(),
                Talking.instruction(HelloWorld.SHELL, pages),
                Talks.instruction(HelloWorld.SHELL, "", Asks.plainWindow(pages, HelloWorld.SHELL), pages),
                Talking.turn("How was my day, now?", java.time.ZonedDateTime.now(), HelloWorld.SHELL, pages),
            )
        }.get(10, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(Landing.NOW_OPENING, done[0])
        assertContains(done[1], "- now, opening \"${Landing.NOW_OPENING}\"")
        assertFalse(done[1].contains("the car wouldn't start"), "the clock's days aren't read into her instruction")
        assertFalse(done[3].contains("What the app read for you"), "nor into a turn that names it")
    }

    @Test
    fun aDayToldIsSortedAndAnAskIsNot() {
        val cards = mapOf("2.1.1" to "I'm ringing the garage tomorrow")
        assertTrue(Landing.isTelling(Telling.WRITTEN, emptyMap()))
        assertTrue(Landing.isTelling(Telling.SPOKEN, emptyMap()))
        assertTrue(Landing.isTelling(Telling.SECOND, cards), "news of a card on his hand")
        assertFalse(Landing.isTelling(Telling.SECOND, emptyMap()), "short, and no card")
        assertFalse(Landing.isTelling("Add to garden: the garage needs clearing.", cards), "an ask to write")
        assertFalse(Landing.isTelling("Did I ring the garage?", cards), "a question")
        assertFalse(Landing.isTelling("Hello there", cards))
    }

    /** The day's node on the clock, walked by its digits. */
    private fun dayNode(storage: MemoryStorage): JsObj {
        var node = parseJson(storage.read(Landing.NOW)!!) as JsObj
        for (d in "20264151") node = node[if (d == '0') "_" else "$d"] as JsObj
        return node
    }
}
