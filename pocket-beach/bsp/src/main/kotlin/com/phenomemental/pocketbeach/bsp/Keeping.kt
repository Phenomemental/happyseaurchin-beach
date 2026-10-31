package com.phenomemental.pocketbeach.bsp

/**
 * 0.6.4 (Matthew, 31 October), the guards and the gate, step one of the
 * step back's design: what the app does with a message of his, decided by
 * code from his words.
 *
 * - The writing check runs only when his words ask to keep something
 *   (keep, note, add, write, save, remember, set or put down), to change a
 *   line, to make a block, or to post; and for the hello world. Otherwise
 *   she only talks ("Focus", 31 October: the check ran after nearly every
 *   message and was told to keep what he told it).
 * - Asked to keep something, with no block of his named, the app asks him
 *   where it goes; the model doesn't choose.
 * - A name he gives her is one line on her shell, by code.
 * - Every line proposed is his words, found in what he said in this
 *   conversation; her words never count.
 */
object Keeping {
    /** What his words ask for. */
    enum class Ask { KEEP, CHANGE, MAKE, POST, HELLO }

    /** What the app does with a message of his. */
    sealed class Way {
        /** She only talks: no writing check. */
        object Talk : Way()
        /** A name he gives her: "Matthew calls me <name>." on her shell, by code. */
        class Name(val name: String) : Way() {
            val line: String get() = "Matthew calls me $name."
        }
        /** He asks to keep [words] and names no block of his: the app asks him where. */
        class Where(val words: String) : Way()
        /** The writing check runs, for what his words ask. */
        class Write(val asks: Set<Ask>) : Way()
    }

    /**
     * What to do with [said]: [before] is what he said earlier in this
     * conversation, newest last; [shell] is her shell.
     */
    fun way(said: String, before: List<String>, pages: Pages, shell: String = HelloWorld.SHELL): Way {
        val asks = asks(said)
        name(said)?.let { name ->
            if (asks.isEmpty() || asks == setOf(Ask.CHANGE)) {
                val held = pages.position(Region.POCKET, shell)?.lines.orEmpty().any { it.text != null && Telling.plain(it.text) == Telling.plain("Matthew calls me $name.") }
                return if (held) Way.Talk else Way.Name(name)
            }
        }
        if (asks.isEmpty()) return Way.Talk
        if (asks == setOf(Ask.KEEP) && named(said, pages).isEmpty()) {
            val words = keptWords(said, before)
            return if (words.isBlank()) Way.Talk else Way.Where(words)
        }
        return Way.Write(asks)
    }

    // ── what his words ask ──

    private val LEADS = listOf(
        "yes", "yeah", "ok", "okay", "so", "and", "but", "then", "now", "right", "please", "hey", "oh", "well", "just", "also", "gemma", "to",
        "could you", "can you", "would you", "will you", "i'd like you to", "i would like you to", "i want you to", "i need you to",
        "i'd love you to", "i would love you to", "let's", "lets", "do you mind", "go ahead and",
    )
    private val LEAD = Regex("""^(?:(?:${LEADS.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }})\b[\s,]*)+""")

    private val KEEP_START = Regex("""^(?:keep|note|add|save|remember|jot|log|record|write\b(?=.*\b(?:down|in|into|to|under|beneath|below)\b)|put\b(?=.*\b(?:down|in|into|under|beneath|below)\b)|set\b(?=.*\bdown\b))\b""")
    private val KEEP_ANYWHERE = Regex(
        """\b(?:keep|save|remember|note)\s+(?:this|that|it|these|those|all (?:of )?(?:this|that|it)|some of (?:this|that|it))\b|""" +
            """\b(?:write|put|set|jot|note)\s+(?:[\p{L}']+\s+){0,5}down\b|\bmake a note\b|\bkeep (?:a )?(?:note|record) of\b""",
    )
    private val CHANGE_START = Regex("""^(?:change|rename|replace|correct)\b""")
    private val CHANGE_ANYWHERE = Regex("""\bchange (?:the|my|your|that|this)\b[^.?!]*\b(?:line|opening|name)\b""")
    private val MAKE = Regex("""\b(?:make|create|start|add)\s+(?:me\s+)?(?:a\s+)?(?:new\s+)?block\b|\bnew block\b""")
    private val POST_START = Regex("""^(?:post|share)\b""")
    private val POST_ANYWHERE = Regex("""\bpost (?:this|that|it|to)\b|\bpost:""")
    private val HELLO = Regex("""hello[\s-]*world""")

