package com.phenomemental.pocketbeach.bsp

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/*
 * Sundial 0.10 (spine:pocket-beach 1.1111787192, 1.1111787197 and the
 * decisions at 1.111178719915 and 1.111178719916): his day kept in Sundial's
 * form, the beach's native way. Time's arithmetic stays here, in code, never
 * in a block (pscale://sundial): the address of a moment at a named grain;
 * the cook names the grain in words ("at today", "each of THE MOMENTS at its
 * minute") and this only computes it.
 *
 * - A day's line is one plain text at the day's address in Sundial's form
 *   (Mark.write), written only where the day is empty or already holds a line
 *   in Sundial's form; a line of his own is never overwritten (days.js:316-331).
 * - Each moment he spoke goes at its minute; when that minute holds one
 *   already, beneath it at its breath; then its second; two within a second
 *   join the line there.
 * - A moment's instant, and the day line's keep instant, go in the
 *   underscore's hidden directory at 3 (sunstone 1.4), as the mark shape's
 *   field 3: {"_": {"_": "his words", "3": "2026-11-03T07:31:04.120Z"}}.
 *   collectUnderscore reads through it to the words; no line walker steps in.
 * - All in UTC, the beach's day: no timezone is stored (pscale://sundial 8.3).
 */
object Keep {
    /** A moment he spoke: his words and their instant. His only, never hers (1.111178719915 (d)). */
    data class Moment(val words: String, val atMs: Long)

    /** The grains a recipe names: the day down to the second (pscale://sundial 2). */
    enum class Grain(val digits: Int, val fine: Int) {
        DAY(8, 0), GATHERING(9, 0), BEAT(10, 0), MINUTE(10, 1), BREATH(10, 2), SECOND(10, 3);

        /** One rung finer, or null below the second. */
        fun finer(): Grain? = values().getOrNull(ordinal + 1)
    }

    /**
     * The grain a word names, as a recipe writes it: "today", "tomorrow" and
     * "its day" are the day; "this gathering" or "its gathering", the beat,
     * the minute, the breath and the second likewise. Null for any other word.
     */
    fun grainOf(words: String): Grain? = when (words.trim().lowercase().replace(Regex("""^(?:(?:at|this|its)\s+)+"""), "")) {
        "today", "tomorrow", "day" -> Grain.DAY
        "gathering" -> Grain.GATHERING
        "beat" -> Grain.BEAT
        "minute" -> Grain.MINUTE
        "breath" -> Grain.BREATH
        "second" -> Grain.SECOND
        else -> null
    }

    /** The full-width address of [ms] at [grain]: 2026421300, 2026421330, 2026421338, 2026421338.4, 2026421338.42, 2026421338.423. */
    fun grainAddress(ms: Long, grain: Grain): String {
        if (grain.fine > 0) return momentToAddress(ms, grain.fine)
        val ten = momentToAddress(ms)
        return ten.take(grain.digits).padEnd(TEMPORAL_FLOOR, '0')
    }

    /** The day after [ms]'s day, as a day's address. */
    fun tomorrowAddress(ms: Long): String = grainAddress(ms + 86_400_000L, Grain.DAY)

    /** A UTC date's day address. */
    fun dayAddress(date: LocalDate): String = Days.dayAddress(date)

    /** The beach's day (UTC) of [ms]. */
    fun dayOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()

    /** The keys walked from a clock's root to [address] at [grain]: a '0' digit is the underscore; the fine digits follow. */
    fun keys(address: String, grain: Grain): List<String> {
        val ten = address.substringBefore('.')
        val fine = address.substringAfter('.', "")
        return ten.take(grain.digits).map { if (it == '0') "_" else "$it" } + fine.take(grain.fine).map { "$it" }
    }

    /** The node at [keys] beneath [clock], made where it isn't; a line standing alone is wrapped as its node's own line. */
    fun walk(clock: JsObj, keys: List<String>): JsObj {
        var node = clock
        for (k in keys) {
            val next = when (val c = node[k]) {
                is JsObj -> c
                is JsStr -> JsObj.of("_" to c)
                else -> JsObj()
            }
            node[k] = next
            node = next
        }
        return node
    }

