package com.phenomemental.pocketbeach.bsp

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Her window and her turn, as the app gives them to her. Sundial 0.12, Gemma
 * released (spine:pocket-beach 1.111178719991): her window holds the time
 * alone, and each turn the time he said it and his words. Here too: the time
 * stamped on his message, the greeting's turn, the app's copies of his blocks
 * as she last saw them, her name, and the lines of a block as his cook reads
 * them.
 */
object HerWindow {
    /** Her heart: how she is with him, in his words; his to change. */
    const val HEART = "heart:pocket-companion"

    /** Her ways: how she works with what he asks; his to change. */
    const val WAYS = "ways:pocket-companion"

    /** Her blocks, his to change on the Write screen; never her proposals' to change. */
    val HERS = setOf(HEART, WAYS)

    private val WHEN = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm", Locale.UK)

    /** Her name, as his shell gives it at her place ("Matthew calls me Gemma."). */
    fun herName(handle: String, pages: Pages): String? = Talking.name(Hatch.shellOf(handle), pages, Hatch.HER)

    const val TALK_LINE = "The app: nothing is being kept or set out from this. Just talk with him, in a sentence or two."

    // ── what she has seen: the app's copies of his blocks ──

    /** His blocks, as she is shown them: not hers, not the app's own, not her shell, not his clocks; never his passport, shell or history. */
    fun his(pages: Pages, shell: String = HelloWorld.SHELL): List<String> = pages.shore().mine.map { it.name }
        .filter { it != shell && it !in HERS && !Keeping.own(it, pages) && it != Asks.LOG && !it.startsWith("bench-") }
        .filter { !it.startsWith("passport:") && !it.startsWith("shell:") && !it.startsWith("history:") }
        .sorted()

    /** The app's copies of his blocks, as JSON: what she has now seen. */
    fun copies(pages: Pages, storage: BlockStorage): String =
        stringify(JsObj.of(*his(pages).mapNotNull { n -> storage.read(n)?.let { n to JsStr(it) } }.toTypedArray()))!!

    // ── the turn ──

    private val HER_OPENING = Regex("""\b(?:your|her)\s+(?:opening|first)\s+line\b""", RegexOption.IGNORE_CASE)

    /** Whether [said] asks to change her shell's opening line: his to change, on the Write screen. */
    fun asksHerOpening(said: String): Boolean = HER_OPENING.containsMatchIn(said.replace('\u2019', '\''))

    /**
     * Rockpool: her turn as his cook hands it: the day and the time, what has
     * changed, how his last proposal ended, what was read for her (only when
     * READ read it), the safety line when his words say he isn't safe, the
     * lines the walker handed her, then his words.
     */
    fun turnHanded(said: String, now: ZonedDateTime, pages: Pages, handed: List<Cook.Handed>, changes: String? = null, told: String? = null, lastMs: Long? = null): String = buildString {
        appendLine("(${stamp(now, lastMs)})")
        changes?.let { appendLine("($it)") }
        told?.let { appendLine("(How his last proposal ended: $it)") }
        for (h in handed) for (m in h.material) appendLine(m)
        for (h in handed) appendLine("(${h.line})")
        if (handed.isEmpty()) appendLine("($TALK_LINE)")
        append(said.trim())
    }

    /**
     * The time stamped on one of his messages in her turn, as the beach gives
     * it: the day and the clock time where he is, the part of the day in the
     * beach's own words (its nine parts, deep night to night, as its clock
     * voices an address: [DAY_PARTS]), and, when his message before
     * ([lastMs]) was more than an hour earlier, how long ago that was.
     * Her one conversation stays open all day (0.10.2), so the time it
     * opened goes stale: on his Pixel on 9 November, at 22:59 and 23:19, she
     * asked "What's on for today?". Every turn carries its own time now; the
     * conversation is never opened again for it. It opens "It is", so it is
     * never played back as hers (echoLines).
     */
    fun stamp(now: ZonedDateTime, lastMs: Long? = null): String {
        val ms = now.toInstant().toEpochMilli()
        val part = DAY_PARTS.getOrNull(momentToAddress(ms)[8] - '1')
        val gap = lastMs?.let { ms - it }?.takeIf { it > HOUR_MS }?.let { " His last message was ${since(it)} ago." }.orEmpty()
        return "It is ${WHEN.format(now)}${part?.let { ", $it" }.orEmpty()}.$gap"
    }

