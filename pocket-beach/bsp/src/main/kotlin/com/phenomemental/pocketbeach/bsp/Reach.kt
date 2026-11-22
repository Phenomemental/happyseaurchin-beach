package com.phenomemental.pocketbeach.bsp

import java.net.URLEncoder

/**
 * Sundial 0.12, the walker (spine:pocket-beach 1.111178719995): any block
 * walked as the beach's bsp() walks it, its text printed character for
 * character as the beach prints it (the golden tests hold the port to that);
 * and, once he turns on Reach the beach, the beach's own blocks read over
 * HTTPS, read only, and walked the same way here. Nothing is written to the
 * beach; a block he wants is copied into his pocket on his yes.
 */
object Reach {
    /** The one beach the walker reads from. */
    const val BEACH = PoolPost.BEACH

    private const val SURFACE = "$BEACH/.well-known/pscale-beach"

    /** A read of one block at the beach: its name in the query, colons kept readable, as the beach's own wire writes it. */
    fun blockUrl(name: String): String =
        "$SURFACE?block=" + URLEncoder.encode(name, "UTF-8").replace("+", "%20").replace("%3A", ":", ignoreCase = true)

    /** The beach's index of its named blocks. */
    fun indexUrl(): String = SURFACE

    /** Whether [url] is one the walker may read: the beach's surface, a block of it or its index, and nothing else. */
    fun readable(url: String): Boolean =
        url == SURFACE || (url.startsWith("$SURFACE?block=") && url.indexOf('&') < 0 && url.indexOf('#') < 0)

    /** The names the beach's index lists, in its order; empty when the answer isn't its index. */
    fun names(index: String): List<String> {
        val o = runCatching { parseJson(index) }.getOrNull() as? JsObj ?: return emptyList()
        val list = o["blocks"] as? JsArr ?: return emptyList()
        return list.items.mapNotNull { (it as? JsStr)?.value }
    }

    /** The block the beach answered with, or null when the answer is no block. */
    fun blockOf(answer: String): JsObj? = runCatching { parseJson(answer) }.getOrNull() as? JsObj

    /**
     * Walk [block] (read from the beach, or from anywhere) as bsp() walks it:
     * [spindle] the address walked to, [attention] the pscale attended to;
     * both null, the block's disc at pscale 0. What comes back is bsp's own
     * text, as the beach prints it.
     */
    fun walk(shelf: Shelf, name: String, block: JsObj, spindle: String?, attention: Double?, nowMs: Long = System.currentTimeMillis()): String {
        val held = MemoryStorage()
        held.write(name, stringify(block)!!)
        return PocketBsp(held, shelf) { nowMs }.call(params(name, spindle, attention), nowMs)
    }

    /** Walk the pocket's own block [name] through the pocket's bsp(), as [walk] does a block read from the beach. */
    fun walkPocket(bsp: PocketBsp, name: String, spindle: String?, attention: Double?): String = bsp.call(params(name, spindle, attention))

    /**
     * The bsp() call a walk makes: a read, never a write. With neither a
     * spindle nor a pscale, the block's disc at pscale 0: a walk never pulls a
     * block whole.
     */
    fun params(name: String, spindle: String?, attention: Double?): JsObj = JsObj().also { c ->
        c["agent_id"] = str("pocket")
        c["block"] = str(name)
        val s = spindle?.trim()?.takeIf { it.isNotEmpty() }
        s?.let { c["spindle"] = str(it) }
        (attention ?: if (s == null) 0.0 else null)?.let { c["pscale_attention"] = JsNum(it) }
    }
}

/**
 * Sundial 0.12: the walker's one way to the beach, a GET of [Reach.readable]
 * addresses only, made only while Reach the beach is on. It returns the
 * status and the text the beach answered.
 */
fun interface BeachReader {
    fun get(url: String): WireReply
}
