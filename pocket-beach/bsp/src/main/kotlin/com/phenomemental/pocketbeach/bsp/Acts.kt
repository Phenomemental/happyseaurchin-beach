package com.phenomemental.pocketbeach.bsp

/**
 * 0.4's action language: what the companion writes to act, one short line an
 * action, naming a line by quoting its words, never by its address. The app
 * finds each quoted line, opens up a plain line when one goes beneath it, and
 * plans the same guarded writes as ever, all shown before Matthew says yes.
 * A quote that more than one line matches is put to him ([Planned.choose]).
 *
 *     new <block>                          make a block
 *     new <block>: <opening line>          make it, with its opening line
 *     add <block>: <line>                  a line beneath its opening line
 *     add <block> under "<quote>": <line>  a line beneath the quoted line
 *     change <block>: <line>               its opening line, rewritten
 *     change <block> "<quote>": <line>     the quoted line, rewritten
 *     read <block>                         what it holds (or: read <block> "<quote>")
 *     post: <message>                      a post offered to Matthew's pool
 */
object Acts {
    sealed class Action {
        data class New(val block: String, val opening: String?) : Action()
        data class Add(val block: String, val under: String?, val text: String) : Action()
        data class Change(val block: String, val line: String?, val text: String) : Action()
        data class Read(val block: String, val line: String?) : Action()
        data class Post(val message: String) : Action()
    }

    class Unreadable(message: String) : Exception(message)

    /** How the companion is told to act, for its standing instruction. */
    val GRAMMAR: String = listOf(
        "new <block>",
        "new <block>: <its opening line>",
        "add <block>: <line>",
        "add <block> under \"<the words of a line>\": <line>",
        "change <block>: <its new opening line>",
        "change <block> \"<the words of a line>\": <the line rewritten>",
        "read <block>",
        "post: <message>",
    ).joinToString("\n")

    private const val Q = """["“”]([^"“”]+)["“”]"""
    private val NEW = Regex("""^(?:new|make)\s+(?:block\s+)?(.+?)(?:\s*:\s*(.*))?$""", RegexOption.IGNORE_CASE)
    // A block's name, as she writes it in an action: no quote or colon in it.
    private const val B = """([^"“”:]+?)"""
    private val ADD_UNDER = Regex("""^add\s+$B\s+(?:under|beneath|below)\s+$Q\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)
    private val ADD = Regex("""^add\s+$B\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)
    private val CHANGE_LINE = Regex("""^change\s+$B\s+$Q\s*(?::\s*|\s+to\s*:?\s*|(?=["“]))(.+)$""", RegexOption.IGNORE_CASE)
    private val CHANGE = Regex("""^change\s+$B\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)
    // Half a change: the line, its new words on the next line ("change garden "old"" then "change garden "new"").
    private val CHANGE_HALF = Regex("""^change\s+$B\s+$Q\s*$""", RegexOption.IGNORE_CASE)
    private val READ = Regex("""^read\s+(.+?)(?:\s+$Q)?\s*$""", RegexOption.IGNORE_CASE)
    private val POST = Regex("""^post\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)

    /**
     * The actions in [text], a line each. Blank lines, list marks and a code
     * fence around them are let be; a line that is none of the actions is
     * unreadable, and says which.
     */
    fun parse(text: String): List<Action> {
        val actions = mutableListOf<Action>()
        val lines = text.lines().map { it.trim().removePrefix("```").trim().replace(Regex("""^(?:[-*•]|\d+[.)])\s+"""), "").trim() }.filter { it.isNotEmpty() }
        var i = 0
        while (i < lines.size) {
            val line = lines[i++]
            val half = CHANGE_HALF.matchEntire(line)
            val next = half?.let { lines.getOrNull(i)?.let(CHANGE_HALF::matchEntire) }
            if (half != null && next != null && half.groupValues[1].trim() == next.groupValues[1].trim()) {
                actions += Action.Change(half.groupValues[1].trim().removePrefix("pocket/"), half.groupValues[2].trim(), next.groupValues[2].trim())
                i++
                continue
            }
            actions += one(line) ?: throw Unreadable("\"${line.take(80)}\" isn't one of the actions.")
        }
        if (actions.isEmpty()) throw Unreadable("There was no action.")
        return actions
    }

