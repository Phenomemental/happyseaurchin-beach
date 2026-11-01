package com.phenomemental.pocketbeach.bsp

import java.time.ZonedDateTime

/**
 * Rockpool, pscale as procedure (spine:pocket-beach 1.111178714): the app's
 * procedures are a cook block in his pocket, its events a kernel block, and
 * this walker runs them exactly as they stand. It follows the beach's
 * convention (spine:code-as-blocks 1.34 and 1.43, cook:keel 1): a step is a
 * sentence; a step the app performs carries its formula in the sentence; a
 * step that names "she" is hers, a ring of at most nine, her digit taken; a
 * step that names "his yes" or "his tap" waits for him; a question's answers
 * stand beneath it; words in capitals are carried forward; every procedure
 * ends with ACCOUNT.
 *
 * The walker has a fixed set of verbs, each a sentence form below. A step it
 * can't read refuses the whole block, and the shipped copy runs instead
 * ([Cook.load]). What stays code is authority at the actuator: the guards,
 * which every write and post the walker asks for passes through.
 */
object Cook {
    /** The shipped copies, on the app's own shelf (pocket-beach/procedures), read only. */
    const val SHIPPED_COOK = "cook"
    const val SHIPPED_KERNEL = "kernel"
    const val P6 = "spine:p6"

    fun cookOf(handle: String) = "cook:$handle"
    fun kernelOf(handle: String) = "kernel:$handle"

    enum class Reader { WALKER, SHE, HIM }

    // ── the parse ──

    /** One step: its address, its sentence, its label ("LIFT", "POST", "Yes"), what it does, and what stands beneath it. */
    class Step(val at: String, val sentence: String, val label: String?, val verb: Verb, val kids: List<Step>, val ring: List<String>) {
        val reader: Reader get() = verb.reader
    }

    class Procedure(val place: String, val name: String, val opening: String, val steps: List<Step>)

    /** One line of cook 9: an answer's words, the words a clause begins with and the phrases it may hold. */
    class Cue(val label: String, val starts: List<String>, val phrases: List<String>)

    class Cues(val answers: Map<String, Cue>, val asking: List<String>, val intent: List<String>) {
        fun cue(label: String): Cue? = answers[label.uppercase()]
    }

    class Parsed(val procedures: Map<String, Procedure>, val cues: Cues, val refused: List<String>) {
        val ok: Boolean get() = refused.isEmpty() && procedures.isNotEmpty()
    }

    sealed class Verb(open val reader: Reader = Reader.WALKER) {
        class Which(val cues: String) : Verb()
        class IsSet(val name: String) : Verb()
        class NameHeld(val name: String) : Verb()
        object AsksHerLines : Verb()
        object TellsDay : Verb()
        class BlockStands(val name: String) : Verb()
        class Lift(val what: String, val sources: List<String>, val into: String) : Verb()
        class FindKeep(val into: String) : Verb()
        class FindAsked(val into: String) : Verb()
        class Read(val block: String, val whole: Int, val into: String) : Verb()
        class Point(val reading: String, val first: Int, val letters: Int, val other: Set<String>, val max: Int, val into: String) : Verb()
        class ChooseRing(val what: String, val toHim: Boolean, val into: String) : Verb(Reader.SHE)
        class ChooseFrom(val from: String, val toHim: Boolean, val into: String) : Verb(Reader.SHE)
        class ChooseEach(val each: String, val source: String, val into: String) : Verb(Reader.SHE)
        class Hand(val line: String, val with: List<String>) : Verb()
        class SetKeep(val words: String, val place: String) : Verb(Reader.HIM)
        class SetPost(val words: String) : Verb(Reader.HIM)
        class SetNew(val name: String, val words: String) : Verb(Reader.HIM)
        class SetName(val name: String) : Verb(Reader.HIM)
        class SetDay(val telling: String, val cards: String, val rungs: String, val pieces: String, val portals: String) : Verb(Reader.HIM)
        class SetCard(val card: String) : Verb(Reader.HIM)
        object OnYes : Verb()
        object OnTapSend : Verb()
        class OnTapRun(val place: String) : Verb()
        class Cut(val from: String, val into: String) : Verb()
        class Intents(val from: String, val cues: String, val into: String) : Verb()
        class Rungs(val from: String, val into: String) : Verb()
        class Run(val place: String) : Verb()
        class WritingCheck(val what: String) : Verb()
        object Close : Verb()
        class Account(val what: String) : Verb()
        object Stop : Verb()
        object WalkOn : Verb()
        /** An answer's label with its steps beneath, or an answer of a ring. */
        object Answer : Verb()
    }

    private const val CAP = """THE [A-Z][A-Z ]*[A-Z](?![A-Za-z])"""
    private fun rx(s: String) = Regex("^$s$")

