package com.phenomemental.pocketbeach.bsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sundial 0.10.1, from his Pixel 9 Pro XL on 3 November, with his test
 * message exactly as he sent it: her reflection of his words was taken for
 * her turn played back (part one gone on E4B; the whole reply gone on E2B);
 * a line once shown is never taken out of view; part one asked for once more
 * when it comes back empty, and never a reply missing it shown as whole; her
 * words kept plain when the question can't be read from them; the danger
 * check the help line and cook 1.1.8.5 share.
 */
class SundialFixesTest {
    /** His test message of 3 November, exactly as he sent it. */
    private val idChecks = "I've done some of the id checks today, furthered the membership form planning, and have responded to a fraud report, interacted with David. All that's left today is to evaluate sundial."
    private val opening = HelloWorld.SHELL_OPENING
    private val reflection = "You've done some of the id checks today, furthered the membership form planning, and responded to a fraud report, and interacted with David."
    private val question = "What will evaluating sundial ask of you?"
    private val e2bLine = "$reflection All that's left is to evaluate sundial. $question"

    // ── fix 3 and 4: the cause ──

    @Test
    fun herReflectionOfHisWordsWasTakenForHerTurnPlayedBack() {
        // As 0.10 had it: eight of his words in a row is his turn played back, and is left out.
        assertEquals("", Talking.unechoed(reflection, idChecks, opening), "E4B: part one, left out")
        assertEquals("", Talking.unechoed(e2bLine, idChecks, opening), "E2B: her one line, the whole reply, left out")
        // 0.10.1: where his cook asks her to give his words back, they are hers to say.
        assertEquals(reflection, Talking.unechoed(reflection, idChecks, opening, hisWords = false))
        assertEquals(e2bLine, Talking.unechoed(e2bLine, idChecks, opening, hisWords = false))
        assertEquals(emptyList(), Talking.copied(e2bLine, idChecks, opening, hisWords = false))
        // Her turn played back is still left out: "Matthew:" and his words, and the app's own line.
        val played = "Matthew: $idChecks\n(The app: nothing is being kept or set out from this.)\n$question"
        assertEquals(question, Talking.unechoed(played, idChecks, opening, listOf("The app: nothing is being kept or set out from this."), hisWords = false))
    }

    @Test
    fun theTestMessageIsNewsOfACardOnHisHandAsOnHisPhone() {
        // Why "Sort as my day" came beneath it: his hand held the membership agreement from his 10:03 telling.
        assertTrue(Landing.isTelling(idChecks, mapOf("3.1" to "and need to update a membership agreement and print it out")))
        assertFalse(Landing.isTelling(idChecks, emptyMap()), "two sentences, thirty-one words: no day on its own")
    }

    // ── fix 3: never taken out of view ──

    private fun streamed(reply: String, shown: Talking.Shown): List<String> {
        val out = mutableListOf<String>()
        val words = StringBuilder()
        for (piece in reply.chunked(4)) {
            words.append(piece)
            out += shown.of(words.toString())
        }
        out += shown.of(reply, over = true)
        return out
    }

    private fun assertNeverTakenBack(outs: List<String>) {
        for ((a, b) in outs.zipWithNext()) assertTrue(b.startsWith(a), "shown \"$a\", then \"$b\"")
    }

    @Test
    fun aLineOnceShownIsNeverTakenOutOfView() {
        // On E2B as Sundial asks her: her one line, shown as it comes, and whole at the end.
        val e2b = streamed(e2bLine, Talking.Shown(idChecks, opening, hisWords = false))
        assertNeverTakenBack(e2b)
        assertEquals(e2bLine, e2b.last())
        // Even where his words count as played back (before a hatch): a line shown before eight of his words come stays.
        val late = "Thank you for telling me all of that, really. I've done some of the id checks today, furthered the membership form planning."
        val before = streamed(late, Talking.Shown(idChecks, opening, hisWords = true))
        assertNeverTakenBack(before)
        assertEquals(late, before.last())
        // A line that plays her turn back from its start is never shown at all.
        val played = streamed("Matthew: $idChecks\n$question", Talking.Shown(idChecks, opening, hisWords = true))
        assertNeverTakenBack(played)
        assertTrue(played.none { it.contains("Matthew:") }, played.joinToString(" | "))
        assertEquals(question, played.last())
    }

    // ── fix 4: part one ──

    private val asked = listOf("THE REFLECTION", "THE QUESTION")

    @Test
    fun partOneEmptyIsAskedForOnceMore() {
        val calls = mutableListOf<Boolean>()
        val got = HerReply.inParts(asked, { again ->
            calls += again
            if (!again) mapOf("THE REFLECTION" to "", "THE QUESTION" to question) else mapOf("THE REFLECTION" to reflection, "THE QUESTION" to question)
        })
        assertEquals(listOf(false, true), calls)
        assertEquals(mapOf("THE REFLECTION" to reflection, "THE QUESTION" to question), got.parts)
        // The workings: each call exactly as she returned it, the empty part shown empty.
        assertEquals("THE REFLECTION “”; THE QUESTION “$question”", HerReply.said(asked, got.attempts[0]))
        assertEquals("THE REFLECTION “$reflection”; THE QUESTION “$question”", HerReply.said(asked, got.attempts[1]))
    }

