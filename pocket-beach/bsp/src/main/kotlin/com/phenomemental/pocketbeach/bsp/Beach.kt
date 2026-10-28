package com.phenomemental.pocketbeach.bsp

/*
 * Beach.kt — the duties the beach does, done on the phone.
 *
 * bsp-mcp-server hands every write and append to a beach, and the beach decides
 * the rest: the next free slot, the supernest when a block's root fills, the
 * arrival stamp, the opening line a new block is born with, the shape gate,
 * the summaries owed, and the listing of blocks. This is a port of those parts
 * of pscale-beach's api/pscale-beach.js and api/floor.js at the pinned commit,
 * for ordinary blocks with no locks (the pocket keeps none: the phone is the
 * latch). It answers in the beach's own words: a status and the JSON body the
 * beach would send.
 */

class BeachAnswer(val status: Int, val body: JsObj)

private fun ok(vararg pairs: Pair<String, Js>) = BeachAnswer(200, JsObj.of("ok" to JsBool(true), *pairs))

private fun refuse(status: Int, error: String, code: String) = BeachAnswer(status, JsObj.of("error" to str(error), "code" to str(code)))

class Beach(private val storage: BlockStorage, val origin: String = "pocket") {

    // ── storage, as the beach's loadBlock / saveBlock ──

    /** The stored block, parsed afresh each time; a stored null reads as absent, as the beach's store returns it. */
    fun loadBlock(name: String): Js? = storage.read(name)?.let(::parseJson)?.takeUnless { it == JsNull }

    private fun saveBlock(name: String, block: Js, born: Boolean, nowMs: Long) {
        var b = block
        if (!hasFloor(b)) {
            val healed = repairFloor(name, b)
            if (healed != null) b = healed
        }
        storage.write(name, stringify(b)!!)
        val iso = isoString(nowMs)
        val s = storage.stamps()
        storage.setStamps(Stamps(s.touched + (name to iso), if (born) s.born + (name to iso) else s.born))
    }

    // ── GET ──

    /** The derived index — GET with no ?block=. */
    fun index(): JsObj {
        val blocks = storage.names().distinct().sorted()
        val present = blocks.toSet()
        fun listed(t: Map<String, String>): JsObj? {
            val kept = JsObj()
            for ((k, v) in t) if (k in present) kept[k] = str(v)
            return if (kept.size > 0) kept else null
        }
        val stamps = storage.stamps()
        val touched = listed(stamps.touched)
        val born = listed(stamps.born)
        // The weight the beach lists is the stored JSON's size in bytes.
        val bytes = if (blocks.isEmpty()) null else JsObj().also { o ->
            for (n in blocks) { val v = storage.read(n)?.toByteArray(Charsets.UTF_8)?.size ?: 0; if (v > 0) o[n] = JsNum(v.toDouble()) }
        }.takeIf { it.size > 0 }
        val text = "URL surface at $origin. Named sibling blocks listed below; address each via ?block=<name>" +
            (if (bytes != null) "; bytes maps each block to its stored size — pick an aperture before the read" else "") +
            (if (touched != null) "; touched maps each block to when it last changed — fetch only what moved" else "") +
            (if (born != null) "; born maps each block to when it first arrived — absent means before the stamp existed" else "") +
            "; now is the moment this was served — ISO, its ten-digit sundial address (the year first), its voicing — the stamp every response also carries as X-Pscale-Now." +
            (if ("lighthouse" in present) " First visit: start at ?block=lighthouse — the compass for this surface." else "") +
            " Substrate-wide conventions at bsp(agent_id='pscale', block='block-conventions')."
        val out = JsObj.of("_" to str(text), "origin" to str(origin), "blocks" to JsArr(blocks.map { str(it) }.toMutableList<Js?>()))
        bytes?.let { out["bytes"] = it }
        touched?.let { out["touched"] = it }
        born?.let { out["born"] = it }
        return out
    }

    // ── POST ──

