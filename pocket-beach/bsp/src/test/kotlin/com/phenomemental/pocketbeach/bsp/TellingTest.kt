package com.phenomemental.pocketbeach.bsp

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TellingTest {
    /** The answer the key asks for, by hand: the test of the key itself against his written telling. */
    private val right = """
        1.1: the car wouldn't start again this morning
        1.1: which meant the bus
        1.1: I was twenty minutes late and walked into that meeting with everyone already looking at me
        1.1: The meeting went on forever but Sam said something that stuck with me
        1.1: Walked home the long way by the water
        1.2: Felt rotten about the morning, that hot-faced thing
        1.2: felt lighter by the end of it
        1.3: I've been putting that car off for weeks and I know it, same as the tax, same as the garden
        1.3: that I'm always the one apologising
        1.4: The thing I keep coming back to is the kids seeing me calm, not this
        1.5: I've decided I'm ringing the garage tomorrow, not fiddling with it myself again
        1.6: Jo's birthday is on the twelfth so I need to think about that at some point
        1.6: Dentist on Thursday, I keep forgetting
        1.6: I'm ringing the garage tomorrow
        1.6: Before the frost comes I want the garden shut down properly for once
        1.6: One day I want a job where mornings aren't a fight
        2.1: I'm ringing the garage tomorrow
        2.2: Dentist on Thursday
        2.3: Jo's birthday is on the twelfth
        2.4: Before the frost comes I want the garden shut down properly for once
        2.5: One day I want a job where mornings aren't a fight
        3.1: I ring the garage and get to the dentist.
        3.2: I mark Jo's birthday.
        3.3: I shut the garden down before the frost.
        3.4: I keep the kids seeing me calm.
    """.trimIndent()

    @Test
    fun theShapeIsHisBlockWithItsThreeLandings() {
        val shape = Telling.shape()
        assertContains(collectUnderscore(shape)!!, "One telling, three landings")
        assertEquals("What happened: what could be seen or checked, before any meaning", ((shape["1"] as JsObj)["1"] as JsStr).value)
        assertEquals("This year or beyond", ((shape["2"] as JsObj)["5"] as JsStr).value)
        assertEquals(6 + 5 + 4, Telling.DAY.size + Telling.HAND.size + Telling.RUNGS.size)
        val asked = Telling.ask(shape, Telling.BENCH_DATE, Telling.WRITTEN)
        assertTrue(asked.startsWith("Today is Thursday 29 October 2026."))
        assertContains(asked, "\"6\": \"What's ahead: what's coming, planned or expected\"")
        assertTrue(asked.endsWith("mornings aren't a fight."))
    }

    @Test
    fun herLinesAreReadByTheirAddresses() {
        val placed = Telling.placed("Here it is:\n- **1.1**: the bus\n1.2 - Felt rotten\n2.1.1: \"Rang the garage\"\n2.3: Jo's birthday\n4.1: nowhere\n1.7: nowhere\n3.1: None\nNot a line")
        assertEquals(
            listOf(Telling.Placed("1.1", null, "the bus"), Telling.Placed("1.2", null, "Felt rotten"), Telling.Placed("2.1", "1", "Rang the garage"), Telling.Placed("2.3", null, "Jo's birthday")),
            placed,
        )
    }

    @Test
    fun theKeysOwnAnswerMeetsEveryMark() {
        val marks = Telling.marks(Telling.WRITTEN, Telling.placed(right))
        for (m in marks) assertTrue(m.met, "${m.name}: ${m.note}")
        assertEquals(11, Telling.sentences(Telling.WRITTEN).size)
    }

    @Test
    fun wordsNotHisAreRefusedAndSentencesLeftOutAreListed() {
        val checked = Telling.check(Telling.WRITTEN, Telling.placed("1.1: The car wouldn't START again this morning!\n1.2: I felt embarrassed\n2.1: ring the garage\n3.1: I'll sort the car."))
        assertEquals(listOf("I felt embarrassed", "ring the garage"), checked.invented.map { it.words })
        // A rung is hers to word, in his voice; the day and the hand are his words.
        assertEquals(listOf("1.1", "3.1"), checked.kept.map { it.place })
        assertEquals(10, checked.dropped.size)
        assertFalse(checked.dropped.any { it.startsWith("Right, so the car") })
        val marks = Telling.marks(Telling.WRITTEN, Telling.placed("1.1: the bus\n3.1: You ring the garage."))
        assertFalse(marks.single { it.name == "Every line in the first person" }.met)
    }

    @Test
    fun aCardOnTheHandIsUpdatedNotDoubled() {
        val first = Telling.check(Telling.WRITTEN, Telling.placed(right))
        val landed = Telling.land(Telling.shape(), first.kept)
        assertEquals("I'm ringing the garage tomorrow", Telling.cards(landed)["2.1.1"])
        assertEquals(5, Telling.cards(landed).size)
        assertEquals("I ring the garage and get to the dentist.", Telling.rungs(landed)["3.1.1"])
        // The same words again, on another shelf: not doubled.
        assertEquals(5, Telling.cards(Telling.land(landed, listOf(Telling.Placed("2.2", null, "I'm ringing the garage tomorrow")))).size)
        // The second telling names the card at its own address: updated, the rungs refreshed.
        val second = Telling.check(Telling.SECOND, Telling.placed("1.1: Rang the garage first thing\n2.1.1: Rang the garage first thing, they're coming Monday\n3.1: I get the car fixed on Monday."))
        val after = Telling.land(landed, second.kept)
        assertEquals("Rang the garage first thing, they're coming Monday", Telling.cards(after)["2.1.1"])
        assertEquals("I get the car fixed on Monday.", Telling.rungs(after)["3.1.1"])
        for (m in Telling.secondMarks(after)) assertTrue(m.met, "${m.name}: ${m.note}")
        // A second card for the garage is what the mark is for.
        val doubled = Telling.land(landed, Telling.placed("2.2: Rang the garage first thing, they're coming Monday"))
        assertFalse(Telling.secondMarks(doubled).single { it.name == "One garage card" }.met)
    }

    @Test
    fun thePlainAnswerIsReadUnderItsHeadings() {
        val plain = """
            Here's your day sorted:

            **The Day**
            * **What happened:** The car wouldn't start, so you took the bus.
            * **What I felt:**
              * Rotten, hot-faced
            ### What I made of it
            - I've been putting that car off for weeks

            **The Hand**
            * **Tomorrow:** Ring the garage
            * **This week:** Dentist on Thursday

            **The Rungs Above**
            * **This week:** I get the car sorted.
        """.trimIndent()
        val placed = Telling.fromPlain(plain)
        assertEquals(
            listOf("1.1", "1.2", "1.3", "2.1", "2.2", "3.1"),
            placed.map { it.place },
        )
        assertEquals("Rotten, hot-faced", placed[1].words)
        assertTrue(Telling.plainAsk(Telling.WRITTEN).startsWith("The day:\n- What happened\n"))
    }

    @Test
    fun thePhoneCutsWhatHeSaidIntoPiecesOfHisOwnWords() {
        val pieces = Telling.cut(Telling.WRITTEN)
        println(pieces.joinToString("\n"))
        assertTrue(pieces.all { Telling.foundIn(it, Telling.WRITTEN) }, "every piece his words")
        assertEquals(
            listOf("the car wouldn't start again this morning", "which meant the bus", "which meant I was twenty minutes late", "walked into that meeting with everyone already looking at me"),
            pieces.take(4),
        )
        assertTrue("Jo's birthday is on the twelfth" in pieces)
        assertTrue("I've decided I'm ringing the garage tomorrow" in pieces)
        assertTrue("that I'm always the one apologising" in pieces)
        assertEquals(listOf("Rang the garage first thing", "they're coming Monday"), Telling.cut(Telling.SECOND))
        // Every sentence has a piece in it, so labelled in the day, nothing is dropped.
        assertTrue(Telling.sentences(Telling.WRITTEN).all { s -> pieces.any { Telling.foundIn(it, s) } })
    }

    @Test
    fun herChoicesForAPieceAreItsPlacesAndPiecesOnOneCardBecomeOne() {
        assertEquals(listOf("1.1" to null, "1.6" to null, "2.1" to null), Telling.places("1.1, 1.6 and 2.1"))
        assertEquals(listOf("2.1" to "1"), Telling.places("2.1.1"))
        assertEquals(emptyList(), Telling.places("none"))
        val placed = Telling.fromPieces(
            Telling.SECOND,
            listOf("Rang the garage first thing" to listOf("1.1" to null, "2.1" to "1"), "they're coming Monday" to listOf("1.6" to null, "2.1" to "1")),
            "3.1: I get the car back on the road.",
        )
        assertTrue(Telling.Placed("2.1", "1", "Rang the garage first thing, they're coming Monday") in placed)
        assertTrue(Telling.Placed("3.1", null, "I get the car back on the road.") in placed)
        assertEquals(listOf("1.1", "1.6"), placed.filter { it.place in Telling.DAY }.map { it.place })
    }

    @Test
    fun theBlockGivenBackFilledIsReadAndItsOwnWordsPassedOver() {
        // The block's own words given back are neither his nor invented.
        assertEquals(emptyList(), Telling.placed("1.1: What happened: what could be seen or checked, before any meaning\n2.1: Tomorrow\n3.4: This year"))
        val strict = Telling.placed("Here:\n{\"1\": {\"1\": {\"_\": \"What happened: what could be seen or checked, before any meaning\", \"1\": \"which meant the bus\"}}, \"2\": {\"1\": {\"_\": \"Tomorrow\", \"1\": \"I'm ringing the garage tomorrow\"}}}")
        assertEquals(listOf(Telling.Placed("1.1", null, "which meant the bus"), Telling.Placed("2.1", "1", "I'm ringing the garage tomorrow")), strict)
        // As she wrote it sampled on the laptop: not quite JSON.
        val loose = Telling.placed(
            """
            Here is the block filled with Matthew's words:

            {
             "1": {
              "1": "What happened: what could be seen or checked, before any meaning": "Right, so the car wouldn't start again this morning",
              "2": "What I felt: in my body and in me": "Felt rotten about the morning",
              "_": "THE DAY: what I said, sorted six ways": "The meeting went on forever"
             },
             "2": {
              "1": "Tomorrow": "I've decided I'm ringing the garage tomorrow, not fiddling with it myself again",
              "3": "This month": "",
             },
             "3": {
              "1": "This week": "I ring the garage."
             }
            }
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                Telling.Placed("1.1", null, "Right, so the car wouldn't start again this morning"),
                Telling.Placed("1.2", null, "Felt rotten about the morning"),
                Telling.Placed("2.1", null, "I've decided I'm ringing the garage tomorrow, not fiddling with it myself again"),
                Telling.Placed("3.1", null, "I ring the garage."),
            ),
            loose,
        )
    }
}
