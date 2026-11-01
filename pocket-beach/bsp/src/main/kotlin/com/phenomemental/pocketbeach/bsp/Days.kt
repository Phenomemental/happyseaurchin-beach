package com.phenomemental.pocketbeach.bsp

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Rockpool: his days, the beach's way (spine:pocket-beach 1.11117871363, read
 * with function:ahead). His now is one clock: each day's line at its date,
 * the rungs above at their levels, a booking at its day, nothing else. His
 * hand is ahead:<handle>, piles by rung (1 within hours, 2 today, 3 this
 * week, 4 this month, 5 this season, 6 this year, 7 this decade), each card
 * his sentence with its stamp beneath at 3. His P6 is p6:<handle>, his mirror
 * of spine:p6: each sentence appended beneath its portal (1.1 to 1.3 the
 * surface, 2.1 to 2.3 the deep), the day it came from at 2 and its stamp at
 * 3. A place holds nine: past nine, the app says so and leaves it out.
 *
 * TELL MY DAY (cook 6) sets out a [DayCard] for his yes; [blocks] is what one
 * yes writes. [moveOut] carries his old telling and the six out of his now,
 * once, after a whole copy is saved.
 */
object Days {
    fun aheadOf(handle: String) = "ahead:$handle"
    fun p6Of(handle: String) = "p6:$handle"

    /** His clock keeps its name in the pocket, since his window and manifest name it. */
    const val NOW = Landing.NOW

    /** Where the old telling goes when it retires: the hidden region keeps it. */
    const val RETIRED_TELLING = "retired-telling"

    val PILES = listOf("within hours", "today", "this week", "this month", "this season", "this year", "this decade")
    val PORTALS = listOf("1.1", "1.2", "1.3", "2.1", "2.2", "2.3")
    val PORTAL_NAMES = mapOf("1.1" to "Facts", "1.2" to "Feelings", "1.3" to "Fictions", "2.1" to "Purpose", "2.2" to "Decision", "2.3" to "Outcomes")
    val RUNG_LEVELS = listOf("week", "month", "season", "year")

    /** One line of his day, where it goes: a portal of his P6, a pile of his hand, a booking or a rung in his now, or the day's line. */
    sealed class Where {
        data class Portal(val at: String) : Where()
        data class Pile(val n: Int) : Where()
        data class Booking(val date: LocalDate) : Where()
        data class Rung(val level: String) : Where()
        object DayLine : Where()
        /** A piece she couldn't place, waiting for him. */
        object Unplaced : Where()
    }

    fun name(w: Where): String = when (w) {
        is Where.Portal -> PORTAL_NAMES[w.at] ?: w.at
        is Where.Pile -> "Your hand: ${PILES.getOrElse(w.n - 1) { "pile ${w.n}" }}"
        is Where.Booking -> "Booked for " + w.date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM", java.util.Locale.UK))
        is Where.Rung -> "This ${w.level}, the rung above"
        Where.DayLine -> "Today's line on your clock"
        Where.Unplaced -> "Not placed yet"
    }

    data class Line(val words: String, val where: Where, val ticked: Boolean = true, val from: Where? = null) {
        val moved: Boolean get() = from != null && from != where
    }

    data class Intent(val words: String, val pile: Int?, val date: LocalDate?, val by: String?)
    data class Rung(val level: String, val words: String)

    /** His day as it is set out for his yes. */
    data class DayCard(val handle: String, val telling: String, val lines: List<Line>, val atMs: Long) {
        fun describe(): String = buildString {
            val kept = lines.filter { it.ticked }
            append("${kept.count { it.where is Where.Portal }} in your P6")
            append(", ${kept.count { it.where is Where.Pile }} on your hand")
            append(", ${kept.count { it.where is Where.Booking }} booked")
            append(", ${kept.count { it.where is Where.Rung }} on the rungs")
            if (kept.any { it.where == Where.DayLine }) append(", and today's line")
        }

