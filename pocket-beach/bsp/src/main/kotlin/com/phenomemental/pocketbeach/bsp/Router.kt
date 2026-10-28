package com.phenomemental.pocketbeach.bsp

/*
 * Router.kt — the bsp() handler, as bsp-mcp-server runs it.
 *
 * A port of the parts of src/tools/bsp.ts (handleBspInner), src/db.ts,
 * src/read-back.ts and src/pscale-wire.ts that ordinary blocks with no locks
 * and no encryption pass through, at the pinned commit. The beach sits behind
 * the same wire it does upstream: every request and answer crosses as JSON, so
 * the beach half sees exactly what an HTTP body would carry.
 *
 * Where upstream names a beach URL, the pocket's region is named "pocket".
 * Reads walk with the router's own walker, the path upstream takes for the
 * teaching blocks. A walk that lands on a bundle carries its window
 * (Bundle.kt), and an append to a block that shell:<hand>'s manifest dials
 * moves the manifest's reference with it, as upstream does for an append under
 * a secret — here with none, the phone being the latch. Left out by decision
 * (pocket-beach/STATUS.md): the clock line and the reflection; the orientation
 * rider rides only for a shell put on (Shells.kt, Seat.kt).
 */

class Call(
    val agentId: String,
    val block: String?,
    val spindle: String?,
    val pscaleAttention: Int?,
    /** null when the call carries no content (JavaScript undefined); JsNull when it carries null. */
    val content: Js?,
    val append: Boolean,
)

private class Rejected(message: String) : Exception(message)

class Router(private val beach: Beach, private val shelf: Shelf) {
    private val surface = beach.origin

    // ── the wire (pscale-wire.ts), in-process ──

    private fun wire(v: Js): Js = parseJson(stringify(v)!!)

    private class WireResult(val ok: Boolean, val status: Int?, val error: String?, val body: JsObj?)

    private fun answer(a: BeachAnswer): WireResult {
        val body = wire(a.body) as JsObj
        return if (a.status >= 400) WireResult(false, a.status, (body["error"] as? JsStr)?.value ?: "HTTP ${a.status}", body)
        else WireResult(true, a.status, null, body)
    }

    /** wire.loadBlock: the block, or null when the beach answers 404. */
    private fun wireLoad(name: String): Js? {
        val j: Js = if (name.isEmpty()) beach.index() else beach.loadBlock(name) ?: return null
        val back = wire(j)
        return if (back is JsObj && back.has("block")) back["block"] else back
    }

    private fun post(name: String, body: JsObj, nowMs: Long) = answer(beach.post(name, wire(body) as JsObj, nowMs))

    private fun beachRejection(kind: String, status: Int?, error: String?): Rejected {
        val base = "Beach $kind rejected: $error"
        val keyish = status == 403 || Regex("lock|secret|latch", RegexOption.IGNORE_CASE).containsMatchIn(error ?: "")
        if (!keyish) return Rejected(base)
        return Rejected(
            "$base — a refused key is usually a drifted spelling (often one capitalised first letter). " +
                "The recovery law: bsp(agent_id=\"https://beach.happyseaurchin.com\", block=\"ways:key\") — " +
                "the three-try rotation probe, then stop.",
        )
    }

    // ── db.ts ──

    private class Row(val block: Js)

    private fun loadBlock(sentinel: Boolean, name: String): Row? {
        if (sentinel) return shelf.load(name)?.let(::Row)
        val b = wireLoad(name) ?: return null
        return Row(if (b == JsNull) JsObj() else b)
    }

    private fun deriveSurgicalValue(block: Js, spindle: String, hasContent: Boolean): Js? {
        val value = readAt(block, spindle)
        if (hasContent && value == null) {
            val fl = floorDepth(block)
            val walk = try { parseSpindle(spindle, fl).digits.joinToString(",").ifEmpty { "(root)" } } catch (_: InvalidAddressError) { spindle }
            throw Rejected(
                "surgical write at \"$spindle\" resolves to digit walk $walk (floor $fl), where the " +
                    "merged block holds nothing — refusing the content-less POST a hollow ack rides on. " +
                    "The local merge landed elsewhere than this address; nothing was sent to the beach.",
            )
        }
        return value
    }

