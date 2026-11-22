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
 * Sundial 0.10.1, item 10, from his Pixel on 3 November: opening a new
 * conversation showed "(The app: Matthew — philosopher, peer and digital
 * tinkerer, …)", a line of his passport she was handed and copied out, then
 * her question in quote marks, then "Kept.". The greeting is never handed
 * his passport; and every reply of hers passes one check before it is shown:
 * no piece beginning "The app:", nothing she was handed, no keep status, and
 * no quote marks wrapping her reply.
 */
class HerOwnWordsTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val now = ZonedDateTime.of(2026, 11, 3, 18, 40, 0, 0, ZoneId.of("Europe/London"))
    private val h = Hatch.MATTHEW_HANDLE
    /** His who-line as 0.8 drafted it with Claude, kept here as the test's own words (Sundial 0.12 took his three lines out of the code). */
    private val who = "Matthew — philosopher, peer and digital tinkerer, working at the edge of what community could become. Underneath it all: managing the environment so my gifts, and other people's, can flow."
    private val question = "What has this day held for you?"

    /** As his phone showed it, and as the stand-in plays it back on the device. */
    private val asOnHisPhone = "(The app: $who)\n“$question”\nKept."

    private fun streamed(reply: String, shown: Talking.Shown): List<String> {
        val out = mutableListOf<String>()
        val words = StringBuilder()
        for (piece in reply.chunked(3)) {
            words.append(piece)
            out += shown.of(words.toString())
        }
        out += shown.of(reply, over = true)
        return out
    }

    @Test
    fun theGreetingAsHisPhoneShowedItIsHerOneQuestionAlone() {
        assertEquals(question, Talking.hers(asOnHisPhone))
        assertEquals(question, Talking.oneQuestion(asOnHisPhone))
        // Her question with a greeting before it, or a second question after: her one question.
        assertEquals(question, Talking.oneQuestion("Good evening. $question Where did the membership agreement go?"))
        // Nothing asked: Sundial's own question stands.
        assertNull(Talking.oneQuestion("(The app: $who)\nKept."))
    }

    @Test
    fun whileItComesNothingNotHersIsEverShownAndNothingShownIsTakenBack() {
        for (reply in listOf(asOnHisPhone, "“$question”", "\"$question\"")) {
            val outs = streamed(reply, Talking.Shown("", "", hisWords = false, handed = listOf(who)))
            for ((a, b) in outs.zipWithNext()) assertTrue(b.startsWith(a), "shown \"$a\", then \"$b\"")
            assertTrue(outs.none { it.contains("The app") || it.contains("philosopher") || it.contains("Kept") || it.contains("“") || it.contains("\"") }, outs.joinToString(" | "))
            assertEquals(question, outs.last())
        }
    }

    @Test
    fun whatSheWasHandedIsNeverShownAsHersInAnyReply() {
        // His passport line, copied with no "The app:" before it, in the middle of a talk reply on E2B.
        val e2b = "You've done some of the id checks today. $who What did David bring to it?"
        assertEquals("You've done some of the id checks today. What did David bring to it?", Talking.hers(e2b, handed = listOf(who)))
        // An app line she was handed, anywhere in her reply, bracketed or not.
        assertEquals("You just want to talk. What's on today?", Talking.hers("The app: his words hold no day yet.\nYou just want to talk. What's on today?"))
        assertEquals("What's on today?", Talking.hers("[The app: nothing is being kept or set out from this.] \nWhat's on today?"))
        // His own words in this message, given back, stay hers to say, even where her window holds them too.
        val said = "I need to update a membership agreement and print it out before Friday."
        val reflection = "You need to update a membership agreement and print it out before Friday."
        assertEquals(reflection, Talking.hers(reflection, said = said, handed = listOf("and need to update a membership agreement and print it out before Friday")))
    }

    @Test
    fun aKeepStatusIsNeverShownInTheConversation() {
        for (s in listOf("Kept.", "Kept", "Not kept.", "Your day is kept.", "Today is kept.", "Saved.")) assertTrue(Talking.keepStatus(s), s)
        for (s in listOf("You kept going.", "Kept going all day?", "What did you keep for tomorrow?")) assertFalse(Talking.keepStatus(s), s)
        assertEquals("You kept going. What came next?", Talking.hers("You kept going. What came next?\nKept."))
    }

    @Test
    fun quoteMarksWrappingHerReplyAreTakenOffAndHerQuotesOfHimStay() {
        assertEquals(question, Talking.hers("“$question”"))
        assertEquals(question, Talking.hers("\"$question\""))
        assertEquals("“Busy,” you said. What filled it?", Talking.hers("“Busy,” you said. What filled it?"))
        assertFalse(Talking.wrapsWhole("“Busy” and “tired”"))
        assertTrue(Talking.wrapsWhole("“$question”"))
    }

    /** Sundial 0.12: her greeting's window is the time alone, so it never holds his passport. */
    @Test
    fun theGreetingIsNeverHandedHisPassport() {
        val window = HerWindow.released(now)
        assertFalse(window.contains("Matthew"), window)
        assertTrue(window.startsWith("It is "), window)
    }
}
