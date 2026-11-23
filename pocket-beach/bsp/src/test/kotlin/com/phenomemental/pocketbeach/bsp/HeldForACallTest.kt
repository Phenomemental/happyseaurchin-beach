package com.phenomemental.pocketbeach.bsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Sundial 0.13.4: her words on a turn with the walker offered, shown once their opening shows they aren't a call. */
class HeldForACallTest {
    /** What was shown, and after which piece each was shown (the pieces' count when it came at the end). */
    private fun run(pieces: List<String>, called: Boolean = false): Pair<List<String>, List<Int>> {
        val shown = mutableListOf<String>()
        val at = mutableListOf<Int>()
        var now = 0
        val h = HeldForACall { shown += it; at += now }
        pieces.forEachIndexed { i, p -> now = i; h.feed(p) }
        now = pieces.size
        h.end(called)
        return shown to at
    }

    @Test
    fun wordsAreShownFromTheirFirstPiece() {
        // As the engine streamed "Based on his block passport…" on the laptop.
        val (shown, at) = run(listOf("Based", " on", " his", " block", " passport"))
        assertEquals(listOf("Based", " on", " his", " block", " passport"), shown)
        assertEquals(0, at.first(), "the first piece shown as it came")
    }

    @Test
    fun aFewPiecesAreHeldOnlyWhileACallIsStillPossible() {
        // "Call" could open "call:"; "Call me" can't. Leading space and a lone "<" or "`" wait a piece.
        val (shown, at) = run(listOf("\n", "Ca", "ll", " me", " Gemma"))
        assertEquals("\nCall me Gemma", shown.joinToString(""))
        assertEquals(3, at.first(), "shown at \" me\", the fourth piece")
        assertEquals("<b>bold</b>", run(listOf("<", "b>", "bold</b>")).first.joinToString(""))
        assertEquals("```python\nx\n```", run(listOf("```", "python", "\nx\n```")).first.joinToString(""))
        assertEquals("Printing it now", run(listOf("Print", "ing", " it now")).first.joinToString(""))
    }

    @Test
    fun aCallWrittenAsTextIsHeldToTheEndAndShownThen() {
        val pretend = listOf("```", "tool", "_code\n", "print(bsp(block=\"passport:Phenomemental\"))", "\n```\nData received.")
        val (shown, at) = run(pretend)
        assertEquals(listOf(pretend.joinToString("")), shown)
        assertEquals(pretend.size, at.single(), "shown only at the end")
        assertTrue(HeldForACall.couldBeACall("{\"name\": \"bsp\""))
        assertTrue(HeldForACall.couldBeACall("<start_function_call>call:bsp{"))
    }

    @Test
    fun whatWasHeldWhenTheEngineTookACallIsTheCallsOwn() {
        assertEquals(emptyList(), run(listOf("{\"", "name\": \"bsp\"}"), called = true).first)
        assertEquals(emptyList(), run(listOf("  "), called = true).first)
        // Words shown before a call stay shown.
        assertEquals(listOf("Let me look."), run(listOf("Let me look."), called = true).first)
    }
}
