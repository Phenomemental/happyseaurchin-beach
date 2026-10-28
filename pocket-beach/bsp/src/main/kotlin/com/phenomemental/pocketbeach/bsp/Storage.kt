package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Where the pocket keeps its blocks — the one thing the core asks of the
 * platform it runs on. The core reads and writes each block's stored JSON text
 * whole, lists the names present, and keeps, beside the blocks, what the beach
 * keeps beside its own: when each block was last written and when it was born.
 *
 * Calls arrive one at a time (PocketBsp serialises them), so an implementation
 * needs no locking of its own; it must only make each write land whole or not
 * at all.
 */
interface BlockStorage {
    /** The stored JSON text of a block, or null when there is none. */
    fun read(name: String): String?

    /** Store a block's JSON text, replacing what stood, whole or not at all. */
    fun write(name: String, json: String)

    /** The names of the blocks present, in any order. */
    fun names(): List<String>

    fun stamps(): Stamps

    fun setStamps(stamps: Stamps)

    /**
     * Replace every block and the stamps at once, as a restore does: afterwards
     * the storage holds exactly [blocks] (name to stored JSON text) and
     * [stamps], or, if anything stops it part way, exactly what it held before.
     */
    fun replaceAll(blocks: Map<String, String>, stamps: Stamps)
}

/** When each block was last written (touched) and first written (born): block name to ISO instant. */
data class Stamps(val touched: Map<String, String> = emptyMap(), val born: Map<String, String> = emptyMap())

/**
 * Blocks as files: <root>/regions/pocket/<name>.json, one per block, and the
 * stamps in <root>/regions/pocket.meta.json. Plain JVM files, so the same class
 * serves Android (root = the app's private files) and a Windows or Mac laptop.
 *
 * A write goes to a temporary file in the same folder and is then renamed over
 * the old one. A block name becomes a file name every file system accepts, and
 * two names never share a file (see [fileNameOf]).
 *
 * A replace of everything (a restore) writes the new blocks into a folder of
 * their own, then swaps the folders by renaming. Should the process stop part
 * way, the next start finishes the swap if it had begun and abandons it if it
 * had not, so the blocks are never a mix of the two.
 */
class FileStorage(root: File) : BlockStorage {
    private val regions = File(root, "regions")
    private val dir = File(regions, "pocket")
    private val metaFile = File(regions, "pocket.meta.json")
    private val incoming = File(regions, "pocket.incoming")
    private val incomingMeta = File(regions, "pocket.meta.incoming.json")
    private val outgoing = File(regions, "pocket.outgoing")

    init {
        finishReplace()
        dir.mkdirs()
    }

    private fun fileOf(name: String) = File(dir, fileNameOf(name))

    override fun read(name: String): String? = fileOf(name).takeIf { it.isFile }?.readText(Charsets.UTF_8)

    override fun write(name: String, json: String) = atomicWrite(fileOf(name), json)

    override fun names(): List<String> = (dir.listFiles() ?: emptyArray())
        .filter { it.isFile && it.name.endsWith(".json") }
        .map { blockNameOf(it.name) }

    override fun stamps(): Stamps {
        val meta = metaFile.takeIf { it.isFile }?.let { runCatching { parseJson(it.readText(Charsets.UTF_8)) as? JsObj }.getOrNull() }
            ?: return Stamps()
        fun map(key: String): Map<String, String> {
            val o = meta[key] as? JsObj ?: return emptyMap()
            return o.keys().associateWith { jsString(o[it]) }
        }
        return Stamps(map("touched"), map("born"))
    }

    override fun setStamps(stamps: Stamps) = atomicWrite(metaFile, metaJson(stamps))

    override fun replaceAll(blocks: Map<String, String>, stamps: Stamps) {
        incoming.deleteRecursively()
        incoming.mkdirs()
        for ((name, json) in blocks) File(incoming, fileNameOf(name)).writeText(json, Charsets.UTF_8)
        atomicWrite(incomingMeta, metaJson(stamps))
        // The swap: once the old folder is moved aside, the new one is going in.
        outgoing.deleteRecursively()
        if (dir.exists()) move(dir, outgoing) else outgoing.mkdirs()
        move(incoming, dir)
        finishReplace()
    }

    /** Complete a replace that had begun its swap, or abandon one that had not (see [replaceAll]). */
    private fun finishReplace() {
        if (incoming.exists()) {
            if (!dir.exists() && outgoing.exists()) {
                move(incoming, dir)
            } else {
                incoming.deleteRecursively()
                incomingMeta.delete()
            }
        }
        if (incomingMeta.exists()) move(incomingMeta, metaFile)
        outgoing.deleteRecursively()
    }

    private fun metaJson(stamps: Stamps): String {
        fun obj(m: Map<String, String>) = JsObj().also { o -> m.forEach { (k, v) -> o[k] = str(v) } }
        return stringify(JsObj.of("touched" to obj(stamps.touched), "born" to obj(stamps.born)))!!
    }

    private fun move(from: File, to: File) {
        Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

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

/** Blocks held in memory only, gone when the process ends. For tests, and for any front end that keeps nothing. */
class MemoryStorage : BlockStorage {
    private val blocks = HashMap<String, String>()
    private var stamps = Stamps()

    override fun read(name: String): String? = blocks[name]
    override fun write(name: String, json: String) { blocks[name] = json }
    override fun names(): List<String> = blocks.keys.toList()
    override fun stamps(): Stamps = stamps
    override fun setStamps(stamps: Stamps) { this.stamps = stamps }

    override fun replaceAll(blocks: Map<String, String>, stamps: Stamps) {
        this.blocks.clear()
        this.blocks.putAll(blocks)
        this.stamps = stamps
    }
}

private const val KEPT = "-_.!~'()"
private val RESERVED = Regex("^(con|prn|aux|nul|com[0-9]|lpt[0-9])$")

private fun hex2(c: Int) = c.toString(16).uppercase().padStart(2, '0')

/**
 * The file a block lives in. Lower-case letters, digits and -_.!~'() stand as
 * themselves, so "garden" is garden.json; every other byte of the name's UTF-8
 * is written %XX. Capitals are encoded too, so on a file system that ignores
 * case (Windows, a Mac) "Garden" and "garden" are still two files; and a name
 * Windows keeps for devices (con, nul, com1, …) has its first letter encoded.
 */
fun fileNameOf(blockName: String): String {
    val out = StringBuilder()
    for (b in blockName.toByteArray(Charsets.UTF_8)) {
        val c = b.toInt() and 0xff
        val ch = c.toChar()
        if (ch in 'a'..'z' || ch in '0'..'9' || ch in KEPT) out.append(ch) else out.append('%').append(hex2(c))
    }
    var s = out.toString()
    if (RESERVED.matches(s.substringBefore('.'))) s = "%" + hex2(s[0].code) + s.substring(1)
    return "$s.json"
}

/** The block name a file holds, the inverse of [fileNameOf]. */
fun blockNameOf(fileName: String): String {
    val s = fileName.removeSuffix(".json")
    val bytes = java.io.ByteArrayOutputStream()
    var i = 0
    while (i < s.length) {
        if (s[i] == '%' && i + 3 <= s.length) {
            bytes.write(s.substring(i + 1, i + 3).toInt(16)); i += 3
        } else {
            bytes.write(s.substring(i, i + 1).toByteArray(Charsets.UTF_8)); i++
        }
    }
    return bytes.toString(Charsets.UTF_8.name())
}
