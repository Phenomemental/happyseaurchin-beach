package com.phenomemental.pocketbeach.bsp

/**
 * Sundial 0.13.4: her words on a turn with the walker offered, shown as they
 * come once their opening shows she is writing words, not a tool call; held
 * back only while a call is still possible. On his phone a turn where the
 * walker was offered showed nothing for 107 seconds: she wrote 781 tokens
 * and walked nothing, and the app held her whole reply back in case it was a
 * call. The engine hands a call it takes as a call, never as text (the
 * laptop's probe, 23 November); a call written out as text, which it doesn't
 * take, is held to the end of her reply, and shown then.
 */
class HeldForACall(private val show: (String) -> Unit) {
    private val held = StringBuilder()

    /** Whether her words are being shown as they come. */
    var showing = false
        private set

    /** One piece of her reply, as the engine streams it. */
    fun feed(piece: String) {
        if (showing) {
            show(piece)
            return
        }
        held.append(piece)
        if (!couldBeACall(held)) {
            showing = true
            show(held.toString())
            held.clear()
        }
    }

    /**
     * Her reply, or this round of it, has ended. Anything still held is her
     * words after all, shown now; unless the engine took a call this round
     * ([called]), when what was held was the call's own.
     */
    fun end(called: Boolean) {
        if (held.isNotEmpty() && !called) show(held.toString())
        held.clear()
    }

    companion object {
        /** How a tool call written out as text opens. */
        val OPENINGS = listOf(
            "<start_function_call>", "<start_of_tool", "<tool_call>", "<|tool_call", "<function",
            "```tool_code", "```tool_call", "```json", "tool_code", "tool_call", "call:", "{\"", "print(", "bsp(",
        )

        /** Whether [text], her words so far, could still be a call: nothing but space yet, the start of an opening, or an opening. */
        fun couldBeACall(text: CharSequence): Boolean {
            val t = text.trimStart().toString().lowercase()
            if (t.isEmpty()) return true
            return OPENINGS.any { o -> o.startsWith(t) || t.startsWith(o) }
        }
    }
}
