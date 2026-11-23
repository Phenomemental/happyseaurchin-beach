package com.phenomemental.pocketbeach.trial

import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.MemoryStorage
import com.phenomemental.pocketbeach.bsp.Pages
import com.phenomemental.pocketbeach.bsp.isoString
import com.phenomemental.pocketbeach.bsp.str

/*
 * The bench's hatched pocket, word for word as Eight.kt made it for the
 * trials after it (Nine, Sand12). Eight.kt is left out of the build since
 * Sundial 0.12's lean (trial/build.gradle.kts) and builds at its own commit.
 */

internal val H = Hatch.MATTHEW_HANDLE

/** Whether the model on the bench is E4B: a ring goes to her; else to him, as the app does. */
internal var BIG = true

/** His passport as the bench adopts it: a stand-in. */
private val PASSPORT = JsObj.of(
    "_" to str("Phenomemental — Matthew, as his passport on the beach has it (a stand-in for the bench)."),
    "1" to str("His heart, in his own words, on the beach."),
    "2" to str("What he needs, in his own words, on the beach."),
)

/** A pocket hatched as 0.8 hatches it: his passport adopted. */
internal fun hatched(storage: MemoryStorage, pages: Pages, nowMs: Long, passport: JsObj = PASSPORT) {
    val planned = Hatch.plan(H, passport, pages, storage, nowMs, "Adopted on this phone from passport-$H.json, read at ${isoString(nowMs)}.")
    val done = Hatch.carryOut(planned, pages, storage)
    check(done.problem == null) { "the bench's pocket couldn't be hatched: ${done.problem}" }
}
