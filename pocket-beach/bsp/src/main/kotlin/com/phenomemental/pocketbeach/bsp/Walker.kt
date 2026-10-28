package com.phenomemental.pocketbeach.bsp

/*
 * Walker.kt — the walker, the shapes and the formatters.
 *
 * A line-by-line port of bsp-mcp-server's src/bsp.ts (the address parser and
 * walk primitives, itself a port of bsp2-star.py) and src/bsp-fn.ts (bspRead,
 * bspWrite, formatRead, formatWrite) at the pinned commit. Names follow the
 * originals so the two can be read side by side; the golden tests in
 * conformance/ hold this file to the original's output, character for
 * character.
 *
 * Left out by decision (pocket-beach/STATUS.md): the clock's "days ago / AHEAD"
 * tags on floor-10 addresses.
 */

class InvalidAddressError(message: String) : Exception(message)

class BspError(message: String) : Exception(message)

// ── src/bsp.ts ──

/** Follow the _._ chain to the semantic string; null when the chain ends in an object without `_`. */
fun collectUnderscore(node: Js?): String? {
    if (!isObject(node) || !jsIn(node, "_")) return null
    val v = jsGet(node, "_")
    if (v is JsStr) return v.value
    if (isObject(v)) return if (jsIn(v, "_")) collectUnderscore(v) else null
    return null
}

/** Follow the underscore chain until a string; the count is the floor. */
fun floorDepth(block: Js?): Int {
    var node = block
    var depth = 0
    while (isObject(node) && jsIn(node, "_")) {
        depth++
        node = jsGet(node, "_")
        if (node is JsStr) return depth
    }
    return depth
}

class ParsedAddress(val leftDigits: List<Char>, val rightDigits: List<Char>, val hadDot: Boolean)

fun parseAddress(text: String): ParsedAddress {
    val dotCount = text.count { it == '.' }
    if (dotCount > 1) {
        throw InvalidAddressError("\"$text\" has multiple decimal points ($dotCount); pscale addresses carry at most one (sunstone:1.5)")
    }
    val left: String
    val right: String
    if (dotCount == 1) {
        left = text.substringBefore('.')
        right = text.substringAfter('.')
    } else {
        left = text
        right = ""
    }
    for (ch in left + right) {
        if (ch < '0' || ch > '9') throw InvalidAddressError("\"$text\" contains non-digit character \"$ch\"")
    }
    return ParsedAddress(left.toList(), right.toList(), dotCount == 1)
}

class ParsedSpindle(val digits: List<Char>, val hasStar: Boolean)

fun parseSpindle(spindle: String?, floor: Int): ParsedSpindle {
    if (spindle == null || spindle == "") return ParsedSpindle(emptyList(), false)
    var s = spindle
    val hasStar = s.endsWith("*")
    if (hasStar) s = s.dropLast(1)
    if (s == "") return ParsedSpindle(emptyList(), hasStar)

    val p = parseAddress(s)
    var leftDigits = p.leftDigits
    if (floor >= 1 && p.hadDot && leftDigits.size > floor) {
        throw InvalidAddressError(
            "\"$s\" has ${leftDigits.size} digits left of decimal; exceeds floor " +
                "$floor (the dot anchors pscale 0 at the floor, so left-of-decimal " +
                "digits cannot exceed floor depth)",
        )
    }
    if (floor > 1 && leftDigits.size < floor) leftDigits = List(floor - leftDigits.size) { '0' } + leftDigits
    val digits = (leftDigits + p.rightDigits).toMutableList()
    while (digits.size > 1 && digits.last() == '0') digits.removeAt(digits.size - 1)
    return ParsedSpindle(digits, hasStar)
}

/** The value given to an address-taking function, as the original stringifies it. */
private fun addressText(address: Js?): String? = when (address) {
    null, JsNull -> null
    else -> jsString(address)
}

fun formatAddress(digits: List<Char>, floor: Int): String {
    var d = digits.toMutableList()
    while (d.size > 1 && d.last() == '0') d.removeAt(d.size - 1)
    if (d.isEmpty()) return ""
    if (d.size <= floor) {
        while (d.size > 1 && d[0] == '0') d = d.subList(1, d.size).toMutableList()
        return d.joinToString("")
    }
    var left = d.subList(0, floor).toMutableList()
    val right = d.subList(floor, d.size)
    while (left.size > 1 && left[0] == '0') left = left.subList(1, left.size).toMutableList()
    return left.joinToString("") + "." + right.joinToString("")
}