    private val FORMS: List<Pair<Regex, (MatchResult) -> Verb>> = listOf(
        rx("""Which does THE MESSAGE ask for, by the words at cook (\d)\? Take the first answer beneath whose words match, in order; else the last\.""") to { m -> Verb.Which(m.groupValues[1]) },
        rx("""(?:Are there words of his in|Is there) ($CAP)\?""") to { m -> Verb.IsSet(m.groupValues[1]) },
        rx("""Does his shell already give her ($CAP)\?""") to { m -> Verb.NameHeld(m.groupValues[1]) },
        rx("""Does THE MESSAGE ask to change her own lines\?""") to { _ -> Verb.AsksHerLines },
        rx("""Does THE MESSAGE tell of his day: three sentences or more, of forty words or more, or news of a card on his hand\?""") to { _ -> Verb.TellsDay },
        rx("""Is there a block called ($CAP) already\?""") to { m -> Verb.BlockStands(m.groupValues[1]) },
        rx("""Lift (his words|his telling|the new block's name|the name he gives her) from THE MESSAGE: (.+?)\. Carry (?:it|them) as ($CAP)\.""") to { m ->
            Verb.Lift(m.groupValues[1], m.groupValues[2].split(Regex(""";\s*else\s+|;\s*""")).map { it.trim() }.filter { it.isNotEmpty() }, m.groupValues[3])
        },
        rx("""Find the block THE MESSAGE names to keep ($CAP) in: one of his own, never his passport, shell or history\. Carry it as ($CAP)\.""") to { m -> Verb.FindKeep(m.groupValues[2]) },
        rx("""Find the block THE MESSAGE asks about: .+? Carry it as ($CAP)\.""") to { m -> Verb.FindAsked(m.groupValues[1]) },
        rx("""Read ($CAP) at its aperture: whole when it is ([\d,]+) characters or fewer, else as a ring of its places by their opening lines\. Carry it as ($CAP)\.""") to { m ->
            Verb.Read(m.groupValues[1], m.groupValues[2].replace(",", "").toInt(), m.groupValues[3])
        },
        rx("""Find the place of ($CAP) his words point to: the one whose first ([\d,]+) characters share the most of his words of (\w+) letters or more, other than (.+?), the first if two share as many, and read it whole, up to ([\d,]+) characters\. Carry it as ($CAP)\.""") to { m ->
            Verb.Point(m.groupValues[1], m.groupValues[2].replace(",", "").toInt(), number(m.groupValues[3]) ?: 4, m.groupValues[4].split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet(),
                m.groupValues[5].replace(",", "").toInt(), m.groupValues[6])
        },
        rx("""She chooses (.+?), from the ring beneath; on E2B, or when her answer isn't whole, (the ring goes to him|nothing is chosen)\. Carry the choice as ($CAP)\.""") to { m ->
            Verb.ChooseRing(m.groupValues[1], m.groupValues[2] == "the ring goes to him", m.groupValues[3])
        },
        rx("""She chooses from ($CAP) .+?; on E2B, or when her answer isn't whole, (the ring goes to him|nothing is chosen)\. Carry the choice as ($CAP)\.""") to { m ->
            Verb.ChooseFrom(m.groupValues[1], m.groupValues[2] == "the ring goes to him", m.groupValues[3])
        },
        rx("""For each of ($CAP), she chooses its portal from the ring beneath, each portal read from (spine:p6); on E2B, or when her answer isn't whole, the piece waits for him to place it\. Carry the choices as ($CAP)\.""") to { m ->
            Verb.ChooseEach(m.groupValues[1], m.groupValues[2], m.groupValues[3])
        },
        rx("""Hand her this line: “(.+)”""") to { m -> Verb.Hand(m.groupValues[1], emptyList()) },
        rx("""Hand her ($CAP(?: and $CAP)*) with this line: “(.+)”""") to { m -> Verb.Hand(m.groupValues[2], m.groupValues[1].split(" and ").map { it.trim() }) },
        rx("""Set out the keeping of ($CAP) in ($CAP) beneath her reply, for his yes\.""") to { m -> Verb.SetKeep(m.groupValues[1], m.groupValues[2]) },
        rx("""Set out a post of ($CAP) to his room beneath her reply, for his tap\.""") to { m -> Verb.SetPost(m.groupValues[1]) },
        rx("""Set out a new block called ($CAP), with ($CAP) as its opening line, beneath her reply, for his yes\.""") to { m -> Verb.SetNew(m.groupValues[1], m.groupValues[2]) },
        rx("""Set out her name, ($CAP), at her place in his shell beneath her reply, for his yes\.""") to { m -> Verb.SetName(m.groupValues[1]) },
        rx("""Set out his day beneath her reply, for his yes: ($CAP) as the day's line at today's date in his now, ($CAP), ($CAP), and each of ($CAP) beneath ($CAP) in his P6\.""") to { m ->
            Verb.SetDay(m.groupValues[1], m.groupValues[2], m.groupValues[3], m.groupValues[4], m.groupValues[5])
        },
        rx("""Set out the card “(.+?)” beneath her reply, for his tap\.""") to { m -> Verb.SetCard(m.groupValues[1]) },
        rx("""On his yes, write it\.""") to { _ -> Verb.OnYes },
        rx("""On his tap, send it\.""") to { _ -> Verb.OnTapSend },
        rx("""On his tap, run cook (\d)\.""") to { m -> Verb.OnTapRun(m.groupValues[1]) },
        rx("""Cut ($CAP) into its sentences\. Carry them as ($CAP)\.""") to { m -> Verb.Cut(m.groupValues[1], m.groupValues[2]) },
        rx("""Find among ($CAP) what he means to do, by the words at cook (\d\.\d): .+ Carry them as ($CAP)\.""") to { m -> Verb.Intents(m.groupValues[1], m.groupValues[2], m.groupValues[3]) },
        rx("""Find among ($CAP) those that name this week, this month, this season or this year\. Carry them as ($CAP), each at its level of his now\.""") to { m -> Verb.Rungs(m.groupValues[1], m.groupValues[2]) },
        rx("""Run cook (\d)\.""") to { m -> Verb.Run(m.groupValues[1]) },
        rx("""Hand ($CAP) to the writing check\.""") to { m -> Verb.WritingCheck(m.groupValues[1]) },
        rx("""Append one entry to his history: .+""") to { _ -> Verb.Close },
        rx("""Stop\.""") to { _ -> Verb.Stop },
        rx("""walk on\.""") to { _ -> Verb.WalkOn },
    )

    private val NUMBERS = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
    private fun number(s: String): Int? = s.toIntOrNull() ?: NUMBERS.indexOf(s.lowercase()).takeIf { it >= 0 }?.plus(1)

    /** A label at a sentence's start: words in capitals (or "Yes", "No") ending in a stop or a colon. Never "THE …", which is carried. */
    private val LABEL = Regex("""^((?!THE )[A-Z][A-Z' ]*[A-Z]|Yes|No)([.:])\s*""")

    /** The sentence of a step, read: its label and its verb; null when no form fits. */
    fun read(text: String): Pair<String?, Verb>? {
        val t = text.trim()
        val label = LABEL.find(t)
        val rest = label?.let { t.substring(it.range.last + 1).trim() } ?: t
        val name = label?.groupValues?.get(1)
        if (rest.isEmpty()) return name to Verb.Answer
        if (name == "ACCOUNT") return name to Verb.Account(rest)
        for ((form, verb) in FORMS) form.matchEntire(rest)?.let { return name to verb(it) }
        return null
    }

    private fun digits(o: JsObj): List<String> = (1..9).map { "$it" }.filter { o.has(it) }

    private fun step(node: Js, at: String, refused: MutableList<String>, ringOf: Boolean = false): Step? {
        val text = when (node) {
            is JsStr -> node.value
            is JsObj -> (node["_"] as? JsStr)?.value ?: collectUnderscore(node).orEmpty()
            else -> ""
        }
        val read = read(text)
        if (read == null) {
            refused += "$at: “${text.take(120)}”"
            return null
        }
        val (label, verb) = read
        val o = node as? JsObj
        val beneath = o?.let(::digits).orEmpty()
        // A she-step's children are the ring offered her, as written; any other step's are its answers or its steps.
        if (verb is Verb.ChooseRing || verb is Verb.ChooseEach) {
            val ring = beneath.map { k -> (o!![k] as? JsStr)?.value ?: collectUnderscore(o[k]!!).orEmpty() }
            return Step(at, text, label, verb, emptyList(), ring)
        }
        val kids = beneath.mapNotNull { k -> step(o!![k]!!, "$at.$k", refused) }
        return Step(at, text, label, verb, kids, emptyList())
    }