    /** saveBlockToBeach: whether the beach said this write made the block. */
    private fun saveBlock(name: String, block: Js, spindle: String, pscale: Int?, nowMs: Long): Boolean {
        val whole = spindle.isEmpty() || spindle == "*"
        if (whole) {
            val r = post(name, JsObj.of("content" to block, "confirm" to JsBool(true)), nowMs)
            if (!r.ok) throw beachRejection("save", r.status, r.error)
            val back = wireLoad(name)
            if (!deepEqual(back, block)) throw beachRejection("save", null, "write to $name at $surface did not read back identical")
            return r.body?.get("born") == JsBool(true)
        }
        val cleaned = spindle.removeSuffix("*")
        val value = deriveSurgicalValue(block, cleaned, true)
        val body = JsObj.of("spindle" to str(cleaned))
        body["content"] = value
        body["pscale_attention"] = pscale?.let { JsNum(it.toDouble()) } ?: JsNull
        val r = post(name, body, nowMs)
        if (!r.ok) throw beachRejection("save", r.status, r.error)
        return r.body?.get("born") == JsBool(true)
    }

    // ── read-back.ts ──

    private fun readBackWalk(sentinel: Boolean, name: String, spindle: String?): String {
        if (spindle.isNullOrEmpty()) return ""
        return try {
            val live = loadBlock(sentinel, name)?.block
            if (!isObject(live)) return ""
            val r = bspRead(live!!, spindle, null)
            if (r.shape != "path-walk") "" else "\n[read-back — \"$spindle\" as its next reader receives it]\n${formatRead(r)}"
        } catch (_: Exception) {
            ""
        }
    }

    private fun readBackAfterWrite(name: String, landed: String, content: Js?): String = try {
        val live = loadBlock(false, name)?.block
        if (!isObject(live)) "" else readBackWalk(false, name, readBackSpindle(live!!, landed, content))
    } catch (_: Exception) {
        ""
    }

    // ── tools/bsp.ts ──

    fun handle(c: Call, nowMs: Long): String {
        val sentinel = c.agentId == "pscale"
        val blockName = c.block ?: ""
        val content = c.content?.let(::normaliseContent)
        val agent = if (sentinel) "pscale" else surface

        if (sentinel && content != null) {
            return "\"pscale\" is a read-only sentinel. The bundled teaching blocks are server-fixed."
        }
        if (c.append) return appendBranch(agent, blockName, c.spindle, content, nowMs)
        if (content == null) return readBranch(sentinel, agent, blockName, c.spindle, c.pscaleAttention, nowMs)
        return writeBranch(agent, blockName, c.spindle, c.pscaleAttention, content, nowMs)
    }