private fun keyOf(d: Char): String = if (d == '0') "_" else d.toString()

/** The terminus of the legacy walk (src/bsp.ts walk), the one the star write takes. */
fun walkTerminal(block: Js, digits: List<Char>): Js? {
    var node: Js? = block
    for (d in digits) {
        val key = keyOf(d)
        if (!isObject(node) || !jsIn(node, key)) break
        val target = jsGet(node, key)
        if (d == '0' && target is JsStr) break
        node = target
        if (node is JsStr) break
    }
    return node
}

/** Read the raw value at an address; null (undefined) when it does not resolve. */
fun readAt(block: Js, address: Js?): Js? {
    if (address == null || address == JsNull || address == str("") || address == str("_")) return jsGet(block, "_")
    val fl = floorDepth(block)
    val digits = parseSpindle(addressText(address), fl).digits
    if (digits.isEmpty()) return jsGet(block, "_")
    var node: Js? = block
    for (d in digits) {
        if (!truthy(node) || !isObject(node)) return null
        val key = keyOf(d)
        if (!jsIn(node, key)) return null
        node = jsGet(node, key)
    }
    return node
}

fun readAt(block: Js, address: String?): Js? = readAt(block, address?.let(::str))

/** Write a value at an address, creating intermediate nodes; a string in the way moves into the new node's `_`. */
fun writeAt(block: Js, address: Js?, value: Js?): Js {
    if (address == str("_") || address == str("") || address == null || address == JsNull) {
        jsSet(block, "_", value)
        return block
    }
    val fl = floorDepth(block)
    val digits = parseSpindle(addressText(address), fl).digits
    if (digits.isEmpty()) {
        jsSet(block, "_", value)
        return block
    }
    var node: Js? = block
    for (i in 0 until digits.size - 1) {
        val key = keyOf(digits[i])
        val existing = jsGet(node, key)
        if (existing is JsStr) {
            jsSet(node, key, JsObj.of("_" to existing))
        } else if (!jsIn(node, key) || !isObject(existing)) {
            jsSet(node, key, JsObj())
        }
        node = jsGet(node, key)
    }
    jsSet(node, keyOf(digits.last()), value)
    return block
}

// ── src/bsp-fn.ts: results ──

class PathWalkEntry(val address: String, val depth: Int, val pscale: Int?, val content: String?, val stamp: String?)

class DiscEntry(val address: String, val content: String?, val stamp: String?, val beneath: Int?)

class ReadResult(val shape: String) {
    var floor: Int? = null
    var block: Js? = null
    var walkEntries: List<PathWalkEntry>? = null
    var discEntries: List<DiscEntry>? = null
    var pathWalk: List<PathWalkEntry>? = null
    var descent: List<PathWalkEntry>? = null
    var spindle: String? = null
    var pscale: Int? = null
    var depth: Int? = null
    var address: String? = null
    var content: String? = null
    var stamp: String? = null
    var note: String? = null
    var targetDepth: Int? = null
    var semantic: String? = null
    var inner: ReadResult? = null
    var beneath: List<String>? = null
    var beneathPscale: Int? = null
    var padding: String? = null
    var head: String? = null
}

class WriteResult(val shape: String, val block: Js) {
    var spindle: String = ""
    var pscaleAttention: Int? = null
    var warning: String? = null
    var landed: String? = null
    var wireSpindle: String? = null
    var landingNote: String? = null
}

class SplitStar(val pre: String?, val post: String?, val hasStar: Boolean)

fun splitStar(spindle: String?): SplitStar {
    if (spindle == null) return SplitStar(null, null, false)
    if (!spindle.contains('*')) return SplitStar(spindle, null, false)
    val parts = spindle.split('*')
    if (parts.size != 2) throw InvalidAddressError("\"$spindle\": star operator appears more than once")
    return SplitStar(parts[0].ifEmpty { null }, parts[1].ifEmpty { null }, true)
}