    fun post(blockName: String?, body: JsObj, nowMs: Long): BeachAnswer {
        if (blockName.isNullOrEmpty()) return refuse(400, "block name required (pass ?block=<name> or \"block\" in body)", "invalid_shape")
        return try {
            standardWrite(blockName, body, nowMs)
        } catch (e: InvalidAddressError) {
            refuse(400, e.message ?: "", "invalid_address")
        }
    }

    private fun standardWrite(blockName: String, body: JsObj, nowMs: Long): BeachAnswer {
        val spindle: Js? = if (body.has("spindle")) body["spindle"] else str("")
        val content = body["content"]
        val confirm = body["confirm"]
        val append = body["append"]

        if (append == JsBool(true)) {
            if (content == null) return refuse(400, "append requires content", "invalid_shape")
            validateShape(content)?.let { return refuse(400, it, "invalid_shape") }
            val appendSpindle = if (spindle == null || spindle == JsNull) "" else jsString(spindle)
            if (appendSpindle != "") return appendAtSpindle(blockName, appendSpindle, content, nowMs)
            val entry = stampAppendTimestamp(content, nowMs)
            val existing = loadBlock(blockName)
            val r = appendWithSupernest(blockName, existing, entry)
            var block = r.block
            if (blockName == "presence") block = sweepStalePresence(block, nowMs)
            saveBlock(blockName, block, existing == null, nowMs)
            val owed = owedSummaries(r.block)
            val answer = ok("slot" to str(r.slot), "supernested" to JsBool(r.supernested), "floor" to JsNum(r.floor.toDouble()))
            if (existing == null) answer.body["born"] = JsBool(true)
            if (owed.isNotEmpty()) answer.body["owed"] = JsArr(owed.map { JsObj.of("slot" to str(it.first), "over" to str(it.second)) }.toMutableList<Js?>())
            return answer
        }

        if (content != null) validateShape(content)?.let { return refuse(400, it, "invalid_shape") }
        val existing = loadBlock(blockName)
        var block: Js? = existing
        val spindleText = if (spindle == null || spindle == JsNull) null else jsString(spindle)
        if (block == null) {
            block = if (spindle == str("") && content is JsObj) null else JsObj.of("_" to str(defaultIdentity(blockName)))
        }
        if (content != null && !truthy(spindle) && existing != null && confirm != JsBool(true)) {
            return refuse(400, "whole-block replace requires {confirm: true}", "confirm_required")
        }
        if (truthy(spindle)) parseSpindle(spindleText!!.removeSuffix("*"), floorDepth(block ?: JsObj()))

        if (content != null) {
            if (!truthy(spindle)) {
                if (!hasFloor(content)) {
                    return refuse(400, "whole-block content has no floor — the root must carry a `_` whose chain reaches an identity string (sunstone:1.51)", "no_floor")
                }
                block = content
            } else {
                if (block == null) block = JsObj.of("_" to str(defaultIdentity(blockName)))
                beachWriteAt(block, spindleText!!, content)
            }
            if (blockName == "presence") block = sweepStalePresence(block, nowMs)
            saveBlock(blockName, block, existing == null, nowMs)
        }
        return if (content != null && existing == null) ok("born" to JsBool(true)) else ok()
    }

