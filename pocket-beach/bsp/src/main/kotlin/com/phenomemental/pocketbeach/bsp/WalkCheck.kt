package com.phenomemental.pocketbeach.bsp

/**
 * The walker as she is given it: one native tool, its block, spindle and
 * pscale_attention as the beach's bsp() has them (spine:pocket-beach
 * 1.1111787199994). The spindle is text in the tool's schema: an address is
 * a number, and a number sent as JSON loses its trailing zeros and, past
 * fifteen digits, its digits.
 */
object WalkTool {
    const val NAME = "bsp"

    /**
     * The walker for a turn whose words named [blocks]: its description names
     * them, and they are the only blocks it takes. On the laptop on 23
     * November (trial/…/WalkProbe.kt), described for any of his blocks she
     * called it only when his words carried an address (6 of 12) and
     * otherwise told him she had no access; named, she called it every time,
     * each block and address as he gave it (12 of 12).
     */
    fun forBlocks(blocks: List<String>): String {
        require(blocks.isNotEmpty()) { "the walker is offered only for blocks his words named" }
        fun q(s: String) = stringify(str(s))!!
        val about = "Sundial holds his blocks on his phone, and his words named ${blocks.joinToString(" and ")}. Read ${if (blocks.size == 1) "it" else "one"}, " +
            "as the beach's bsp() reads a block: no address for its opening line and each part's, or an address for the line there."
        return """{"name":"$NAME","description":${q(about)},"parameters":{"type":"object","properties":{""" +
            """"block":{"type":"string","enum":[${blocks.joinToString(",") { q(it) }}],"description":"The block his words named"},""" +
            """"spindle":{"type":"string","description":"An address, such as 1 or 2.3; omit for the block's opening"},""" +
            """"pscale_attention":{"type":"integer","description":"How deep: omit for a walk; 0 with no spindle for every place's opening line"}},"required":["block"]}}"""
    }
}

/**
 * Sundial 0.12.1, the check on his phone (spine:pocket-beach 1.1111787199993):
 * twenty walk calls from her on the GPU, each asked for a known address, and
 * how many arrived intact. A Gemma 4 E2B test on a Mali GPU (the Pixel's
 * kind) reported numbers corrupted in JSON through the Conversation API;
 * addresses are numbers. Nothing is walked: only what arrives is read.
 */
object WalkCheck {
    /** The block her calls name; it need not exist, as nothing is walked. */
    const val BLOCK = "walk-check"

    /** The walker as 0.13 gives it her, named for the check's block. */
    val TOOL: String get() = WalkTool.forBlocks(listOf(BLOCK))

    /** Twenty addresses, short and long: the two section 9 ran, places with repeated and mixed digits, and a long one of every digit. */
    val ADDRESSES = listOf(
        "2.2", "1.111178719993", "3", "1.1", "4.56", "2.27", "9.9", "5.123", "1.11", "7.4",
        "3.333", "8.21", "6.789", "1.2345", "2.468", "9.876", "4.4", "5.5", "1.111", "1.23456789",
    )

    /** His message for [address], as he would ask it. */
    fun message(address: String): String = "Read me what's at $address in my $BLOCK block."

    /** Her window for each call: the time alone, as 0.12 gives it. */
    fun window(now: java.time.ZonedDateTime): String = HerWindow.released(now)

    /** How one address arrived: as text and the same, as text and altered, as a number, or not at all. */
    class Outcome(val wanted: String, val arrived: String?, val asNumber: Boolean) {
        val intact: Boolean get() = !asNumber && arrived == wanted

        /** It came, and not as sent: as a number, or changed. */
        val altered: Boolean get() = arrived != null && !intact

        /** In plain words, for the check's list. */
        val said: String get() = when {
            intact -> "$wanted arrived as $wanted"
            arrived == null -> "$wanted: no call came back"
            asNumber -> "$wanted arrived as a number, $arrived"
            else -> "$wanted arrived as $arrived"
        }
    }

    /** What arrived for [wanted], from her call's arguments exactly as the engine handed them (null: she made no call). */
    fun outcome(wanted: String, arguments: Map<String, Any?>?): Outcome {
        val s = arguments?.get("spindle") ?: return Outcome(wanted, null, false)
        return when (s) {
            is Number -> Outcome(wanted, s.toString(), true)
            else -> Outcome(wanted, s.toString().trim(), false)
        }
    }

    /** "18 of 20 arrived intact." */
    fun said(outcomes: List<Outcome>): String = "${outcomes.count { it.intact }} of ${outcomes.size} arrived intact."
}
