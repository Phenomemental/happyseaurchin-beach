package com.phenomemental.pocketbeach

import com.phenomemental.pocketbeach.bsp.Js
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.JsStr
import com.phenomemental.pocketbeach.bsp.Landing
import com.phenomemental.pocketbeach.bsp.Telling
import com.phenomemental.pocketbeach.bsp.momentToAddress
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.bsp.stringify

/**
 * His now as 0.6 to 0.9 kept a day (the day's line, and the six in its hidden
 * directory), for device tests of what still opens or moves it. The core no
 * longer writes it.
 */
internal object Before012 {
    private val SIX = mapOf(
        "1.1" to "What happened", "1.2" to "What I felt", "1.3" to "What I made of it", "1.4" to "What matters to me",
        "1.5" to "What I chose", "1.6" to "What's ahead",
    )

    /** The day's clock address, full width, for [nowMs]: its eight digits and two zeros. */
    fun dayAddress(nowMs: Long): String = momentToAddress(nowMs).take(8) + "00"

    /** [now] (or a new clock) with the day at [address]: the telling as the day's line, and the six in the day's hidden directory. */
    fun withDay(now: Js?, address: String, telling: String, six: Map<String, List<String>>): Pair<JsObj, List<String>> {
        val root = (now as? JsObj)?.let { parseJson(stringify(it)!!) as JsObj } ?: Landing.emptyClock()
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
        dir["_"] = str(
            when {
                had == null -> telling.trim()
                had.split("\n\n").any { Telling.plain(it) == Telling.plain(telling) } -> had
                else -> had + "\n\n" + telling.trim()
            },
        )
        val over = mutableListOf<String>()
        for ((i, place) in Telling.DAY.withIndex()) {
            val words = six[place].orEmpty()
            if (words.isEmpty()) continue
            val at = when (val p = dir["${i + 1}"]) {
                is JsObj -> p
                is JsStr -> JsObj.of("_" to p)
                else -> JsObj.of("_" to str(SIX[place] ?: place))
            }
            for (w in words) {
                if ((1..9).any { k -> (at["$k"] as? JsStr)?.value?.let { Telling.plain(it) == Telling.plain(w) } == true || ((at["$k"] as? JsObj)?.get("_") as? JsStr)?.value?.let { Telling.plain(it) == Telling.plain(w) } == true }) continue
                val free = (1..9).firstOrNull { !at.has("$it") }
                if (free == null) over += w else at["$free"] = str(w)
            }
            dir["${i + 1}"] = at
        }
        node["_"] = dir
        return root to over
    }
}