    private fun appendAtSpindle(blockName: String, spindle: String, content: Js, nowMs: Long): BeachAnswer {
        if (spindle.contains('*')) {
            return refuse(400, "append walks to a node; the star operator addresses a hidden directory — \"$spindle\" cannot take an append", "invalid_address")
        }
        val pre = loadBlock(blockName)
            ?: return refuse(404, "block \"$blockName\" not found — append at a spindle grows an existing node, never creates the block", "not_found")
        parseSpindle(spindle, floorDepth(pre))
        val entry = stampAppendTimestamp(content, nowMs)
        val existing = loadBlock(blockName) ?: return refuse(404, "no node at \"$spindle\" of \"$blockName\" — append lands beneath an existing node; write the node first", "not_found")
        val fl = floorDepth(existing)
        val digits = parseSpindle(spindle, fl).digits
        val r = appendAtNode(existing, digits, entry)
        when (r) {
            is NodeAppend.Missing -> return refuse(404, "no node at \"$spindle\" of \"$blockName\" — append lands beneath an existing node; write the node first", "not_found")
            is NodeAppend.Leaf -> return refuse(
                409,
                "\"$spindle\" of \"$blockName\" holds a scalar, not a node — appending beneath it would bury that text under a wrap it never asked for; write the position as an object (its prose at \"_\") before growing beneath it",
                "append_at_leaf",
            )
            is NodeAppend.Full -> {
                fun at(slot: String) = formatAddress(digits + slot.toList(), fl)
                val nodeAddr = formatAddress(digits, fl)
                val occupied = r.slots.joinToString(", ") { at(it.first) }
                val oldest = r.oldest?.let { " Longest-standing is ${at(it.first)} (${it.second})." }
                    ?: " No slot carries a date, so which is oldest is yours to judge — slot order does not say, once anything has been overwritten."
                return refuse(
                    409,
                    "\"$nodeAddr\" of \"$blockName\" is full: nine slots, all taken ($occupied)." + oldest +
                        " A digit position holds nine and does not grow — accumulation belongs at a block root, where a supernest can raise the floor. Overwrite a slot with an ordinary write to its address; nothing here expires by itself, so choosing which one goes is yours." +
                        " If this is a channel rather than a store, put a POINTER in the slot and keep the thing itself in a block of your own, where it can accumulate properly (ways:grain 6, the letter).",
                    "node_full",
                )
            }
            is NodeAppend.Landed -> {
                saveBlock(blockName, existing, false, nowMs)
                return ok(
                    "slot" to str(r.slot),
                    "address" to str(formatAddress(digits + r.slot.toList(), fl)),
                    "node" to str(formatAddress(digits, fl)),
                )
            }
        }
    }

    // ── floor.js ──

    fun hasFloor(block: Js?): Boolean {
        var node = block
        while (truthy(node) && isObject(node) && jsIn(node, "_")) {
            node = jsGet(node, "_")
            if (node is JsStr) return true
        }
        return false
    }

    fun defaultIdentity(name: String): String {
        val base = name.split(':')[0]
        val at = " at $origin."
        return when (base) {
            "marks" -> "Marks$at Open stigmergy — each digit-path slot is one contribution (block-conventions:9)."
            "presence" -> "Presence$at One slot per agent, heartbeat-overwritten (block-conventions:4.6)."
            "liquid" -> "Liquid composition buffer$at Pre-commit slots (block-conventions:4.5)."
            else -> "$name$at"
        }
    }

    /** The healed block, or null when nothing needed changing. */
    private fun repairFloor(name: String, block: Js): Js? {
        if (block !is JsObj) return null
        if (hasFloor(block)) return null
        val healed = JsObj.of("_" to str(defaultIdentity(name)))
        for (k in block.keys()) healed[k] = block[k]
        return healed
    }

    class Appended(val block: Js, val slot: String, val supernested: Boolean, val floor: Int)

    private fun appendWithSupernest(name: String, existing: Js?, content: Js): Appended {
        var block: Js = when {
            existing == null -> JsObj.of("_" to str(defaultIdentity(name)))
            !hasFloor(existing) -> repairFloor(name, existing) ?: existing
            else -> existing
        }
        var floor = floorDepth(block)
        var supernested = false
        var slot = nextZeroFreeSlot(block, floor)
        if (slot == null) {
            block = JsObj.of("_" to block)
            floor += 1
            supernested = true
            slot = nextZeroFreeSlot(block, floor)!!
        }
        rawSet(block, slot, content)
        return Appended(block, slot, supernested, floor)
    }

    sealed class NodeAppend {
        object Missing : NodeAppend()
        object Leaf : NodeAppend()
        class Full(val slots: List<Pair<String, String?>>, val oldest: Pair<String, String?>?) : NodeAppend()
        class Landed(val slot: String) : NodeAppend()
    }