    /** His cook read: each procedure's steps, cook 9's words, and every step it couldn't read. */
    fun parse(block: Js?): Parsed {
        val refused = mutableListOf<String>()
        val o = block as? JsObj ?: return Parsed(emptyMap(), Cues(emptyMap(), emptyList(), emptyList()), listOf("the cook block is missing or isn't a block"))
        val procedures = LinkedHashMap<String, Procedure>()
        var cues = Cues(emptyMap(), emptyList(), emptyList())
        for (k in digits(o)) {
            val node = o[k] as? JsObj ?: continue
            val opening = (node["_"] as? JsStr)?.value ?: continue
            if (opening.startsWith("THE WORDS THE APP LISTENS FOR")) {
                cues = cues(node)
                continue
            }
            val name = opening.substringBefore(" —").substringBefore(" -").trim()
            val steps = digits(node).mapNotNull { s -> step(node[s]!!, "$k.$s", refused) }
            procedures[k] = Procedure(k, name, opening, steps)
        }
        if (cues.answers.isEmpty()) refused += "cook 9: the words the app listens for are missing"
        return Parsed(procedures, cues, refused)
    }

    private fun cues(node: JsObj): Cues {
        val answers = LinkedHashMap<String, Cue>()
        var asking = emptyList<String>()
        var intent = emptyList<String>()
        for (k in digits(node)) {
            val line = (node[k] as? JsStr)?.value ?: collectUnderscore(node[k]!!) ?: continue
            val label = line.substringBefore(':').trim().substringBefore(',').trim()
            val body = line.substringAfter(':', "").trim()
            fun list(s: String) = s.split(',').map { it.trim().lowercase().replace('’', '\'') }.filter { it.isNotEmpty() }
            when {
                label.startsWith("THE ASKING") -> asking = list(body)
                label.startsWith("WHAT HE MEANS TO DO") -> intent = list(body)
                else -> {
                    val starts = list(body.substringBefore('—', body.takeIf { '—' !in it } ?: ""))
                    val phrases = if ('—' in body) list(body.substringAfter('—')) else emptyList()
                    answers[label.uppercase()] = Cue(label.uppercase(), starts, phrases)
                }
            }
        }
        return Cues(answers, asking, intent)
    }

    /** The kernel read: which procedure his message runs, and which the close runs. */
    class Kernel(val message: String?, val close: String?)

    fun kernel(block: Js?): Kernel {
        val o = block as? JsObj ?: return Kernel(null, null)
        fun run(label: String): String? = digits(o).mapNotNull { k -> (o[k] as? JsStr)?.value ?: collectUnderscore(o[k]!!) }
            .firstOrNull { it.trim().startsWith(label) }?.let { Regex("""Run cook (\d)\.""").find(it)?.groupValues?.get(1) }
        return Kernel(run("HIS MESSAGE"), run("THE CLOSE OF A CONVERSATION"))
    }

    // ── his words against cook 9 ──

    private fun norm(s: String) = s.lowercase().replace('’', '\'').replace('‘', '\'').replace('“', '"').replace('”', '"')

    /** A clause of his words with its asking taken off its start, again and again. */
    fun unasked(clause: String, asking: List<String>): String {
        var c = clause.trim()
        val lead = Regex("""^(?:(?:${asking.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }})\b[\s,]*)+""")
        if (asking.isNotEmpty()) c = c.replace(lead, "").trim()
        return c
    }

    private fun clauses(said: String): List<String> = norm(said).split(Regex("""[.!?;:\n,]+""")).map { it.trim() }.filter { it.isNotEmpty() }

    /** A phrase of cook 9 as a pattern: "…" stands for up to five of his words. */
    fun phrase(p: String): Regex {
        val parts = p.split('…').map { it.trim() }
        val body = parts.joinToString("""(?:\s+[\p{L}\p{N}'"]+){0,5}?\s+""") { part ->
            Regex.escape(part).let { if (part.firstOrNull()?.isLetterOrDigit() == true) "\\b$it" else it }
                .let { if (part.lastOrNull()?.isLetterOrDigit() == true) "$it\\b" else it }
        }
        return Regex(body)
    }

    /** Whether his words ask for [cue]: a clause, its asking taken off, begins with one of its words; or his words hold one of its phrases. */
    fun asks(said: String, cue: Cue, asking: List<String>): Boolean {
        val text = norm(said)
        if (clauses(said).map { unasked(it, asking) }.any { c -> cue.starts.any { w -> Regex("""^${Regex.escape(w)}\b""").containsMatchIn(c) } }) return true
        return cue.phrases.any { phrase(it).containsMatchIn(text) }
    }

    /** Where in [said] the cue first asks: the start of the matching clause, or of the phrase; null when it doesn't. */
    fun askAt(said: String, cue: Cue, asking: List<String>): IntRange? {
        val text = norm(said)
        var best: IntRange? = null
        for (p in cue.phrases) phrase(p).find(text)?.let { m -> if (best == null || m.range.first < best!!.first) best = m.range }
        var from = 0
        for (raw in text.split(Regex("""(?<=[.!?;:\n,])"""))) {
            val start = from
            from += raw.length
            val c = raw.trim().trimEnd('.', '!', '?', ';', ':', ',')
            val un = unasked(c, asking)
            val lead = c.length - un.length
            val hit = cue.starts.firstOrNull { w -> Regex("""^${Regex.escape(w)}\b""").containsMatchIn(un) } ?: continue
            val at = start + (raw.length - raw.trimStart().length) + lead
            if (best == null || at < best!!.first) best = at until at + hit.length
        }
        return best
    }

    // ── the walk ──

    /** What the walker needs of the world: his pocket, the shelf, the hour, which model she is, and the acts it asks for. */
    interface World {
        val handle: String
        val pages: Pages
        val storage: BlockStorage
        val shelf: Shelf?
        val now: ZonedDateTime
        /** E4B: a ring goes to her; E2B: to him. */
        val big: Boolean
        /** What he said before in this conversation, newest last. */
        val before: List<String>
        /** His cards on his hand, by address, for "news of a card". */
        fun cards(): Map<String, String> = emptyMap()
        /** Carry out a card he said yes to, through the guards: null when done, else why not. */
        fun write(card: Card): String?
        /** Send his post on his tap: null when sent (or kept to send), else why not. */
        fun send(words: String): String? = null
        /** Write the close's history entry: null when written, else why not. */
        fun close(): String? = null
    }

