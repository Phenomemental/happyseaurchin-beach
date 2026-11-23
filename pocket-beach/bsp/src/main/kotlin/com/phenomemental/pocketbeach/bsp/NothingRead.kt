package com.phenomemental.pocketbeach.bsp

/**
 * Sundial 0.13.3, plain machinery (spine:pocket-beach 1.1111787199994,
 * revised 23 November): on his phone she wrote a pretend bsp call and an
 * invented passport as if read. A reply that writes a tool call, or says its
 * data was received, on a turn where nothing was read from his blocks (no
 * walk of hers, no read of the app's), is marked so, plainly. Nothing about
 * how she answers: her words stay as she wrote them.
 */
object NothingRead {
    private val SHAPES = listOf(
        Regex("""\bbsp\s*\(""", RegexOption.IGNORE_CASE),
        Regex("""\b(tool_code|tool_call|function_call|tool_response)\b""", RegexOption.IGNORE_CASE),
        Regex("""<\|?\s*(tool|function)""", RegexOption.IGNORE_CASE),
        Regex(""""name"\s*:\s*"bsp"""", RegexOption.IGNORE_CASE),
        Regex("""\bdata\s+(received|retrieved|loaded)\b""", RegexOption.IGNORE_CASE),
    )

    /** Whether [reply] writes a tool call or says data was received. */
    fun pretends(reply: String): Boolean = SHAPES.any { it.containsMatchIn(reply) }

    /** Said beneath such a reply. */
    const val SAID = "Nothing was read from your blocks in this reply."
}
