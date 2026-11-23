package com.phenomemental.pocketbeach.bsp

/**
 * Sundial 0.13.1, plain machinery (spine:pocket-beach 1.1111787199993): a
 * reply that repeats the same word or phrase many times running ("the the
 * the …", as on his phone on 23 November) is a loop, and the app stops it.
 * Nothing about how she answers: only whether her words, as they come,
 * have fallen into one word or phrase over and over at their end.
 */
object Looping {
    /** The longest phrase, in words, looked for. */
    const val LONGEST = 8

    /** How many times running makes a loop: one word 8 times, a phrase of two words 6 times, a longer phrase 4 times. */
    fun enough(words: Int): Int = when (words) {
        1 -> 8
        2 -> 6
        else -> 4
    }

    /** Only the end of her words is read: a loop is where she is now. */
    private const val TAIL = 1500

    /** A loop found: the word or phrase, how many times running, and where the first time ends (in the text read). */
    class Loop(val unit: String, val times: Int, val firstEnds: Int)

    private val WORD = Regex("""[\p{L}\p{N}][\p{L}\p{N}'’\-]*""")

    /**
     * The loop [text] ends in, or null. Words are compared whatever their
     * capitals and the punctuation between them; marks alone (a table's
     * rules, a list's dashes) are not words. A last word that may still be
     * coming is read both with and without.
     */
    fun of(text: String): Loop? {
        val from = maxOf(0, text.length - TAIL)
        val words = WORD.findAll(text, from).map { it.value.lowercase() to it.range.last + 1 }.toList()
        val ways = if (words.isNotEmpty() && words.last().second == text.length) listOf(words, words.dropLast(1)) else listOf(words)
        for (w in ways) for (n in 1..LONGEST) {
            val need = enough(n)
            if (w.size < n * need) continue
            val unit = w.subList(w.size - n, w.size).map { it.first }
            var times = 1
            while (w.size >= (times + 1) * n && w.subList(w.size - (times + 1) * n, w.size - times * n).map { it.first } == unit) times++
            if (times >= need) return Loop(unit.joinToString(" "), times, w[w.size - times * n + n - 1].second)
        }
        return null
    }

    /** [text] cut where the loop began: its first time kept, with any mark right after it, then "…". */
    fun cut(text: String, loop: Loop): String {
        var end = loop.firstEnds
        while (end < text.length && !text[end].isWhitespace() && !text[end].isLetterOrDigit()) end++
        return text.substring(0, end).trimEnd() + " …"
    }

    /** Said beneath a reply the app stopped. */
    const val STOPPED = "Sundial stopped her reply: it was repeating the same words over and over."
}
