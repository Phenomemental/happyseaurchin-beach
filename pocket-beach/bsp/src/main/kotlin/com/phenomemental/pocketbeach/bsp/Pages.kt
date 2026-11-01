package com.phenomemental.pocketbeach.bsp

/** The two regions a front end shows: Matthew's own blocks, and the teaching shelf. */
enum class Region(val agentId: String) { POCKET("pocket"), TEACHING("pscale") }

/** A block as the Shore lists it. */
class BlockSummary(val region: Region, val name: String, val opening: String?, val touched: String?, val born: String?)

class ShoreView(val mine: List<BlockSummary>, val teaching: List<BlockSummary>, val hidden: List<BlockSummary> = emptyList())

/** One line of a block: where it stands, what it says, its arrival stamp if it was appended, and how many lines stand one step beneath it. */
class Line(val address: String, val text: String?, val stamp: String?, val beneath: Int)

/** A summary the beach's convention says is owed: a line written at [at] that voices lines [over]. */
class Owed(val at: String, val over: String)

/**
 * Where Matthew stands in a block. At the block's opening ([address] empty)
 * the lines are every line at pscale 0, the disc bsp() gives; beneath, they
 * are the lines one step finer. [trail] is the way back up, coarsest first.
 */
class PositionView(
    val region: Region,
    val block: String,
    val address: String,
    val floor: Int,
    val text: String?,
    val stamp: String?,
    val trail: List<Line>,
    val lines: List<Line>,
    /** Every digit is taken: beneath a line, nothing more fits; at the opening, the next line wraps the block. */
    val full: Boolean,
    /** How many times the block's opening has wrapped, as its floor shows. */
    val wrapped: Int,
    val owed: List<Owed>,
)

enum class Kind { CHANGE, ADD, NEW_BLOCK, WHOLE }

/**
 * A write as it will happen: the bsp() [call] that runs, and where its line
 * [landsAt] (a full-width address; empty for a block's opening line), learned
 * by running the call on a copy of the block first. A plan that can't go ahead
 * carries a [refusal] in plain words and no call.
 */
class Plan internal constructor(
    val kind: Kind,
    val block: String,
    val address: String,
    val text: String,
    val call: JsObj?,
    val landsAt: String?,
    val wraps: Boolean,
    val refusal: String?,
    internal val expected: String?,
    internal val nowMs: Long,
    /** What an add puts there when it is more than the line (an entry with fields beneath it), or the whole block a keep makes. */
    internal val content: Js? = null,
    /** For a whole-block write, the block as it stood when planned: it is written only while it still stands so. */
    internal val before: String? = null,
)

/** What carrying out a plan did: bsp()'s own [reply], and a [problem] in plain words if it did not land as planned. */
class Done(val ok: Boolean, val reply: String, val landsAt: String?, val wraps: Boolean, val problem: String?)

/**
 * What a front end shows of the pocket, and the writes it offers.
 *
 * Every read goes through the walker bsp() reads with: a block opens on the
 * disc at pscale 0, and a position beneath shows its path-walk with one layer
 * of descent. Every write goes through bsp() itself, and only after a dry run
 * on a copy of the block has shown where it lands and that no other line
 * moves or changes. The same call then runs for real, and what it left is
 * compared with what the dry run showed.
 */
