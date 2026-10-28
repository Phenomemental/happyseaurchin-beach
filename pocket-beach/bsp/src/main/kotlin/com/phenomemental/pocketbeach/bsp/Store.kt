package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Where the pocket's blocks live: one JSON file per block, named
 * regions/pocket/<block-name>.json under the app's private files (the name
 * percent-encoded as encodeURIComponent writes it, so any name is a safe file
 * name). Beside the folder, one small record keeps what the beach keeps beside
 * its blocks: when each was last touched and when it was born.
 *
 * Every write lands whole or not at all: it goes to a temporary file in the
 * same folder, then is renamed over the old one.
 */
class BlockStore(root: File) {
    private val dir = File(root, "regions/pocket")
    private val metaFile = File(root, "regions/pocket.meta.json")

    init {
        dir.mkdirs()
    }

    private fun fileOf(name: String) = File(dir, encodeURIComponent(name) + ".json")

    /** The stored JSON text of a block, or null when there is none. */
    fun read(name: String): String? = fileOf(name).takeIf { it.isFile }?.readText(Charsets.UTF_8)

    fun write(name: String, json: String) = atomicWrite(fileOf(name), json)

    /** The block names present, sorted as JavaScript sorts strings. */
    fun names(): List<String> = (dir.listFiles() ?: emptyArray())
        .filter { it.isFile && it.name.endsWith(".json") }
        .map { decodeURIComponent(it.name.removeSuffix(".json")) }
        .sorted()

    /** The stored size in bytes, as the beach weighs a block. */
    fun bytes(name: String): Long = fileOf(name).takeIf { it.isFile }?.length() ?: 0

    fun touched(): JsObj = meta()["touched"] as? JsObj ?: JsObj()
    fun born(): JsObj = meta()["born"] as? JsObj ?: JsObj()

    fun stamp(name: String, iso: String, born: Boolean) {
        val m = meta()
        val t = m["touched"] as? JsObj ?: JsObj()
        t[name] = str(iso)
        m["touched"] = t
        if (born) {
            val b = m["born"] as? JsObj ?: JsObj()
            b[name] = str(iso)
            m["born"] = b
        }
        atomicWrite(metaFile, stringify(m)!!)
    }

    /** Record a birth on its own, as a restored record carries it. */
    fun stampBorn(name: String, iso: String) {
        val m = meta()
        val b = m["born"] as? JsObj ?: JsObj()
        b[name] = str(iso)
        m["born"] = b
        atomicWrite(metaFile, stringify(m)!!)
    }

    private fun meta(): JsObj =
        metaFile.takeIf { it.isFile }?.let { runCatching { parseJson(it.readText(Charsets.UTF_8)) as? JsObj }.getOrNull() } ?: JsObj()

    private fun atomicWrite(target: File, text: String) {
        target.parentFile.mkdirs()
        val tmp = File.createTempFile(".write-", ".tmp", target.parentFile)
        try {
            tmp.writeText(text, Charsets.UTF_8)
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            tmp.delete()
        }
    }
}

private const val URI_SAFE = "-_.!~*'()"

/** encodeURIComponent */
fun encodeURIComponent(s: String): String {
    val out = StringBuilder()
    for (b in s.toByteArray(Charsets.UTF_8)) {
        val c = b.toInt() and 0xff
        val ch = c.toChar()
        if (c < 0x80 && (ch.isLetterOrDigit() || ch in URI_SAFE)) out.append(ch) else out.append('%').append(c.toString(16).uppercase().padStart(2, '0'))
    }
    return out.toString()
}

/** decodeURIComponent, for names this store encoded. */
fun decodeURIComponent(s: String): String {
    val bytes = java.io.ByteArrayOutputStream()
    var i = 0
    while (i < s.length) {
        if (s[i] == '%' && i + 2 < s.length) {
            bytes.write(s.substring(i + 1, i + 3).toInt(16)); i += 3
        } else {
            bytes.write(s.substring(i, i + 1).toByteArray(Charsets.UTF_8)); i++
        }
    }
    return bytes.toString(Charsets.UTF_8.name())
}
