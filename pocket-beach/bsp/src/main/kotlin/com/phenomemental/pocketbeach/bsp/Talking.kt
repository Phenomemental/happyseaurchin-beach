package com.phenomemental.pocketbeach.bsp

/**
 * How she talks, as the app guards it: what of her reply he is shown, never
 * her turn played back ([Shown]); her one question as the dial opens; her
 * name as his shell gives it; and the blocks his words name. Sundial 0.12,
 * Gemma released: her instruction is the time alone (HerWindow.released), so
 * the friend line and the lines that drew her are no longer here.
 */
object Talking {
    /** The name his shell gives her, from its latest "Matthew calls me …" line; null before he has named her. */
    fun name(shell: String, pages: Pages, at: String = ""): String? {
        val view = pages.position(Region.POCKET, shell, at) ?: return null
        return view.lines.mapNotNull { it.text }.mapNotNull { CALLS_ME.matchEntire(it.trim())?.groupValues?.get(1)?.trim()?.trim('"', '“', '”', '\'')?.trim() }
            .lastOrNull { it.isNotEmpty() && !it.endsWith("…") && !it.endsWith("...") }
    }

    private val CALLS_ME = Regex("""(?:Matthew|[\p{Lu}][\p{L}'’-]*) calls me (.+?)[.!]?""", RegexOption.IGNORE_CASE)

    /**
     * The block his safety words were kept in, before Sundial 0.12 took the
     * safety check out (spine:pocket-beach 1.111178719991 and 1.111178719999):
     * a block he kept stays his, private, and nothing reads it.
     */
    const val SAFETY_BLOCK = "safety-words"

    // ── her turn, never played back (0.6.1) ──

    private val YEAR = Regex("""\b(19|20)\d\d\b""")
    private val CLOCK = Regex("""\b\d{1,2}[:.]\d{2}\b""")
    private val WEEKDAYS = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

    /**
     * The lines of her [reply] that play her turn back, as she did on his
     * Pixel on 29 October: her shell's line, or six words of it in a row
     * (on the laptop she played it back as "I live on your phone, read
     * freely, and write only when you say yes"), the date and time,
     * "Matthew:" and his words, or his words whole ([said]: eight words of his
     * in a row; when he said fewer, a line that is his words and nothing
     * else, so "Good morning, Matthew" to his "Good morning" is no copy).
     *
     * Sundial 0.10.1 ([hisWords] false): where his cook asks her to give
     * back what he said in his own words (TALK's part one), his words in her
     * reply are hers to say, not her turn played back. On his Pixel on 3
     * November her reflection of "I've done some of the id checks today, …"
     * held eight of his words in a row, and was left out: on E4B part one
     * was missing; on E2B, her one line, the whole reply.
     */
    fun copied(reply: String, said: String, shellOpening: String, appLines: List<String> = emptyList(), hisWords: Boolean = true): List<String> {
        val his = Telling.plain(said).split(' ').filter { it.isNotEmpty() }
        val runs = if (his.size < 8) emptySet() else (0..his.size - 8).map { his.subList(it, it + 8).joinToString(" ") }.toSet()
        val whole = his.joinToString(" ")
        val shell = Telling.plain(shellOpening)
        val own = shell.split(' ').filter { it.isNotEmpty() }
        val shellRuns = if (own.size < 6) emptySet() else (0..own.size - 6).map { own.subList(it, it + 6).joinToString(" ") }.toSet()
        // 0.7: a line of the app's in her turn (the time, what has changed, the app's line), played back.
        val appRuns = appLines.flatMap { l -> Telling.plain(l).split(' ').filter { it.isNotEmpty() }.let { w -> if (w.size < 6) listOf(w.joinToString(" ")).filter { it.isNotEmpty() } else (0..w.size - 6).map { w.subList(it, it + 6).joinToString(" ") } } }.toSet()
        return reply.lines().filter { line ->
            val p = Telling.plain(line)
            if (p.isEmpty()) return@filter false
            if (line.trimStart().startsWith("(") && line.trimEnd().endsWith(")") && appLines.isNotEmpty()) return@filter true
            if (p.startsWith("the app ")) return@filter true
            if (appRuns.any { " $p ".contains(" $it ") }) return@filter true
            p.startsWith("shell ") || p.startsWith("your shell") || (shell.isNotEmpty() && p.contains(shell)) ||
                line.trimStart().trimStart('*', '_', '"').startsWith("Matthew:") ||
                (YEAR.containsMatchIn(line) && CLOCK.containsMatchIn(line)) ||
                ((p.startsWith("it is ") || p.startsWith("it's ")) && WEEKDAYS.any { p.contains(it) } && YEAR.containsMatchIn(line)) ||
                (hisWords && (runs.any { " $p ".contains(" $it ") } || (his.size < 8 && p == whole))) || shellRuns.any { " $p ".contains(" $it ") }
        }
    }

