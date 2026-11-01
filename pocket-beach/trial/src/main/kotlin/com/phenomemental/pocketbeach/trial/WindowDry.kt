package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.ToolCall
import com.phenomemental.pocketbeach.bsp.DirShelfSource
import com.phenomemental.pocketbeach.bsp.Shelf
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime

/** The window trial's pocket, window and the app's answers to looks, with no model: to check the plumbing. */
fun main() {
    val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "shelf")))
    val at = ZonedDateTime.of(2026, 11, 1, 8, 40, 0, 0, ZoneId.of("Europe/London"))
    val last = ZonedDateTime.of(2026, 11, 1, 0, 16, 0, 0, ZoneId.of("Europe/London"))
    val (_, seen) = scratchPocket(shelf, last.toInstant().toEpochMilli(), films = true, hers = true, walk = true, beds = false)
    val (storage, pages) = scratchPocket(shelf, at.toInstant().toEpochMilli(), films = true, hers = true, walk = true)
    check(pages.carryOut(pages.planWhole("review:pocket-companion", review(pages, seen, last, at))).ok)
    val w = window(pages, storage, shelf, at)
    println("== window (${w.text.length} chars; ${w.members})\n${w.text}\n")
    for (args in listOf(
        mapOf("block" to "films"),
        mapOf("block" to "films", "at" to "2", "how" to "beneath"),
        mapOf("block" to "films", "at" to "2.1", "how" to "walk"),
        mapOf("block" to "garden"),
        mapOf("block" to "now", "at" to "today"),
        mapOf("block" to "now"),
        mapOf("block" to "telling", "at" to "2", "how" to "beneath"),
        mapOf("block" to "telling", "at" to "2.2", "how" to "beneath"),
        mapOf("block" to "pilot"),
    )) {
        val (found, sees) = runLook(pages, ToolCall("look", args), at)
        println("== look $args  [$sees]\n$found\n")
    }
    val (fold, sees) = runFold(pages, ToolCall("fold", mapOf("blocks" to "now, telling")))
    println("== fold now, telling [$sees] (${fold.length} chars)\n$fold\n")
}
