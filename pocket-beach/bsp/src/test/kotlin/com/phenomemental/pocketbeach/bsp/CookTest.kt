package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Rockpool, pscale as procedure (spine:pocket-beach 1.111178714): the shipped
 * cook read whole; each procedure walked on a pocket hatched as his; the
 * proofs of point 6, a block edited and the behaviour changed with no code
 * changed; and the cases 0.8.1 held, held still.
 */
class CookTest {
    private fun jsGet(v: Js?, vararg keys: String): Js? = keys.fold(v) { n, k -> com.phenomemental.pocketbeach.bsp.jsGet(n, k) }

    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val procedures = Shelf(DirShelfSource(File(System.getProperty("pocket.procedures") ?: "../assets/procedures")))
    private val now = ZonedDateTime.of(2026, 11, 1, 17, 30, 0, 0, ZoneId.of("Europe/London"))
    private val nowMs = now.toInstant().toEpochMilli()
    private val h = Hatch.MATTHEW_HANDLE

    private val passport: JsObj = JsObj.of(
        "_" to str("Matthew Bushell — digital social innovator, aspiring writer, therapeutic coach."),
        "1" to str("The heart. bi-polar · one axis, two ends. " + "whole-less, as in boundless. ".repeat(60)),
        "2" to str("The other side of the &: people who will meet me in the middle and build with, not at. " + "LEROs and CLERO. ".repeat(80)),
        "6" to JsObj.of("_" to str("WHERE THINGS GO — for an AI working with me. Confirmed with me in conversation."), "1" to str("Films go in films."), "2" to str("Ideas go in my stash.")),
    )

