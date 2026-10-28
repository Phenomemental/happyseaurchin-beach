package com.phenomemental.pocketbeach.bsp

/*
 * Seat.kt — the seat, and what moved since: a port of bsp-mcp-server's
 * src/orientation.ts at the pinned commit.
 *
 * A session that put a shell on holds a seat: the surface it stands at, the
 * hand, the node its shell dialed as the bundle, and the stamp of the last
 * reply it was served. On each later bsp() reply the rider compiles the
 * seat's bundle relative to that stamp and appends only what moved: a clock
 * member read at the current beat and cut at the divergence rung; any other
 * member only when its block was touched after the seat was last told. Then
 * the stamp advances.
 *
 * Upstream keeps one seat per MCP session in a map; here the one who wears
 * the shell holds its seat. The two reads the rider makes come through
 * [RiderIO], so it runs on the phone's own blocks and in tests on fixtures.
 */

/** The two reads the rider makes: a loader over the seat's surface, and the surface's touched stamps. */
interface RiderIO {
    fun load(origin: String): (String) -> Js?
    fun touched(origin: String): Map<String, String>?
}

/**
 * A loader over one surface: the teaching shelf's names first (the teaching is
 * the same everywhere), then the surface's own blocks, each cached for the
 * loader's life. Only an object is a block.
 */
fun surfaceLoader(shelf: Shelf, surface: (String) -> Js?): (String) -> Js? {
    val cache = HashMap<String, Js?>()
    val sentinels = shelf.names().toSet()
    return { name ->
        if (cache.containsKey(name)) cache[name]
        else {
            val b = try { if (name in sentinels) shelf.load(name) else surface(name) } catch (_: Exception) { null }
            b.takeIf { truthy(it) && isObject(it) }.also { cache[name] = it }
        }
    }
}

/** How long the touched stamps are kept before they are read again. */
const val INDEX_TTL_MS = 5_000L

class Seat internal constructor(
    val origin: String,
    val handle: String,
    /** The block and spindle of the bundle node the door compiled. */
    val block: String,
    val spindle: String,
    node: Js?,
    nowMs: Long,
) {
    var node: Js? = node
        private set
    /** The stamp of the last reply served — the clock's since. */
    var lastMs: Long = nowMs
        private set
    /** When the touched stamps were last read, and the stamps; the touched since. */
    private var indexAt = 0L
    private var toldAt = nowMs
    private var touched: Map<String, String>? = null
    /** The bundle block's touched stamp when [node] was read. */
    private var nodeStamp: String? = null

    /** The whole was just delivered: nothing is owed before now. */
    fun told(nowMs: Long) {
        lastMs = nowMs
        toldAt = nowMs
    }

    /** What moved since this seat was last told — "" when nothing did. Advances the seat's stamps. */
    fun rider(nowMs: Long, io: RiderIO): String {
        val now = momentToAddress(nowMs, FINE)
        val since = momentToAddress(lastMs, FINE)
        if (nowMs - indexAt >= INDEX_TTL_MS) {
            touched = io.touched(origin)
            indexAt = nowMs
        }
        val toldAt = toldAt
        fun touchedAfter(name: String): Boolean {
            val t = touched?.get(name)
            if (t.isNullOrEmpty()) return false
            val ms = parseIsoMs(t) ?: return false
            return ms > toldAt
        }
        val load = io.load(origin)
        // The bundle itself may have been re-dialed: re-read its node when its block moved.
        val bundleStamp = touched?.get(block)
        if (!bundleStamp.isNullOrEmpty() && bundleStamp != nodeStamp && (parseIsoMs(bundleStamp) ?: Long.MIN_VALUE) > toldAt) {
            val b = load(block)
            if (truthy(b)) {
                node = readAt(b!!, spindle)
                nodeStamp = bundleStamp
            }
        }
        val beatChanged = now.substring(0, TEMPORAL_FLOOR) != since.substring(0, TEMPORAL_FLOOR)
        val moved = mutableListOf<Member>()
        val quiet = mutableListOf<String>()
        for (s in bundleSlots(node)) {
            val ref = s.ref ?: continue
            val touchedHere = candidateNames(ref, handle).any(::touchedAfter)
            val changed = if (ref.word == "now") beatChanged || touchedHere else touchedHere
            if (!changed) { quiet += s.leaf!!; continue }
            val m = resolveMember(s, BundleContext(load, handle, nowMs, since, touchedHere))
            // An absent or unreadable member cannot have moved; a clock member whose range is empty moved nothing either.
            if (m.absent || m.cut?.same == true || (m.result == null && m.lines.isNullOrEmpty())) { quiet += s.leaf!!; continue }
            moved += m
        }
        lastMs = nowMs
        this.toldAt = indexAt
        if (moved.isEmpty()) return ""
        val lines = mutableListOf("\n[orientation — $handle: what moved since $since (${voiceAddress(since)})]")
        for (m in moved) lines += renderMember(m, "  ")
        if (quiet.isNotEmpty()) lines += "  (unchanged: ${quiet.joinToString(" · ")})"
        return lines.joinToString("\n")
    }

    /** A reply with what moved riding on it; the rider never breaks the reply it rides on. */
    fun ride(reply: String, nowMs: Long, io: RiderIO): String = try {
        reply + rider(nowMs, io)
    } catch (_: Exception) {
        reply
    }
}

/** The door: this shell dialed this node as its bundle. Null without a hand, as upstream declares no seat then. */
fun declareSeat(origin: String, handle: String, block: String, spindle: String, node: Js?, nowMs: Long): Seat? =
    if (handle.isEmpty()) null else Seat(origin, handle, block, spindle, node, nowMs)
