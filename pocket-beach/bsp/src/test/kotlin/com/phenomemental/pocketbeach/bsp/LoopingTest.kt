package com.phenomemental.pocketbeach.bsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Sundial 0.13.1 (spine:pocket-beach 1.1111787199993): a reply repeating one word or phrase many times running is a loop. */
class LoopingTest {
    @Test
    fun oneWordOverAndOverIsALoop() {
        val said = "A long walk by the sea is a good way to " + "the ".repeat(12)
        val loop = assertNotNull(Looping.of(said))
        assertEquals("the", loop.unit)
        assertEquals(12, loop.times)
        assertEquals("A long walk by the sea is a good way to the …", Looping.cut(said, loop))
        // Seven times is not yet.
        assertNull(Looping.of("Well " + "the ".repeat(7)))
    }

    @Test
    fun aPhraseOverAndOverIsALoop() {
        val said = "You said it was stiff. " + "It was stiff but free. ".repeat(4)
        val loop = assertNotNull(Looping.of(said))
        assertEquals("it was stiff but free", loop.unit)
        assertEquals("You said it was stiff. It was stiff but free. …", Looping.cut(said, loop))
        assertNull(Looping.of("You said it was stiff. " + "It was stiff but free. ".repeat(3)))
        // Two words six times; whatever their capitals and the marks between them.
        assertNotNull(Looping.of("So: " + "Thank you! ".repeat(3) + "thank you, ".repeat(3)))
        assertNull(Looping.of("So: " + "thank you ".repeat(5)))
    }

    @Test
    fun aWordStillComingIsReadBothWays() {
        // Mid-stream: the last word part written.
        assertNotNull(Looping.of("I think " + "the ".repeat(8) + "th"))
        assertNotNull(Looping.of("I think " + "the ".repeat(8) + "the"))
    }

    @Test
    fun ordinaryRepetitionIsNotALoop() {
        assertNull(Looping.of("No, no, no, no. That's not what I meant at all."))
        assertNull(Looping.of("| a | b | c | d | e | f | g | h |\n|---|---|---|---|---|---|---|---|\n| 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |"))
        assertNull(Looping.of("- walk Haven\n- the form\n- the ID checks\n- walk somewhere new"))
        assertNull(Looping.of("very very good"))
        // A loop long past is not where she is now.
        assertNull(Looping.of("the ".repeat(10) + "and then she went on to say something else entirely."))
    }
}
