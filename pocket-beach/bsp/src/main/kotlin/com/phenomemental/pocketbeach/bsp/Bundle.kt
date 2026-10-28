package com.phenomemental.pocketbeach.bsp

/*
 * Bundle.kt — the bundle is a shape, and the clock cuts it: a port of
 * bsp-mcp-server's src/bundle.ts (with genus.ts parseReference) at the pinned
 * commit.
 *
 * A node whose digit children are references — name[:address[:attention]],
 * name:now, name:head — is a bundle. Read through the ordinary bsp() it
 * compiles into one window: the index first, then each member unfolded beneath
 * at its own aperture. The shell door compiles a shell's manifest through these
 * same functions, so a door and a bare read cannot diverge. The shape is read
 * off the node's form and settled by resolution: at least one member must name
 * a block that stands.
 *
 * Upstream loads members asynchronously and in parallel; here the loader is a
 * plain function, called slot by slot. Loads are reads, so the order changes
 * nothing.
 */

/** A leaf read as a reference. [word] is "now" (the clock's current beat) or "head" (an accumulator's newest entry). */
class BundleRef(val ref: String, val name: String, val address: String?, val word: String?, val attention: Long?)

private val NAME_RE = Regex("""[A-Za-z][A-Za-z0-9_-]*""")
private val ADDR_RE = Regex("""\d+(\.\d+)?""")
private val ATT_RE = Regex("""-?\d+""")
private val ENTRY_STAMP = Regex("""^\d{4}-\d{2}-\d{2}T""")

/** parseInt of an attention: its whole value, held as a Long (past that, at the edge). */
private fun attentionOf(s: String): Long = s.toBigInteger().coerceIn(Long.MIN_VALUE.toBigInteger(), Long.MAX_VALUE.toBigInteger()).toLong()

/** genus.ts parseReference: name | name:address | name:address:attention. */
fun parseReference(leaf: String?): Triple<String, String?, Long?>? {
    if (leaf == null || leaf == "" || leaf.contains(' ')) return null
    val parts = leaf.split(":")
    var i = 0
    val nameSegs = mutableListOf<String>()
    while (i < parts.size && NAME_RE.matches(parts[i])) {
        nameSegs += parts[i]
        i++
    }
    if (nameSegs.isEmpty()) return null
    val name = nameSegs.joinToString(":")
    var address: String? = null
    var attention: Long? = null
    if (i < parts.size) {
        if (ADDR_RE.matches(parts[i])) {
            // Address 0 is the root with the spindle omitted, never the underscore step.
            address = if (parts[i] == "0") null else parts[i]
            i++
        } else return null
    }
    if (i < parts.size) {
        if (ATT_RE.matches(parts[i])) {
            attention = attentionOf(parts[i])
            i++
        } else return null
    }
    return if (i == parts.size) Triple(name, address, attention) else null
}

/** A leaf as a reference, the two address words included. Prose never parses. */
fun parseBundleRef(leaf: String?): BundleRef? {
    if (leaf == null) return null
    val s = jsTrim(leaf)
    if (s.isEmpty() || s.any(::isJsSpace) || s.length > 200) return null
    val parts = s.split(":")
    // The word form: name segments, then now | head, then at most an attention.
    for (i in 1 until parts.size) {
        val w = parts[i].lowercase()
        if (w != "now" && w != "head") continue
        val nameSegs = parts.subList(0, i)
        if (!nameSegs.all { NAME_RE.matches(it) }) break
        val rest = parts.subList(i + 1, parts.size)
        if (rest.size > 1 || (rest.size == 1 && !ATT_RE.matches(rest[0]))) break
        return BundleRef(s, nameSegs.joinToString(":"), null, w, if (rest.isNotEmpty()) attentionOf(rest[0]) else null)
    }
    val r = parseReference(s) ?: return null
    return BundleRef(s, r.first, r.second, null, r.third)
}

/** A digit child of a node: the string there (or the voicing of an object there), and that leaf as a reference. */
class BundleSlot(val slot: String, val leaf: String?, val ref: BundleRef?)

/** The digit children of a node as bundle slots, in address order. */
fun bundleSlots(node: Js?): List<BundleSlot> {
    if (node !is JsObj) return emptyList()
    return digitKeys(node).map { k ->
        val leaf = when (val v = node[k]) {
            is JsStr -> v.value
            is JsObj -> (v["_"] as? JsStr)?.value
            else -> null
        }
        BundleSlot(k, leaf, if (!leaf.isNullOrEmpty()) parseBundleRef(leaf) else null)
    }
}