    private const val HOUR_MS = 3_600_000L

    /** How long [ms] is, in his words: "an hour and 20 minutes", "3 hours", "a day and 2 hours", "4 days". */
    fun since(ms: Long): String {
        fun count(n: Long, one: String) = if (n == 1L) (if (one == "hour") "an hour" else "a $one") else "$n ${one}s"
        val days = ms / 86_400_000L
        val hours = (ms % 86_400_000L) / HOUR_MS
        val minutes = (ms % HOUR_MS) / 60_000L
        return if (days > 0) count(days, "day") + (if (hours > 0) " and " + count(hours, "hour") else "")
        else count(hours, "hour") + (if (minutes > 0) " and " + count(minutes, "minute") else "")
    }

    /**
     * Sundial 0.12, Gemma released (spine:pocket-beach 1.111178719991): her
     * window holds the time alone. Nothing of his shell, no friend line, no
     * three lines, no recipe: nothing added to shape how she answers.
     */
    fun released(now: ZonedDateTime): String = "It is ${WHEN.format(now)} as this conversation opens."

    /** Sundial 0.12: her turn, released: the time as the beach gives it, then his words, and nothing else. */
    fun releasedTurn(said: String, now: ZonedDateTime, lastMs: Long? = null): String = "(${stamp(now, lastMs)})\n" + said.trim()

    /**
     * The lines of a turn she must never play back: the time, how his last
     * proposal ended, and the app's line. Not what has changed, nor what was
     * read for her, nor the safety line: those hold his words, or words she
     * may well say again.
     */
    fun echoLines(turn: String): List<String> = turn.lines().filter { it.startsWith("(") && it.endsWith(")") }.map { it.removePrefix("(").removeSuffix(")") }
        .filter { it.startsWith("It is ") || it.startsWith("The app: ") || it.startsWith("How his last proposal ended") }

    private val MY_FOUR = Regex("""\bmy\s+(passport|stash|shell|history)\b""", RegexOption.IGNORE_CASE)

    /** 0.8.1: his own four blocks his words name as his ("Read my passport"), once he has hatched. */
    fun hisFour(said: String, storage: BlockStorage): List<String> {
        val h = Hatch.hatched(storage) ?: return emptyList()
        return MY_FOUR.findAll(said.replace('\u2019', '\'')).map { "${it.groupValues[1].lowercase()}:$h" }.distinct().filter { storage.read(it) != null }.toList()
    }

    /** A block's name as she is told it: his own four by their role ("his passport"), any other by its name. */
    fun called(name: String): String = Regex("""^(passport|stash|shell|history):""").find(name)?.groupValues?.get(1) ?: name

    /** A place in a long block: read whole up to this many characters. */
    const val PLACE_MAX = 3000

    // ── lines ──

    private val STAMP = Regex("""^\d{4}-\d\d-\d\dT\d\d:\d\d""")

    /** A block's lines, by address (1, 2, 2.1…), each its text; the arrival stamps an append leaves are no lines. */
    fun linesOf(block: Js): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        fun walk(node: Js?, at: String) {
            val o = node as? JsObj ?: return
            for (k in 1..9) {
                val c = o["$k"] ?: continue
                val a = if (at.isEmpty()) "$k" else "$at.$k"
                val text = (c as? JsStr)?.value ?: collectUnderscore(c)
                if (!text.isNullOrBlank() && !STAMP.containsMatchIn(text)) out += a to text.trim()
                walk(c, a)
            }
        }
        walk(block, "")
        return out
    }
}
