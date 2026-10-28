package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * bsp-mcp-server's smoke:unit (scripts/smoke-bsp.ts at the pinned commit),
 * assertion for assertion: the walker against sunstone and whetstone, then a
 * point write, a descent write and a subtree write.
 */
class SmokeTest {
    private val shelfDir = File(System.getProperty("pocket.shelf") ?: "../shelf")
    private fun shelfBlock(name: String) = parseJson(File(shelfDir, "$name.json").readText())
    private val sunstone = shelfBlock("sunstone")
    private val whetstone = shelfBlock("whetstone")

    @Test
    fun sunstoneMetadata() {
        assertEquals(1, floorDepth(sunstone))
        assertEquals(null, pscaleAt(0, 1))
        assertEquals(0, pscaleAt(1, 1))
    }

    @Test
    fun wholeBlockRead() {
        val whole = bspRead(sunstone, "", null)
        assertEquals("block", whole.shape, "whole block shape")
        assertSame(sunstone, whole.block, "whole block returns same reference")
    }

    @Test
    fun pathWalkAtOnePointOne() {
        val p11 = bspRead(sunstone, "1.1", null)
        assertEquals("path-walk", p11.shape, "path-walk shape")
        assertTrue(formatRead(p11).contains("polar geometry"), "path-walk text contains polar geometry")
    }

    @Test
    fun descentAtBranchOne() {
        val ring = bspRead(sunstone, "1", -1)
        assertEquals("path-walk+descent", ring.shape, "path-walk+descent shape")
        assertTrue((ring.descent?.size ?: 0) > 0, "descent has digit children")
    }

    @Test
    fun discAtPscaleZero() {
        val disc = bspRead(sunstone, "", 0)
        assertEquals("disc", disc.shape, "disc shape")
        assertTrue((disc.discEntries?.size ?: 0) >= 8, "disc returns at least 8 nodes (sunstone branches)")
    }

    @Test
    fun starAtSeven() {
        val star7 = bspRead(sunstone, "7*", null)
        assertEquals("star", star7.shape, "star shape")
        assertNotNull(star7.semantic, "star has semantic text")
        assertNotNull(star7.inner, "star has inner content")
    }

    @Test
    fun whetstoneSignature() {
        val w11 = bspRead(whetstone, "1.1", null)
        assertEquals("path-walk", w11.shape, "whetstone path-walk shape")
        assertTrue(formatRead(w11).contains("agent_id"), "whetstone 1.1 describes agent_id")
    }

    @Test
    fun pointWriteRoundtrip() {
        val block = parseJson("""{"_":"test","1":"original"}""") as JsObj
        val r = bspWrite(block, "1", null, str("updated"))
        assertEquals("point", r.shape, "point write succeeded")
        assertEquals(str("updated"), block["1"], "point write changed value")
    }

    @Test
    fun descentWriteReplacesTheSubtree() {
        val block = parseJson("""{"_":"parent","1":{"_":"first child","9":"pre-existing"}}""") as JsObj
        val r = bspWrite(block, "1", -1, parseJson("""{"1":"a","2":"b","3":"c"}"""))
        assertEquals("path-walk+descent", r.shape, "descent write shape")
        val one = block["1"] as JsObj
        assertEquals(str("a"), one["1"], "descent digit 1 written")
        assertEquals(str("b"), one["2"], "descent digit 2 written")
        assertEquals(str("c"), one["3"], "descent digit 3 written")
        assertTrue(one["_"] == null && !one.has("9"), "subtree replaced, not merged")
    }

    @Test
    fun subtreeWriteReplacesTheWholeNode() {
        val block = parseJson("""{"_":"root","2":{"_":"old","1":"a"}}""") as JsObj
        bspWrite(block, "2", -3, parseJson("""{"_":"new","1":"x","2":"y"}"""))
        val two = block["2"] as JsObj
        assertEquals(str("new"), two["_"], "subtree underscore replaced")
        assertEquals(str("x"), two["1"], "subtree digit 1 replaced")
        assertTrue(!two.has("a"), "old content gone")
    }
}
