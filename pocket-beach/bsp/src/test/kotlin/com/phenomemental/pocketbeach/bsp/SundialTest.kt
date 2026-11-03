package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sundial 0.10 (spine:pocket-beach 1.111178719 and the decisions at
 * 1.11117871991): the clock's grain as a word, each moment at its minute,
 * the day's line in Sundial's form, his days and the six pools read back
 * with his old P6 counted by each piece's day, the waiting room's four
 * conditions, the room read from his hatched shell, and the walks of GREET
 * and LISTEN.
 */
class SundialTest {
    private fun jsGet(v: Js?, vararg keys: String): Js? = keys.fold(v) { n, k -> com.phenomemental.pocketbeach.bsp.jsGet(n, k) }

    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val procedures = Shelf(DirShelfSource(File(System.getProperty("pocket.procedures") ?: "../assets/procedures")))
    private val london = ZoneId.of("Europe/London")
    private val now = ZonedDateTime.of(2026, 11, 3, 7, 31, 4, 120_000_000, london)
    private val nowMs = now.toInstant().toEpochMilli()
    private val h = Hatch.MATTHEW_HANDLE

    // ── the grain, as a word ──

    @Test
    fun theGrainIsAWordAndTheCodeOnlyComputesIt() {
        assertEquals(Keep.Grain.DAY, Keep.grainOf("today"))
        assertEquals(Keep.Grain.GATHERING, Keep.grainOf("this gathering"))
        assertEquals(Keep.Grain.BEAT, Keep.grainOf("at this beat"))
        assertEquals(Keep.Grain.MINUTE, Keep.grainOf("its minute"))
        assertEquals(Keep.Grain.BREATH, Keep.grainOf("this breath"))
        assertNull(Keep.grainOf("this hour"))
        // 07:31:04 UTC on 3 November 2026 (GMT in November): the beat is 2026421338, as the beach's clock says it.
        assertEquals("2026421300", Keep.grainAddress(nowMs, Keep.Grain.DAY))
        assertEquals("2026421330", Keep.grainAddress(nowMs, Keep.Grain.GATHERING))
        assertEquals("2026421338", Keep.grainAddress(nowMs, Keep.Grain.BEAT))
        assertEquals(momentToAddress(nowMs, 1), Keep.grainAddress(nowMs, Keep.Grain.MINUTE))
        assertEquals(momentToAddress(nowMs, 2), Keep.grainAddress(nowMs, Keep.Grain.BREATH))
        assertTrue(Keep.grainAddress(nowMs, Keep.Grain.MINUTE).matches(Regex("""2026421338\.[1-9]""")))
        assertEquals(listOf("2", "_", "2", "6", "4", "2", "1", "3", "3", "8"), Keep.keys("2026421338", Keep.Grain.BEAT), "the century's 0 is the underscore")
    }

    @Test
    fun aMomentGoesAtItsMinuteElseItsBreathElseItsSecond() {
        val clock = Landing.emptyClock(Days.NOW_OPENING)
        val a = Keep.Moment("Kettle's on.", nowMs)
        val b = Keep.Moment("Haven's asleep.", nowMs + 20_000)
        val c = Keep.Moment("The post came.", nowMs + 20_500)
        assertEquals(Keep.Landed(a, Keep.grainAddress(a.atMs, Keep.Grain.MINUTE), false), Keep.moment(clock, a))
        val lb = Keep.moment(clock, b)
        assertEquals(Keep.grainAddress(b.atMs, Keep.Grain.BREATH), lb.address, "a second moment in the same minute goes beneath it, at the breath")
        assertTrue(lb.address!!.startsWith(Keep.grainAddress(a.atMs, Keep.Grain.MINUTE)))
        val lc = Keep.moment(clock, c)
        assertEquals(Keep.grainAddress(c.atMs, Keep.Grain.SECOND), lc.address, "a third in the same breath goes at its second")
        assertTrue(Keep.moment(clock, a).already, "a moment already standing isn't written again")
        // Its instant in the hidden directory at 3; its words read through it.
        val node = Keep.find(clock, Keep.keys(lb.address!!, Keep.Grain.BREATH))
        assertEquals("Haven's asleep.", Keep.lineOf(node))
        assertEquals(b.atMs, Keep.instantOf(node))
        assertEquals("Haven's asleep.", collectUnderscore(node))
        // The day's moments read back in order, each with its instant.
        val day = Keep.days(clock, null, now.toLocalDate()).single()
        assertEquals(listOf("Kettle's on.", "Haven's asleep.", "The post came."), day.moments.map { it.words })
        assertEquals(listOf(a.atMs, b.atMs, c.atMs), day.moments.map { it.atMs })
        assertNull(day.text)
    }