    /** His words as clauses, each lowercased and with its leading words of asking ("could you", "please") taken off. */
    private fun clauses(said: String): List<String> =
        said.lowercase().replace('’', '\'').split(Regex("""[.!?;:\n,]+""")).map { it.trim().replace(LEAD, "").trim() }.filter { it.isNotEmpty() }

    /** What [said] asks for: to keep, change, make a block, post, or the hello world; empty when it asks for none. A question asks only when it says so plainly ("Can you keep that?"). */
    fun asks(said: String): Set<Ask> {
        val lower = said.lowercase().replace('’', '\'')
        val parts = clauses(said)
        val out = mutableSetOf<Ask>()
        if (HELLO.containsMatchIn(lower)) out += Ask.HELLO
        if (MAKE.containsMatchIn(lower)) out += Ask.MAKE
        if (parts.any { CHANGE_START.containsMatchIn(it) } || CHANGE_ANYWHERE.containsMatchIn(lower)) out += Ask.CHANGE
        if (parts.any { POST_START.containsMatchIn(it) } || POST_ANYWHERE.containsMatchIn(lower)) out += Ask.POST
        if (Ask.MAKE !in out && (parts.any { KEEP_START.containsMatchIn(it) } || KEEP_ANYWHERE.containsMatchIn(lower))) out += Ask.KEEP
        return out
    }

    /** Whether his words ask for a post, or the hello world (whose shell offers one). */
    fun asksToPost(said: String): Boolean = asks(said).let { Ask.POST in it || Ask.HELLO in it }

    // ── a name he gives her ──

    private val NAMING = Regex("""\b(?:call you|calling you|your name is|your name's|you're called|you are called|name you|change your name to)\s+([\p{L}][\p{L}'’-]*)(?:\s+([\p{Lu}][\p{L}'’-]*))?""", RegexOption.IGNORE_CASE)
    private val NOT_NAMES = setOf(
        "the", "a", "an", "that", "this", "it", "something", "anything", "whatever", "what", "later", "back", "tomorrow", "soon", "again", "now",
        "when", "if", "by", "for", "to", "and", "my", "your", "his", "her", "please", "then", "today", "tonight", "i",
    )

    /** The name his words give her ("I'd like to call you Gemma", "your name is Gemma"); null when they give none, or ask. */
    fun name(said: String): String? {
        if (said.trim().endsWith("?")) return null
        val m = NAMING.find(said) ?: return null
        val first = m.groupValues[1].trim('\'', '’', '-')
        if (first.lowercase() in NOT_NAMES || first.length < 2) return null
        val second = m.groupValues[2].takeIf { it.isNotEmpty() && it.lowercase() !in NOT_NAMES && it != "I" }
        return listOfNotNull(first, second).joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    }

    // ── the block he names ──

    /** His blocks his words name, whole words, whatever their capitals: never the app's own. */
    fun named(said: String, pages: Pages): List<String> {
        val lower = said.lowercase()
        return pages.shore().mine.map { it.name }.filter { !own(it, pages) }
            .filter { Regex("""(?<![\p{L}\p{N}])${Regex.escape(it.lowercase())}(?![\p{L}\p{N}])""").containsMatchIn(lower) }
    }

    private val MADE = listOf(
        Regex("""\bblock\s+(?:called|named)\s+["“']?([\p{L}\p{N}][\p{L}\p{N}_'\- ]*?)["”']?\s*(?=[:,.;!?]|$|\s+(?:and|with|for|to|that|where|please)\b)""", RegexOption.IGNORE_CASE),
        Regex("""\bnew block\s+["“']?([\p{L}\p{N}][\p{L}\p{N}_'\-]*)""", RegexOption.IGNORE_CASE),
    )

    /** The name he gives a new block ("make a block called sailing"); null when he names none. */
    fun made(said: String): String? = MADE.firstNotNullOfOrNull { it.find(said)?.groupValues?.get(1)?.trim()?.takeIf(String::isNotEmpty) }

