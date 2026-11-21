package com.phenomemental.pocketbeach.bsp

import java.io.File

/**
 * Sundial 0.11, the move (spine:pocket-beach 1.111178719933 and
 * 1.111178719942): his blocks from the app's private files into his folder,
 * by copy, check and switch.
 *
 * 1. **Check.** Every block of his that would go to the folder is looked
 *    at first. If any name is already taken in the folder, whatever its
 *    capitals, or any block can't be read as one, the move does not start,
 *    and those names are listed.
 * 2. **A whole copy** of every block, the app's own and his, is saved in the
 *    app's own files, read back and checked as a copy that can be restored.
 * 3. **Copy.** His shell, his history and the app's own blocks still in the
 *    pocket go to the private region beside the app's own; every other
 *    block goes to the folder, with its stamps. Each is read back and
 *    compared with the original, byte for byte.
 * 4. **Switch,** by the caller, only once all of that has passed. If anything
 *    fails, what was written into the folder is taken out again and nothing
 *    switches. The private pocket is never touched: it stays, whole, until
 *    he removes it.
 */
object Move {
    /** What the move would do: [moving] to the folder, [keptPrivate] beside the app's own; and why it can't start, if it can't. */
    class Check(val moving: List<String>, val keptPrivate: List<String>, val clashes: List<String>, val unreadable: List<String>) {
        val ok: Boolean get() = clashes.isEmpty() && unreadable.isEmpty()

        /** Why the move can't start, in plain words; null when it can. */
        val refusal: String?
            get() = when {
                clashes.isNotEmpty() -> "Documents/Sundial already holds " + list(clashes) + ", so nothing was moved. Rename or move " +
                    (if (clashes.size == 1) "that file" else "those files") + " out of the folder, or rename the block here, and try again."
                unreadable.isNotEmpty() -> "These can't be read as blocks, so nothing was moved: " + unreadable.joinToString("; ") + "."
                else -> null
            }

        private fun list(xs: List<String>) = if (xs.size == 1) xs[0] else xs.dropLast(1).joinToString(", ") + " and " + xs.last()
    }

    /** Look at [pocket] (his blocks in the app's private files) against [folder], writing nothing. */
    fun check(pocket: BlockStorage, folder: FolderStorage, routes: (String) -> Boolean): Check {
        val moving = mutableListOf<String>()
        val kept = mutableListOf<String>()
        val clashes = mutableListOf<String>()
        val unreadable = mutableListOf<String>()
        for (name in pocket.names().distinct().sorted()) {
            val text = pocket.read(name) ?: continue
            if (routes(name)) {
                kept += name
                continue
            }
            blockProblem(text)?.let { unreadable += "$name $it" }
            folder.taken(name)?.let { clashes += if (it == folderFileNameOf(name)) it else "$it (for $name)" }
            moving += name
        }
        return Check(moving, kept, clashes, unreadable)
    }

    /** What the move did: the blocks now in the folder and kept private, where the whole copy was saved; or why nothing switched. */
    class Moved(val moved: List<String>, val keptPrivate: List<String>, val copy: File?, val problem: String?) {
        val ok: Boolean get() = problem == null
    }

    /**
     * Carry the move out: [whole] (every block, as the app reads them) saved
     * to [copyTo] first, then each block copied and checked. The caller
     * switches the pocket to [folder] only when this comes back [Moved.ok].
     */
    fun carryOut(pocket: BlockStorage, hidden: BlockStorage, folder: FolderStorage, routes: (String) -> Boolean, whole: BlockStorage, copyTo: File, nowMs: Long): Moved {
        val check = check(pocket, folder, routes)
        check.refusal?.let { return Moved(emptyList(), emptyList(), null, it) }

        // 2. The whole copy, read back as a copy that can be restored.
        val copyText = Copies.make(whole, nowMs)
        try {
            copyTo.parentFile?.mkdirs()
            copyTo.writeText(copyText, Charsets.UTF_8)
            val back = Copies.read(copyTo.readText(Charsets.UTF_8))
            if (back.blocks.keys != whole.names().filter { whole.read(it) != null }.toSet()) error("the copy doesn't hold every block")
        } catch (e: Exception) {
            copyTo.delete()
            return Moved(emptyList(), emptyList(), null, "The whole copy couldn't be saved first (${e.message}), so nothing was moved.")
        }

        // 3. Copy and check, each block.
        val written = mutableListOf<String>()
        fun undo(why: String): Moved {
            for (n in written) runCatching { folder.delete(n) }
            return Moved(emptyList(), emptyList(), copyTo, "$why Nothing was moved: your blocks are where they were.")
        }
        try {
            folder.dir.mkdirs()
            if (!folder.dir.isDirectory) return undo("Documents/Sundial couldn't be made.")
            for (name in check.keptPrivate) {
                val text = pocket.read(name) ?: continue
                if (hidden.read(name) == null) hidden.write(name, text)
                if (hidden.read(name) == null) return undo("$name couldn't be kept in the app's own files.")
            }
            for (name in check.moving) {
                val text = pocket.read(name) ?: continue
                folder.write(name, text)
                written += name
                if (folder.read(name) != text) return undo("$name didn't read back from the folder as it was written.")
            }
            val stamps = pocket.stamps()
            fun only(m: Map<String, String>, names: Collection<String>) = m.filterKeys { it in names }
            val folderStamps = Stamps(only(stamps.touched, check.moving), only(stamps.born, check.moving))
            folder.setStamps(folderStamps)
            if (folder.stamps() != folderStamps) return undo("The folder's stamps didn't read back as they were written.")
            val h = hidden.stamps()
            hidden.setStamps(Stamps(only(stamps.touched, check.keptPrivate) + h.touched, only(stamps.born, check.keptPrivate) + h.born))
        } catch (e: Exception) {
            return undo("The move stopped part way (${e.message ?: e::class.simpleName}).")
        }
        return Moved(check.moving, check.keptPrivate, copyTo, null)
    }
}
