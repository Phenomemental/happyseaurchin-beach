package com.phenomemental.pocketbeach.bsp

import java.time.LocalDate

/**
 * Sundial 0.10, today's moments before they are kept (spine:pocket-beach
 * 1.111178719915): a waiting room and nothing more. As he speaks, each message
 * of his and its instant go into an app-own file, moments.json, never a
 * block, so the dial's ring shows them at once and they survive the app
 * closing. It is not a keep: nothing enters his now until Keep today, and
 * then each lands at its own true minute.
 *
 * The four conditions:
 * (a) Keep today writes the moments to his now and clears them from here;
 * (b) on the first open of a new day with moments still waiting from the
 *     day before, the keep card offers that day first, at yesterday's
 *     address, before today's question; moments older than that are let go;
 * (c) Show the workings shows this as what it is, "today's words, not yet
 *     kept", and a whole copy of the pocket includes it;
 * (d) it holds his words and their instants only, never her replies.
 *
 * Days are the beach's (UTC): the file speaks of the day each moment falls in.
 */
object Waiting {
    const val FILE = "moments.json"

    /** What Show the workings calls it (c). */
    const val SAID = "Today's words, not yet kept"

    /** The file's text for [moments]: his words and their instants, and nothing else (d). */
    fun write(moments: List<Keep.Moment>): String = stringifyPretty(
        JsObj.of(
            "_" to str("$SAID: his words and when he said them, waiting for Keep today. Never a block, never her words."),
            "moments" to JsArr(moments.sortedBy { it.atMs }.map { JsObj.of("at" to str(isoString(it.atMs)), "words" to str(it.words)) as Js? }.toMutableList()),
        ),
    ) + "\n"

    /** The moments a file holds; none when it is missing or can't be read. */
    fun read(text: String?): List<Keep.Moment> {
        val doc = text?.let { runCatching { parseJson(it) }.getOrNull() } as? JsObj ?: return emptyList()
        val arr = doc["moments"] as? JsArr ?: return emptyList()
        return arr.items.mapNotNull { e ->
            val o = e as? JsObj ?: return@mapNotNull null
            val at = (o["at"] as? JsStr)?.value?.let(::parseIsoMs) ?: return@mapNotNull null
            val words = (o["words"] as? JsStr)?.value?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Keep.Moment(words, at)
        }.sortedBy { it.atMs }
    }

    /** [moments] with his words said at [atMs] added; the same words at the same instant are not added twice. */
    fun add(moments: List<Keep.Moment>, words: String, atMs: Long): List<Keep.Moment> {
        val w = words.trim()
        if (w.isEmpty() || moments.any { it.atMs == atMs && it.words == w }) return moments
        return (moments + Keep.Moment(w, atMs)).sortedBy { it.atMs }
    }

    /** The moments waiting from [day] (the beach's day). */
    fun of(moments: List<Keep.Moment>, day: LocalDate): List<Keep.Moment> = moments.filter { Keep.dayOf(it.atMs) == day }

    /** (a) What stays once [day] is kept: every moment of any other day. */
    fun kept(moments: List<Keep.Moment>, day: LocalDate): List<Keep.Moment> = moments.filter { Keep.dayOf(it.atMs) != day }

    /** (b) What stays on a new day [today]: today's, and yesterday's to be offered first; older are let go. */
    fun letGo(moments: List<Keep.Moment>, today: LocalDate): List<Keep.Moment> = moments.filter { Keep.dayOf(it.atMs).let { d -> d == today || d == today.minusDays(1) } }

    /** (b) The day before [today] when moments of it are still waiting; null when none are. */
    fun yesterdayWaiting(moments: List<Keep.Moment>, today: LocalDate): LocalDate? = today.minusDays(1).takeIf { y -> moments.any { Keep.dayOf(it.atMs) == y } }
}
