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
 * Sundial 0.10.2, from his Pixel's E4B on 3 November: 29.0 s from his
 * message to her whole reply. Her reply's tool call came back with no call
 * (18.0 s), and she was asked again free (10.8 s), every turn, in a
 * conversation opened afresh. Now she writes freely in one kept
 * conversation: her places line, when she gives one, is read and taken off;
 * of what is shown, a closing question is in italic and what comes before it
 * part one; with no closing question, plain words.
 */
class FreeReplyTest {
    private val idChecks = "I've done some of the id checks today, furthered the membership form planning, and have responded to a fraud report, interacted with David. All that's left today is to evaluate sundial."
    private val glum = "no I'm not focused at all. I'm feeling glum"
    private val reflection = "You've done some of the id checks today, furthered the membership form planning, and responded to a fraud report, and interacted with David."
    private val question = "What will evaluating sundial ask of you?"

    @Test
    fun herPlacesLineIsReadAndTakenOffAndHerClosingQuestionIsSplitFromPartOne() {
        val raw = "$reflection\n$question\nPlaces: what happened, what's ahead"
        val shown = Talking.hers(raw, idChecks)
        assertEquals("$reflection\n$question", shown, "her places line is never shown")
        val f = HerReply.free(raw, shown)
        assertEquals("Places: what happened, what's ahead", f.placesLine)
        assertEquals(listOf(1, 6), f.places)
        assertEquals("REFLECT", f.shape)
        assertEquals(reflection, f.partOne)
        assertEquals(question, f.question)
        assertEquals(raw, f.raw)
    }

    @Test
    fun herPlacesLineOnTheSameLineAsHerQuestionIsTakenOffToo() {
        val raw = "$reflection $question Places: what happened, what he made of it"
        val shown = Talking.hers(raw, idChecks)
        assertEquals("$reflection $question", shown)
        val f = HerReply.free(raw, shown)
        assertEquals(listOf(1, 3), f.places)
        assertEquals("REFLECT", f.shape)
    }

    @Test
    fun allSixPlacesNamedCountAsNone() {
        val raw = "$reflection\n$question\nPlaces: what happened, what he felt, what he made of it, what matters, a choice, what's ahead"
        val f = HerReply.free(raw, Talking.hers(raw))
        assertTrue(f.allSix)
        assertEquals(emptyList(), f.places)
    }

    @Test
    fun theShapeTheSplitFinds() {
        // His glum message on his Pixel, as her free reply had it: part one, then her question.
        val g = "You're feeling not focused at all and glum.\nWhat's one small thing in front of you right now?"
        HerReply.free(g, Talking.hers(g)).let { f ->
            assertEquals("REFLECT", f.shape)
            assertEquals("You're feeling not focused at all and glum.", f.partOne)
            assertEquals("What's one small thing in front of you right now?", f.question)
            assertNull(f.placesLine, "no day in his words: no places line")
        }
        // His question answered, with no closing question: plain words.
        val a = "Yes: the philosopher of process, who held that the world is made of events rather than things."
        assertEquals("ANSWER", HerReply.free(a, Talking.hers(a)).shape)
        assertEquals("" to "", HerReply.split(""))
        // Her question alone, in italic; a question in emphasis marks shown without them.
        assertEquals("QUESTION", HerReply.free(question, Talking.hers(question)).shape)
        assertEquals("You feel glum." to "What's weighing most?", HerReply.split("You feel glum.\n*What's weighing most?*"))
        // A question inside part one isn't the closing question.
        assertEquals("ANSWER", HerReply.free("Did he really? You said so, and it stands.", "Did he really? You said so, and it stands.").shape)
    }

    @Test
    fun herPlacesLineIsNeverShownWhileHerReplyComesAndNothingShownIsTakenBack() {
        val raw = "$reflection\n$question\nPlaces: what happened, what's ahead"
        val shown = Talking.Shown(idChecks, "", hisWords = false)
        val outs = mutableListOf<String>()
        val words = StringBuilder()
        for (piece in raw.chunked(3)) { words.append(piece); outs += shown.of(words.toString()) }
        outs += shown.of(raw, over = true)
        for ((a, b) in outs.zipWithNext()) assertTrue(b.startsWith(a), "shown \"$a\", then \"$b\"")
        assertTrue(outs.none { it.contains("Places") }, outs.last())
        assertEquals("$reflection\n$question", outs.last())
    }

    /**
     * Item 3, on the laptop's E4B: "How did you get on with "evaluate sundial"
     * today?" was dropped as six words in a row of her instruction's example.
     * The example is a frame for her question: six of its words are hers to
     * use; her example played back whole (eight of its words) never is.
     */
    @Test
    fun herGreetingInTheFrameOfHerExampleIsHersToShow() {
        val line = "The app: The app shows him that line just before your question. Ask him one question about how it went today. Name what he wrote. Speak to him as you. " +
            "Put the word you in your question. For example, for “ring Jo”: How did you get on with ringing Jo today? Ask about the day, not his feelings. One question only."
        val material = "Yesterday he wrote this line for today: “evaluate sundial.”"
        val clock = listOf("It is Tuesday 3 November 2026, 18:40.")
        val hers = "How did you get on with \"evaluate sundial\" today?"
        assertEquals(hers, Talking.oneQuestion(hers, clock, listOf(line, material)))
        assertNull(Talking.oneQuestion("How did you get on with ringing Jo today?", clock, listOf(line, material)), "her example, played back whole")
    }

    @Test
    fun herTurnInHerKeptConversationIsHisNewMessageAndWhatIsNewWithIt() {
        val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
        val now = ZonedDateTime.of(2026, 11, 3, 20, 10, 0, 0, ZoneId.of("Europe/London"))
        val (_, pages) = Bench.fixture(shelf, now.toInstant().toEpochMilli(), Talks.SHELL_RECIPE)
        val recipe = "The app: nothing is being kept or set out from this. Your reply has two parts, unless he asks you something."
        val noDay = "The app: his words hold no day yet; if you reflect, ask what's on today."
        val turn = HerWindow.turnKept(glum, pages, listOf(Cook.Handed(recipe), Cook.Handed(noDay)), recipe)
        assertEquals("($noDay)\n$glum", turn, "never her recipe again, never the time")
        val opening = HerWindow.opening("1. His heart (his words, about him, not you): “The heart.”", now, recipe)
        assertTrue(opening.contains("It is Tuesday 3 November 2026, 20:10 as this conversation opens."), opening)
        assertTrue(opening.endsWith(recipe), "her recipe in her instruction, once")
    }
}