    /** The node at [keys], or null when it doesn't stand. */
    fun find(clock: Js?, keys: List<String>): Js? {
        var node: Js? = clock
        for (k in keys) node = (node as? JsObj)?.get(k) ?: return null
        return node
    }

    /** A node's own line: the string at its underscore, read through a hidden directory. */
    fun lineOf(node: Js?): String? = when (node) {
        is JsStr -> node.value
        is JsObj -> collectUnderscore(node)
        else -> null
    }?.takeIf { it.isNotBlank() }

    /** The instant kept with a node's line, in its hidden directory at 3; null when none. */
    fun instantOf(node: Js?): Long? = ((node as? JsObj)?.get("_") as? JsObj)?.get("3")?.let { (it as? JsStr)?.value }?.let(::parseIsoMs)

    private fun plain(s: String) = Telling.plain(s)

    /** A line with its instant beside it, in the underscore's hidden directory. */
    fun stamped(words: String, ms: Long): JsObj = JsObj.of("_" to str(words.trim()), "3" to str(isoString(ms)))

    /** Whether [text] is a line Sundial wrote: one whose last line names its places, or "In your own words." (days.js:316-331). */
    fun sundials(text: String?): Boolean = text != null && Mark.read(text).let { it.places.isNotEmpty() || it.own }

    /** What became of one moment: where it landed, or that it already stood. */
    data class Landed(val moment: Moment, val address: String?, val already: Boolean)

    /**
     * [m] written into [clock] at its [grain] (the minute, unless the recipe
     * names another): beneath at the next rung while the place holds another
     * moment; at the second, two within it join the line there. A moment
     * already standing anywhere on its way is not written again.
     */
    fun moment(clock: JsObj, m: Moment, grain: Grain = Grain.MINUTE): Landed {
        var g = grain
        while (true) {
            val address = grainAddress(m.atMs, g)
            val node = walk(clock, keys(address, g))
            val had = lineOf(node)
            when {
                had == null -> {
                    node["_"] = stamped(m.words, m.atMs)
                    return Landed(m, address, false)
                }
                had.split("\n\n").any { plain(it) == plain(m.words) } -> return Landed(m, address, true)
                else -> {
                    val f = g.finer()
                    if (f == null) {
                        // Two within one second: the line joins the one there.
                        val dir = node["_"] as? JsObj
                        if (dir != null) dir["_"] = str(had + "\n\n" + m.words.trim()) else node["_"] = str(had + "\n\n" + m.words.trim())
                        return Landed(m, address, false)
                    }
                    g = f
                }
            }
        }
    }

    /** What became of the day's line. */
    enum class DayLine { WRITTEN, SAME, HIS }

    /**
     * The day's [text] (in Sundial's form) at [day]'s address, with the keep's
     * instant [keptMs]: only where the day holds nothing yet or a line Sundial
     * wrote; a line of his own stands exactly as it is (HIS).
     */
    fun dayLine(clock: JsObj, day: LocalDate, text: String, keptMs: Long): DayLine {
        val node = walk(clock, keys(dayAddress(day), Grain.DAY))
        val had = lineOf(node)
        if (had != null && plain(had) == plain(text)) return DayLine.SAME
        if (had != null && !sundials(had)) return DayLine.HIS
        node["_"] = stamped(text, keptMs)
        return DayLine.WRITTEN
    }

    /** Whether the day at [day] holds a line of his own (not Sundial's), which a keep leaves as it stands. */
    fun hisOwnLine(clock: Js?, day: LocalDate): String? = lineOf(find(clock, keys(dayAddress(day), Grain.DAY)))?.takeIf { !sundials(it) }