    /** Her [reply] with every line that plays her turn back left out: what Ask shows of her. */
    fun unechoed(reply: String, said: String, shellOpening: String, appLines: List<String> = emptyList(), hisWords: Boolean = true): String {
        val copies = copied(reply, said, shellOpening, appLines, hisWords).toSet()
        return reply.lines().filter { it !in copies }.joinToString("\n").trim().replace(Regex("\n{3,}"), "\n\n")
    }

    /**
     * Sundial 0.10.1: her words as he is shown them while they come, and at
     * the end: the one check every reply of hers passes before it is shown
     * (talk, her parts, the greeting).
     *
     * A piece that plays her turn back is left out ([copied]); and, from his
     * Pixel on 3 November (the greeting showed "(The app: Matthew —
     * philosopher, peer and digital tinkerer, …)", then her question in quote
     * marks, then "Kept."), a piece that begins "The app:", a piece that
     * copies what she was handed ([handed]: eight words of it in a row, not
     * his own words in this message), and a keep status ("Kept.") are never
     * shown as hers. A line wrapped whole in quote marks is shown without them.
     * Sundial 0.10.2: her places line ("Places: …") is the app's to read, and
     * is never shown.
     *
     * But a piece once shown is never taken out of view again, whatever
     * follows it (on his Pixel her reply showed, then went). So each piece
     * (a sentence; a line in brackets, a line beginning with a name and a
     * colon, or a line in quote marks whole) is shown only once it can be
     * judged: when the next has begun, when [JUDGED_AT] of its words have
     * come whole (never a line in brackets or quote marks, held to its end),
     * or when her reply is over ([of] with `over`); from then it stays.
     */
    class Shown(
        private val said: String, private val shellOpening: String, private val appLines: List<String> = emptyList(),
        private val hisWords: Boolean = true, handed: List<String> = emptyList(),
    ) {
        private val kept = HashSet<Pair<Int, Int>>()
        private val dropped = HashSet<Pair<Int, Int>>()
        private val handedRuns = runsOf(handed, said)

        /** What is shown of [text], her reply so far ([over]: all of it has come). */
        fun of(text: String, over: Boolean = false): String {
            val lines = text.lines()
            val out = mutableListOf<String>()
            for ((i, raw) in lines.withIndex()) {
                // A blank line between two of hers stays, as she set it.
                if (raw.isBlank()) { if (out.isNotEmpty()) out += ""; continue }
                val lineOver = over || i < lines.lastIndex
                val quoted = wrapsWhole(raw.trim())
                // A line in quote marks is held to its end: shown without them when they wrap it whole.
                if (OPENS.containsKey(raw.trim().firstOrNull() ?: ' ') && !lineOver) continue
                val line = if (quoted) raw.trim().drop(1).dropLast(1).trim() else raw
                val pieces = piecesOf(line)
                val shown = mutableListOf<String>()
                for ((j, piece) in pieces.withIndex()) {
                    val key = i to j
                    if (key in kept) { shown += piece; continue }
                    if (key in dropped || piece.isBlank()) continue
                    val pieceOver = lineOver || j < pieces.lastIndex
                    val held = piece.trimStart().let { it.startsWith("(") || it.startsWith("[") }
                    val judged = pieceOver || appLine(piece) || (!held && whole(piece) >= JUDGED_AT)
                    if (!judged) break
                    if (notHers(piece)) { dropped += key; continue }
                    shown += piece
                    kept += key
                }
                if (shown.isNotEmpty()) out += shown.joinToString(" ") { it.trim() }
            }
            return out.joinToString("\n").trim().replace(Regex("\n{3,}"), "\n\n")
        }

        /** Her words in [piece] that have come whole: the last, while a letter may still follow it, isn't yet. */
        private fun whole(piece: String): Int = Telling.plain(piece).split(' ').count { it.isNotEmpty() }.let { n -> if (piece.lastOrNull()?.isLetterOrDigit() == true) n - 1 else n }

        private fun notHers(piece: String): Boolean =
            appLine(piece) || keepStatus(piece) || HerReply.placesLine(piece) != null || copied(piece, said, shellOpening, appLines, hisWords).isNotEmpty() ||
                Telling.plain(piece).let { p -> handedRuns.any { " $p ".contains(" $it ") } }
    }

    /**
     * A sentence still coming is judged once twelve of its words have come
     * whole: a copy of eight words that begins by its fifth word is seen
     * before any of it shows. (At eight, "I've done some of the id checks
     * tod" was judged before "today" had come, and passed.)
     */
    const val JUDGED_AT = 12

    /**
     * Sundial 0.10.1: her reply as he may be shown it, all of it come: the
     * one check before display ([Shown]), for a reply that came whole (her
     * parts, the greeting) as for one streamed.
     */
    fun hers(reply: String, said: String = "", shellOpening: String = "", appLines: List<String> = emptyList(), hisWords: Boolean = false, handed: List<String> = emptyList()): String =
        Shown(said, shellOpening, appLines, hisWords, handed).of(reply, over = true)

