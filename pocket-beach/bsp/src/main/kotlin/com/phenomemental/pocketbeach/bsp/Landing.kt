package com.phenomemental.pocketbeach.bsp

/**
 * A told day (0.6, spine:pocket-beach 1.111174): the blocks the app kept it
 * in, the names the pocket knows (so his speech is never cut before one), a
 * clock with nothing on it yet, and the day check his cook's DAY asks of his
 * words ([tellsDay]). Keep today writes his day itself (Days, CookActs.keepDay).
 */
object Landing {
    /** His structure block: the six places named, the hand's shelves and the rungs ([Telling.SHAPE]). */
    const val TELLING = "telling"

    /** His clock in the pocket: a block of the clock's form, floor 10, as his now is on the beach. */
    const val NOW = "now"

    /** The places he moved pieces to himself, the newest nine, shown to her when she sorts. */
    const val EXAMPLES = "sort-examples"

    const val NOW_OPENING = "My now, on the clock: each day as I told it, with the six kept with it."

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

    /** A clock of the floor-10 form, with nothing on it yet. */
    fun emptyClock(opening: String = NOW_OPENING): JsObj {
        var node: Js = str(opening)
        repeat(10) { node = JsObj.of("_" to node) }
        return node as JsObj
    }

    // ── when his words are a telling ──

    private val ASKS = setOf("add", "note", "change", "make", "post", "put", "write", "keep", "remember", "save", "remove", "delete", "rename", "read", "show", "tell", "do")

    /**
     * Sundial 0.10.3: the day check as cook 1.1.8.3's sentence states it.
     * [sentences] and [words]: a telling that long is his day; [cardLetters]:
     * a word of that many letters or more that a card on his hand also holds
     * is his day; [neverQuestion], [neverOpens]: never when it ends with a
     * question mark, or opens with one of those words. [legacyCard]: the
     * sentences before 0.10.3 ("or news of a card on his hand"), checked as
     * they always were: a piece sharing any but the small words with a card.
     */
    data class DayCheck(
        val sentences: Int, val words: Int, val cardLetters: Int? = null,
        val neverQuestion: Boolean = false, val neverOpens: List<String>? = null, val legacyCard: Boolean = false,
    ) {
        companion object {
            /** As the sentence of 0.9.1 to 0.10.2 was checked: three sentences and forty words, or news of a card; never a question or an ask. */
            val SINCE_09 = DayCheck(3, 40, neverQuestion = true, neverOpens = ASKS.toList(), legacyCard = true)
        }
    }

    /** Whether [said] tells of his day, by [check]: exactly what its sentence says. */
    fun tellsDay(said: String, cards: Map<String, String>, names: Set<String>, check: DayCheck): Boolean {
        val trimmed = said.trim()
        if (trimmed.isEmpty()) return false
        if (check.neverQuestion && trimmed.endsWith("?")) return false
        val words = Telling.plain(trimmed).split(' ').filter { it.isNotEmpty() }
        if (check.neverOpens != null && words.firstOrNull() in check.neverOpens.map { Telling.plain(it) }) return false
        if (Telling.sentences(trimmed, names).size >= check.sentences && words.size >= check.words) return true
        if (check.legacyCard) return Telling.cut(trimmed, names).any { Telling.cardsNear(it, cards).isNotEmpty() }
        val n = check.cardLetters ?: return false
        val mine = words.filter { it.length >= n }.toSet()
        return cards.values.any { card -> Telling.plain(card).split(' ').any { it.length >= n && it in mine } }
    }
}
