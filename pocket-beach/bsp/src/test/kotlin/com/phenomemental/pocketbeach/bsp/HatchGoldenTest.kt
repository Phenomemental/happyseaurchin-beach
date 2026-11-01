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

    @Test
    fun theHatchCasesAreWhatTheHatchMakes() {
        val made = stringifyPretty(JsArr(mutableListOf<Js?>(case("the hatch, adopted, her name changed", true), case("the hatch, her name added", false), corrected()))) + "\n"
        if (System.getenv("HATCH_WRITE") == "1") file.writeText(made)
        assertTrue(file.isFile, "no $file: run with HATCH_WRITE=1, then conformance/generate.sh")
        assertEquals(file.readText(), made, "conformance/hatch-case.json lags the hatch: run with HATCH_WRITE=1, then conformance/generate.sh")
    }
}