        fun tick(i: Int) = copy(lines = lines.mapIndexed { k, l -> if (k == i) l.copy(ticked = !l.ticked) else l })
        fun move(i: Int, to: Where) = copy(lines = lines.mapIndexed { k, l -> if (k == i) l.copy(where = to, from = l.from ?: l.where, ticked = true) else l })
    }

    /** What his day sets out: the day's line, each piece beneath its portal (or waiting for him), his cards, his rungs. */
    fun landing(handle: String, telling: String, pieces: List<String>, portals: List<String?>, cards: List<Intent>, rungs: List<Rung>, now: ZonedDateTime): DayCard {
        val lines = mutableListOf<Line>()
        if (telling.isNotBlank()) lines += Line(telling.trim(), Where.DayLine)
        pieces.forEachIndexed { i, p -> lines += Line(p, portals.getOrNull(i)?.let { Where.Portal(it) } ?: Where.Unplaced, ticked = portals.getOrNull(i) != null) }
        for (c in cards) lines += Line(c.words, c.date?.let { Where.Booking(it) } ?: Where.Pile(c.pile ?: 3))
        for (r in rungs) lines += Line(r.words, Where.Rung(r.level))
        return DayCard(handle, telling, lines, now.toInstant().toEpochMilli())
    }

    // ── what he means to do, and the rungs ──

    private fun plain(s: String) = Telling.plain(s)

    /** His time words, if he keeps them (Settings): the shelves' words, tomorrow to the year. */
    fun timeWords(storage: BlockStorage): JsObj? = storage.read(Whens.BLOCK)?.let { runCatching { parseJson(it) }.getOrNull() } as? JsObj

    /** The pieces that say what he means to do (cook 9.9's words), each sized: a date is a booking, else the pile its time words name, else this week. */
    fun intents(pieces: List<String>, words: List<String>, today: LocalDate, timeWords: JsObj?): List<Intent> = pieces.mapNotNull { p ->
        val said = " ${plain(p)} "
        if (words.none { w -> said.contains(" ${plain(w)} ") }) return@mapNotNull null
        size(p, today, timeWords).let { (pile, date, by) -> Intent(p.trim(), pile, date, by) }
    }

    /** A piece's size: a date it names (a booking), or a pile; and the words that said so. */
    fun size(piece: String, today: LocalDate, timeWords: JsObj? = null): Triple<Int?, LocalDate?, String?> {
        val said = " ${plain(piece)} "
        fun has(vararg ws: String) = ws.firstOrNull { said.contains(" $it ") }
        has("tomorrow")?.let { return Triple(null, today.plusDays(1), it) }
        has("tonight", "today", "this morning", "this afternoon", "this evening", "later today")?.let { return Triple(2, null, it) }
        has("in an hour", "in a minute", "right now", "soon", "shortly", "in a bit")?.let { return Triple(1, null, it) }
        for (day in DayOfWeek.values()) {
            val n = day.name.lowercase()
            if (said.contains(" $n ")) {
                val ahead = ((day.value - today.dayOfWeek.value + 7) % 7).let { if (it == 0) 7 else it }
                return Triple(null, today.plusDays(ahead.toLong()), n.replaceFirstChar(Char::uppercase))
            }
        }
        val w = Whens.of(piece, today, timeWords ?: Whens.first())
        if (w != null) {
            val pile = when (w.shelf) {
                "2.1" -> return Triple(null, today.plusDays(1), w.by)
                "2.2" -> 3
                "2.3" -> 4
                "2.4" -> 5
                else -> 6
            }
            return Triple(pile, null, w.by)
        }
        has("this decade", "in ten years", "one day", "someday")?.let { return Triple(7, null, it) }
        return Triple(3, null, null)
    }

    private val RUNG_WORDS = mapOf("week" to listOf("this week"), "month" to listOf("this month"), "season" to listOf("this season", "this autumn", "this winter", "this spring", "this summer"), "year" to listOf("this year"))

    /** The pieces that name a rung: this week, this month, this season, this year. */
    fun rungs(pieces: List<String>): List<Rung> = pieces.mapNotNull { p ->
        val said = " ${plain(p)} "
        RUNG_WORDS.entries.firstOrNull { (_, ws) -> ws.any { said.contains(" $it ") } }?.let { (level, _) -> Rung(level, p.trim()) }
    }