    /**
     * Sundial 0.10.1: the greeting is his tomorrow line (the app's), then her
     * one question, and nothing else: of [reply], once checked ([hers]), the
     * first sentence that asks; null when she asked nothing, and Sundial's
     * own question stands.
     */
    fun oneQuestion(reply: String, appLines: List<String> = emptyList(), handed: List<String> = emptyList()): String? {
        val checked = hers(reply, appLines = appLines, handed = handed)
        return checked.lines().flatMap(::piecesOf).map { it.trim() }.firstOrNull { it.trimEnd('”', '"', '’', ')').endsWith("?") }
            ?.let { q -> if (wrapsWhole(q)) q.drop(1).dropLast(1).trim() else q }?.takeIf { it.isNotBlank() }
    }

    /** "The app:" at the start of a piece, inside a bracket or quote mark or not: the app's line, never hers. */
    fun appLine(piece: String): Boolean = piece.trimStart().trimStart('(', '[', '“', '"', '‘', '*', '_', ' ').startsWith("The app:", ignoreCase = true) ||
        piece.trimStart().trimStart('(', '[', '“', '"', '‘', '*', '_', ' ').startsWith("The app :", ignoreCase = true)

    /** A keep status ("Kept.", "Your day is kept."): the conversation never shows one; a kept day shows only as its shell. */
    fun keepStatus(piece: String): Boolean = KEEP_STATUS.matches(Telling.plain(piece))

    private val KEEP_STATUS = Regex("""(?:(?:your |his |the )?(?:day|today) (?:is |was |has been )?)?(?:not )?(?:kept|saved)(?: today)?""")

    private val OPENS = mapOf('“' to '”', '"' to '"', '«' to '»', '‘' to '’')

    /** Quote marks wrapping [t] whole: its first opens, its last closes, and no other of them stands between. */
    fun wrapsWhole(t: String): Boolean {
        if (t.length < 2) return false
        val close = OPENS[t.first()] ?: return false
        if (t.last() != close) return false
        val inside = t.substring(1, t.length - 1)
        return t.first() !in inside && close !in inside
    }

    /**
     * A line's pieces, as [Shown] judges them: its sentences (each to its
     * stop, and the quote mark or bracket closing it); whole, a line in
     * brackets, or one beginning with a name and a colon ("Matthew: …").
     */
    fun piecesOf(line: String): List<String> {
        val t = line.trimStart()
        if (t.startsWith("(") || t.startsWith("[") || SPEAKER.containsMatchIn(t)) return listOf(line)
        val out = mutableListOf<String>()
        var from = 0
        var i = 0
        while (i < line.length) {
            if (line[i] in ".!?…") {
                var k = i + 1
                while (k < line.length && line[k] in ".!?…”\"’)]") k++
                if (k < line.length && line[k].isWhitespace()) {
                    out += line.substring(from, k)
                    while (k < line.length && line[k].isWhitespace()) k++
                    from = k
                    i = k
                    continue
                }
                i = k
                continue
            }
            i++
        }
        out += line.substring(from)
        return out
    }

    private val SPEAKER = Regex("""^[*_"“]*\p{Lu}[\p{L}'’]*(?: \p{Lu}[\p{L}'’]*)?[*_]*:""")

    /** Eight words in a row of what she was handed, never eight that are his own words in this message ([said]). */
    private fun runsOf(handed: List<String>, said: String): Set<String> {
        fun runs(t: String): Set<String> = Telling.plain(t).split(' ').filter { it.isNotEmpty() }.let { w -> if (w.size < 8) emptySet() else (0..w.size - 8).map { w.subList(it, it + 8).joinToString(" ") }.toSet() }
        val his = runs(said)
        return handed.flatMap { runs(it) }.filter { it !in his }.toSet()
    }

    /** 0.6.1: his turn as she is given it, his words alone; the date, the time and her shell are in her instruction. */
    fun said(words: String): String = words.trim()

    /**
     * The blocks his words name whose lines she can't see whole in her
     * instruction: any teaching block, and a block of his with more than nine
     * lines or lines beneath its own; or, with [all] (0.6.1, whose instruction
     * sets out no block), any block of his. Two at most; never the shell, the
     * log or a clock.
     */
    fun named(said: String, pages: Pages, all: Boolean = false): List<String> {
        val lower = said.lowercase()
        fun names(name: String) = Regex("""(?<![\p{L}\p{N}])${Regex.escape(name.lowercase())}(?![\p{L}\p{N}])""").containsMatchIn(lower)
        val shore = pages.shore()
        val mine = shore.mine.map { it.name }.filter { it != Asks.LOG && it != HelloWorld.SHELL && names(it) && pages.floorOf(it) < TEMPORAL_FLOOR }.filter { name ->
            val lines = pages.position(Region.POCKET, name)?.lines.orEmpty()
            all || lines.size > 9 || lines.any { it.beneath > 0 }
        }
        val teaching = shore.teaching.map { it.name }.filter(::names)
        return (mine + teaching).distinct().take(2)
    }
}