    private fun appendBranch(agent: String, blockName: String, spindle: String?, content: Js?, nowMs: Long): String {
        if (content == null) return "Append rejected: append needs `content` (the entry to append)."
        if (content is JsStr && jsTrim(content.value) == "") {
            return "Append rejected: an empty entry is nothing to keep — the slot would be consumed and pool reads skip it as a tombstone."
        }
        val appendSpindle = spindle ?: ""
        var entry: Js = content
        var stampedNote = ""
        if (entry is JsStr) {
            val iso = isoString(nowMs).replace(Regex("""\.\d{3}Z$"""), "Z")
            entry = JsObj.of("_" to entry, "3" to str(iso))
            stampedNote = " · stamped $iso"
        }
        return try {
            val body = JsObj.of("append" to JsBool(true), "content" to entry)
            if (appendSpindle.isNotEmpty()) body["spindle"] = str(appendSpindle)
            val r = post(blockName, body, nowMs)
            if (!r.ok) throw beachRejection("append", r.status, r.error)
            val j = r.body!!
            val slot = j["slot"]?.takeUnless { it == JsNull }?.let(::jsString)
            val address = (j["address"] as? JsStr)?.value
            val node = (j["node"] as? JsStr)?.value
            val dues = (j["owed"] as? JsArr)?.items?.map { d -> jsString(jsGet(d, "slot")) to jsString(jsGet(d, "over")) } ?: emptyList()
            val bornNote = if (j["born"] == JsBool(true)) formatBorn(blockName) else ""
            val owed = if (dues.isNotEmpty()) {
                "\n  ⓘ summary owed: ${dues.take(3).joinToString(", ") { "${it.first} over ${it.second}" }}" +
                    (if (dues.size > 3) " (+${dues.size - 3} older)" else "") +
                    " — one line written at that address, in the open or gray, voices the container and leaves its entries untouched."
            } else ""
            var span = ""
            if (dues.isNotEmpty()) {
                try {
                    val row = loadBlock(false, blockName)
                    if (row != null && truthy(row.block)) span = spanLines(row.block, dues.take(3))
                } catch (_: Exception) {
                }
            }
            val landedAt = address ?: slot ?: ""
            val followed = if (landedAt.isNotEmpty()) bundleFollowsAppend(blockName, landedAt, nowMs) else ""
            val readBack = readBackWalk(false, blockName, address ?: slot)
            val supernested = truthy(j["supernested"])
            if (address != null) {
                val grew = if (supernested) "  ⤴ node supernested — the ladder continues within" else ""
                "[append @ \"$agent/$blockName\" → $address (slot ${slot ?: "?"} beneath node ${node ?: appendSpindle})$grew$stampedNote]$bornNote$owed$span$followed$readBack"
            } else {
                val grew = if (supernested) "  ⤴ supernested → floor ${jsString(j["floor"])}" else ""
                "[append @ \"$agent/$blockName\" → slot ${slot ?: "?"}$grew$stampedNote]$bornNote$owed$span$followed$readBack"
            }
        } catch (e: Rejected) {
            "Append rejected: ${e.message}"
        }
    }

    /**
     * The manifest follows a landing: when shell:<hand>'s manifest dials this
     * block of the same hand at an address, the reference moves to what just
     * landed. Upstream does this only for an append under a secret; the pocket
     * keeps none.
     */
    private fun bundleFollowsAppend(block: String, landed: String, nowMs: Long): String {
        val m = FOLLOWS.matchEntire(block) ?: return ""
        if (landed.isEmpty()) return ""
        val shellName = "shell:${m.groupValues[2]}"
        val row = try { loadBlock(false, shellName) } catch (_: Exception) { return "" } ?: return ""
        val manifest = jsGet(row.block, "3")
        if (!truthy(manifest) || !isObject(manifest)) return ""
        for (k in jsKeys(manifest)) {
            if (!(k.length == 1 && k[0] in '1'..'9')) continue
            val next = nextRef(jsGet(manifest, k), block, landed) ?: continue
            return try {
                val shell = deepCopy(row.block)
                writeAt(shell, str("3.$k"), str(next))
                saveBlock(shellName, shell, "3.$k", null, nowMs)
                "\n  ⟲ the bundle follows: $shellName 3.$k now dials $next"
            } catch (e: Exception) {
                "\n  ⟲ the bundle did not follow ($shellName 3.$k): ${e.message ?: e.toString()}"
            }
        }
        return ""
    }

    /** A walk that lands on a bundle node carries the bundle's window beneath it. */
    private fun bundleWindow(sentinel: Boolean, agent: String, blockName: String, block: Js, spindle: String?, nowMs: Long): String? {
        val node = try { readAt(block, spindle) } catch (_: Exception) { return null }
        if (!looksLikeBundle(node)) return null
        val load: (String) -> Js? =
            if (sentinel) { name -> try { shelf.load(name) } catch (_: Exception) { null } }
            else surfaceLoader(shelf) { name -> loadBlock(false, name)?.block }
        val members = compileBundle(node, BundleContext(load, handleOfBlock(blockName), nowMs)) ?: return null
        val at = jsTrim(spindle ?: "")
        return renderBundle(members, "\"$agent/$blockName\" $at", nowMs)
    }