/** An accumulator's entry carries its arrival stamp at field 3; an entry is never a bundle. */
fun isEntryNode(node: Js?): Boolean {
    val s = if (truthy(node) && isObject(node)) jsGet(node, "3") else null
    return s is JsStr && ENTRY_STAMP.containsMatchIn(s.value)
}

/** More than half of the non-empty digit children parse as references, and at least one does. */
fun looksLikeBundle(node: Js?): Boolean {
    if (node !is JsObj || isEntryNode(node)) return false
    val slots = bundleSlots(node).filter { it.leaf != null && jsTrim(it.leaf) != "" }
    val parsed = slots.count { it.ref != null }
    return parsed >= 1 && parsed * 2 > slots.size
}

/** The hand a block is of, read off its name: shell:weft is of weft; a collective, a grain or an archive is of nobody. */
fun handleOfBlock(blockName: String): String? {
    val parts = blockName.split(":")
    if (parts.size < 2) return null
    if (parts[0] in listOf("sed", "grain", "archive")) return null
    val h = parts.last()
    return if (h.isNotEmpty() && NAME_RE.matches(h)) h else null
}

// ── the clock cut ──

/** Digits two addresses share from the left. */
fun sharedDigits(a: String, b: String): Int {
    var n = 0
    while (n < a.length && n < b.length && a[n] == b[n]) n++
    return n
}

/** The fine digits a clock stamp carries beneath the beat: the minute, the breath and the second. */
const val FINE = 3

private fun tenAndFine(s: String): Pair<String, String> {
    val parts = s.filter { it in '0'..'9' || it == '.' }.split(".")
    return parts[0] to (parts.getOrNull(1) ?: "")
}

/** A stamp in the clock's canonical form: ten digits, a point, up to three fine digits. */
fun clockStamp(s: String): String {
    val (ten, fine) = tenAndFine(s)
    val f = jsSlice(fine, FINE).trimEnd('0')
    return ten.padEnd(TEMPORAL_FLOOR, '0').substring(0, TEMPORAL_FLOOR) + if (f.isNotEmpty()) ".$f" else ""
}

private fun clockDigits(s: String): List<Int> {
    val (ten, fine) = tenAndFine(s)
    return (ten.padEnd(TEMPORAL_FLOOR, '0').substring(0, TEMPORAL_FLOOR) + fine.padEnd(FINE, '0').substring(0, FINE)).map { it - '0' }
}

/** The coarsest pscale that changed between two stamps; null when they name the same second. */
fun divergencePscale(since: String, now: String): Int? {
    val n = sharedDigits(clockDigits(since).joinToString(""), clockDigits(now).joinToString(""))
    return if (n >= TEMPORAL_FLOOR + FINE) null else TEMPORAL_FLOOR - 1 - n
}

/** One thing standing on a clock block: a rung's voicing or a line said at its instant. */
class ClockLine(val address: String, val pscale: Int, val text: String, val stamp: String?)

private fun sayOf(n: Js?): String? {
    if (n is JsStr) return if (jsTrim(n.value) == "") null else n.value
    if (n is JsObj) {
        val u = n["_"]
        if (u is JsStr && jsTrim(u.value) != "") return u.value
    }
    return null
}

private fun stampOf(n: Js?): String? = if (truthy(n) && isObject(n)) (jsGet(n, "3") as? JsStr)?.value else null

private fun digitsOf(n: Js?): List<Int> = if (n is JsObj) digitKeys(n).map { it[0] - '0' } else emptyList()

private fun clockAddress(ds: List<Int>): String {
    val ten = ds.take(TEMPORAL_FLOOR).joinToString("").padEnd(TEMPORAL_FLOOR, '0')
    return if (ds.size > TEMPORAL_FLOOR) "$ten.${ds.drop(TEMPORAL_FLOOR).joinToString("")}" else ten
}

/**
 * Everything on a clock block strictly after [since] up to and including
 * [now], in clock order, to the second: the tail of since's own containers
 * (deepest first), the containers between, then now's own path.
 */
