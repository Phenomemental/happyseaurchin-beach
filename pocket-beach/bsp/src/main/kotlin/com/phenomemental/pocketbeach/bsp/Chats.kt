package com.phenomemental.pocketbeach.bsp

import java.io.File

/**
 * Sundial 0.12, the plain chat (spine:pocket-beach 1.111178719993): files
 * added to his message and read into the conversation as text, his
 * conversations kept inside the app and listed, and a conversation or a
 * reply shared out as text. Plain Kotlin; the app keeps the files.
 */
object Chats {
    /**
     * A file added to one of his messages: its name as he sees it, its text
     * as she reads it, and whether it is a block. A block is never handed her
     * as JSON (spine:pocket-beach 1.111178719993, revised 22 November): it is
     * readable text, its root line and parts indented; [whole] is false when it
     * was too big for her window, so she has its root and each part's opening
     * line, and the chip says so. Sundial 0.13.3: [byApp], a block his words
     * named, read by the app itself before she answers (its disc at pscale 0),
     * beside his words as the paperclip's are; its chip says Sundial read it.
     */
    data class Attached(val name: String, val text: String, val block: Boolean, val whole: Boolean = true, val byApp: Boolean = false, val view: Boolean = false)

    /** One exchange: his words, the files with them, her reply, and when he sent it. */
    data class Said(val his: String, val files: List<Attached>, val hers: String, val atMs: Long)

    /** A conversation as kept: when it began, and its exchanges in order. */
    data class Kept(val startedMs: Long, val exchanges: List<Said>) {
        /** Its first words of his, for the list of older conversations. */
        val opening: String get() = exchanges.firstOrNull()?.his?.trim()?.let { if (it.length <= 80) it else it.take(79).trimEnd() + "…" } ?: ""
    }

    /** The most of one file read into the conversation, in characters: her window is 8,192 tokens. */
    const val MOST_TEXT = 12_000

    /** The text files of his folder the paperclip offers: plain text, never hidden, never Sundial's own housekeeping. */
    val TEXT_KINDS = setOf("txt", "md", "markdown", "text")

    /** His turn with the files he added, each read in as text beneath his words, the time first as part 1 gives it. */
    fun withFiles(said: String, files: List<Attached>): String = buildString {
        append(said.trim())
        for (f in files) {
            append("\n\n(")
            append(
                when {
                    f.view -> "What he is looking at: the block ${f.name}, then its shape, worked out by the app:"
                    f.byApp -> "His block ${f.name}, which his words named, read by the app: its opening line, then each part's opening line beneath it:"
                    f.block && f.whole -> "His block ${f.name}: its opening line, then each part beneath it:"
                    f.block -> "His block ${f.name}, too big to read whole here: its opening line, then each part's opening line beneath it:"
                    else -> "His file ${f.name}:"
                },
            )
            append(")\n")
            append(clip(f.text))
        }
    }

    /**
     * His block [name] as she reads it, never JSON: whole, in pscale structure,
     * when it fits her window ([most] characters, at most [Pages.outline]'s
     * lines); otherwise its root and each part's opening line, the disc at
     * pscale 0, marked as not whole. Null when there is no such block.
     */
    fun blockFile(pages: Pages, region: Region, name: String, most: Int = MOST_TEXT): Attached? {
        val lines = pages.outline(region, name) ?: return null
        val whole = outlineAsText(lines)
        if (whole.length <= most && lines.size < OUTLINE_MOST) return Attached(name, whole, block = true)
        return Attached(name, discText(pages, region, name) ?: return null, block = true, whole = false)
    }

    /**
     * Sundial 0.13.3 (spine:pocket-beach 1.1111787199994, revised 23
     * November): a block his words named, read by the app before she answers:
     * its root and each part's opening line, the disc at pscale 0. Null when
     * there is no such block.
     */
    fun readByApp(pages: Pages, region: Region, name: String): Attached? =
        discText(pages, region, name)?.let { Attached(name, it, block = true, whole = false, byApp = true) }

    /**
     * Sundial 0.13.5, talk about this: what he is looking at ([view], as his
     * screen shows the block [name]) with its shape worked out by the app
     * (BlockShape), one file beside his next words.
     */
    fun lookingAt(name: String, view: String, block: Js): Attached =
        Attached(name, view.trim() + "\n\n" + BlockShape.of(name, block), block = false, view = true)

    /** The most lines of a block read whole; a block with more is given as its opening lines. */
    const val OUTLINE_MOST = 600

