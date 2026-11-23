package com.phenomemental.pocketbeach.bsp

/**
 * Sundial 0.13.5, talk about this: a block's shape, worked out by the app
 * and handed her as readable text beside what he is looking at, so she can
 * speak to its patterns: how many parts at each depth, how deep each part
 * goes, which places are full at nine, which are empty, and where it has
 * grown most. Plain counting; nothing about what the words say.
 */
object BlockShape {
    /** The most places named in one list; the rest counted. */
    private const val MOST = 20

    private fun words(n: Js?): String? = when (n) {
        is JsStr -> n.value
        is JsObj -> (n["_"] as? JsStr)?.value
        else -> null
    }

    private fun address(digits: List<Char>): String = when (digits.size) {
        0 -> ""
        1 -> digits[0].toString()
        else -> digits[0] + "." + digits.drop(1).joinToString("")
    }

    private fun children(n: Js?): List<Pair<Char, Js>> = (n as? JsObj)?.let { o -> ('1'..'9').mapNotNull { d -> o["$d"]?.let { d to it } } }.orEmpty()

    /** How many places stand beneath [n], and how many levels deep they go. */
    private fun under(n: Js?): Pair<Int, Int> {
        var count = 0
        var deepest = 0
        fun walk(x: Js?, depth: Int) {
            for ((_, c) in children(x)) { count++; deepest = maxOf(deepest, depth); walk(c, depth + 1) }
        }
        walk(n, 1)
        return count to deepest
    }

    private fun listed(items: List<String>): String =
        items.take(MOST).joinToString("; ") + if (items.size > MOST) "; and ${items.size - MOST} more" else ""

    /** [block]'s shape as readable text, [name] its name. */
    fun of(name: String, block: Js): String {
        val perDepth = sortedMapOf<Int, Int>()
        val full = mutableListOf<String>()
        val empty = mutableListOf<String>()
        fun walk(n: Js, digits: List<Char>) {
            if (digits.isNotEmpty()) perDepth[digits.size] = (perDepth[digits.size] ?: 0) + 1
            val kids = children(n)
            if (kids.size == 9) full += if (digits.isEmpty()) "the parts beneath the opening line" else "beneath ${address(digits)}"
            if (words(n).isNullOrBlank()) empty += if (digits.isEmpty()) "the opening line" else address(digits)
            for ((d, c) in kids) walk(c, digits + d)
        }
        walk(block, emptyList())
        val (all, deepest) = under(block)
        val tops = children(block).map { (d, c) -> d to under(c) }
        return buildString {
            append("(The shape of his block $name, worked out by the app:)\n")
            if (all == 0) { append("It is its opening line alone, with no parts beneath it."); return@buildString }
            append("It goes ${count(deepest, "level")} deep beneath its opening line, with ${count(all, "part")} in all.\n")
            append("At each depth: " + perDepth.entries.joinToString(", ") { (d, n) -> "${count(n, "part")} at the ${ordinal(d)} level" } + ".\n")
            append("Each part beneath the opening line, and how deep it goes:")
            for ((d, u) in tops) {
                val (n, deep) = u
                append("\n  $d  " + if (n == 0) "nothing beneath it" else "goes ${count(deep, "level")} deeper, ${count(n, "part")} beneath it")
            }
            append("\n")
            append("Full at nine, with no room for a tenth beside them: " + (if (full.isEmpty()) "none" else listed(full)) + ".\n")
            append("Empty, with nothing written there: " + (if (empty.isEmpty()) "none" else listed(empty)) + ".\n")
            val most = tops.filter { it.second.first > 0 }.maxByOrNull { it.second.first }
            append(if (most == null) "None of its parts has grown beneath its opening line." else "It has grown most at ${most.first}: ${count(most.second.first, "part")} beneath it, ${count(most.second.second, "level")} deep.")
        }
    }

    private fun count(n: Int, one: String): String = "$n " + if (n == 1) one else one + "s"

    private fun ordinal(n: Int): String = when (n) {
        1 -> "first"; 2 -> "second"; 3 -> "third"; 4 -> "fourth"; 5 -> "fifth"; 6 -> "sixth"; 7 -> "seventh"; 8 -> "eighth"; 9 -> "ninth"
        else -> "${n}th"
    }
}
