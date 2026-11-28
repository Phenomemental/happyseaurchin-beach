package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sundial 0.13, the walk (spine:pocket-beach 1.1111787199994 and
 * 1.1111787199995): the blocks his words name, the guard before a walk, and
 * what she is handed, readable text, never JSON.
 */
class WalkingTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val nowMs = 1_795_000_000_000L
    private val h = "Phenomemental"
    private val mine = listOf("passport:$h", "shell:$h", "history:$h", "garden", "membership", "pocket-companion", "spine:demo")

    @Test
    fun hisWordsNameABlockByItsNameOrItsPlainForm() {
        assertEquals(listOf("membership"), Walking.named("What's left on my membership block?", mine, h))
        assertEquals(listOf("passport:$h"), Walking.named("Read my passport for me", mine, h))
        assertEquals(listOf("shell:$h"), Walking.named("What's in the shell?", mine, h))
        assertEquals(listOf("garden"), Walking.named("How's the Garden looking?", mine, h))
        assertEquals(listOf("pocket-companion"), Walking.named("open pocket companion", mine, h))
        assertEquals(listOf("spine:demo"), Walking.named("What does spine:demo say at 2?", mine, h))
        assertEquals(listOf("passport:$h", "garden"), Walking.named("Compare my passport with the garden", mine, h))
        // Not named: a part of a word, a role word with no "my", a day's talk.
        assertEquals(emptyList(), Walking.named("I walked Haven then the gardener came", mine, h))
        assertEquals(emptyList(), Walking.named("I need a new passport photo", mine, h))
        assertEquals(emptyList(), Walking.named("I walked Haven this morning then spent an hour on the membership form.", mine.filter { it != "membership" }, h))
    }

    @Test
    fun herCallsBlockIsResolvedAmongThoseHeNamed() {
        val named = listOf("passport:$h", "garden")
        assertEquals("passport:$h", Walking.resolve("passport", named, h))
        assertEquals("passport:$h", Walking.resolve("passport:Phenomemental", named, h))
        assertEquals("garden", Walking.resolve("Garden", named, h))
        assertNull(Walking.resolve("history", named, h), "a block he didn't name isn't walked")
        assertEquals("garden", Walking.resolve(null, listOf("garden"), h))
    }

    private fun pocket(): Pages {
        val (_, pages) = Bench.fixture(shelf, nowMs)
        check(pages.carryOut(pages.planKeep("membership", JsObj.of(
            "_" to str("The membership form, step by step."),
            "1" to str("Agree the categories with David"),
            "2" to JsObj.of("_" to str("ID checks"), "1" to str("Finish the remaining checks"), "2" to str("Log each one in the register")),
            "3" to str("Print the agreement for signing"),
        ))).ok)
        return pages
    }

    @Test
    fun aWalkIsHandedHerAsReadableTextNeverJson() {
        val pages = pocket()
        val named = listOf("membership")
        val point = Walking.walk(pages, mapOf("block" to "membership", "spindle" to "2.2"), named, h)
        assertTrue(point.found)
        assertEquals("membership at 2.2", point.chip)
        assertEquals("walked membership at 2.2", point.said)
        assertEquals(
            "(His block membership, walked to 2.2: the path from its opening line down to it.)\nThe membership form, step by step.\n  2  ID checks\n    2.2  Log each one in the register",
            point.text,
        )
        val deeper = Walking.walk(pages, mapOf("block" to "membership", "spindle" to "2", "pscale_attention" to -1), named, h)
        assertEquals(
            "(His block membership, walked to 2: the path from its opening line down to it, then the lines beneath it.)\nThe membership form, step by step.\n  2  ID checks\n    2.1  Finish the remaining checks\n    2.2  Log each one in the register",
            deeper.text,
        )
        val disc = Walking.walk(pages, mapOf("block" to "membership"), named, h)
        assertEquals("membership", disc.chip)
        assertTrue(disc.text.startsWith("(His block membership, as bsp() reads it: the start of its opening line and of each part's, and how many lines stand beneath each part.)\nThe membership form, step by step.\n  1  Agree the categories with David\n  2  ID checks  (2 beneath)"), disc.text)
        for (w in listOf(point, deeper, disc)) assertFalse(w.text.contains("{") || w.text.contains("\"_\""), w.text)
        // Deeper is as bsp() reads it: a pscale below the address's own. At 2.2's parent 2 (pscale 0), 0 is the place itself.
        assertFalse(Walking.walk(pages, mapOf("block" to "membership", "spindle" to "2", "pscale_attention" to 0), named, h).text.contains("2.1"))
        check(pages.carryOut(pages.planKeep("plan", JsObj.of("_" to str("A plan."), "1" to JsObj.of("_" to str("A"), "1" to JsObj.of("_" to str("A1"), "1" to str("A1a")))))).ok)
        val atA1 = { p: Int -> Walking.walk(pages, mapOf("block" to "plan", "spindle" to "1.1", "pscale_attention" to p), listOf("plan"), h).text }
        assertFalse(atA1(-1).contains("A1a"), "pscale -1 is 1.1's own: the place, not beneath it")
        assertTrue(atA1(-2).contains("1.11  A1a"), atA1(-2))
    }

    @Test
    fun theGuardWalksNoPlaceThatIsntThere() {
        val pages = pocket()
        val named = listOf("membership")
        for (s in listOf("7", "2.9", "2.2.1", "2._", "abc")) {
            val w = Walking.walk(pages, mapOf("block" to "membership", "spindle" to s), named, h)
            assertFalse(w.found, s)
            assertEquals("There is no place $s in his block membership. Answer him without it.", w.text)
            assertEquals("looked for $s in membership", w.said)
        }
        val other = Walking.walk(pages, mapOf("block" to "history", "spindle" to "1"), named, h)
        assertFalse(other.found)
        assertTrue(other.text.startsWith("There is no block called history among those he named"), other.text)
        // A number sent as JSON is read as the address it was, and checked like any other.
        assertTrue(Walking.walk(pages, mapOf("block" to "membership", "spindle" to 2.2), named, h).found)
    }
}
