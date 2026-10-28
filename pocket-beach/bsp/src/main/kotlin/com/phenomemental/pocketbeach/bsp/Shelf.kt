package com.phenomemental.pocketbeach.bsp

/**
 * The teaching shelf: bsp-mcp-server's bundled sentinel blocks, read-only, as
 * agent_id "pscale". [index] is the shelf's index.json — each name with the
 * file that holds it (protocol-paywall is payway under its old name) — and
 * [open] reads a file by name.
 */
class Shelf(index: String, private val open: (String) -> String?) {
    private val files: Map<String, String> = (parseJson(index) as JsArr).items.associate { e ->
        jsString(jsGet(e, "name")) to jsString(jsGet(e, "file"))
    }
    private val cache = HashMap<String, Js>()

    fun names(): List<String> = files.keys.sorted()

    fun load(name: String): Js? = synchronized(cache) {
        cache[name] ?: files[name]?.let(open)?.let(::parseJson)?.also { cache[name] = it }
    }
}
