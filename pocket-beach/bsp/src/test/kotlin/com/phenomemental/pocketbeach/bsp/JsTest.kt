package com.phenomemental.pocketbeach.bsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** JavaScript's own rules, checked against what Node 22 (V8) gives for the same inputs. */
class JsTest {
    @Test
    fun numbersPrintAsJavaScriptPrintsThem() {
        val cases = mapOf(
            0.0 to "0", -0.0 to "0", 1.0 to "1", -3.5 to "-3.5", 1.5 to "1.5", 0.1 to "0.1",
            0.30000000000000004 to "0.30000000000000004", 1e21 to "1e+21", 1e20 to "100000000000000000000",
            123456789012.0 to "123456789012", 1e-7 to "1e-7", 1.5e-7 to "1.5e-7", 0.000001 to "0.000001",
            -2.25e-7 to "-2.25e-7", 2.5e21 to "2.5e+21", 5e-324 to "5e-324", 1.7976931348623157e308 to "1.7976931348623157e+308",
            123.456 to "123.456", 100.0 to "100", 1234567.0 to "1234567",
        )
        for ((d, s) in cases) assertEquals(s, jsNumberToString(d), "$d")
    }

    @Test
    fun stringsEscapeAsJsonStringifyEscapes() {
        val s = "q\" b\\ t\t n\n bell\u0007 nbsp  ls  🌊 lone\uD83D end"
        assertEquals("\"q\\\" b\\\\ t\\t n\\n bell\\u0007 nbsp  ls  🌊 lone\\ud83d end\"", stringify(str(s)))
    }

    @Test
    fun objectsListIndexKeysFirst() {
        val o = parseJson("""{"_":"u","9":"n","b":"x","1":"o","10":"t","01":"z"}""")
        assertEquals("""{"1":"o","9":"n","10":"t","_":"u","b":"x","01":"z"}""", stringify(o))
    }

    @Test
    fun prettyPrintMatchesTwoSpaceIndent() {
        assertEquals("{\n  \"1\": [\n    1,\n    {}\n  ],\n  \"_\": \"x\"\n}", stringifyPretty(parseJson("""{"_":"x","1":[1,{}]}""")))
    }

    @Test
    fun datesParseWhereV8ParsesThem() {
        val ok = mapOf(
            "2026-10-28T09:00Z" to 1793178000000, "2026-10-28T09:00:00.1Z" to 1793178000100,
            "2026-10-28T09:00:00.123456789Z" to 1793178000123, "2026-10-28T09:00:00+0100" to 1793174400000,
            "2026-02-30T00:00Z" to 1772409600000, "2026-10-28T24:00Z" to 1793232000000,
            "0000-01-01T00:00Z" to -62167219200000, "2026-10-28T09:00:00.5+14:00" to 1793127600500,
            "2026-10-28T09:00:00+23:59" to 1793091660000, "2026-10-28T09:00" to 1793178000000,
        )
        for ((s, ms) in ok) assertEquals(ms, parseIsoMs(s), s)
        for (s in listOf("2026-13-01T00:00Z", "2026-10-28T24:00:01Z", "2026-10-28T23:60Z", "2026-10-28T23:59:60Z",
            "2026-00-10T00:00Z", "2026-10-00T00:00Z", "2026-10-28T09:00:00-99:99", "2026-10-28T09:00:00+24:00", "tag")) {
            assertNull(parseIsoMs(s), s)
        }
        assertEquals("2026-10-28T09:00:00.000Z", isoString(1793178000000))
    }

    @Test
    fun trimFollowsJavaScriptWhitespace() {
        assertEquals("x", jsTrim("  ﻿\t x 　"))
        assertEquals("\u001Cx\u0085", jsTrim("\u001Cx\u0085"))
    }
}