    /** A card beneath her reply, for his yes or his tap. */
    sealed class Card {
        data class Keep(val block: String, val words: String) : Card()
        data class Post(val words: String) : Card()
        data class New(val block: String, val words: String) : Card()
        data class Name(val name: String) : Card()
        data class Day(val landing: Days.DayCard) : Card()
        data class Offer(val title: String) : Card()
        /** The ring given to him, where her choice would have been: his places, at most nine. */
        data class Ring(val words: String, val places: List<String>) : Card()
    }

    /** What the walker waits for. */
    sealed class Wait {
        /** Her choice from each ring: a digit, or null when her answer isn't whole. */
        data class Her(val question: String, val rings: List<Ring>) : Wait()
        data class Him(val card: Card) : Wait()
    }

    /** One ring offered her: the piece it is about ("" for one), and its places as she reads them. */
    data class Ring(val about: String, val places: List<String>)

    /** One step taken, as the film strip shows it. */
    data class Trace(val at: String, val reader: Reader, val did: String, val carried: Map<String, String> = emptyMap()) {
        override fun toString(): String = "cook $at (${reader.name.lowercase().let { if (it == "walker") "the app" else it }}): $did" +
            (if (carried.isEmpty()) "" else " — " + carried.entries.joinToString("; ") { (k, v) -> "$k = ${shortly(v)}" })
    }

    /** A line handed her, with what was read for her beneath it. */
    data class Handed(val line: String, val material: List<String> = emptyList())

    private class Frame(val steps: List<Step>, var i: Int = 0)

    /** The material read for her: a ring of places or a block whole, by its name as she is told it. */
    data class Reading(val block: String, val whole: Boolean, val text: String, val places: List<Pair<String, String>>)

    /** A place read whole: the block, its address, its text. */
    data class PlaceRead(val block: String, val key: String, val text: String)

    class Walk(val parsed: Parsed, val world: World, message: String, private val matched: String? = null) {
        val carried = LinkedHashMap<String, Any>()
        val trace = mutableListOf<Trace>()
        val handed = mutableListOf<Handed>()
        val accounts = mutableListOf<String>()
        val reads = mutableListOf<String>()
        var writingCheck = false
            private set
        var waiting: Wait? = null
            private set
        var done = false
            private set
        /** The card he was last asked about, and the procedure's answer that his message asked for. */
        var card: Card? = null
            private set
        var answer: String? = null
            private set
        private val stack = ArrayDeque<Frame>()
        private var current: Step? = null
        private var pendingHer: Step? = null

        init {
            carried["THE MESSAGE"] = message
        }

        fun start(place: String): Walk {
            val p = parsed.procedures[place] ?: error("cook $place doesn't stand")
            trace += Trace(place, Reader.WALKER, "begins ${p.name}")
            stack.addLast(Frame(p.steps))
            return go()
        }

        private fun str(name: String): String = when (val v = carried[name]) {
            is String -> v
            is Reading -> v.text
            is PlaceRead -> v.text
            null -> ""
            else -> v.toString()
        }

        private fun has(name: String): Boolean = when (val v = carried[name]) {
            null -> false
            is String -> v.isNotBlank()
            is List<*> -> v.isNotEmpty()
            else -> true
        }

        private fun put(name: String, value: Any?) {
            if (value == null || (value is String && value.isBlank())) carried.remove(name) else carried[name] = value
        }

        /** Run until the walker waits for her or for him, or the walk ends. */
        fun go(): Walk {
            while (waiting == null && !done) {
                val frame = stack.lastOrNull() ?: run { done = true; break }
                if (frame.i >= frame.steps.size) {
                    stack.removeLast()
                    continue
                }
                val s = frame.steps[frame.i++]
                current = s
                perform(s)
            }
            return this
        }

        private fun note(s: Step, did: String, vararg names: String) {
            trace += Trace(s.at, s.reader, did, names.filter { carried.containsKey(it) }.associateWith { str(it) })
        }

        private fun answerOf(s: Step, i: Int) {
            val a = s.kids.getOrNull(i) ?: return
            if (a.verb is Verb.Answer) stack.addLast(Frame(a.kids)) else stack.addLast(Frame(listOf(a)))
        }