    private fun hatched(): Pair<MemoryStorage, Pages> {
        val (storage, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
        val done = Hatch.carryOut(Hatch.plan(h, passport, Hatch.MATTHEW, pages, storage, nowMs, "Adopted for the test."), pages, storage)
        check(done.problem == null) { done.problem!! }
        for (b in listOf("films", "book")) check(pages.carryOut(pages.planNewBlock(b)).ok)
        return storage to pages
    }

    private inner class World(override val storage: MemoryStorage, override val pages: Pages, override val big: Boolean = true, override val before: List<String> = emptyList()) : Cook.World {
        override val handle = h
        override val shelf: Shelf = procedures
        override val now = this@CookTest.now
        val sent = mutableListOf<String>()
        override fun write(card: Cook.Card): String? = CookActs.carryOut(card, h, pages, storage, this@CookTest.shelf, nowMs)
        override fun send(words: String): String? { sent += words; return null }
    }

    private val cook get() = Cook.parse(procedures.load(Cook.SHIPPED_COOK))

    private fun walk(world: World, said: String, cookBlock: Cook.Parsed = cook): Cook.Walk = Cook.Walk(cookBlock, world, said).start("1")

    private fun edited(edit: (JsObj) -> Unit): Cook.Parsed {
        val block = parseJson(stringify(procedures.load(Cook.SHIPPED_COOK))!!) as JsObj
        edit(block)
        return Cook.parse(block)
    }

    // ── the shipped blocks read whole ──

    @Test
    fun theShippedCookReadsWhole() {
        val p = cook
        assertEquals(emptyList(), p.refused)
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8"), p.procedures.keys.toList())
        assertEquals(listOf("ANSWER", "KEEP", "POST", "MAKE", "READ", "TELL MY DAY", "CLOSE", "HER NAME"), p.procedures.values.map { it.name })
        assertEquals(listOf("HER NAME", "POST", "MAKE", "TELL MY DAY", "KEEP", "READ", "CHANGE"), p.cues.answers.keys.toList())
        assertTrue("i meant" in p.cues.asking)
        val k = Cook.kernel(procedures.load(Cook.SHIPPED_KERNEL))
        assertEquals("1", k.message)
        assertEquals("7", k.close)
    }

    @Test
    fun aStepTheWalkerCantReadRefusesTheBlockAndTheShippedCopyRuns() {
        val (storage, pages) = hatched()
        val mine = parseJson(stringify(procedures.load(Cook.SHIPPED_COOK))!!) as JsObj
        (mine["3"] as JsObj)["4"] = str("Set out something lovely for him, whatever he likes.")
        check(pages.carryOut(pages.planWhole(Cook.cookOf(h), mine)).ok)
        val loaded = Cook.load(h, storage, procedures)
        assertFalse(loaded.own)
        assertTrue(loaded.refusal!!.startsWith("Your cook couldn't be read at 3.4"), loaded.refusal)
        assertTrue(loaded.parsed.ok)
    }

    @Test
    fun hisOwnCookRunsWhenItReads() {
        val (storage, pages) = hatched()
        Cook.bringIn(h, pages, storage, procedures).forEach { check(pages.carryOut(it).ok) }
        val loaded = Cook.load(h, storage, procedures)
        assertTrue(loaded.own)
        assertNull(loaded.refusal)
        assertEquals(emptyList(), Cook.bringIn(h, pages, storage, procedures), "once in, nothing more")
    }

    @Test
    fun aCookHeChangedIsLeftAsHeHasIt() {
        val (storage, pages) = hatched()
        Cook.bringIn(h, pages, storage, procedures).forEach { check(pages.carryOut(it).ok) }
        val shippedBefore = storage.read(Cook.cookOf(h))!!
        check(pages.carryOut(pages.planChange(Cook.cookOf(h), "9.5", (procedures.load(Cook.SHIPPED_COOK) as JsObj).let { jsString(jsGet(it, "9", "5")) } + ", stow")).ok)
        assertEquals(emptyList(), Cook.bringIn(h, pages, storage, procedures) { shippedBefore.takeIf { false } }, "his change is his")
    }

    // ── ANSWER: what his message asks for ──

    @Test
    fun answerTakesWhatHisMessageAsksFor() {
        val (storage, pages) = hatched()
        val w = World(storage, pages)
        fun asked(said: String) = walk(w, said).answer
        assertEquals("TALK", asked("Hi Gemma I've been making huge progress with my film making and am eagerly awaiting my first pilot to be rendered. I'm hoping it's endearing."))
        assertEquals("KEEP", asked("Keep that for me please"))
        assertEquals("MAKE", asked("Make a block called films: I'm looking for fast ways to develop films"))
        assertEquals("CHANGE", asked("Change your opening line to: I'm Gemma"))
        assertEquals("HER NAME", asked("I'd like to call you Gemma"))
        assertEquals("HER NAME", asked("Hey I'm quite happy to call you Gemma I like Gemma and one of the main things that I'm trying to produce is a tool"))
        assertEquals("TALK", asked("I don't feel safe tonight."))
        assertEquals("TALK", asked("What have I got on today?"))
        assertEquals("POST", asked("Can you post a message to my room on the beach?"))
        assertEquals("POST", asked("Send a post to my room."))
        assertEquals("POST", asked("Tell my room I'm back on Tuesday"))
        assertEquals("POST", asked("Leave a note in my pool: back soon"))
        assertEquals("READ", asked("Read my passport"))
        assertEquals("READ", asked("What does my passport say about where things go?"))
        assertEquals("READ", asked("What do you know about my films?"))
        // His first evening on 0.8 (1.1111787149).
        assertEquals("KEEP", asked("I meant Keep that for me please"))
        assertEquals("KEEP", asked("Hold that for me"))
        assertEquals("TELL MY DAY", asked("Can you add in a little from my day today, my now?"))
    }

    // ── KEEP ──

    @Test
    fun keepThatOffersHerTheRingWithHisStashFirstThenSetsOutHisWords() {
        val (storage, pages) = hatched()
        val first = "Hi Gemma I've been making huge progress with my film making."
        val w = World(storage, pages, before = listOf(first))
        val k = walk(w, "Keep that for me please")
        val her = k.waiting as Cook.Wait.Her
        assertEquals("stash", her.rings.single().places.first())
        assertTrue("films" in her.rings.single().places)
        assertEquals(HerWindow.WRITE_LINE, k.handed.single().line)
        k.her(listOf(1))
        val card = (k.waiting as Cook.Wait.Him).card as Cook.Card.Keep
        assertEquals(Cook.Card.Keep(Hatch.stashOf(h), first), card)
        k.yes()
        assertTrue(k.done)
        assertTrue(storage.read(Hatch.stashOf(h))!!.contains("film making"))
        assertTrue(k.accounts.any { it.contains("kept in stash") }, k.accounts.toString() + "\n" + k.trace.joinToString("\n"))
    }

    @Test
    fun onE2BTheRingGoesToHim() {
        val (storage, pages) = hatched()
        val k = walk(World(storage, pages, big = false, before = listOf("Remember the wifi code is otter-42")), "Keep that for me please")
        val ring = (k.waiting as Cook.Wait.Him).card as Cook.Card.Ring
        assertEquals(Hatch.stashOf(h), ring.places.first())
        k.hisChoice("films")
        assertEquals(Cook.Card.Keep("films", "Remember the wifi code is otter-42"), (k.waiting as Cook.Wait.Him).card)
    }

    @Test
    fun aBlockHisWordsNameIsWhereItGoesWithNoRing() {
        val (storage, pages) = hatched()
        val k = walk(World(storage, pages), "Keep this in films: Jo's doing the music")
        assertEquals(Cook.Card.Keep("films", "Jo's doing the music"), (k.waiting as Cook.Wait.Him).card)
    }

    @Test
    fun iMeantIsTakenOffAndHoldThatIsHeard() {
        val (storage, pages) = hatched()
        val w = World(storage, pages, before = listOf("The pilot renders on Friday"))
        val k = walk(w, "I meant Keep that for me please")
        assertEquals("The pilot renders on Friday", k.carried["THE WORDS"])
        val hold = walk(w, "Hold that for me")
        assertEquals("KEEP", hold.answer)
        assertEquals("The pilot renders on Friday", hold.carried["THE WORDS"])
    }

    // ── POST ──

    @Test
    fun hisOwnWordsAreSetOutAsAPostWithNoModelCall() {
        val (storage, pages) = hatched()
        val w = World(storage, pages)
        val k = walk(w, "Post this message to my room: \"I've had a relaxing weekend.\"")
        assertEquals(Cook.Card.Post("I've had a relaxing weekend."), (k.waiting as Cook.Wait.Him).card)
        assertTrue(k.trace.none { it.reader == Cook.Reader.SHE }, "no model call")
        assertEquals(HerWindow.POST_LINE, k.handed.single().line)
        k.yes()
        assertEquals(listOf("I've had a relaxing weekend."), w.sent)
    }

    @Test
    fun withNoWordsOfHisSheAsksWhatHeWouldLikeToSay() {
        val (storage, pages) = hatched()
        val k = walk(World(storage, pages), "Send a post to my room.")
        assertTrue(k.done)
        assertTrue(k.handed.single().line.contains("Ask him, in a few words, what he'd like to say"))
    }

    // ── MAKE ──

    @Test
    fun aNewBlockIsSetOutByCode() {
        val (storage, pages) = hatched()
        val k = walk(World(storage, pages), "Make a block called sailing: knots I'm learning")
        assertEquals(Cook.Card.New("sailing", "knots I'm learning"), (k.waiting as Cook.Wait.Him).card)
        assertEquals(HerWindow.WRITE_LINE, k.handed.single().line)
        k.yes()
        assertTrue(pages.exists("sailing"))
    }

    @Test
    fun aBlockThatStandsIsKeptIn() {
        val (storage, pages) = hatched()
        val k = walk(World(storage, pages), "Make a block called films: I'm looking for fast ways to develop films")
        assertEquals(Cook.Card.Keep("films", "I'm looking for fast ways to develop films"), (k.waiting as Cook.Wait.Him).card)
    }

    // ── READ ──

    @Test
    fun readMyPassportGivesHerItsPlacesAndHerChoiceIsReadWhole() {
        val (storage, pages) = hatched()
        val k = walk(World(storage, pages), "Read my passport")
        val her = k.waiting as Cook.Wait.Her
        assertEquals(listOf("1. The heart.", "2. The other side of the &: people who will meet me in the middle and build with, not at.", "6. WHERE THINGS GO — for an AI working with me."), her.rings.single().places)
        k.her(listOf(3))
        val handed = k.handed.single()
        assertTrue(handed.material.first().contains("6. WHERE THINGS GO"), handed.material.toString())
        assertTrue(handed.material.last().startsWith("(Place 6 of his passport, read whole:)\nWHERE THINGS GO"), handed.material.toString())
    }

    @Test
    fun thePlaceHisWordsPointToIsReadWholeWithNoRing() {
        val (storage, pages) = hatched()
        val k = walk(World(storage, pages), "What does my passport say about where things go?")
        assertTrue(k.done)
        assertTrue(k.trace.none { it.reader == Cook.Reader.SHE })
        assertTrue(k.handed.single().material.last().contains("- Films go in films."))
    }

    @Test
    fun aBlockNamedInPassingIsNotRead() {
        val (storage, pages) = hatched()
        val k = walk(World(storage, pages), "I've been book writing all afternoon and the films are coming on.")
        assertEquals("TALK", k.answer)
        assertTrue(k.handed.all { it.material.isEmpty() })
        assertTrue(k.reads.isEmpty())
    }

    // ── HER NAME ──

    @Test
    fun herNameHeldOrSetOut() {
        val (storage, pages) = hatched()
        val held = walk(World(storage, pages), "I'd like to call you Gemma")
        assertTrue(held.done)
        assertTrue(held.handed.single().line.contains("the name you already have"), held.handed.toString() + "\n" + held.trace.joinToString("\n"))
        val ada = walk(World(storage, pages), "Your name is Ada")
        assertEquals(Cook.Card.Name("Ada"), (ada.waiting as Cook.Wait.Him).card)
        assertTrue(ada.handed.single().line.contains("(“Ada it is.”)"))
    }

    // ── CHANGE ──

    @Test
    fun herLinesAreHisToChangeAndHisOwnGoToTheWritingCheck() {
        val (storage, pages) = hatched()
        val lines = walk(World(storage, pages), "Change your opening line to: I'm Gemma")
        assertEquals(HerWindow.LINES_LINE, lines.handed.single().line)
        assertFalse(lines.writingCheck)
        val his = walk(World(storage, pages), "Change the bulbs line in garden to: plant the tulips")
        assertTrue(his.writingCheck)
        assertEquals(HerWindow.WRITE_JOB_LINE, his.handed.single().line)
    }

    // ── TELL MY DAY ──

    @Test
    fun hisDayLandsTheBeachsWay() {
        val (storage, pages) = hatched()
        val telling = "Took the dog out with Sam this afternoon. I felt calm for the first time this week. I need to ring Jo tomorrow about the music. This month is all about the pilot."
        val k = Cook.Walk(cook, World(storage, pages), telling).start("6")
        val her = k.waiting as Cook.Wait.Her
        assertEquals(4, her.rings.size)
        assertEquals(6, her.rings.first().places.size)
        assertTrue(her.rings.first().places.first().startsWith("FACTS"), her.rings.first().places.first())
        k.her(listOf(1, 2, 5, 4))
        val day = ((k.waiting as Cook.Wait.Him).card as Cook.Card.Day).landing
        assertTrue(day.lines.any { it.where == Days.Where.Booking(now.toLocalDate().plusDays(1)) && it.words.startsWith("I need to ring Jo") })
        assertTrue(day.lines.any { it.where == Days.Where.Rung("month") })
        assertTrue(day.lines.any { it.where == Days.Where.Portal("1.2") && it.words.startsWith("I felt calm") })
        k.yes()
        val p6 = parseJson(storage.read(Days.p6Of(h))!!)
        assertEquals("Took the dog out with Sam this afternoon.", jsString(jsGet(p6, "1", "1", "1", "_")))
        assertEquals("I felt calm for the first time this week.", jsString(jsGet(p6, "1", "2", "1", "_")))
        val clock = storage.read(Days.NOW)!!
        assertTrue(clock.contains("I need to ring Jo tomorrow"))
        assertFalse(clock.contains("\"1\":{\"_\":\"What happened\""), "no P6 in his now")
    }

    @Test
    fun theOldTellingMovesOutOnce() {
        val (storage, pages) = hatched()
        val old = Telling.shape()
        (old["2"] as JsObj)["2"] = JsObj.of("_" to str("This week"), "1" to str("Cut the second film"))
        (old["3"] as JsObj)["2"] = JsObj.of("_" to str("This month"), "1" to str("Get the pilot out"))
        check(pages.carryOut(pages.planWhole(Landing.TELLING, old)).ok)
        val (clock, _) = Landing.withDay(null, Landing.dayAddress(nowMs), "Walked the dog.", mapOf("1.1" to listOf("Walked the dog."), "1.2" to listOf("Calm")))
        check(pages.carryOut(pages.planWhole(Landing.NOW, clock)).ok)
        assertTrue(Days.toMove(storage))
        val moved = Days.moveOut(h, storage, nowMs)!!
        assertEquals(2, moved.pieces)
        assertEquals(1, moved.cards)
        assertEquals(1, moved.rungs)
        assertEquals("Calm", jsString(jsGet(moved.p6!!, "1", "2", "1", "_")))
        assertEquals("Cut the second film", jsString(jsGet(moved.ahead!!, "3", "1", "_")))
        assertFalse(stringify(moved.now)!!.contains("Calm"), "the six are out of his now")
        assertTrue(stringify(moved.now)!!.contains("Walked the dog."), "the day's line stays")
    }

    // ── the proofs (point 6): a block edited, the behaviour changed, no code changed ──

    @Test
    fun proofA_aCueLineEditedChangesWhatIsHeard() {
        val (storage, pages) = hatched()
        val said = "Send a message to my room: back on Tuesday"
        val without = edited { b -> ((b["9"] as JsObj))["2"] = str(jsString(jsGet(b, "9", "2")).replace("send … to my room, ", "")) }
        val talk = walk(World(storage, pages), said, without)
        assertEquals("TALK", talk.answer)
        assertNull(talk.waiting)
        val with = walk(World(storage, pages), said, cook)
        assertEquals("POST", with.answer)
        assertEquals(Cook.Card.Post("back on Tuesday"), (with.waiting as Cook.Wait.Him).card)
    }

    @Test
    fun proofB_theKeepRingReorderedChangesWhatIsOfferedFirst() {
        val (storage, pages) = hatched()
        val w = World(storage, pages, before = listOf("Jo's doing the music"))
        assertEquals("stash", (walk(w, "Keep that for me please").waiting as Cook.Wait.Her).rings.single().places.first())
        val moved = edited { b ->
            val ring = jsGet(b, "2", "5", "2", "1") as JsObj
            ring["1"] = str("films"); ring["2"] = str("book"); ring["3"] = str("his stash"); ring["4"] = str("his other blocks, in his manifest's order")
        }
        val places = (walk(w, "Keep that for me please", moved).waiting as Cook.Wait.Her).rings.single().places
        assertEquals(listOf("films", "book", "stash"), places.take(3))
    }

    @Test
    fun proofC_aStepRewrittenChangesTheTrace() {
        val (storage, pages) = hatched()
        val said = "Post this message to my room: \"Back on Tuesday\""
        val before = walk(World(storage, pages), said)
        val rewritten = edited { b -> (b["3"] as JsObj)["3"] = str("Hand her this line: “The app: his words are set out beneath for his tap. Say only: “It's there for your tap.””") }
        val after = walk(World(storage, pages), said, rewritten)
        assertEquals(HerWindow.POST_LINE, before.handed.single().line)
        assertEquals("The app: his words are set out beneath for his tap. Say only: “It's there for your tap.”", after.handed.single().line)
        assertEquals(before.trace.map { it.at }, after.trace.map { it.at }, "the same steps")
        assertTrue(before.trace.map { it.toString() } != after.trace.map { it.toString() } || before.handed != after.handed)
    }

    // ── what 0.8.1 held, held still ──

    @Test
    fun theCasesTheGateHeldStillHold() {
        val (storage, pages) = hatched()
        val w = World(storage, pages)
        for (said in listOf("Send a post to my room.", "Send a message to my room", "Tell my room I'm back on Tuesday", "Post this message to my room: \"Hello all\"", "Can you post a message to my room on the beach?", "Leave a note in my pool: back soon"))
            assertEquals("POST", walk(w, said).answer, said)
        for (said in listOf("I've been making huge progress with my film making.", "What's in my room on the beach?"))
            assertTrue(walk(w, said).answer != "POST", said)
        assertEquals("back soon", walk(w, "Leave a note in my pool: back soon").carried["THE WORDS"])
        assertEquals("Hello all", walk(w, "Can you send “Hello all” to my room?").carried["THE WORDS"])
        assertNotNull(walk(w, "Keep that for me please"))
    }
}