    // ── the writes ──

    /** The day's address on the clock, full width (yyyy s m w d 00), and its rungs': the week, month, season, year by their digits. */
    fun dayAddress(date: LocalDate): String = momentToAddress(date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()).take(8) + "00"

    private fun rungDigits(level: String): Int = when (level) { "week" -> 7; "month" -> 6; "season" -> 5; else -> 4 }

    /** What one yes writes: his now, his ahead, his P6, each whole; and what couldn't go (past nine). */
    class Written(val blocks: List<Pair<String, JsObj>>, val over: List<String>)

    fun blocks(l: DayCard, read: (String) -> Js?, nowMs: Long = l.atMs): Written {
        val over = mutableListOf<String>()
        val stamp = isoString(nowMs)
        val today = java.time.Instant.ofEpochMilli(l.atMs).atZone(ZoneOffset.UTC).toLocalDate()
        val kept = l.lines.filter { it.ticked }
        val out = mutableListOf<Pair<String, JsObj>>()

        // His now: the day's line, bookings, rungs.
        val nowLines = kept.filter { it.where == Where.DayLine || it.where is Where.Booking || it.where is Where.Rung }
        if (nowLines.isNotEmpty()) {
            var clock = (read(NOW) as? JsObj)?.let { parseJson(stringify(it)!!) as JsObj } ?: Landing.emptyClock(NOW_OPENING)
            for (line in nowLines) {
                val digits = when (val w = line.where) {
                    Where.DayLine -> dayAddress(today).take(8)
                    is Where.Booking -> dayAddress(w.date).take(8)
                    is Where.Rung -> dayAddress(today).take(rungDigits(w.level))
                    else -> continue
                }
                clock = voiced(clock, digits, line.words)
            }
            out += NOW to clock
        }

        // His hand: each card at its pile, nine at most.
        val cards = kept.filter { it.where is Where.Pile }
        if (cards.isNotEmpty()) {
            val name = aheadOf(l.handle)
            val ahead = (read(name) as? JsObj)?.let { parseJson(stringify(it)!!) as JsObj } ?: emptyAhead(l.handle)
            for (c in cards) {
                val n = (c.where as Where.Pile).n
                val pile = node(ahead, "$n") { JsObj.of("_" to str(PILES[n - 1])) }
                if ((1..9).any { k -> textOf(pile["$k"])?.let { plain(it) == plain(c.words) } == true }) continue
                val free = (1..9).firstOrNull { !pile.has("$it") || textOf(pile["$it"]).isNullOrBlank() }
                if (free == null) over += "“${c.words}” (your hand, ${PILES[n - 1]}, holds nine)" else pile["$free"] = JsObj.of("_" to str(c.words), "3" to str(stamp))
            }
            out += name to ahead
        }

        // His P6: each piece beneath its portal, nine at most.
        val pieces = kept.filter { it.where is Where.Portal }
        if (pieces.isNotEmpty()) {
            val name = p6Of(l.handle)
            val p6 = (read(name) as? JsObj)?.let { parseJson(stringify(it)!!) as JsObj } ?: emptyP6(l.handle)
            for (p in pieces) {
                val at = (p.where as Where.Portal).at
                val (ring, k) = at.split('.')
                val portal = node(node(p6, ring) { JsObj.of("_" to str(if (ring == "1") "THE SURFACE" else "THE DEEP")) }, k) { JsObj.of("_" to str(PORTAL_NAMES[at]!!.uppercase())) }
                if ((1..9).any { j -> textOf(portal["$j"])?.let { plain(it) == plain(p.words) } == true }) continue
                val free = (1..9).firstOrNull { !portal.has("$it") }
                if (free == null) over += "“${p.words}” (${PORTAL_NAMES[at]} holds nine)"
                else portal["$free"] = JsObj.of("_" to str(p.words), "2" to str(dayAddress(today)), "3" to str(stamp))
            }
            out += name to p6
        }
        return Written(out, over)
    }

