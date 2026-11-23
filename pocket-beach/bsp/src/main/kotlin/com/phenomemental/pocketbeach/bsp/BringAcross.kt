package com.phenomemental.pocketbeach.bsp

/**
 * Sundial 0.13.5: his blocks brought across from the beach into his pocket,
 * new or replacing his, read only from the beach and written only on his
 * yes, after a whole copy of his pocket is saved. Plain Kotlin: the app reads
 * the beach and keeps the copy; this orders the beach's names, says which
 * places of a block would change, and carries the blocks in one by one,
 * saying what came across and what was refused, with why.
 */
object BringAcross {
    /** Whether [name] is named with his [handle]: passport:Phenomemental, spine:Phenomemental, Phenomemental:notes. */
    fun isHis(name: String, handle: String?): Boolean =
        handle != null && name.split(':').any { it.equals(handle, ignoreCase = true) }

    /** The beach's [names], those named with his [handle] first, each group in the order the beach gave. */
    fun ordered(names: List<String>, handle: String?): List<String> {
        val (his, rest) = names.distinct().partition { isHis(it, handle) }
        return his + rest
    }

    /** A place where his block and the beach's differ: [kind] "changes", "is new" or "goes". */
    class Change(val address: String, val kind: String) {
        /** In words, for the card: "3.4 changes", "the opening line changes". */
        val said: String get() = (if (address.isEmpty()) "the opening line" else address) + " " + kind
    }

    /**
     * The places where [mine] and [theirs] differ, in address order: a place
     * whose words differ "changes", one only the beach's has "is new", one
     * only his has "goes". Places beneath one that is new or goes aren't
     * listed again.
     */
    fun changes(mine: Js?, theirs: Js?): List<Change> {
        val out = mutableListOf<Change>()
        fun words(n: Js?): String? = when (n) {
            is JsStr -> n.value
            is JsObj -> n["_"].let { u -> if (u is JsStr) u.value else stringify(u) }
            null -> null
            else -> stringify(n)
        }
        fun address(digits: List<Char>): String = when (digits.size) {
            0 -> ""
            1 -> digits[0].toString()
            else -> digits[0] + "." + digits.drop(1).joinToString("")
        }
        fun walk(a: Js?, b: Js?, digits: List<Char>) {
            if (a == null && b == null) return
            val at = address(digits)
            if (a == null) { out += Change(at, "is new"); return }
            if (b == null) { out += Change(at, "goes"); return }
            if (words(a) != words(b)) out += Change(at, "changes")
            for (d in '1'..'9') walk((a as? JsObj)?.get("$d"), (b as? JsObj)?.get("$d"), digits + d)
        }
        walk(mine, theirs, emptyList())
        return out
    }

    /** One block that came across: new, or replacing his. */
    class Came(val name: String, val replaced: Boolean) {
        val said: String get() = name + if (replaced) ", replacing yours" else ", new"
    }

    /** One block that didn't, and why, in plain words. */
    class Refused(val name: String, val why: String) {
        val said: String get() = "$name: $why"
    }

    class Report(val came: List<Came>, val refused: List<Refused>)

    /** One block read from the beach: the block, or why it couldn't be. */
    class Read(val name: String, val block: JsObj?, val why: String? = null)

    /**
     * Carry [blocks] into [pages], one by one, each new or replacing the
     * block of that name: what came across, and what was refused with why
     * (it couldn't be read from the beach, his is already the same, or the
     * pocket refused it). Nothing is written for a refused block, and none
     * stops the rest.
     */
    fun bring(pages: Pages, blocks: List<Read>): Report {
        val came = mutableListOf<Came>()
        val refused = mutableListOf<Refused>()
        for (r in blocks) {
            val block = r.block
            if (block == null) { refused += Refused(r.name, r.why ?: "it couldn't be read from the beach"); continue }
            val had = pages.exists(r.name)
            if (had && pages.whole(r.name)?.let { deepEqual(it, block) } == true) {
                refused += Refused(r.name, "yours is already the same as the beach's")
                continue
            }
            val plan = if (had) pages.planWhole(r.name, block) else pages.planKeep(r.name, block)
            plan.refusal?.let { refused += Refused(r.name, it); continue }
            val done = pages.carryOut(plan)
            if (done.ok) came += Came(r.name, had) else refused += Refused(r.name, done.problem ?: "it didn't land as planned")
        }
        return Report(came, refused)
    }
}
