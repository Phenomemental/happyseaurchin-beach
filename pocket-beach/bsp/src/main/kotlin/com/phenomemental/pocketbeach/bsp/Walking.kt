package com.phenomemental.pocketbeach.bsp

/**
 * Sundial 0.13, the walk offered when his words name a block
 * (spine:pocket-beach 1.1111787199994 and 1.1111787199995): which of his
 * blocks his words name; the walk she asks for, checked against the block
 * before it is walked; and what she is handed back, readable text in the
 * block's own structure, never JSON.
 */
object Walking {
    /**
     * The blocks of his ([names], his pocket's) that [words] name: a block's
     * whole name as a word ("garden", "spine:demo"), and for a block of his
     * own role ("passport:Phenomemental") its plain forms, "my passport", "the
     * passport", "passport block". A name is matched whole and in any
     * capitals; a hyphen may be spoken as a space ("pocket companion").
     */
    fun named(words: String, names: Collection<String>, handle: String?): List<String> {
        val said = " " + words.lowercase().replace('’', '\'').replace(Regex("""[^\p{L}\p{N}:'\-.]+"""), " ").trim() + " "
        fun has(phrase: String): Boolean {
            val p = phrase.lowercase().trim()
            if (p.isEmpty()) return false
            return Regex("""(?<![\p{L}\p{N}:\-])${Regex.escape(p)}(?![\p{L}\p{N}:\-])""").containsMatchIn(said) ||
                (p.contains('-') && Regex("""(?<![\p{L}\p{N}])${Regex.escape(p.replace('-', ' '))}(?![\p{L}\p{N}])""").containsMatchIn(said))
        }
        return names.filter { name ->
            val role = handle?.let { h -> name.removeSuffix(":$h").takeIf { it != name } }
            // Sundial 0.13.3: Sundial's own day block, "now", is a word he says all the time; it is named only as "now block".
            if (name in AS_BLOCK_ONLY) return@filter has("$name block")
            has(name) || (role != null && (has("my $role") || has("the $role") || has("$role block") || has("my ${role}s")))
        }.distinct()
    }

    /** Blocks named only with the word "block" after them: Sundial's own day block, whose name is a word he uses all the time. */
    val AS_BLOCK_ONLY: Set<String> = setOf(Days.NOW)

    /** The block her call names, among [named] (the blocks his words named): her own words for it matched as his are; null when it isn't one of them. */
    fun resolve(asked: String?, named: List<String>, handle: String?): String? {
        val a = asked?.trim()?.removePrefix("pocket/")?.takeIf { it.isNotEmpty() } ?: return named.singleOrNull()
        named.firstOrNull { it.equals(a, ignoreCase = true) }?.let { return it }
        return named.firstOrNull { n -> handle != null && n.equals("$a:$handle", ignoreCase = true) } ?: named(a, named, handle).singleOrNull()
            ?: named(" my $a ", named, handle).singleOrNull()
    }

    /** What the walk she asked for came to: the block, the address, and what she is handed. */
    class Walked(val block: String?, val spindle: String?, val text: String, val found: Boolean, val again: Boolean = false) {
        /** What she walked: the block, and the address when she gave one. */
        val chip: String get() = block + (spindle?.let { " at $it" } ?: "")

        /** The chip on her reply, in words: "walked membership at 2.2", or for a walk the guard refused, "looked for 2.9 in membership". */
        val said: String get() = when {
            again -> "already handed: $chip"
            found -> "walked $chip"
            else -> "looked for ${spindle?.let { "$it in " }.orEmpty()}${block ?: "a block"}"
        }
    }

    /**
     * Walk [arguments] (her call's, as the engine handed them) in [pages], his
     * pocket, among the blocks his words [named]. The guard first: the block
     * must be one he named, and the spindle a place that stands in it;
     * otherwise nothing is walked and she is told plainly there is no such
     * place, to answer without it.
     */
    fun walk(pages: Pages, arguments: Map<String, Any?>, named: List<String>, handle: String?): Walked {
        val asked = arguments["block"]?.toString()
        val block = resolve(asked, named, handle) ?: return Walked(asked, null, "There is no block called ${asked ?: "that"} among those he named (${named.joinToString()}). Answer him without it.", false)
        val spindle = when (val s = arguments["spindle"]) {
            null -> null
            is Number -> s.toString()
            else -> s.toString().trim().takeIf { it.isNotEmpty() && it != "null" }
        }
        val attention = when (val p = arguments["pscale_attention"]) {
            is Number -> p.toInt()
            null -> null
            else -> p.toString().trim().toIntOrNull()
        }
        if (spindle != null) {
            val bad = spindle.count { it == '.' } > 1 || !spindle.all { it.isDigit() || it == '.' || it == ',' }
            val view = if (bad) null else pages.position(Region.POCKET, block, spindle.replace(",", ""))
            if (view == null || view.address.isEmpty()) return Walked(block, spindle, "There is no place $spindle in his block $block. Answer him without it.", false)
            return Walked(block, view.address, readable(block, view, attention, pages.floorOf(block), pages.position(Region.POCKET, block)?.text), true)
        }
        val top = pages.position(Region.POCKET, block) ?: return Walked(block, null, "There is no block called $block in his pocket. Answer him without it.", false)
        return Walked(block, null, "(His block $block, as bsp() reads it: ${Chats.TOP_LINES}.)\n" + Chats.topLines(top), true)
    }

    /**
     * Sundial 0.13.5: [w], a walk of a block or a place in it already handed
     * her, not handed again: she is told plainly it is above. Sundial 0.13.6:
     * what she is handed is hers for that turn only, so this holds within the
     * turn: a walk of a block's top when the app has just handed it her.
     */
    fun again(w: Walked): Walked = Walked(
        w.block, w.spindle,
        "His block ${w.block}${w.spindle?.let { " at $it" }.orEmpty()} was handed you with his words on this turn; it is above. Answer from it, or walk to one of its places.",
        found = false, again = true,
    )

    /**
     * A place walked to, as readable text: the path from the opening line down
     * to it, each line indented by its depth, then the lines beneath it when
     * she asked deeper, as bsp() reads a pscale below the address's own (the
     * block's floor less the address's digits).
     */
    private fun readable(block: String, view: PositionView, attention: Int?, floor: Int, opening: String?): String = buildString {
        append("(His block $block, walked to ${view.address}: the path from its opening line down to it")
        val deeper = attention != null && attention < floor - view.address.count { it.isDigit() }
        append(if (deeper) ", then the lines beneath it.)" else ".)")
        append("\n" + (opening ?: "(no opening line)"))
        var depth = 1
        for (l in view.trail) {
            append("\n" + "  ".repeat(depth) + l.address + "  " + (l.text ?: "(nothing written here)"))
            depth++
        }
        append("\n" + "  ".repeat(depth) + view.address + "  " + (view.text ?: "(nothing written here)"))
        if (deeper) for (l in view.lines) append("\n" + "  ".repeat(depth + 1) + l.address + "  " + (l.text ?: "(nothing written here)") + (if (l.beneath > 0) "  (${l.beneath} beneath)" else ""))
        else if (view.lines.isNotEmpty()) append("\n" + "  ".repeat(depth + 1) + "(${view.lines.size} beneath)")
    }
}