        private fun perform(s: Step) {
            when (val v = s.verb) {
                is Verb.Which -> {
                    val cues = parsed.cues
                    val said = str("THE MESSAGE")
                    val i = s.kids.indexOfFirst { a -> a.label?.let(cues::cue)?.let { asks(said, it, cues.asking) } == true }.takeIf { it >= 0 } ?: (s.kids.size - 1)
                    answer = s.kids.getOrNull(i)?.label
                    put("THE ANSWER", answer)
                    note(s, "his message asks for ${answer ?: "nothing"}", "THE ANSWER")
                    answerOf(s, i)
                }
                is Verb.IsSet -> yesNo(s, has(v.name), "${v.name} ${if (has(v.name)) "holds something" else "holds nothing"}")
                is Verb.NameHeld -> {
                    val held = Talking.name(Hatch.shellOf(world.handle), world.pages, Hatch.HER)?.let { Telling.plain(it) == Telling.plain(str(v.name)) } ?: false
                    yesNo(s, held, if (held) "his shell already gives her ${str(v.name)}" else "his shell doesn't give her ${str(v.name)} yet")
                }
                Verb.AsksHerLines -> HerWindow.asksHerOpening(str("THE MESSAGE")).let { yesNo(s, it, if (it) "he asks to change her lines" else "he asks to change a line of his") }
                Verb.TellsDay -> Landing.isTelling(str("THE MESSAGE"), world.cards(), runCatching { Landing.names(world.pages) }.getOrDefault(emptySet()))
                    .let { yesNo(s, it, if (it) "his words tell of his day" else "his words don't tell of his day") }
                is Verb.BlockStands -> world.pages.exists(str(v.name)).let { yesNo(s, it, if (it) "a block called ${str(v.name)} stands" else "no block called ${str(v.name)} stands") }
                is Verb.Lift -> {
                    put(v.into, lift(v))
                    note(s, "lifted ${v.what}", v.into)
                }
                is Verb.FindKeep -> {
                    val said = str("THE MESSAGE")
                    val found = Keeping.named(said, world.pages).firstOrNull { !Keeping.own(it, world.pages) && it !in HerWindow.HERS }
                        ?: Regex("""\bmy\s+stash\b""", RegexOption.IGNORE_CASE).find(said)?.let { Hatch.stashOf(world.handle) }
                    put(v.into, found)
                    note(s, if (found != null) "his words name where to keep it" else "his words name no block of his to keep it in", v.into)
                }
                is Verb.FindAsked -> {
                    val said = str("THE MESSAGE")
                    val found = (HerWindow.hisFour(said, world.storage) + Talking.named(said, world.pages, all = true)).distinct().firstOrNull { it !in HerWindow.HERS }
                    put(v.into, found)
                    note(s, if (found != null) "his words ask about a block of his" else "his words name no block of his", v.into)
                }
                is Verb.Read -> {
                    val r = readBlock(str(v.block), v.whole)
                    put(v.into, r)
                    if (r != null) reads += "${r.block}${if (r.whole) ", whole" else ", its places by their opening lines"}"
                    note(s, if (r == null) "nothing to read" else if (r.whole) "read ${r.block} whole" else "read ${r.block} as a ring of ${r.places.size} places", v.into)
                }
                is Verb.Point -> {
                    val r = carried[v.reading] as? Reading
                    val p = r?.takeIf { !it.whole }?.let { point(it, v.first, v.letters, v.other, v.max) }
                    put(v.into, p)
                    if (p != null) reads += "${p.block} ${p.key}, whole"
                    note(s, if (p != null) "his words point to place ${p.key}" else "his words point to no one place", v.into)
                }
                is Verb.ChooseRing -> {
                    val places = ringPlaces(s.ring)
                    if (places.isEmpty()) {
                        note(s, "no ring to offer")
                    } else if (!world.big) {
                        if (v.toHim) wait(s, Wait.Him(Card.Ring(str("THE WORDS"), places)), "the ring goes to him (E2B)")
                        else note(s, "nothing is chosen (E2B)")
                    } else {
                        pendingHer = s
                        waiting = Wait.Her(v.what, listOf(Ring(str("THE WORDS"), places.map { HerWindow.called(it) })))
                        carried["_ring"] = places
                    }
                }
                is Verb.ChooseFrom -> {
                    val r = carried[v.from] as? Reading
                    if (r == null || r.whole || r.places.isEmpty()) note(s, "nothing to choose from")
                    else if (!world.big) note(s, "nothing is chosen (E2B)")
                    else {
                        pendingHer = s
                        waiting = Wait.Her("the place to read whole first", listOf(Ring("", r.places.map { (k, t) -> "$k. $t" })))
                    }
                }
                is Verb.ChooseEach -> {
                    @Suppress("UNCHECKED_CAST") val pieces = (carried[v.each] as? List<String>).orEmpty()
                    val portals = s.ring.map { portal(it) }
                    if (pieces.isEmpty()) note(s, "no pieces")
                    else if (!world.big) {
                        carried[v.into] = pieces.map { null as String? }
                        note(s, "each piece waits for him to place it (E2B)", v.into)
                    } else {
                        pendingHer = s
                        waiting = Wait.Her("its portal", pieces.map { Ring(it, portals.map { p -> p.second }) })
                        carried["_portals"] = portals.map { it.first }
                    }
                }
                is Verb.Hand -> {
                    val line = fill(v.line)
                    val material = v.with.mapNotNull { name ->
                        when (val x = carried[name]) {
                            is Reading -> if (x.whole) "(${HerWindow.called(x.block).let { "His $it" }}, his lines as he wrote them:)\n${x.text}"
                                else "(His ${HerWindow.called(x.block)}, its places by their opening lines:)\n${x.text}"
                            is PlaceRead -> "(Place ${x.key} of his ${HerWindow.called(x.block)}, read whole:)\n${x.text}"
                            is String -> x.takeIf { it.isNotBlank() }
                            else -> null
                        }
                    }
                    handed += Handed(line, material)
                    note(s, "handed her \u201c${shortly(line)}\u201d" + (if (material.isNotEmpty()) ", and what was read" else ""))
                }
                is Verb.SetKeep -> setOut(s, Card.Keep(str(v.place), str(v.words)), "set out the keeping, for his yes")
                is Verb.SetPost -> setOut(s, Card.Post(str(v.words)), "set out his post, for his tap")
                is Verb.SetNew -> setOut(s, Card.New(str(v.name), str(v.words)), "set out a new block, for his yes")
                is Verb.SetName -> setOut(s, Card.Name(str(v.name)), "set out her name, for his yes")
                is Verb.SetCard -> setOut(s, Card.Offer(v.card), "set out the card “${v.card}”, for his tap")
                is Verb.SetDay -> {
                    @Suppress("UNCHECKED_CAST") val pieces = (carried[v.pieces] as? List<String>).orEmpty()
                    @Suppress("UNCHECKED_CAST") val portals = (carried[v.portals] as? List<String?>).orEmpty()
                    @Suppress("UNCHECKED_CAST") val cards = (carried[v.cards] as? List<Days.Intent>).orEmpty()
                    @Suppress("UNCHECKED_CAST") val rungs = (carried[v.rungs] as? List<Days.Rung>).orEmpty()
                    val landing = Days.landing(world.handle, str(v.telling), pieces, portals, cards, rungs, world.now)
                    setOut(s, Card.Day(landing), "set out his day, for his yes")
                }
                Verb.OnYes -> {
                    val c = card
                    val why = if (c == null) "nothing was set out" else world.write(c)
                    if (why == null) accounts += written(c!!)
                    note(s, if (why == null) "wrote it" else "couldn't write it: $why")
                    if (why != null) done = true
                }
                Verb.OnTapSend -> {
                    val why = (card as? Card.Post)?.let { world.send(it.words) } ?: if (card !is Card.Post) "nothing was set out to send" else null
                    if (why == null) accounts += "posted his words to his room"
                    note(s, if (why == null) "sent it" else "couldn't send it: $why")
                }
                is Verb.OnTapRun -> {
                    note(s, "runs cook ${v.place}")
                    run(v.place)
                }
                is Verb.Cut -> {
                    val pieces = Telling.sentences(str(v.from), runCatching { Landing.names(world.pages) }.getOrDefault(emptySet()))
                    carried[v.into] = pieces
                    note(s, "cut it into ${pieces.size} sentence${if (pieces.size == 1) "" else "s"}", v.into)
                }
                is Verb.Intents -> {
                    @Suppress("UNCHECKED_CAST") val pieces = (carried[v.from] as? List<String>).orEmpty()
                    val intents = Days.intents(pieces, parsed.cues.intent, world.now.toLocalDate(), Days.timeWords(world.storage))
                    carried[v.into] = intents
                    note(s, "found ${intents.size} thing${if (intents.size == 1) "" else "s"} he means to do", v.into)
                }
                is Verb.Rungs -> {
                    @Suppress("UNCHECKED_CAST") val pieces = (carried[v.from] as? List<String>).orEmpty()
                    val rungs = Days.rungs(pieces)
                    carried[v.into] = rungs
                    note(s, "found ${rungs.size} rung${if (rungs.size == 1) "" else "s"}", v.into)
                }
                is Verb.Run -> {
                    note(s, "runs cook ${v.place}")
                    run(v.place)
                }
                is Verb.WritingCheck -> {
                    writingCheck = true
                    note(s, "handed it to the writing check")
                }
                Verb.Close -> {
                    val why = world.close()
                    note(s, if (why == null) "appended one entry to his history" else "the entry couldn't be written: $why")
                }
                is Verb.Account -> {
                    accounts += "${s.at.substringBefore('.')}: ${v.what}"
                    note(s, "accounted")
                }
                Verb.Stop -> {
                    note(s, "stops")
                    stack.clear()
                    done = true
                }
                Verb.WalkOn -> {}
                Verb.Answer -> stack.addLast(Frame(s.kids))
            }
        }

