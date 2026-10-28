package com.phenomemental.pocketbeach.bsp

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/*
 * Temporal.kt — the clock's address ⇄ moment, as bsp-mcp-server's
 * src/temporal.ts reckons it at the pinned commit: the parts a bundle needs
 * (a `name:now` member and the window's stamp line). Ten rungs from the
 * millennium to the beat, three fine rungs beneath (minute, breath, second).
 */

/** Floor of the temporal spine: ten rungs, pscale 9 (millennium) → 0 (beat). */
const val TEMPORAL_FLOOR = 10

/** The fine rungs beneath the beat, by name: −1, −2, −3. */
val FINE_RUNG_NAMES = listOf("minute", "breath", "second")

/** The nine day-parts (pscale 1) — 2h40m each, named as humans name them. */
val DAY_PARTS = listOf("deep night", "dawn", "early morning", "morning", "midday", "afternoon", "late afternoon", "evening", "night")

private val MONTHS = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val WEEKDAYS = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

/** A UTC moment → the canonical full-width ten-digit address, with [fine] decimals beneath the beat. */
fun momentToAddress(epochMs: Long, fine: Int = 0): String {
    val t = Instant.ofEpochMilli(epochMs).atZone(ZoneOffset.UTC)
    val y = t.year
    if (y < 1000 || y > 9999) throw IllegalArgumentException("temporal: year $y is outside the floor-10 form (1000..9999)")
    val month = t.monthValue - 1
    val dom = t.dayOfMonth
    val secOfDay = t.hour * 3600 + t.minute * 60 + t.second + Math.floorMod(epochMs, 1000L) / 1000.0

    val partS = 86400.0 / 9
    val beatS = partS / 9
    val part = Math.floor(secOfDay / partS).toInt()
    val beat = Math.floor((secOfDay - part * partS) / beatS).toInt()

    val digits = listOf(
        (y / 1000) % 10, (y / 100) % 10, (y / 10) % 10, y % 10,
        month / 3 + 1, month % 3 + 1, (dom - 1) / 7 + 1, (dom - 1) % 7 + 1,
        part + 1, beat + 1,
    )
    val ten = digits.joinToString("")
    if (fine <= 0) return ten
    var rem = secOfDay - part * partS - beat * beatS
    var w = beatS
    val fd = StringBuilder()
    for (i in 0 until minOf(fine, FINE_RUNG_NAMES.size)) {
        w /= 9
        val d = minOf(8.0, maxOf(0.0, Math.floor(rem / w))).toInt()
        fd.append(d + 1)
        rem -= d * w
    }
    return "$ten.$fd"
}

/**
 * The span an address names, [start, end) in UTC epoch ms, and its pscale.
 * Whole milliseconds, as a JavaScript Date keeps them: a fraction is dropped
 * toward zero wherever the original makes a Date of it.
 */
class Span(val startMs: Long, val endMs: Long, val pscale: Int) {
    constructor(startMs: Double, endMs: Double, pscale: Int) : this(startMs.toLong(), endMs.toLong(), pscale)
}

private val FINE_ADDRESS = Regex("""^(\d{10})\.([1-9]{1,3})$""")
private val TEN_DIGITS = Regex("""^\d{10}$""")

/** JavaScript's Date.UTC: a month or day out of range rolls into the next, and a year 0..99 is 1900..1999, as there. */
private fun utc(y: Int, month0: Int, day: Int): Double =
    LocalDate.of(if (y in 0..99) 1900 + y else y, 1, 1).plusMonths(month0.toLong()).plusDays((day - 1).toLong()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli().toDouble()

fun addressToSpan(addr: String): Span {
    FINE_ADDRESS.find(addr)?.let { m ->
        val beat = addressToSpan(m.groupValues[1])
        if (beat.pscale != 0) throw IllegalArgumentException("temporal: \"$addr\" has decimals beneath a rung coarser than the beat")
        var start = beat.startMs.toDouble()
        var width = (beat.endMs - beat.startMs).toDouble()
        var pscale = 0
        for (ch in m.groupValues[2]) {
            width /= 9
            start += (ch - '0' - 1) * width
            pscale--
        }
        return Span(start, start + width, pscale)
    }
    if (!TEN_DIGITS.matches(addr)) throw IllegalArgumentException("temporal: \"$addr\" is not a canonical full-width floor-10 address")
    val d = addr.map { it - '0' }
    var depth = 10
    while (depth > 1 && d[depth - 1] == 0) depth--
    val y = d[0] * 1000 + d[1] * 100 + d[2] * 10 + d[3]
    if (depth <= 3) {
        val step = listOf(1000, 100, 10)[depth - 1]
        val base = Math.floorDiv(y, step) * step
        return Span(utc(base, 0, 1), utc(base + step, 0, 1), 10 - depth)
    }
    if (depth == 4) return Span(utc(y, 0, 1), utc(y + 1, 0, 1), 6)
    val season = d[4] - 1
    if (depth == 5) return Span(utc(y, season * 3, 1), utc(y, season * 3 + 3, 1), 5)
    val month = season * 3 + (d[5] - 1)
    if (depth == 6) return Span(utc(y, month, 1), utc(y, month + 1, 1), 4)
    val bandStart = (d[6] - 1) * 7 + 1
    if (depth == 7) return Span(utc(y, month, bandStart), minOf(utc(y, month, bandStart + 7), utc(y, month + 1, 1)), 3)
    val dom = bandStart + (d[7] - 1)
    val dayStart = utc(y, month, dom)
    if (depth == 8) return Span(dayStart, dayStart + 86_400_000, 2)
    val partS = 9_600_000.0
    val pStart = dayStart + (d[8] - 1) * partS
    if (depth == 9) return Span(pStart, pStart + partS, 1)
    val beatS = partS / 9
    val bStart = pStart + (d[9] - 1) * beatS
    return Span(bStart, bStart + beatS, 0)
}

/** The human voicing of an address: "Tuesday 15 July 2026, late afternoon". */
fun voiceAddress(addr: String): String {
    FINE_ADDRESS.find(addr)?.let { m ->
        val tail = m.groupValues[2].mapIndexed { i, ch -> "${FINE_RUNG_NAMES[i]} $ch" }.joinToString(", ")
        return "${voiceAddress(m.groupValues[1])}, $tail"
    }
    val span = addressToSpan(addr)
    val start = Instant.ofEpochMilli(span.startMs).atZone(ZoneOffset.UTC)
    val y = start.year
    val p = span.pscale
    if (p >= 7) return "the ${y}s"
    if (p == 6) return "$y"
    if (p == 5) return "${listOf("winter-quarter", "spring-quarter", "summer-quarter", "autumn-quarter")[(start.monthValue - 1) / 3]} $y"
    if (p == 4) return "${MONTHS[start.monthValue - 1]} $y"
    if (p == 3) return "the week of ${start.dayOfMonth} ${MONTHS[start.monthValue - 1]} $y"
    val day = "${WEEKDAYS[start.dayOfWeek.value % 7]} ${start.dayOfMonth} ${MONTHS[start.monthValue - 1]} $y"
    if (p == 2) return day
    // An interior zero at the day-part names no part: JavaScript reads it as undefined.
    val part = DAY_PARTS.getOrNull(addr[8] - '1') ?: "undefined"
    if (p == 1) return "$day, $part"
    return "$day, $part (beat ${addr[9]})"
}