    private fun one(line: String): Action? {
        fun name(s: String) = s.trim().trim('"', '“', '”', '`').removePrefix("pocket/").trim()
        fun words(s: String) = s.trim().let { if (it.length >= 2 && it.first() in "\"“" && it.last() in "\"”") it.substring(1, it.length - 1).trim() else it }
        ADD_UNDER.matchEntire(line)?.let { return Action.Add(name(it.groupValues[1]), it.groupValues[2].trim(), words(it.groupValues[3])) }
        ADD.matchEntire(line)?.let { return Action.Add(name(it.groupValues[1]), null, words(it.groupValues[2])) }
        CHANGE_LINE.matchEntire(line)?.let { return Action.Change(name(it.groupValues[1]), it.groupValues[2].trim(), words(it.groupValues[3])) }
        CHANGE.matchEntire(line)?.let { return Action.Change(name(it.groupValues[1]), null, words(it.groupValues[2])) }
        READ.matchEntire(line)?.let { m -> return Action.Read(name(m.groupValues[1]), m.groupValues[2].trim().ifEmpty { null }) }
        POST.matchEntire(line)?.let { return Action.Post(words(it.groupValues[1])) }
        NEW.matchEntire(line)?.let { return Action.New(name(it.groupValues[1]), it.groupValues[2].takeIf { o -> o.isNotBlank() }?.let(::words)) }
        return null
    }

    // ── finding a quoted line ──

    /** What a quote found in a block: one line, several (to put to Matthew), or none. */
    sealed class Found {
        class One(val line: Line) : Found()
        class Many(val lines: List<Line>) : Found()
        class None(val why: String) : Found()
    }

    /** Every line of a block, at every depth, with its address. */
    fun lines(pages: Pages, block: String): List<Line> {
        val out = mutableListOf<Line>()
        fun walk(address: String) {
            val view = pages.position(Region.POCKET, block, address) ?: return
            for (l in view.lines) {
                out += l
                if (l.beneath > 0) walk(l.address)
            }
        }
        walk("")
        return out
    }

    private fun plain(s: String) = s.lowercase().replace(Regex("""\s+"""), " ").trim().trim('.', ',', ';', ':', '!', '?', ' ')

    /**
     * The line [quote] names in [block]: the same words first, then the same
     * words whatever their case and the stops at their ends, then a line
     * holding the quote; the first of these to find any decides, and finding
     * more than one asks Matthew which.
     */
    fun find(pages: Pages, block: String, quote: String): Found {
        if (!pages.exists(block)) return Found.None("There's no block called $block.")
        val all = lines(pages, block).filter { it.text != null }
        val q = plain(quote)
        val tiers = listOf<(Line) -> Boolean>(
            { it.text == quote.trim() },
            { plain(it.text!!) == q },
            { q.isNotEmpty() && plain(it.text!!).contains(q) },
        )
        for (tier in tiers) {
            val hits = all.filter(tier)
            if (hits.size == 1) return Found.One(hits.single())
            if (hits.size > 1) return Found.Many(hits)
        }
        return Found.None("No line in $block says “${quote.trim()}”.")
    }

    // ── planning ──

    /** A quote more than one line matches, put to Matthew: which of [lines] [action] (by its place in the answer) means. */
    class Choice(val action: Int, val block: String, val quote: String, val lines: List<Line>)

    /**
     * What an act comes to: the proposal (writes on a copy, shown before
     * Yes), or a quote to put to Matthew first ([choose]), or why it can't go
     * ahead ([problem]); and the reads it asks for.
     */
    class Planned(val proposal: Proposal?, val choose: Choice?, val problem: String?, val reads: List<Step>, val dropped: List<String> = emptyList()) {
        /** Every action she wrote was let be, each a line that already has its home or was cut short: nothing is proposed. */
        val nothing: Boolean get() = proposal == null && choose == null && problem == null && reads.isEmpty() && dropped.isNotEmpty()
    }

