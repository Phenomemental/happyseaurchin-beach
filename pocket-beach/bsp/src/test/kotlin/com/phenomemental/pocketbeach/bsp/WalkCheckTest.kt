package com.phenomemental.pocketbeach.bsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Sundial 0.12.1, the walk check (spine:pocket-beach 1.1111787199993): twenty known addresses, and how each arrived. */
class WalkCheckTest {
    @Test
    fun twentyAddressesEachAnAddressTheWalkerReads() {
        assertEquals(20, WalkCheck.ADDRESSES.size)
        assertEquals(20, WalkCheck.ADDRESSES.toSet().size)
        assertTrue("1.111178719993" in WalkCheck.ADDRESSES && "2.2" in WalkCheck.ADDRESSES)
        for (a in WalkCheck.ADDRESSES) {
            assertTrue(a.count { it == '.' } <= 1, a)
            assertTrue(a.all { it.isDigit() && it != '0' || it == '.' }, a)
            parseSpindle(a, 1)
        }
        assertEquals("Read me what's at 2.2 in my walk-check block.", WalkCheck.message("2.2"))
    }

    @Test
    fun anAddressArrivesIntactOnlyAsTheSameText() {
        assertTrue(WalkCheck.outcome("1.111178719993", mapOf("block" to "walk-check", "spindle" to "1.111178719993")).intact)
        assertTrue(WalkCheck.outcome("2.2", mapOf("spindle" to " 2.2 ")).intact, "space around it is no alteration")
        val asNumber = WalkCheck.outcome("2.2", mapOf("spindle" to 2.2))
        assertFalse(asNumber.intact)
        assertEquals("2.2 arrived as a number, 2.2", asNumber.said)
        val altered = WalkCheck.outcome("1.111178719993", mapOf("spindle" to "1.11117871999"))
        assertFalse(altered.intact)
        assertEquals("1.111178719993 arrived as 1.11117871999", altered.said)
        assertEquals("2.2: no call came back", WalkCheck.outcome("2.2", null).said)
        // Altered is what came and wasn't as sent; a call that never came isn't counted altered.
        assertTrue(asNumber.altered && altered.altered)
        assertFalse(WalkCheck.outcome("2.2", null).altered || WalkCheck.outcome("2.2", null).intact)
        assertFalse(WalkCheck.outcome("2.2", mapOf("spindle" to "2.2")).altered)
        assertEquals("1 of 2 arrived intact.", WalkCheck.said(listOf(WalkCheck.outcome("2.2", mapOf("spindle" to "2.2")), asNumber)))
    }

    @Test
    fun theToolNamesTheBlocksHisWordsNamedAndTakesTheSpindleAsText() {
        val tool = parseJson(WalkTool.forBlocks(listOf("membership", "passport:Phenomemental"))) as JsObj
        assertEquals("bsp", (tool["name"] as JsStr).value)
        assertEquals(
            "Sundial holds his blocks on his phone, and his words named membership and passport:Phenomemental. Read one, as the beach's bsp() reads a block: no address for its opening line and each part's, or an address for the line there. " +
                "Each line of a block's top is only the start of that part: a part with lines beneath it holds the detail, and walking to its address gives it whole.",
            (tool["description"] as JsStr).value,
        )
        val props = ((tool["parameters"] as JsObj)["properties"] as JsObj)
        assertEquals(listOf("membership", "passport:Phenomemental"), ((props["block"] as JsObj)["enum"] as JsArr).items.map { (it as JsStr).value })
        assertEquals("string", ((props["spindle"] as JsObj)["type"] as JsStr).value)
        assertEquals("integer", ((props["pscale_attention"] as JsObj)["type"] as JsStr).value)
        // The check's own: its block named, as 0.13 names his.
        val check = parseJson(WalkCheck.TOOL) as JsObj
        assertTrue((check["description"] as JsStr).value.contains("his words named walk-check. Read it,"))
        // A name with a quote in it stays whole JSON.
        assertEquals("say \"hi\"", ((((parseJson(WalkTool.forBlocks(listOf("say \"hi\""))) as JsObj)["parameters"] as JsObj)["properties"] as JsObj)["block"] as JsObj).let { ((it["enum"] as JsArr).items[0] as JsStr).value })
    }
}
