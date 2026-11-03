package com.phenomemental.pocketbeach.bsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Mark.kt — Sundial's mark, as mark.js at the repository's root reckons it:
 * the six places a day can stand in, a day as the plain text its one slot
 * holds, the sums the drawing is made from, and the drawing's geometry. The
 * drawing itself (mark.js's dial() and coil()) stays with whatever draws;
 * everything here runs without a page, as mark.js's own parts outside dial() do.
 *
 * Where mark.js reckons the light and the shadow from the phone's clock
 * (shadowAngle and sunHeight, mark.js:177-189), these take the shadow's
 * direction and the sun's height as given, so the app can supply them from
 * the real sun.
 */

/** The six places, a day's plain text, and the sums (mark.js:9-169). */
object Mark {
    /** One of the six places, in pool order 1 to 6 (mark.js:11-24). hue as 0xFFRRGGBB. */
    data class Pool(val n: Int, val name: String, val hue: Long, val phrase: String, val at: String, val question: String)

    /** The six in pool order, by the numbers Claude names them by (mark.js:11-24). */
    val POOLS: List<Pool> = listOf(
        Pool(1, "Facts", 0xFF4A6B8A, "what happened", "1.1", "What could you have pointed to today?"),
        Pool(2, "Feelings", 0xFFB0413E, "what you felt", "1.2", "What did your body know today?"),
        Pool(3, "Fictions", 0xFF7A5C99, "what you made of it", "1.3", "What did you make of it, that might not be so?"),
        Pool(4, "Purpose", 0xFF2E7D6B, "what matters", "2.1", "What mattered, underneath?"),
        Pool(5, "Decision", 0xFFB8860B, "a choice", "2.2", "What did you choose, or let be chosen?"),
        Pool(6, "Outcomes", 0xFF6FA8DC, "what’s ahead", "2.3", "What are you expecting, or fearing?"),
    )

    /** P6's order round presence, clockwise from Facts at the top: the order every picture and every quiet line keeps (mark.js:26-30). */
    val P6: List<Int> = listOf(1, 6, 3, 5, 4, 2)

    /** The quiet line that ends a day kept in their own words (mark.js:76-80). */
    const val OWN = "In your own words."

    /** A phrase as it is compared: lower case, either curly apostrophe or a backtick made straight, its spaces collapsed and trimmed (mark.js:39-41). */
    fun plainPhrase(s: String): String =
        jsTrim(collapseSpace(s.lowercase().replace('’', '\'').replace('‘', '\'').replace('`', '\'')))

    /** The place a phrase names, by its number; 0 when it names none (mark.js:42-46). */
    fun poolByPhrase(s: String): Int {
        val want = plainPhrase(s)
        return POOLS.firstOrNull { plainPhrase(it.phrase) == want }?.n ?: 0
    }

    /** Places as pool numbers, each once, in P6's order; anything that is not a place left out. Never ranked (mark.js:48-54). */
    fun inOrder(places: Collection<Int>): List<Int> {
        val have = places.toSet()
        return P6.filter { it in have }
    }

    /** The quiet words for places, in P6's order, joined by " · " ("" for none) (mark.js:55-59). */
    fun placesWords(places: Collection<Int>): String = inOrder(places).joinToString(" · ") { POOLS[it - 1].phrase }

    /** The same places said in a sentence: "what happened, what’s ahead, and what you felt" ("" for none) (mark.js:60-66). */
    fun placesSaid(places: Collection<Int>): String {
        val ws = inOrder(places).map { POOLS[it - 1].phrase }
        return if (ws.size > 1) ws.dropLast(1).joinToString(", ") + ", and " + ws.last() else ws.joinToString("")
    }

    /**
     * The places a last line names, when every piece of it is a place's phrase; an
     * older text's "A day of what happened." names that one place (mark.js:67-74).
     */
    fun placesIn(row: String): List<Int> {
        olderLine(row)?.let { said ->
            val n = poolByPhrase(said)
            return if (n != 0) listOf(n) else emptyList()
        }
        val pieces = finalStopDropped(row).split('·').map(::jsTrim)
        val ns = pieces.map(::poolByPhrase)
        return if (ns.all { it != 0 }) inOrder(ns) else emptyList()
    }

    /** A sheet as she returns it. */
    data class Sheet(val title: String, val lines: List<String>, val tomorrow: String = "", val places: List<Int> = emptyList(), val own: Boolean = false)

    /**
     * A sheet and its place as the text of the slot: "place — title", the lines,
     * "Tomorrow: …" when there is one, then the places line, or the own line when
     * it is in their own words and names no place (mark.js:82-97).
     */
    fun write(sheet: Sheet, place: String = ""): String {
        val rows = mutableListOf((if (place.isNotEmpty()) "$place — " else "") + jsTrim(sheet.title))
        for (l in sheet.lines) jsTrim(l).let { if (it.isNotEmpty()) rows += it }
        val tomorrow = jsTrim(sheet.tomorrow).let { t -> afterAscii(t, "tomorrow:")?.trimStart(::isJsSpace) ?: t }
        if (tomorrow.isNotEmpty()) rows += "Tomorrow: $tomorrow"
        val words = placesWords(sheet.places)
        if (words.isNotEmpty()) rows += words.substring(0, 1).uppercase() + words.substring(1) + "."
        else if (sheet.own) rows += OWN
        return rows.joinToString("\n")
    }

    /** A day's text read back. */
    data class Day(val place: String, val title: String, val lines: List<String>, val tomorrow: String, val places: List<Int>, val own: Boolean)

    /**
     * The text of a slot read back; the places are read only from a last line
     * that names them, and only the first "Tomorrow:" line is tomorrow's
     * (mark.js:99-126).
     */
    fun read(text: String): Day {
        val rows = text.split('\n').map(::jsTrim).filter { it.isNotEmpty() }.toMutableList()
        if (rows.isEmpty()) return Day("", "", emptyList(), "", emptyList(), false)
        var places = emptyList<Int>()
        var own = false
        val named = if (rows.size > 1) placesIn(rows.last()) else emptyList()
        if (named.isNotEmpty()) {
            places = named
            rows.removeAt(rows.lastIndex)
        } else if (rows.size > 1 && rows.last() == OWN) {
            own = true
            rows.removeAt(rows.lastIndex)
        }
        val first = rows.removeAt(0)
        val cut = first.indexOf(" — ")
        val place = if (cut > 0) jsTrim(first.substring(0, cut)) else ""
        val title = if (cut > 0) jsTrim(first.substring(cut + 3)) else first
        var tomorrow = ""
        val lines = mutableListOf<String>()
        for (r in rows) {
            val t = tomorrowIn(r)
            if (t != null && tomorrow.isEmpty()) tomorrow = t else lines += r
        }
        return Day(place, title, lines, tomorrow, places, own)
    }

    /**
     * Every place a day's text touched, in P6's order: its own quiet line, else
     * the places [given] for it (a day kept through Claude has no line); [] when
     * it names none (mark.js:128-136).
     */
    fun placesOf(text: String, given: Collection<Int> = emptyList()): List<Int> = read(text).places.ifEmpty { inOrder(given) }

    /** How many days touched each place, in pool order, from each day's places. */
    fun counts(days: List<List<Int>>): List<Int> {
        // mark.js:138-147: a day counts once in every place it touched.
        val c = IntArray(6)
        for (d in days) for (n in inOrder(d)) c[n - 1]++
        return c.toList()
    }

    /** Each place's share: its count over all the touches, summing to 1; all nought before any day (mark.js:149-155). */
    fun shares(counts: List<Int>): List<Double> {
        val total = counts.sum()
        return counts.map { if (total != 0) it.toDouble() / total else 0.0 }
    }

    private const val MOST = 5.0 / 36

    /**
     * How evenly the days spread across the six: 1 less the variance of the
     * shares over the most it can be (5/36), so 1 when all six hold the same and
     * 0 when one holds everything; nought before any day (mark.js:157-169).
     */
    fun evenness(shares: List<Double>): Double {
        val total = shares.sum()
        if (total == 0.0 || total.isNaN()) return 0.0
        val mean = 1.0 / 6
        var v = 0.0
        for (x in shares) v += (x - mean) * (x - mean)
        v /= 6
        return minOf(1.0, maxOf(0.0, 1 - v / MOST))
    }

    /** What /^a day of (.+?)\.?$/i catches (mark.js:69), or null: JavaScript's `.` takes no line ending and its /i folds ASCII alone. */
    private fun olderLine(row: String): String? {
        val rest = afterAscii(row, "a day of ") ?: return null
        if (rest.isEmpty() || rest.any(::isLineEnd)) return null
        return if (rest.length > 1 && rest.endsWith('.')) rest.dropLast(1) else rest
    }

    /** row.replace(/\.\s*$/, '') (mark.js:71). */
    private fun finalStopDropped(row: String): String {
        val end = row.trimEnd(::isJsSpace)
        return if (end.endsWith('.')) end.dropLast(1) else row
    }

    /** What /^tomorrow:\s*(.*)$/i catches (mark.js:121), or null. */
    private fun tomorrowIn(r: String): String? {
        val said = afterAscii(r, "tomorrow:")?.trimStart(::isJsSpace) ?: return null
        return if (said.any(::isLineEnd)) null else said
    }

    /** [s] past [prefix] (lower case), its letters matched in either case; null when [s] does not start with it. */
    private fun afterAscii(s: String, prefix: String): String? {
        if (s.length < prefix.length) return null
        for (i in prefix.indices) {
            val c = s[i]
            if ((if (c in 'A'..'Z') c + 32 else c) != prefix[i]) return null
        }
        return s.substring(prefix.length)
    }

    private fun isLineEnd(c: Char): Boolean = c == '\n' || c == '\r' || c == ' ' || c == ' '
}

/** The dial's sums (mark.js:171-282, 477-492 and 902-962): the numbers the drawing is made from, in the heart's own units. */
object DialMath {
    /** The dial's centre (mark.js:242). */
    const val CX = 232.0
    const val CY = 140.0

    /** The ring, a place's stretch of the stroke (of 1000), the moments' ring and the light's reach (mark.js:243). */
    const val RING = 182.0
    const val STRETCH = 160.0
    const val MOMENTS = 166.0
    const val LIGHT_R = 118.0

    /** Warm apricot at dawn, pale sun by day, amber at evening (mark.js:203). */
    const val DAWN = 0xFFF3CB8FL
    const val MIDDAY = 0xFFF1E79BL
    const val EVENING = 0xFFEDB98AL

    private const val SPIRAL_IN = 182.0
    private const val SPIRAL_STEP = 0.3
    private const val POOL_RING = 128.0
    private const val POOL_EMPTY = 14.0
    private const val POOL_MIN = 16.0
    private const val POOL_MAX = 50.0
    private const val COIL_R = 25.0
    private const val COIL_TURNS = 2.5
    private const val COIL_STEPS = 300
    private const val COIL_GAP = 0.03

    data class Pt(val x: Double, val y: Double)

    /** The point [r] from the centre at [angleDeg] (screen degrees, 0 = right, clockwise) (mark.js:250-253). */
    fun at(angleDeg: Double, r: Double): Pt {
        val a = angleDeg * PI / 180
        return Pt(CX + r * cos(a), CY + r * sin(a))
    }

    /** The shadow thrown along [shadowDeg] (screen degrees, 0 = right, clockwise) for sun height [s] in 0..1 (mark.js:189-197). */
    data class Cast(val dx: Double, val dy: Double, val length: Double, val opacity: Double)

    /** The heart's shadow: 14 + 70(1 - s) long, at ink 0.16 min(1, 4s), long and soft at dawn and dusk (mark.js:189-197). */
    fun castOf(shadowDeg: Double, s: Double): Cast {
        val a = shadowDeg * PI / 180
        val l = 14 + 70 * (1 - s)
        return Cast(l * cos(a), l * sin(a), l, 0.16 * minOf(1.0, 4 * s))
    }

    /** The sunlight's brightest point, colour and strength (mark.js:198-219): [beforeNoon] picks apricot or amber; [days] kept so far. */
    data class Light(val x: Double, val y: Double, val shift: Double, val colour: Long, val strength: Double)

    /**
     * The sunlight: its brightest point 36(1 - s) + 10 from the centre towards
     * the sun, opposite the shadow; its colour by the sun's height, from apricot
     * ([beforeNoon]: before 12:45, when mark.js's sun stands highest) or amber
     * towards pale sun; its strength (0.25 + 0.5s), all of it at 27 days, never
     * under 0.35 of it (mark.js:198-219).
     */
    fun lightOf(shadowDeg: Double, s: Double, beforeNoon: Boolean, days: Int): Light {
        val a = (shadowDeg + 180) * PI / 180
        val d = 36 * (1 - s) + 10
        return Light(
            CX + d * cos(a), CY + d * sin(a), d,
            mixHex(if (beforeNoon) DAWN else EVENING, MIDDAY, s),
            (0.25 + 0.5 * s) * maxOf(0.35, minOf(1.0, days / 27.0)),
        )
    }

    /**
     * [a] mixed towards [b] by [f], each channel rounded half up as Math.round
     * rounds, and held to 0..255; opaque (mark.js:204-211).
     */
    fun mixHex(a: Long, b: Long, f: Double): Long {
        var out = 0xFF000000L
        for (sh in intArrayOf(16, 8, 0)) {
            val v = Math.round(((a shr sh) and 255L) * (1 - f) + ((b shr sh) and 255L) * f)
            out = out or (v.coerceIn(0L, 255L) shl sh)
        }
        return out
    }

    data class Centre(val scale: Double, val opacity: Double)

    /** The crimson centre: 0.55 to 1 by evenness once there are days; before any, half size, at 0.45 until they are in (mark.js:221-226). */
    fun centre(days: Int, even: Double, lit: Boolean): Centre =
        if (days != 0) Centre(0.55 + 0.45 * even, 1.0) else Centre(0.5, if (lit) 1.0 else 0.45)

    /** Day [n] (from 1) on the spiral: at -90 + 40(n - 1) degrees, 182 + 0.3(n - 1) out, so the 81st lies at 206 (mark.js:255-262). */
    fun dayAt(n: Int): Pt = spiralAt(n.toDouble())

    private fun spiralAt(n: Double): Pt = at(-90 + 40 * (n - 1), SPIRAL_IN + SPIRAL_STEP * (n - 1))

    /** The spiral's line from day 1 to day [n], a point every tenth of a day and day [n] last; none before the second day (mark.js:263-270). */
    fun spiralPoints(n: Int): List<Pt> {
        if (n < 2) return emptyList()
        val pts = mutableListOf<Pt>()
        var k = 0
        while (1 + k / 10.0 < n) {
            pts += spiralAt(1 + k / 10.0)
            k++
        }
        pts += dayAt(n)
        return pts
    }

    /** "ninth" | "full" | "faint"; a ninth's opacity is its stroke's, 1. */
    data class Dot(val kind: String, val r: Double, val opacity: Double)

    /** How day [k] of [n] is drawn: every ninth an open circle, the latest nine full, earlier ones fine and faint (mark.js:271-277). */
    fun dotOf(k: Int, n: Int): Dot = when {
        k % 9 == 0 -> Dot("ninth", 5.0, 1.0)
        k > n - 9 -> Dot("full", 6.0, 1.0)
        else -> Dot("faint", 2.0, 0.35)
    }

    /** A place's stretch of colour inside the ink: 0.35 + 1.6 × its share, at most 1; nothing for a place with no days (mark.js:278-282). */
    fun tint(share: Double): Double = if (share > 0) minOf(1.0, 0.35 + 1.6 * share) else 0.0

    data class PoolAt(val x: Double, val y: Double, val r: Double, val days: Int)

    /**
     * The six tide-pools round the centre, in pool order, from the six counts:
     * each placed in P6's order clockwise from the top, sized by its share (by
     * area), an empty one at its own small size (mark.js:918-932).
     */
    fun poolsAt(counts: List<Int>): List<PoolAt> {
        val s = Mark.shares(counts)
        return Mark.POOLS.mapIndexed { i, p ->
            val c = at(-90.0 + Mark.P6.indexOf(p.n) * 60, POOL_RING)
            PoolAt(c.x, c.y, if (s[i] > 0) POOL_MIN + (POOL_MAX - POOL_MIN) * sqrt(s[i]) else POOL_EMPTY, counts[i])
        }
    }

    /** Where a moment spoken at [hourOfDay] (0 to 24) sits on its ring, read as a 24-hour day: noon up, midnight down (mark.js:473-492). */
    fun momentAngle(hourOfDay: Double): Double = -90 + (hourOfDay - 12) * 15

    /** A circle's phrase as the lines it is drawn in: one, or past fourteen letters two, broken where the longer line is shortest (mark.js:902-916). */
    fun captionLines(phrase: String): List<String> {
        val words = phrase.split(' ')
        var best = listOf(phrase)
        var most = phrase.length
        if (most <= 14) return best
        for (k in 1 until words.size) {
            val a = words.subList(0, k).joinToString(" ")
            val b = words.subList(k, words.size).joinToString(" ")
            if (maxOf(a.length, b.length) < most) {
                most = maxOf(a.length, b.length)
                best = listOf(a, b)
            }
        }
        return best
    }

    data class CoilPt(val x: Double, val y: Double, val at: Double)

    /**
     * A day's shell, seen from above: a flat spiral of two and a half turns out
     * from the top, 2.5 to 25 from its centre, in 301 points, each with how far
     * along the coil's length it lies (0 to 1). Drawn in -30..30 (mark.js:934-955).
     */
    fun coilPoints(): List<CoilPt> {
        val xs = DoubleArray(COIL_STEPS + 1)
        val ys = DoubleArray(COIL_STEPS + 1)
        val lens = DoubleArray(COIL_STEPS + 1)
        var len = 0.0
        for (i in 0..COIL_STEPS) {
            val f = i.toDouble() / COIL_STEPS
            val a = (-90 + f * COIL_TURNS * 360) * PI / 180
            val r = COIL_R * (0.1 + 0.9 * f)
            xs[i] = r * cos(a)
            ys[i] = r * sin(a)
            if (i > 0) len += hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1])
            lens[i] = len
        }
        return (0..COIL_STEPS).map { CoilPt(xs[it], ys[it], lens[it] / len) }
    }

    data class Band(val n: Int, val from: Double, val to: Double)

    /** A day's places as bands along its coil, in P6's order from the centre out, equal shares of its length with a gap of 0.03 between (mark.js:956-962). */
    fun coilBands(places: List<Int>): List<Band> {
        val ps = Mark.inOrder(places)
        val n = ps.size
        return ps.mapIndexed { k, q ->
            Band(q, k.toDouble() / n + (if (k != 0) COIL_GAP / 2 else 0.0), (k + 1).toDouble() / n - (if (k < n - 1) COIL_GAP / 2 else 0.0))
        }
    }
}
