package com.phenomemental.pocketbeach.bsp

/**
 * A told day, landed for one Yes (0.6, Stage 4 in the app,
 * spine:pocket-beach 1.111174). The pocket has sorted his telling a piece at
 * a time ([Telling.sorted]); here its lines become one proposal he can
 * correct before Yes: any line unticked, or moved to another place. Each move
 * is kept as a worked example she is shown the next time she sorts.
 *
 * Where it lands, as his structure says (STATUS, "Item 7"):
 * - The day at today's place on his clock ([NOW]): the telling whole as the
 *   day's line, with the six kept with it. The digits beneath a day on the
 *   clock are its day-parts, so the six are kept in the day's hidden
 *   directory (block-conventions 4.7.2, sunstone 1.4), never on its digits.
 * - The hand's cards and the rungs, in his structure block ([TELLING]):
 *   a card already there is updated, not doubled.
 * - His time words ([Whens.BLOCK]) are written with the first Yes, so he can
 *   change them.
 */
object Landing {
    /** His structure block: the six places named, the hand's shelves and the rungs ([Telling.SHAPE]). */
    const val TELLING = "telling"

    /** His clock in the pocket: a block of the clock's form, floor 10, as his now is on the beach. */
    const val NOW = "now"

    /** The places he moved pieces to himself, the newest nine, shown to her when she sorts. */
    const val EXAMPLES = "sort-examples"

    const val NOW_OPENING = "My now, on the clock: each day as I told it, with the six kept with it."
    const val EXAMPLES_OPENING = "How I placed pieces of my day myself, for the companion to learn from: the newest nine."

    /** Where a line can go: the day's six places and the hand's five shelves (the rungs are hers, in his voice, and only ticked or not). */
    val PLACES: List<String> = Telling.DAY + Telling.HAND

    private val NAMES = mapOf(
        "1.1" to "What happened", "1.2" to "What I felt", "1.3" to "What I made of it", "1.4" to "What matters to me",
        "1.5" to "What I chose", "1.6" to "What's ahead",
        "2.1" to "Tomorrow", "2.2" to "This week", "2.3" to "This month", "2.4" to "This season", "2.5" to "This year or beyond",
        "3.1" to "This week", "3.2" to "This month", "3.3" to "This season", "3.4" to "This year",
    )

    /** A place by its plain name. */
    fun name(place: String): String = NAMES[place] ?: place

    /**
     * One line of the proposal: where it goes ([place]; null for a sentence
     * of his that landed nowhere), the card it updates on the hand, his words,
     * whether it is ticked, and where she had put it when he moved it ([from]:
     * "" for a sentence she placed nowhere).
     */
    data class Line(val place: String?, val card: String?, val words: String, val ticked: Boolean = true, val from: String? = null, val heard: String? = null) {
        val moved: Boolean get() = from != null && from != place

        /** He corrected a word the phone misheard: [heard] is what it heard, [words] his now. */
        val corrected: Boolean get() = heard != null && heard != words

        /** His correction of a misheard word ("torque" to "talk"): it counts as his words. */
        fun correctedTo(to: String): Line = to.trim().let { t -> if (t.isEmpty()) this else copy(words = t, heard = heard ?: words) }

        /** He moves it to [to]: kept as a worked example, unless it goes back where she put it. A card it updated is no longer that card's on another shelf. */
        fun movedTo(to: String): Line {
            val first = from ?: place ?: ""
            return copy(place = to, card = if (to == place) card else null, ticked = true, from = first.takeIf { it != to })
        }
    }

    /** The sorted telling as he is shown it: every line kept, and his sentences that landed nowhere, to place himself. */
    class Sorting(val said: String, val lines: List<Line>, val invented: List<Telling.Placed>)

    /**
     * The names the pocket knows, in lower case, so the phone never cuts his
     * speech before one: her name (Gemma, and any name her shell gives
     * her), the names of his blocks, and the words his blocks' lines give a
     * capital where no sentence begins.
     */
    fun names(pages: Pages): Set<String> {
        val out = mutableSetOf("gemma")
        val shore = pages.shore().mine
        for (b in shore) {
            if (b.name == Asks.LOG || b.name.startsWith("bench-")) continue
            out += b.name.lowercase()
            if (pages.floorOf(b.name) >= TEMPORAL_FLOOR) continue
            val texts = listOfNotNull(b.opening) + pages.position(Region.POCKET, b.name)?.lines.orEmpty().mapNotNull { it.text }
            for (t in texts) {
                val words = t.split(Regex("\\s+"))
                for ((i, w) in words.withIndex()) {
                    if (i == 0 || words[i - 1].endsWith('.') || words[i - 1].endsWith('?') || words[i - 1].endsWith('!') || words[i - 1].endsWith(':')) continue
                    val bare = w.trim(',', '.', ';', ':', '!', '?', '"', '\u201c', '\u201d', '(', ')').removeSuffix("'s").removeSuffix("\u2019s")
                    if (bare.length >= 2 && bare.first().isUpperCase() && bare.lowercase() !in setOf("i", "i'm", "i've", "i'll", "i'd")) out += bare.lowercase()
                }
            }
        }
        return out
    }