    const val NOW_OPENING = "My now, on the clock (spine:now): each day's line at its date, and the week, month, season and year above it, in my words."

    fun emptyAhead(handle: String): JsObj = JsObj.of(
        "_" to str("AHEAD — $handle's piles of the indexical future (function:ahead): intents at the grain they are sized for, unordered within their pile, never arriving unless pulled into now and landed as record. A pile is appetite, not sequence."),
        *PILES.mapIndexed { i, p -> "${i + 1}" to JsObj.of("_" to str(p)) }.toTypedArray(),
    )

    fun emptyP6(handle: String): JsObj = JsObj.of(
        "_" to str("P6 — $handle's mirror of spine:p6: pieces of my days, my words, each beneath the portal it belongs to, with the day it came from at 2 and when it arrived at 3."),
        "1" to JsObj.of("_" to str("THE SURFACE"), "1" to JsObj.of("_" to str("FACTS")), "2" to JsObj.of("_" to str("FEELINGS")), "3" to JsObj.of("_" to str("FICTIONS"))),
        "2" to JsObj.of("_" to str("THE DEEP"), "1" to JsObj.of("_" to str("PURPOSE")), "2" to JsObj.of("_" to str("DECISION")), "3" to JsObj.of("_" to str("OUTCOMES"))),
    )

    private fun textOf(n: Js?): String? = when (n) {
        is JsStr -> n.value
        is JsObj -> collectUnderscore(n)
        else -> null
    }

    private fun node(parent: JsObj, key: String, make: () -> JsObj): JsObj = when (val c = parent[key]) {
        is JsObj -> c
        is JsStr -> JsObj.of("_" to c).also { parent[key] = it }
        else -> make().also { parent[key] = it }
    }

    /** [clock] with [words] voiced at the address [digits] (a '0' digit is the underscore): after any line it had, never twice. */
    fun voiced(clock: JsObj, digits: String, words: String): JsObj {
        var node = clock
        for (d in digits) {
            val key = if (d == '0') "_" else "$d"
            val next = when (val c = node[key]) {
                is JsObj -> c
                is JsStr -> JsObj.of("_" to c)
                else -> JsObj()
            }
            node[key] = next
            node = next
        }
        val had = when (val u = node["_"]) {
            is JsStr -> u.value
            is JsObj -> collectUnderscore(u)
            else -> null
        }?.takeIf { it.isNotBlank() }
        node["_"] = str(
            when {
                had == null -> words.trim()
                had.split("\n\n").any { plain(it) == plain(words) } -> had
                else -> had + "\n\n" + words.trim()
            },
        )
        return clock
    }

    // ── the move out of telling, once ──

    private val OLD_TO_PORTAL = mapOf("1" to "1.1", "2" to "1.2", "3" to "1.3", "4" to "2.1", "5" to "2.2", "6" to "2.3")
    private val OLD_SHELF_TO_PILE = mapOf("1" to 2, "2" to 3, "3" to 4, "4" to 5, "5" to 6)
    private val OLD_RUNG = mapOf("1" to "week", "2" to "month", "3" to "season", "4" to "year")

    /** Whether his days still stand the old way: a telling block, or the six inside a day of his now. */
    fun toMove(storage: BlockStorage): Boolean {
        if (storage.read(Landing.TELLING) != null) return true
        val clock = storage.read(NOW)?.let { runCatching { parseJson(it) }.getOrNull() } as? JsObj ?: return false
        return days(clock).any { (_, day) -> day["_"] is JsObj && OLD_TO_PORTAL.keys.any { (day["_"] as JsObj).has(it) } }
    }

    /** Each day node of the clock (eight digits deep), with its digits. */
    private fun days(clock: JsObj): List<Pair<String, JsObj>> {
        val out = mutableListOf<Pair<String, JsObj>>()
        fun walk(node: Js?, digits: String) {
            val o = node as? JsObj ?: return
            if (digits.length == 8) { out += digits to o; return }
            for (k in o.keys()) walk(o[k], digits + (if (k == "_") "0" else k))
        }
        walk(clock, "")
        return out
    }

