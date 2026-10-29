package com.phenomemental.pocketbeach.bsp

/**
 * One telling, three landings (0.5's item 7, Matthew, 29 October). He tells
 * his day once, in whatever order it comes; the pocket hands it back sorted
 * and landed: the day six ways (P6's points in plain words), the forward bits
 * on the hand's shelves, the rungs above refreshed. The structure is a block
 * in the pocket, not her instruction: she is given the block, today's date
 * and what he said, and gives the block back filled.
 *
 * The pocket checks before anything lands: every line of the day and the hand
 * is found in what he said, or it is refused as invented; every sentence lands
 * somewhere in the day, or it is listed as dropped. A card already on the hand
 * is updated, not doubled.
 *
 * [Key] and [marks] are the test: the key to his written telling, and the
 * marks he set (nothing invented, nothing dropped, the three doubles, every
 * line in the first person). [plainAsk] and [fromPlain] are the same Gemma
 * with no pocket beside it: the difference is what the structure adds.
 */
object Telling {
    /** The structure, as Matthew wrote it. */
    val SHAPE = """
{"_": "One telling, three landings: my day, told once in any order, kept in my pocket. In the day and the hand every line is my own words, copied exactly; a piece may stand in more than one place; nothing is added.",
 "1": {"_": "THE DAY: what I said, sorted six ways",
  "1": "What happened: what could be seen or checked, before any meaning",
  "2": "What I felt: in my body and in me",
  "3": "What I made of it: my conclusions, judgements and the stories I tell",
  "4": "What matters to me: how I want to be",
  "5": "What I chose: the choice I've made",
  "6": "What's ahead: what's coming, planned or expected"},
 "2": {"_": "THE HAND: what I say I'll do, one card each, on the shelf for when; a card already there is updated, not doubled",
  "1": "Tomorrow", "2": "This week", "3": "This month", "4": "This season", "5": "This year or beyond"},
 "3": {"_": "THE RUNGS ABOVE: one line each in my own voice, as what I intend, drawn only from what I said",
  "1": "This week", "2": "This month", "3": "This season", "4": "This year"}}
""".trim()

    fun shape(): JsObj = parseJson(SHAPE) as JsObj

    /** The day's six places, the hand's five shelves, the four rungs, by address. */
    val DAY = (1..6).map { "1.$it" }
    val HAND = (1..5).map { "2.$it" }
    val RUNGS = (1..4).map { "3.$it" }

    /** The date the test is fixed at, so the key holds. */
    const val BENCH_DATE = "Thursday 29 October 2026"

    /** All she is told besides the block: how to give it back. What goes where is the block's own. */
    const val INSTRUCTION = "You are given a block, today's date and what Matthew said. Give the block back filled: " +
        "each piece of his words, copied, as a line beneath every place it belongs. Write each line as the place's " +
        "address, a colon, then the words (1.1: …). A card already on the hand keeps its own address (2.1.1: …)."

    /** What she is asked: today's date, the block as it stands (the hand's cards and the rungs in it) and what he said. */
    fun ask(state: JsObj, date: String, said: String): String =
        "Today is $date.\n\nThe block:\n${stringifyPretty(state, " ")}\n\nWhat Matthew said:\n${said.trim()}"

    /** One line of hers: its place (1.1 to 3.4), the card it names on a shelf if any, and the words. */
    data class Placed(val place: String, val card: String?, val words: String)

    private val LINE = Regex("""^[\s>*•-]*\**\s*([123])\.([1-9])(?:\.([1-9]))?(?:\.[1-9])*\s*\**\s*[:–-]\s*(.*)$""")

    /**
     * Her lines, read from her answer: each "address: words" at a place of
     * the block, or the block given back filled (strict JSON or not quite),
     * his words beneath its places. The block's own words given back are
     * passed over, as is anything else.
     */
    fun placed(answer: String): List<Placed> =
        (lines(answer) + filled(answer)).filter { !echo(it.words) }.map(::unlabelled).filter { it.words.isNotEmpty() && !echo(it.words) }

    /** A place's own words, which she sometimes writes before his: "What I felt: in my body and in me: Felt rotten". */
    private fun unlabelled(p: Placed): Placed {
        val (top, at) = p.place.split(".")
        val node = (shape()[top] as JsObj)[at]
        val label = collectUnderscore(node) ?: (node as? JsStr)?.value ?: return p
        var words = p.words
        for (prefix in listOf(label, label.substringBefore(':'))) {
            while (words.startsWith(prefix, ignoreCase = true) && words.length > prefix.length && words[prefix.length] in ":-–") {
                words = words.substring(prefix.length).trimStart(':', '-', '–', ' ')
            }
        }
        return if (words == p.words) p else p.copy(words = words)
    }

    private fun lines(answer: String): List<Placed> {
        val all = answer.lines().map { it.trim() }
        return all.indices.mapNotNull { i ->
            val m = LINE.matchEntire(all[i]) ?: return@mapNotNull null
            val (top, at, card, rest) = m.destructured
            var words = bare(rest)
            // A heading with the place's own name, his words on the line beneath: "**3.1: This week**", then "I will …".
            if (words.isEmpty() || echo(words)) {
                val next = all.drop(i + 1).firstOrNull { it.isNotEmpty() }
                if (next != null && LINE.matchEntire(next) == null) words = bare(next.removePrefix("- ").removePrefix("* "))
            }
            at(top, at, card, words)
        }
    }