    fun sorting(said: String, placed: List<Telling.Placed>): Sorting {
        val checked = Telling.check(said, placed)
        val lines = checked.kept.distinctBy { Triple(it.place, it.card, Telling.plain(it.words)) }.map { Line(it.place, it.card, it.words) } +
            checked.dropped.map { Line(null, null, it, ticked = false) }
        return Sorting(said, lines, checked.invented)
    }

    /**
     * The proposal whole, as the saved conversation keeps it (0.6.1): every
     * line with its place, ticked or not, moved or not, corrected or not, the
     * rungs, his sentences she placed nowhere, and what was left out as not
     * his words.
     */
    fun describe(lines: List<Line>, invented: List<Telling.Placed>): String = buildString {
        fun group(place: String?) = when (place) {
            null -> "Not placed"
            in Telling.DAY -> "Today, ${name(place)}"
            in Telling.HAND -> "The hand, ${name(place)}"
            else -> "The rung above, ${name(place)}"
        }
        for (l in lines.sortedBy { (PLACES + Telling.RUNGS).indexOf(it.place).let { i -> if (i < 0) 99 else i } }) {
            append(if (l.ticked && l.place != null) "  [x] " else "  [ ] ")
            append(group(l.place)).append(": \u201c").append(l.words).append("\u201d")
            if (l.card != null) append(" (updates the card at ${l.place}.${l.card})")
            if (l.moved) append(" (moved by him from ${l.from?.takeIf { it.isNotEmpty() }?.let(::group) ?: "not placed"})")
            if (l.corrected) append(" (corrected by him; heard \u201c${l.heard}\u201d)")
            appendLine()
        }
        if (invented.isNotEmpty()) appendLine("  Left out, as not his words: " + invented.joinToString("; ") { "${it.place} \u201c${it.words}\u201d" })
    }.trimEnd()

    // ── what is written ──

    /** The day's clock address, full width, for [nowMs]: its eight digits and two zeros (the beach's clock is UTC). */
    fun dayAddress(nowMs: Long): String = momentToAddress(nowMs).take(8) + "00"

    /** A worked example from a move: his words and the word she would answer for the place he chose. */
    fun example(line: Line): Pair<String, String>? {
        if (!line.moved || !line.ticked) return null
        val place = line.place ?: return null
        val word = Telling.DAY_WORDS.firstOrNull { it.second == place }?.first ?: if (place in Telling.HAND) "ahead" else return null
        return line.words to word
    }

    private val EXAMPLE = Regex("""^“(.+)”\s*→\s*([a-z]+)$""")

    /** The worked examples [block] keeps, oldest first. */
    fun examples(block: Js?): List<Pair<String, String>> = (block as? JsObj)?.let { b ->
        (1..9).mapNotNull { (b["$it"] as? JsStr)?.value?.let(EXAMPLE::find)?.destructured?.let { (w, word) -> w to word } }
    }.orEmpty()

    /** The examples block with [added] after what it keeps: the newest nine. */
    fun withExamples(block: Js?, added: List<Pair<String, String>>): JsObj {
        val all = (examples(block) + added).distinctBy { Telling.plain(it.first) }.takeLast(9)
        val out = JsObj.of("_" to str(((block as? JsObj)?.let(::collectUnderscore)) ?: EXAMPLES_OPENING))
        all.forEachIndexed { i, (w, word) -> out["${i + 1}"] = str("“$w” → $word") }
        return out
    }

    /** A clock of the floor-10 form, with nothing on it yet. */
    fun emptyClock(opening: String = NOW_OPENING): JsObj {
        var node: Js = str(opening)
        repeat(10) { node = JsObj.of("_" to node) }
        return node as JsObj
    }