    private fun readBranch(sentinel: Boolean, agent: String, blockName: String, spindle: String?, pscale: Int?, nowMs: Long): String {
        if (blockName.isEmpty()) {
            return if (sentinel) formatSentinelIndex() else formatBeachIndex(wireLoad("") as JsObj)
        }
        val row = loadBlock(sentinel, blockName) ?: return notFound(sentinel, agent, blockName)
        return try {
            val wholeNamed = jsTrim(spindle ?: "") == "0" && pscale == null
            val result = bspRead(row.block, if (wholeNamed) "" else (spindle ?: ""), pscale)
            val localText = formatRead(result)
            val probe = if (wholeNamed) null else probeInsteadOfWhole(row.block, spindle, pscale, localText)
            var text = probe ?: tooLarge(result, localText) ?: localText
            if (probe == null && result.shape == "path-walk") {
                bundleWindow(sentinel, agent, blockName, row.block, spindle, nowMs)?.let { text = "$localText\n$it" }
            }
            text
        } catch (e: InvalidAddressError) {
            "Read rejected: ${e.message}"
        }
    }

    private fun writeBranch(agent: String, blockName: String, spindle: String?, pscale: Int?, content: Js, nowMs: Long): String {
        val block: Js = loadBlock(false, blockName)?.block ?: JsObj()
        val nine = jsGet(block, "9")
        val hasKeyring = isObject(nine) && jsGet(nine, "_") == str(GROUP_KEYRING_MARKER)

        if (!spindle.isNullOrEmpty()) {
            try {
                val crossed = grayCrossing(block, spindle)
                if (crossed != null) {
                    return "Write rejected: \"$spindle\" descends into the gray envelope at $crossed — that address is inside ciphertext, not content, and writing there destroys the entry beyond recovery. To replace that entry, write AT $crossed. To add a new one, append (append:true), which carries gray and allocates its own slot."
                }
            } catch (e: InvalidAddressError) {
                return "Write rejected: ${e.message}"
            }
        }
        if (hasKeyring) return "Write rejected: group operations need enc_secret (the key that unwraps the shared group key)."

        val written = try {
            bspWrite(block, spindle ?: "", pscale, content)
        } catch (e: Exception) {
            return "Write rejected: ${e.message}"
        }
        val degrayed = content is JsStr && written.landed != null && shedGray(block, written.landed!!)

        val born = try {
            saveBlock(blockName, written.block, written.wireSpindle ?: spindle ?: "", pscale, nowMs)
        } catch (e: Rejected) {
            return "Write rejected: ${e.message}"
        } catch (e: InvalidAddressError) {
            return "Write rejected: ${e.message}"
        }
        val bornNote = if (born) formatBorn(blockName) else ""
        val degrayNote = if (degrayed) {
            "\n  ⓘ the open line takes the gray envelope's place at \"$spindle\" — the ciphertext is gone, so a keyed reader and an open one read this same line."
        } else ""
        val readBack = readBackAfterWrite(blockName, written.landed ?: (spindle ?: ""), content)
        return formatWrite(written) + degrayNote + bornNote + readBack
    }

    private fun notFound(sentinel: Boolean, agent: String, blockName: String): String =
        if (sentinel) "Block \"$agent/$blockName\" not found."
        else "Block \"$blockName\" not found at federated host $surface. The host is federated and serves /.well-known/pscale-beach, but does not host this block. If you intended a per-agent block, the canonical name is \"<role>:<handle>\" — e.g. shell:happyseaurchin, passport:happyseaurchin (per block-conventions branches 1, 2, 3 position 8)."

    private fun formatSentinelIndex(): String {
        val names = shelf.names()
        val lines = mutableListOf("[sentinel index @ \"pscale\"]")
        lines += "  ${names.size} bundled block${if (names.size == 1) "" else "s"} — read each via block=\"<name>\":"
        for (n in names) lines += "    • $n"
        return lines.joinToString("\n")
    }