fun pscaleAt(depth: Int, floor: Int): Int? = if (depth == 0) null else floor - depth

fun depthAt(pscale: Int, floor: Int): Int = floor - pscale

private fun walkDigits(block: Js?, digits: List<Char>): Js? {
    var node = block
    for (d in digits) {
        val key = keyOf(d)
        if (!truthy(node) || !isObject(node) || !jsIn(node, key)) return null
        node = jsGet(node, key)
    }
    return node
}

private fun semantic(node: Js?): String? = when (node) {
    is JsStr -> node.value
    is JsNum, is JsBool -> stringify(node)
    else -> if (isObject(node)) collectUnderscore(node) else null
}

// ── src/bsp-fn.ts: read ──

private val DOTLESS = Regex("^[1-9][0-9]*$")

fun bspRead(block: Js, spindle: String?, pscaleAttention: Int?): ReadResult {
    val floor = floorDepth(block)

    val star = splitStar(spindle)
    if (star.hasStar) {
        val preDigits = if (star.pre != null) parseSpindle(star.pre, floor).digits else emptyList()
        val terminus = walkDigits(block, preDigits)
        var sem: String? = null
        var inner: ReadResult? = null
        if (truthy(terminus) && isObject(terminus)) {
            sem = collectUnderscore(terminus)
            val hidden = jsGet(terminus, "_")
            if (truthy(hidden) && isObject(hidden)) inner = bspRead(hidden!!, star.post, pscaleAttention)
        } else if (terminus is JsStr) {
            sem = terminus.value
        }
        return ReadResult("star").also { it.floor = floor; it.spindle = spindle; it.semantic = sem; it.inner = inner }
    }

    val digits = parseSpindle(spindle, floor).digits
    val padding = if (spindle != null && DOTLESS.matches(spindle) && spindle.length < floor) {
        "\"$spindle\" is shorter than the floor ($floor) and was read as ${spindle.padStart(floor, '0')}, down the root's underscore chain — the container of the ${spindle}0s is ${spindle.padEnd(floor, '0')}"
    } else null

    if (digits.isEmpty() && pscaleAttention == null) {
        return ReadResult("block").also { it.floor = floor; it.block = block }
    }

    if (digits.isEmpty()) {
        val target = depthAt(pscaleAttention!!, floor)
        return ReadResult("disc").also {
            it.floor = floor; it.pscale = pscaleAttention; it.targetDepth = target
            it.discEntries = collectDisc(block, target, floor)
        }
    }

    val pEnd = floor - digits.size

    if (pscaleAttention == null) {
        val terminus = walkDigits(block, digits)
        val kids = ringBeneath(terminus)
        val head = if (kids.isNotEmpty()) headOf(terminus, digits, floor) else null
        return ReadResult("path-walk").also {
            it.floor = floor; it.spindle = spindle
            it.walkEntries = buildPathWalk(block, digits, floor)
            if (kids.isNotEmpty()) { it.beneath = kids.map { k -> fullWidthAddress(digits + k[0], floor) }; it.beneathPscale = pEnd - 1 }
            it.head = head
            it.padding = padding
        }
    }

    if (pscaleAttention >= pEnd) {
        val target = depthAt(pscaleAttention, floor)
        if (target < 1 || target > digits.size) {
            return ReadResult("point").also {
                it.floor = floor; it.spindle = spindle; it.pscale = pscaleAttention
                it.note = "pscale $pscaleAttention is off the spindle (depth $target)"
            }
        }
        val prefix = digits.subList(0, target)
        val node = walkDigits(block, prefix)
        val kids = ringBeneath(node)
        val head = if (kids.isNotEmpty()) headOf(node, prefix, floor) else null
        return ReadResult("point").also {
            it.floor = floor; it.spindle = spindle; it.pscale = pscaleAttention; it.depth = target
            it.address = fullWidthAddress(prefix, floor)
            it.content = semantic(node)
            it.stamp = entryStamp(node)
            if (kids.isNotEmpty()) { it.beneath = kids.map { k -> fullWidthAddress(prefix + k[0], floor) }; it.beneathPscale = pscaleAttention - 1 }
            it.head = head
            it.padding = padding
        }
    }

    val target = depthAt(pscaleAttention, floor)
    val layers = target - digits.size
    val terminus = walkDigits(block, digits)
    return ReadResult("path-walk+descent").also {
        it.floor = floor; it.spindle = spindle; it.pscale = pscaleAttention
        it.pathWalk = buildPathWalk(block, digits, floor)
        it.descent = collectDescent(terminus, digits, floor, layers)
        it.padding = padding
    }
}