fun clockRange(block: Js, since: String, now: String): List<ClockLine> {
    val s0 = clockDigits(since)
    val n0 = clockDigits(now)
    val w = s0.size
    var k = 0
    while (k < w && s0[k] == n0[k]) k++
    if (k >= w || s0[k] > n0[k]) return emptyList()
    val out = mutableListOf<ClockLine>()
    fun nodeAt(ds: List<Int>): Js? = try { readAt(block, clockAddress(ds)) } catch (_: Exception) { null }
    // A node reached twice is voiced once: objects by identity, strings by value, as a JavaScript Set holds them.
    val seen = HashSet<Js>()
    fun push(ds: List<Int>, node: Js?) {
        if (node == null || node == JsNull || !seen.add(node)) return
        val text = sayOf(node)
        if (text != null) out += ClockLine(clockAddress(ds), TEMPORAL_FLOOR - ds.size, text, stampOf(node))
    }
    // A scalar beneath an entry is a field of the entry, never a line.
    fun whole(ds: List<Int>, node: Js?) {
        if (node == null || node == JsNull) return
        push(ds, node)
        val entry = stampOf(node) != null
        for (d in digitsOf(node)) {
            val c = jsGet(node, d.toString())
            if (entry && !isObject(c)) continue
            whole(ds + d, c)
        }
    }
    var s = w
    while (s > 0 && s0[s - 1] == 0) s--
    var j = minOf(w - 1, s - 1)
    while (j > k) {
        val parent = nodeAt(s0.subList(0, j))
        for (d in digitsOf(parent)) if (d > s0[j]) whole(s0.subList(0, j) + d, jsGet(parent, d.toString()))
        j--
    }
    val parentK = nodeAt(s0.subList(0, k))
    for (d in digitsOf(parentK)) if (d > s0[k] && d < n0[k]) whole(s0.subList(0, k) + d, jsGet(parentK, d.toString()))
    for (jj in k until w) {
        val parent = nodeAt(n0.subList(0, jj))
        if (!truthy(parent) || !isObject(parent)) break
        if (jj > k) for (d in digitsOf(parent)) if (d < n0[jj]) whole(n0.subList(0, jj) + d, jsGet(parent, d.toString()))
        val node = nodeAt(n0.subList(0, jj + 1))
        if (node == null || node == JsNull) break
        if (jj == w - 1) whole(n0, node) else push(n0.subList(0, jj + 1), node)
    }
    return out
}

/** The lines a range delivers; past the cap the rest are counted and the address to read on from is named. */
fun renderClockLines(lines: List<ClockLine>, cap: Int = 40): String {
    val shown = lines.take(cap).map { l -> "  [${l.address}] ${l.text}${if (!l.stamp.isNullOrEmpty()) " · ${l.stamp}" else ""}" }.toMutableList()
    if (lines.size > cap) shown += "  (… and ${lines.size - cap} more on the clock after ${lines[cap - 1].address} — read the block on from there)"
    return shown.joinToString("\n")
}

class CutWalk(val entries: List<PathWalkEntry>, val from: Int, val same: Boolean, val within: Boolean)

/** The rungs of a clock walk the reader does not already hold: those at or below the divergence. */
fun cutClockWalk(entries: List<PathWalkEntry>, since: String, now: String, touched: Boolean = false): CutWalk {
    val d = divergencePscale(since, now)
    if (d == null && !touched) return CutWalk(emptyList(), -1, same = true, within = false)
    val from = d ?: 0
    return CutWalk(entries.filter { (it.pscale ?: -1) <= from }, from, same = false, within = d == null)
}

private val RUNG_NAMES = listOf("beat", "gathering", "day", "week", "month", "season", "year", "decade", "century", "millennium")
private val FINE_NAMES = listOf("minute", "breath", "second")

fun rungName(pscale: Int): String =
    if (pscale < 0) FINE_NAMES.getOrNull(-pscale - 1) ?: "p$pscale" else RUNG_NAMES.getOrNull(pscale) ?: "p$pscale"

/** The newest entry of an accumulator; on a block not yet folded, its greatest digit; null with no digit children. */
fun headAddress(block: Js): String? {
    val floor = floorDepth(block)
    headOf(block, emptyList(), floor)?.let { return it }
    val kids = digitKeys(block)
    return if (kids.isNotEmpty()) fullWidthAddress(listOf(kids.last()[0]), floor) else null
}

// ── resolution ──

/**
 * What a bundle resolves against: [load] (the shelf first, then the surface),
 * the hand a bare name is tried as `<name>:<hand>` with first, the moment the
 * window is compiled at, and the reader's last stamp ([since]; null means
 * never here, and the whole is handed over).
 */