    /** What the move writes: his now with the six taken out of each day, his ahead and his P6 with what was in them. Null when there is nothing to move. */
    class Moved(val now: JsObj?, val ahead: JsObj?, val p6: JsObj?, val pieces: Int, val cards: Int, val rungs: Int, val over: List<String>)

    fun moveOut(handle: String, storage: BlockStorage, nowMs: Long): Moved? {
        if (!toMove(storage)) return null
        val stamp = isoString(nowMs)
        val today = java.time.Instant.ofEpochMilli(nowMs).atZone(ZoneOffset.UTC).toLocalDate()
        val clock = storage.read(NOW)?.let { runCatching { parseJson(it) }.getOrNull() } as? JsObj
        val ahead = storage.read(aheadOf(handle))?.let { runCatching { parseJson(it) }.getOrNull() } as? JsObj ?: emptyAhead(handle)
        val p6 = storage.read(p6Of(handle))?.let { runCatching { parseJson(it) }.getOrNull() } as? JsObj ?: emptyP6(handle)
        val over = mutableListOf<String>()
        var pieces = 0
        var cards = 0
        var rungs = 0
        // The six out of each day of his now; the day's line stays at its date.
        if (clock != null) for ((digits, day) in days(clock)) {
            val dir = day["_"] as? JsObj ?: continue
            for ((old, at) in OLD_TO_PORTAL) {
                val place = dir[old] ?: continue
                val (ring, k) = at.split('.')
                val portal = node(node(p6, ring) { JsObj() }, k) { JsObj.of("_" to str(PORTAL_NAMES[at]!!.uppercase())) }
                for (j in 1..9) {
                    val w = textOf((place as? JsObj)?.get("$j"))?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                    if ((1..9).any { x -> textOf(portal["$x"])?.let { plain(it) == plain(w) } == true }) continue
                    val free = (1..9).firstOrNull { !portal.has("$it") }
                    if (free == null) over += "“$w” (${PORTAL_NAMES[at]} holds nine)" else { portal["$free"] = JsObj.of("_" to str(w), "2" to str(digits + "00"), "3" to str(stamp)); pieces++ }
                }
            }
            day["_"] = str(collectUnderscore(dir).orEmpty())
        }
        // The old hand and rungs out of telling.
        val telling = storage.read(Landing.TELLING)?.let { runCatching { parseJson(it) }.getOrNull() } as? JsObj
        var newClock = clock ?: Landing.emptyClock(NOW_OPENING)
        if (telling != null) {
            (telling["2"] as? JsObj)?.let { hand ->
                for ((shelf, n) in OLD_SHELF_TO_PILE) {
                    val s = hand[shelf] as? JsObj ?: continue
                    val pile = node(ahead, "$n") { JsObj.of("_" to str(PILES[n - 1])) }
                    for (j in 1..9) {
                        val w = textOf(s["$j"])?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                        if ((1..9).any { x -> textOf(pile["$x"])?.let { plain(it) == plain(w) } == true }) continue
                        val free = (1..9).firstOrNull { !pile.has("$it") || textOf(pile["$it"]).isNullOrBlank() }
                        if (free == null) over += "“$w” (your hand, ${PILES[n - 1]}, holds nine)" else { pile["$free"] = JsObj.of("_" to str(w), "3" to str(stamp)); cards++ }
                    }
                }
            }
            (telling["3"] as? JsObj)?.let { r ->
                for ((k, level) in OLD_RUNG) {
                    val node = r[k] as? JsObj ?: continue
                    val w = (1..9).mapNotNull { j -> textOf(node["$j"])?.trim()?.takeIf { it.isNotEmpty() } }.firstOrNull() ?: continue
                    newClock = voiced(newClock, dayAddress(today).take(rungDigits(level)), w)
                    rungs++
                }
            }
        }
        return Moved(if (clock != null || rungs > 0) newClock else null, ahead, p6, pieces, cards, rungs, over)
    }
}