    @Test
    fun aReplyMissingPartOneIsNeverShownAsWhole() {
        val still = HerReply.inParts(asked, { mapOf("THE REFLECTION" to " ", "THE QUESTION" to question) })
        assertEquals(2, still.attempts.size, "asked once more, and no more")
        assertNull(still.parts, "not whole: she is asked free")
        // Part one left out by Ask's cleaning counts as empty, as 0.10's did to his reflection.
        val cleaned = HerReply.inParts(asked, { mapOf("THE REFLECTION" to reflection, "THE QUESTION" to question) }, { Talking.unechoed(it, idChecks, opening) })
        assertEquals(2, cleaned.attempts.size)
        assertNull(cleaned.parts)
        // No whole tool call: no second ask; she is asked free.
        var n = 0
        val none = HerReply.inParts(asked, { n++; null })
        assertEquals(1, n)
        assertNull(none.parts)
        assertEquals("no whole tool call came back", HerReply.said(asked, none.attempts.single()))
        // Part two missing: not whole either.
        assertNull(HerReply.inParts(asked, { mapOf("THE REFLECTION" to reflection, "THE QUESTION" to "") }).parts)
    }

    @Test
    fun herWordsStayPlainWhenTheQuestionCantBeReadFromThem() {
        assertEquals(reflection to question, Talking.splitQuestion("$reflection $question"))
        assertEquals(question to "", Talking.splitQuestion(question), "a lone question: plain, as she said it")
        assertEquals(reflection to "", Talking.splitQuestion(reflection))
    }

    // ── fix 2: the one danger check ──

    @Test
    fun theDangerCheckIsTheAppsOwn() {
        assertFalse(Talking.unsafe(idChecks, Talking.SAFETY_WORDS), "his test message shows no danger")
        assertTrue(Talking.unsafe("I don't feel safe tonight.", Talking.SAFETY_WORDS))
    }
}

/** Sundial 0.10.1, fix 5: the captions on hold, clear of every line, dot and edge, however his days rest. */
class DialLabelsTest {
    /** A serif italic at 19 of the dial's units runs about half its size a letter; a generous ten, to be safe. */
    private fun widths(lines: List<String>) = lines.map { it.length * 10.0 }

    private fun check(counts: List<Int>, what: String) {
        val pools = DialMath.poolsAt(counts)
        val lines = Mark.POOLS.map { DialMath.captionLines(it.phrase) }
        val caps = DialLabels.place(pools, lines, lines.map(::widths))
        caps.forEachIndexed { i, c ->
            val name = "${Mark.POOLS[i].phrase} ($what)"
            assertTrue(c.box.inside(DialLabels.BOUNDS), "$name is within the dial: ${c.box}")
            assertFalse(DialLabels.onInk(c.box), "$name crosses no line of ink: ${c.box}")
            pools.forEachIndexed { j, p -> assertFalse(DialLabels.onCircle(c.box, DialMath.Pt(p.x, p.y), p.r), "$name clear of ${Mark.POOLS[j].phrase}'s pool") }
            caps.forEachIndexed { j, o -> if (j != i) assertFalse(c.box.overlaps(o.box), "$name clear of ${Mark.POOLS[j].phrase}'s caption") }
        }
    }

    @Test
    fun captionsAreClearHoweverHisDaysRest() {
        check(listOf(0, 0, 0, 0, 0, 0), "no days")
        check(listOf(1, 1, 1, 1, 1, 1), "evenly")
        check(listOf(9, 0, 0, 0, 0, 0), "all in what happened")
        check(listOf(0, 9, 0, 0, 0, 0), "all in what you felt")
        check(listOf(0, 0, 0, 9, 0, 0), "all in what matters")
        check(listOf(0, 0, 0, 0, 0, 9), "all in what's ahead")
        check(listOf(5, 4, 3, 2, 1, 0), "a slope")
        check(listOf(3, 3, 0, 1, 0, 6), "as on his phone")
    }

    @Test
    fun theOldPlaceBeneathEachPoolRanIntoTheHeart() {
        // As 0.10 drew them: each caption beneath its pool, 20 below its edge.
        val pools = DialMath.poolsAt(listOf(1, 1, 1, 1, 1, 1))
        val crossed = Mark.POOLS.indices.filter { i ->
            val p = pools[i]
            val lines = DialMath.captionLines(Mark.POOLS[i].phrase)
            val w = (lines.map { it.length * 10.0 }.maxOrNull() ?: 0.0)
            val top = p.y + p.r + 20 - 19 * 0.78
            DialLabels.onInk(DialLabels.Box(p.x - w / 2, top, p.x + w / 2, top + 19 * 1.04 + 21.0 * (lines.size - 1)))
        }
        assertTrue(crossed.isNotEmpty(), "some caption crossed the heart, as on his phone")
    }
}

/** Sundial 0.10.1, fix 6: a line of his kept day once, with no quote mark left open. */
class KeptLinesTest {
    private val line = "I'm also going to need to do ID checks this week."

    @Test
    fun aQuoteMarkLeftOpenIsTakenOff() {
        assertEquals(line, Telling.unquoted("“$line"))
        assertEquals(line, Telling.unquoted("\"$line"))
        assertEquals(line, Telling.unquoted("$line”"))
        assertEquals("“$line”", Telling.unquoted("“$line”"), "both ends: his quote, kept")
        assertEquals("the dogs’", Telling.unquoted("the dogs’"), "an apostrophe at the end stays")
        assertEquals("He said “go” to it.", Telling.unquoted("He said “go” to it."))
    }

    @Test
    fun aPieceOnHisHandIsNotARungAsWell() {
        val now = java.time.ZonedDateTime.of(2026, 11, 3, 16, 0, 0, 0, java.time.ZoneOffset.UTC)
        val card = Days.landing(
            Hatch.MATTHEW_HANDLE, now.toLocalDate(), null, emptyList(), emptyList(),
            listOf(Days.Intent(line, 3, null, null)), listOf(Days.Rung("week", "“$line")), now,
        )
        assertEquals(listOf(line), card.lines.map { it.words }, "once, on his hand, with no stray quote")
        assertTrue(card.lines.single().where is Days.Where.Pile)
    }
}
