package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The blocks his words name, and what his words ask of the gate. */
class TalkingTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))

    @Test
    fun aBlockWhoseLinesSheCantSeeIsReadForHer() {
        val (_, pages) = Bench.fixture(shelf, 1_793_178_000_000L)
        assertEquals(listOf("whetstone"), Talking.named("What does the whetstone say?", pages), "a teaching block")
        assertEquals(emptyList(), Talking.named("What's in my garden block?", pages).filter { pages.position(Region.POCKET, it)?.lines.orEmpty().size <= 9 }, "a short block of his is in her instruction already")
        assertEquals(emptyList(), Talking.named("Nothing named here.", pages))
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
