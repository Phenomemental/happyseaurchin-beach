package com.phenomemental.pocketbeach.bsp

/*
 * Shells.kt — putting a shell on, offline: the part of bsp-mcp-server's play
 * door (src/tools/play.ts, at the pinned commit) that compiles a hand's own
 * context, run over the pocket's blocks.
 *
 * The shell's opening line comes first. Then its own context as the door lays
 * it out: when the shell carries a manifest — position 3 by convention, else
 * the first digit position that reads as a bundle — the manifest is the law:
 * each member it dials is unfolded at its own aperture, the legacy whole-block
 * dumps stand down (the passport aside), and the wearer is seated, so later
 * replies carry what moved. A shell with no manifest is laid out whole, beside
 * whatever passport, witnessed, knows, history and stash blocks stand for its
 * hand.
 *
 * What the door gathers from a live beach — the gate and genesis for a fresh
 * arrival, the room's live scene, who is here — has nothing to come from on
 * the phone, so each is one plain line saying it is left out offline.
 */

/**
 * A shell put on: its [opening] line (null when it has none), the [window] to
 * stand in a model's instructions, and the [seat] its manifest declared — null
 * for a shell with no manifest, or none that resolved.
 */
class Worn(val shell: String, val opening: String?, val window: String, val seat: Seat? = null)

class Shells(private val storage: BlockStorage, private val shelf: Shelf) {

    private fun pocketBlock(name: String): Js? =
        try { storage.read(name)?.let(::parseJson) } catch (_: JsonSyntaxError) { null }?.takeIf { truthy(it) && isObject(it) }

    /** A loader over the pocket as a bundle sees it: the shelf's names first, then the pocket's blocks. */
    fun loader(): (String) -> Js? = surfaceLoader(shelf, ::pocketBlock)

    /** The rider's two reads over the pocket: a fresh loader, and when each block last changed. */
    val io: RiderIO = object : RiderIO {
        override fun load(origin: String): (String) -> Js? = loader()
        override fun touched(origin: String): Map<String, String> = storage.stamps().touched
    }

    /** Put on the shell named [shell] at [nowMs]. */
    fun wear(shell: String, nowMs: Long): Worn {
        val block = pocketBlock(shell)
            ?: return Worn(shell, null, "(No block named \"$shell\" stands in the pocket, so there is no shell to put on.)")
        val opening = collectUnderscore(block)
        // A bare name in the manifest is tried as <name>:<hand> first; a shell named for a hand (shell:weft) is of that hand.
        val hand = handleOfBlock(shell) ?: shell

        val legacy = mutableListOf<Pair<String, String>>()
        for (b in listOf("passport", "witnessed", "knows", "shell", "history", "stash")) {
            val name = if (b == "shell") shell else "$b:$hand"
            val row = if (b == "shell") block else pocketBlock(name)
            if (row != null) legacy += name to stringifyPretty(row, " ")
        }

        val manifestSlot = pickBundleSlot(block)
        val manifestNode = manifestSlot?.let { jsGet(block, it) }
        val nominates = manifestNode != null && looksLikeBundle(manifestNode)
        val own = mutableListOf<Pair<String, String>>()
        val seen = HashSet<String>()
        for (entry in legacy) {
            if (nominates && !entry.first.startsWith("passport:")) continue
            own += entry
            seen += entry.first
        }
        var seat: Seat? = null
        if (nominates && manifestSlot != null) {
            try {
                val members = compileBundle(manifestNode, BundleContext(loader(), hand, nowMs))
                if (members != null) {
                    for (m in members) {
                        if (m.ref == null || m.leaf.isNullOrEmpty() || !seen.add(m.leaf)) continue
                        own += m.leaf to renderMemberBody(m)
                    }
                    seat = declareSeat(POCKET, hand, shell, manifestSlot, manifestNode, nowMs)
                }
            } catch (e: Exception) {
                own += "$shell $manifestSlot (manifest)" to
                    stringify(str("did not compile: ${jsSlice(e.message ?: e.toString(), 120)} — read its addresses directly via bsp"))!!
            }
        }

        val out = mutableListOf<String>()
        if (opening != null) out += opening
        out += ""
        out += "(Left out offline: the gate, where a new arrival meets others before a character exists. The pocket is no world to arrive at.)"
        out += "(Left out offline: genesis, the passage that makes a character. There is no world here to make one in.)"
        out += "(Left out offline: the room's live scene. The pocket has no room.)"
        out += "(Left out offline: who is here. No one else is present on this phone.)"
        out += ""
        if (own.isNotEmpty()) {
            out += "═══════════ YOUR OWN CONTEXT ($hand) ═══════════"
            for ((name, json) in own) out += "── $name ──\n$json"
        } else {
            out += "(No blocks for $hand on this world yet — you are fresh here.)"
        }
        return Worn(shell, opening, out.joinToString("\n"), seat)
    }

    companion object {
        /** The surface a pocket seat stands at. */
        const val POCKET = "pocket"

        /** Where a shell keeps its bundle: position 3 by convention, else the first digit position that reads as a bundle by form. */
        fun pickBundleSlot(shell: Js): String? {
            if (looksLikeBundle(jsGet(shell, "3"))) return "3"
            for (d in "123456789") if (looksLikeBundle(jsGet(shell, d.toString()))) return d.toString()
            return null
        }
    }
}
