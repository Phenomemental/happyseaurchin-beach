package com.phenomemental.pocketbeach.bsp

/**
 * Sundial 0.13, the walk offered when his words name a block
 * (spine:pocket-beach 1.1111787199994 and 1.1111787199995): which of his
 * blocks his words name; the walk she asks for; and what she is handed
 * back, readable text in the block's own structure, never JSON.
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
            has(name) || (role != null && (has("my $role") || has("the $role") || has("$role block") || has("my ${role}s")))
        }.distinct()
    }

    /** The block her call names, among [named] (the blocks his words named): her own words for it matched as his are; null when it isn't one of them. */
    fun resolve(asked: String?, named: List<String>, handle: String?): String? {
        val a = asked?.trim()?.removePrefix("pocket/")?.takeIf { it.isNotEmpty() } ?: return named.singleOrNull()
        named.firstOrNull { it.equals(a, ignoreCase = true) }?.let { return it }
        return named.firstOrNull { n -> handle != null && n.equals("$a:$handle", ignoreCase = true) } ?: named(a, named, handle).singleOrNull()
            ?: named(" my $a ", named, handle).singleOrNull()
    }

    /** What the walk she asked for came to: the block, the address, and what she is handed. */
    class Walked(val block: String?, val spindle: String?, val text: String, val found: Boolean) {
        /** What she walked: the block, and the address when she gave one. */
        val chip: String get() = block + (spindle?.let { " at $it" } ?: "")

        /** The chip on her reply, in words: "walked membership at 2.2". */
        val said: String get() = (if (found) "walked " else "looked for ") + chip
    }

    /** Walk [arguments] (her call's, as the engine handed them) in [pages], his pocket, among the blocks his words [named]. */
    fun walk(pages: Pages, arguments: Map<String, Any?>, named: List<String>, handle: String?): Walked {
        val asked = arguments["block"]?.toString()
        val block = resolve(asked, named, handle) ?: return Walked(asked, null, "He named ${named.joinToString()}; nothing else is walked.", false)
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
            val view = runCatching { pages.position(Region.POCKET, block, spindle) }.getOrNull()
                ?: return Walked(block, spindle, "Nothing was found at $spindle in his block $block.", false)
            return Walked(block, view.address, readable(block, view, attention, pages.floorOf(block), pages.position(Region.POCKET, block)?.text), true)
        }
        val top = pages.position(Region.POCKET, block) ?: return Walked(block, null, "Nothing was found in his block $block.", false)
        return Walked(block, null, "(His block $block: its opening line, then each part's opening line beneath it.)\n" + discOf(top), true)
    }

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

    private fun discOf(top: PositionView): String = buildString {
        append(top.text ?: "(no opening line)")
        for (l in top.lines) {
            append("\n  ${l.address}  ${l.text ?: "(nothing written here)"}")
            if (l.beneath > 0) append("  (${l.beneath} beneath)")
        }
    }
}
