package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Sundial's mark (Mark.kt) against mark.js. Every expected value here came
 * from mark.js itself, run in node 22.23.4 on 3 November 2026 (mark.js as it
 * stood at commit 4d8f5a8), its private helpers reached by loading the file
 * with them added to its exports; the sums are mark.js's own Mark.test ones,
 * worked by hand. The fixtures are read from the repository's fixtures/.
 */
class MarkTest {
    private fun fixture(name: String): List<Pair<String, List<Int>>> {
        // The core's tests run in pocket-beach/bsp, so the repository's fixtures/ is two up.
        val file = File("../../fixtures/$name")
        assertTrue(file.isFile, "no fixture at ${file.absolutePath}")
        return (parseJson(file.readText(Charsets.UTF_8)) as JsArr).items.map { d ->
            val day = d as JsObj
            (day["text"] as JsStr).value to ((day["places"] as? JsArr)?.items?.map { (it as JsNum).value.toInt() } ?: emptyList())
        }
    }

    private val trial by lazy { fixture("days-trial.json") }
    private val mixed by lazy { fixture("days-mixed.json") }

    private fun near(want: Double, got: Double, what: String, eps: Double = 1e-9) =
        assertTrue(abs(want - got) <= eps, "$what: expected $want, got $got")

    private fun near(want: List<Double>, got: List<Double>, what: String, eps: Double = 1e-12) {
        assertEquals(want.size, got.size, "$what: how many")
        want.indices.forEach { near(want[it], got[it], "$what [$it]", eps) }
    }

    private fun near(want: DialMath.Pt, got: DialMath.Pt, what: String) {
        near(want.x, got.x, "$what x")
        near(want.y, got.y, "$what y")
    }

    // ---- The six places ---------------------------------------------------------