class Pages(
    private val storage: BlockStorage,
    private val shelf: Shelf,
    private val bsp: PocketBsp,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val beach = Beach(storage)

    fun shore(): ShoreView {
        val stamps = storage.stamps()
        val all = storage.names().distinct().mapNotNull { name ->
            val block = beach.loadBlock(name) ?: return@mapNotNull null
            BlockSummary(Region.POCKET, name, collectUnderscore(block), stamps.touched[name], stamps.born[name])
        }.sortedWith(compareByDescending<BlockSummary> { it.touched ?: it.born ?: "" }.thenBy { it.name })
        val teaching = shelf.shelved().mapNotNull { name ->
            shelf.load(name)?.let { BlockSummary(Region.TEACHING, name, collectUnderscore(it), null, null) }
        }
        // 0.8: once he has hatched, the app's own blocks are in the hidden region: never in his map, never in hers.
        val hatched = Hatch.hatched(storage) != null
        val (hidden, mine) = all.partition { hatched && Hidden.routes(it.name) }
        return ShoreView(mine, teaching, hidden)
    }

    fun exists(name: String): Boolean = beach.loadBlock(name) != null

    /** The floor of the pocket block [name] (10 for a clock), or 0 when there is none. */
    fun floorOf(name: String): Int = beach.loadBlock(name)?.let(::floorDepth) ?: 0

    /** The position at [address] in a block, or null when the block or the position isn't there. */
    fun position(region: Region, name: String, address: String = ""): PositionView? {
        val block = load(region, name) ?: return null
        val floor = floorDepth(block)
        if (address.isEmpty()) {
            val lines = bspRead(block, null, 0).discEntries.orEmpty()
                .filter { !onRootChain(it.address) }
                .map { Line(it.address, it.content, it.stamp, it.beneath ?: 0) }
            return PositionView(
                region, name, "", floor, collectUnderscore(block), null, emptyList(), lines,
                full = beach.nextZeroFreeSlot(block, floor) == null,
                wrapped = maxOf(0, floor - 1),
                // Counting the summaries owed walks every path to the floor's last rung: 9^9 of them on a
                // clock's floor-10 form, which never ends. A pocket accumulator is far below floor 7.
                owed = if (floor > OWED_FLOOR) emptyList() else beach.owedSummaries(block).map { Owed(it.first, it.second) },
            )
        }
        val digits = try { parseSpindle(address, floor).digits } catch (_: InvalidAddressError) { return null }
        if (digits.isEmpty()) return position(region, name)
        val node = walk(block, digits) ?: return null
        val read = bspRead(block, address, floor - digits.size - 1)
        val walked = read.pathWalk.orEmpty()
        val here = walked.last()
        val stampAt = if (entryStamp(node) != null) fullWidthAddress(digits + '3', floor) else null
        val lines = read.descent.orEmpty().filter { it.address != stampAt }.map {
            Line(it.address, it.content, it.stamp, ringBeneath(readAt(block, it.address)).size)
        }
        return PositionView(
            region, name, here.address, floor, here.content, here.stamp,
            trail = walked.dropLast(1).filter { !onRootChain(it.address) }.map { Line(it.address, it.content, it.stamp, 0) },
            lines = lines,
            full = node is JsObj && (1..9).all { node.has(it.toString()) },
            wrapped = 0,
            owed = emptyList(),
        )
    }

    // ── writes ──

    /** Change the line Matthew stands on ([address] empty: the block's opening line). */
    fun planChange(name: String, address: String, text: String): Plan {
        val nowMs = clock()
        val before = beach.loadBlock(name) ?: return refused(Kind.CHANGE, name, address, text, "There's no block called $name.")
        if (text.isBlank()) return refused(Kind.CHANGE, name, address, text, "Write the line first.")
        val digits = if (address.isEmpty()) emptyList() else
            try { parseSpindle(address, floorDepth(before)).digits } catch (_: InvalidAddressError) { return refused(Kind.CHANGE, name, address, text, "There's no line at $address.") }
        val target = if (digits.isEmpty()) rootVoiceKey(before) else "${rawPath(digits)}#"
        if (voicesOf(before)[target] == text) return refused(Kind.CHANGE, name, address, text, "The line already says that.")
        // The opening line is re-voiced through the root's underscore ("0"). On a
        // block wrapped twice that address reaches a node one step too deep, so a
        // whole-block write carrying the one changed line is tried after it.
        val calls = if (digits.isEmpty()) listOf(call(name, "0", str(text)), call(name, null, withRootVoice(before, text)))
        else listOf(call(name, address, str(text)))
        for (c in calls) {
            val after = dryRun(name, c, nowMs).second ?: continue
            val a = voicesOf(after)
            val b = voicesOf(before)
            val changed = (b.keys + a.keys).filter { b[it] != a[it] }
            if (changed == listOf(target) && a[target] == text) {
                return Plan(Kind.CHANGE, name, address, text, c, address, false, null, stringify(after), nowMs)
            }
        }
        return refused(Kind.CHANGE, name, address, text, "This line can't be changed here without disturbing the lines around it.")
    }

    /** Add a line beneath the one Matthew stands on ([address] empty: beneath the block's opening line). */
    fun planAdd(name: String, address: String, text: String): Plan = planAddOf(name, address, text, str(text))

    /**
     * Add an entry beneath a block's opening line: a line ([entry]'s `_`) with
     * fields beneath it, as a log keeps them. The beach stamps its arrival at
     * field 3, as it does every entry appended.
     */
    fun planEntry(name: String, entry: JsObj): Plan {
        val text = (entry["_"] as? JsStr)?.value ?: return refused(Kind.ADD, name, "", "", "An entry needs a line of its own.")
        return planAddOf(name, "", text, entry)
    }

    private fun planAddOf(name: String, address: String, text: String, content: Js): Plan {
        val nowMs = clock()
        val before = beach.loadBlock(name) ?: return refused(Kind.ADD, name, address, text, "There's no block called $name.")
        if (text.isBlank()) return refused(Kind.ADD, name, address, text, "Write the line first.")
        if (address.isNotEmpty()) {
            val node = try { readAt(before, address) } catch (_: InvalidAddressError) { null }
                ?: return refused(Kind.ADD, name, address, text, "There's no line at $address.")
            if (node is JsObj && (1..9).all { node.has(it.toString()) }) return refused(Kind.ADD, name, address, text, FULL)
        }
        val c = call(name, address.ifEmpty { null }, content, append = true)
        val (reply, after) = dryRun(name, c, nowMs)
        if (after == null || deepEqual(after, before)) return refused(Kind.ADD, name, address, text, plainRefusal(reply))
        val rise = floorDepth(after) - floorDepth(before)
        if (rise !in 0..1) return refused(Kind.ADD, name, address, text, DISTURBS)
        val b = voicesOf(before, "_/".repeat(rise))
        val a = voicesOf(after)
        if (b.any { (k, v) -> a[k] != v }) return refused(Kind.ADD, name, address, text, DISTURBS)
        val added = a.keys - b.keys
        val landing = added.singleOrNull { k -> a[k] == text && (added - k).all { it.startsWith(k.removeSuffix("#") + "/") } }
            ?: return refused(Kind.ADD, name, address, text, DISTURBS)
        val path = landing.removeSuffix("#")
        val landsAt = fullWidthAddress(path.split('/').map { if (it == "_") '0' else it[0] }, floorDepth(after))
        return Plan(Kind.ADD, name, address, text, c, landsAt, rise == 1, null, stringify(after), nowMs, content.takeIf { it is JsObj })
    }

    /** Make a new block, its opening line the one the beach gives a block it makes ("garden at pocket."). */
    fun planNewBlock(typed: String): Plan {
        val name = typed.trim()
        fun no(why: String) = refused(Kind.NEW_BLOCK, name, "", "", why)
        nameRefusal(name)?.let { return no(it) }
        val nowMs = clock()
        val opening = beach.defaultIdentity(name)
        val c = call(name, null, JsObj.of("_" to str(opening)))
        val after = dryRun(name, c, nowMs).second
        if (after == null || !deepEqual(after, JsObj.of("_" to str(opening)))) return no(DISTURBS)
        return Plan(Kind.NEW_BLOCK, name, "", opening, c, "", false, null, stringify(after), nowMs)
    }

    /**
     * Keep a block that arrived from elsewhere (decision 19) as a new block of
     * the pocket, exactly as it came: one whole-block write, refused if what
     * it leaves isn't the block as received.
     */
    fun planKeep(typed: String, block: Js): Plan {
        val name = typed.trim()
        fun no(why: String) = refused(Kind.NEW_BLOCK, name, "", "", why)
        nameRefusal(name)?.let { return no(it) }
        if (block !is JsObj) return no("Only a block (a JSON object) can be kept.")
        val nowMs = clock()
        val c = call(name, null, block)
        val (reply, after) = dryRun(name, c, nowMs)
        if (after == null) return no(plainRefusal(reply))
        if (!deepEqual(after, block)) return no("The pocket would store this block differently from how it came, so it wasn't kept.")
        return Plan(Kind.NEW_BLOCK, name, "", collectUnderscore(block) ?: "", c, "", false, null, stringify(after), nowMs, block)
    }

    /**
     * Write [block] whole in place of the pocket block [typed], as a sorted
     * day lands (0.6): one whole-block write, refused if what it leaves isn't
     * [block]. A block not yet in the pocket is made. It is carried out only
     * while the block still stands as it did when this was planned.
     */
    fun planWhole(typed: String, block: JsObj): Plan {
        val name = typed.trim()
        val before = beach.loadBlock(name) ?: return planKeep(name, block)
        fun no(why: String) = refused(Kind.WHOLE, name, "", "", why)
        val nowMs = clock()
        val c = call(name, null, block)
        val (reply, after) = dryRun(name, c, nowMs)
        if (after == null) return no(plainRefusal(reply))
        if (!deepEqual(after, block)) return no("The pocket would store $name differently from how it was set out, so it wasn't written.")
        if (deepEqual(after, before)) return no("$name holds that already.")
        return Plan(Kind.WHOLE, name, "", collectUnderscore(block) ?: "", c, "", false, null, stringify(after), nowMs, block, stringify(before))
    }

    // ── naming and removing ──

    /** Why the pocket block [name] can't be renamed [typed], or null if it can. */
    fun renameRefusal(name: String, typed: String): String? {
        if (storage.read(name) == null) return "There's no block called $name."
        val to = typed.trim()
        if (to == name) return "That's its name already."
        return nameRefusal(to, besides = name)
    }

    /**
     * Rename a pocket block: the same block, line for line, under a new name,
     * born when it was born and touched now. It is written under the new name
     * before the old is removed, so a stop part way leaves two, never none.
     */
    fun rename(name: String, typed: String): Done = synchronized(bsp) {
        renameRefusal(name, typed)?.let { return Done(false, "", null, false, it) }
        val to = typed.trim()
        val block = storage.read(name)!!
        if (to.equals(name, ignoreCase = true)) {
            // Only its capitals change. Where a disk doesn't tell capitals apart, the new name is the old one's file,
            // so the old goes first; it is kept under a passing name meanwhile, so a stop part way leaves it, never none.
            val passing = "$to (being renamed)"
            storage.write(passing, block)
            storage.delete(name)
            storage.write(to, block)
            storage.delete(passing)
        } else {
            storage.write(to, block)
        }
        val s = storage.stamps()
        val touched = s.touched - name + (to to isoString(clock()))
        val born = s.born - name + listOfNotNull(s.born[name]?.let { to to it })
        storage.setStamps(Stamps(touched, born))
        if (!to.equals(name, ignoreCase = true)) storage.delete(name)
        Done(true, "", null, false, null)
    }

    /** Remove a pocket block from this phone. Only a copy saved before holds it after. */
    fun remove(name: String): Done = synchronized(bsp) {
        if (storage.read(name) == null) return Done(false, "", null, false, "There's no block called $name.")
        storage.delete(name)
        val s = storage.stamps()
        storage.setStamps(Stamps(s.touched - name, s.born - name))
        Done(true, "", null, false, null)
    }

    /**
     * Why [name] can't be a new block's name, or null. A name differing from
     * another block's only in its capitals is that block's ("million" is
     * Million), so it can't be a second one; [besides] is the block being
     * renamed, which may change its own capitals.
     */
    private fun nameRefusal(name: String, besides: String? = null): String? = when {
        name.isEmpty() -> "Give the block a name."
        name.any { it.isISOControl() } -> "A name fits on one line."
        name.length > 120 -> "That name is too long: keep it under 120 letters."
        name.startsWith("sed:") || name.startsWith("grain:") -> "Names starting sed: or grain: belong to the beach, which the pocket can't reach in this version."
        storage.read(name) != null -> "There's already a block called $name."
        else -> storage.names().firstOrNull { it != besides && it.equals(name, ignoreCase = true) }?.let { "There's already a block called $it." }
    }

    /** The pocket's block [typed] means: its own name if there is one so called, else the one block whose name differs only in its capitals. */
    fun named(typed: String): String {
        if (storage.read(typed) != null) return typed
        return storage.names().filter { it.equals(typed, ignoreCase = true) }.singleOrNull() ?: typed
    }

    /**
     * Carry out a plan Matthew has seen. It is planned again at this instant;
     * if it would now land anywhere else, nothing is written.
     */
    fun carryOut(plan: Plan): Done = synchronized(bsp) {
        if (plan.call == null) return Done(false, "", null, false, plan.refusal)
        val fresh = when (plan.kind) {
            Kind.CHANGE -> planChange(plan.block, plan.address, plan.text)
            Kind.ADD -> planAddOf(plan.block, plan.address, plan.text, plan.content ?: str(plan.text))
            Kind.NEW_BLOCK -> plan.content?.let { planKeep(plan.block, it) } ?: planNewBlock(plan.block)
            Kind.WHOLE -> if (beach.loadBlock(plan.block)?.let(::stringify) != plan.before) refused(Kind.WHOLE, plan.block, "", "", CHANGED)
                else planWhole(plan.block, plan.content as JsObj)
        }
        val call = fresh.call ?: return Done(false, "", null, false, fresh.refusal)
        if (fresh.landsAt != plan.landsAt || fresh.wraps != plan.wraps) {
            return Done(false, "", null, false, CHANGED)
        }
        val reply = bsp.call(call, fresh.nowMs)
        val after = beach.loadBlock(plan.block)
        if (after == null || stringify(after) != fresh.expected) {
            return Done(false, reply, fresh.landsAt, fresh.wraps, "The write did not land as shown. Read bsp's reply below.")
        }
        Done(true, reply, fresh.landsAt, fresh.wraps, null)
    }

    // ── copies ──

    /** Every block as one copy file's text (see [Copies]), taken between writes. */
    fun saveCopy(): String = synchronized(bsp) { Copies.make(storage, clock()) }

    /** Put a copy in place of every block, between writes. */
    fun restore(copy: Copies.Copy) = synchronized(bsp) { Copies.restore(storage, copy) }

    fun stamps(): Stamps = storage.stamps()

    // ── helpers ──

    private fun load(region: Region, name: String): Js? = when (region) {
        Region.POCKET -> beach.loadBlock(name)
        Region.TEACHING -> shelf.load(name)
    }

    private fun refused(kind: Kind, name: String, address: String, text: String, why: String) =
        Plan(kind, name, address, text, null, null, false, why, null, 0)

    private fun call(name: String, spindle: String?, content: Js, append: Boolean = false): JsObj = JsObj().also { c ->
        c["agent_id"] = str("pocket")
        c["block"] = str(name)
        if (spindle != null) c["spindle"] = str(spindle)
        c["content"] = content
        if (append) c["append"] = JsBool(true)
    }

    /** Run a call on a copy of its block alone, at a given instant: bsp()'s reply and the block it leaves. */
    private fun dryRun(name: String, call: JsObj, nowMs: Long): Pair<String, Js?> {
        val trial = MemoryStorage()
        storage.read(name)?.let { trial.write(name, it) }
        val s = storage.stamps()
        trial.setStamps(Stamps(s.touched.filterKeys { it == name }, s.born.filterKeys { it == name }))
        val reply = PocketBsp(trial, shelf).call(call, nowMs)
        return reply to Beach(trial).loadBlock(name)
    }

    private fun withRootVoice(block: Js, text: String): Js {
        val copy = deepCopy(block)
        var node: Js = copy
        while (true) {
            val u = jsGet(node, "_")
            if (u is JsObj) node = u else break
        }
        jsSet(node, "_", str(text))
        return copy
    }

    companion object {
        const val FULL = "Nothing more fits beneath this line: it holds nine. Only a block's opening line wraps past nine."
        const val DISTURBS = "This write would move or change other lines, so it wasn't made."
        const val CHANGED = "The block changed while this was open, so nothing was saved. Look again, then save."

        /** The highest floor whose owed summaries are counted when a block opens (9^5 paths at most). */
        const val OWED_FLOOR = 6

        private fun onRootChain(address: String) = address.isNotEmpty() && address.all { it == '0' || it == '.' }

        private fun walk(block: Js, digits: List<Char>): Js? {
            var node: Js? = block
            for (d in digits) {
                val key = if (d == '0') "_" else d.toString()
                if (!truthy(node) || !isObject(node) || !jsIn(node, key)) return null
                node = jsGet(node, key)
            }
            return node
        }

        private fun rawPath(digits: List<Char>) = digits.joinToString("/") { if (it == '0') "_" else it.toString() }

        /**
         * Where a string stands in a block, the same whether the store keeps a
         * line as a bare string or as the `_` of an object with lines beneath:
         * "1" and "1/_" are both line 1's own text, "1#".
         */
        private fun voiceKey(path: String): String = when {
            path == "_" -> "#"
            path.endsWith("/_") -> path.dropLast(2) + "#"
            else -> "$path#"
        }

        private fun rootVoiceKey(block: Js): String = voiceKey(List(maxOf(1, floorDepth(block))) { "_" }.joinToString("/"))

        /** Every value in a block by where it stands ([prefix] places the whole beneath a wrapped root). */
        private fun voicesOf(block: Js?, prefix: String = ""): Map<String, String> {
            val out = LinkedHashMap<String, String>()
            fun visit(node: Js?, path: String) {
                when (node) {
                    is JsStr -> out[voiceKey(path)] = node.value
                    is JsObj -> for (k in node.keys()) visit(node[k], if (path.isEmpty()) k else "$path/$k")
                    null -> {}
                    else -> out[voiceKey(path)] = "\u0000" + stringify(node)
                }
            }
            visit(block, prefix.removeSuffix("/"))
            return out
        }

        /** bsp()'s refusal of an add, in plain words where it is a known one. */
        private fun plainRefusal(reply: String): String = when {
            reply.contains("is full: nine slots") -> FULL
            else -> reply
        }
    }
}