class BundleContext(
    val load: (String) -> Js?,
    val handle: String? = null,
    val nowMs: Long,
    val since: String? = null,
    val touched: Boolean = false,
)

class ClockCut(val from: Int, val since: String, val same: Boolean, val within: Boolean)

class Member(
    val slot: String,
    val leaf: String?,
    /** Null for prose, which rides through as written. */
    val ref: BundleRef?,
    /** The block name that resolved (or was tried first). */
    val name: String? = null,
    /** The address read: the stamp for now, the head for head, "" for the root. */
    val address: String? = null,
    val result: ReadResult? = null,
    /** On a clock member: the lines said this beat, or everything that landed since the reader's last stamp. */
    val lines: List<ClockLine>? = null,
    val cut: ClockCut? = null,
    val note: String? = null,
    val absent: Boolean = false,
)

/** A whole-block member larger than this is delivered as its disc at pscale 0. */
const val BUNDLE_WHOLE_CHARS = 6000

/** The names a reference may resolve to, in order. */
fun candidateNames(ref: BundleRef, handle: String?): List<String> =
    if (ref.name.contains(':') || handle.isNullOrEmpty()) listOf(ref.name) else listOf("${ref.name}:$handle", ref.name)

fun resolveMember(s: BundleSlot, ctx: BundleContext): Member {
    val ref = s.ref ?: return Member(s.slot, s.leaf, null)
    val names = candidateNames(ref, ctx.handle)
    var block: Js? = null
    var name: String? = null
    for (n in names) {
        block = try { ctx.load(n) } catch (_: Exception) { null }
        if (truthy(block)) { name = n; break }
    }
    if (!truthy(block) || name == null) return Member(s.slot, s.leaf, ref, name = names[0], absent = true)
    val b = block!!
    // The walker takes an Int; an attention past it reads at its edge.
    val att = ref.attention?.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())?.toInt()
    fun member(address: String? = null, result: ReadResult? = null, lines: List<ClockLine>? = null, cut: ClockCut? = null, note: String? = null) =
        Member(s.slot, s.leaf, ref, name, address, result, lines, cut, note)
    return try {
        when {
            ref.word == "now" -> {
                val floor = floorDepth(b)
                if (floor != TEMPORAL_FLOOR) {
                    member(note = "'now' names a beat on the clock, and $name is not on the clock (floor $floor) — dial a digit address instead")
                } else {
                    val stamp = momentToAddress(ctx.nowMs, FINE)
                    val beat = stamp.substring(0, TEMPORAL_FLOOR)
                    if (!ctx.since.isNullOrEmpty()) {
                        // The range: everything on the clock after the reader's last stamp, to the second.
                        val d = divergencePscale(ctx.since, stamp)
                        val lines = if (d == null) emptyList() else clockRange(b, ctx.since, stamp)
                        member(stamp, null, lines, ClockCut(d ?: -FINE, clockStamp(ctx.since), d == null, d != null && d < 0))
                    } else {
                        // Cold: the ladder down to the beat, and what was said in this beat so far.
                        val result = bspRead(b, beat, att)
                        member(stamp, result, clockRange(b, beat, stamp))
                    }
                }
            }
            ref.word == "head" -> {
                val head = headAddress(b)
                if (head == null) member(note = "$name holds no entries yet — nothing stands at its head")
                else member(head, bspRead(b, head, att))
            }
            ref.address == null && att == null -> {
                val size = stringify(b)!!.length
                if (size > BUNDLE_WHOLE_CHARS) {
                    member("", bspRead(b, "", 0), note = "the whole block is $size characters — delivered as its disc at pscale 0; dial an address to take a part whole")
                } else member("", bspRead(b, "", null))
            }
            else -> member(ref.address ?: "", bspRead(b, ref.address ?: "", att))
        }
    } catch (e: Exception) {
        member(note = "did not read: ${jsSlice(e.message ?: e.toString(), 160)}")
    }
}

/** Compile a bundle node: every slot resolved. Null when the node is not a bundle by form, or when no member stands. */
fun compileBundle(node: Js?, ctx: BundleContext): List<Member>? {
    if (!looksLikeBundle(node)) return null
    val members = bundleSlots(node).map { resolveMember(it, ctx) }
    return if (members.any { it.ref != null && !it.absent }) members else null
}

// ── rendering ──

private fun u(v: Any?): String = v?.toString() ?: "undefined"