    @Test
    fun theSixPlacesAreMarkJsOwn() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6), Mark.POOLS.map { it.n })
        assertEquals(listOf("Facts", "Feelings", "Fictions", "Purpose", "Decision", "Outcomes"), Mark.POOLS.map { it.name })
        assertEquals(listOf(0xFF4A6B8A, 0xFFB0413E, 0xFF7A5C99, 0xFF2E7D6B, 0xFFB8860B, 0xFF6FA8DC), Mark.POOLS.map { it.hue })
        assertEquals(listOf("what happened", "what you felt", "what you made of it", "what matters", "a choice", "what’s ahead"), Mark.POOLS.map { it.phrase })
        assertEquals(listOf("1.1", "1.2", "1.3", "2.1", "2.2", "2.3"), Mark.POOLS.map { it.at })
        assertEquals(
            listOf(
                "What could you have pointed to today?", "What did your body know today?", "What did you make of it, that might not be so?",
                "What mattered, underneath?", "What did you choose, or let be chosen?", "What are you expecting, or fearing?",
            ),
            Mark.POOLS.map { it.question },
        )
        assertEquals("Facts,Outcomes,Fictions,Decision,Purpose,Feelings", Mark.P6.joinToString(",") { Mark.POOLS[it - 1].name })
        assertEquals("In your own words.", Mark.OWN)
    }

    @Test
    fun aPhraseMatchesWithEitherApostropheAnyCaseAndLooseSpaces() {
        val cases = listOf(
            Triple("What’s  Ahead ", "what's ahead", 6),
            Triple("what's ahead", "what's ahead", 6),
            Triple("WHAT‘S AHEAD", "what's ahead", 6),
            Triple("what`s ahead", "what's ahead", 6),
            Triple(" a\tchoice\n", "a choice", 5),
            Triple("a choic", "a choic", 0),
            Triple("", "", 0),
        )
        for ((s, plain, pool) in cases) {
            assertEquals(plain, Mark.plainPhrase(s), "plainPhrase($s)")
            assertEquals(pool, Mark.poolByPhrase(s), "poolByPhrase($s)")
        }
    }

    @Test
    fun placesAreSaidInP6sOrderEachOnce() {
        val cases = listOf(
            Triple(emptyList<Int>(), "", ""),
            Triple(listOf(1), "what happened", "what happened"),
            Triple(listOf(2, 1), "what happened · what you felt", "what happened, and what you felt"),
            Triple(listOf(2, 6, 1), "what happened · what’s ahead · what you felt", "what happened, what’s ahead, and what you felt"),
            Triple(listOf(4, 1, 4, 9, 3), "what happened · what you made of it · what matters", "what happened, what you made of it, and what matters"),
            Triple(listOf(6, 5), "what’s ahead · a choice", "what’s ahead, and a choice"),
            Triple(
                listOf(1, 2, 3, 4, 5, 6),
                "what happened · what’s ahead · what you made of it · a choice · what matters · what you felt",
                "what happened, what’s ahead, what you made of it, a choice, what matters, and what you felt",
            ),
            Triple(listOf(2, 2, 0, 7), "what you felt", "what you felt"),
        )
        for ((places, words, said) in cases) {
            assertEquals(words, Mark.placesWords(places), "placesWords($places)")
            assertEquals(said, Mark.placesSaid(places), "placesSaid($places)")
        }
        assertEquals(listOf(1, 6, 3, 5, 4, 2), Mark.inOrder(setOf(2, 4, 5, 3, 6, 1)))
    }

    @Test
    fun aLastLineNamesPlacesOnlyWhenEveryPieceIsOneAndTheOlderLineStillReads() {
        val cases = listOf(
            "A day of what happened." to listOf(1),
            "a day of what you felt" to listOf(2),
            "A DAY OF A CHOICE." to listOf(5),
            "A day of what's ahead." to listOf(6),
            "A day of what’s ahead.." to emptyList<Int>(),
            "A day of rain, mostly." to emptyList<Int>(),
            "A day of what happened · what you felt." to emptyList<Int>(),
            "A day of ." to emptyList<Int>(),
            "A day of" to emptyList<Int>(),
            "Aday of what happened." to emptyList<Int>(),
            "What happened · what's ahead · what you felt." to listOf(1, 6, 2),
            "What you felt · what happened" to listOf(1, 2),
            "what happened.  " to listOf(1),
            "What happened · the sea." to emptyList<Int>(),
            "Tea · toast · the sea." to emptyList<Int>(),
            "What happened ·  · what you felt." to emptyList<Int>(),
            "" to emptyList<Int>(),
            "What happened. ." to emptyList<Int>(),
            "what matters · WHAT MATTERS." to listOf(4),
        )
        for ((row, places) in cases) assertEquals(places, Mark.placesIn(row), "placesIn($row)")
    }

    // ---- A day's text -----------------------------------------------------------

    @Test
    fun aSheetWritesAsMarkJsWritesIt() {
        val cases = listOf(
            Triple(
                Mark.Sheet("Lizards and the first quiet", listOf("A slow morning.", "Your youngest laughing."), "Tomorrow: walk up early.", listOf(2, 6, 1)), "Arona",
                "Arona — Lizards and the first quiet\nA slow morning.\nYour youngest laughing.\nTomorrow: walk up early.\nWhat happened · what’s ahead · what you felt.",
            ),
            Triple(Mark.Sheet("T", listOf(), places = listOf(4, 1, 4, 9, 3)), "", "T\nWhat happened · what you made of it · what matters."),
            Triple(Mark.Sheet("T", listOf("x"), places = listOf(6)), "", "T\nx\nWhat’s ahead."),
            Triple(
                Mark.Sheet("  Rain all day  ", listOf("  ", " Wet. ", ""), "  tomorrow:   dry, maybe ", own = true), "Home",
                "Home — Rain all day\nWet.\nTomorrow: dry, maybe\nIn your own words.",
            ),
            Triple(Mark.Sheet("Own", listOf("Mine."), "TOMORROW:", own = true), "", "Own\nMine.\nIn your own words."),
            Triple(Mark.Sheet("Both", listOf("x"), "Tomorrowland", listOf(5), own = true), "", "Both\nx\nTomorrow: Tomorrowland\nA choice."),
            Triple(Mark.Sheet("Neither", listOf("x")), "Here", "Here — Neither\nx"),
        )
        for ((sheet, place, text) in cases) assertEquals(text, Mark.write(sheet, place), "write($sheet, $place)")
    }

    @Test
    fun aDayReadsBackAsMarkJsReadsIt() {
        val cases = listOf(
            "" to Mark.Day("", "", emptyList(), "", emptyList(), false),
            "\n \n" to Mark.Day("", "", emptyList(), "", emptyList(), false),
            "Into bed early, quietly relieved." to Mark.Day("", "Into bed early, quietly relieved.", emptyList(), "", emptyList(), false),
            // The own line alone is a title, not a mark.
            "In your own words." to Mark.Day("", "In your own words.", emptyList(), "", emptyList(), false),
            "Home — Rain\nWet.\nIn your own words." to Mark.Day("Home", "Rain", listOf("Wet."), "", emptyList(), true),
            "Here — A day\nSome words.\nA day of what's ahead." to Mark.Day("Here", "A day", listOf("Some words."), "", listOf(6), false),
            "Home — Rain\nA day of rain, mostly." to Mark.Day("Home", "Rain", listOf("A day of rain, mostly."), "", emptyList(), false),
            "Tea\nWhat happened · the sea." to Mark.Day("", "Tea", listOf("What happened · the sea."), "", emptyList(), false),
            " — Dash first\nx" to Mark.Day("", "— Dash first", listOf("x"), "", emptyList(), false),
            "Place —Title" to Mark.Day("", "Place —Title", emptyList(), "", emptyList(), false),
            "A — B — C\ntomorrow:   go early\nTOMORROW: and late\nWhat happened." to Mark.Day("A", "B — C", listOf("TOMORROW: and late"), "go early", listOf(1), false),
            "T\nTomorrow:\nTomorrow: the second\nx" to Mark.Day("", "T", listOf("x"), "the second", emptyList(), false),
            "T\nTomorrow: a b\nx" to Mark.Day("", "T", listOf("Tomorrow: a b", "x"), "", emptyList(), false),
            "T\nTomorrowland\nA day of a choice" to Mark.Day("", "T", listOf("Tomorrowland"), "", listOf(5), false),
            "Mine\nx\nA choice.\nIn your own words." to Mark.Day("", "Mine", listOf("x", "A choice."), "", emptyList(), true),
            "Mine\nx\nIn your own words.\nA choice." to Mark.Day("", "Mine", listOf("x", "In your own words."), "", listOf(5), false),
        )
        for ((text, day) in cases) assertEquals(day, Mark.read(text), "read($text)")
    }

    private fun sheetOf(d: Mark.Day) = Mark.Sheet(d.title, d.lines, d.tomorrow, d.places, d.own)

    private fun roundTrips(fx: List<Pair<String, List<Int>>>, want: List<Pair<Mark.Day, String>>, what: String) {
        assertEquals(want.size, fx.size, "$what: days")
        fx.forEachIndexed { i, (text, _) ->
            val day = Mark.read(text)
            assertEquals(want[i].first, day, "$what day ${i + 1} read")
            val again = Mark.write(sheetOf(day), day.place)
            assertEquals(want[i].second, again, "$what day ${i + 1} written again")
            assertEquals(day, Mark.read(again), "$what day ${i + 1} reads back the same")
        }
    }

    @Test
    fun theTrialFixtureReadsAndWritesAsMarkJsDoes() = roundTrips(
        trial,
        listOf(
            Mark.Day("Arona", "Landed late, the sea in the dark", listOf("The flight in at four, the hire car, the wrong turning twice. The apartment smelt of salt and someone else's sun cream. You heard the sea before you saw it."), "find the bakery and the beach, nothing more.", listOf(1), false) to
                "Arona — Landed late, the sea in the dark\nThe flight in at four, the hire car, the wrong turning twice. The apartment smelt of salt and someone else's sun cream. You heard the sea before you saw it.\nTomorrow: find the bakery and the beach, nothing more.\nWhat happened.",
            Mark.Day("Arona", "Black sand and a bakery", listOf("The bakery by the roundabout, the black sand too hot by eleven. Both kids in the water until their lips went blue. You read four pages and fell asleep under the umbrella."), "", listOf(1), false) to
                "Arona — Black sand and a bakery\nThe bakery by the roundabout, the black sand too hot by eleven. Both kids in the water until their lips went blue. You read four pages and fell asleep under the umbrella.\nWhat happened.",
            Mark.Day("Arona", "The couple who looked so sure", listOf("The couple at the next table, matching and quiet, and the story you told yourself about them: that they had it all worked out. Then the thought that someone might tell the same story about you."), "the drive north.", listOf(3), false) to
                "Arona — The couple who looked so sure\nThe couple at the next table, matching and quiet, and the story you told yourself about them: that they had it all worked out. Then the thought that someone might tell the same story about you.\nTomorrow: the drive north.\nWhat you made of it.",
            Mark.Day("Puerto de la Cruz", "Tight in the chest on the mountain road", listOf("The bends and the drop, your hands tight on the wheel and something tight in your chest that stayed after you parked. Then the warm old square, and your shoulders letting go."), "", listOf(2), false) to
                "Puerto de la Cruz — Tight in the chest on the mountain road\nThe bends and the drop, your hands tight on the wheel and something tight in your chest that stayed after you parked. Then the warm old square, and your shoulders letting go.\nWhat you felt.",
            Mark.Day("Puerto de la Cruz", "The pools by the sea wall", listOf("The pools by the sea wall, the waves coming over, your eldest counting the big ones. Wrinkled potatoes and green sauce at a plastic table. Rain for ten minutes and nobody minded."), "Garachico, and the rock pools there.", listOf(1), false) to
                "Puerto de la Cruz — The pools by the sea wall\nThe pools by the sea wall, the waves coming over, your eldest counting the big ones. Wrinkled potatoes and green sauce at a plastic table. Rain for ten minutes and nobody minded.\nTomorrow: Garachico, and the rock pools there.\nWhat happened.",
            Mark.Day("Garachico", "A town built on what buried it", listOf("The town the lava buried, built again on top. You kept turning it over: the rock pools you swam in were made by the thing that ruined it. It felt as if it said something about you, and you are not sure it does."), "", listOf(3), false) to
                "Garachico — A town built on what buried it\nThe town the lava buried, built again on top. You kept turning it over: the rock pools you swam in were made by the thing that ruined it. It felt as if it said something about you, and you are not sure it does.\nWhat you made of it.",
            Mark.Day("Garachico", "Closing the laptop", listOf("The laptop on the table all evening. You answered the two that mattered and chose to close it for the rest of the week. You told your partner, so it would hold."), "back south, the long way round by the coast.", listOf(5), false) to
                "Garachico — Closing the laptop\nThe laptop on the table all evening. You answered the two that mattered and chose to close it for the rest of the week. You told your partner, so it would hold.\nTomorrow: back south, the long way round by the coast.\nA choice.",
            Mark.Day("Arona", "Monday, arriving early", listOf("Monday arrived in your head on the drive back: the inbox, the meeting you have been putting off. You pictured it going badly, then going fine. Neither has happened yet."), "a slow morning, nothing before ten.", listOf(6), false) to
                "Arona — Monday, arriving early\nMonday arrived in your head on the drive back: the inbox, the meeting you have been putting off. You pictured it going badly, then going fine. Neither has happened yet.\nTomorrow: a slow morning, nothing before ten.\nWhat’s ahead.",
            Mark.Day("Arona", "Lizards and the first quiet", listOf("A slow morning by the pool. The kids in fits over lizards on the wall, your youngest laughing. The first morning without email. Monday is there, not dreaded, just known."), "walk up to the old town before it gets hot.", listOf(1), false) to
                "Arona — Lizards and the first quiet\nA slow morning by the pool. The kids in fits over lizards on the wall, your youngest laughing. The first morning without email. Monday is there, not dreaded, just known.\nTomorrow: walk up to the old town before it gets hot.\nWhat happened.",
        ),
        "trial",
    )

    @Test
    fun theMixedFixtureReadsAndWritesAsMarkJsDoes() = roundTrips(
        mixed,
        listOf(
            Mark.Day("Arona", "The sea in the dark", listOf("You landed late and heard the sea before you saw it; something in your chest loosened at the sound."), "", listOf(1, 2), false) to
                "Arona — The sea in the dark\nYou landed late and heard the sea before you saw it; something in your chest loosened at the sound.\nWhat happened · what you felt.",
            Mark.Day("Arona", "Black sand and a bakery", listOf("The bakery by the roundabout, the black sand too hot by eleven, both kids in the water until their lips went blue."), "", listOf(1), false) to
                "Arona — Black sand and a bakery\nThe bakery by the roundabout, the black sand too hot by eleven, both kids in the water until their lips went blue.\nWhat happened.",
            Mark.Day("Arona", "Thinking about Monday", listOf("The drive north booked for tomorrow. Monday already in your head, and a tightness that came with it."), "the drive north.", listOf(1, 6, 2), false) to
                "Arona — Thinking about Monday\nThe drive north booked for tomorrow. Monday already in your head, and a tightness that came with it.\nTomorrow: the drive north.\nWhat happened · what’s ahead · what you felt.",
            Mark.Day("Puerto de la Cruz", "The couple who looked so sure", listOf("The story you told yourself about the couple at the next table: that they had it all worked out."), "", listOf(3), false) to
                "Puerto de la Cruz — The couple who looked so sure\nThe story you told yourself about the couple at the next table: that they had it all worked out.\nWhat you made of it.",
            Mark.Day("Puerto de la Cruz", "The pools and what they were for", listOf("The pools by the sea wall, and the thought that this is what the holiday is for: being near them while they are small.", "You wondered whether you always tell it that way."), "", listOf(1, 3, 4), false) to
                "Puerto de la Cruz — The pools and what they were for\nThe pools by the sea wall, and the thought that this is what the holiday is for: being near them while they are small.\nYou wondered whether you always tell it that way.\nWhat happened · what you made of it · what matters.",
            Mark.Day("Garachico", "Heavy legs", listOf("Your legs heavy all day, and you did not mind it."), "", listOf(2), false) to
                "Garachico — Heavy legs\nYour legs heavy all day, and you did not mind it.\nWhat you felt.",
            Mark.Day("Garachico", "Closing the laptop", listOf("You chose to close the laptop for the week, and pictured Monday going fine."), "back south by the coast.", listOf(6, 5), false) to
                "Garachico — Closing the laptop\nYou chose to close the laptop for the week, and pictured Monday going fine.\nTomorrow: back south by the coast.\nWhat’s ahead · a choice.",
            Mark.Day("Arona", "Everything at once", listOf("The drive back, the sense the week had a shape, what it might mean, the choice to stay off email, what matters about it, the quiet it left."), "", listOf(1, 6, 3, 5, 4, 2), false) to
                "Arona — Everything at once\nThe drive back, the sense the week had a shape, what it might mean, the choice to stay off email, what matters about it, the quiet it left.\nWhat happened · what’s ahead · what you made of it · a choice · what matters · what you felt.",
            Mark.Day("Arona", "Lizards and the first quiet", listOf("Your youngest laughing at the lizards, and how much that matters to you."), "walk up to the old town before it gets hot.", listOf(4, 2), false) to
                "Arona — Lizards and the first quiet\nYour youngest laughing at the lizards, and how much that matters to you.\nTomorrow: walk up to the old town before it gets hot.\nWhat matters · what you felt.",
        ),
        "mixed",
    )

    @Test
    fun everyPlaceADayTouchedInP6sOrder() {
        assertEquals(listOf(listOf(1), listOf(1), listOf(3), listOf(2), listOf(1), listOf(3), listOf(5), listOf(6), listOf(1)), trial.map { Mark.placesOf(it.first) })
        assertEquals(
            listOf(listOf(1, 2), listOf(1), listOf(1, 6, 2), listOf(3), listOf(1, 3, 4), listOf(2), listOf(6, 5), listOf(1, 6, 3, 5, 4, 2), listOf(4, 2)),
            mixed.map { Mark.placesOf(it.first) },
        )
        // As mark.js's own check: the mixed fixture's places, as it gives them, in P6's order.
        mixed.forEach { (text, places) -> assertEquals(Mark.inOrder(places), Mark.placesOf(text)) }
        assertEquals(listOf(1, 6), Mark.placesOf("Here — A day\nSome words.\nWhat happened · what's ahead."), "a straight apostrophe names the same place")
        assertEquals(emptyList<Int>(), Mark.placesOf("Into bed early."))
        // A day with no line of its own stands on the places given for it; its own line wins.
        assertEquals(listOf(1, 2), Mark.placesOf("Kept through Claude.", listOf(2, 1)))
        assertEquals(listOf(5), Mark.placesOf("Mine\nx\nA choice.", listOf(1)))
    }

    // ---- The sums ---------------------------------------------------------------

    private fun sums(fx: List<Pair<String, List<Int>>>, n: Int): List<Int> = Mark.counts(fx.take(n).map { Mark.placesOf(it.first) })

    @Test
    fun theSumsOfBothFixturesAreMarkJsOwn() {
        val none = Mark.counts(emptyList())
        assertEquals(listOf(0, 0, 0, 0, 0, 0), none)
        near(listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), Mark.shares(none), "no days: shares")
        assertEquals(0.0, Mark.evenness(Mark.shares(none)))

        assertEquals(listOf(1, 0, 0, 0, 0, 0), sums(trial, 1))
        assertEquals(0.0, Mark.evenness(Mark.shares(sums(trial, 1))))
        assertEquals(listOf(2, 0, 1, 0, 0, 0), sums(trial, 3))
        near(listOf(2.0 / 3, 0.0, 1.0 / 3, 0.0, 0.0, 0.0), Mark.shares(sums(trial, 3)), "trial, 3 days")
        near(8.0 / 15, Mark.evenness(Mark.shares(sums(trial, 3))), "trial, 3 days: evenness 8/15", 1e-12)
        assertEquals(listOf(4, 1, 2, 0, 1, 1), sums(trial, 9))
        near(listOf(4.0 / 9, 1.0 / 9, 2.0 / 9, 0.0, 1.0 / 9, 1.0 / 9), Mark.shares(sums(trial, 9)), "trial, 9 days")
        assertEquals(0.8592592592592593, Mark.evenness(Mark.shares(sums(trial, 9))), "trial, 9 days: evenness 116/135, to the bit")
        near(116.0 / 135, Mark.evenness(Mark.shares(sums(trial, 9))), "116/135", 1e-12)
        val thrice = (trial + trial + trial).map { Mark.placesOf(it.first) }
        assertEquals(listOf(12, 3, 6, 0, 3, 3), Mark.counts(thrice))
        near(116.0 / 135, Mark.evenness(Mark.shares(Mark.counts(thrice))), "27 days: as nine", 1e-12)

        assertEquals(listOf(1, 1, 0, 0, 0, 0), sums(mixed, 1))
        assertEquals(0.5999999999999999, Mark.evenness(Mark.shares(sums(mixed, 1))), "mixed, 1 day, to the bit")
        assertEquals(listOf(3, 2, 0, 0, 0, 1), sums(mixed, 3))
        near(11.0 / 15, Mark.evenness(Mark.shares(sums(mixed, 3))), "mixed, 3 days: evenness 11/15", 1e-12)
        assertEquals(listOf(5, 5, 3, 3, 2, 3), sums(mixed, 9))
        near(listOf(5, 5, 3, 3, 2, 3).map { it / 21.0 }, Mark.shares(sums(mixed, 9)), "mixed, 9 days: touches over 21")
        assertEquals(0.9795918367346939, Mark.evenness(Mark.shares(sums(mixed, 9))), "mixed, 9 days: evenness 48/49, to the bit")
        near(48.0 / 49, Mark.evenness(Mark.shares(sums(mixed, 9))), "48/49", 1e-12)

        // One day touching all six counts once in each; a day's repeats and strays count nothing more.
        assertEquals(listOf(1, 1, 1, 1, 1, 1), Mark.counts(listOf(Mark.placesOf("Here\nx\nWhat happened · what’s ahead · what you made of it · a choice · what matters · what you felt."))))
        near(1.0, Mark.evenness(Mark.shares(listOf(1, 1, 1, 1, 1, 1))), "all six even", 1e-12)
        assertEquals(listOf(1, 0, 0, 1, 0, 0), Mark.counts(listOf(listOf(4, 1, 4, 9))))
    }

    // ---- The dial ---------------------------------------------------------------

    @Test
    fun theSpiralOfDays() {
        near(DialMath.Pt(232.0, -42.0), DialMath.dayAt(1), "day 1")
        near(DialMath.Pt(349.18018124585615, 0.3500980194102965), DialMath.dayAt(2), "day 2")
        near(DialMath.Pt(113.46996477380212, -1.2585953111395156), DialMath.dayAt(9), "day 9")
        near(DialMath.Pt(231.99999999999997, -44.69999999999999), DialMath.dayAt(10), "day 10")
        near(DialMath.Pt(99.58575240457242, -17.805155282509048), DialMath.dayAt(81), "day 81")
        val d81 = DialMath.dayAt(81)
        near(206.0, kotlin.math.hypot(d81.x - DialMath.CX, d81.y - DialMath.CY), "day 81 at 206")

        assertEquals(emptyList<DialMath.Pt>(), DialMath.spiralPoints(0))
        assertEquals(emptyList<DialMath.Pt>(), DialMath.spiralPoints(1))
        assertEquals(81, DialMath.spiralPoints(9).size)
        assertEquals(91, DialMath.spiralPoints(10).size)
        assertEquals(801, DialMath.spiralPoints(81).size)
        for (n in listOf(9, 10, 81)) {
            near(DialMath.dayAt(1), DialMath.spiralPoints(n).first(), "spiral $n first")
            near(DialMath.dayAt(n), DialMath.spiralPoints(n).last(), "spiral $n last")
        }
        near(DialMath.Pt(294.2989691067706, -31.165010876153218), DialMath.spiralPoints(10)[5], "spiral 10 at day 1.5")
        near(DialMath.Pt(257.50903767497044, 321.50623431964243), DialMath.spiralPoints(10)[43], "spiral 10 at day 5.3")
        near(DialMath.Pt(219.11807199367234, -44.22015316148173), DialMath.spiralPoints(10)[89], "spiral 10 at day 9.9")
        near(DialMath.Pt(69.74070572483936, 13.229145615694407), DialMath.spiralPoints(81)[797], "spiral 81 at day 80.7")
        // mark.js's spiralPath(2), each point to one decimal.
        val path = "M 232.0,-42.0 L 244.7,-41.6 L 257.3,-40.3 L 269.9,-38.1 L 282.2,-35.1 L 294.3,-31.2 L 306.1,-26.4 L 317.5,-20.9 L 328.6,-14.5 L 339.1,-7.5 L 349.2,0.4"
        val drawn = path.removePrefix("M ").split(" L ").map { p -> p.split(",").let { DialMath.Pt(it[0].toDouble(), it[1].toDouble()) } }
        val two = DialMath.spiralPoints(2)
        assertEquals(drawn.size, two.size)
        drawn.forEachIndexed { i, p ->
            near(p.x, two[i].x, "spiral 2 point $i x", 0.05 + 1e-9)
            near(p.y, two[i].y, "spiral 2 point $i y", 0.05 + 1e-9)
        }

        val ninth = DialMath.Dot("ninth", 5.0, 1.0)
        val full = DialMath.Dot("full", 6.0, 1.0)
        val faint = DialMath.Dot("faint", 2.0, 0.35)
        assertEquals(full, DialMath.dotOf(1, 1))
        assertEquals(listOf(full, full, full, ninth), listOf(1, 2, 8, 9).map { DialMath.dotOf(it, 9) })
        assertEquals(listOf(faint, full, ninth, full), listOf(1, 2, 9, 10).map { DialMath.dotOf(it, 10) })
        assertEquals(
            listOf(faint, faint, ninth, ninth, ninth, ninth, full, full, ninth),
            listOf(1, 2, 9, 18, 27, 72, 73, 80, 81).map { DialMath.dotOf(it, 81) },
        )
        // mark.js's own: the latest nine full, earlier faint, every ninth open.
        assertEquals(listOf(full, full, faint, ninth, ninth), listOf(22, 14, 13, 9, 18).map { DialMath.dotOf(it, 22) })
    }

    @Test
    fun theColourInsideTheInk() {
        val cases = listOf(
            0.0 to 0.0, 1.0 / 21 to 0.4261904761904762, 2.0 / 21 to 0.5023809523809524, 1.0 / 9 to 0.5277777777777778,
            0.4 to 0.9900000000000001, 13.0 / 32 to 1.0, 4.0 / 9 to 1.0, 1.0 to 1.0, -0.1 to 0.0,
        )
        for ((share, want) in cases) assertEquals(want, DialMath.tint(share), "tint($share)")
    }

    @Test
    fun thePoolsOfTheMixedFixture() {
        val want = listOf(
            DialMath.PoolAt(232.0, 12.0, 32.590301240125065, 5),
            DialMath.PoolAt(121.14874831559186, 75.99999999999999, 32.590301240125065, 5),
            DialMath.PoolAt(342.85125168440817, 204.0, 28.850792082313724, 3),
            DialMath.PoolAt(121.14874831559185, 204.0, 28.850792082313724, 3),
            DialMath.PoolAt(232.0, 268.0, 26.49262779742225, 2),
            DialMath.PoolAt(342.85125168440817, 76.0, 28.850792082313724, 3),
        )
        val got = DialMath.poolsAt(sums(mixed, 9))
        assertEquals(6, got.size)
        want.forEachIndexed { i, p ->
            near(p.x, got[i].x, "pool ${i + 1} x")
            near(p.y, got[i].y, "pool ${i + 1} y")
            near(p.r, got[i].r, "pool ${i + 1} r", 1e-12)
            assertEquals(p.days, got[i].days, "pool ${i + 1} days")
        }
        // The trial's Purpose has no day: a pale ring of its own small size.
        val trialPools = DialMath.poolsAt(sums(trial, 9))
        assertEquals(14.0, trialPools[3].r)
        near(38.666666666666664, trialPools[0].r, "trial Facts r", 1e-12)
        assertTrue(DialMath.poolsAt(listOf(0, 0, 0, 0, 0, 0)).all { it.r == 14.0 && it.days == 0 })
    }

    @Test
    fun aCirclesPhraseOnOneLineOrTwo() {
        assertEquals(
            listOf(listOf("what happened"), listOf("what you felt"), listOf("what you", "made of it"), listOf("what matters"), listOf("a choice"), listOf("what’s ahead")),
            Mark.POOLS.map { DialMath.captionLines(it.phrase) },
        )
        assertEquals(listOf("what you made", "of it, really"), DialMath.captionLines("what you made of it, really"))
        assertEquals(listOf("abcdefghijklmnop"), DialMath.captionLines("abcdefghijklmnop"))
        assertEquals(listOf("a b c d", "e f g h i"), DialMath.captionLines("a b c d e f g h i"))
    }

    @Test
    fun aShellsCoilAndItsBands() {
        val pts = DialMath.coilPoints()
        assertEquals(301, pts.size)
        val want = mapOf(
            0 to DialMath.CoilPt(1.5308084989341916e-16, -2.5, 0.0),
            1 to DialMath.CoilPt(0.13476508732558073, -2.571471051993028, 0.0007010915688517178),
            100 to DialMath.CoilPt(-8.660254037844386, -5.000000000000001, 0.1548299150134811),
            150 to DialMath.CoilPt(13.750000000000002, -3.3677786976552217e-15, 0.29874050134836083),
            299 to DialMath.CoilPt(1.304473709355352, 24.890841153757755, 0.9939836989030877),
            300 to DialMath.CoilPt(5.818619747541399e-14, 25.0, 1.0),
        )
        for ((i, p) in want) {
            near(p.x, pts[i].x, "coil $i x")
            near(p.y, pts[i].y, "coil $i y")
            near(p.at, pts[i].at, "coil $i at")
        }

        val three = DialMath.coilBands(listOf(2, 6, 1))
        assertEquals(listOf(1, 6, 2), three.map { it.n })
        near(listOf(0.0, 0.3183333333333333, 0.34833333333333333, 0.6516666666666666, 0.6816666666666666, 1.0), three.flatMap { listOf(it.from, it.to) }, "three bands")
        assertEquals(listOf(DialMath.Band(4, 0.0, 1.0)), DialMath.coilBands(listOf(4)))
        assertEquals(emptyList<DialMath.Band>(), DialMath.coilBands(emptyList()))
        assertEquals(listOf(DialMath.Band(3, 0.0, 0.485), DialMath.Band(5, 0.515, 1.0)), DialMath.coilBands(listOf(3, 3, 5, 8)))
    }

    @Test
    fun coloursMixRoundingEachChannelHalfUp() {
        assertEquals(0xFF808080L, DialMath.mixHex(0xFF000000L, 0xFFFFFFFFL, 0.5))
        assertEquals(0xFFF3CB8FL, DialMath.mixHex(DialMath.DAWN, DialMath.MIDDAY, 0.0))
        assertEquals(0xFFF1E79BL, DialMath.mixHex(DialMath.DAWN, DialMath.MIDDAY, 1.0))
        assertEquals(0xFFEEC78FL, DialMath.mixHex(DialMath.EVENING, DialMath.MIDDAY, 0.3))
        assertEquals(0xFF010101L, DialMath.mixHex(0xFF000000L, 0xFF010101L, 0.5), "0.5 rounds up, as Math.round does")
        assertEquals(0xFF020202L, DialMath.mixHex(0xFF000000L, 0xFF030303L, 0.5), "1.5 rounds up")
        assertEquals(0xFFF2DF97L, DialMath.mixHex(DialMath.DAWN, DialMath.MIDDAY, 0.7071067811865476))
        assertEquals(0xFF646177L, DialMath.mixHex(0xFF4A6B8AL, 0xFFB0413EL, 0.25))
    }

    @Test
    fun theCrimsonCentre() {
        assertEquals(DialMath.Centre(0.5, 0.45), DialMath.centre(0, 0.0, false))
        assertEquals(DialMath.Centre(0.5, 1.0), DialMath.centre(0, 0.0, true))
        assertEquals(DialMath.Centre(0.55, 1.0), DialMath.centre(3, 0.0, false))
        assertEquals(DialMath.Centre(1.0, 1.0), DialMath.centre(9, 1.0, true))
        assertEquals(DialMath.Centre(0.9366666666666668, 1.0), DialMath.centre(9, 116.0 / 135, false))
    }

    /** mark.js at h:m on a day: its shadowAngle, sunHeight and (hour < 12.75) as the inputs, and its castOf and lightOf(date, days). */
    private data class Hour(val h: Int, val m: Int, val shadowDeg: Double, val s: Double, val beforeNoon: Boolean, val days: Int, val cast: DialMath.Cast, val light: DialMath.Light)

    @Test
    fun theShadowAndTheLightAgreeWithMarkJsByTheClock() {
        val hours = listOf(
            Hour(6, 0, -180.0, 0.0, true, 1, DialMath.Cast(-84.0, -1.0287033112837767e-14, 84.0, 0.0), DialMath.Light(278.0, 140.0, 46.0, 0xFFF3CB8FL, 0.0875)),
            Hour(6, 0, -180.0, 0.0, true, 18, DialMath.Cast(-84.0, -1.0287033112837767e-14, 84.0, 0.0), DialMath.Light(278.0, 140.0, 46.0, 0xFFF3CB8FL, 0.16666666666666666)),
            Hour(6, 0, -180.0, 0.0, true, 27, DialMath.Cast(-84.0, -1.0287033112837767e-14, 84.0, 0.0), DialMath.Light(278.0, 140.0, 46.0, 0xFFF3CB8FL, 0.25)),
            Hour(6, 30, -172.5, 0.11609291412523023, true, 1, DialMath.Cast(-75.22438773480413, -9.90347852476963, 75.87349601123388, 0.07429946504014735), DialMath.Light(273.4628735897459, 145.45869086524053, 41.82065509149171, 0xFFF3CE90L, 0.1078162599719153)),
            Hour(6, 30, -172.5, 0.11609291412523023, true, 18, DialMath.Cast(-75.22438773480413, -9.90347852476963, 75.87349601123388, 0.07429946504014735), DialMath.Light(273.4628735897459, 145.45869086524053, 41.82065509149171, 0xFFF3CE90L, 0.20536430470841008)),
            Hour(6, 30, -172.5, 0.11609291412523023, true, 27, DialMath.Cast(-75.22438773480413, -9.90347852476963, 75.87349601123388, 0.07429946504014735), DialMath.Light(273.4628735897459, 145.45869086524053, 41.82065509149171, 0xFFF3CE90L, 0.30804645706261513)),
            Hour(9, 0, -135.0, 0.6427876096865393, true, 1, DialMath.Cast(-27.58060618262694, -27.580606182626944, 39.00486732194226, 0.16), DialMath.Light(248.16421073838762, 156.16421073838762, 22.859646051284585, 0xFFF2DD97L, 0.19998783169514436)),
            Hour(9, 0, -135.0, 0.6427876096865393, true, 18, DialMath.Cast(-27.58060618262694, -27.580606182626944, 39.00486732194226, 0.16), DialMath.Light(248.16421073838762, 156.16421073838762, 22.859646051284585, 0xFFF2DD97L, 0.3809292032288464)),
            Hour(9, 0, -135.0, 0.6427876096865393, true, 27, DialMath.Cast(-27.58060618262694, -27.580606182626944, 39.00486732194226, 0.16), DialMath.Light(248.16421073838762, 156.16421073838762, 22.859646051284585, 0xFFF2DD97L, 0.5713938048432696)),
            Hour(12, 0, -90.0, 0.984807753012208, true, 1, DialMath.Cast(9.223707376622414e-16, -15.063457289145438, 15.063457289145438, 0.16), DialMath.Light(232.0, 150.54692089156052, 10.54692089156051, 0xFFF1E79BL, 0.25984135677713643)),
            Hour(12, 0, -90.0, 0.984807753012208, true, 18, DialMath.Cast(9.223707376622414e-16, -15.063457289145438, 15.063457289145438, 0.16), DialMath.Light(232.0, 150.54692089156052, 10.54692089156051, 0xFFF1E79BL, 0.494935917670736)),
            Hour(12, 0, -90.0, 0.984807753012208, true, 27, DialMath.Cast(9.223707376622414e-16, -15.063457289145438, 15.063457289145438, 0.16), DialMath.Light(232.0, 150.54692089156052, 10.54692089156051, 0xFFF1E79BL, 0.7424038765061041)),
            Hour(12, 44, -79.00000000000001, 0.9999924785916561, true, 1, DialMath.Cast(2.67142639593752, -13.74329739358947, 14.000526498584074, 0.16), DialMath.Light(230.09185838074924, 149.8165376303566, 10.000270770700382, 0xFFF1E79BL, 0.2624986837535398)),
            Hour(12, 44, -79.00000000000001, 0.9999924785916561, true, 18, DialMath.Cast(2.67142639593752, -13.74329739358947, 14.000526498584074, 0.16), DialMath.Light(230.09185838074924, 149.8165376303566, 10.000270770700382, 0xFFF1E79BL, 0.49999749286388534)),
            Hour(12, 44, -79.00000000000001, 0.9999924785916561, true, 27, DialMath.Cast(2.67142639593752, -13.74329739358947, 14.000526498584074, 0.16), DialMath.Light(230.09185838074924, 149.8165376303566, 10.000270770700382, 0xFFF1E79BL, 0.749996239295828)),
            Hour(12, 45, -78.75, 1.0, false, 1, DialMath.Cast(2.7312645082257965, -13.730993925645226, 14.0, 0.16), DialMath.Light(230.04909677983872, 149.8078528040323, 10.0, 0xFFF1E79BL, 0.26249999999999996)),
            Hour(12, 45, -78.75, 1.0, false, 18, DialMath.Cast(2.7312645082257965, -13.730993925645226, 14.0, 0.16), DialMath.Light(230.04909677983872, 149.8078528040323, 10.0, 0xFFF1E79BL, 0.5)),
            Hour(12, 45, -78.75, 1.0, false, 27, DialMath.Cast(2.7312645082257965, -13.730993925645226, 14.0, 0.16), DialMath.Light(230.04909677983872, 149.8078528040323, 10.0, 0xFFF1E79BL, 0.75)),
            Hour(15, 10, -42.50000000000001, 0.8459864259198412, false, 1, DialMath.Cast(18.270432956471712, -16.741767280810354, 24.78095018561112, 0.16), DialMath.Light(220.53940079360333, 150.5017043257406, 15.544488666885718, 0xFFF0E098L, 0.23554762453597217)),
            Hour(15, 10, -42.50000000000001, 0.8459864259198412, false, 18, DialMath.Cast(18.270432956471712, -16.741767280810354, 24.78095018561112, 0.16), DialMath.Light(220.53940079360333, 150.5017043257406, 15.544488666885718, 0xFFF0E098L, 0.44866214197328036)),
            Hour(15, 10, -42.50000000000001, 0.8459864259198412, false, 27, DialMath.Cast(18.270432956471712, -16.741767280810354, 24.78095018561112, 0.16), DialMath.Light(220.53940079360333, 150.5017043257406, 15.544488666885718, 0xFFF0E098L, 0.6729932129599205)),
            Hour(17, 0, -15.0, 0.5495089780708062, false, 1, DialMath.Cast(43.982825449540385, -11.785162560043377, 45.53437153504356, 0.16), DialMath.Light(206.67566888376984, 146.78563407145222, 26.217676789450977, 0xFFEFD293L, 0.18366407116239108)),
            Hour(17, 0, -15.0, 0.5495089780708062, false, 18, DialMath.Cast(43.982825449540385, -11.785162560043377, 45.53437153504356, 0.16), DialMath.Light(206.67566888376984, 146.78563407145222, 26.217676789450977, 0xFFEFD293L, 0.34983632602360204)),
            Hour(17, 0, -15.0, 0.5495089780708062, false, 27, DialMath.Cast(43.982825449540385, -11.785162560043377, 45.53437153504356, 0.16), DialMath.Light(206.67566888376984, 146.78563407145222, 26.217676789450977, 0xFFEFD293L, 0.5247544890354031)),
            Hour(19, 29, 22.250000000000014, 0.0038784997249199362, false, 1, DialMath.Cast(77.49412273615916, 31.7036826585274, 83.7285050192556, 0.0024822398239487593), DialMath.Light(189.55436632444037, 122.63503278988483, 45.860374009902884, 0xFFEDB98AL, 0.08817873745186099)),
            Hour(19, 29, 22.250000000000014, 0.0038784997249199362, false, 18, DialMath.Cast(77.49412273615916, 31.7036826585274, 83.7285050192556, 0.0024822398239487593), DialMath.Light(189.55436632444037, 122.63503278988483, 45.860374009902884, 0xFFEDB98AL, 0.16795949990830664)),
            Hour(19, 29, 22.250000000000014, 0.0038784997249199362, false, 27, DialMath.Cast(77.49412273615916, 31.7036826585274, 83.7285050192556, 0.0024822398239487593), DialMath.Light(189.55436632444037, 122.63503278988483, 45.860374009902884, 0xFFEDB98AL, 0.25193924986245997)),
            Hour(19, 30, 22.5, 1.2246467991473532e-16, false, 1, DialMath.Cast(77.60588073094807, 32.145408318667535, 83.99999999999999, 7.83773951454306e-17), DialMath.Light(189.5015415044808, 122.3965621112059, 45.99999999999999, 0xFFEDB98AL, 0.08750000000000001)),
            Hour(19, 30, 22.5, 1.2246467991473532e-16, false, 18, DialMath.Cast(77.60588073094807, 32.145408318667535, 83.99999999999999, 7.83773951454306e-17), DialMath.Light(189.5015415044808, 122.3965621112059, 45.99999999999999, 0xFFEDB98AL, 0.16666666666666669)),
            Hour(19, 30, 22.5, 1.2246467991473532e-16, false, 27, DialMath.Cast(77.60588073094807, 32.145408318667535, 83.99999999999999, 7.83773951454306e-17), DialMath.Light(189.5015415044808, 122.3965621112059, 45.99999999999999, 0xFFEDB98AL, 0.25000000000000006)),
        )
        for (t in hours) {
            val at = "%02d:%02d, %d days".format(t.h, t.m, t.days)
            assertEquals(t.shadowDeg, DialMath.momentAngle(t.h + t.m / 60.0), "$at: the moments' angle is the shadow's")
            val c = DialMath.castOf(t.shadowDeg, t.s)
            near(t.cast.dx, c.dx, "$at cast dx")
            near(t.cast.dy, c.dy, "$at cast dy")
            near(t.cast.length, c.length, "$at cast length", 1e-12)
            near(t.cast.opacity, c.opacity, "$at cast opacity", 1e-15)
            val l = DialMath.lightOf(t.shadowDeg, t.s, t.beforeNoon, t.days)
            near(t.light.x, l.x, "$at light x")
            near(t.light.y, l.y, "$at light y")
            near(t.light.shift, l.shift, "$at light shift", 1e-12)
            assertEquals(t.light.colour, l.colour, "$at light colour")
            near(t.light.strength, l.strength, "$at light strength", 1e-15)
        }
        // At 12:45 the light stands 10 from the centre, towards the sun, opposite the shadow.
        val noon = DialMath.lightOf(-78.75, 1.0, false, 27)
        near(10.0, kotlin.math.hypot(noon.x - DialMath.CX, noon.y - DialMath.CY), "the light at 12:45")
    }

    @Test
    fun aMomentSitsByTheHourAndThePointsByTheirAngle() {
        assertEquals(-270.0, DialMath.momentAngle(0.0))
        assertEquals(-131.25, DialMath.momentAngle(9.25))
        assertEquals(-90.0, DialMath.momentAngle(12.0))
        assertEquals(7.5, DialMath.momentAngle(18.5))
        near(DialMath.Pt(232.0, -42.0), DialMath.at(-90.0, DialMath.RING), "the ring's top")
        near(DialMath.Pt(398.0, 140.0), DialMath.at(0.0, DialMath.MOMENTS), "the moments' ring, at the right")
        near(DialMath.Pt(315.4386001800126, 223.4386001800126), DialMath.at(45.0, DialMath.LIGHT_R), "the light's reach, at 45 degrees")
        near(DialMath.Pt(232.0, 268.0), DialMath.at(90.0, 128.0), "the pools' ring, at the bottom")
    }
}
