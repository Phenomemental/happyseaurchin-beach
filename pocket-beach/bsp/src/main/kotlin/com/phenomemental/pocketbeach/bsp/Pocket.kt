package com.phenomemental.pocketbeach.bsp

import java.io.File

/**
 * The pocket's bsp(): the one entry point the app calls.
 *
 * Two regions answer. "pocket" (or no agent_id at all) is Matthew's own
 * blocks on this phone; "pscale" is the teaching shelf, read-only. Anything
 * else would need the beach, which this version cannot reach. Locks and
 * encryption are refused rather than ignored: the phone is the latch.
 *
 * Calls run one at a time, so an append's slot can never be taken twice.
 */
class PocketBsp(root: File, shelf: Shelf, private val clock: () -> Long = System::currentTimeMillis) {
    private val router = Router(Beach(BlockStore(root)), shelf)

    fun call(json: String): String {
        val params = try {
            parseJson(json)
        } catch (e: JsonSyntaxError) {
            return "The call is not readable JSON: ${e.message}"
        }
        if (params !is JsObj) return "The call must be a JSON object of bsp() parameters."
        return call(params)
    }

    fun call(params: JsObj): String = call(params, clock())

    fun call(params: JsObj, nowMs: Long): String = synchronized(this) {
        val call = when (val checked = check(params)) {
            is Checked.Refused -> return checked.text
            is Checked.Ok -> checked.call
        }
        try {
            router.handle(call, nowMs)
        } catch (e: Exception) {
            "The call failed: ${e.message ?: e::class.simpleName}"
        }
    }

    private sealed class Checked {
        class Ok(val call: Call) : Checked()
        class Refused(val text: String) : Checked()
    }

    private fun check(p: JsObj): Checked {
        val agent = when (val a = p["agent_id"]) {
            null -> "pocket"
            is JsStr -> a.value
            else -> return Checked.Refused("agent_id must be text: \"pocket\" for your own blocks, \"pscale\" for the teaching blocks.")
        }
        if (agent != "pocket" && agent != "pscale") return Checked.Refused(BEACH_OUT_OF_REACH)

        val latched = LATCH_PARAMS.filter { p.has(it) }
        if (latched.isNotEmpty()) {
            return Checked.Refused(
                "The pocket keeps no locks and no encryption in this version — the phone itself is the latch. " +
                    "Leave out ${latched.joinToString(", ")} and call again.",
            )
        }

        val block = when (val b = p["block"]) {
            null -> null
            is JsStr -> b.value
            else -> return Checked.Refused("block must be text: the name of a block, or left out to list the blocks.")
        }
        if (agent == "pocket" && block != null && (block.startsWith("sed:") || block.startsWith("grain:"))) {
            return Checked.Refused(BEACH_OUT_OF_REACH)
        }
        val spindle = when (val s = p["spindle"]) {
            null, JsNull -> null
            is JsStr -> s.value
            else -> return Checked.Refused("spindle must be text, an address such as \"1.2\", or left out.")
        }
        val pscale = when (val n = p["pscale_attention"]) {
            null, JsNull -> null
            is JsNum -> if (n.value == kotlin.math.floor(n.value) && kotlin.math.abs(n.value) < 1e9) n.value.toInt()
            else return Checked.Refused("pscale_attention must be a whole number, or left out.")
            else -> return Checked.Refused("pscale_attention must be a whole number, or left out.")
        }
        val append = when (val a = p["append"]) {
            null -> false
            is JsBool -> a.value
            else -> return Checked.Refused("append must be true or false.")
        }
        return Checked.Ok(Call(agent, block, spindle, pscale, if (p.has("content")) p["content"] ?: JsNull else null, append))
    }

    companion object {
        const val BEACH_OUT_OF_REACH = "The pocket can't reach the beach in this version."
        val LATCH_PARAMS = listOf("secret", "new_lock", "gray", "enc_secret", "members")

        /** A pocket whose shelf is read from a folder holding index.json and the blocks. */
        fun withShelfDir(root: File, shelfDir: File, clock: () -> Long = System::currentTimeMillis): PocketBsp =
            PocketBsp(root, Shelf(File(shelfDir, "index.json").readText()) { f -> File(shelfDir, f).takeIf { it.isFile }?.readText() }, clock)
    }
}
