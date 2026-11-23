package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 0.8.1, the small release after 0.8 (spine:pocket-beach 1.1111787136):
 * the shell's opening said truly; "my passport" named as his; a post heard
 * however he asks for it, his own words lifted by code and set out with no
 * model call.
 */
class SmallReleaseTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val now = ZonedDateTime.of(2026, 11, 1, 17, 30, 0, 0, ZoneId.of("Europe/London"))
    private val nowMs = now.toInstant().toEpochMilli()
    private val h = Hatch.MATTHEW_HANDLE

    /** A passport as long as his: eight places, the heart first, each well over a thousand characters. */
    private val longPassport: JsObj = JsObj.of(
        "_" to str("Matthew Bushell — digital social innovator, aspiring writer, therapeutic coach."),
        "1" to str("The heart. bi-polar · one axis, two ends. " + "whole-less, as in boundless. ".repeat(60)),
        "2" to str("The other side of the &: people who will meet me in the middle and build with, not at. " + "LEROs and CLERO. ".repeat(80)),
        "3" to str("Meet me in the pool: leave a note in my pool on the beach. " + "I'll see it when I next open. ".repeat(40)),
        "5" to str("IF YOU ARE AN AI WORKING WITH ME — you're here to help me do with my information what must be done. " + "Assistance in role. ".repeat(60)),
        "6" to JsObj.of("_" to str("WHERE THINGS GO — for an AI working with me. Confirmed with me in conversation."), "1" to str("Films go in films."), "2" to str("Ideas go in my stash.")),
        "8" to str("WRITING, EVERY TIME — ask me before placing, check where things go, write, then read it back. " + "Read it back. ".repeat(80)),
    )

    private fun hatched(passport: JsObj = longPassport): Pair<MemoryStorage, Pages> {
        val (storage, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
        val done = Hatch.carryOut(Hatch.plan(h, passport, pages, storage, nowMs, "Adopted on this phone from passport-$h.json."), pages, storage)
        check(done.problem == null) { done.problem!! }
        return storage to pages
    }

    // ── (b): the shell's opening said truly ──

    @Test
    fun theShellSaysTrulyItIsNotLocked() {
        val (storage, _) = hatched()
        val opening = collectUnderscore(parseJson(storage.read("shell:$h")!!))!!
        assertFalse(opening.contains("Locked with"), opening)
        assertTrue(opening.endsWith(Hatch.UNLOCKED_LINE), opening)
        assertTrue(opening.startsWith("Phenomemental's shell at the pocket on this phone."), "the beach address stays, since it is true")
    }

    @Test
    fun aShellHatchedBy08IsCorrected() {
        val (storage, pages) = hatched()
        // As 0.8 left it: the template's lock in its opening. (Sundial 0.12: his third line is no longer corrected; his three lines are gone.)
        check(pages.carryOut(pages.planChange("shell:$h", "", collectUnderscore(parseJson(storage.read("shell:$h")!!))!!.replace(Hatch.UNLOCKED_LINE, Hatch.lockedLine(h)))).ok)
        val plans = Hatch.corrections(pages, storage)
        assertEquals(1, plans.size)
        plans.forEach { assertTrue(pages.carryOut(it).ok, it.refusal) }
        val shell = parseJson(storage.read("shell:$h")!!) as JsObj
        assertTrue(collectUnderscore(shell)!!.endsWith(Hatch.UNLOCKED_LINE))
        assertEquals(emptyList(), Hatch.corrections(pages, storage), "once corrected, nothing more")
    }

    // ── (d): "Read my passport" ──

    @Test
    fun readMyPassportNamesHisOwn() {
        val (storage, _) = hatched()
        assertEquals(listOf("passport:$h"), HerWindow.hisFour("Read my passport", storage))
    }

    // ── (e) and (f): posts ──

    @Test
    fun thePostIsHeardHoweverHeAsksForIt() {
        for (said in listOf("Send a post to my room.", "Send a message to my room", "Tell my room I'm back on Tuesday", "Post this message to my room: \"Hello all\"", "Can you post a message to my room on the beach?", "Leave a note in my pool: back soon"))
            assertTrue(Keeping.Ask.POST in Keeping.asks(said), said)
        for (said in listOf("I've been making huge progress with my film making.", "Keep that for me please", "What's in my room on the beach?"))
            assertFalse(Keeping.Ask.POST in Keeping.asks(said), said)
    }

    @Test
    fun hisOwnWordsAreLiftedExactlyByCode() {
        assertEquals("I've had a relaxing weekend, a visit from my para-trooper nephew, halloween parties for the girls…",
            Keeping.postWords("Post this message to my room: \"I've had a relaxing weekend, a visit from my para-trooper nephew, halloween parties for the girls…\""))
        assertEquals("back soon", Keeping.postWords("Leave a note in my pool: back soon"))
        assertEquals("Hello all", Keeping.postWords("Can you send “Hello all” to my room?"))
        assertNull(Keeping.postWords("Send a post to my room."))
        assertNull(Keeping.postWords("Can you post a message to my room on the beach?"))
    }

    @Test
    fun aPostOfHisWordsIsSetOutWithNoModelCall() {
        val (storage, pages) = hatched()
        val said = "Post this message to my room: \"I've had a relaxing weekend.\""
        val way = Keeping.way(said, emptyList(), pages, Hatch.shellOf(h), Hatch.HER)
        assertTrue(way is Keeping.Way.Post && way.words == "I've had a relaxing weekend.", "${way::class.simpleName}")
        // The app's own proposal, by code: no Write job, no model.
        val planned = Bench.byApp(way, storage, shelf, nowMs)!!
        assertEquals("I've had a relaxing weekend.", planned.proposal!!.post!!.message)
    }

    @Test
    fun aPostAskedWithNoWordsOfHisLiftsNone() {
        val (_, pages) = hatched()
        val way = Keeping.way("Send a post to my room.", emptyList(), pages, Hatch.shellOf(h), Hatch.HER)
        assertTrue(way is Keeping.Way.Post && way.words == null)
    }
}
