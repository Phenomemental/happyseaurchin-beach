package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 0.5's Stage 2: her talk, its short instruction, and every turn with the date, the time and her shell. */
class TalkingTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val now = ZonedDateTime.of(2026, 10, 29, 10, 15, 0, 0, ZoneId.of("Europe/London"))

    @Test
    fun herInstructionIsShortAndHasNoRouteWord() {
        val (_, pages) = Bench.fixture(shelf, 1_793_178_000_000L)
        val i = Talking.instruction(HelloWorld.SHELL, pages)
        assertContains(i, "garden", message = "the pocket's blocks are listed")
        assertContains(i, "nothing is written until he says yes")
        for (rule in listOf("talk:", "read:", "act:", "add <block>", Acts.GRAMMAR.lines().first())) assertFalse(i.contains(rule), "no rulebook: $rule")
        val full = Talks.instruction(HelloWorld.SHELL, "", Asks.plainWindow(pages, HelloWorld.SHELL), pages)
        assertTrue(i.length < full.length * 2 / 3, "shorter than 0.4's: ${i.length} against ${full.length}")
    }

    @Test
    fun everyTurnCarriesTheDateTheTimeAndHerShell() {
        val (_, pages) = Bench.fixture(shelf, 1_793_178_000_000L)
        val t = Talking.turn("How's the garden doing?", now, HelloWorld.SHELL, pages)
        assertContains(t, "It is Thursday 29 October 2026, 10:15.")
        assertContains(t, "Your shell, ${HelloWorld.SHELL}: \"${HelloWorld.SHELL_OPENING}\"")
        assertFalse(t.contains("Hello world:"), "the hello world line is the writing job's")
        assertTrue(t.endsWith("Matthew: How's the garden doing?"))
        val told = Talking.turn("And now?", now, HelloWorld.SHELL, pages, before = "Matthew said yes; garden was written at 4.")
        assertContains(told, "(Matthew said yes; garden was written at 4.)")
    }

    @Test
    fun aBlockWhoseLinesSheCantSeeIsReadForHer() {
        val (_, pages) = Bench.fixture(shelf, 1_793_178_000_000L)
        assertEquals(listOf("whetstone"), Talking.named("What does the whetstone say?", pages), "a teaching block")
        assertEquals(emptyList(), Talking.named("What's in my garden block?", pages).filter { pages.position(Region.POCKET, it)?.lines.orEmpty().size <= 9 }, "a short block of his is in her instruction already")
        assertEquals(emptyList(), Talking.named("Nothing named here.", pages))
        val t = Talking.turn("What does the whetstone say?", now, HelloWorld.SHELL, pages)
        assertContains(t, "What the app read for you")
        assertContains(t, "the teaching block whetstone")
    }

    @Test
    fun theWritingJobRunsOnlyWhenHisWordsAsk() {
        // 0.6.4: something told is not an ask to keep it.
        for (asks in listOf("Add to shopping: oat milk.", "Can you add oat milk to shopping?", "Do the hello world. My secret is purple walrus.", "Remember that Jo's birthday is on the twelfth.")) {
            assertTrue(Keeping.asks(asks).isNotEmpty(), asks)
        }
        for (talk in listOf("Hello!", "Thanks!", "Good morning", "What's your name?", "How are you today?", "What does the whetstone say?", "",
            "The shopping is on Saturdays from now on.", "I want to run a marathon next spring.")) {
            assertTrue(Keeping.asks(talk).isEmpty(), talk)
        }
    }
}