    private fun formatBeachIndex(idx: JsObj): String {
        val origin = jsString(idx["origin"])
        val lines = mutableListOf("[beach index @ $origin]")
        (idx["_"] as? JsStr)?.let { if (it.value.isNotEmpty()) lines += "  ${it.value}" }
        val blocks = (idx["blocks"] as? JsArr)?.items?.map { jsString(it) } ?: emptyList()
        val bytes = idx["bytes"] as? JsObj
        if (blocks.isEmpty()) {
            lines += "  (no named blocks yet — empty surface)"
        } else {
            val sized = bytes != null && bytes.size > 0
            lines += "  ${blocks.size} block${if (blocks.size == 1) "" else "s"} — address each via block=\"<name>\"${if (sized) "; sizes are stored-JSON bytes — pick an aperture before the read" else ""}:"
            for (name in blocks) {
                val b = bytes?.get(name) as? JsNum
                lines += "    • $name${if (b != null) " (${fmtBlockBytes(b.value)})" else ""}"
            }
        }
        return lines.joinToString("\n")
    }
}

private val FOLLOWS = Regex("""^([a-z0-9-]+):([^:]+)$""", RegexOption.IGNORE_CASE)
// JavaScript's `.`: anything but a line terminator.
private val NEXT_REF = Regex("""^([^\n\r  ]*):(\d[\d.]*):(-?\d+)$""")

/** The manifest ref that follows a landing: '<block>:<addr>:<attention>' moved to what just landed; null when the ref is another block's or already there. */
fun nextRef(ref: Js?, block: String, landed: String): String? {
    if (ref !is JsStr || landed.isEmpty()) return null
    val pr = NEXT_REF.matchEntire(ref.value) ?: return null
    if (pr.groupValues[1] != block) return null
    val next = "$block:$landed:${pr.groupValues[3]}"
    return if (next == ref.value) null else next
}

const val WINDOW_CHARS = 30000
const val WHOLE_CHARS = 6000
const val GROUP_KEYRING_MARKER = "group-keyring"

fun fmtBlockBytes(n: Double): String {
    if (n < 1000) return "${jsNumberToString(n)}b"
    val k = n / 1000
    return if (k < 10) java.math.BigDecimal(k).setScale(1, java.math.RoundingMode.HALF_UP).toPlainString() + "k"
    else "${jsNumberToString(kotlin.math.floor(k + 0.5))}k"
}

/** A read no window can carry is refused at the door, with the shape that would serve named. */
fun tooLarge(result: ReadResult, text: String): String? {
    if (text.length <= WINDOW_CHARS) return null
    val n = text.length
    val entries = result.discEntries
    if (result.shape == "disc" && !entries.isNullOrEmpty()) {
        val addrs = entries.map { it.address }.filter { a -> a.isNotEmpty() && a.all { it in '0'..'9' } }
            .sortedWith(compareBy<String> { it.length }.thenBy { it })
        val last = addrs.lastOrNull() ?: ""
        val container = if (last.isNotEmpty()) last.dropLast(1) + "0" else ""
        val p = result.pscale ?: 0
        return "Read refused at the door: this disc is $n characters over ${entries.size} positions — more than a window carries, and a disc at pscale $p over an accumulator is every entry it ever took. " +
            (if (container.isNotEmpty()) "Read its latest container instead — spindle='$container', pscale_attention=${p - 1} — or walk the newest entry, spindle='$last' — " else "Read one container — spindle=<its zero slot>, one pscale finer — ") +
            "or walk the entry you need by its address: a path-walk frames it with the summaries above it."
    }
    val p = result.pscale
    return "Read refused at the door: this ${result.shape} is $n characters, more than a window carries. Walk a spindle (spindle=<address>) for the part you need, or read the disc one pscale coarser${if (p != null) " (pscale_attention=${p + 1})" else " (pscale_attention=1)"} to see what stands."
}

