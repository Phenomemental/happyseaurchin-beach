package com.phenomemental.pocketbeach.bsp

/**
 * Sundial 0.10.3 (spine:pocket-beach 1.111178719927): her places for each of
 * his moments not yet placed, asked once, in free text the app reads, never
 * through a tool call. On his Pixel on 4 November the places call (10.4 s)
 * and the sheet call (15.4 s) both came back as whole tool calls, and the
 * card still showed no place: her places for the day as a whole touched all
 * six, which counts as none. Now each moment keeps its own: from the places
 * line of her reply to it, and, for a moment without one, from this ask at
 * Keep today.
 */
object Placing {
    /** Her line for the ask: each moment numbered, one line back for each, its number, a colon, the places by name. */
    fun instruction(question: String, ring: List<String>): String =
        "The app: name, for each of his moments below, every place of this ring its words plainly touch, never ranked; " +
            "${question.trim().trimEnd('.')}. The ring: ${ring.joinToString(", ")}. " +
            "Write one line for each moment: its number, a colon, then its places by name, as “1: ${ring.first()}”. " +
            "Where a moment touches no place, write its number and a colon only. Write nothing else."

    /** His moments as she is handed them, numbered from 1, each on one line, his words alone. */
    fun material(moments: List<Keep.Moment>): String =
        moments.mapIndexed { i, m -> "${i + 1}. ${m.words.replace(Regex("""\s*\n\s*"""), " / ").trim()}" }.joinToString("\n")

    /**
     * Her answer read: for each line "N: places", the places she named for
     * the Nth moment (by name, in P6's order), keyed by that moment's
     * instant. A moment she left out, or named none for, is left unplaced.
     */
    fun read(reply: String, moments: List<Keep.Moment>): Map<Long, List<Int>> {
        val out = LinkedHashMap<Long, List<Int>>()
        for (line in reply.lines()) {
            val m = Regex("""^\W*(\d+)\s*[:.)\-–—]\s*(.*)$""").find(line.trim()) ?: continue
            val i = m.groupValues[1].toInt() - 1
            val moment = moments.getOrNull(i) ?: continue
            val named = HerReply.placesOf(m.groupValues[2])
            if (named.isNotEmpty()) out[moment.atMs] = Mark.inOrder((out[moment.atMs].orEmpty() + named).distinct())
        }
        return out
    }
}