fun fullWidthAddress(digits: List<Char>, floor: Int): String {
    if (digits.size <= floor) return digits.joinToString("").padEnd(floor, '0')
    return digits.subList(0, floor).joinToString("") + "." + digits.subList(floor, digits.size).joinToString("")
}

private fun digitChildren(node: Js?): List<String> = if (!truthy(node) || !isObject(node)) emptyList() else digitKeys(node)

private fun ringBeneath(node: Js?): List<String> {
    val kids = digitChildren(node)
    return if (entryStamp(node) != null) kids.filter { it != "3" } else kids
}

private fun isPlainObj(v: Js?): Boolean = v is JsObj

fun headOf(node: Js?, digits: List<Char>, floor: Int): String? {
    if (!truthy(node) || !isObject(node)) return null
    val containers = digitChildren(node).filter { k ->
        val c = jsGet(node, k)
        isPlainObj(c) && entryStamp(c) == null && digitChildren(c).any { j -> isPlainObj(jsGet(c, j)) }
    }
    if (containers.isEmpty()) return null
    var cur: Js? = node
    val path = digits.toMutableList()
    while (true) {
        val ks = digitChildren(cur)
        if (ks.isEmpty()) break
        val k = ks.last()
        val next = jsGet(cur, k)
        path.add(k[0])
        if (!isPlainObj(next) || entryStamp(next) != null || !digitChildren(next).any { j -> isPlainObj(jsGet(next, j)) }) break
        cur = next
    }
    return fullWidthAddress(path, floor)
}

private fun deepestChain(node: Js?): List<Char> {
    if (!truthy(node) || !isObject(node)) return emptyList()
    var best = emptyList<Char>()
    for (k in digitChildren(node)) {
        val chain = listOf(k[0]) + deepestChain(jsGet(node, k))
        if (chain.size > best.size) best = chain
    }
    return best
}

/** The spindle a write's read-back walks: the landed address extended by the payload's deepest chain. */
fun readBackSpindle(block: Js, landed: String?, content: Js?): String? {
    val floor = floorDepth(block)
    var base = emptyList<Char>()
    if (!landed.isNullOrEmpty()) {
        base = try { parseSpindle(landed, floor).digits } catch (e: InvalidAddressError) { return null }
    }
    val digits = base + deepestChain(content)
    if (digits.isEmpty()) return null
    return fullWidthAddress(digits, floor)
}

