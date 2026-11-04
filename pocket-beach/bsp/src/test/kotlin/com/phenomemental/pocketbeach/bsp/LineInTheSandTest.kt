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
 * THE LINE IN THE SAND (spine:pocket-beach 1.11117871992, 4 November 2026):
 * the nine tests every Sundial release is held to, from 0.10.3 on. Each test
 * here is the CODE part of one of them, what the app does itself, run with
 * his own messages of 4 November. The RECIPE parts rest on lines handed to
 * her and are run against his cook on the laptop's E4B (trial/Sand.kt); what
 * only his Pixel can confirm (her speed, what he sees) is said in the report.
 */
class LineInTheSandTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val procedures = Shelf(DirShelfSource(File(System.getProperty("pocket.procedures") ?: "../assets/procedures")))
    private val now = ZonedDateTime.of(2026, 11, 4, 9, 54, 0, 0, ZoneId.of("Europe/London"))
    private val nowMs = now.toInstant().toEpochMilli()
    private val today = Keep.dayOf(nowMs)
    private val h = Hatch.MATTHEW_HANDLE

    private val passport: JsObj = JsObj.of(
        "_" to str("Matthew Bushell — digital social innovator, aspiring writer, therapeutic coach."),
        "1" to str("The heart. bi-polar · one axis, two ends."),
    )

    private fun hatched(): Pair<MemoryStorage, Pages> {
        val (storage, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
        val done = Hatch.carryOut(Hatch.plan(h, passport, Hatch.MATTHEW, pages, storage, nowMs, "Adopted for the test."), pages, storage)
        check(done.problem == null) { done.problem!! }
        Cook.bringIn(h, pages, storage, procedures).forEach { check(pages.carryOut(it).ok) }
        return storage to pages
    }

    private inner class World(override val storage: MemoryStorage, override val pages: Pages, val hand: Map<String, String> = emptyMap(), override val now: ZonedDateTime = this@LineInTheSandTest.now) : Cook.World {
        override val handle = h
        override val shelf: Shelf = procedures
        override val big = true
        override val before: List<String> = emptyList()
        val moments = mutableListOf<Keep.Moment>()
        override fun moments(): List<Keep.Moment> = moments
        override fun cards(): Map<String, String> = hand.ifEmpty { Days.handOf(storage.read(Days.aheadOf(h))?.let(::parseJson) as? JsObj) }
        override fun write(card: Cook.Card): String? = CookActs.carryOut(card, h, pages, storage, this@LineInTheSandTest.shelf, nowMs)
    }

    private val shippedCook: JsObj get() = parseJson(stringify(procedures.load(Cook.SHIPPED_COOK))!!) as JsObj
    private val cook get() = Cook.parse(procedures.load(Cook.SHIPPED_COOK))
    private val shipped: Map<String, String> get() = Cook.places(procedures.load(Cook.SHIPPED_COOK))

    /** The day check as cook 1.1.8.3's sentence states it: every number and clause read from the sentence. */
    private fun checkOf(sentence: String): Landing.DayCheck = (Cook.read(sentence)!!.second as Cook.Verb.TellsDay).check
    private val shippedCheck get() = checkOf(shipped.getValue("1.1.8.3"))

    /** His hand on 4 November, as his pocket copy holds it (spine 1.111178719923). */
    private val hisHand = mapOf("3.1" to "I need to finish the membership form", "2.1" to "I'm going to do the ID checks")

    private fun js(s: String): Js = parseJson(s)
    private fun clock(storage: BlockStorage): JsObj? = storage.read(Days.NOW)?.let(::parseJson) as? JsObj
    private fun hand(storage: BlockStorage): Map<String, String> = Days.handOf(storage.read(Days.aheadOf(h))?.let(::parseJson) as? JsObj)

    /** His history's entries, oldest first, each its opening line. */
    private fun history(storage: BlockStorage): List<String> {
        val out = mutableListOf<String>()
        fun walk(n: Js?) {
            val o = n as? JsObj ?: return
            val inner = o["_"]
            if (inner is JsObj) walk(inner)
            for (k in 1..9) (o["$k"] as? JsObj)?.let { e -> collectUnderscore(e)?.let { out += it } }
        }
        walk(storage.read(Hatch.historyOf(h))?.let(::parseJson))
        return out
    }

    // ── 1, 2, 4, 5, 6: the app's own day judgement (cook 1.1.8.3), exactly as its sentence says ──

    @Test
    fun test1_hisDayIsJudgedHisDayByTheSentenceAndChangingTheSentenceChangesTheCheck() {
        val said = "I walked Haven this morning then spent an hour on the membership form"
        // As shipped: three sentences and forty words, or a word of five letters or more that a card on his hand also holds.
        assertTrue(Landing.tellsDay(said, hisHand, emptySet(), shippedCheck), "“membership” is on his hand")
        assertFalse(Landing.tellsDay(said, emptyMap(), emptySet(), shippedCheck), "one sentence of thirteen words, and no card")
        // He changes the numbers in the sentence, and the check is what the sentence now says.
        val his = shipped.getValue("1.1.8.3").replace("three sentences or more and forty words or more", "one sentence or more and twelve words or more")
        assertTrue(Landing.tellsDay(said, emptyMap(), emptySet(), checkOf(his)))
        assertFalse(Landing.tellsDay("Everything feels too much this morning", emptyMap(), emptySet(), checkOf(his)), "six words")
        val longer = shipped.getValue("1.1.8.3").replace("a word of five letters or more", "a word of eleven letters or more")
        assertFalse(Landing.tellsDay(said, hisHand, emptySet(), checkOf(longer)), "“membership” has ten letters")
        // Through his cook's walk: the day line hands her the places line; the walk says why.
        val (storage, pages) = hatched()
        val k = Cook.Walk(cook, World(storage, pages, hisHand), said).start("1")
        assertTrue(k.trace.any { it.did == "his words tell of his day" }, k.trace.joinToString("\n") { it.did })
        assertTrue(k.handed.any { it.line.contains("Places:") })
    }

    @Test
    fun test2_hisShortAnswersAreNeverJudgedHisDay() {
        val (storage, pages) = hatched()
        for (said in listOf("The sound. the space.", "stiff but free", "the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling.")) {
            assertFalse(Landing.tellsDay(said, hisHand, emptySet(), shippedCheck), said)
            val k = Cook.Walk(cook, World(storage, pages, hisHand), said).start("1")
            assertTrue(k.trace.any { it.did == "his words don't tell of his day" }, said)
            val noDay = Regex("“(.+)”").find(shipped.getValue("1.1.8.3.2.1"))!!.groupValues[1]
            assertTrue(k.handed.any { it.line == noDay }, "the no-day line: $said")
            assertTrue(k.handed.none { it.line.contains("Places:") }, "no places asked of her: $said")
        }
    }

    @Test
    fun tests4to6_tooMuchAskingAndAQuestionAreNotHisDay() {
        for (said in listOf(
            "Everything feels too much this morning.",
            "Tell me about Alfred North Whitehead",
            "Boole's work on the middle point. Are you familiar?",
            "I finished the ID checks today. Do you know what a LERO is?",
        )) assertFalse(Landing.tellsDay(said, hisHand, emptySet(), shippedCheck), said)
        // The sentence of 0.9.1 to 0.10.2, should his cook still hold it, still reads and checks as it did.
        val old = "Does THE MESSAGE tell of his day: three sentences or more and forty words or more in all, or news of a card on his hand?"
        assertEquals(Landing.DayCheck.SINCE_09, checkOf(old))
        // A number that isn't one is said plainly.
        val bad = shipped.getValue("1.1.8.3").replace("forty words", "fourty words")
        assertNull(Cook.read(bad))
        assertTrue(Cook.why(bad, shipped.getValue("1.1.8.3")).contains("“fourty” isn't a number"), Cook.why(bad, shipped.getValue("1.1.8.3")))
    }

    // ── 3: what lands on his hand: cutting, booking, a card taken off ──

    @Test
    fun test3_eachMomentIsItsOwnPieceALineBreakEndsOneAndACardIsOnlyThePieceHoldingTheWords() {
        val a = Keep.Moment("I walked Haven this morning then spent an hour on the membership form", nowMs - 600_000)
        val b = Keep.Moment("I'm going to finish the membership form\nthe garden looked lovely", nowMs - 300_000)
        val c = Keep.Moment("tomorrow I want to walk somewhere new", nowMs - 60_000)
        assertEquals(
            listOf("I walked Haven this morning then spent an hour on the membership form", "I'm going to finish the membership form", "the garden looked lovely", "tomorrow I want to walk somewhere new"),
            Cook.units(Cook.Gathered(listOf(a, b, c), java.time.ZoneOffset.UTC), ""),
        )
        val (storage, pages) = hatched()
        val w = World(storage, pages)
        w.moments += listOf(a.copy(places = listOf(1)), b.copy(places = listOf(1, 6)), c.copy(places = listOf(6)))
        val k = Cook.Walk(cook, w, "").start("6.1")
        assertTrue(k.waiting is Cook.Wait.HerSheet, "every moment placed already: she isn't asked")
        k.sheet(Mark.Sheet("Haven and the form", listOf("I walked Haven and worked on the membership form."), "walk somewhere new"))
        @Suppress("UNCHECKED_CAST") val pieces = k.carried["THE PIECES"] as List<String>
        assertEquals(4, pieces.size, pieces.joinToString(" | "))
        val card = ((k.waiting as Cook.Wait.Him).card as Cook.Card.Day).landing
        val hand = card.lines.filter { it.where is Days.Where.Pile }.map { it.words }
        assertEquals(listOf("I'm going to finish the membership form"), hand, "a card only the piece holding the words at cook 9.9; never the moments together")
        val booked = card.lines.single { it.where is Days.Where.Booking }
        assertEquals("tomorrow I want to walk somewhere new", booked.words)
        assertEquals(Days.Where.Booking(today.plusDays(1)), booked.where)
        k.yes()
        assertEquals("tomorrow I want to walk somewhere new", Keep.dayText(clock(storage), today.plusDays(1)), "his words alone at tomorrow: no “What’s ahead.”")
        assertEquals(listOf("I'm going to finish the membership form"), hand(storage).values.toList())
    }

    @Test
    fun test3_heTakesACardOffHisHandHimselfAndHisHistoryKeepsANote() {
        val (storage, pages) = hatched()
        val ahead = Days.emptyAhead(h)
        (ahead["3"] as JsObj)["1"] = JsObj.of("_" to str("I need to finish the membership form"))
        (ahead["3"] as JsObj)["2"] = JsObj.of("_" to str("I need to reply to Jo"))
        (ahead["2"] as JsObj)["1"] = JsObj.of("_" to str("I'm going to do the ID checks"))
        check(pages.carryOut(pages.planWhole(Days.aheadOf(h), ahead)).ok)
        val before = history(storage).size
        assertNull(Days.takeOff(h, "3.1", pages, storage))
        assertEquals(mapOf("3.1" to "I need to reply to Jo", "2.1" to "I'm going to do the ID checks"), hand(storage), "the rest of the pile moves up; nothing else changes")
        assertEquals(before + 1, history(storage).size)
        assertEquals("On his yes: a card taken off his hand.", history(storage).last())
        assertEquals("that card isn't on your hand now", Days.takeOff(h, "3.5", pages, storage))
    }

    // ── 7: KEEP TODAY ──

    @Test
    fun test7_herPlacesForEachMomentAreOnTheCardAndOnlyUnplacedMomentsAreAsked() {
        val (storage, pages) = hatched()
        val w = World(storage, pages)
        val placed = Keep.Moment("I walked Haven this morning then spent an hour on the membership form", nowMs - 600_000, listOf(1, 2))
        val asked = Keep.Moment("everything feels too much this morning", nowMs - 300_000)
        val six = Keep.Moment("the movement fell a little stiff but free", nowMs - 200_000)
        w.moments += listOf(placed, asked, six)
        val k = Cook.Walk(cook, w, "").start("6.1")
        val many = k.waiting as Cook.Wait.HerMany
        assertEquals(listOf(asked, six), many.unplaced, "only the moments she hasn't placed")
        // Her free answer, read: a line for each moment asked; all six for one counts as none for it.
        val reply = "1: what he felt\n2: what happened, what he felt, what he made of it, what matters, a choice, what's ahead"
        val map = Placing.read(reply, many.unplaced)
        assertEquals(mapOf(asked.atMs to listOf(2), six.atMs to Mark.inOrder(listOf(1, 2, 3, 4, 5, 6))), map)
        k.herEach(map)
        k.sheet(Mark.Sheet("Haven, and a lot", listOf("I walked Haven.", "Everything felt too much."), ""))
        val card = ((k.waiting as Cook.Wait.Him).card as Cook.Card.Day).landing
        assertEquals(listOf(listOf(1, 2), listOf(2), emptyList()), card.moments.map { it.places }, "each moment's places on the card")
        assertEquals(listOf(1, 2), card.places, "the day's places: all of theirs")
        // He taps a colour beside a moment only to change it.
        val changed = card.placeMoment(2, 4)
        assertEquals(listOf(4), changed.moments[2].places)
        assertEquals(Mark.inOrder(listOf(1, 2, 4)), changed.places)
        // The places line is the last line, and only the places line.
        val text = changed.text!!
        assertEquals(Mark.placesWords(Mark.inOrder(listOf(1, 2, 4))).replaceFirstChar { it.uppercase() } + ".", text.lines().last())
        assertFalse(text.contains(Mark.OWN))
        // The places asked of her: numbered, his words alone.
        assertEquals("1. everything feels too much this morning\n2. the movement fell a little stiff but free", Placing.material(many.unplaced))
        assertTrue(Placing.instruction(many.question, many.places).contains("Write one line for each moment"))
    }

    @Test
    fun test7_noPlaceMeansNoPlacesLineAndNoLabelInHisWords() {
        val card = Days.landing(h, today, Mark.Sheet("A quiet day", listOf("I stayed in."), ""), emptyList(), listOf(Keep.Moment("I stayed in", nowMs)), emptyList(), emptyList(), now)
        assertEquals("A quiet day\nI stayed in.", card.text, "no “In your own words.”, no places line")
    }

    @Test
    fun test7_aCardOnHisHandAlreadyIsSkippedAndSaidSoAndOneTapLandsEverythingOnce() {
        val (storage, pages) = hatched()
        val ahead = Days.emptyAhead(h)
        (ahead["3"] as JsObj)["1"] = JsObj.of("_" to str("I'm going to finish the membership form"))
        check(pages.carryOut(pages.planWhole(Days.aheadOf(h), ahead)).ok)
        val moments = listOf(
            Keep.Moment("I'm going to finish the membership form", nowMs - 600_000, listOf(6)),
            Keep.Moment("I need to ring Jo about the music", nowMs - 300_000, listOf(6)),
        )
        fun card() = Days.landing(h, today, Mark.Sheet("The form, and Jo", listOf("I worked on the form."), ""), emptyList(), moments,
            listOf(Days.Intent(moments[0].words, 3, null, null), Days.Intent(moments[1].words, 3, null, null)), emptyList(), now, onHand = hand(storage).values)
        val first = card()
        assertTrue(first.lines[0].already && !first.lines[0].ticked, "on his hand already: said so, not ticked")
        assertFalse(first.lines[1].already)
        val historyBefore = history(storage).size
        val kept = CookActs.keepDay(first, h, pages, storage, nowMs)
        assertNull(kept.problem, "never a failure")
        assertEquals(listOf("I'm going to finish the membership form"), kept.already)
        assertEquals(listOf("I'm going to finish the membership form", "I need to ring Jo about the music"), hand(storage).values.toList(), "each card once")
        assertEquals(historyBefore + 1, history(storage).size, "one entry for the yes")
        assertTrue(history(storage).last().startsWith("On his yes: his day of"), history(storage).last())
        val clockOnce = stringify(clock(storage))
        // Kept again (moments still waiting, or Keep it tapped twice): nothing is written twice, and nothing fails.
        val again = CookActs.keepDay(card(), h, pages, storage, nowMs + 60_000)
        assertNull(again.problem)
        assertEquals(clockOnce, stringify(clock(storage)), "his now as it was: the day's line and each moment once")
        assertEquals(2, hand(storage).size)
        val dayNode = Keep.find(clock(storage), Keep.keys(Days.dayAddress(today), Keep.Grain.DAY))
        assertEquals(2, moments.count { m -> Keep.find(clock(storage), Keep.keys(Keep.grainAddress(m.atMs, Keep.Grain.MINUTE), Keep.Grain.MINUTE))?.let(Keep::lineOf) == m.words })
        assertNotNull(dayNode)
    }

    @Test
    fun test7_noKeepFailsWholeBecauseOneWriteIsRefused() {
        val (storage, pages) = hatched()
        val m = Keep.Moment("I need to ring Jo about the music", nowMs - 60_000, listOf(6))
        val card = Days.landing(h, today, Mark.Sheet("Jo", listOf("I thought about the music."), ""), emptyList(), listOf(m), listOf(Days.Intent(m.words, 3, null, null)), emptyList(), now)
        val kept = CookActs.keepDay(card, h, pages, storage, nowMs) { name, block ->
            if (name == Days.aheadOf(h)) "it was refused for the test" else pages.carryOut(pages.planWhole(name, block)).let { if (it.ok) null else it.problem }
        }
        assertEquals("${Days.aheadOf(h)}: it was refused for the test", kept.problem)
        assertEquals("Jo\nI thought about the music.\nWhat’s ahead.", Keep.dayText(clock(storage), today), "his now landed all the same")
        assertTrue(history(storage).last().endsWith("not all of it could be written."), history(storage).last())
    }

    @Test
    fun test7_hisOwnEditOfTheSheetStepIsAccepted() {
        val (storage, pages) = hatched()
        val sheet = shipped.getValue("6.1.5")
        val inside = Regex("“(.+?)” On E2B").find(sheet)!!.groupValues[1]
        val his = inside.replace("in the second person", "in the first person, as I would say it, the whole day and not one thread")
            .replace("he is keeping today", "I am keeping today")
        for (variant in listOf(
            sheet.replace(inside, his),
            // His Write screen's straight quotes, a line break inside, curly apostrophes, the full stop left off.
            sheet.replace("“$inside”", "\"$his\"").replace("I am keeping today. ", "I am keeping today.\n"),
            sheet.replace(inside, his.replace("'", "’")).removeSuffix("."),
            sheet.replace(inside, his).replace("  ", " ").replace(", with this line:", ",  with this line:"),
        )) {
            val mine = shippedCook
            ((mine["6"] as JsObj)["1"] as JsObj)["5"] = str(variant)
            check(pages.carryOut(pages.planWhole(Cook.cookOf(h), mine)).ok)
            val loaded = Cook.load(h, storage, procedures)
            assertTrue(loaded.own && loaded.refusal == null, "${loaded.refusal}\n$variant")
            val w = World(storage, pages)
            w.moments += Keep.Moment("I walked Haven this morning", nowMs, listOf(1))
            val k = Cook.Walk(loaded.parsed, w, "").start("6.1")
            val asked = k.waiting as Cook.Wait.HerSheet
            assertTrue(asked.line.contains("in the first person, as I would say it"), asked.line)
        }
    }

    // ── 8: TOMORROW ──

    @Test
    fun test8_hisTomorrowLineIsReadNextMorningForHerOneQuestion() {
        val (storage, pages) = hatched()
        val m = Keep.Moment("Tomorrow I want to walk somewhere new", nowMs, listOf(6))
        val card = Days.landing(h, today, Mark.Sheet("A walk ahead", listOf("I want to walk somewhere new."), "walk somewhere new"), emptyList(), listOf(m),
            listOf(Days.Intent(m.words, null, today.plusDays(1), "tomorrow")), emptyList(), now)
        assertNull(CookActs.keepDay(card, h, pages, storage, nowMs).problem)
        val next = now.plusDays(1).withHour(7)
        val k = Cook.Walk(cook, World(storage, pages, now = next), "").start("6.3")
        assertEquals(listOf("Yesterday he wrote this line for today: “walk somewhere new.”"), k.handed.single().material)
        assertEquals("walk somewhere new", Keep.tomorrowOf(Keep.lastDay(Keep.days(clock(storage), null, next.toLocalDate()), next.toLocalDate())))
    }

    // ── 9: THE RECORD, THE HAND, THE SPEED ──

    @Test
    fun test9_nothingIsWrittenUntilHisYesAndEveryYesHasItsEntry() {
        val (storage, pages) = hatched()
        val w = World(storage, pages)
        w.moments += Keep.Moment("I'm going to finish the membership form", nowMs - 60_000, listOf(6))
        val before = storage.names().associateWith { storage.read(it) }
        // His messages walked, her places asked, the card set out: nothing written.
        Cook.Walk(cook, World(storage, pages, hisHand), "I walked Haven this morning then spent an hour on the membership form").start("1")
        val k = Cook.Walk(cook, w, "").start("6.1")
        k.sheet(Mark.Sheet("The form", listOf("I'm going to finish the form."), ""))
        assertTrue((k.waiting as Cook.Wait.Him).card is Cook.Card.Day)
        assertEquals(before, storage.names().associateWith { storage.read(it) }, "nothing written before his yes")
        k.yes()
        assertTrue(history(storage).last().startsWith("On his yes: his day of"), "the keep's own entry")
        val n = history(storage).size
        assertNull(CookActs.carryOut(Cook.Card.Whisper(2, "It felt like a lot.", "What did you feel?"), h, pages, storage, shelf, nowMs))
        assertEquals(n + 1, history(storage).size, "a whisper's own entry")
        assertEquals("On his yes: a whisper kept at what you felt.", history(storage).last())
    }

    @Test
    fun test9_anyWordingInsideTheQuotesOfEveryStepThatHandsHerALineIsAccepted() {
        val places = shipped.filterValues { "“" in it && "”" in it }
        assertTrue(places.size >= 10, "every step that hands her a line: ${places.size}")
        for ((at, step) in places) {
            val quoted = Regex("“([^”]+)”").findAll(step).map { it.groupValues[1] }.maxByOrNull { it.length }!!
            if (quoted.length < 40) continue // a phrase the app reads (“Tomorrow:”, “and I”), not a line handed to her
            val his = "Whatever I write here is mine, in my own way: it's said as I'd say it.\nEven on a second line, with \"quotes\" of my own"
            val edited = step.replace(quoted, his)
            assertNotNull(Cook.read(edited), "cook $at with his wording inside its quotes:\n$edited")
        }
    }

    @Test
    fun test9_aStepThatCantBeReadSaysWhichPartInPlainWords() {
        val (storage, pages) = hatched()
        val mine = shippedCook
        val sheet = shipped.getValue("6.1.5")
        ((mine["6"] as JsObj)["1"] as JsObj)["5"] = str(sheet.replace("She draws the day's sheet", "She writes the day's sheet"))
        check(pages.carryOut(pages.planWhole(Cook.cookOf(h), mine)).ok)
        val loaded = Cook.load(h, storage, procedures)
        assertFalse(loaded.own)
        val said = loaded.refusal!!
        assertTrue(said.startsWith("Your cook couldn't be read at 6.1.5 (SHEET): outside its quotes it reads “writes the day's sheet"), said)
        assertTrue(said.contains("where the app's form reads “draws the day's sheet"), said)
        assertTrue(said.contains("the words inside its quotes are yours to change"), said)
        // A quote mark left open is said so.
        val open = sheet.replace("Never advise, interpret, diagnose, praise or cheer. Never use the words block, pool, passport, handle, beach, pscale, Being, Facts, Feelings, Fictions, Purpose, Decision or Outcomes.”", "")
        assertTrue(Cook.why(open, sheet).contains("never closes"), Cook.why(open, sheet))
    }

    @Test
    fun test9_hisCookFrom0102IsBroughtUpAndNothingComesOverHisEditedLines() {
        val (storage, pages) = hatched()
        val old = parseJson(javaClass.getResource("/cook-0.10.2.json")!!.readText()) as JsObj
        val talk = "Hand her this line: “The app: just talk with me, warmly; when I'm struggling, ask whether I'd like to sit with it a moment. Speak to him as you, never by his name.”"
        val noDay = "Hand her this line: “The app: this message is not news of his day. If he seems to be struggling, ask gently whether he'd like to sit with it a moment; else talk with him. Speak to him as you, never by his name.”"
        val sheet = Cook.places(old).getValue("6.1.5").replace("in the second person", "in the first person")
        (jsGetPath(old, "1", "1", "8") as JsObj)["1"] = str(talk)
        (jsGetPath(old, "1", "1", "8", "3", "2") as JsObj)["1"] = str(noDay)
        ((old["6"] as JsObj)["1"] as JsObj)["5"] = str(sheet)
        check(pages.carryOut(pages.planWhole(Cook.cookOf(h), old)).ok)
        val ups = Cook.bringUp(h, pages, storage, procedures, { name -> if (name == Cook.SHIPPED_COOK) javaClass.getResource("/cook-0.10.2.json")!!.readText() else stringify(procedures.load(name)) })
        for ((_, plan) in ups) plan?.let { check(pages.carryOut(it).ok) }
        val now = Cook.places(parseJson(storage.read(Cook.cookOf(h))!!))
        assertEquals(talk, now["1.1.8.1"], "his TALK line")
        assertEquals(noDay, now["1.1.8.3.2.1"], "his no-day line")
        assertEquals(sheet, now["6.1.5"], "his SHEET step")
        for (at in listOf("1.1.8.3", "6.1.6.1", "6.1.6.2")) assertEquals(shipped[at], now[at], "cook $at brought up")
        assertTrue(ups.flatMap { it.first.yours }.none { it.offer }, "nothing offered over his edited lines")
        val loaded = Cook.load(h, storage, procedures)
        assertTrue(loaded.own && loaded.refusal == null, loaded.refusal ?: "")
    }

    private fun jsGetPath(v: Js?, vararg keys: String): Js? = keys.fold(v) { n, k -> jsGet(n, k) }
}