/** Both coordinates omitted on a block whose whole runs long reads as the probe, the disc at pscale 0. */
fun probeInsteadOfWhole(block: Js, spindle: String?, pscale: Int?, wholeText: String): String? {
    val noSpindle = spindle == null || jsTrim(spindle) == ""
    if (!noSpindle || pscale != null) return null
    if (wholeText.length <= WHOLE_CHARS) return null
    val disc = bspRead(block, "", 0)
    val discText = formatRead(disc)
    tooLarge(disc, discText)?.let { return it }
    return "$discText\n  [the whole block is ${wholeText.length} characters: both coordinates omitted gives this probe, the disc at pscale 0. To take it whole, name the root — spindle='0' with no attention; or walk a spindle for the part you need.]"
}

/** The owed span rides the ack: each entry a summary is owed over, by its opening line. */
fun spanLines(block: Js, dues: List<Pair<String, String>>): String {
    val out = mutableListOf<String>()
    val over = Regex("""^(\d+)-(\d+)$""")
    for ((slot, span) in dues) {
        val m = over.matchEntire(span) ?: continue
        val a = m.groupValues[1].toInt()
        val b = m.groupValues[2].toInt()
        if (!(b >= a && b - a < 12)) continue
        val width = m.groupValues[1].length
        val rows = mutableListOf<String>()
        for (k in a..b) {
            if (k % 10 == 0) continue
            val addr = k.toString().padStart(width, '0')
            val text = try {
                (bspRead(block, addr, null).walkEntries ?: emptyList()).lastOrNull()?.content ?: ""
            } catch (_: Exception) {
                ""
            }
            if (jsTrim(text).isNotEmpty()) rows += "      $addr ${jsSlice(collapseSpace(text), 140)}"
        }
        if (rows.isNotEmpty()) {
            out += "    the span of $slot, by its opening lines:"
            out += rows
        }
    }
    return if (out.isNotEmpty()) "\n" + out.joinToString("\n") else ""
}

/** Content a model sent as a JSON string is read as the structure it spells. */
fun normaliseContent(value: Js): Js = when (value) {
    is JsStr -> {
        val trimmed = jsTrim(value.value)
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                val parsed = parseJson(trimmed)
                if (isObject(parsed)) normaliseContent(parsed) else value
            } catch (_: JsonSyntaxError) {
                value
            }
        } else value
    }
    is JsArr -> JsArr(value.items.map { it?.let(::normaliseContent) }.toMutableList())
    is JsObj -> JsObj().also { o -> for (k in value.keys()) o[k] = value[k]?.let(::normaliseContent) }
    else -> value
}

private fun isGrayEnvelope(node: Js?): Boolean {
    if (!truthy(node) || !isObject(node)) return false
    if (jsGet(node, "1") !is JsStr || jsGet(node, "2") !is JsStr) return false
    val nine = jsGet(node, "9")
    return truthy(nine) && isObject(nine) && jsGet(nine, "_") == str("gray")
}

private fun grayMode(node: Js?): String? {
    if (!isGrayEnvelope(node)) return null
    val m = (jsGet(jsGet(node, "9"), "1") as? JsStr)?.value
    return if (m == "self" || m == "grain" || m == "group") m else null
}

private fun nodeAddress(digits: List<Char>, floor: Int): String {
    val short = floor - digits.size
    return formatAddress(digits, floor) + if (short > 0) "0".repeat(short) else ""
}

private fun grayCrossing(block: Js, address: String): String? {
    val floor = floorDepth(block)
    val digits = parseSpindle(address, floor).digits
    var node: Js? = block
    for (i in 0 until digits.size - 1) {
        node = jsGet(node, if (digits[i] == '0') "_" else digits[i].toString())
        if (isGrayEnvelope(node)) return nodeAddress(digits.subList(0, i + 1), floor)
    }
    return null
}

private fun shedGray(block: Js, address: String): Boolean {
    val node = readAt(block, address)
    if (grayMode(node) == null) return false
    for (k in listOf("1", "2", "9")) {
        when (node) {
            is JsObj -> node.remove(k)
            is JsArr -> node.items[k.toInt()] = null
            else -> {}
        }
    }
    return true
}