    /**
     * 0.6.2, one home per fact: a line cut short ("Matthew calls me…"), or
     * one still holding a slot ("<the name>"), is never proposed. On his
     * Pixel (30 October) the Write job proposed "Matthew calls me…" beneath
     * her shell's opening though "Matthew calls me Gemma." stood there.
     */
    fun cutShort(text: String): Boolean = text.trimEnd().let { it.endsWith("…") || it.endsWith("...") } || Regex("""<[^<>]+>""").containsMatchIn(text)

    /** The line of [block] (its opening line too) that already says [text], the same words whatever their case and the stops at their ends; null when none does. */
    fun saying(pages: Pages, block: String, text: String): String? {
        val t = plain(text)
        if (t.isEmpty() || !pages.exists(block)) return null
        pages.position(Region.POCKET, block)?.text?.takeIf { plain(it) == t }?.let { return it }
        return lines(pages, block).firstOrNull { it.text != null && plain(it.text) == t }?.text
    }

    /**
     * Plan [actions] on a copy of the pocket, in order, each quote found on
     * the copy as the actions before it left it. [chosen] holds Matthew's
     * answers to quotes put to him: an action's place to the address he
     * picked.
     */
    fun propose(
        say: String,
        actions: List<Action>,
        storage: BlockStorage,
        shelf: Shelf,
        shellOpening: String,
        seal: String,
        nowMs: Long,
        chosen: Map<Int, String> = emptyMap(),
        secret: String? = null,
    ): Planned {
        val trial = MemoryStorage()
        for (name in storage.names()) storage.read(name)?.let { trial.write(name, it) }
        trial.setStamps(storage.stamps())
        val pages = Pages(trial, shelf, PocketBsp(trial, shelf), clock = { nowMs })
        val run = Asks.Run(pages, trial, shellOpening, seal)
        val steps = mutableListOf<Step>()
        val reads = mutableListOf<Step>()
        val dropped = mutableListOf<String>()
        val post = actions.filterIsInstance<Action.Post>().lastOrNull()?.let { Offer(HelloWorld.POOL, it.message) }

        fun address(i: Int, block: String, quote: String): Pair<String?, Planned?> {
            chosen[i]?.let { return it to null }
            val f = find(pages, block, quote)
            // A quote of the block's opening names the block itself: beneath it is the block's own lines.
            val opening = pages.position(Region.POCKET, block)?.text
            if (f !is Found.One && opening != null && plain(opening) == plain(quote)) return "" to null
            return when (f) {
                is Found.One -> f.line.address to null
                is Found.Many -> null to Planned(null, Choice(i, block, quote, f.lines), null, reads)
                is Found.None -> null to Planned(null, null, "Action ${i + 1}: ${f.why}", reads)
            }
        }

        // A line she adds that the block already holds, only to quote it in a later action, is said, not meant: let be.
        val restated = actions.withIndex().filter { (i, a) ->
            a is Action.Add && a.under == null && lines(pages, pages.named(a.block)).any { it.text != null && plain(it.text) == plain(a.text) } &&
                actions.drop(i + 1).any { b -> (b as? Action.Add)?.under?.let { plain(it) == plain(a.text) } == true || (b as? Action.Change)?.line?.let { plain(it) == plain(a.text) } == true }
        }.map { it.index }.toSet()

        // A hello world made in this act: its first line is the shell's opening, which the app writes if she didn't.
        val makesHello = actions.any { it is Action.New && it.block == HelloWorld.BLOCK }
        var shellLine = false

        for ((i, typed) in actions.withIndex()) {
            if (i in restated) continue
            // A block named in other capitals is that block: speech writes "million", and the block is Million.
            // A new block, with words, of a name already in the pocket is those words kept in it.
            val action = when (typed) {
                is Action.New -> pages.named(typed.block).let { named ->
                    if (typed.opening != null && pages.exists(named)) Action.Add(named, null, typed.opening) else typed
                }
                is Action.Add -> typed.copy(block = pages.named(typed.block))
                is Action.Change -> typed.copy(block = pages.named(typed.block))
                is Action.Read -> typed.copy(block = pages.named(typed.block))
                else -> typed
            }
            // 0.6.2, one home per fact: a line already in its block, or one cut short, is not proposed again; nor written beneath a line that already says it.
            // (The hello world is the recipe's own, written whole each time: let be.)
            val again = if (makesHello || (action as? Action.Add)?.block == HelloWorld.BLOCK) null else when (action) {
                is Action.New -> action.opening?.takeIf(::cutShort)?.let { "\u201c$it\u201d was cut short" }
                is Action.Add -> if (cutShort(action.text)) "\u201c${action.text}\u201d was cut short"
                    else saying(pages, action.block, action.text)?.let { "${action.block} already says \u201c$it\u201d" }
                is Action.Change -> if (cutShort(action.text)) "\u201c${action.text}\u201d was cut short"
                    else saying(pages, action.block, action.text)?.let { "${action.block} already says \u201c$it\u201d" }
                else -> null
            }
            if (again != null) {
                dropped += again
                continue
            }
            val step = when (action) {
                is Action.New -> Step(Act.MAKE, action.block, "", action.opening)
                is Action.Add -> {
                    val at = action.under?.let { q -> address(i, action.block, q).let { (a, stop) -> a ?: return stop!! } } ?: ""
                    // The shell's opening, written out in the hello world, is the line the app fills: {{shell}}.
                    val text = if (action.block == HelloWorld.BLOCK && plain(action.text).contains(plain(shellOpening))) HelloWorld.SHELL_LINES.first() else action.text
                    Step(Act.ADD, action.block, at, text)
                }
                is Action.Change -> {
                    val at = action.line?.let { q -> address(i, action.block, q).let { (a, stop) -> a ?: return stop!! } } ?: ""
                    Step(Act.CHANGE, action.block, at, action.text)
                }
                is Action.Read -> {
                    val at = action.line?.let { q -> address(i, action.block, q).let { (a, stop) -> a ?: return stop!! } } ?: ""
                    reads += Step(Act.READ, action.block, at, null)
                    continue
                }
                is Action.Post -> continue
            }
            // The secret he gave, where she left its slot: the app writes it in, and he sees it before Yes.
            val written = if (secret != null && step.text?.contains(HelloWorld.SECRET_SLOT) == true) Step(step.act, step.block, step.at, step.text.replace(HelloWorld.SECRET_SLOT, secret)) else step
            if (written.text != null && written.text.contains(HelloWorld.SECRET_SLOT)) {
                return Planned(null, null, "Action ${i + 1} leaves ${HelloWorld.SECRET_SLOT} as it is; the secret Matthew gave goes in its place.", reads)
            }
            if (makesHello && written.act == Act.ADD && written.block == HelloWorld.BLOCK) {
                if (written.text == HelloWorld.SHELL_LINES.first()) {
                    shellLine = true
                } else if (!shellLine) {
                    val app = Step(Act.ADD, HelloWorld.BLOCK, "", HelloWorld.SHELL_LINES.first(), byApp = true)
                    (run.step(app, null) as? Asks.Outcome.Stopped)?.let { r -> return Planned(null, null, "The shell's line in ${HelloWorld.BLOCK}: ${r.why}", reads) }
                    steps += app
                    shellLine = true
                }
            }
            for (s in Asks.expand(listOf(written))) {
                when (val r = run.step(s, null)) {
                    is Asks.Outcome.Stopped -> return Planned(null, null, "Action ${i + 1}: ${r.why}", reads)
                    is Asks.Outcome.Landed -> {}
                }
            }
            steps += written
        }
        if (steps.isEmpty() && post == null && reads.isEmpty() && dropped.isNotEmpty()) return Planned(null, null, null, reads, dropped)
        val answer = Answer(say, steps, post)
        val hello = Asks.isHello(answer)
        if (hello) {
            run.sealAndFingerprint()?.let { why -> return Planned(Proposal(answer, seal, run.done, "${Asks.APP_WRITE} ${HelloWorld.BLOCK}: $why", null, null), null, null, reads) }
        }
        val (offered, postRefusal) = run.post(post, hello)
        return Planned(Proposal(answer, seal, run.done, null, offered, postRefusal), null, null, reads, dropped)
    }
}
