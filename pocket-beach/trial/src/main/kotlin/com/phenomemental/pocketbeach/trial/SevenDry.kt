package com.phenomemental.pocketbeach.trial

import com.phenomemental.pocketbeach.bsp.DirShelfSource
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.Keeping
import com.phenomemental.pocketbeach.bsp.Shelf
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime

/** 0.7's window and turns on the scratch pocket, with no model: to read what she is given. */
fun main() {
    val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "shelf")))
    val now = ZonedDateTime.of(2026, 11, 1, 8, 40, 0, 0, ZoneId.of("Europe/London"))
    val (storage, pages) = scratchPocket(shelf, now.toInstant().toEpochMilli(), films = true, hers = false, walk = false)
    println("made: ${HerWindow.ensure(pages)}")
    val window = HerWindow.instruction(HelloWorld.SHELL, pages, storage, shelf, now)
    println("== window (${window.length} chars)\n$window\n")
    var seen: String? = null
    for (said in listOf(NOV1[0], NOV1[1], NOV1[3], POST_ASK, "What's in my films block?", "I don't feel safe tonight.")) {
        val turn = HerWindow.turn(said, now, pages, storage, HerWindow.appLine(Keeping.way(said, emptyList(), pages), said), HerWindow.changes(pages, storage, seen))
        println("== turn\n$turn\n   echo lines: ${HerWindow.echoLines(turn)}\n")
        seen = HerWindow.copies(pages, storage)
        if (said == NOV1[1]) check(pages.carryOut(pages.planAdd("garden", "", "Buy more compost")).ok)
    }
}