    /** A block's root and each part's opening line, the disc at pscale 0, as readable text: how many lines stand beneath each part said beside it. */
    fun discText(pages: Pages, region: Region, name: String): String? {
        val top = pages.position(region, name) ?: return null
        return buildString {
            append(top.text ?: "(no opening line)")
            for (l in top.lines) {
                append("\n  ${l.address}  ${l.text ?: "(nothing written here)"}")
                if (l.beneath > 0) append("  (${l.beneath} beneath)")
            }
        }
    }

    /**
     * A file chosen from elsewhere, as she reads it: a pscale block (a JSON
     * object of an underscore and digits) is turned into readable text as his
     * own blocks are, never handed her as JSON; anything else is its text.
     */
    fun picked(name: String, text: String, shelf: Shelf): Attached {
        val block = runCatching { parseJson(text) }.getOrNull() as? JsObj
        if (block != null && block.keys().isNotEmpty() && block.keys().all { it == "_" || it.length == 1 && it[0] in '1'..'9' }) {
            val held = MemoryStorage()
            val title = name.substringBeforeLast(".json").ifBlank { "block" }
            held.write(title, stringify(block)!!)
            val read = runCatching { blockFile(Pages(held, shelf, PocketBsp(held, shelf)), Region.POCKET, title) }.getOrNull()
            if (read != null) return read
        }
        return Attached(name, text, block = false)
    }

    /** A file's text, cut to [MOST_TEXT] with a plain mark where it was cut. */
    fun clip(text: String): String = if (text.length <= MOST_TEXT) text.trimEnd() else text.take(MOST_TEXT).trimEnd() + "\n… (cut here: the file runs on)"

    /** The text files in [dir], newest first: plain text only, no hidden file and no folder. */
    fun textFiles(dir: File): List<File> =
        (dir.listFiles() ?: emptyArray()).filter { it.isFile && !it.name.startsWith(".") && it.extension.lowercase() in TEXT_KINDS }
            .sortedByDescending { it.lastModified() }

    /** A conversation as text, to share out or save: his words and hers, a line between each. */
    fun asText(kept: Kept, his: String = "Matthew", hers: String = "Gemma"): String = kept.exchanges.joinToString("\n\n") { s ->
        buildString {
            append("$his: ${s.his.trim()}")
            if (s.files.isNotEmpty()) append("\n(with ${s.files.joinToString(", ") { it.name }})")
            if (s.hers.isNotBlank()) append("\n\n$hers: ${s.hers.trim()}")
        }
    }

    // ── kept as JSON in the app's own files ──

    fun toJson(kept: Kept): String = stringify(JsObj.of(
        "started" to JsNum(kept.startedMs.toDouble()),
        "exchanges" to JsArr(kept.exchanges.map { s ->
            JsObj.of(
                "his" to str(s.his), "hers" to str(s.hers), "at" to JsNum(s.atMs.toDouble()),
                "files" to JsArr(s.files.map { f -> JsObj.of("name" to str(f.name), "text" to str(f.text), "block" to JsBool(f.block), "whole" to JsBool(f.whole)) as Js? }.toMutableList()),
            ) as Js?
        }.toMutableList()),
    ))!!

    fun fromJson(text: String): Kept? = runCatching {
        val o = parseJson(text) as JsObj
        Kept(
            (o["started"] as JsNum).value.toLong(),
            (o["exchanges"] as JsArr).items.map { e ->
                val x = e as JsObj
                Said(
                    (x["his"] as JsStr).value, ((x["files"] as? JsArr)?.items ?: emptyList()).map { f ->
                        val g = f as JsObj
                        Attached((g["name"] as JsStr).value, (g["text"] as JsStr).value, (g["block"] as? JsBool)?.value ?: false, (g["whole"] as? JsBool)?.value ?: true)
                    },
                    (x["hers"] as? JsStr)?.value ?: "", (x["at"] as JsNum).value.toLong(),
                )
            },
        )
    }.getOrNull()

    /** The file a conversation begun at [startedMs] is kept in, inside [dir]. */
    fun fileOf(dir: File, startedMs: Long): File = File(dir, "talk-$startedMs.json")

    /** Every kept conversation in [dir], newest first; a file that can't be read is passed over. */
    fun all(dir: File): List<Kept> = (dir.listFiles() ?: emptyArray()).filter { it.isFile && it.name.startsWith("talk-") && it.name.endsWith(".json") }
        .mapNotNull { runCatching { fromJson(it.readText()) }.getOrNull() }.filter { it.exchanges.isNotEmpty() }.sortedByDescending { it.startedMs }

    /** Keep [kept] in [dir], written whole through a temporary file. */
    fun keep(dir: File, kept: Kept) {
        dir.mkdirs()
        val file = fileOf(dir, kept.startedMs)
        val tmp = File(dir, file.name + ".tmp")
        tmp.writeText(toJson(kept))
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }
}
