package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Putting a shell on, offline. The bundle it compiles is the one the unit and
 * golden tests hold to the original; these check the door's own layout around
 * it: the opening line, the left-out lines, the manifest law and the seat.
 */
class ShellsTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val storage = MemoryStorage()
    private var clock = 1_793_178_000_000L
    private val pocket = PocketBsp(storage, shelf) { clock }
    private val shells = Shells(storage, shelf)

    private fun call(json: String): String = pocket.call(json).also { clock += 1000 }

    private val opening = "Pocket companion. I live on this phone, read freely, and write only when Matthew says yes."

    private val leftOut = listOf(
        "(Left out offline: the gate, where a new arrival meets others before a character exists. The pocket is no world to arrive at.)",
        "(Left out offline: genesis, the passage that makes a character. There is no world here to make one in.)",
        "(Left out offline: the room's live scene. The pocket has no room.)",
        "(Left out offline: who is here. No one else is present on this phone.)",
    )

    @Test
    fun aShellWithNoManifestIsLaidOutWhole() {
        call("""{"agent_id":"pocket","block":"pocket-companion","content":{"_":"$opening"}}""")
        val worn = shells.wear("pocket-companion", clock)
        assertEquals(opening, worn.opening)
        assertNull(worn.seat, "no manifest, no seat")
        val want = listOf(opening, "") + leftOut + listOf(
            "",
            "═══════════ YOUR OWN CONTEXT (pocket-companion) ═══════════",
            "── pocket-companion ──",
            "{",
            " \"_\": \"$opening\"",
            "}",
        )
        assertEquals(want.joinToString("\n"), worn.window)
    }

    @Test
    fun theHandsOwnBlocksRideBesideAShellWithNoManifest() {
        call("""{"agent_id":"pocket","block":"pocket-companion","content":{"_":"$opening","1":"a line of the shell"}}""")
        call("""{"agent_id":"pocket","block":"passport:pocket-companion","content":{"_":"who the companion is"}}""")
        call("""{"agent_id":"pocket","block":"stash:pocket-companion","content":{"_":"what it keeps by"}}""")
        val window = shells.wear("pocket-companion", clock).window
        val order = listOf("── passport:pocket-companion ──", "── pocket-companion ──", "── stash:pocket-companion ──").map { window.indexOf(it) }
        assertTrue(order.all { it > 0 } && order == order.sorted(), "passport, shell, stash, in the door's order:\n$window")
        assertTrue("\"1\": \"a line of the shell\"" in window)
    }

    @Test
    fun aManifestIsTheLawAndSeatsTheWearer() {
        call("""{"agent_id":"pocket","block":"garden","content":{"_":"Garden.","1":{"_":"Spring","1":"peas"},"2":"Summer"}}""")
        call("""{"agent_id":"pocket","block":"notes:pocket-companion","content":{"_":"The companion's notes","1":"first note"}}""")
        call("""{"agent_id":"pocket","block":"stash:pocket-companion","content":{"_":"stood down beneath a manifest"}}""")
        call("""{"agent_id":"pocket","block":"passport:pocket-companion","content":{"_":"rides whatever the manifest says"}}""")
        call(
            """{"agent_id":"pocket","block":"pocket-companion","content":{"_":"$opening",""" +
                """"3":{"_":"what the companion holds","1":"notes","2":"garden:1:-1","3":"whetstone:2","4":"missing","5":"a line of prose"}}}""",
        )
        val worn = shells.wear("pocket-companion", clock)
        val w = worn.window
        assertTrue(w.startsWith("$opening\n\n" + leftOut.joinToString("\n")), w)
        assertTrue("── passport:pocket-companion ──" in w, "the passport rides beside a manifest")
        assertTrue("stood down beneath a manifest" !in w && "── pocket-companion ──" !in w, "legacy dumps stand down:\n$w")
        assertTrue("── notes ──\n[whole block]" in w && "first note" in w, "a bare name is the hand's own block first:\n$w")
        assertTrue("── garden:1:-1 ──\n[path-walk+descent @ \"1\" pscale -1]" in w)
        assertTrue("── whetstone:2 ──\n[path-walk @ \"2\"]" in w, "the shelf answers a reference to a teaching block")
        assertTrue("── missing ──\n(absent at this surface: missing:pocket-companion)" in w)
        assertTrue("── a line of prose ──" !in w, "prose is in the manifest's index only, as the door renders members")

        val seat = assertNotNull(worn.seat)
        assertEquals("pocket-companion" to "3", seat.block to seat.spindle)
        // Nothing moved yet; then a note lands and rides the next reply, once.
        clock += 6000
        assertEquals("reply", seat.ride("reply", clock, shells.io))
        // A touch counts when it lands after the seat was last told, so a second later.
        clock += 1000
        call("""{"agent_id":"pocket","block":"notes:pocket-companion","content":"second note","append":true}""")
        clock += 6000
        val rode = seat.ride("reply", clock, shells.io)
        assertTrue(rode.startsWith("reply\n[orientation — pocket-companion: what moved since "), rode)
        assertTrue("── 1 · notes ──" in rode && "second note" in rode && "(unchanged: garden:1:-1 · whetstone:2 · missing)" in rode, rode)
        clock += 6000
        assertEquals("reply", seat.ride("reply", clock, shells.io))
    }

    @Test
    fun theManifestIsFoundByItsForm() {
        val notBundle = JsObj.of("1" to str("a line"), "2" to str("another line"))
        val bundle = JsObj.of("1" to str("garden"), "2" to str("notes:1"))
        assertEquals("3", Shells.pickBundleSlot(JsObj.of("1" to bundle, "3" to bundle)))
        assertEquals("1", Shells.pickBundleSlot(JsObj.of("1" to bundle, "3" to notBundle)))
        assertEquals("9", Shells.pickBundleSlot(JsObj.of("3" to notBundle, "9" to bundle)))
        assertNull(Shells.pickBundleSlot(JsObj.of("3" to notBundle)))
    }

    @Test
    fun aShellThatIsNotThereSaysSo() {
        val worn = shells.wear("pocket-companion", clock)
        assertNull(worn.opening)
        assertNull(worn.seat)
        assertEquals("(No block named \"pocket-companion\" stands in the pocket, so there is no shell to put on.)", worn.window)
    }
}
