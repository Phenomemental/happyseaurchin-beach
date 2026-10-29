package com.phenomemental.pocketbeach.bsp

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

/**
 * When a forward piece falls, reckoned by code (0.5's Stage 4, Matthew, 29
 * October): from the time words a piece carries, read from a block he can
 * change, and from the days, dates and months it names, counted from today.
 * A piece with a when goes on its shelf of the hand, and is also what's ahead.
 */
object Whens {
    /** The block the time words are kept in, in the pocket. */
    const val BLOCK = "time-words"

    /** The time words as they are first kept: each shelf, its words. He changes them as he likes. */
    val FIRST = """
{"_": "Time words: the words that say when something falls, one shelf of the hand each. Days of the week, dates (the twelfth, the 12th) and months are counted from today, and need no line here.",
 "1": {"_": "Tomorrow", "1": "tomorrow", "2": "tomorrow morning", "3": "first thing tomorrow"},
 "2": {"_": "This week", "1": "this week", "2": "tonight", "3": "this weekend", "4": "at the weekend", "5": "in a few days", "6": "later this week"},
 "3": {"_": "This month", "1": "this month", "2": "next week", "3": "in a fortnight", "4": "in two weeks", "5": "in a couple of weeks", "6": "in a few weeks", "7": "later this month"},
 "4": {"_": "This season", "1": "before the frost", "2": "this winter", "3": "this spring", "4": "this summer", "5": "this autumn", "6": "next month", "7": "before christmas", "8": "before the winter"},
 "5": {"_": "This year or beyond", "1": "one day", "2": "someday", "3": "some day", "4": "this year", "5": "next year", "6": "eventually", "7": "in the end", "8": "one of these days", "9": "in a few years"}}
""".trim()

    fun first(): JsObj = parseJson(FIRST) as JsObj

    /** The shelves, by address: 2.1 tomorrow to 2.5 this year or beyond, as in [Telling.HAND]. */
    private val SHELVES = Telling.HAND

    /** The words of each shelf in [block], as he keeps them. */
    fun words(block: JsObj): Map<String, List<String>> = (1..5).associate { i ->
        val node = block["$i"] as? JsObj
        SHELVES[i - 1] to (node?.keys()?.filter { it != "_" }?.mapNotNull { (node[it] as? JsStr)?.value?.trim()?.takeIf(String::isNotEmpty) } ?: emptyList())
    }

    private val ORDINALS = listOf(
        "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth", "eleventh", "twelfth", "thirteenth",
        "fourteenth", "fifteenth", "sixteenth", "seventeenth", "eighteenth", "nineteenth", "twentieth", "twenty first", "twenty second",
        "twenty third", "twenty fourth", "twenty fifth", "twenty sixth", "twenty seventh", "twenty eighth", "twenty ninth", "thirtieth", "thirty first",
    )

    /** A shelf for a span of days ahead: tomorrow, within a week, a month, a season, or beyond. */
    fun shelf(days: Long): String = when {
        days <= 1 -> SHELVES[0]
        days <= 7 -> SHELVES[1]
        days <= 31 -> SHELVES[2]
        days <= 92 -> SHELVES[3]
        else -> SHELVES[4]
    }

    /** Why a piece falls when it does: the shelf, and the words that said so. */
    data class When(val shelf: String, val by: String)

    /**
     * When [piece] falls, from today: by a time word of his first (the
     * longest that fits), then a day of the week (its next coming), a date
     * of the month ("the twelfth": its next coming), or a month by name. Null
     * if it carries none.
     */
    fun of(piece: String, today: LocalDate, block: JsObj = first()): When? {
        val said = " ${Telling.plain(piece)} "
        words(block).flatMap { (shelf, words) -> words.map { shelf to it } }
            .filter { (_, w) -> said.contains(" ${Telling.plain(w)} ") }
            .maxByOrNull { (_, w) -> w.length }
            ?.let { (shelf, w) -> return When(shelf, w) }
        for (day in DayOfWeek.values()) {
            val name = day.name.lowercase()
            if (said.contains(" $name ") || said.contains(" ${name}s ")) {
                val ahead = ((day.value - today.dayOfWeek.value + 7) % 7).let { if (it == 0) 7 else it }
                return When(shelf(ahead.toLong()), name.replaceFirstChar(Char::uppercase))
            }
        }
        // A date of the month, in words or figures: "the twelfth", "the 12th", "on the 12".
        // "The first" names a date only as "on the first": "the first thing", "the first time" do not.
        val ordinal = ORDINALS.indices.reversed().firstOrNull { i -> said.contains(" on the ${ORDINALS[i]} ") || i >= 3 && said.contains(" the ${ORDINALS[i]} ") }?.let { it + 1 }
            ?: Regex(" (?:the |on )(\\d{1,2})(?:st|nd|rd|th)? ").find(said)?.groupValues?.get(1)?.toInt()?.takeIf { it in 1..31 }
        if (ordinal != null) {
            var date = today.withDayOfMonth(minOf(ordinal, today.lengthOfMonth()))
            if (!date.isAfter(today)) date = today.plusMonths(1).let { it.withDayOfMonth(minOf(ordinal, it.lengthOfMonth())) }
            return When(shelf(ChronoUnit.DAYS.between(today, date)), "the ${ORDINALS[ordinal - 1]}")
        }
        for (month in Month.values()) {
            val name = month.name.lowercase()
            if (said.contains(" $name ") && month != today.month) {
                var date = LocalDate.of(today.year, month, 1)
                if (date.isBefore(today)) date = date.plusYears(1)
                return When(shelf(ChronoUnit.DAYS.between(today, date)), name.replaceFirstChar(Char::uppercase))
            }
        }
        return null
    }
}
