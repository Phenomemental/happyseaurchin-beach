package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Sundial 0.11 (spine:pocket-beach 1.111178719942): his blocks in a folder
 * he can see, Documents/Sundial on the phone, one readable file per block,
 * directly in the folder, so he, his laptop and Claude can open them.
 *
 * FileStorage was made for a folder nobody else touches; this one is made
 * for a folder a person and a sync app both write into
 * (FINDINGS-local-first.md 2a):
 *
 * - **Readable names.** Only what the common file systems refuse is encoded
 *   ([folderFileNameOf]), so "Garden" is Garden.json, and a file he makes
 *   on the laptop is a block of that name ([folderBlockNameOf]).
 * - **Housekeeping out of sight,** in one hidden folder, [HOUSE]: the
 *   stamps, the temporary files every write goes through (so a sync never
 *   sees a half-written block), and a restore's journal.
 * - **Reads that never stop.** Every file is looked at and none can stop a
 *   listing. A file that isn't a well-formed block is set aside, left as it
 *   is and never written over, and [setAside] says why: so is a second
 *   version a sync app kept beside a block, and a second file for a block
 *   that already has one. Files that don't end in .json, and hidden files
 *   (a sync's own temporary files among them), are never touched.
 * - **A restore file by file,** never by moving the folder: the blocks the
 *   copy holds are written, the blocks it doesn't are removed, and every
 *   other file stays. A journal lets a restore stopped part way finish at
 *   the next start.
 *
 * Calls arrive one at a time, as for every BlockStorage. Nothing here keeps
 * a block's text between calls, so each read is what the folder holds now.
 */
class FolderStorage(val dir: File) : BlockStorage {
    private val house = File(dir, HOUSE)
    private val stampsFile = File(house, "stamps.json")
    private val tmp = File(house, "tmp")
    private val incoming = File(house, "incoming")
    private val incomingStamps = File(house, "incoming-stamps.json")
    private val journal = File(house, "restore.json")

    // ── what the folder holds ──

    /** Why a file was last judged as it was: its size and time, and what was wrong with it (null when it is a block). */
    private class Verdict(val length: Long, val modified: Long, val why: String?)

    private val verdicts = HashMap<String, Verdict>()

    /** The files that may be blocks: every .json file in the folder that isn't hidden. */
    private fun files(): List<File> = runCatching {
        (dir.listFiles() ?: emptyArray()).filter { it.isFile && !it.name.startsWith(".") && it.name.endsWith(JSON, ignoreCase = true) }
    }.getOrDefault(emptyList())

    /** What is wrong with [file] as a block, or null when it is one; judged again only when its size or time moves. */
    private fun why(file: File): String? {
        val length = file.length()
        val modified = file.lastModified()
        verdicts[file.name]?.takeIf { it.length == length && it.modified == modified }?.let { return it.why }
        val why = try {
            blockProblem(file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            "couldn't be read (${e.message ?: e::class.simpleName})"
        }
        verdicts[file.name] = Verdict(length, modified, why)
        return why
    }

    private class Scan(val blocks: Map<String, File>, val aside: List<SetAside>)

    /** Every file judged: the blocks by name, and the files set aside with why. */
    private fun scan(): Scan {
        val byName = LinkedHashMap<String, MutableList<File>>()
        for (f in files().sortedBy { it.name }) byName.getOrPut(folderBlockNameOf(f.name)) { mutableListOf() } += f
        val blocks = LinkedHashMap<String, File>()
        val aside = mutableListOf<SetAside>()
        for ((name, list) in byName) {
            val canonical = folderFileNameOf(name)
            var holder: File? = null
            for (f in list.sortedBy { if (it.name == canonical) 0 else 1 }) {
                val why = why(f)
                when {
                    why != null -> aside += SetAside(f.name, name, why)
                    holder == null -> holder = f
                    else -> aside += SetAside(f.name, name, "is a second file for the block $name, which ${holder.name} holds")
                }
            }
            if (holder != null) blocks[name] = holder
        }
        // A second version a sync app kept beside a block is his to join, never a block of its own.
        val seconds = blocks.keys.mapNotNull { n -> secondVersionOf(n) { blocks.containsKey(it) }?.let { n to it } }
        for ((n, of) in seconds) {
            aside += SetAside(blocks.getValue(n).name, n, "is a second version of $of, kept by your sync app")
            blocks.remove(n)
        }
        return Scan(blocks, aside.sortedBy { it.file })
    }

    /**
     * The names in the folder, kept for a moment: a read is the commonest
     * call, and the folder is listed again once anything in it is added,
     * removed or renamed (its time moves), after a second at most, and after
     * every write of Sundial's own.
     */
    private var listing: Triple<Long, Long, Set<String>>? = null

    private fun listing(): Set<String> {
        val now = System.currentTimeMillis()
        val modified = dir.lastModified()
        listing?.let { (at, m, names) -> if (m == modified && now - at < 1000) return names }
        val names = runCatching { dir.list()?.toSet() }.getOrNull().orEmpty()
        listing = Triple(now, modified, names)
        return names
    }

    private fun relist() {
        listing = null
    }

    /** What Sundial itself last wrote to each block in this run (null: removed), to tell its own changes from another hand's. */
    private val mine = HashMap<String, String?>()

    override fun writtenHere(name: String, text: String?): Boolean = mine.containsKey(name) && mine[name] == text

    init {
        runCatching { finishRestore() }
    }

    /**
     * The file that holds [name] now, if any: its own name's file, or another
     * that comes back to the same name. Matched exactly, so on a disk that
     * ignores capitals Garden.json is never taken for garden's.
     */
    private fun fileOf(name: String): File? {
        val names = listing()
        val own = folderFileNameOf(name)
        if (own in names) return File(dir, own).takeIf { it.isFile }
        return names.firstOrNull { !it.startsWith(".") && it.endsWith(JSON, ignoreCase = true) && folderBlockNameOf(it) == name }
            ?.let { File(dir, it) }?.takeIf { it.isFile }
    }

    /** [file], when it holds [name] as a block Sundial reads and writes; null when it is set aside. */
    private fun readable(name: String, file: File): Boolean =
        why(file) == null && secondVersionOf(name) { fileOf(it) != null } == null

    override fun read(name: String): String? {
        val f = fileOf(name) ?: return null
        val length = f.length()
        val modified = f.lastModified()
        val text = runCatching { f.readText(Charsets.UTF_8) }.getOrNull() ?: return null
        // Judged again unless the file is as it was when last judged (its size, its time, and the bytes just read).
        val cached = verdicts[f.name]?.takeIf { it.length == length && it.modified == modified && text.toByteArray(Charsets.UTF_8).size.toLong() == length }
        val why = cached?.why ?: if (cached != null) null else blockProblem(text).also { verdicts[f.name] = Verdict(length, modified, it) }
        if (why != null) return null
        if (secondVersionOf(name) { fileOf(it) != null } != null) return null
        return text
    }

    override fun write(name: String, json: String) {
        val existing = fileOf(name)
        if (existing != null && !readable(name, existing)) throw SetAsideFile("${existing.name} is a file Sundial can't read as a block, so it is left as it is.")
        val target = existing ?: File(dir, folderFileNameOf(name))
        if (existing == null && target.exists()) throw SetAsideFile("${target.name} would be written over a file of another name that differs only in its capitals.")
        atomicWrite(target, json)
        mine[name] = json
        relist()
    }

    override fun delete(name: String) {
        val f = fileOf(name) ?: return
        if (readable(name, f)) {
            f.delete()
            mine[name] = null
        }
        relist()
    }

    override fun names(): List<String> = runCatching { scan().blocks.keys.toList() }.getOrDefault(emptyList())

    override fun setAside(): List<SetAside> = runCatching { scan().aside }.getOrDefault(emptyList())

    /** Whether [name] is taken in the folder, whatever its capitals, by a block or by any file that comes back to it. For the move. */
    fun taken(name: String): String? {
        val own = folderFileNameOf(name)
        return files().firstOrNull { it.name.equals(own, ignoreCase = true) || folderBlockNameOf(it.name).equals(name, ignoreCase = true) }?.name
    }

    /**
     * A short line that changes whenever anything in the folder does: a file
     * added, removed, renamed, or written. Sundial looks at it on return and
     * every few seconds while open, and reads again when it moves.
     */
    fun fingerprint(): String = runCatching {
        (dir.listFiles() ?: emptyArray()).filter { it.isFile && !it.name.startsWith(".") }.sortedBy { it.name }
            .joinToString("|") { "${it.name}:${it.length()}:${it.lastModified()}" }.hashCode().toString(16) +
            ":" + stampsFile.lastModified()
    }.getOrDefault("unreadable")

    // ── stamps ──

    override fun stamps(): Stamps {
        val meta = runCatching { stampsFile.takeIf { it.isFile }?.let { parseJson(it.readText(Charsets.UTF_8)) as? JsObj } }.getOrNull() ?: return Stamps()
        fun map(key: String): Map<String, String> {
            val o = meta[key] as? JsObj ?: return emptyMap()
            return o.keys().filter { o[it] is JsStr }.associateWith { jsString(o[it]) }
        }
        return Stamps(map("touched"), map("born"))
    }

    override fun setStamps(stamps: Stamps) = atomicWrite(stampsFile, stampsJson(stamps))

    // ── a restore ──

    override fun replaceAll(blocks: Map<String, String>, stamps: Stamps) {
        val now = scan()
        val targets = LinkedHashMap<String, String>()
        for (name in blocks.keys) {
            val f = now.blocks[name] ?: fileOf(name)
            if (f != null && now.blocks[name] == null) throw SetAsideFile("${f.name} is a file Sundial can't read as a block, so a copy holding $name can't be restored over it.")
            val target = f?.name ?: folderFileNameOf(name)
            if (f == null && File(dir, target).exists()) throw SetAsideFile("$target differs from a file in the folder only in its capitals, so the copy can't be restored over it.")
            targets[name] = target
        }
        incoming.deleteRecursively()
        incoming.mkdirs()
        for ((name, json) in blocks) File(incoming, targets.getValue(name)).writeText(json, Charsets.UTF_8)
        incomingStamps.writeText(stampsJson(stamps), Charsets.UTF_8)
        // The journal is the moment the restore is decided: from here it is finished, at the next start if not now.
        val remove = now.blocks.filterKeys { it !in blocks }.values.map { it.name }.sorted()
        atomicWrite(journal, stringify(JsObj.of("remove" to JsArr(remove.map { str(it) }.toMutableList<Js?>())))!!)
        applyJournal()
        for (n in now.blocks.keys) mine[n] = null
        mine.putAll(blocks)
        relist()
    }

    /** Finish a restore whose journal was written, or abandon one whose was not (see [replaceAll]). */
    private fun finishRestore() {
        if (journal.isFile) applyJournal()
        else if (incoming.exists() || incomingStamps.exists()) {
            incoming.deleteRecursively()
            incomingStamps.delete()
        }
        tmp.listFiles()?.forEach { it.delete() }
    }

    private fun applyJournal() {
        val remove = runCatching { ((parseJson(journal.readText(Charsets.UTF_8)) as JsObj)["remove"] as JsArr).items.map { jsString(it) } }.getOrDefault(emptyList())
        for (f in (incoming.listFiles() ?: emptyArray()).sortedBy { it.name }) move(f, File(dir, f.name))
        for (n in remove) File(dir, n).takeIf { it.isFile && why(it) == null }?.delete()
        if (incomingStamps.isFile) move(incomingStamps, stampsFile)
        incoming.deleteRecursively()
        journal.delete()
    }

    // ── writing ──

    private fun stampsJson(stamps: Stamps): String {
        fun obj(m: Map<String, String>) = JsObj().also { o -> m.toSortedMap().forEach { (k, v) -> o[k] = str(v) } }
        return stringifyPretty(JsObj.of("touched" to obj(stamps.touched), "born" to obj(stamps.born))) + "\n"
    }

    private fun move(from: File, to: File) {
        to.parentFile.mkdirs()
        Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    /** Write [text] to a temporary file in the hidden folder, then rename it over [target]: it lands whole or not at all. */
    private fun atomicWrite(target: File, text: String) {
        tmp.mkdirs()
        target.parentFile.mkdirs()
        val t = File.createTempFile("write-", ".tmp", tmp)
        try {
            t.writeText(text, Charsets.UTF_8)
            Files.move(t.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            t.delete()
        }
    }

    companion object {
        /** The hidden folder inside his that holds Sundial's own housekeeping. */
        const val HOUSE = ".sundial"
        private const val JSON = ".json"
    }
}

/** A write or restore that would land on a file the folder holds but Sundial can't read as a block. */
class SetAsideFile(message: String) : IllegalStateException(message)

/** Why [text] isn't a block the pocket can read, in plain words; null when it is one (the same test a restored copy passes). */
fun blockProblem(text: String): String? {
    if (text.isBlank()) return "is empty"
    val js = try {
        parseJson(text)
    } catch (_: JsonSyntaxError) {
        return "isn't readable JSON"
    }
    if (js !is JsObj) return "is JSON, but not a block: a block is an object"
    validateShape(js)?.let { why ->
        val key = Regex("""invalid key "([^"]*)"""").find(why)?.groupValues?.get(1)
        return if (key != null) "is JSON, but not a block: it holds \"$key\", where a block holds only _ and 1 to 9"
        else "is JSON, but not a block: a line in it holds a block written as text"
    }
    if (!hasFloor(js)) return "is JSON, but not a block: it has no opening line"
    return null
}

private const val REFUSED = "/\\:*?\"<>|"
private val DEVICE = Regex("^(con|prn|aux|nul|com[0-9]|lpt[0-9])$", RegexOption.IGNORE_CASE)

private fun isHex(c: Char) = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'

private fun pct(bytes: ByteArray) = bytes.joinToString("") { "%" + (it.toInt() and 0xff).toString(16).uppercase().padStart(2, '0') }

/**
 * The file a block lives in, in his folder: its name as it stands, with only
 * what the common file systems refuse written %XX (the UTF-8 of
 * / \ : * ? " < > |, control characters, a dot or space at either end, the
 * first letter of a name Windows keeps for devices), and a % that would
 * otherwise read as one. So "Garden" is Garden.json and
 * "passport:Phenomemental" is passport%3APhenomemental.json.
 */
fun folderFileNameOf(blockName: String): String {
    val out = StringBuilder()
    var i = 0
    while (i < blockName.length) {
        val cp = blockName.codePointAt(i)
        val n = Character.charCount(cp)
        val ch = blockName.substring(i, i + n)
        val refuse = cp < 0x20 || cp == 0x7f || (n == 1 && ch[0] in REFUSED) ||
            (cp == '%'.code && i + 2 < blockName.length && isHex(blockName[i + 1]) && isHex(blockName[i + 2]))
        out.append(if (refuse) pct(ch.toByteArray(Charsets.UTF_8)) else ch)
        i += n
    }
    var s = out.toString()
    if (s.startsWith(".") || s.startsWith(" ")) s = pct(s.substring(0, 1).toByteArray()) + s.substring(1)
    if (s.endsWith(".") || s.endsWith(" ")) s = s.dropLast(1) + pct(s.takeLast(1).toByteArray())
    if (DEVICE.matches(s.substringBefore('.'))) s = pct(s.substring(0, 1).toByteArray()) + s.substring(1)
    return "$s.json"
}

/**
 * The block a file in his folder holds: its name without .json, each %XX
 * that reads as UTF-8 read back. A file named any other way is the block of
 * that name as it stands, so every file comes back to a name.
 */
fun folderBlockNameOf(fileName: String): String {
    val s = if (fileName.endsWith(".json", ignoreCase = true)) fileName.dropLast(5) else fileName
    val out = StringBuilder()
    var i = 0
    while (i < s.length) {
        if (s[i] == '%' && i + 2 < s.length && isHex(s[i + 1]) && isHex(s[i + 2])) {
            // A run of %XX is one UTF-8 sequence: read back only when it is a whole, valid one.
            var j = i
            val bytes = java.io.ByteArrayOutputStream()
            while (j + 2 < s.length && s[j] == '%' && isHex(s[j + 1]) && isHex(s[j + 2])) {
                bytes.write(s.substring(j + 1, j + 3).toInt(16))
                j += 3
            }
            val decoded = runCatching {
                Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes.toByteArray())).toString()
            }.getOrNull()
            out.append(decoded ?: s.substring(i, j))
            i = j
        } else {
            out.append(s[i])
            i++
        }
    }
    return out.toString()
}

/**
 * When [name] is the name a sync app gives a second version of a block it
 * found changed on both sides, the block it is a second version of; else
 * null. Syncthing's .sync-conflict-, a "conflicted copy" (Dropbox,
 * Nextcloud, ownCloud), Seafile's SFConflict and a "Case Conflict" always
 * say so; a numbered copy, "garden (1)", only while garden itself stands
 * ([exists]).
 */
fun secondVersionOf(name: String, exists: (String) -> Boolean): String? {
    SECOND_ALWAYS.firstNotNullOfOrNull { it.find(name)?.groupValues?.get(1) }?.let { return it }
    NUMBERED.find(name)?.groupValues?.get(1)?.takeIf(exists)?.let { return it }
    return null
}

private val SECOND_ALWAYS = listOf(
    Regex("""^(.+)\.sync-conflict-\d{8}-\d{6}-[A-Z0-9]{7}$"""),
    Regex("""^(.+?) \((?:[^()]*'s )?conflicted copy[^()]*\)$""", RegexOption.IGNORE_CASE),
    Regex("""^(.+?) \(SFConflict [^()]*\)$"""),
    Regex("""^(.+?) \(Case Conflict(?: \d+)?\)$""", RegexOption.IGNORE_CASE),
)

private val NUMBERED = Regex("""^(.+?) \((\d{1,3})\)$""")
