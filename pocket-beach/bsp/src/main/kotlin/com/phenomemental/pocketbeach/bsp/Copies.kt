package com.phenomemental.pocketbeach.bsp

/**
 * A copy of the pocket: every block, with when each was touched and born, in
 * one JSON file that Matthew keeps wherever he chooses ("Save a copy") and can
 * bring back ("Restore"). It is the pocket's only backup (decision 3).
 *
 * The blocks are carried as JSON, not as text, so the file reads plainly; a
 * restored block is stored exactly as it was saved, byte for byte.
 */
object Copies {
    const val FORMAT = "pocket-beach copy"
    const val VERSION = 1

    /** A reminder to save is due this long after the last copy, when something has changed since. */
    const val REMIND_AFTER_MS = 3L * 24 * 60 * 60 * 1000

    class Copy(val saved: String, val blocks: Map<String, String>, val stamps: Stamps)

    /** A file that is not a copy the pocket can restore, and why, in plain words. */
    class Unreadable(message: String) : Exception(message)

    fun make(storage: BlockStorage, nowMs: Long): String {
        val blocks = JsObj()
        for (name in storage.names().distinct().sorted()) {
            val block = storage.read(name)?.let(::parseJson)?.takeUnless { it == JsNull } ?: continue
            blocks[name] = block
        }
        val stamps = storage.stamps()
        fun obj(m: Map<String, String>) = JsObj().also { o -> m.filterKeys { blocks.has(it) }.forEach { (k, v) -> o[k] = str(v) } }
        return stringifyPretty(
            JsObj.of(
                "format" to str(FORMAT),
                "version" to JsNum(VERSION.toDouble()),
                "saved" to str(isoString(nowMs)),
                "blocks" to blocks,
                "touched" to obj(stamps.touched),
                "born" to obj(stamps.born),
            ),
        ) + "\n"
    }

    fun read(text: String): Copy {
        val doc = try { parseJson(text) } catch (_: JsonSyntaxError) { throw Unreadable("This file isn't a copy saved by Pocket beach: it isn't readable JSON.") }
        if (doc !is JsObj || doc["format"] != str(FORMAT)) throw Unreadable("This file isn't a copy saved by Pocket beach.")
        val version = (doc["version"] as? JsNum)?.value
        if (version == null || version > VERSION) throw Unreadable("This copy was saved by a newer Pocket beach. Update the app, then restore it.")
        val saved = (doc["saved"] as? JsStr)?.value?.takeIf { parseIsoMs(it) != null } ?: throw Unreadable("This copy is damaged: it doesn't say when it was saved.")
        val blocks = doc["blocks"] as? JsObj ?: throw Unreadable("This copy is damaged: it holds no blocks.")
        val out = LinkedHashMap<String, String>()
        for (name in blocks.keys()) {
            val block = blocks[name]
            when {
                name.isBlank() -> throw Unreadable("This copy is damaged: a block in it has no name.")
                name.startsWith("sed:") || name.startsWith("grain:") -> throw Unreadable("This copy holds \"$name\", which belongs to the beach, not the pocket.")
                block !is JsObj || !hasFloor(block) -> throw Unreadable("This copy is damaged: \"$name\" isn't a block.")
            }
            validateShape(block)?.let { throw Unreadable("This copy is damaged: \"$name\" has an $it") }
            out[name] = stringify(block)!!
        }
        fun map(key: String): Map<String, String> {
            val o = doc[key] as? JsObj ?: return emptyMap()
            return o.keys().filter { it in out && o[it] is JsStr }.associateWith { jsString(o[it]) }
        }
        return Copy(saved, out, Stamps(map("touched"), map("born")))
    }

    /** Put a copy in place of every block on the phone, whole or not at all. */
    fun restore(storage: BlockStorage, copy: Copy) = storage.replaceAll(copy.blocks, copy.stamps)

    /** Whether any block was written after [lastSavedMs] (with no copy ever saved: whether there is any block at all). */
    fun changedSince(lastSavedMs: Long?, stamps: Stamps): Boolean {
        val touched = stamps.touched.values.mapNotNull(::parseIsoMs)
        if (lastSavedMs == null) return touched.isNotEmpty() || stamps.born.isNotEmpty()
        return touched.any { it > lastSavedMs }
    }

    /**
     * Whether to remind Matthew to save a copy: three days after the last one,
     * if anything has changed since; with none saved yet, three days after the
     * first block was made.
     */
    fun reminderDue(lastSavedMs: Long?, stamps: Stamps, nowMs: Long): Boolean {
        if (!changedSince(lastSavedMs, stamps)) return false
        val since = lastSavedMs ?: stamps.born.values.mapNotNull(::parseIsoMs).minOrNull() ?: return false
        return nowMs - since >= REMIND_AFTER_MS
    }
}
