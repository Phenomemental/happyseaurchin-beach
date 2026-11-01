package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 0.8: golden cases for every hatch write (spine:pocket-beach 1.1111787135).
 * The calls the phone's hatch makes, exactly as its Pages plan them, are
 * kept in conformance/hatch-case.json; conformance/corpus.mjs puts them to
 * the pinned handler, and GoldenTest holds the core to every reply and every
 * block it leaves. This test holds that file to what the hatch makes now:
 * run with HATCH_WRITE=1 to write it again, then conformance/generate.sh.
 *
 * Two cases: an adopted pocket whose companion is named (the four blocks
 * whole, "Keep that" appended to his stash, the close's history entry, a
 * new name changed at her place), and a pocket hatched with her unnamed (her
 * name added at her place).
 */
class HatchGoldenTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val now = ZonedDateTime.of(2026, 11, 1, 8, 40, 0, 0, ZoneId.of("Europe/London"))
    private val nowMs = now.toInstant().toEpochMilli()
    private val h = Hatch.MATTHEW_HANDLE
    private val file = File("../conformance/hatch-case.json")

    private val passport = JsObj.of(
        "_" to str("Phenomemental — Matthew, his passport as the beach holds it (a stand-in for the golden case)."),
        "1" to str("His heart, in his own words."),
        "2" to JsObj.of("_" to str("What he needs."), "1" to str("Collaborators for the commons.")),
    )

    private fun case(name: String, named: Boolean): JsObj {
        val (storage, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        if (named) check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
        HerWindow.ensure(pages)
        val seed = JsArr(storage.names().sorted().map<String, Js?> { n -> JsObj.of("block" to str(n), "content" to parseJson(storage.read(n)!!)) }.toMutableList())
        val calls = mutableListOf<Js?>()
        fun go(plan: Plan) {
            assertTrue(plan.refusal == null, "${plan.block}: ${plan.refusal}")
            calls += parseJson(stringify(plan.call!!)!!)
            assertTrue(pages.carryOut(plan).ok, plan.block)
        }
        fun proposed(action: Acts.Action) = Acts.propose("", listOf(action), storage, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), nowMs).proposal!!.writes.single().plan
        val planned = Hatch.plan(h, passport, Hatch.MATTHEW, pages, storage, nowMs, "Adopted on this phone from passport-$h.json, read at ${isoString(nowMs)}.")
        planned.plans.forEach(::go)
        if (named) {
            go(proposed(Acts.Action.Add(Hatch.stashOf(h), null, "I've been making huge progress with my film making and am eagerly awaiting my first pilot to be rendered.")))
            go(Close.plan(Close.Talk(listOf("Hi Gemma I've been making huge progress with my film making.", "Keep that for me please"), listOf("her window"), listOf("stash:$h: one line")), pages, storage)!!)
            go(proposed(Hatch.nameAction(h, pages, "Robin")))
        } else {
            go(proposed(Hatch.nameAction(h, pages, "Gemma")))
        }
        for (b in Hatch.fourOf(h)) calls += JsObj.of("agent_id" to str("pocket"), "block" to str(b))
        calls += JsObj.of("agent_id" to str("pocket"), "block" to str(Hatch.shellOf(h)), "spindle" to str(Hatch.WINDOW))
        return JsObj.of("name" to str(name), "seed" to seed, "calls" to JsArr(calls))
    }

    /** 0.8.1: a shell 0.8 hatched, corrected: its opening said truly, his third line as he changed it. */
    private fun corrected(): JsObj {
        val (storage, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        check(Hatch.carryOut(Hatch.plan(h, passport, Hatch.MATTHEW.copy(met = Hatch.MATTHEW_MET_08), pages, storage, nowMs, "Adopted on this phone."), pages, storage).problem == null)
        val opening = collectUnderscore(parseJson(storage.read(Hatch.shellOf(h))!!))!!
        check(pages.carryOut(pages.planChange(Hatch.shellOf(h), "", opening.replace(Hatch.UNLOCKED_LINE, Hatch.lockedLine(h)))).ok)
        val seed = JsArr(storage.names().sorted().map<String, Js?> { n -> JsObj.of("block" to str(n), "content" to parseJson(storage.read(n)!!)) }.toMutableList())
        val calls = mutableListOf<Js?>()
        for (plan in Hatch.corrections(pages, storage)) {
            calls += parseJson(stringify(plan.call!!)!!)
            assertTrue(pages.carryOut(plan).ok)
        }
        assertTrue(calls.size == 2)
        calls += JsObj.of("agent_id" to str("pocket"), "block" to str(Hatch.shellOf(h)))
        return JsObj.of("name" to str("the hatch of 0.8, corrected"), "seed" to seed, "calls" to JsArr(calls))
    }

    /**
     * Rockpool: a pocket 0.8.1 left, brought up to this release. His cook and
     * kernel brought in whole; his days moved out of telling (his now with
     * the six taken out, his ahead, his P6); her window pointed at his ahead;
     * then a day told and landed, and a keeping the walker set out.
     */
    private fun rockpool(): JsObj {
        val procedures = Shelf(DirShelfSource(File(System.getProperty("pocket.procedures") ?: "../assets/procedures")))
        val (storage, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
        check(Hatch.carryOut(Hatch.plan(h, passport, Hatch.MATTHEW, pages, storage, nowMs, "Adopted on this phone."), pages, storage).problem == null)
        check(pages.carryOut(pages.planChange(Hatch.shellOf(h), "${Hatch.WINDOW}.5", Hatch.OLD_HAND)).ok)
        val old = Telling.shape()
        (old["2"] as JsObj)["2"] = JsObj.of("_" to str("This week"), "1" to str("Cut the second film"))
        check(pages.carryOut(pages.planWhole(Landing.TELLING, old)).ok)
        check(pages.carryOut(pages.planWhole(Landing.NOW, Landing.withDay(null, Landing.dayAddress(nowMs), "Walked the dog with Sam.", mapOf("1.1" to listOf("Walked the dog with Sam."), "1.2" to listOf("Calm"))).first)).ok)
        val seed = JsArr(storage.names().sorted().map<String, Js?> { n -> JsObj.of("block" to str(n), "content" to parseJson(storage.read(n)!!)) }.toMutableList())
        val calls = mutableListOf<Js?>()
        fun go(plan: Plan) {
            assertTrue(plan.refusal == null, "${plan.block}: ${plan.refusal}")
            calls += parseJson(stringify(plan.call!!)!!)
            assertTrue(pages.carryOut(plan).ok, plan.block)
        }
        // As the app does it at opening: his shell put right, his cook and kernel brought in and listed, his days moved and listed.
        Hatch.corrections(pages, storage).forEach(::go)
        Cook.bringIn(h, pages, storage, procedures).forEach(::go)
        Hatch.listInManifest(h, listOf(Cook.cookOf(h), Cook.kernelOf(h)), pages) { go(it); true }
        val moved = Days.moveOut(h, storage, nowMs)!!
        listOfNotNull(moved.now?.let { Days.NOW to it }, moved.ahead?.let { Days.aheadOf(h) to it }, moved.p6?.let { Days.p6Of(h) to it }).forEach { (n, b) -> go(pages.planWhole(n, b)) }
        Hatch.listInManifest(h, listOf(Days.aheadOf(h), Days.p6Of(h)), pages) { go(it); true }
        val day = Days.landing(h, "Rang Jo about the music. I need to send the pilot this week.", listOf("Rang Jo about the music.", "I need to send the pilot this week."), listOf("1.1", "2.2"),
            Days.intents(listOf("I need to send the pilot this week."), listOf("i need to"), now.toLocalDate(), null), emptyList(), now)
        Days.blocks(day, { n -> storage.read(n)?.let(::parseJson) }, nowMs).blocks.forEach { (n, b) -> go(pages.planWhole(n, b)) }
        go(Acts.propose("", CookActs.actions(Cook.Card.Keep(Hatch.stashOf(h), "Jo's doing the music for the pilot."), h, pages), storage, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), nowMs).proposal!!.writes.single().plan)
        for (b in listOf(Cook.cookOf(h), Cook.kernelOf(h), Days.NOW, Days.aheadOf(h), Days.p6Of(h), Hatch.stashOf(h))) calls += JsObj.of("agent_id" to str("pocket"), "block" to str(b))
        calls += JsObj.of("agent_id" to str("pocket"), "block" to str(Hatch.shellOf(h)), "spindle" to str(Hatch.WINDOW))
        return JsObj.of("name" to str("rockpool: his cook brought in, his days moved and told"), "seed" to seed, "calls" to JsArr(calls))
    }

    @Test
    fun theHatchCasesAreWhatTheHatchMakes() {
        val made = stringifyPretty(JsArr(mutableListOf<Js?>(case("the hatch, adopted, her name changed", true), case("the hatch, her name added", false), corrected(), rockpool()))) + "\n"
        if (System.getenv("HATCH_WRITE") == "1") file.writeText(made)
        assertTrue(file.isFile, "no $file: run with HATCH_WRITE=1, then conformance/generate.sh")
        assertEquals(file.readText(), made, "conformance/hatch-case.json lags the hatch: run with HATCH_WRITE=1, then conformance/generate.sh")
    }
}
