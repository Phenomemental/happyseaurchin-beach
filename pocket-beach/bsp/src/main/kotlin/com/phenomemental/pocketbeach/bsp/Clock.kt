package com.phenomemental.pocketbeach.bsp

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * 0.9.1 (spine:pocket-beach 1.1111787164): a clock as the Block screen shows
 * it. A block of floor 10 or more whose opening names spine:now, or the
 * app's own now (whose opening, as 0.6 wrote it, says "on the clock"), is a
 * clock: it opens at today, its rungs above as words, the days either side
 * to step to. Its addresses are always full width (2026421200): eight digits
 * alone would be read from the floor and land on another date.
 */
object Clock {
    fun isClock(name: String, floor: Int, opening: String?): Boolean =
        floor >= TEMPORAL_FLOOR && (opening?.contains("spine:now") == true || name == Days.NOW)

    /** A day's full-width address: yyyy s m w d, then 00. */
    fun dayOf(date: LocalDate): String = Days.dayAddress(date)

    /**
     * The rung an address stands at, by its digits that aren't zero: year (4),
     * season (5), month (6), week (7), day (8); Sundial 0.10, the gathering
     * (9), the beat (10), and past the point the minute (11), the breath (12)
     * and the second (13).
     */
    fun level(address: String): Int {
        val fine = address.substringAfter('.', "")
        if (fine.isNotEmpty()) return 10 + fine.length.coerceAtMost(3)
        return address.trimEnd('0').length.coerceIn(0, 10)
    }

    /** The date a day's address names, or null when it names no day. */
    fun dateOf(address: String): LocalDate? {
        // Sundial 0.10: a gathering, beat or moment names its day too.
        if (level(address) > 8) return dateOf(address.take(8) + "00")
        if (address.length != TEMPORAL_FLOOR || level(address) != 8 || address.any { !it.isDigit() }) return null
        val d = address.map { it - '0' }
        val year = d[0] * 1000 + d[1] * 100 + d[2] * 10 + d[3]
        val month0 = (d[4] - 1) * 3 + (d[5] - 1)
        val dom = (d[6] - 1) * 7 + d[7]
        if (d[4] !in 1..4 || d[5] !in 1..3 || d[6] !in 1..5 || d[7] !in 1..7) return null
        return runCatching { LocalDate.of(year, month0 + 1, dom) }.getOrNull()
    }

    /** A day as he says it: "Today, Monday 2 November", "Yesterday, …", "Tomorrow, …", else "Monday 9 November", with the year when it isn't this one. */
    fun said(date: LocalDate, today: LocalDate): String {
        val plain = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.UK) + " " + date.dayOfMonth + " " + date.month.getDisplayName(TextStyle.FULL, Locale.UK) +
            if (date.year != today.year) " ${date.year}" else ""
        return when (date) {
            today -> "Today, $plain"
            today.minusDays(1) -> "Yesterday, $plain"
            today.plusDays(1) -> "Tomorrow, $plain"
            else -> plain
        }
    }

    private val SEASONS = listOf("January to March", "April to June", "July to September", "October to December")

    /** A rung as words: "2026", "October to December 2026", "November 2026", "1 to 7 November", or the day. */
    fun rungSaid(address: String, today: LocalDate, zone: java.time.ZoneId = java.time.ZoneOffset.UTC): String {
        // Sundial 0.10: the gathering by its name, a beat or a moment by the time it began on the phone's clock.
        val lv = level(address)
        if (lv >= 9) {
            val day = dateOf(address)?.let { said(it, today) } ?: address
            val part = DAY_PARTS.getOrNull(address[8] - '1') ?: return address
            if (lv == 9) return "$day, $part"
            val start = runCatching { addressToSpan(if (lv == 10) address.take(10) else address) }.getOrNull() ?: return "$day, $part"
            val t = java.time.Instant.ofEpochMilli(start.startMs).atZone(zone).toLocalTime()
            val hm = "%02d:%02d".format(t.hour, t.minute)
            return when (lv) {
                10 -> "$day, $part, from $hm"
                11 -> "$day, $hm"
                else -> "$day, $hm:%02d".format(t.second)
            }
        }
        val d = address.padEnd(TEMPORAL_FLOOR, '0').map { it - '0' }
        val year = d[0] * 1000 + d[1] * 100 + d[2] * 10 + d[3]
        val month0 = (d[4] - 1) * 3 + (d[5] - 1)
        return when (level(address)) {
            in 0..4 -> "$year"
            5 -> "${SEASONS.getOrElse(d[4] - 1) { "season ${d[4]}" }} $year"
            6 -> runCatching { java.time.Month.of(month0 + 1).getDisplayName(TextStyle.FULL, Locale.UK) + " $year" }.getOrDefault("month ${d[5]}")
            7 -> runCatching {
                val m = java.time.YearMonth.of(year, month0 + 1)
                val first = (d[6] - 1) * 7 + 1
                "$first to ${minOf(first + 6, m.lengthOfMonth())} ${m.month.getDisplayName(TextStyle.FULL, Locale.UK)}"
            }.getOrDefault("week ${d[6]}")
            else -> dateOf(address.take(8) + "00")?.let { said(it, today) } ?: address
        }
    }

    /** The rungs above a day or rung, coarsest first: the year, the season, the month, the week, each at its full-width address. */
    fun trail(address: String): List<String> {
        val lv = level(address)
        val coarse = (4..10).filter { it < lv }.map { address.take(it).padEnd(TEMPORAL_FLOOR, '0') }
        val fine = (11 until lv).map { address.take(10) + "." + address.substringAfter('.').take(it - 10) }
        return coarse + fine
    }
}
