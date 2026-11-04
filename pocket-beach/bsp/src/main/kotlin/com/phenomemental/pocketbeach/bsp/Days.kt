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

    /** One line of his day beyond its sheet, where it goes: a pile of his hand, a booking at a later day, or a rung of his now. */
    sealed class Where {
        data class Pile(val n: Int) : Where()
        data class Booking(val date: LocalDate) : Where()
        data class Rung(val level: String) : Where()
    }

    fun name(w: Where): String = when (w) {
        is Where.Pile -> "Your hand: ${PILES.getOrElse(w.n - 1) { "pile ${w.n}" }}"
        is Where.Booking -> "Booked for " + w.date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM", java.util.Locale.UK))
        is Where.Rung -> "This ${w.level}, the rung above"
    }

    /** A line of his day beyond its sheet; [already]: Sundial 0.10.3, it stands on his hand already, so it is said so and not written again. */
    data class Line(val words: String, val where: Where, val ticked: Boolean = true, val from: Where? = null, val already: Boolean = false) {
        val moved: Boolean get() = from != null && from != where
    }

    data class Intent(val words: String, val pile: Int?, val date: LocalDate?, val by: String?)
    data class Rung(val level: String, val words: String)

    /**
     * Sundial 0.10, the keep card (spine:pocket-beach 1.1111787197, 1.111178719916):
     * his day as it is set out for his yes. The [sheet] is hers, in Sundial's
     * form and the second person (or his own words, for an old cook); the
     * [places] are the coloured dots he can tap off or on; his [moments], each
     * kept whole at its [momentGrain]; and his hand, bookings and rungs as
     * [lines]. No P6 piece is made for a day. [day] is the beach's day kept
     * (yesterday's, when the card offers the day before first); [his] is a
     * line of his own already standing there, which Keep it leaves as it is.
     */
    data class DayCard(
        val handle: String,
        val day: LocalDate,
        val sheet: Mark.Sheet?,
        val places: List<Int>,
        val moments: List<Keep.Moment>,
        val lines: List<Line>,
        val atMs: Long,
        val momentGrain: Keep.Grain = Keep.Grain.MINUTE,
        val his: String? = null,
        val yesterday: Boolean = false,
    ) {
        /**
         * The day's line as Keep it writes it: the sheet in Sundial's form, its
         * places line last. Sundial 0.10.3: with no place, no line at all for
         * them; no label ("In your own words.") ever goes into his words.
         */
        val text: String? get() = sheet?.let { s -> Mark.write(s.copy(places = Mark.inOrder(places), own = false)) }

        fun describe(): String = buildString {
            val kept = lines.filter { it.ticked }
            val parts = mutableListOf<String>()
            if (sheet != null && his == null) parts += "the day's line"
            if (moments.isNotEmpty()) parts += "${moments.size} moment${if (moments.size == 1) "" else "s"}"
            kept.count { it.where is Where.Pile }.takeIf { it > 0 }?.let { parts += "$it on your hand" }
            kept.count { it.where is Where.Booking }.takeIf { it > 0 }?.let { parts += "$it booked" }
            kept.count { it.where is Where.Rung }.takeIf { it > 0 }?.let { parts += "$it on the rungs" }
            append(parts.joinToString(", ").ifEmpty { "nothing" })
        }

        fun tick(i: Int) = copy(lines = lines.mapIndexed { k, l -> if (k == i && !l.already) l.copy(ticked = !l.ticked) else l })

        /** Sundial 0.10.3: a place's colour tapped beside his [i]th moment: off if it was on, on if it was off; the day's places, all of theirs. */
        fun placeMoment(i: Int, n: Int): DayCard {
            val ms = moments.mapIndexed { k, m -> if (k == i) m.copy(places = Mark.inOrder(if (n in m.places) m.places - n else m.places + n)) else m }
            return copy(moments = ms, places = Mark.inOrder(ms.flatMap { it.places }))
        }
        fun move(i: Int, to: Where) = copy(lines = lines.mapIndexed { k, l -> if (k == i) l.copy(where = to, from = l.from ?: l.where, ticked = true) else l })
        /** A place's dot tapped: off if it was on, on if it was off. */
        fun place(n: Int) = copy(places = Mark.inOrder(if (n in places) places - n else places + n))
    }

    /**
     * What a keep sets out: [sheet] as the day's line at [day], [places] named
     * last, [moments] each at [grain], his cards, bookings and rungs. A line of
     * his own already at that day ([his]) stays as it is, and only the moments
     * go beneath it.
     */
    fun landing(
        handle: String, day: LocalDate, sheet: Mark.Sheet?, places: List<Int>, moments: List<Keep.Moment>,
        cards: List<Intent>, rungs: List<Rung>, now: ZonedDateTime, his: String? = null, grain: Keep.Grain = Keep.Grain.MINUTE, yesterday: Boolean = false,
        onHand: Collection<String> = emptyList(),
    ): DayCard {
        val lines = mutableListOf<Line>()
        // Sundial 0.10.1: each line once, its words without a quote mark left open (his Pixel, 3 November: "I'm also going to need to do
        // ID checks this week." was on his hand and on this week's rung both, once with a stray opening quote). A piece he means to do,
        // sized for this week, is on his hand; it isn't a rung as well.
        val seen = HashSet<String>()
        fun once(words: String): String? = Telling.unquoted(words).takeIf { it.isNotBlank() && seen.add(Telling.plain(it)) }
        // Sundial 0.10.3: a card standing on his hand already, in any pile, is said so and never written again.
        val held = onHand.map { Telling.plain(it) }.toSet()
        for (c in cards) once(c.words)?.let { w ->
            val already = c.date == null && Telling.plain(w) in held
            lines += Line(w, c.date?.let { d -> Where.Booking(d) } ?: Where.Pile(c.pile ?: 3), ticked = !already, already = already)
        }
        for (r in rungs) once(r.words)?.let { lines += Line(it, Where.Rung(r.level)) }
        // Sundial 0.10.3: the day's places are her places for its moments, all of them; else those she named for the day.
        val fromMoments = Mark.inOrder(moments.flatMap { it.places })
        return DayCard(handle, day, sheet, fromMoments.ifEmpty { Mark.inOrder(places) }, moments, lines, now.toInstant().toEpochMilli(), grain, his, yesterday)
    }

    // ── what he means to do, and the rungs ──

    private fun plain(s: String) = Telling.plain(s)

    /** His time words, if he keeps them (Settings): the shelves' words, tomorrow to the year. */
    fun timeWords(storage: BlockStorage): JsObj? = storage.read(Whens.BLOCK)?.let { runCatching { parseJson(it) }.getOrNull() } as? JsObj

    /** The pieces that say what he means to do (cook 9.9's words), each sized: a date is a booking, else the pile its time words name, else this week. */
    fun intents(pieces: List<String>, words: List<String>, today: LocalDate, timeWords: JsObj?): List<Intent> = intents(pieces, words, today, timeWords, emptyList(), emptyList())

    /**
     * 0.9.1 (spine:pocket-beach 1.1111787163): as above, and a piece cut at
     * one of [joins] ("and need to", "as well as"), with no I of its own,
     * carries on the piece before it: it is what he means to do when that one
     * is. A piece naming one of [appointments] (a meeting, a call) with its day
     * is a booking at that day, whether or not he says he means to.
     */
    fun intents(pieces: List<String>, words: List<String>, today: LocalDate, timeWords: JsObj?, joins: List<String>, appointments: List<String>, naming: Boolean = false): List<Intent> {
        val out = mutableListOf<Intent>()
        var before = false
        for (p in pieces) {
            val said = " ${plain(p)} "
            // The words of cook 9.9 it holds, the first of them; 0.9.1: a card only when what follows them names something to do.
            val at = words.map { w -> " ${plain(w)} " }.filter { said.contains(it) }.minByOrNull { said.indexOf(it) }
            val holds = at != null
            val names = at != null && (!naming || said.substring(said.indexOf(at) + at.length).split(' ').any { it.isNotBlank() && it !in NOT_A_THING })
            val carried = before && joins.any { j -> said.startsWith(" ${plain(j)} ") } && !Regex("""\b(?:i|i'm|i've|i'll|i'd|we|we're|we've|we'll)\b""").containsMatchIn(plain(p))
            val day = if (appointments.any { a -> said.contains(" ${plain(a)} ") }) dayOf(p, today, timeWords) else null
            when {
                day != null -> out += Intent(p.trim(), null, day.first, day.second)
                names || carried -> out += size(p, today, timeWords).let { (pile, date, by) -> Intent(p.trim(), pile, date, by) }
            }
            before = day == null && (holds || carried)
        }
        return out
    }

    /** What names nothing to do, after "I need to": "do", "do it", "do that", and the like. */
    private val NOT_A_THING = setOf("do", "it", "that", "this", "them", "those", "these", "too", "as", "well", "so", "then", "now", "today", "tonight", "also", "and")

    /** The day a piece names, and the words that name it: today for today's words and the next hours, else the date its words give; null when it names none. */
    private fun dayOf(piece: String, today: LocalDate, timeWords: JsObj?): Pair<LocalDate, String>? {
        val (pile, date, by) = size(piece, today, timeWords)
        if (by == null) return null
        return when {
            date != null -> date to by
            pile == 1 || pile == 2 -> today to by
            else -> null
        }
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

    /**
     * What one yes writes: his now and his ahead, each whole and only when it
     * changes; what couldn't go (past nine); what became of the day's line;
     * and Sundial 0.10.3: the cards skipped, standing on his hand already.
     */
    class Written(val blocks: List<Pair<String, JsObj>>, val over: List<String>, val dayLine: Keep.DayLine? = null, val moments: List<Keep.Landed> = emptyList(), val already: List<String> = emptyList())

    fun blocks(l: DayCard, read: (String) -> Js?, nowMs: Long = l.atMs): Written {
        val over = mutableListOf<String>()
        val already = l.lines.filter { it.already }.map { it.words }.toMutableList()
        val stamp = isoString(nowMs)
        val kept = l.lines.filter { it.ticked && !it.already }
        val out = mutableListOf<Pair<String, JsObj>>()

        // His now: the day's line in Sundial's form, each moment at its grain, bookings at their later day, rungs.
        val nowBefore = read(NOW) as? JsObj
        var clock = nowBefore?.let { parseJson(stringify(it)!!) as JsObj } ?: Landing.emptyClock(NOW_OPENING)
        var dayLine: Keep.DayLine? = null
        l.text?.let { text -> dayLine = Keep.dayLine(clock, l.day, text, nowMs) }
        val landed = l.moments.sortedBy { it.atMs }.map { Keep.moment(clock, it, l.momentGrain) }
        for (line in kept) {
            when (val w = line.where) {
                // A booking at the day kept is in its moments and its sheet already.
                is Where.Booking -> if (w.date != l.day) Keep.booking(clock, w.date, line.words, nowMs)
                is Where.Rung -> clock = voiced(clock, dayAddress(l.day).take(rungDigits(w.level)), line.words)
                else -> {}
            }
        }
        // Sundial 0.10.3: written only when it changes; a block that would stand as it does is no write, and never a refusal.
        if (nowBefore == null || !deepEqual(clock, nowBefore)) out += NOW to clock

        // His hand: each card at its pile, nine at most; a card standing anywhere on his hand already is skipped and said so.
        val cards = kept.filter { it.where is Where.Pile }
        if (cards.isNotEmpty()) {
            val name = aheadOf(l.handle)
            val aheadBefore = read(name) as? JsObj
            val ahead = aheadBefore?.let { parseJson(stringify(it)!!) as JsObj } ?: emptyAhead(l.handle)
            val held = handOf(ahead).values.map { plain(it) }.toMutableSet()
            for (c in cards) {
                val n = (c.where as Where.Pile).n
                if (plain(c.words) in held) { already += c.words; continue }
                val pile = node(ahead, "$n") { JsObj.of("_" to str(PILES[n - 1])) }
                val free = (1..9).firstOrNull { !pile.has("$it") || textOf(pile["$it"]).isNullOrBlank() }
                if (free == null) over += "“${c.words}” (your hand, ${PILES[n - 1]}, holds nine)"
                else { pile["$free"] = JsObj.of("_" to str(c.words), "3" to str(stamp)); held += plain(c.words) }
            }
            if (aheadBefore == null || !deepEqual(ahead, aheadBefore)) out += name to ahead
        }
        return Written(out, over, dayLine, landed, already.distinct())
    }

    /** Sundial 0.10.3: the cards on his hand, by address ("3.1"), in every pile. */
    fun handOf(ahead: JsObj?): Map<String, String> = buildMap {
        for (k in 1..9) {
            val pile = ahead?.get("$k") as? JsObj ?: continue
            for (c in 1..9) textOf(pile["$c"])?.trim()?.takeIf { it.isNotEmpty() }?.let { put("$k.$c", it) }
        }
    }

    /**
     * Sundial 0.10.3: his hand with the card at [address] ("3.1") taken off,
     * on his tap; the cards after it in its pile move up one, so none is left
     * empty. Null when nothing stands there.
     */
    fun takenOff(ahead: JsObj, address: String): JsObj? {
        val (p, c) = address.split('.').takeIf { it.size == 2 }?.let { it[0] to it[1].toIntOrNull() } ?: return null
        val copy = parseJson(stringify(ahead)!!) as JsObj
        val pile = copy[p] as? JsObj ?: return null
        if (c == null || textOf(pile["$c"]).isNullOrBlank()) return null
        val rest = (1..9).filter { it != c }.mapNotNull { k -> pile["$k"]?.takeIf { textOf(it)?.isNotBlank() == true } }
        for (k in 1..9) pile.remove("$k")
        rest.forEachIndexed { i, v -> pile["${i + 1}"] = v }
        return copy
    }

    /**
     * Sundial 0.10.3 (spine:pocket-beach 1.111178719929): the card at
     * [address] ("3.1") taken off [handle]'s hand on his own tap, the app
     * never taking one off for him, with its entry in his history. Null when
     * done, else why not, in plain words.
     */
    fun takeOff(handle: String, address: String, pages: Pages, storage: BlockStorage): String? {
        val name = aheadOf(handle)
        val ahead = storage.read(name)?.let { runCatching { parseJson(it) }.getOrNull() } as? JsObj ?: return "your hand couldn't be read"
        val words = handOf(ahead)[address] ?: return "that card isn't on your hand now"
        val after = takenOff(ahead, address) ?: return "that card isn't on your hand now"
        val done = runCatching { pages.carryOut(pages.planWhole(name, after)) }.getOrNull()
        val problem = if (done?.ok == true) null else done?.problem ?: "your hand couldn't be written"
        val pile = PILES.getOrElse((address.substringBefore('.').toIntOrNull() ?: 0) - 1) { "a pile" }
        Close.recordYes(handle, "a card taken off his hand", if (problem == null) listOf("his hand: “${words.trim()}” taken off, $pile") else emptyList(), listOfNotNull(problem), pages)
        return problem
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
        // Sundial 0.10: a day line's hidden directory holds its keep instant at 3, a string; the old six were places, objects (1.111178719915).
        return days(clock).any { (_, day) -> (day["_"] as? JsObj)?.let { dir -> OLD_TO_PORTAL.keys.any { dir[it] is JsObj } } == true }
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