    @Test
    fun twoWithinOneSecondJoinTheLine() {
        val clock = Landing.emptyClock(Days.NOW_OPENING)
        Keep.moment(clock, Keep.Moment("One.", nowMs))
        Keep.moment(clock, Keep.Moment("Two.", nowMs + 200))
        Keep.moment(clock, Keep.Moment("Three.", nowMs + 400))
        Keep.moment(clock, Keep.Moment("Four.", nowMs + 450))
        // One at its minute, Two at its breath, Three at its second; Four, in that same second, joins Three's line.
        val lines = Keep.days(clock, null, now.toLocalDate()).single().moments.map { it.words }
        assertEquals(listOf("One.", "Two.", "Three.\n\nFour."), lines)
    }

    @Test
    fun theDaysLineIsSundialsOnlyWhereTheDayIsEmptyOrSundials() {
        val clock = Landing.emptyClock(Days.NOW_OPENING)
        val day = now.toLocalDate()
        val first = Mark.write(Mark.Sheet("Kettle and Haven", listOf("You're up, with Haven beside you."), "", listOf(1)))
        assertEquals(Keep.DayLine.WRITTEN, Keep.dayLine(clock, day, first, nowMs))
        assertEquals(nowMs, Keep.instantOf(Keep.find(clock, Keep.keys(Days.dayAddress(day), Keep.Grain.DAY))), "the keep's instant beside the day's line")
        val again = Mark.write(Mark.Sheet("Kettle, Haven, the post", listOf("You're up, with Haven beside you.", "The post came."), "", listOf(1, 2)))
        assertEquals(Keep.DayLine.WRITTEN, Keep.dayLine(clock, day, again, nowMs + 1000), "a line Sundial wrote is folded anew")
        assertEquals(again, Keep.dayText(clock, day))
        // A line of his own is never overwritten.
        val his = Landing.emptyClock(Days.NOW_OPENING)
        Days.voiced(his, Days.dayAddress(day).take(8), "Worked with David on the proposal.")
        assertEquals(Keep.DayLine.HIS, Keep.dayLine(his, day, first, nowMs))
        assertEquals("Worked with David on the proposal.", Keep.dayText(his, day))
        assertEquals("Worked with David on the proposal.", Keep.hisOwnLine(his, day))
        assertNull(Keep.hisOwnLine(clock, day))
    }