    /** The day's line at [day], whatever wrote it. */
    fun dayText(clock: Js?, day: LocalDate): String? = lineOf(find(clock, keys(dayAddress(day), Grain.DAY)))

    /**
     * A booking at a later day, as Sundial sets a line ahead (sundial.html:4951-4955):
     * his words, then the places line "What’s ahead."; beside an ahead line
     * already there, a line of it; beside a line of his own, after it.
     */
    fun booking(clock: JsObj, day: LocalDate, words: String) {
        val node = walk(clock, keys(dayAddress(day), Grain.DAY))
        val had = lineOf(node)
        val ahead = Mark.placesWords(listOf(6)).replaceFirstChar { it.uppercase() } + "."
        node["_"] = when {
            had == null -> str(words.trim() + "\n" + ahead)
            had.split("\n").any { plain(it) == plain(words) } -> return
            sundials(had) -> {
                val rows = had.split("\n").toMutableList()
                rows.add(rows.size - 1, words.trim())
                str(rows.joinToString("\n"))
            }
            else -> str(had + "\n\n" + words.trim())
        }
    }

    // ── his days, read back ──

    /** A moment kept beneath a day: its address, his words, and its instant when one was kept with it. */
    data class Kept(val address: String, val words: String, val atMs: Long?)

    /**
     * A day kept: its address, its line (null when only moments or pieces
     * stand), the moments beneath it, the places it rests in, the instant it
     * was kept (its hidden directory, else the newest of its P6 pieces), and,
     * for a day before Sundial, the pieces of it his P6 holds, by place.
     */
    data class KeptDay(
        val address: String,
        val text: String?,
        val moments: List<Kept>,
        val places: List<Int>,
        val keptMs: Long?,
        val pieces: List<Pair<Int, String>> = emptyList(),
    ) {
        val date: LocalDate? get() = Clock.dateOf(address)
        /** Its title, as Sundial reads a day; for a day in his own words, its first sentence. */
        val title: String get() = text?.let { t ->
            val r = Mark.read(t)
            if (r.places.isNotEmpty() || r.own || r.lines.isNotEmpty()) r.title else Regex("""^.{1,120}?[.!?](?=\s|$)""").find(t.trim())?.value ?: t.trim().take(120)
        } ?: moments.firstOrNull()?.words?.let { Regex("""^.{1,120}?[.!?](?=\s|$)""").find(it.trim())?.value ?: it.trim().take(120) } ?: ""
    }

    /** P6's portals as Sundial's pools (mark.js:11-24, `at`). */
    val PORTAL_POOL = mapOf("1.1" to 1, "1.2" to 2, "1.3" to 3, "2.1" to 4, "2.2" to 5, "2.3" to 6)