    /** The blocks the app keeps itself, by name: written only by the app, on his taps. */
    val OWN: Set<String> = setOf(com.phenomemental.pocketbeach.bsp.Asks.LOG, Landing.NOW, Landing.TELLING, Landing.EXAMPLES, Talking.SAFETY_BLOCK, Whens.BLOCK)

    /** Whether [name] is one of the app's own blocks: by name, whether or not it stands yet, or a clock. */
    fun own(name: String, pages: Pages): Boolean = name.lowercase() in OWN || Talks.appOwn(name, pages)

    // ── his words ──

    private fun tokens(text: String): List<String> =
        Regex("""[\p{L}\p{N}']+""").findAll(text.lowercase().replace('’', '\'').replace('‘', '\'')).map { it.value.trim('\'') }.filter { it.isNotEmpty() }.toList()

    /**
     * Whether [line] is his words: its words, in order and together, found
     * in one of [his] messages, or across them; capitals, stops and
     * apostrophes aside. Her words are never among [his].
     */
    fun hisWords(line: String, his: List<String>): Boolean {
        val want = tokens(line)
        if (want.isEmpty()) return false
        fun within(hay: List<String>) = hay.size >= want.size && (0..hay.size - want.size).any { i -> hay.subList(i, i + want.size) == want }
        return his.any { within(tokens(it)) } || within(tokens(his.joinToString(" ")))
    }

    /**
     * What he asks to keep, as his own words: after the colon of "Keep this:
     * …"; else his sentences with the asking taken off ("…, keep that for
     * me please"), and what follows an asking verb ("Note that …",
     * "Remember to …"); else, when that leaves nothing ("Keep that."), what
     * he said before.
     */
    fun keptWords(said: String, before: List<String> = emptyList()): String {
        val text = said.trim()
        val colon = text.indexOf(':')
        if (colon > 0 && Ask.KEEP in asks(text.substring(0, colon))) {
            val after = clean(text.substring(colon + 1))
            if (tokens(after).isNotEmpty()) return after
        }
        val sentences = Regex("""[^.!?\n]+[.!?]*""").findAll(text).map { it.value.trim() }.filter { it.isNotEmpty() }.toList()
        val kept = sentences.mapNotNull { s -> if (Ask.KEEP !in asks(s)) s else cutAsking(s) }.joinToString(" ").trim()
        if (tokens(kept).isNotEmpty()) return clean(kept)
        return before.lastOrNull()?.let(::clean).orEmpty()
    }

    private val TRAIL = Regex("""(?:[\s,]+|\b(?:${(LEADS + listOf("i", "you", "for me")).sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }})\b)+$""", RegexOption.IGNORE_CASE)
    private val OBJECT = setOf("this", "that", "it", "these", "those", "all", "of", "some", "for", "me", "please", "down", "safe", "too", "as", "well", "in", "mind")

    /** A sentence that asks to keep, as what of it is his to keep: before the asking, and what follows an asking verb; null when nothing is left. */
    private fun cutAsking(s: String): String? {
        val lower = s.lowercase().replace('\u2019', '\'')
        var at = KEEP_ANYWHERE.find(lower)?.range?.first ?: Int.MAX_VALUE
        var rest = ""
        for (part in Regex("""[^,;:]+""").findAll(lower)) {
            val lead = LEAD.find(part.value.trimStart())?.value?.length ?: 0
            val start = part.range.first + (part.value.length - part.value.trimStart().length) + lead
            val verb = KEEP_START.find(lower.substring(start)) ?: continue
            if (part.range.first <= at) {
                at = part.range.first
                // What follows the verb: "Note that the tulips came", "Remember to buy stamps".
                val after = s.substring(start + verb.value.length).replace(Regex("""^\s*(?:in mind\s+)?(?:that|to)?\b""", RegexOption.IGNORE_CASE), "").trim()
                if (tokens(after).any { it !in OBJECT } && tokens(after).size >= 2) rest = after
            }
            break
        }
        if (at == Int.MAX_VALUE) return s
        val head = s.substring(0, at).replace(TRAIL, "").trim().trim(',', ';', ':')
        return listOf(head, rest).filter { tokens(it).isNotEmpty() }.joinToString(" ").takeIf { it.isNotBlank() }
    }

    private fun clean(words: String): String = words.trim().trim(',', ';', ':', '-', '—').trim()
}