/** A member's reading rendered whole: a disc's lines and a descent's entries untruncated; a walk and a point as the walker's formatter does. */
private fun renderResult(r: ReadResult): String = when (r.shape) {
    "disc" -> {
        val lines = mutableListOf("[disc @ pscale ${u(r.pscale)}]")
        for (e in r.discEntries ?: emptyList()) {
            lines += "[${e.address}]: ${e.content ?: "(no content)"}${if (!e.stamp.isNullOrEmpty()) " · ${e.stamp}" else ""}${if (e.beneath != null && e.beneath != 0) " · ${e.beneath} beneath" else ""}"
        }
        lines.joinToString("\n")
    }
    "path-walk+descent" -> {
        val lines = mutableListOf("[path-walk+descent @ \"${u(r.spindle)}\" pscale ${u(r.pscale)}]", "path-walk:")
        for (e in r.pathWalk ?: emptyList()) lines += "  d${e.depth} p${e.pscale} [${e.address}]: ${e.content ?: "(no content)"}"
        lines += "descent:"
        for (e in r.descent ?: emptyList()) lines += "  d${e.depth} p${e.pscale} [${e.address}]: ${e.content ?: "(no content)"}${if (!e.stamp.isNullOrEmpty()) " · ${e.stamp}" else ""}"
        lines.joinToString("\n")
    }
    else -> formatRead(r)
}

/** The body of a member — what stands beneath its header. */
fun renderMemberBody(m: Member): String {
    if (m.ref == null) return m.leaf ?: "(empty)"
    if (m.absent) return "(absent at this surface: ${m.name})"
    if (m.result == null && m.lines == null) return "(${m.note ?: "did not resolve"})"
    val out = mutableListOf<String>()
    if (m.note != null) out += "ⓘ ${m.note}"
    val cut = m.cut
    if (cut != null) {
        if (cut.same) {
            out += "(the same second as your last stamp ${cut.since} — nothing new on the clock)"
            return out.joinToString("\n")
        }
        out += if (cut.within) "(within this beat — what landed on the clock since your last stamp ${cut.since}, ${voiceAddress(cut.since)})"
        else "(from the ${rungName(cut.from)} down — what landed on the clock since your last stamp ${cut.since}, ${voiceAddress(cut.since)})"
    }
    m.result?.let { out += renderResult(it) }
    if (!m.lines.isNullOrEmpty()) {
        if (m.result != null) out += "said this beat:"
        out += renderClockLines(m.lines)
    } else if (cut != null) {
        out += "(nothing landed)"
    }
    return out.joinToString("\n")
}

fun renderMember(m: Member, indent: String = "  "): String {
    val head = "── ${m.slot} · ${m.leaf ?: "(empty)"} ──"
    val body = renderMemberBody(m).split("\n").joinToString("\n") { indent + it }
    return "$head\n$body"
}

/** A window carries this much from one bundle read; beyond it, the remaining members are named, never silently dropped. */
const val BUNDLE_WINDOW_CHARS = 30000

/** The window: the index first, then every member at its aperture. */
fun renderBundle(members: List<Member>, label: String, nowMs: Long, since: String? = null, cap: Int = BUNDLE_WINDOW_CHARS): String {
    val stamp = momentToAddress(nowMs)
    val refs = members.count { it.ref != null }
    val lines = mutableListOf(
        "[bundle @ $label — $refs reference${if (refs == 1) "" else "s"} compiled at $stamp (${voiceAddress(stamp)})" +
            "${if (!since.isNullOrEmpty()) "; what moved since ${clockStamp(since)}" else ""}; the index first, then each at its own aperture]",
        "  index: ${members.joinToString(" · ") { "${it.slot} ${if (it.ref != null) it.leaf else "(prose)"}" }}",
    )
    var used = lines.joinToString("\n").length
    val left = mutableListOf<String>()
    for (m in members) {
        val t = renderMember(m)
        if (used + t.length > cap) { left += "${m.slot} ${m.leaf ?: ""}"; continue }
        lines += t
        used += t.length + 1
    }
    if (left.isNotEmpty()) {
        lines += "  (the window is full at $cap characters — not unfolded: ${left.joinToString(" · ")}; read them at their addresses, or dial them narrower)"
    }
    lines += "  (a reference's address is a number, or now — the clock's current beat — or head — an accumulator's newest entry; re-dial by writing this node)"
    return lines.joinToString("\n")
}