private val LEADING_ISO = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})?""")

private fun entryStamp(node: Js?): String? {
    val v = if (truthy(node) && isObject(node)) jsGet(node, "3") else null
    if (v !is JsStr) return null
    return LEADING_ISO.find(v.value)?.value
}

private fun buildPathWalk(block: Js, digits: List<Char>, floor: Int): List<PathWalkEntry> =
    (1..digits.size).map { i ->
        val prefix = digits.subList(0, i)
        val node = walkDigits(block, prefix)
        PathWalkEntry(fullWidthAddress(prefix, floor), i, pscaleAt(i, floor), semantic(node), entryStamp(node))
    }

private fun collectDisc(block: Js, targetDepth: Int, floor: Int): List<DiscEntry> {
    if (targetDepth < 1) return emptyList()
    val results = mutableListOf<DiscEntry>()
    fun recurse(node: Js?, depth: Int, walked: List<Char>) {
        if (depth == targetDepth) {
            val onChainIntermediate = walked.isNotEmpty() && walked.all { it == '0' } && isObject(node)
            if (!onChainIntermediate) {
                val ring = ringBeneath(node).size
                results.add(DiscEntry(fullWidthAddress(walked, floor), semantic(node), entryStamp(node), if (ring > 0) ring else null))
            }
            return
        }
        if (!truthy(node) || !isObject(node)) return
        if (jsIn(node, "_")) {
            val u = jsGet(node, "_")
            if (truthy(u) && isObject(u)) {
                recurse(u, depth + 1, walked + '0')
            } else if (u is JsStr) {
                val onFloorChain = walked.all { it == '0' }
                if (onFloorChain && depth + 1 == targetDepth) {
                    results.add(DiscEntry(fullWidthAddress(walked + '0', floor), u.value, null, null))
                }
            }
        }
        for (d in "123456789") {
            if (jsIn(node, d.toString())) recurse(jsGet(node, d.toString()), depth + 1, walked + d)
        }
    }
    recurse(block, 0, emptyList())
    return results
}

private fun collectDescent(terminus: Js?, walked: List<Char>, floor: Int, layers: Int): List<PathWalkEntry> {
    val results = mutableListOf<PathWalkEntry>()
    if (layers <= 0 || !truthy(terminus) || !isObject(terminus)) return results
    var frontier = listOf(terminus to walked)
    for (layer in 1..layers) {
        val next = mutableListOf<Pair<Js?, List<Char>>>()
        for ((node, path) in frontier) {
            if (!truthy(node) || !isObject(node)) continue
            for (d in "123456789") {
                if (jsIn(node, d.toString())) {
                    val child = jsGet(node, d.toString())
                    val childDepth = path.size + 1
                    results.add(PathWalkEntry(fullWidthAddress(path + d, floor), childDepth, pscaleAt(childDepth, floor), semantic(child), entryStamp(child)))
                    if (truthy(child) && isObject(child)) next.add(child to (path + d))
                }
            }
        }
        frontier = next
    }
    return results
}

// ── src/bsp-fn.ts: write ──

private val SPACED_OPEN = Regex("^[\\u0009-\\u000D\\u0020\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]*[\\{\\[]")

fun bspWrite(block: Js, spindle: String?, pscaleAttention: Int?, content: Js?): WriteResult {
    val floor = floorDepth(block)
    val parsed = parseSpindle(spindle, floor)
    val digits = parsed.digits

    if (parsed.hasStar) {
        val terminal = walkTerminal(block, digits)
        if (!truthy(terminal) || !isObject(terminal)) throw BspError("Star write: terminus at \"$spindle\" is not an object")
        val u = jsGet(terminal, "_")
        if (!jsIn(terminal, "_") || !(isObject(u) || u == JsNull)) {
            val old = if (u is JsStr) u.value else ""
            jsSet(terminal, "_", JsObj.of("_" to str(old)))
        }
        val innerBlock = jsGet(terminal, "_")!!
        bspWriteInPlace(innerBlock, "", pscaleAttention, content)
        return WriteResult("star", block).also { it.spindle = spindle ?: ""; it.pscaleAttention = pscaleAttention }
    }

    val (shape, landedDigits) = bspWriteInPlace(block, spindle ?: "", pscaleAttention, content)
    val result = WriteResult(shape, block).also { it.spindle = spindle ?: ""; it.pscaleAttention = pscaleAttention }
    if (landedDigits != null && digits.isNotEmpty()) {
        result.landed = fullWidthAddress(landedDigits, floor)
        if (landedDigits.size != digits.size) {
            result.wireSpindle = result.landed
            result.landingNote = "pscale $pscaleAttention addresses depth ${landedDigits.size} of the spindle — " +
                "the write landed at \"${result.landed}\", not at the terminus of \"$spindle\". " +
                "To write the terminus, omit pscale_attention or set it to ${floor - digits.size}."
        } else if (landedDigits[0] == '0') {
            var addedByPad = false
            try {
                val left = parseAddress((spindle ?: "").removeSuffix("*")).leftDigits
                addedByPad = floor > 1 && left.size < floor
            } catch (_: InvalidAddressError) {
            }
            val firstNonZero = landedDigits.indexOfFirst { it != '0' }
            if (addedByPad && firstNonZero > 0) {
                val stripped = landedDigits.subList(firstNonZero, landedDigits.size)
                result.landingNote = "\"$spindle\" walks ${landedDigits.joinToString(",")} at floor $floor — left-of-decimal pads to " +
                    "floor width, so the walk enters the root underscore chain (digit 0 = \"_\"). " +
                    "If you meant the branch walk ${stripped.joinToString(",")}, that address is " +
                    "\"${fullWidthAddress(stripped, floor)}\"."
            }
        }
    }
    if (shape == "point" && content is JsStr && SPACED_OPEN.containsMatchIn(content.value)) {
        try {
            parseJson(content.value)
            result.warning = "Stored as a string-leaf at \"$spindle\". The content parses as JSON — " +
                "if you meant a subtree, pass the OBJECT (not a JSON-encoded string)."
        } catch (_: JsonSyntaxError) {
        }
    }
    return result
}

/** Object.assign(target, source) for the JSON shapes a write can carry. */
private fun assignInto(target: Js, source: Js) {
    for (k in jsKeys(source)) jsSet(target, k, jsGet(source, k))
}

private fun bspWriteInPlace(block: Js, spindle: String, pscaleAttention: Int?, content: Js?): Pair<String, List<Char>?> {
    val floor = floorDepth(block)
    val digits = parseSpindle(spindle, floor).digits

    if (digits.isEmpty()) {
        if (pscaleAttention == null) {
            if (!truthy(content) || !isObject(content)) throw BspError("Whole-block write requires an object payload")
            if (!isObject(block)) {
                val kind = if (block == JsNull) "null" else jsTypeof(block)
                throw BspError(
                    "Whole-block write requires an object root; existing block is $kind. " +
                        "If healing a malformed block, DELETE first then write fresh.",
                )
            }
            when (block) {
                is JsObj -> block.clear()
                is JsArr -> { block.items.clear(); block.props.clear() }
                else -> {}
            }
            assignInto(block, content!!)
            return "block" to null
        }
        when {
            content is JsArr -> {
                for (entry in content.items) {
                    if (truthy(entry) && isObject(entry) && jsIn(entry, "address")) writeAt(block, jsGet(entry, "address"), jsGet(entry, "content"))
                }
            }
            truthy(content) && isObject(content) -> {
                for (k in jsKeys(content)) writeAt(block, str(k), jsGet(content, k))
            }
            else -> throw BspError("Disc write requires array of {address, content} or sparse object")
        }
        return "disc" to null
    }

    val pEnd = floor - digits.size
    val pAtt = pscaleAttention ?: if (content != JsNull && isObject(content)) pEnd - 1 else pEnd

    if (pAtt >= pEnd) {
        if (content !is JsStr) throw BspError("Point write requires a string payload (got ${jsTypeof(content)})")
        val target = depthAt(pAtt, floor)
        val useDigits = if (target >= 1 && target <= digits.size) digits.subList(0, target) else digits
        val parent = walkOrCreate(block, useDigits.dropLast(1))
        val key = keyOf(useDigits.last())
        val existing = jsGet(parent, key)
        if (jsIn(parent, key) && existing != null && existing != JsNull && isObject(existing)) {
            jsSet(existing, "_", content)
        } else {
            jsSet(parent, key, content)
        }
        return "point" to useDigits
    }

    if (!isObject(content)) throw BspError("Subtree (path-walk+descent) write requires an object payload")
    val parent = walkOrCreate(block, digits.dropLast(1))
    jsSet(parent, keyOf(digits.last()), content)
    return "path-walk+descent" to digits
}

private fun walkOrCreate(block: Js, digits: List<Char>): Js {
    var node: Js = block
    for (d in digits) {
        val key = keyOf(d)
        val existing = jsGet(node, key)
        if (existing is JsStr) {
            jsSet(node, key, JsObj.of("_" to existing))
        } else if (!jsIn(node, key) || !isObject(existing)) {
            jsSet(node, key, JsObj())
        }
        node = jsGet(node, key)!!
    }
    return node
}

// ── src/bsp-fn.ts: formatters ──

private fun truncate(s: String, max: Int): String = if (s.length > max) jsSlice(s, max) + "..." else s

private fun floorOf(r: ReadResult): Int? {
    r.floor?.let { return it }
    for (e in (r.pathWalk ?: emptyList()) + (r.walkEntries ?: emptyList()) + (r.descent ?: emptyList())) {
        if (e.pscale != null) return e.depth + e.pscale
    }
    if (r.depth != null && r.pscale != null) return r.depth!! + r.pscale!!
    if (r.targetDepth != null && r.pscale != null) return r.targetDepth!! + r.pscale!!
    return null
}

private val LABEL = Regex("""^\d+(\.\d+)?$""")
private val ZEROS = Regex("^0*$")

private fun anchoredLabel(address: String, floor: Int?, pscale: Int?, walked: List<Char>?): String {
    if (floor == null || pscale == null || !LABEL.matches(address)) return address
    val depth = floor - pscale
    if (depth < 1) return address
    val left = address.substringBefore('.')
    val right = if (address.contains('.')) address.substringAfter('.') else null
    var digits: List<Char>? = null
    if (right != null) {
        if (left.length <= floor && depth > floor && right.length <= depth - floor) {
            digits = (left.padStart(floor, '0') + right.padEnd(depth - floor, '0')).toList()
        }
    } else if (left.length == floor && depth <= floor && ZEROS.matches(left.substring(minOf(depth, left.length)))) {
        digits = left.substring(0, depth).toList()
    } else if (left.length <= minOf(depth, floor)) {
        val spare = depth - left.length
        val lead = minOf(depth, floor) - left.length
        val ks = (0..lead).map { i -> if (floor == 10) i else lead - i }
        val cands = ks.map { k -> ("0".repeat(k) + left + "0".repeat(spare - k)).toList() }
        digits = cands.firstOrNull { c -> walked.isNullOrEmpty() || c.indices.all { i -> i >= walked.size || c[i] == walked[i] } } ?: cands[0]
    }
    return if (digits != null) fullWidthAddress(digits, floor) else address
}

private fun walkedDigits(r: ReadResult, floor: Int?): List<Char> {
    val s = r.spindle
    if (floor == null || s.isNullOrEmpty()) return emptyList()
    return try { parseSpindle(s, floor).digits } catch (e: InvalidAddressError) { emptyList() }
}

private fun addrLabel(address: String, floor: Int?, entryPscale: Int?, walked: List<Char>?): String =
    "[${anchoredLabel(address, floor, entryPscale, walked)}]"

private fun stampSuffix(stamp: String?): String = if (stamp != null) " · $stamp" else ""

private fun paddingLine(r: ReadResult): List<String> = r.padding?.let { listOf("  [note] $it") } ?: emptyList()

private fun beneathLine(r: ReadResult): String {
    val fl = floorOf(r)
    val walked = walkedDigits(r, fl)
    val labels = (r.beneath ?: emptyList()).joinToString(" · ") { anchoredLabel(it, fl, r.beneathPscale, walked) }
    return "  beneath (pscale ${r.beneathPscale}): $labels${r.head?.let { " · head $it" } ?: ""}"
}

const val WHOLE_RUNG = 400

private fun walkLines(entries: List<PathWalkEntry>, fl: Int?, walked: List<Char>, indent: String): List<String> {
    val out = mutableListOf<String>()
    val run = mutableListOf<PathWalkEntry>()
    fun label(e: PathWalkEntry) = addrLabel(e.address, fl, e.pscale, walked)
    fun flush() {
        if (run.isEmpty()) return
        if (run.size == 1) {
            val e = run[0]
            out.add("${indent}d${e.depth} p${e.pscale} ${label(e)}: (no content)")
        } else {
            val a = run.first()
            val b = run.last()
            out.add("${indent}d${a.depth}–d${b.depth} p${a.pscale}–p${b.pscale} ${label(a)} … ${label(b)}: (no content, ${run.size} rungs)")
        }
        run.clear()
    }
    entries.forEachIndexed { i, e ->
        val last = i == entries.size - 1
        val empty = e.content == null || jsTrim(e.content) == ""
        if (empty && !last) {
            run.add(e)
            return@forEachIndexed
        }
        flush()
        val content = if (empty) "(no content)" else e.content!!
        val text = if (last || content.length <= WHOLE_RUNG) content else truncate(content, 150)
        out.add("${indent}d${e.depth} p${e.pscale} ${label(e)}: $text${stampSuffix(e.stamp)}")
    }
    flush()
    return out
}

private const val UNFRAMED = "  (a rung above with no content is a summary slot not yet paid: this entry stands without its frame — block-conventions:3.5)"

private fun unframed(entries: List<PathWalkEntry>, floor: Int): Boolean =
    floor >= 2 && entries.dropLast(1).any { jsTrim(it.content ?: "") == "" }

fun formatRead(r: ReadResult): String = when (r.shape) {
    "block" -> "[whole block]\n${stringifyPretty(r.block!!)}"
    "path-walk" -> {
        val lines = mutableListOf("[path-walk @ \"${r.spindle}\"]")
        lines += paddingLine(r)
        val entries = r.walkEntries ?: emptyList()
        val fl = floorOf(r)
        lines += walkLines(entries, fl, walkedDigits(r, fl), "  ")
        if (unframed(entries, fl ?: 0)) lines += UNFRAMED
        if (!r.beneath.isNullOrEmpty()) lines += beneathLine(r)
        lines.joinToString("\n")
    }
    "disc" -> {
        val lines = mutableListOf("[disc @ pscale ${r.pscale} (depth ${r.targetDepth})]")
        val fl = floorOf(r)
        for (e in r.discEntries ?: emptyList()) {
            lines += "  ${addrLabel(e.address, fl, r.pscale, null)}: ${truncate(e.content ?: "(no content)", 150)}${stampSuffix(e.stamp)}${e.beneath?.let { " · $it beneath" } ?: ""}"
        }
        lines.joinToString("\n")
    }
    "point" -> {
        if (r.note != null) {
            "[point @ pscale ${r.pscale}] ${r.note}"
        } else {
            val fl = floorOf(r)
            "[point @ pscale ${r.pscale} depth ${r.depth} ${addrLabel(r.address.toString(), fl, r.pscale, walkedDigits(r, fl))}]" +
                paddingLine(r).joinToString("") { "\n$it" } +
                "\n  ${r.content ?: "(no content)"}${stampSuffix(r.stamp)}" +
                (if (!r.beneath.isNullOrEmpty()) "\n${beneathLine(r)}" else "")
        }
    }
    "path-walk+descent" -> {
        val lines = mutableListOf("[path-walk+descent @ \"${r.spindle}\" pscale ${r.pscale}]")
        lines += paddingLine(r)
        lines += "  path-walk:"
        val pw = r.pathWalk ?: emptyList()
        val fl = floorOf(r)
        val walked = walkedDigits(r, fl)
        lines += walkLines(pw, fl, walked, "    ")
        if (unframed(pw, fl ?: 0)) lines += UNFRAMED
        lines += "  descent:"
        for (e in r.descent ?: emptyList()) {
            lines += "    d${e.depth} p${e.pscale} ${addrLabel(e.address, fl, e.pscale, walked)}: ${truncate(e.content ?: "", 150)}${stampSuffix(e.stamp)}"
        }
        lines.joinToString("\n")
    }
    "star" -> {
        val lines = mutableListOf("[star @ \"${r.spindle}\"]")
        if (!r.semantic.isNullOrEmpty()) lines += "  semantic: ${truncate(r.semantic!!, 200)}"
        val inner = r.inner
        if (inner != null) {
            lines += "  inner shape: ${inner.shape}"
            for (line in formatRead(inner).split('\n')) lines += "    $line"
        } else {
            lines += "  (no hidden directory)"
        }
        lines.joinToString("\n")
    }
    else -> "[error] (no message)"
}

fun formatBorn(blockName: String): String =
    "\n  ⓘ new: this write created \"$blockName\" — tell your person; a slip can be set aside."

fun formatWrite(r: WriteResult): String {
    val landedTag = if (r.landed != null && r.landed != r.spindle) " → landed at \"${r.landed}\"" else ""
    val head = "[wrote ${r.shape} @ \"${r.spindle}\"${if (r.pscaleAttention != null) " pscale ${r.pscaleAttention}" else ""}$landedTag]"
    val lines = mutableListOf(head)
    r.landingNote?.let { lines += "[note] $it" }
    r.warning?.let { lines += "[warning] $it" }
    return lines.joinToString("\n")
}