        private fun yesNo(s: Step, yes: Boolean, did: String) {
            note(s, did)
            answerOf(s, if (yes) 0 else 1)
        }

        private fun run(place: String) {
            val p = parsed.procedures[place]
            if (p == null) {
                trace += Trace(place, Reader.WALKER, "cook $place doesn't stand")
                return
            }
            trace += Trace(place, Reader.WALKER, "begins ${p.name}")
            stack.addLast(Frame(p.steps))
        }

        private fun setOut(s: Step, c: Card, did: String) {
            card = c
            wait(s, Wait.Him(c), did)
        }

        private fun wait(s: Step, w: Wait, did: String) {
            waiting = w
            note(s, did)
        }

        /** Her choices, a digit for each ring (null where her answer wasn't whole): the walk goes on. */
        fun her(choices: List<Int?>): Walk {
            val s = pendingHer ?: error("the walker isn't waiting for her")
            pendingHer = null
            waiting = null
            when (val v = s.verb) {
                is Verb.ChooseRing -> {
                    @Suppress("UNCHECKED_CAST") val places = carried.remove("_ring") as List<String>
                    val d = choices.firstOrNull()?.takeIf { it in 1..places.size }
                    if (d != null) {
                        put(v.into, places[d - 1])
                        note(s, "she chose $d", v.into)
                    } else if (v.toHim) {
                        wait(s, Wait.Him(Card.Ring(str("THE WORDS"), places)), "her answer wasn't whole: the ring goes to him")
                    } else note(s, "her answer wasn't whole: nothing is chosen")
                }
                is Verb.ChooseFrom -> {
                    val r = carried[v.from] as Reading
                    val d = choices.firstOrNull()?.takeIf { it in 1..r.places.size }
                    if (d != null) {
                        val key = r.places[d - 1].first
                        val p = placeWhole(r.block, key, HerWindow.PLACE_MAX)
                        put(v.into, p)
                        if (p != null) reads += "${p.block} ${p.key}, whole"
                        note(s, "she chose $d", v.into)
                    } else note(s, "her answer wasn't whole: nothing is chosen")
                }
                is Verb.ChooseEach -> {
                    @Suppress("UNCHECKED_CAST") val portals = carried.remove("_portals") as List<String>
                    val chosen = choices.map { d -> d?.takeIf { it in 1..portals.size }?.let { portals[it - 1] } }
                    carried[v.into] = chosen
                    note(s, "she chose ${chosen.count { it != null }} of ${chosen.size} portals", v.into)
                }
                else -> {}
            }
            return go()
        }

        /** His tap on the ring he was given: the place he chose. */
        fun hisChoice(place: String): Walk {
            val s = current ?: error("nothing waits")
            waiting = null
            val into = (s.verb as? Verb.ChooseRing)?.into ?: "THE PLACE"
            put(into, place)
            note(s, "he chose", into)
            return go()
        }

        /** His yes, or his tap: the walk goes on from the step after the one that waited. */
        fun yes(): Walk {
            waiting = null
            return go()
        }

        /** His "not now": the walk ends there. */
        fun notNow(): Walk {
            current?.let { trace += Trace(it.at, Reader.HIM, "he said not now") }
            waiting = null
            done = true
            stack.clear()
            return this
        }

        /** [card] as he changed it before saying yes (his day with a line moved or unticked). */
        fun changed(c: Card) {
            card = c
            waiting = (waiting as? Wait.Him)?.let { Wait.Him(c) } ?: waiting
        }

        private fun written(c: Card): String = when (c) {
            is Card.Keep -> "kept in ${HerWindow.called(c.block)}: “${shortly(c.words)}”"
            is Card.Post -> "posted his words"
            is Card.New -> "made a block called ${c.block}"
            is Card.Name -> "her name in his shell: ${c.name}"
            is Card.Day -> "his day: ${c.landing.describe()}"
            is Card.Offer -> c.title
            is Card.Ring -> "a place chosen"
        }

        private fun fill(line: String): String = Regex(CAP).replace(line) { m -> carried[m.value]?.let { str(m.value) } ?: m.value }

        // ── lifting ──

        private fun lift(v: Verb.Lift): String? {
            val said = str("THE MESSAGE")
            val cues = parsed.cues
            val cue = matched?.let(cues::cue) ?: answer?.let(cues::cue)
            return when (v.what) {
                "the name he gives her" -> liftName(said, cues.cue("HER NAME") ?: cue)
                "the new block's name" -> Keeping.made(said)
                else -> {
                    for (source in v.sources) {
                        val got = when {
                            source.startsWith("after the colon") -> afterColon(said, cue, cues.asking)
                            source.startsWith("in quotes") -> QUOTED.find(said)?.groupValues?.get(1)?.trim()?.takeIf { words(it).isNotEmpty() }
                            source.startsWith("his sentences") -> sentencesUnasked(said, cue, cues.asking)
                            source.startsWith("what he said before") -> world.before.lastOrNull()?.let(::clean)
                            else -> null
                        }
                        if (!got.isNullOrBlank()) return got
                    }
                    null
                }
            }
        }

