package com.phenomemental.pocketbeach.ui

import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Kind
import com.phenomemental.pocketbeach.bsp.Plan
import com.phenomemental.pocketbeach.bsp.parseIsoMs
import com.phenomemental.pocketbeach.bsp.stringify
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** The app's words for times, counts and places. */
object Words {
    private val zone get() = ZoneId.systemDefault()

    /** "today at 10:04", "yesterday at 18:30", "on 25 October", "on 3 March 2025". */
    fun at(ms: Long, nowMs: Long = System.currentTimeMillis()): String {
        val d = Instant.ofEpochMilli(ms).atZone(zone)
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val time = d.toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        val month = d.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        return when (d.toLocalDate()) {
            today -> "today at $time"
            today.minusDays(1) -> "yesterday at $time"
            else -> if (d.year == today.year) "on ${d.dayOfMonth} $month" else "on ${d.dayOfMonth} $month ${d.year}"
        }
    }

    fun at(iso: String?): String? = iso?.let(::parseIsoMs)?.let { at(it) }

    fun days(sinceMs: Long, nowMs: Long = System.currentTimeMillis()): Int = ((nowMs - sinceMs) / (24L * 60 * 60 * 1000)).toInt()

    private val small = listOf("no", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")

    fun number(n: Int): String = small.getOrNull(n) ?: n.toString()

    /** "one block", "12 blocks". */
    fun count(n: Int, one: String, many: String = one + "s"): String = "${number(n)} ${if (n == 1) one else many}"

    fun quoted(text: String?, max: Int = 60): String =
        if (text == null) "an empty line" else "“" + (if (text.length > max) text.take(max - 1).trimEnd() + "…" else text) + "”"

    /** "lines 1 to 9" from the beach's "1-9". */
    fun span(over: String): String = "lines " + over.replace("-", " to ")

    /** Where a write lands, in a sentence: 0.6.3 (Matthew, 31 October), in words, never by a line's number. */
    fun landing(kind: Kind, block: String, address: String, landsAt: String?, wraps: Boolean, standing: String?): String = when (kind) {
        Kind.NEW_BLOCK -> "A new block called $block."
        Kind.WHOLE -> "$block, written whole with the lines shown."
        Kind.CHANGE ->
            if (address.isEmpty()) "The opening line of $block becomes what you write here. Everything beneath it stays as it is."
            else quoted(standing).replaceFirstChar { it.uppercase() } + " in your $block block becomes what you write here. The lines beneath it stay as they are."
        Kind.ADD -> when {
            wraps -> "A new line in your $block block. Its opening is full, so this line wraps it first: the lines there move one step beneath the opening line, and this one begins the next nine."
            address.isEmpty() -> "A new line in your $block block."
            else -> "A new line in your $block block, beneath ${quoted(standing)}."
        }
    }

    /** The bsp() call a plan runs, written out. */
    fun call(plan: Plan): String {
        val c = plan.call ?: return ""
        val parts = c.keys().map { k ->
            val v = c[k]
            val shown = if (k == "content" && v is JsObj && plan.kind == Kind.CHANGE) "{the whole block, its opening line changed}"
                else if (k == "content" && v is JsObj && plan.kind == Kind.WHOLE) "{the whole block, as shown}" else stringify(v) ?: ""
            "$k: $shown"
        }
        return "bsp(" + parts.joinToString(", ") + ")"
    }
}
