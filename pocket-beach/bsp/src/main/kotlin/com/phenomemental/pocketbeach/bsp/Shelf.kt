package com.phenomemental.pocketbeach.bsp

import java.io.File

/** Where the teaching shelf's files come from: the app's assets on Android, a folder on a laptop. */
fun interface ShelfSource {
    /** The text of one shelf file, or null when there is none. */
    fun read(file: String): String?
}

/** A shelf kept in a folder. */
class DirShelfSource(private val dir: File) : ShelfSource {
    override fun read(file: String): String? = File(dir, file).takeIf { it.isFile }?.readText(Charsets.UTF_8)
}

/**
 * The teaching shelf: bsp-mcp-server's bundled sentinel blocks, read-only, as
 * agent_id "pscale". Its index.json names each block with the file that holds
 * it (protocol-paywall is payway under its old name).
 */
class Shelf(private val source: ShelfSource) {
    private val files: Map<String, String> =
        (parseJson(source.read("index.json") ?: error("the teaching shelf has no index.json")) as JsArr).items.associate { e ->
            jsString(jsGet(e, "name")) to jsString(jsGet(e, "file"))
        }
    private val cache = HashMap<String, Js>()

    fun names(): List<String> = files.keys.sorted()

    /** The names to show, in the shelf's own order, each block once: a second name for the same file is left out. */
    fun shelved(): List<String> = files.entries.distinctBy { it.value }.map { it.key }

    fun load(name: String): Js? = synchronized(cache) {
        cache[name] ?: files[name]?.let(source::read)?.let(::parseJson)?.also { cache[name] = it }
    }
}