    private fun appendAtNode(block: Js, nodeDigits: List<Char>, content: Js): NodeAppend {
        if (block !is JsObj) return NodeAppend.Missing
        var parent: Js? = null
        var node: Js? = block
        for (d in nodeDigits) {
            val k = if (d == '0') "_" else d.toString()
            if (node !is JsObj || !node.has(k)) return NodeAppend.Missing
            parent = node
            node = node[k]
        }
        if (parent == null) return NodeAppend.Missing
        if (node !is JsObj) return NodeAppend.Leaf
        val slot = nextZeroFreeSlot(node, 1) ?: return nodeOccupancy(node)
        rawSet(node, slot, content)
        return NodeAppend.Landed(slot)
    }

    private fun nodeOccupancy(node: JsObj): NodeAppend.Full {
        val slots = mutableListOf<Pair<String, String?>>()
        for (d in 1..9) {
            val v = node[d.toString()] ?: continue
            val ts = (if (isObject(v)) jsGet(v, "3") as? JsStr else null)?.value
            slots += d.toString() to ts
        }
        val dated = slots.filter { it.second != null }.sortedWith { a, b -> if (a.second!! < b.second!!) -1 else 1 }
        val oldest = if (dated.size == slots.size && dated.isNotEmpty()) dated[0] else null
        return NodeAppend.Full(slots, oldest)
    }

    /** owedSummaries — the zero-slot summaries due and unpaid, oldest first, as (slot, over). */
    fun owedSummaries(block: Js?): List<Pair<String, String>> {
        if (!isObject(block)) return emptyList()
        val floor = floorDepth(block)
        if (floor < 2) return emptyList()
        val prefixLen = floor - 1
        fun complete(node: Js?) = isObject(node) && (1..9).all { jsIn(node, it.toString()) }
        val out = mutableListOf<Pair<String, String>>()
        val total = pow9(prefixLen)
        for (i in 0 until total) {
            val container = zeroFreePath(i, prefixLen)
            val node = rawWalk(block!!, container)
            if (!isObject(node)) continue
            val prevPath = if (i == 0) "_".repeat(prefixLen) else zeroFreePath(i - 1, prefixLen)
            if (!complete(rawWalk(block, prevPath))) continue
            val u = jsGet(node, "_")
            if (u is JsStr && jsTrim(u.value) != "") continue
            val p = prevPath.replace("_", "")
            out += "${container}0" to "${p}1-${p}9"
        }
        return out
    }

    private fun pow9(n: Int): Int { var t = 1; repeat(n) { t *= 9 }; return t }

    private fun rawWalk(block: Js, path: String): Js? {
        var node: Js? = block
        for (ch in path) {
            val k = ch.toString()
            if (node == null || node == JsNull || !isObject(node) || !jsIn(node, k)) return null
            node = jsGet(node, k)
        }
        return node
    }

    private fun zeroFreePath(index: Int, length: Int): String {
        var i = index
        var s = ""
        repeat(length) { s = ((i % 9) + 1).toString() + s; i /= 9 }
        return s
    }

    private fun nextZeroFreeSlot(block: Js, floor: Int): String? {
        val total = pow9(floor)
        for (i in 0 until total) {
            val p = zeroFreePath(i, floor)
            if (rawWalk(block, p) == null) return p
        }
        return null
    }

    private fun rawSet(block: Js, path: String, value: Js) {
        var node: Js = block
        for (k in 0 until path.length - 1) {
            val key = path[k].toString()
            val v = jsGet(node, key)
            if (v is JsStr) jsSet(node, key, JsObj.of("_" to v))
            else if (v == null || v == JsNull || !isObject(v) || v is JsArr) jsSet(node, key, JsObj())
            node = jsGet(node, key)!!
        }
        jsSet(node, path.last().toString(), value)
    }

    // ── the handler's helpers ──