    /**
     * [now] (or a new clock) with the day at [address]: the telling as the
     * day's line, after any line the day had, and the six in the day's hidden
     * directory, after the lines they had. A place keeps nine lines; what
     * would go past nine is returned, not written.
     */
    fun withDay(now: Js?, address: String, telling: String, six: Map<String, List<String>>): Pair<JsObj, List<String>> {
        val root = (now as? JsObj)?.let { parseJson(stringify(it)!!) as JsObj } ?: emptyClock()
        var node = root
        for (d in address.take(8)) {
            val key = if (d == '0') "_" else "$d"
            val next = when (val c = node[key]) {
                is JsObj -> c
                is JsStr -> JsObj.of("_" to c)
                else -> JsObj()
            }
            node[key] = next
            node = next
        }
        val dir = when (val u = node["_"]) {
            is JsObj -> u
            is JsStr -> JsObj.of("_" to u)
            else -> JsObj()
        }
        val had = (dir["_"] as? JsStr)?.value?.takeIf { it.isNotBlank() }
        dir["_"] = str(if (had == null) telling.trim() else had + "\n\n" + telling.trim())
        val over = mutableListOf<String>()
        for ((i, place) in Telling.DAY.withIndex()) {
            val words = six[place].orEmpty()
            if (words.isEmpty()) continue
            val at = when (val p = dir["${i + 1}"]) {
                is JsObj -> p
                is JsStr -> JsObj.of("_" to p)
                else -> JsObj.of("_" to str(name(place)))
            }
            for (w in words) {
                val free = (1..9).firstOrNull { !at.has("$it") }
                if (free == null) over += w else at["$free"] = str(w)
            }
            dir["${i + 1}"] = at
        }
        node["_"] = dir
        return root to over
    }

    /** What one Yes writes: each block whole, in order, and what couldn't go (past nine lines in a place). */
    class Landed(val blocks: List<Pair<String, JsObj>>, val over: List<String>, val examples: List<Pair<String, String>>)

    /**
     * The blocks [lines] leave, from the pocket as it stands ([read] gives a
     * block by name, null when there is none): his clock with today's day,
     * his structure block with the hand and the rungs, his time words if he
     * has none yet, and the examples his moves make.
     */
    fun landed(lines: List<Line>, said: String, nowMs: Long, read: (String) -> Js?): Landed {
        val kept = lines.filter { it.ticked && it.place != null }
        val six = Telling.DAY.associateWith { place -> kept.filter { it.place == place }.map { it.words } }
        // His corrections of misheard words are his words, in the telling as the day's line too.
        val told = lines.filter { it.corrected }.fold(said) { t, l -> l.heard?.let { h -> t.replaceFirst(h, l.words) } ?: t }
        val (now, over) = withDay(read(NOW), dayAddress(nowMs), told, six)
        val state = (read(TELLING) as? JsObj) ?: Telling.shape()
        val hand = Telling.land(state, kept.filter { it.place in Telling.HAND || it.place in Telling.RUNGS }.map { Telling.Placed(it.place!!, it.card, it.words) })
        val examples = lines.mapNotNull(::example)
        val blocks = buildList {
            if (read(Whens.BLOCK) == null) add(Whens.BLOCK to Whens.first())
            // His structure block only when its hand or rungs change.
            if (read(TELLING) == null || !deepEqual(hand, state)) add(TELLING to hand)
            add(NOW to now)
            if (examples.isNotEmpty()) add(EXAMPLES to withExamples(read(EXAMPLES), examples))
        }
        return Landed(blocks, over, examples)
    }

    /** The writes for one Yes, each planned whole on the pocket as it stands. */
    fun plans(landed: Landed, pages: Pages): List<Plan> = landed.blocks.map { (name, block) -> pages.planWhole(name, block) }

    // ── when his words are a telling ──

    /**
     * Whether his words are a day told, to be sorted: three sentences or
     * more, of forty words or more, not a question; or news of a card on his
     * hand (a sentence sharing words with one). An ask to write is the Write
     * job's, never a telling.
     */
    fun isTelling(said: String, cards: Map<String, String>, names: Set<String> = emptySet()): Boolean {
        val trimmed = said.trim()
        if (trimmed.isEmpty() || trimmed.endsWith("?")) return false
        val words = Telling.plain(trimmed).split(' ').filter { it.isNotEmpty() }
        if (words.firstOrNull() in ASKS) return false
        if (Telling.sentences(trimmed, names).size >= 3 && words.size >= 40) return true
        return Telling.cut(trimmed, names).any { Telling.cardsNear(it, cards).isNotEmpty() }
    }

    private val ASKS = setOf("add", "note", "change", "make", "post", "put", "write", "keep", "remember", "save", "remove", "delete", "rename", "read", "show", "tell", "do")
}
