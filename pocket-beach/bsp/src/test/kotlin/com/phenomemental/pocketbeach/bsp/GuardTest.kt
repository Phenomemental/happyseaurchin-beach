package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Sundial 0.11, the guard (spine:pocket-beach 1.111178719943): every write
 * carries the block as he saw it and lands only while it still stands so;
 * otherwise nothing is written and it is held. Here the blocks are in a
 * folder and the outside change is a file written by another hand, as a
 * sync from the laptop writes it.
 */
class GuardTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val roots = mutableListOf<File>()
    private var now = 1_793_178_000_000

    @AfterTest
    fun cleanUp() = roots.forEach { it.deleteRecursively() }

    private val dir = Files.createTempDirectory("sundial-guard").toFile().also { roots += it }
    private val storage = FolderStorage(dir)
    private val bsp = PocketBsp(storage, shelf) { now }
    private val pages = Pages(storage, shelf, bsp) { now }

    init {
        check(pages.carryOut(pages.planNewBlock("garden")).ok)
        check(pages.carryOut(pages.planAdd("garden", "", "plant the bulbs")).ok)
        check(pages.carryOut(pages.planAdd("garden", "", "cut the hedge")).ok)
    }

    /** The laptop's edit: line 1 rewritten in the file itself. */
    private fun laptopRewritesLineOne(words: String) {
        val block = parseJson(File(dir, "garden.json").readText()) as JsObj
        block["1"] = str(words)
        File(dir, "garden.json").writeText(stringify(block)!!)
    }

    private fun lines() = pages.position(Region.POCKET, "garden")!!.lines.map { it.text }

    @Test
    fun aChangeHeSawIsHeldWhenTheLineWasRewrittenOutside() {
        val seen = pages.seen("garden")
        val plan = pages.planChange("garden", "1", "plant the tulips")
        laptopRewritesLineOne("plant the bulbs, done")
        // Planned again, it would still land at line 1: only what he saw tells the two apart.
        val done = pages.carryOut(plan, seen)
        assertFalse(done.ok)
        assertTrue(done.held)
        assertEquals(Pages.CHANGED, done.problem)
        assertEquals(listOf("plant the bulbs, done", "cut the hedge"), lines(), "the laptop's words stand")
        // Planned again on this version, it lands.
        val again = pages.seen("garden")
        assertTrue(pages.carryOut(pages.planChange("garden", "1", "plant the tulips"), again).ok)
        assertEquals(listOf("plant the tulips", "cut the hedge"), lines())
    }

    @Test
    fun anAddHeSawIsHeldWhenTheBlockChangedOutside() {
        val seen = pages.seen("garden")
        val plan = pages.planAdd("garden", "", "water the roses")
        laptopRewritesLineOne("plant the bulbs, done")
        val done = pages.carryOut(plan, seen)
        assertTrue(done.held)
        assertEquals(2, lines().size)
    }

    @Test
    fun aWriteWithNothingChangedLandsAsBefore() {
        val seen = pages.seen("garden")
        assertTrue(pages.carryOut(pages.planChange("garden", "2", "cut the hedge low"), seen).ok)
    }

    @Test
    fun herProposalIsHeldWholeWhenABlockItWritesChangedSinceItWasShown() {
        val answer = Answer("Two lines.", listOf(Step(Act.ADD, "garden", "", "water the roses"), Step(Act.CHANGE, "garden", "2", "cut the hedge low")), null)
        val proposal = Asks.propose(answer, storage, shelf, "shell", "seal", now)
        assertEquals(null, proposal.refusal)
        assertEquals(setOf("garden"), proposal.seen.keys)
        laptopRewritesLineOne("plant the bulbs, done")
        val carried = Asks.carryOut(proposal, pages, storage, "shell")
        assertEquals(listOf("garden"), carried.held)
        assertTrue(carried.landed.isEmpty())
        assertEquals("garden has changed since this was set out. Nothing was written.", carried.problem)
        assertEquals(listOf("plant the bulbs, done", "cut the hedge"), lines())
        // Planned again on this version, it lands.
        val again = Asks.propose(answer, storage, shelf, "shell", "seal", now)
        assertTrue(Asks.carryOut(again, pages, storage, "shell").problem == null)
        assertEquals(listOf("plant the bulbs, done", "cut the hedge low", "water the roses"), lines())
    }

    @Test
    fun renameAndRemoveAreHeldTheSameWay() {
        val seen = pages.seen("garden")
        laptopRewritesLineOne("plant the bulbs, done")
        val renamed = pages.rename("garden", "allotment", seen)
        assertTrue(renamed.held)
        val removed = pages.remove("garden", seen)
        assertTrue(removed.held)
        assertEquals(listOf("garden"), storage.names())
        assertTrue(pages.remove("garden", pages.seen("garden")).ok)
        assertEquals(emptyList(), storage.names())
    }
}