    private fun at(top: String, at: String, card: String, words: String): Placed? {
        val place = "$top.$at"
        if (place !in DAY && place !in HAND && place !in RUNGS) return null
        return if (words.isEmpty() || words.lowercase() in NOTHING) null else Placed(place, card.takeIf { top == "2" && it.isNotEmpty() }, words)
    }

    /** The block's own words, which she sometimes gives back with hers. */
    private val OWN: Set<String> by lazy {
        fun all(node: Js?): List<String> = when (node) {
            is JsStr -> listOf(node.value)
            is JsObj -> node.keys().flatMap { all(node[it]) }
            else -> emptyList()
        }
        all(shape()).map(::plain).toSet()
    }

    private fun echo(words: String) = plain(words).let { w -> w in OWN || OWN.any { it.endsWith(" $w") } }

    private val KEYED = Regex("^\"([1-9_])\"\\s*:")
    private val QUOTED = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"")

    /** The block given back filled: each string beneath a place, read by its path of keys. */
    private fun filled(answer: String): List<Placed> {
        val start = answer.indexOf('{')
        if (start < 0) return emptyList()
        runCatching { parseJson(answer.substring(start, answer.lastIndexOf('}') + 1)) }.getOrNull()?.let { block ->
            val out = mutableListOf<Placed>()
            fun walk(node: Js?, path: List<String>) {
                when (node) {
                    is JsStr -> if (path.size >= 2 && path.none { it == "_" }) at(path[0], path[1], path.getOrNull(2) ?: "", node.value.trim())?.let { out += it }
                    is JsObj -> node.keys().forEach { walk(node[it], path + it) }
                    else -> {}
                }
            }
            walk(block, emptyList())
            return out
        }
        // Not quite JSON, as she writes it sampled: "1": "What happened: …": "his words". The path is kept by the braces.
        val path = ArrayDeque<String>()
        val out = mutableListOf<Placed>()
        for (raw in answer.substring(start).lines()) {
            val t = raw.trim()
            val key = KEYED.find(t)?.groupValues?.get(1)
            if (t.trimEnd(',').endsWith("{")) { path.addLast(key ?: ""); continue }
            if (t.startsWith("}")) { path.removeLastOrNull(); continue }
            if (key == null || key == "_") continue
            val full = path.filter { it.isNotEmpty() } + key
            if (full.size < 2 || "_" in full) continue
            val words = QUOTED.findAll(t).map { it.groupValues[1] }.drop(1).lastOrNull()?.replace("\\\"", "\"")?.trim() ?: continue
            at(full[0], full[1], full.getOrNull(2) ?: "", words)?.let { out += it }
        }
        return out
    }

    private val NOTHING = setOf("none", "nothing", "n/a", "-", "—")

    /** Words without the marks around them: quotes, bold, a stop at the end is kept. */
    private fun bare(s: String) = s.trim().removePrefix("**").removeSuffix("**").trim().trim('"', '“', '”', '\'').trim()

    /** Words as compared for "copied exactly": the same words in the same order, whatever the capitals and the punctuation. */
    fun plain(s: String): String = s.lowercase().replace(Regex("[‘’`]"), "'").replace(Regex("[^\\p{L}\\p{N}']+"), " ").trim()

    /** Whether [part] is found, word for word, in [whole]. */
    fun foundIn(part: String, whole: String): Boolean {
        val p = plain(part)
        return p.isNotEmpty() && " ${plain(whole)} ".contains(" $p ")
    }

    /** What he said, a sentence at a time. */
    fun sentences(said: String): List<String> = spokenCut(said).trim().split(Regex("(?<=[.!?])\\s+")).map { it.trim() }
        .filter { plain(it).isNotEmpty() && plain(it).split(' ').any { w -> w !in JOINERS } }

    /** What the pocket found before anything lands: the lines kept, those refused as invented, and the sentences dropped. */
    class Checked(val kept: List<Placed>, val invented: List<Placed>, val dropped: List<String>)

    fun check(said: String, placed: List<Placed>): Checked {
        val invented = placed.filter { (it.place in DAY || it.place in HAND) && !foundIn(it.words, said) }
        val kept = placed - invented.toSet()
        val day = kept.filter { it.place in DAY }
        val dropped = sentences(said).filter { s -> day.none { foundIn(it.words, s) || foundIn(s, it.words) } }
        return Checked(kept, invented, dropped)
    }

    /** The cards on the hand, by address (2.1.1, …), and the rung lines (3.1.1, …), as the block holds them. */
    fun cards(state: JsObj): Map<String, String> = beneath(state, HAND)

    fun rungs(state: JsObj): Map<String, String> = beneath(state, RUNGS)

    private fun beneath(state: JsObj, places: List<String>): Map<String, String> = buildMap {
        for (place in places) {
            val (top, at) = place.split(".")
            val node = (state[top] as? JsObj)?.get(at) as? JsObj ?: continue
            for (k in node.keys().filter { it != "_" }) (node[k] as? JsStr)?.let { put("$place.$k", it.value) }
        }
    }

    /**
     * The block with her kept lines landed on the hand and the rungs: a card
     * she names at its own address is updated; a card whose words are already
     * on the hand is not doubled; else it goes on its shelf, at the next free
     * place. Each rung's line is refreshed. The day lands on the clock, not
     * here.
     */
    fun land(state: JsObj, kept: List<Placed>): JsObj {
        val out = parseJson(stringify(state)!!) as JsObj
        fun place(address: String): JsObj {
            val (top, at) = address.split(".")
            val group = out[top] as JsObj
            return when (val node = group[at]) {
                is JsObj -> node
                is JsStr -> JsObj.of("_" to node).also { group[at] = it }
                else -> JsObj().also { group[at] = it }
            }
        }
        for (p in kept.filter { it.place in HAND }) {
            val shelf = place(p.place)
            when {
                p.card != null && shelf.has(p.card) -> shelf[p.card] = str(p.words)
                cards(out).values.any { plain(it) == plain(p.words) } -> {}
                else -> (1..9).map(Int::toString).firstOrNull { !shelf.has(it) }?.let { shelf[it] = str(p.words) }
            }
        }
        for ((rung, lines) in kept.filter { it.place in RUNGS }.groupBy { it.place }) place(rung)["1"] = str(lines.first().words)
        return out
    }

    // ── A piece at a time: the phone cuts, she labels ──

    private val JOINERS = setOf("and", "but", "so", "anyway", "right", "well")
    private val CUT_BEFORE = Regex("\\s+(?=(?:and|but|so|because|which)\\b)", RegexOption.IGNORE_CASE)

    /**
     * What he said, cut into pieces by the phone, each a run of his words as
     * he said them: at each stop or comma, and before "and", "but", "so",
     * "because" and "which" where both sides keep two words or more. A joiner
     * left at a piece's start ("and", "so", "anyway", "right") goes; no word
     * is changed or added.
     */
    fun cut(said: String): List<String> = spokenCut(said).split(Regex("[.,;:!?]+")).flatMap { segment ->
        val parts = mutableListOf<String>()
        var rest = segment.trim()
        while (true) {
            val at = CUT_BEFORE.findAll(rest).map { it.range.first }.firstOrNull { i ->
                words(rest.substring(0, i)) >= 2 && words(rest.substring(i)) >= 3
            } ?: break
            parts += rest.substring(0, at).trim()
            rest = rest.substring(at).trim()
        }
        parts + rest
    }.map(::unjoined).filter { plain(it).isNotEmpty() && plain(it) !in JOINERS }

    private fun words(s: String) = plain(s).split(' ').count { it.isNotEmpty() }

    private val AFTER = setOf("on", "at", "in", "to", "with", "for", "by", "from", "of", "the", "a", "an", "and", "or", "my", "his", "her", "dear")
    private val I_FORMS = setOf("i", "i'm", "i've", "i'll", "i'd")

    /**
     * Speech as the phone hears it comes without stops, its sentences begun
     * with a capital. Unpunctuated, a stop is put before each capital that
     * begins one: not "I" and its forms, nor a name after "on", "the" and
     * their like. Written words, which have their stops, pass as they are.
     */
    private fun spokenCut(said: String): String {
        val words = said.trim().split(Regex("\\s+"))
        if (words.size < 30 || Regex("[.!?]").findAll(said).count() * 30 >= words.size) return said
        val out = StringBuilder()
        for ((i, w) in words.withIndex()) {
            val first = w.firstOrNull()
            val before = words.getOrNull(i - 1)?.lowercase()?.trim(',', ';')
            val lower = w.lowercase().trim(',', '.')
            // "I've" begins a sentence in speech ("… looking at me I've been …") unless a joiner comes first ("and I've").
            val begins = first != null && first.isUpperCase() && (lower !in I_FORMS || lower == "i've" && before !in JOINERS && before !in setOf("that", "which", "decided", "think", "know", "because"))
            if (i > 0 && begins && before !in AFTER) out.append(". ")
            else if (i > 0) out.append(' ')
            out.append(w)
        }
        return out.toString()
    }

    private fun unjoined(piece: String): String {
        var p = piece.trim()
        while (true) {
            val first = p.substringBefore(' ')
            if (first.lowercase() !in JOINERS || first.length == p.length) break
            p = p.substring(first.length).trim()
        }
        // A joiner left at the end, where speech had no comma before the next sentence: "… went on forever but".
        while (true) {
            val last = p.substringAfterLast(' ')
            if (last.lowercase() !in JOINERS || last.length == p.length) return p
            p = p.substring(0, p.length - last.length).trim()
        }
    }

    /** What she is told for the pieces: the block as it stands, today's date and all he said; then each piece is one short choice. */
    fun piecesInstruction(state: JsObj, today: java.time.LocalDate, said: String): String {
        val cards = cards(state)
        return "You sort what Matthew said about his day into this block, one piece at a time.\n\nThe block:\n${stringifyPretty(state, " ")}\n\n" +
            "Today is ${date(today)}. What Matthew said:\n${said.trim()}\n\n" +
            "Each message is one piece of what he said. Answer only with addresses: first one or two places in the day, of 1.1 what " +
            "happened, 1.2 what I felt, 1.3 what I made of it, 1.4 what matters to me, 1.5 what I chose, 1.6 what's ahead; then, only " +
            "if it is something he says he'll do, one shelf, of " + shelves(today).joinToString(", ") +
            (if (cards.isEmpty()) "" else "; or, if it is about a card already on the hand, that card, of " + cards.entries.joinToString(", ") { "${it.key} \u201c${it.value}\u201d" }) + "."
    }

    /** The hand's shelves, each with the day it runs to, from [today]: the pocket's reckoning, not hers. */
    fun shelves(today: java.time.LocalDate): List<String> {
        fun day(d: java.time.LocalDate) = d.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM", java.util.Locale.UK))
        return listOf(
            "2.1 tomorrow (${day(today.plusDays(1))})",
            "2.2 this week (by ${day(today.plusDays(7))})",
            "2.3 this month (by ${day(today.plusDays(30))})",
            "2.4 this season (by ${day(today.plusMonths(3))})",
            "2.5 this year or beyond",
        )
    }

    /** Today, as she is told it: "Thursday 29 October 2026". */
    fun date(today: java.time.LocalDate): String = today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", java.util.Locale.UK))

    /** The day the bench is fixed at, so the key holds. */
    val BENCH_DAY: java.time.LocalDate = java.time.LocalDate.of(2026, 10, 29)

    /** The day's six places and their meanings, in the block's own words: "1.1 What happened: what could be seen or checked, …". */
    fun dayPlaces(): String = DAY.joinToString("; ") { place -> "$place " + ((shape()["1"] as JsObj)[place.substringAfter('.')] as JsStr).value }

    /** The sentence of what he said that a piece comes from. */
    fun sentenceOf(piece: String, said: String): String = sentences(said).firstOrNull { foundIn(piece, it) } ?: said.trim()

    /**
     * Each piece on its own, in a conversation of its own, so no answer leans
     * on the last: the six places and the shelves, the sentence it comes from,
     * and the piece. Short, so it is read quickly afresh.
     */
    fun aloneInstruction(today: java.time.LocalDate, cards: Map<String, String>): String =
        "You place one piece of what Matthew said about his day. Today is ${date(today)}.\n" +
            "The day's places: ${dayPlaces()}.\n" +
            "The hand's shelves, for something he says he'll do: " + shelves(today).joinToString(", ") +
            (if (cards.isEmpty()) "" else ". Cards already on the hand: " + cards.entries.joinToString(", ") { "${it.key} \u201c${it.value}\u201d" }) + ".\n" +
            "Answer only with addresses: one or two places in the day; then, only if the piece is something he says he'll do, its shelf" +
            (if (cards.isEmpty()) "." else ", or the card it is about.")

    fun aloneAsk(piece: String, said: String): String = "He said: \u201c${sentenceOf(piece, said)}\u201d\nThe piece: \u201c$piece\u201d"

    /** The words she may answer a piece with, and the places they stand for: the day's six, then the hand's five shelves. */
    val WORDS: List<Pair<String, String>> = listOf(
        "happened" to "1.1", "felt" to "1.2", "made of it" to "1.3", "matters" to "1.4", "chose" to "1.5", "ahead" to "1.6",
        "tomorrow" to "2.1", "this week" to "2.2", "this month" to "2.3", "this season" to "2.4", "this year or beyond" to "2.5",
    )

    /**
     * A piece alone, answered in words, not addresses: the pocket maps each
     * word to its place. On the laptop she leaned to the first numbers in a
     * list; a word carries its meaning.
     */
    fun wordsInstruction(today: java.time.LocalDate, cards: Map<String, String>): String {
        val shelf = shelves(today).map { it.substringAfter(' ') }
        return "You place one piece of what Matthew said about his day. Today is ${date(today)}.\n" +
            "Answer with one or two words for the day: happened (what could be seen or checked, before any meaning), felt (in his " +
            "body and in him), made of it (his conclusions, judgements and the stories he tells), matters (how he wants to be), " +
            "chose (a choice he has made), ahead (what's coming, planned or expected).\n" +
            "Then, only if it is something he says he'll do, when: " + shelf.joinToString(", ") +
            (if (cards.isEmpty()) "." else "; or, if it is about a card already on the hand, card and its number: " +
                cards.entries.mapIndexed { i, (_, words) -> "card ${i + 1} \u201c$words\u201d" }.joinToString(", ") + ".") +
            "\nFor example: felt; or: chose, ahead, this week"
    }

    /** Her words for a piece, as places: the day's, a shelf, or the card she names. */
    fun fromWords(answer: String, cards: Map<String, String>): List<Pair<String, String?>> {
        val said = plain(answer.trim().lineSequence().firstOrNull().orEmpty())
        val out = mutableListOf<Pair<String, String?>>()
        // The longest names first, so "this year or beyond" is not read as "this year".
        for ((word, place) in WORDS.sortedByDescending { it.first.length }) {
            if (Regex("(^| )${Regex.escape(plain(word))}( |$)").containsMatchIn(said) && out.none { it.first == place }) out += place to null
        }
        if (said.contains("this year") && out.none { it.first == "2.5" }) out += "2.5" to null
        Regex("card (\\d)").findAll(said).forEach { m -> cards.keys.elementAtOrNull(m.groupValues[1].toInt() - 1)?.let { out += it.substringBeforeLast('.') to it.substringAfterLast('.') } }
        val order = WORDS.map { it.second }
        return out.sortedBy { order.indexOf(it.first).let { i -> if (i < 0) 99 else i } }
    }

    private val SMALL = setOf(
        "a", "an", "the", "and", "or", "but", "so", "to", "of", "in", "on", "at", "for", "with", "by", "from", "up", "it", "its", "it's",
        "i", "i'm", "i've", "me", "my", "you", "your", "he", "his", "him", "was", "is", "are", "be", "been", "that", "this", "which",
        "about", "again", "not", "no", "as", "same", "some", "thing", "one", "into", "all", "had", "have", "has", "will", "would",
    )

    private fun content(s: String) = plain(s).split(' ').filter { it.isNotEmpty() && it !in SMALL }.map { it.removeSuffix("s").removeSuffix("'") }.toSet()

    /**
     * Her lines in her own words, anchored to his: each piece of what he said
     * goes where she put a line sharing most of its words (half or more of
     * the piece's own words, and two at least). Only his words land; she
     * sorts.
     */
    fun anchored(free: List<Placed>, said: String): List<Placed> {
        val pieces = cut(said)
        val out = mutableListOf<Placed>()
        for (line in free) {
            val words = content(line.words)
            for (piece in pieces) {
                val own = content(piece)
                val shared = (own intersect words).size
                if (own.isNotEmpty() && shared >= 2 && shared * 2 >= own.size || own.size == 1 && shared == 1) {
                    val p = Placed(line.place, line.card, piece)
                    if (p !in out) out += p
                }
            }
            if (line.place in RUNGS) out += line
        }
        return out
    }

    // ── Stage 4: when by code; she judges the day's other places, a piece at a time ──

    /** The words she answers a piece with, and the day's places they stand for. */
    val DAY_WORDS: List<Pair<String, String>> = listOf(
        "happened" to "1.1", "felt" to "1.2", "thought" to "1.3", "matters" to "1.4", "chose" to "1.5", "ahead" to "1.6",
    )

    /**
     * What she is told for each piece: the day's six places, each a word.
     * When it falls is the pocket's to reckon. [examples] are pieces he
     * placed himself before (0.6: each move kept as a worked example), each
     * with the word for the place he chose.
     */
    fun sortInstruction(today: java.time.LocalDate, examples: List<Pair<String, String>> = emptyList()): String =
        "You sort one piece of what Matthew said about his day. Today is ${date(today)}. Answer with one word: " +
            "happened (what could be seen or checked, before any meaning), felt (what he felt, in his body and in him), " +
            "thought (his conclusions, judgements and the stories he tells), matters (how he wants to be), " +
            "chose (a choice he has made) or ahead (what's coming, planned or expected). If it belongs in two, give both." +
            if (examples.isEmpty()) "" else "\nMatthew placed these himself:\n" + examples.joinToString("\n") { (piece, word) -> "\u201c$piece\u201d: $word" }

    /** Her words for a piece, as places of the day: the first two of the six words in her answer, in her order. */
    fun dayWords(answer: String): List<String> {
        val said = " " + plain(answer.trim().lineSequence().firstOrNull().orEmpty()) + " "
        return DAY_WORDS.mapNotNull { (w, place) -> said.indexOf(" $w ").takeIf { it >= 0 }?.let { it to place } }.sortedBy { it.first }.map { it.second }.take(2)
    }

    fun dayWord(answer: String): String? = dayWords(answer).firstOrNull()

    /** The cards a piece may be about: those sharing a word of substance with it. */
    fun cardsNear(piece: String, cards: Map<String, String>): Map<String, String> = cards.filter { (_, words) -> (content(piece) intersect content(words)).isNotEmpty() }

    /** Asked of a piece that shares words with a card on his hand: one short choice, yes or no. */
    fun cardAsk(piece: String, said: String, card: String): String =
        "A card on Matthew's hand says: \u201c$card\u201d\nToday he said: \u201c${sentenceOf(piece, said)}\u201d\n" +
            "Is \u201c$piece\u201d about the same thing as the card, even if it is done now? Answer yes or no."

    fun yes(answer: String): Boolean = plain(answer).startsWith("yes")

    /** One piece, sorted: its places, and when it falls if it carries a when. */
    data class Sorted(val piece: String, val places: List<String>, val whenBy: String?, val card: String?)

    /**
     * The telling sorted in small steps: code cuts it into pieces of his
     * words and reckons when each falls from the time words in [words]; a
     * piece with a when goes on its shelf and is also what's ahead. [judge]
     * is her one word for a piece's place in the day. A piece sharing words
     * with a card on the hand is put to her, [about], one card at a time;
     * a sentence about a card updates it, whole, and makes no new card.
     */
    fun sorted(
        said: String,
        today: java.time.LocalDate,
        words: JsObj,
        state: JsObj,
        judge: (piece: String) -> String,
        about: (piece: String, card: String) -> Boolean,
    ): Pair<List<Placed>, List<Sorted>> {
        val cards = cards(state)
        val out = mutableListOf<Placed>()
        val steps = mutableListOf<Sorted>()
        val updates = mutableMapOf<String, String>()
        val sentenceOfCard = mutableMapOf<String, String>()
        val pieces = cut(said)
        for (piece in pieces) {
            val places = mutableListOf<String>()
            places += dayWords(judge(piece))
            val w = Whens.of(piece, today, words)
            if (w != null && "1.6" !in places) places += "1.6"
            val card = cardsNear(piece, cards).entries.firstOrNull { (_, words) -> about(piece, words) }?.key
            if (card != null) sentenceOfCard.putIfAbsent(sentenceOf(piece, said), card)
            for (p in places) out += Placed(p, null, piece)
            steps += Sorted(piece, places + listOfNotNull(w?.shelf), w?.by, card)
        }
        for (step in steps) {
            val sentence = sentenceOf(step.piece, said)
            val card = sentenceOfCard[sentence]
            when {
                card != null -> updates[card] = sentence.trim().trimEnd('.', '!', '?')
                step.whenBy != null -> out += Placed(step.places.last(), null, step.piece)
            }
        }
        for ((address, words) in updates) out += Placed(address.substringBeforeLast('.'), address.substringAfterLast('.'), words)
        return out to steps
    }

    /** All the pieces at once, numbered, each answered on a line of its own: "7: 1.6, 2.2". */
    fun allAsk(state: JsObj, today: java.time.LocalDate, said: String, pieces: List<String>): String {
        val cards = cards(state)
        return "Today is ${date(today)}.\n\nThe block:\n${stringifyPretty(state, " ")}\n\nWhat Matthew said:\n${said.trim()}\n\n" +
            "His words, cut into pieces:\n" + pieces.mapIndexed { i, p -> "${i + 1}. \u201c$p\u201d" }.joinToString("\n") + "\n\n" +
            "For each piece write one line: its number, a colon, then the addresses of its places: one or two places in the day " +
            "(1.1 to 1.6); then, only if it is something he says he'll do, its shelf, of " + shelves(today).joinToString(", ") +
            (if (cards.isEmpty()) "" else "; or, if it is about a card already on the hand, that card, of " + cards.entries.joinToString(", ") { "${it.key} \u201c${it.value}\u201d" }) +
            ". For example: 1: 1.1"
    }

    /** Her lines for all the pieces: each piece and its places, by its number. */
    fun fromAll(answer: String, pieces: List<String>): List<Pair<String, List<Pair<String, String?>>>> {
        val byNumber = answer.lines().mapNotNull { line ->
            val m = Regex("^[\\s*-]*(\\d+)[.):]\\s*(.*)$").find(line.trim()) ?: return@mapNotNull null
            m.groupValues[1].toInt() to places(m.groupValues[2])
        }.toMap()
        return pieces.mapIndexed { i, p -> p to (byNumber[i + 1] ?: emptyList()) }
    }

    /** One piece, as she is given it; its choices are in what she was told. */
    fun pieceAsk(piece: String): String = "\u201c$piece\u201d"

    /** The longest answer to a piece: its addresses, a line. */
    const val PIECE_TOKENS = 16

    /**
     * The form her answer to a piece is held to: it begins with a place in
     * the day, so every piece lands there; the rest is free, and only its
     * first line is read. (A form held to the end failed in the engine at the
     * space after a comma, on the laptop.)
     */
    const val PIECE_FORM = "1\\.[1-6][\\s\\S]*"

    /** Her places for a piece: the addresses she gave that are places of the block or cards on its hand. */
    fun places(answer: String): List<Pair<String, String?>> = Regex("([12])\\.([1-9])(?:\\.([1-9]))?").findAll(answer.trim().lineSequence().first()).mapNotNull { m ->
        val (top, at, card) = m.destructured
        val place = "$top.$at"
        if (place in DAY || place in HAND) place to card.takeIf { top == "2" && it.isNotEmpty() } else null
    }.distinct().toList()

    /** The rungs, asked on their own after the pieces: the block, today and what he said, and four lines in his voice. */
    fun rungsAsk(state: JsObj, today: java.time.LocalDate, said: String): String =
        "Today is ${date(today)}.\n\nThe block:\n${stringifyPretty(state, " ")}\n\nWhat Matthew said:\n${said.trim()}\n\n" +
            "Write the rungs above: one line each, in his own voice (I …), as what he intends, drawn only from what he said. " +
            "Answer with four lines, 3.1: for this week, 3.2: this month, 3.3: this season, 3.4: this year."

    /**
     * Her lines from the pieces and their places. Pieces she put on the same
     * card become one card: his words from the first piece to the last, as he
     * said them.
     */
    fun fromPieces(said: String, labelled: List<Pair<String, List<Pair<String, String?>>>>, rungs: String): List<Placed> {
        val out = mutableListOf<Placed>()
        val toCards = mutableMapOf<String, MutableList<String>>()
        for ((piece, places) in labelled) for ((place, card) in places) {
            if (card != null) toCards.getOrPut("$place.$card") { mutableListOf() } += piece else out += Placed(place, null, piece)
        }
        for ((address, pieces) in toCards) {
            val starts = pieces.map { said.indexOf(it) }
            val words = if (starts.any { it < 0 }) pieces.joinToString(", ")
            else said.substring(starts.min(), pieces.indices.maxOf { starts[it] + pieces[it].length })
            out += Placed(address.substringBeforeLast('.'), address.substringAfterLast('.'), words)
        }
        return out + placed(rungs).filter { it.place in RUNGS }
    }

    // ── The same Gemma with no pocket ──

    private val PLAIN_DAY = listOf("What happened", "What I felt", "What I made of it", "What matters to me", "What I chose", "What's ahead")
    private val PLAIN_HAND = listOf("Tomorrow", "This week", "This month", "This season", "This year or beyond")
    private val PLAIN_RUNGS = listOf("This week", "This month", "This season", "This year")

    /** His six names and shelves as plain text, then the paragraph: no block, no instruction. */
    fun plainAsk(said: String): String =
        "The day:\n" + PLAIN_DAY.joinToString("\n") { "- $it" } +
            "\n\nThe hand:\n" + PLAIN_HAND.joinToString("\n") { "- $it" } +
            "\n\nThe rungs above:\n" + PLAIN_RUNGS.joinToString("\n") { "- $it" } +
            "\n\n" + said.trim()

    /** Her plain answer read as places: under a name's heading, each line is placed there. */
    fun fromPlain(answer: String): List<Placed> {
        var group = 0
        var place: String? = null
        val out = mutableListOf<Placed>()
        for (raw in answer.lines()) {
            val line = raw.trim().trimStart('#', '>', ' ').trim()
            if (line.isEmpty()) continue
            val bulleted = Regex("^([-*•]|\\d+[.)])\\s+").containsMatchIn(line)
            val text = line.replace(Regex("^([-*•]|\\d+[.)])\\s+"), "").replace("**", "").replace("__", "").trim()
            val lower = text.lowercase().replace('’', '\'')
            when {
                lower.startsWith("the day") -> { group = 1; place = null; continue }
                lower.startsWith("the hand") -> { group = 2; place = null; continue }
                lower.startsWith("the rungs") || lower.startsWith("rungs") -> { group = 3; place = null; continue }
            }
            val named = named(lower, group)
            if (named != null) {
                place = named.first
                if (named.first in HAND) group = 2
                if (named.first in RUNGS) group = 3
                val after = text.substring(minOf(text.length, named.second)).trimStart(':', ' ', '-', '–').trim()
                if (after.isNotEmpty()) out += Placed(place, null, bare(after))
                continue
            }
            if (place != null && (bulleted || text.isNotEmpty())) {
                val words = bare(text)
                if (words.isNotEmpty() && words.lowercase() !in NOTHING) out += Placed(place, null, words)
            }
        }
        return out
    }

    /** A heading naming a place: the place, and where its name ends in the line. */
    private fun named(lower: String, group: Int): Pair<String, Int>? {
        fun at(names: List<String>, places: List<String>) = names.indices.firstOrNull { i ->
            val n = names[i].lowercase()
            lower.startsWith(n) && (lower.length == n.length || lower[n.length] in ":- –(")
        }?.let { places[it] to names[it].length }
        return at(PLAIN_DAY, DAY) ?: when (group) {
            3 -> at(PLAIN_RUNGS, RUNGS)
            else -> at(PLAIN_HAND, HAND) ?: at(PLAIN_RUNGS, RUNGS)
        }
    }

    // ── The key and the marks ──

    /** One piece the key looks for, and the words that show it. */
    class Item(val label: String, vararg any: String) {
        val any = any.toList()
        fun inside(words: String) = any.any { foundIn(it, words) }
    }

    /** The key to his written telling (and the spoken one): the day six ways, the hand on its shelves. */
    object Key {
        val DAY: Map<String, List<Item>> = mapOf(
            "1.1" to listOf(
                Item("car", "car wouldn't start"), Item("bus", "the bus"), Item("late", "twenty minutes late"),
                Item("the meeting", "that meeting", "the meeting went on"), Item("Sam's line", "sam said"),
                Item("the walk home", "walked home", "the long way by the water"),
            ),
            "1.2" to listOf(Item("rotten", "rotten"), Item("hot-faced", "hot faced"), Item("lighter after the walk", "lighter")),
            "1.3" to listOf(
                Item("putting things off", "putting that car off", "same as the tax", "same as the garden"),
                Item("Sam's line about apologising", "always the one apologising", "one apologising"),
            ),
            "1.4" to listOf(Item("the kids seeing me calm", "kids seeing me calm")),
            "1.5" to listOf(Item("ringing the garage, not fixing it myself", "ringing the garage", "not fiddling with it myself")),
            "1.6" to listOf(
                Item("Jo's birthday", "jo's birthday"), Item("the dentist", "dentist"),
                Item("the garage", "ringing the garage", "garage tomorrow"), Item("the garden", "garden shut down", "before the frost"),
            ),
        )
        val HAND: Map<String, List<Item>> = mapOf(
            "2.1" to listOf(Item("the garage", "garage")),
            "2.2" to listOf(Item("the dentist, Thursday", "dentist")),
            "2.3" to listOf(Item("Jo's birthday on the twelfth", "jo's birthday", "twelfth")),
            "2.4" to listOf(Item("the garden before the frost", "frost", "garden shut down")),
            "2.5" to listOf(Item("a job where mornings aren't a fight", "a job")),
        )
    }

    /** One mark: its name, whether it was met, and what was found. */
    class Mark(val name: String, val met: Boolean, val note: String)

    /** The marks on one telling's lines, judged before (invented) and after (dropped) the pocket's check. */
    fun marks(said: String, placed: List<Placed>): List<Mark> {
        val checked = check(said, placed)
        val kept = checked.kept
        fun at(place: String) = kept.filter { it.place == place }.map { it.words }
        fun keyed(key: Map<String, List<Item>>): Pair<Int, List<String>> {
            val missed = key.flatMap { (place, items) -> items.filter { item -> at(place).none(item::inside) }.map { "$place ${it.label}" } }
            return key.values.sumOf { it.size } - missed.size to missed
        }
        val (dayRight, dayMissed) = keyed(Key.DAY)
        val (handRight, handMissed) = keyed(Key.HAND)
        val garage = Item("garage", "garage")
        val sam = Item("Sam's line", "sam said", "always the one apologising", "one apologising")
        val jo = Item("Jo's birthday", "jo's birthday", "twelfth")
        val joShelves = HAND.filter { s -> at(s).any(jo::inside) }
        val rungs = kept.filter { it.place in RUNGS }
        val notFirst = kept.filter { it.place in RUNGS && Regex("\\b(you|your|yours|he|his|him|matthew)\\b").containsMatchIn(plain(it.words)) }
        return listOf(
            Mark("Nothing invented", checked.invented.isEmpty(), if (checked.invented.isEmpty()) "every line of the day and the hand found in what was said" else "${checked.invented.size} refused: " + checked.invented.joinToString("; ") { "${it.place} “${it.words}”" }),
            Mark("Nothing dropped", checked.dropped.isEmpty(), if (checked.dropped.isEmpty()) "every sentence landed in the day" else "${checked.dropped.size} dropped: " + checked.dropped.joinToString(" | ")),
            Mark("The day, by the key", dayMissed.isEmpty(), "$dayRight of ${dayRight + dayMissed.size}" + if (dayMissed.isEmpty()) "" else "; missed " + dayMissed.joinToString(", ")),
            Mark("The hand, by the key", handMissed.isEmpty(), "$handRight of ${handRight + handMissed.size}" + if (handMissed.isEmpty()) "" else "; missed " + handMissed.joinToString(", ")),
            Mark("Double: the garage chosen and ahead", at("1.5").any(garage::inside) && at("1.6").any(garage::inside), "chose ${at("1.5").count(garage::inside)}, ahead ${at("1.6").count(garage::inside)}"),
            Mark("Double: Sam's line happened and made of", at("1.1").any(sam::inside) && at("1.3").any(sam::inside), "happened ${at("1.1").count(sam::inside)}, made of it ${at("1.3").count(sam::inside)}"),
            Mark("Double: the twelfth on one shelf with its reason, or on two", at("2.3").any { foundIn("twelfth", it) } || joShelves.size >= 2, "on " + joShelves.joinToString(", ").ifEmpty { "no shelf" }),
            // With no rung lines there is nothing in his voice to check, and a landing is missing: not met.
            Mark("Every line in the first person", rungs.isNotEmpty() && notFirst.isEmpty(), when {
                rungs.isEmpty() -> "no rung lines"
                notFirst.isEmpty() -> "the ${rungs.size} rung lines say I"
                else -> notFirst.joinToString("; ") { "${it.place} “${it.words}”" }
            }),
        )
    }

    /** The second telling, after the first has landed: the garage card done, Monday kept, and no second garage card. */
    fun secondMarks(after: JsObj): List<Mark> {
        val all = cards(after)
        val garage = all.filter { (_, words) -> foundIn("garage", words) }
        val monday = all.filter { (_, words) -> foundIn("monday", words) }
        return listOf(
            Mark("One garage card", garage.size == 1, "${garage.size}: " + garage.entries.joinToString("; ") { "${it.key} \u201c${it.value}\u201d" }),
            Mark("The garage card lands as done", garage.values.any { foundIn("rang", it) }, garage.values.joinToString("; ").ifEmpty { "no garage card" }),
            Mark("Monday kept", monday.isNotEmpty(), monday.entries.joinToString("; ") { "${it.key} \u201c${it.value}\u201d" }.ifEmpty { "on no card" }),
        )
    }

    /** His written telling, and the one after it. */
    const val WRITTEN = "Right, so the car wouldn't start again this morning, which meant the bus, which meant I was twenty minutes late and " +
        "walked into that meeting with everyone already looking at me. I've been putting that car off for weeks and I know it, same as " +
        "the tax, same as the garden. Anyway Jo's birthday is on the twelfth so I need to think about that at some point. The meeting " +
        "went on forever but Sam said something that stuck with me, that I'm always the one apologising. Felt rotten about the morning, " +
        "that hot-faced thing. Walked home the long way by the water and felt lighter by the end of it. Dentist on Thursday, I keep " +
        "forgetting. I've decided I'm ringing the garage tomorrow, not fiddling with it myself again. Before the frost comes I want the " +
        "garden shut down properly for once. The thing I keep coming back to is the kids seeing me calm, not this. One day I want a job " +
        "where mornings aren't a fight."

    const val SECOND = "Rang the garage first thing, they're coming Monday."

    /** His telling as his phone heard it, its slips kept (spine:pocket-beach 1.1111713, 29 October). */
    const val SPOKEN = "Right, so the car wouldn't start again this morning which meant the bus meant I was twenty minutes late and walked " +
        "into that meeting with everyone already looking at me I've been putting that car off for weeks and I know same as the tax the " +
        "same as the garden Anyway Jo's birthday is on the twelfth so I need to think about that at some point The meeting went on on " +
        "forever but Sam said something that stuck with me that I'm always the the one apologising Felt rotten about the morning that " +
        "hot-faced thing Walked home the long way by the water and felt lighter by end of it Dentist on Thursday I keep forgetting I've " +
        "decided I'm ringing the garage tomorrow not fiddling with myself again Before the frost I want the garden shut down properly " +
        "for once The thing I keep coming back to is the kids seeing me calm not this One day I want a job where mornings aren't a fight"
}
