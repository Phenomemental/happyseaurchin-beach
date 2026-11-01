package com.phenomemental.pocketbeach.trial

import com.phenomemental.pocketbeach.bsp.DirShelfSource
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.Keeping
import com.phenomemental.pocketbeach.bsp.Shelf
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime

/** 0.8's window and turns on the hatched scratch pocket, with no model: to read what she is given. */
object EightDry {
    @JvmStatic
    fun main(args: Array<String>) {
        val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "shelf")))
        for ((h, m, films) in listOf(Triple(0, 12, false), Triple(8, 40, true))) {
            val now = ZonedDateTime.of(2026, 11, 1, h, m, 0, 0, ZoneId.of("Europe/London"))
            val (storage, pages) = scratchPocket(shelf, now.toInstant().toEpochMilli(), films = films, hers = false, walk = false)
            HerWindow.ensure(pages)
            hatched(storage, pages, now.toInstant().toEpochMilli())
            val window = HerWindow.window(Hatch.MATTHEW_HANDLE, pages, storage, shelf, now)
            println("== window at $h:$m (${window.length} chars)\n$window\n")
            println("== his shell\n" + storage.read("shell:${Hatch.MATTHEW_HANDLE}") + "\n")
            val before = mutableListOf<String>()
            for (said in NOV1 + listOf("I'd like to call you Robin")) {
                val way = Keeping.way(said, before, pages, Hatch.shellOf(Hatch.MATTHEW_HANDLE), Hatch.HER)
                println("== $said\n   the gate: ${way::class.simpleName}\n   ${HerWindow.appLine8(way, said)}")
                before += said
            }
            println()
        }
    }
}
