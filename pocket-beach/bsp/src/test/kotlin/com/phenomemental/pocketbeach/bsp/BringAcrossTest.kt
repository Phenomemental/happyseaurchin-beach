package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Sundial 0.13.5: his blocks brought across from the beach, new or replacing, with what came across and what was refused. */
class BringAcrossTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val h = "Phenomemental"

    @Test
    fun hisBlocksComeFirst() {
        val beach = listOf("lighthouse", "passport:Phenomemental", "spine:pocket-beach", "shell:Phenomemental", "pool:weft", "Phenomemental:notes", "passport:weft")
        assertEquals(
            listOf("passport:Phenomemental", "shell:Phenomemental", "Phenomemental:notes", "lighthouse", "spine:pocket-beach", "pool:weft", "passport:weft"),
            BringAcross.ordered(beach, h),
        )
        assertEquals(beach, BringAcross.ordered(beach, null))
    }

    @Test
    fun theListShowsHisAndOnlyWhatHisSearchFindsOfTheRest() {
        // The beach as it stood on 28 November: thousands of blocks, eighty his.
        val rest = (1..2_540).map { "block-$it" } + listOf("lighthouse", "spine:pocket-beach")
        val his = (1..78).map { "notes-$it:Phenomemental" } + listOf("passport:Phenomemental", "shell:Phenomemental")
        val beach = (rest + his).sorted()

        val opened = BringAcross.shown(beach, h, "", emptySet())
        assertEquals(80, opened.his.size)
        assertEquals(emptyList(), opened.rest, "none of the rest until he searches")
        assertEquals(2_542, opened.restCount)

        val light = BringAcross.shown(beach, h, " light", emptySet())
        assertEquals(listOf("lighthouse"), light.rest)
        assertEquals(emptyList(), light.his, "his search narrows his too")
        assertEquals(0, light.more)

        val many = BringAcross.shown(beach, h, "block-", emptySet())
        assertEquals(BringAcross.REST_AT_ONCE, many.rest.size)
        assertEquals(2_540 - BringAcross.REST_AT_ONCE, many.more)

        // What he ticked stays shown whatever he searches next.
        val ticked = setOf("lighthouse", "passport:Phenomemental")
        val after = BringAcross.shown(beach, h, "spine", ticked)
        assertEquals(listOf("lighthouse", "spine:pocket-beach"), after.rest)
        assertEquals(listOf("passport:Phenomemental"), after.his)

        // With no handle known, nothing is his: the rest wait for his search.
        val unknown = BringAcross.shown(beach, null, "", emptySet())
        assertEquals(emptyList(), unknown.his)
        assertEquals(emptyList(), unknown.rest)
        assertEquals(2_622, unknown.restCount)
        assertEquals(listOf("passport:Phenomemental"), BringAcross.shown(beach, null, "passport:Phen", emptySet()).rest)
    }

    @Test
    fun thePlacesThatWouldChangeAreNamed() {
        val mine = JsObj.of("_" to str("My shell."), "1" to str("Faces."), "3" to JsObj.of("_" to str("Manifest."), "1" to str("passport"), "2" to str("history")), "6" to str("Old place."))
        val theirs = JsObj.of("_" to str("My shell."), "1" to str("Faces, renamed."), "3" to JsObj.of("_" to str("Manifest."), "1" to str("passport"), "2" to str("history"), "3" to str("lists")), "7" to JsObj.of("_" to str("New."), "1" to str("Under it.")))
        assertEquals(listOf("1 changes", "3.3 is new", "6 goes", "7 is new"), BringAcross.changes(mine, theirs).map { it.said })
        assertEquals(listOf("the opening line changes"), BringAcross.changes(JsObj.of("_" to str("a")), JsObj.of("_" to str("b"))).map { it.said })
        assertEquals(emptyList(), BringAcross.changes(mine, mine))
    }

    @Test
    fun eachComesAcrossNewOrReplacingAndWhatIsRefusedSaysWhy() {
        val (_, pages) = Bench.fixture(shelf, 1_795_000_000_000L)
        check(pages.carryOut(pages.planKeep("spine:demo", JsObj.of("_" to str("Mine.")))).ok)
        check(pages.carryOut(pages.planKeep("lists:Phenomemental", JsObj.of("_" to str("My projects.")))).ok)
        val report = BringAcross.bring(pages, listOf(
            BringAcross.Read("passport:weft", JsObj.of("_" to str("Weft's passport."))),
            BringAcross.Read("spine:demo", JsObj.of("_" to str("The beach's."), "1" to str("A decision."))),
            BringAcross.Read("lists:Phenomemental", JsObj.of("_" to str("My projects."))),
            BringAcross.Read("pool:weft", null, "the beach answered 500"),
        ))
        assertEquals(listOf("passport:weft, new", "spine:demo, replacing yours"), report.came.map { it.said })
        assertEquals(listOf("lists:Phenomemental: yours is already the same as the beach's", "pool:weft: the beach answered 500"), report.refused.map { it.said })
        assertTrue(deepEqual(pages.whole("spine:demo")!!, JsObj.of("_" to str("The beach's."), "1" to str("A decision."))))
        assertTrue(pages.exists("passport:weft"))
    }
}