    /**
     * Every day kept in his now, up to and including [today] (the beach's
     * day), oldest first: a day with a line, a moment beneath it, or a piece
     * in his [p6]. Its places are read from its own last line (mark.js:131-136);
     * a day with none rests where his P6 holds a piece of it, by the day's
     * address at 2 of each piece (Days.kt). A day ahead (a booking) is no day kept.
     */
    fun days(clock: Js?, p6: Js?, today: LocalDate): List<KeptDay> {
        val todayAt = dayAddress(today)
        val found = LinkedHashMap<String, Pair<String?, MutableList<Kept>>>()
        fun walkDays(node: Js?, digits: String) {
            val o = node as? JsObj ?: return
            for (k in o.keys()) {
                val d = if (k == "_") '0' else k.singleOrNull() ?: continue
                val ok = when {
                    digits.isEmpty() -> d in '1'..'9'
                    digits.length < 4 -> d in '0'..'9'
                    else -> d in '1'..'9'
                }
                if (!ok) continue
                val next = o[k]
                if (digits.length == 7) {
                    val address = digits + d + "00"
                    if (address > todayAt) continue
                    val moments = mutableListOf<Kept>()
                    collectMoments(next, address.take(8), moments)
                    val text = lineOf(next)
                    if (text != null || moments.isNotEmpty()) found[address] = text to moments
                } else walkDays(next, digits + d)
            }
        }
        walkDays(clock, "")
        // His P6, read by each piece's day.
        val pieces = LinkedHashMap<String, MutableList<Triple<Int, String, Long?>>>()
        (p6 as? JsObj)?.let { o ->
            for ((at, pool) in PORTAL_POOL) {
                val (ring, k) = at.split('.')
                val portal = (o[ring] as? JsObj)?.get(k) as? JsObj ?: continue
                for (j in 1..9) {
                    val piece = portal["$j"] as? JsObj ?: continue
                    val words = lineOf(piece) ?: continue
                    val day = (piece["2"] as? JsStr)?.value?.takeIf { it.length == 10 } ?: continue
                    if (day > todayAt) continue
                    pieces.getOrPut(day) { mutableListOf() } += Triple(pool, words, (piece["3"] as? JsStr)?.value?.let(::parseIsoMs))
                }
            }
        }
        val all = (found.keys + pieces.keys).distinct().sorted()
        return all.map { address ->
            val (text, moments) = found[address] ?: (null to mutableListOf())
            val own = text?.let { Mark.placesOf(it) }.orEmpty()
            val ps = pieces[address].orEmpty()
            val places = own.ifEmpty { Mark.inOrder(ps.map { it.first }.toSet()) }
            val dayNode = find(clock, keys(address, Grain.DAY))
            val kept = instantOf(dayNode) ?: ps.mapNotNull { it.third }.maxOrNull()
            KeptDay(address, text, moments.sortedBy { it.address }, places, kept, if (own.isEmpty()) ps.map { it.first to it.second } else emptyList())
        }
    }

    /** Every line beneath a day node, as moments: gathering, beat and the fine rungs. */
    private fun collectMoments(node: Js?, day8: String, out: MutableList<Kept>, path: String = "") {
        val o = node as? JsObj ?: return
        for (k in digitKeys(o)) {
            val child = o[k]
            val p = path + k
            val address = when {
                p.length <= 2 -> (day8 + p).padEnd(TEMPORAL_FLOOR, '0')
                else -> day8 + p.take(2) + "." + p.drop(2)
            }
            lineOf(child)?.let { out += Kept(address, it, instantOf(child)) }
            if (p.length < 5) collectMoments(child, day8, out, p)
        }
    }

    /** How many days rest in each place, in pool order (mark.js:141-147). */
    fun counts(days: List<KeptDay>): List<Int> = Mark.counts(days.map { it.places })

    /**
     * The latest [n] days resting in [pool], newest last, each whole up to
     * [max] characters with "…" past it (1.111178719916), a day before Sundial
     * with the pieces of it resting there.
     */
    fun restingIn(days: List<KeptDay>, pool: Int, n: Int = 9, max: Int = 1500): List<Pair<KeptDay, String>> =
        days.filter { pool in it.places }.takeLast(n).map { d ->
            val body = buildString {
                d.text?.let { append(it.trim()) }
                val mine = d.pieces.filter { it.first == pool }.map { it.second }
                if (d.text == null && mine.isNotEmpty()) append(mine.joinToString("\n"))
                else if (mine.isNotEmpty()) append("\n" + mine.joinToString("\n") { "· $it" })
                if (d.text == null && mine.isEmpty()) append(d.moments.joinToString("\n") { it.words })
            }
            d to (if (body.length <= max) body else body.take(max - 1).trimEnd() + "…")
        }

    /** The last day kept before [today], whole, with its tomorrow line (for the greeting). */
    fun lastDay(days: List<KeptDay>, today: LocalDate): KeptDay? = days.lastOrNull { it.date?.isBefore(today) == true && (it.text != null || it.moments.isNotEmpty()) }

    /** The tomorrow line a day said, in his words as it stands; null when none. */
    fun tomorrowOf(day: KeptDay?): String? = day?.text?.let { Mark.read(it).tomorrow }?.takeIf { it.isNotBlank() }
}
