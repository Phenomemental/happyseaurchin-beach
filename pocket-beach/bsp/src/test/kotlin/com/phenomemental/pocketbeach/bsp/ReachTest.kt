package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sundial 0.12, the walker (spine:pocket-beach 1.111178719995): a block read
 * from the beach walked here exactly as the pocket walks its own; the beach
 * read at its surface only, and only a block or its index.
 */
class ReachTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val nowMs = 1_795_000_000_000L

    private val orchard = JsObj.of(
        "_" to str("The orchard, through the year."),
        "1" to JsObj.of("_" to str("Autumn"), "1" to str("pick the apples"), "2" to str("rake the leaves")),
        "2" to str("Winter: prune the pears"),
    )

    @Test
    fun aBlockFromTheBeachWalksAsThePocketWalksItsOwn() {
        val (storage, pages) = Bench.fixture(shelf, nowMs)
        check(pages.carryOut(pages.planKeep("orchard", orchard)).ok)
        val bsp = PocketBsp(storage, shelf) { nowMs }
        val read = Reach.blockOf(stringify(orchard)!!)!!
        for ((spindle, attention) in listOf(null to null, "1" to null, "1.2" to null, "1" to -1.0, null to 0.0)) {
            val here = Reach.walk(shelf, "orchard", read, spindle, attention, nowMs)
            assertEquals(Reach.walkPocket(bsp, "orchard", spindle, attention), here, "spindle $spindle, pscale $attention")
        }
        val disc = Reach.walk(shelf, "orchard", read, null, null, nowMs)
        assertTrue(disc.contains("[disc @ pscale 0"), disc)
        assertTrue(disc.contains("Winter: prune the pears"), disc)
        assertTrue(Reach.walk(shelf, "orchard", read, "1.2", null, nowMs).contains("rake the leaves"))
    }

    @Test
    fun theWalkIsAReadNeverAWrite() {
        val p = Reach.params("orchard", " 1.2 ", -1.0)
        assertEquals(setOf("agent_id", "block", "spindle", "pscale_attention"), p.keys().toSet())
        assertEquals("1.2", (p["spindle"] as JsStr).value)
        assertFalse(p.has("content"))
        // Neither given: the disc at pscale 0, never the block whole.
        assertEquals(JsNum(0.0), Reach.params("orchard", "", null)["pscale_attention"])
        assertFalse(Reach.params("orchard", "1", null).has("pscale_attention"))
    }

    @Test
    fun theBeachIsReadAtItsSurfaceOnly() {
        assertEquals("https://beach.happyseaurchin.com/.well-known/pscale-beach?block=pool:Phenomemental", Reach.blockUrl("pool:Phenomemental"))
        assertEquals("https://beach.happyseaurchin.com/.well-known/pscale-beach?block=ahead:Mark%20Beall", Reach.blockUrl("ahead:Mark Beall"))
        assertTrue(Reach.readable(Reach.blockUrl("spine:pocket-beach")))
        assertTrue(Reach.readable(Reach.indexUrl()))
        assertFalse(Reach.readable(Reach.blockUrl("a") + "&spindle=1"))
        assertFalse(Reach.readable("https://example.com/.well-known/pscale-beach?block=a"))
        assertFalse(Reach.readable("http://beach.happyseaurchin.com/.well-known/pscale-beach?block=a"))
        val index = """{"_":"URL surface at beach.happyseaurchin.com.","origin":"beach.happyseaurchin.com","blocks":["lighthouse","pool:Phenomemental","spine:pocket-beach"],"bytes":{}}"""
        assertEquals(listOf("lighthouse", "pool:Phenomemental", "spine:pocket-beach"), Reach.names(index))
        assertEquals(emptyList(), Reach.names("not json"))
        assertNull(Reach.blockOf("[1,2]"))
    }
}
