package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Sundial 0.13.3 (spine:pocket-beach 1.1111787199994, revised 23 November):
 * a block his words name is read by the app first, its disc at pscale 0,
 * beside his words; Sundial's own "now" is named only as "now block"; and a
 * reply that writes a tool call or "data received" is told apart.
 */
class ReadFirstTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val h = "Phenomemental"

    @Test
    fun aBlockHisWordsNameIsReadByTheAppAsItsDiscBesideHisWords() {
        val (_, pages) = Bench.fixture(shelf, 1_795_000_000_000L)
        check(pages.carryOut(pages.planKeep("passport:$h", JsObj.of(
            "_" to str("Matthew Bushell — digital social innovator."),
            "1" to JsObj.of("_" to str("The heart."), "1" to str("Kindness first.")),
            "2" to str("How to work with me: short sentences, one thing at a time."),
        ))).ok)
        val read = Chats.readByApp(pages, Region.POCKET, "passport:$h")!!
        assertTrue(read.byApp && read.block && !read.whole)
        val turn = Chats.withFiles("what does my passport tell you about how to work with me", listOf(read))
        assertEquals(
            "what does my passport tell you about how to work with me\n\n" +
                "(His block passport:$h, which his words named, read by the app: its opening line, then each part's opening line beneath it:)\n" +
                "Matthew Bushell — digital social innovator.\n  1  The heart.  (1 beneath)\n  2  How to work with me: short sentences, one thing at a time.",
            turn,
        )
        assertFalse(turn.contains("{") || turn.contains("\"_\""), "never JSON")
        assertEquals(null, Chats.readByApp(pages, Region.POCKET, "no-such-block"))
    }

    @Test
    fun sundialsOwnNowIsNamedOnlyAsNowBlock() {
        val mine = listOf("now", "garden", "passport:$h")
        assertEquals(emptyList(), Walking.named("What do I do now?", mine, h))
        assertEquals(emptyList(), Walking.named("Right now I'm tired", mine, h))
        assertEquals(listOf("now"), Walking.named("What's in my now block?", mine, h))
        assertEquals(listOf("passport:$h"), Walking.named("what does my passport tell you about how to work with me", mine, h))
        assertEquals(emptyList(), Walking.named("do what you think is best", mine, h))
    }

    @Test
    fun aWrittenToolCallOrDataReceivedIsToldApart() {
        assertTrue(NothingRead.pretends("```tool_code\nprint(bsp(block=\"passport:Phenomemental\"))\n```"))
        assertTrue(NothingRead.pretends("bsp(agent_id=\"pocket\", block=\"passport:Phenomemental\")"))
        assertTrue(NothingRead.pretends("Data received. Your passport says you belong to the Temporal Weavers Guild."))
        assertTrue(NothingRead.pretends("{\"name\": \"bsp\", \"arguments\": {\"block\": \"passport\"}}"))
        assertTrue(NothingRead.pretends("<|tool_call|>call:bsp{block:passport}"))
        assertFalse(NothingRead.pretends("Your passport says to keep things short, one thing at a time."))
        assertFalse(NothingRead.pretends("I haven't read your passport yet. Shall I?"))
        assertFalse(NothingRead.pretends("The data you gave me was received with thanks."))
        // His phone, 19:21: her summary of the file he attached named the protocol, and the mark fired.
        assertFalse(NothingRead.pretends("The documentation describes the **BSP (Block-Spindle-Point)** protocol, a hierarchical addressing system."))
        assertFalse(NothingRead.pretends("BSP (Block Spindle Point) walks a block"))
        assertTrue(NothingRead.pretends("bsp( block = \"strata\", spindle = \"3.4\" )"))
        assertTrue(NothingRead.pretends("BSP(\"strata\")"))
    }
}