        private fun liftName(said: String, cue: Cue?): String? {
            if (cue == null || said.trim().endsWith("?")) return null
            val at = cue.phrases.mapNotNull { p -> phrase(p).find(norm(said))?.let { it.range.last + 1 } }.minOrNull() ?: return null
            val rest = said.substring(at).trim()
            val m = Regex("""^([\p{L}][\p{L}'’-]*)(?:\s+([\p{Lu}][\p{L}'’-]*))?""").find(rest) ?: return null
            val first = m.groupValues[1].trim('\'', '’', '-')
            if (first.lowercase() in NOT_NAMES || first.length < 2) return null
            val second = m.groupValues[2].takeIf { it.isNotEmpty() && it.lowercase() !in NOT_NAMES && it != "I" }
            return listOfNotNull(first, second).joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
        }

        private fun afterColon(said: String, cue: Cue?, asking: List<String>): String? {
            val colon = said.indexOf(':')
            if (colon <= 0) return null
            val before = said.substring(0, colon)
            if (cue != null && !asks("$before.", cue, asking)) return null
            val after = said.substring(colon + 1).trim()
            val inner = QUOTED.matchEntire(after)?.groupValues?.get(1)?.trim() ?: after.removeSurrounding("'").trim()
            return clean(inner).takeIf { words(it).isNotEmpty() }
        }

        /** His sentences, each that asks with its asking cut out: what comes before the ask, and what follows an asking verb ("Note that …"). */
        private fun sentencesUnasked(said: String, cue: Cue?, asking: List<String>): String? {
            val sentences = Regex("""[^.!?\n]+[.!?]*""").findAll(said.trim()).map { it.value.trim() }.filter { it.isNotEmpty() }.toList()
            val kept = sentences.mapNotNull { s ->
                if (cue == null || !asks(s, cue, asking)) s else cut(s, cue, asking)
            }.joinToString(" ").trim()
            return clean(kept).takeIf { words(it).any { w -> w !in asking } }
        }

        private fun cut(s: String, cue: Cue, asking: List<String>): String? {
            val at = askAt(s, cue, asking) ?: return s
            val head = s.substring(0, at.first).let { h -> stripTrailing(h, asking) }
            // What follows a starting verb: "Note that the tulips came", "Remember to buy stamps".
            val verb = cue.starts.firstOrNull { w -> norm(s).substring(at.first).startsWith(w) }
            val rest = if (verb != null) s.substring(at.first + verb.length).replace(Regex("""^\s*(?:in mind\s+)?(?:that|to)?\b""", RegexOption.IGNORE_CASE), "").trim()
                .takeIf { r -> words(r).count { it !in OBJECT } >= 1 && words(r).size >= 2 } else null
            return listOfNotNull(head.takeIf { words(it).isNotEmpty() }, rest).joinToString(" ").takeIf { it.isNotBlank() }
        }

        private fun stripTrailing(h: String, asking: List<String>): String {
            var t = h.trim().trim(',', ';', ':', '-', '—').trim()
            val tail = Regex("""(?:[\s,]+|\b(?:${(asking + listOf("i", "you", "for me")).sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }})\b)+$""", RegexOption.IGNORE_CASE)
            t = t.replace(tail, "").trim().trim(',', ';', ':')
            return t
        }

        // ── reading ──

        private fun readBlock(name: String, whole: Int): Reading? {
            val block = world.storage.read(name)?.let { runCatching { parseJson(it) }.getOrNull() } ?: return null
            val lines = HerWindow.linesOf(block)
            val full = lines.joinToString("\n") { (a, t) -> "  ".repeat(a.count { it == '.' }) + "- " + t }
            if (full.length <= whole) return Reading(name, true, full, emptyList())
            val o = block as? JsObj ?: return Reading(name, true, full.take(whole), emptyList())
            val places = (1..9).map { "$it" }.filter { o.has(it) }.mapNotNull { k -> (o[k]?.let { (it as? JsStr)?.value ?: collectUnderscore(it) })?.trim()?.takeIf { it.isNotEmpty() }?.let { k to firstSentence(it) } }
            return Reading(name, false, places.joinToString("\n") { (k, t) -> "$k. $t" }, places)
        }

        private fun point(r: Reading, first: Int, letters: Int, other: Set<String>, max: Int): PlaceRead? {
            val words = Regex("""[\p{L}']+""").findAll(str("THE MESSAGE").lowercase()).map { it.value.trim('\'') }.filter { it.length >= letters && it !in other }.toSet()
            val block = world.storage.read(r.block)?.let { runCatching { parseJson(it) }.getOrNull() } as? JsObj ?: return null
            val key = r.places.map { (k, _) ->
                val t = (block[k] as? JsStr)?.value ?: collectUnderscore(block[k]!!).orEmpty()
                k to Regex("""[\p{L}']+""").findAll(clip(t, first).lowercase()).map { it.value }.count { it in words }
            }.filter { it.second > 0 }.maxByOrNull { it.second }?.first ?: return null
            return placeWhole(r.block, key, max)
        }

        private fun placeWhole(name: String, key: String, max: Int): PlaceRead? {
            val block = world.storage.read(name)?.let { runCatching { parseJson(it) }.getOrNull() } as? JsObj ?: return null
            val place = block[key] ?: return null
            val text = listOf((place as? JsStr)?.value ?: collectUnderscore(place).orEmpty()) + HerWindow.linesOf(place).map { (a, t) -> "  ".repeat(a.count { it == '.' }) + "- " + t }
            return PlaceRead(name, key, clip(text.filter { it.isNotBlank() }.joinToString("\n"), max))
        }

        /** The ring a she-step offers, as its lines say: block names, "his stash", "his other blocks …"; nine at most. */
        private fun ringPlaces(ring: List<String>): List<String> {
            val his = runCatching { hisBlocks() }.getOrDefault(emptyList())
            val out = mutableListOf<String>()
            for (line in ring) {
                val l = line.trim().lowercase()
                when {
                    l.startsWith("his stash") -> out += Hatch.stashOf(world.handle)
                    l.startsWith("his other blocks") -> out += his.filter { it !in out }
                    else -> line.trim().trimEnd('.').let { n -> if (world.pages.exists(n) && !Keeping.own(n, world.pages)) out += n }
                }
            }
            return out.distinct().filter { world.storage.read(it) != null || it == Hatch.stashOf(world.handle) }.take(9)
        }

        /** His own blocks in his manifest's order, never the app's own, her shell or the four his hatch made. */
        private fun hisBlocks(): List<String> = world.pages.shore().mine.map { it.name }
            .filter { !Keeping.own(it, world.pages) && it != HelloWorld.SHELL && it != HelloWorld.BLOCK && !it.startsWith("bench-") && it !in HerWindow.HERS && !it.startsWith("stash:") && !it.startsWith("cook:") && !it.startsWith("kernel:") && !it.startsWith("ahead:") && !it.startsWith("p6:") && !it.startsWith("now:") }