    private val iso8601 = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,9})?)?(Z|[+-]\d{2}:?\d{2})?$""")

    private fun isIso8601DateTime(v: Js?): Boolean = v is JsStr && iso8601.matches(v.value) && parseIsoMs(v.value) != null

    private fun stampAppendTimestamp(content: Js, nowMs: Long): Js {
        if (content !is JsObj) return content
        if (isIso8601DateTime(content["3"])) return content
        val copy = JsObj()
        for (k in content.keys()) copy[k] = content[k]
        copy["3"] = str(isoString(nowMs))
        return copy
    }

    private fun isPresenceEntry(node: Js?): Boolean =
        node is JsObj && node["1"] is JsStr && node["2"] is JsStr && node["3"] is JsStr && node["4"] == null

    private fun sweepStalePresence(block: Js, nowMs: Long): Js {
        if (block !is JsObj) return block
        val cutoff = nowMs - 60_000
        val out = JsObj()
        for (key in block.keys()) {
            val v = block[key]
            if (key == "_") { out[key] = v; continue }
            if (!(key.length == 1 && key[0] in '1'..'9')) { out[key] = v; continue }
            if (isPresenceEntry(v)) {
                val ts = parseIsoMs((v as JsObj)["3"].let { (it as JsStr).value })
                if (ts != null && ts < cutoff) continue
                out[key] = v
            } else if (v is JsObj) {
                val swept = sweepStalePresence(v, nowMs)
                if (jsKeys(swept).any { it.length == 1 && it[0] in '1'..'9' }) out[key] = swept
            } else {
                out[key] = v
            }
        }
        return out
    }

    /** The shape gate: only `_` and single digits 1-9 as keys, and no JSON passed as a string. */
    fun validateShape(content: Js?, path: String = ""): String? {
        if (content == null || content == JsNull) return null
        if (content is JsStr) {
            val trimmed = jsTrim(content.value)
            if (trimmed.length > 1 && (trimmed[0] == '{' || trimmed[0] == '[')) {
                try {
                    val parsed = parseJson(trimmed)
                    if (isObject(parsed)) {
                        return "invalid value at \"${path.ifEmpty { "<root>" }}\" — JSON-stringified ${if (parsed is JsArr) "array" else "object"}; write the structure directly, not as a string"
                    }
                } catch (_: JsonSyntaxError) {
                }
            }
            return null
        }
        if (content is JsArr) {
            content.items.forEachIndexed { i, x -> validateShape(x, "$path[$i]")?.let { return it } }
            return null
        }
        if (content !is JsObj) return null
        for (key in content.keys()) {
            if (key != "_" && !(key.length == 1 && key[0] in '1'..'9')) {
                return "invalid key \"${if (path.isNotEmpty()) "$path." else ""}$key\" — pscale spine accepts only \"_\" and single digits 1-9 at each level (compound supernest slots like 11 or 234 are stored hierarchically as nested single-digit keys, not literal keys)"
            }
            validateShape(content[key], if (path.isNotEmpty()) "$path.$key" else key)?.let { return it }
        }
        return null
    }

    /** The beach's writeAt: as the walker's, and a scalar at a node voices it rather than replacing it. */
    private fun beachWriteAt(block: Js, address: String, value: Js) {
        if (address.isEmpty()) return
        val digits = parseSpindle(address, floorDepth(block)).digits
        if (digits.isEmpty()) return
        var node: Js = block
        for (i in 0 until digits.size - 1) {
            val key = if (digits[i] == '0') "_" else digits[i].toString()
            val existing = jsGet(node, key)
            if (existing is JsStr) jsSet(node, key, JsObj.of("_" to existing))
            else if (!isObject(existing)) jsSet(node, key, JsObj())
            node = jsGet(node, key)!!
        }
        val lastKey = if (digits.last() == '0') "_" else digits.last().toString()
        val target = jsGet(node, lastKey)
        if (target is JsObj && (value == JsNull || !isObject(value))) {
            target["_"] = value
            return
        }
        jsSet(node, lastKey, value)
    }
}