    @Test
    fun aKeepInstantIsNoOldSixToMoveOut() {
        val (storage, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        val clock = Landing.emptyClock(Days.NOW_OPENING)
        Keep.dayLine(clock, now.toLocalDate(), Mark.write(Mark.Sheet("A day", listOf("You kept it."), "", listOf(3))), nowMs)
        check(pages.carryOut(pages.planWhole(Days.NOW, clock)).ok)
        assertFalse(Days.toMove(storage), "a string at 3 in the day line's hidden directory is its instant, not an old place")
    }

    // ── his days and the six pools, the old P6 counted by each piece's day ──

    @Test
    fun hisDaysAndPoolsCountTheOldP6ByItsDayAndNoBookingAhead() {
        val today = now.toLocalDate()
        val clock = Landing.emptyClock(Days.NOW_OPENING)
        // A Rockpool day, 1 November: his telling, no places line; his P6 holds two pieces of it.
        Days.voiced(clock, Days.dayAddress(today.minusDays(2)).take(8), "Took the dog out with Sam. I felt calm.")
        val p6 = Days.emptyP6(h)
        (jsGet(p6, "1", "1") as JsObj)["1"] = JsObj.of("_" to str("Took the dog out with Sam."), "2" to str(Days.dayAddress(today.minusDays(2))), "3" to str("2026-11-01T17:31:00.000Z"))
        (jsGet(p6, "1", "2") as JsObj)["1"] = JsObj.of("_" to str("I felt calm."), "2" to str(Days.dayAddress(today.minusDays(2))), "3" to str("2026-11-01T17:32:00.000Z"))
        // A day kept by Sundial, today, with its places line.
        Keep.dayLine(clock, today, Mark.write(Mark.Sheet("Phase two", listOf("You worked with David."), "", listOf(1, 4))), nowMs)
        // A booking on Friday: a day ahead, not a day kept.
        Keep.booking(clock, today.plusDays(3), "Meeting with Jo.")
        val days = Keep.days(clock, p6, today)
        assertEquals(listOf(Days.dayAddress(today.minusDays(2)), Days.dayAddress(today)), days.map { it.address })
        assertEquals(listOf(1, 2), days[0].places, "the old day rests where his P6 holds it")
        assertEquals(Mark.inOrder(listOf(1, 4)), days[1].places)
        assertEquals(parseIsoMs("2026-11-01T17:32:00.000Z"), days[0].keptMs, "the newest stamp of its P6 pieces")
        assertEquals(nowMs, days[1].keptMs)
        assertEquals(listOf(2, 1, 0, 1, 0, 0), Keep.counts(days))
        assertEquals("Took the dog out with Sam.", days[0].title)
        assertEquals("Phase two", days[1].title)
        val resting = Keep.restingIn(days, 2)
        assertEquals(1, resting.size)
        assertTrue(resting.single().second.contains("· I felt calm."), "the pieces resting there read with the day")
        assertEquals("Meeting with Jo.\nWhat’s ahead.", Keep.dayText(clock, today.plusDays(3)))
        assertEquals("Took the dog out with Sam. I felt calm.", Keep.lastDay(days, today)!!.text)
    }

    @Test
    fun aDayRestingIsReadWholeUpTo1500Characters() {
        val clock = Landing.emptyClock(Days.NOW_OPENING)
        val long = "You walked. ".repeat(200).trim()
        Keep.dayLine(clock, now.toLocalDate(), Mark.write(Mark.Sheet("Long", listOf(long), "", listOf(1))), nowMs)
        val body = Keep.restingIn(Keep.days(clock, null, now.toLocalDate()), 1).single().second
        assertEquals(1500, body.length)
        assertTrue(body.endsWith("…"))
    }

    // ── the waiting room (1.111178719915) ──

    @Test
    fun theWaitingRoomKeepsHisWordsAndTheirInstantsOnly() {
        var ms = Waiting.add(emptyList(), "  Kettle's on.  ", nowMs)
        ms = Waiting.add(ms, "Kettle's on.", nowMs)
        assertEquals(listOf(Keep.Moment("Kettle's on.", nowMs)), ms, "the same words at the same instant once")
        val text = Waiting.write(ms)
        assertEquals(ms, Waiting.read(text))
        val o = parseJson(text) as JsObj
        assertEquals(setOf("at", "words"), ((o["moments"] as JsArr).items.single() as JsObj).keys().toSet(), "(d) his words and their instants only")
        assertTrue(jsString(o["_"]).startsWith(Waiting.SAID), "(c) shown as what it is")
        assertEquals(emptyList(), Waiting.read(null))
        assertEquals(emptyList(), Waiting.read("not json"))
    }

    @Test
    fun keepTodayClearsItsDayAndANewDayOffersYesterdayFirstAndLetsOlderGo() {
        val today = now.toLocalDate()
        val ms = listOf(
            Keep.Moment("Three days ago.", nowMs - 3 * 86_400_000L),
            Keep.Moment("Yesterday.", nowMs - 86_400_000L),
            Keep.Moment("Today.", nowMs),
        )
        // (a) Keep today writes them and clears them.
        assertEquals(listOf("Three days ago.", "Yesterday."), Waiting.kept(ms, today).map { it.words })
        // (b) A new day: yesterday's offered first; older let go.
        val left = Waiting.letGo(ms, today)
        assertEquals(listOf("Yesterday.", "Today."), left.map { it.words })
        assertEquals(today.minusDays(1), Waiting.yesterdayWaiting(left, today))
        assertNull(Waiting.yesterdayWaiting(Waiting.kept(left, today.minusDays(1)), today))
        assertEquals(listOf("Today."), Waiting.of(ms, today).map { it.words })
    }

    @Test
    fun aWholeCopyCarriesTheWaitingRoom() {
        val (storage, _) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        val waiting = Waiting.write(listOf(Keep.Moment("Kettle's on.", nowMs)))
        val copy = Copies.read(Copies.make(storage, nowMs, waiting))
        assertEquals(listOf(Keep.Moment("Kettle's on.", nowMs)), Waiting.read(copy.waiting))
        assertNull(Copies.read(Copies.make(storage, nowMs)).waiting)
    }

    @Test
    fun theDayIsTheBeachsUtcDay() {
        // 00:30 BST on 1 July is 23:30 UTC on 30 June: the moment belongs to the day before, as the beach has it.
        val late = ZonedDateTime.of(2026, 7, 1, 0, 30, 0, 0, london).toInstant().toEpochMilli()
        assertEquals(LocalDate.of(2026, 6, 30), Keep.dayOf(late))
        assertEquals(Days.dayAddress(LocalDate.of(2026, 6, 30)), Keep.grainAddress(late, Keep.Grain.DAY))
    }

    // ── the room, from his hatched shell (1.1111787171) ──

    private fun hatchedAs(handle: String, passport: JsObj): Pair<MemoryStorage, Pages> {
        val (storage, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        storage.standInRoom = null
        val done = Hatch.carryOut(Hatch.plan(handle, passport, Hatch.MATTHEW, pages, storage, nowMs, "Adopted for the test."), pages, storage)
        check(done.problem == null) { done.problem!! }
        return storage to pages
    }

    @Test
    fun theRoomIsTheFirstPoolHisPassportNamesElseHisOwn() {
        val (s1, _) = hatchedAs(h, JsObj.of("_" to str("Matthew"), "3" to str("Meet me in the pool: leave a note in my pool on the beach (pool:Phenomemental), and I'll see it.")))
        val r1 = PoolPost.room(s1)!!
        assertEquals("Phenomemental", r1.author)
        assertEquals("Phenomemental", r1.pool)
        // A second handle posts to its own room under its own name.
        val (s2, _) = hatchedAs("ada-lovelace", JsObj.of("_" to str("Ada"), "1" to str("Her heart.")))
        val r2 = PoolPost.room(s2)!!
        assertEquals("ada-lovelace", r2.author)
        assertEquals("ada-lovelace", r2.pool)
        val m = PoolPost.prepare("pool:ada-lovelace", "Hello from the phone.", r2)
        assertEquals("https://beach.happyseaurchin.com/.well-known/pscale-beach?block=pool:ada-lovelace", m.url)
        assertEquals("""{"append":true,"content":{"1":"ada-lovelace","_":"Hello from the phone."}}""", m.body)
        assertTrue(runCatching { PoolPost.prepare("pool:Phenomemental", "Into Matthew's room.", r2) }.isFailure, "never another's room")
        assertNull(PoolPost.room(MemoryStorage()), "never before a hatch")
    }

    // ── GREET and LISTEN, walked ──

    private inner class World(override val storage: MemoryStorage, override val pages: Pages, override val big: Boolean = true) : Cook.World {
        override val handle = h
        override val shelf: Shelf = procedures
        override val now = this@SundialTest.now
        override val before: List<String> = emptyList()
        val moments = mutableListOf<Keep.Moment>()
        override fun moments(): List<Keep.Moment> = moments
        override fun write(card: Cook.Card): String? = CookActs.carryOut(card, h, pages, storage, this@SundialTest.shelf, nowMs)
    }

    private val cook get() = Cook.parse(procedures.load(Cook.SHIPPED_COOK))

    @Test
    fun theGreetingIsHandedHisLastDayAndAsksOneQuestionThroughTheToolCall() {
        val (storage, pages) = hatchedAs(h, JsObj.of("_" to str("Matthew"), "1" to str("His heart.")))
        val clock = Landing.emptyClock(Days.NOW_OPENING)
        Keep.dayLine(clock, now.toLocalDate().minusDays(1), Mark.write(Mark.Sheet("Phase two", listOf("You worked with David."), "print the minutes", listOf(1))), nowMs - 86_400_000L)
        check(pages.carryOut(pages.planWhole(Days.NOW, clock)).ok)
        val w = World(storage, pages)
        val k = Cook.Walk(cook, w, "").start("6.3")
        assertTrue(k.done)
        assertEquals(listOf("THE QUESTION"), k.parts)
        val handed = k.handed.single()
        assertTrue(handed.line.startsWith("The app: he has opened his sundial and hasn't spoken to you yet today."), handed.line)
        assertTrue(handed.material.single().contains("Tomorrow: print the minutes"), handed.material.toString())
        assertEquals("print the minutes", Keep.tomorrowOf(Keep.lastDay(w.kept(), now.toLocalDate())))
        // On E2B the same walk asks her reply free.
        assertEquals(listOf("THE QUESTION"), Cook.Walk(cook, World(storage, pages, big = false), "").start("6.3").parts)
    }

    @Test
    fun listeningReadsTheDaysRestingThereAndAWhisperIsKeptInHisStashOnHisTap() {
        val (storage, pages) = hatchedAs(h, JsObj.of("_" to str("Matthew"), "1" to str("His heart.")))
        val clock = Landing.emptyClock(Days.NOW_OPENING)
        Keep.dayLine(clock, now.toLocalDate().minusDays(1), Mark.write(Mark.Sheet("Calm", listOf("You felt calm by the water."), "", listOf(2))), nowMs - 86_400_000L)
        check(pages.carryOut(pages.planWhole(Days.NOW, clock)).ok)
        val k = Cook.Walk(cook, World(storage, pages), "")
        k.carried["THE PLACE"] = "what you felt"
        k.start("6.4")
        val days = k.carried["THE DAYS"] as String
        assertTrue(days.contains("You felt calm by the water."), days)
        assertEquals(listOf("THE WAY", "THE HEARD", "THE NOTICING", "THE QUESTION"), k.parts)
        val card = (k.waiting as Cook.Wait.Him).card as Cook.Card.Whisper
        assertEquals(2, card.pool)
        k.changed(card.copy(words = "It was the water, mostly."))
        k.yes()
        val stash = parseJson(storage.read(Hatch.stashOf(h))!!)
        val entry = (1..9).mapNotNull { jsGet(stash, "$it") as? JsObj }.last()
        assertTrue(jsString(entry["_"]).startsWith("whisper · what you felt · "), jsString(entry["_"]))
        assertEquals("It was the water, mostly.", jsString(entry["1"]))
    }

    @Test
    fun theClockScreenSaysAMomentByItsTime() {
        val at = momentToAddress(nowMs, 1)
        assertEquals(11, Clock.level(at))
        assertEquals(9, Clock.level("2026421330"))
        assertEquals(LocalDate.of(2026, 11, 3), Clock.dateOf(at))
        assertEquals(listOf("2026000000", "2026400000", "2026420000", "2026421000", "2026421300", "2026421330", "2026421338"), Clock.trail(at))
        val said = Clock.rungSaid(at, LocalDate.of(2026, 11, 3), london)
        assertTrue(said.startsWith("Today, Tuesday 3 November, 07:3"), said)
        assertEquals("Today, Tuesday 3 November, early morning", Clock.rungSaid("2026421330", LocalDate.of(2026, 11, 3), london))
    }

    @Test
    fun wordsSundialReadsAsABlock() {
        val words = procedures.load("words:sundial") as JsObj
        assertEquals("what happened", jsString(jsGet(words, "1", "1", "_")))
        assertEquals("#6FA8DC", jsString(jsGet(words, "1", "6", "2")))
        assertEquals("If you need help now: your local emergency number, or Samaritans on 116 123 (UK and Ireland).", jsString(jsGet(words, "7", "1")))
        for (k in 1..6) assertEquals(Mark.POOLS[k - 1].phrase, jsString(jsGet(words, "1", "$k", "_")), "place $k as mark.js names it")
        assertNotNull(jsGet(words, "6", "6", "1"))
    }

    /** About, as Claude asked after reading judged-0.10.md: no daily limit claimed; one thing leaves, her own-Claude voice held for 0.12. */
    @Test
    fun aboutClaimsNoDailyLimitAndOnlyTheRoomNoteLeaves() {
        val words = procedures.load("words:sundial") as JsObj
        val why = jsString(jsGet(words, "6", "3", "1"))!!
        assertFalse(why.contains("each day has a limit") || why.contains("It is bounded"), why)
        val where = jsString(jsGet(words, "6", "6", "1"))!!
        assertTrue(where.contains("One thing can leave, and only when you choose: a note you post to your room, sent when you tap Post."), where)
        assertFalse(where.contains("Claude") || where.contains("Anthropic") || where.contains("Two things"), where)
        // Held for 0.12, beneath the part, where About doesn't read it.
        assertTrue(jsString(jsGet(words, "6", "6", "9", "1"))!!.contains("if you choose your own Claude as her voice"))
        assertNull(jsGet(words, "6", "6", "2"), "nothing at 6.6.2, which About would show as a part of its own")
    }

    /** Only the phone's own pocket posts: a room read from a pocket held in memory is scratch, and its post never touches the wire. */
    @Test
    fun aScratchPocketsPostNeverTouchesTheWire() {
        val sent = mutableListOf<String>()
        val wire = Wire { url, _ -> sent += url; WireReply(200, """{"slot":"7"}""") }
        // The bench's scratch pocket: its stand-in room is marked scratch.
        val (bench, _) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        val standIn = PoolPost.room(bench)!!
        assertTrue(standIn.scratch)
        val fromBench = PoolPost.prepare("pool:${standIn.pool}", "From the bench.", standIn)
        assertTrue(fromBench.scratch)
        val refused = PoolPost.send(fromBench, wire)
        assertTrue(refused is Posted.Refused && refused.why == PoolPost.SCRATCH)
        // A scratch pocket hatched (as a trial's copy of a hatched pocket is): still scratch.
        val (hatched, _) = hatchedAs("ada-lovelace", JsObj.of("_" to str("Ada"), "1" to str("Her heart.")))
        val room = PoolPost.room(hatched)!!
        assertTrue(room.scratch)
        assertTrue(PoolPost.send(PoolPost.prepare("pool:ada-lovelace", "From a trial.", room), wire) is Posted.Refused)
        // A trial's copy of a scratch pocket keeps its stand-in room, and it stays scratch.
        val planned = Acts.propose("", listOf(Acts.Action.Post("From a trial's copy.")), bench, shelf, HelloWorld.SHELL_OPENING, "seal", nowMs)
        planned.proposal?.post?.let { assertTrue(it.scratch); assertTrue(PoolPost.send(it, wire) is Posted.Refused) }
        assertTrue(sent.isEmpty(), "the wire was never touched: $sent")
        // The phone's own room (never held in memory) goes.
        assertTrue(PoolPost.send(PoolPost.prepare("pool:Phenomemental", "From the phone.", Room("Phenomemental", "Phenomemental")), wire) is Posted.Landed)
        assertEquals(1, sent.size)
    }
}