        /** A portal of spine:p6, as the ring line names it ("spine:p6 1.1"): its address and its words. */
        private fun portal(line: String): Pair<String, String> {
            val at = Regex("""(\d\.\d)""").find(line)?.groupValues?.get(1) ?: line
            val p6 = world.shelf?.load(P6) as? JsObj
            val node = p6?.let { o -> at.split('.').fold<String, Js?>(o) { n, d -> (n as? JsObj)?.get(d) } }
            val text = node?.let { (it as? JsStr)?.value ?: collectUnderscore(it) } ?: at
            return at to text
        }
    }

    private val QUOTED = Regex("""[“"]([^“”"]+)[”"]""")
    private val NOT_NAMES = setOf(
        "the", "a", "an", "that", "this", "it", "something", "anything", "whatever", "what", "later", "back", "tomorrow", "soon", "again", "now",
        "when", "if", "by", "for", "to", "and", "my", "your", "his", "her", "please", "then", "today", "tonight", "i",
    )
    private val OBJECT = setOf("this", "that", "it", "these", "those", "all", "of", "some", "for", "me", "please", "down", "safe", "too", "as", "well", "in", "mind")

    private fun words(text: String): List<String> = Regex("""[\p{L}\p{N}']+""").findAll(norm(text)).map { it.value.trim('\'') }.filter { it.isNotEmpty() }.toList()
    private fun clean(words: String): String = words.trim().trim(',', ';', ':', '-', '—').trim()
    private fun clip(s: String, n: Int) = if (s.length <= n) s else s.take(n - 1).trimEnd() + "…"
    private fun firstSentence(t: String) = Regex("""^.{1,200}?[.!?](?=\s|$)""").find(t)?.value ?: clip(t, 200)
    fun shortly(s: String) = s.replace("\n", " / ").let { if (it.length <= 90) it else it.take(89).trimEnd() + "…" }

    // ── which copy runs ──

    /** The cook that runs: his own, as it stands, when it reads; else the shipped copy, and why. */
    class Loaded(val parsed: Parsed, val kernel: Kernel, val own: Boolean, val refusal: String?)

    fun load(handle: String?, storage: BlockStorage, procedures: Shelf?): Loaded {
        val shipped = parse(procedures?.load(SHIPPED_COOK))
        val shippedKernel = kernel(procedures?.load(SHIPPED_KERNEL))
        if (handle == null) return Loaded(shipped, shippedKernel, false, null)
        val mine = storage.read(cookOf(handle))?.let { runCatching { parseJson(it) }.getOrNull() } ?: return Loaded(shipped, shippedKernel, false, null)
        val parsed = parse(mine)
        val kernel = storage.read(kernelOf(handle))?.let { runCatching { parseJson(it) }.getOrNull() }?.let(::kernel)?.takeIf { it.message != null } ?: shippedKernel
        val missing = (listOf(kernel.message, kernel.close).filterNotNull() - parsed.procedures.keys)
        return when {
            !parsed.ok -> Loaded(shipped, shippedKernel, false, "Your cook couldn't be read at ${parsed.refused.first()}, so the shipped copy ran.")
            missing.isNotEmpty() -> Loaded(shipped, shippedKernel, false, "Your kernel names cook ${missing.first()}, which doesn't stand, so the shipped copy ran.")
            else -> Loaded(parsed, kernel, true, null)
        }
    }

    // ── bringing them into his pocket ──

    /** The plans that bring the shipped cook and kernel into his pocket, or up to date when he hasn't changed them ([lastShipped] is what was shipped before). Empty when nothing is to do. */
    fun bringIn(handle: String, pages: Pages, storage: BlockStorage, procedures: Shelf, lastShipped: (String) -> String? = { null }): List<Plan> {
        val plans = mutableListOf<Plan>()
        for ((shipped, mine) in listOf(SHIPPED_COOK to cookOf(handle), SHIPPED_KERNEL to kernelOf(handle))) {
            val block = procedures.load(shipped) as? JsObj ?: continue
            val now = storage.read(mine)
            val up = when {
                now == null -> true
                deepEqual(parseJson(now), block) -> false
                else -> lastShipped(shipped)?.let { last -> runCatching { deepEqual(parseJson(now), parseJson(last)) }.getOrDefault(false) } == true
            }
            if (up) plans += pages.planWhole(mine, parseJson(stringify(block)!!) as JsObj)
        }
        return plans
    }
}

/**
 * The walker's cards carried out as the app carries out its own offers
 * (Asking.byApp): planned on a copy through Acts.propose, then through
 * Asks.carryOut, so the same guards hold every write. His day is written
 * whole, each block planned on the pocket as it stands.
 */
object CookActs {
    fun actions(card: Cook.Card, handle: String, pages: Pages): List<Acts.Action> = when (card) {
        is Cook.Card.Keep -> listOf(Acts.Action.Add(card.block, null, card.words))
        is Cook.Card.Post -> listOf(Acts.Action.Post(card.words))
        is Cook.Card.New -> listOf(Acts.Action.New(card.block, card.words.ifBlank { null }))
        is Cook.Card.Name -> listOf(Hatch.nameAction(handle, pages, card.name))
        else -> emptyList()
    }

    /** The proposal a card makes, planned on a copy: what he says yes to. */
    fun planned(card: Cook.Card, handle: String, pages: Pages, storage: BlockStorage, shelf: Shelf, nowMs: Long): Acts.Planned =
        Acts.propose("", actions(card, handle, pages), storage, shelf, shellOpening(pages), HelloWorld.seal(), nowMs)

    fun shellOpening(pages: Pages): String = pages.position(Region.POCKET, HelloWorld.SHELL)?.text ?: HelloWorld.SHELL_OPENING

    /** Carry [card] out: null when done, else why not. His day is written whole, block by block. */
    fun carryOut(card: Cook.Card, handle: String, pages: Pages, storage: BlockStorage, shelf: Shelf, nowMs: Long): String? {
        if (card is Cook.Card.Day) {
            val written = Days.blocks(card.landing, { name -> storage.read(name)?.let { runCatching { parseJson(it) }.getOrNull() } }, nowMs)
            for ((name, block) in written.blocks) {
                val done = pages.carryOut(pages.planWhole(name, block))
                if (!done.ok) return done.problem ?: "$name couldn't be written"
            }
            return null
        }
        val p = planned(card, handle, pages, storage, shelf, nowMs)
        val proposal = p.proposal ?: return p.problem ?: "nothing to write"
        proposal.refusal?.let { return it }
        return Asks.carryOut(proposal, pages, storage, shellOpening(pages)).problem
    }
}
