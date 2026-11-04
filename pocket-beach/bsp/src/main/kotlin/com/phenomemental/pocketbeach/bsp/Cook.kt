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

    class Cues(val answers: Map<String, Cue>, val asking: List<String>, val intent: List<String>, val joins: List<String> = emptyList()) {
        fun cue(label: String): Cue? = answers[label.uppercase()]
    }

    class Parsed(val procedures: Map<String, Procedure>, val cues: Cues, val refused: List<String>, val unreadable: List<Pair<String, String>> = emptyList()) {
        val ok: Boolean get() = refused.isEmpty() && procedures.isNotEmpty()
    }

    sealed class Verb(open val reader: Reader = Reader.WALKER) {
        class Which(val cues: String) : Verb()
        class IsSet(val name: String) : Verb()
        class NameHeld(val name: String) : Verb()
        object AsksHerLines : Verb()
        /** Sundial 0.10.3: the day check as its step's sentence states it, every number and clause read from it. */
        class TellsDay(val check: Landing.DayCheck) : Verb()
        /** Sundial 0.10.1: whether [of]'s words say he may be in danger, by his safety words: the app's own check, never hers. */
        class Danger(val of: String) : Verb()
        class BlockStands(val name: String) : Verb()
        class Lift(val what: String, val sources: List<String>, val into: String) : Verb()
        /** [many]: where two or more named blocks are carried (0.9.1); null in the 0.9 sentence, which carries the first named only. */
        class FindKeep(val into: String, val stash: Boolean = false, val many: String? = null) : Verb()
        class FindAsked(val into: String) : Verb()
        class Read(val block: String, val whole: Int, val into: String) : Verb()
        class Point(val reading: String, val first: Int, val letters: Int, val other: Set<String>, val max: Int, val into: String, val cutLonger: Boolean = false) : Verb()
        /** [among]: the ring is only these places, in the ring's order (0.9.1's KEEP). */
        class ChooseRing(val what: String, val toHim: Boolean, val into: String, val among: String? = null) : Verb(Reader.SHE)
        class ChooseFrom(val from: String, val toHim: Boolean, val into: String) : Verb(Reader.SHE)
        class ChooseEach(val each: String, val source: String, val into: String) : Verb(Reader.SHE)
        class Hand(val line: String, val with: List<String>) : Verb()
        class SetKeep(val words: String, val place: String) : Verb(Reader.HIM)
        class SetPost(val words: String) : Verb(Reader.HIM)
        class SetNew(val name: String, val words: String) : Verb(Reader.HIM)
        class SetName(val name: String) : Verb(Reader.HIM)
        /** 0.9's form with P6 ([pieces], [portals]) still reads for one release, and writes no P6 (Sundial 0.10). */
        class SetDay(val telling: String, val cards: String, val rungs: String, val pieces: String? = null, val portals: String? = null) : Verb(Reader.HIM)
        /** Sundial 0.10, the keep card: the sheet as the day's line at [dayGrain], the places last, each moment at [grain], his hand, bookings and rungs. */
        class KeepDay(val sheet: String, val dayGrain: String, val places: String, val moments: String, val grain: String, val cards: String, val rungs: String) : Verb(Reader.HIM)
        class Carry(val from: String, val into: String) : Verb()
        class Gather(val into: String) : Verb()
        /** Her placing: every place [of] touches, from the ring beneath ([how] is what she is told). */
        class NameMany(val of: String, val how: String, val toHim: Boolean, val into: String) : Verb(Reader.SHE)
        class Untouched(val touched: String, val into: String) : Verb()
        /** Her reply in named parts, through the tool call on E4B; Sundial 0.10.1: or in one of [shapes], hers to choose. */
        class Parts(val names: List<String>, val shapes: List<HerReply.Shape> = emptyList()) : Verb(Reader.SHE)
        class Sheet(val from: String, val withToday: Boolean, val line: String, val into: String) : Verb(Reader.SHE)
        class LastDay(val into: String) : Verb()
        /** Sundial 0.10.2: the tomorrow line of his last day kept ("Yesterday he wrote this line for today: “…”"); none when it left none. */
        class LastTomorrow(val into: String) : Verb()
        class DaysIn(val n: Int, val place: String, val into: String) : Verb()
        class Whispers(val place: String, val into: String) : Verb()
        class Ways(val place: String, val into: String) : Verb()
        class SetWhisper(val place: String, val question: String) : Verb(Reader.HIM)
        class SetCard(val card: String) : Verb(Reader.HIM)
        object OnYes : Verb()
        object OnTapSend : Verb()
        class OnTapRun(val place: String) : Verb()
        /** [joins]: where the joins live (cook 9.4.1) when a spoken telling is cut at them (0.9.1); null, by its sentences only. */
        class Cut(val from: String, val into: String, val joins: String? = null, val perStop: Int = 30) : Verb()
        /** [carriesOn]: a piece cut at a join, with no I of its own, carries on the one before; [appointments]: what a booking names whatever he means (0.9.1). */
        class Intents(val from: String, val cues: String, val into: String, val carriesOn: Boolean = false, val appointments: List<String> = emptyList(), val naming: Boolean = false) : Verb()
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
    /**
     * A form, as the app reads a step against it. Sundial 0.10.3: the app's
     * own words are read loosely, his words inside quotes freely: a line
     * break anywhere (his line may run over several), a quote mark straight
     * or curly, an apostrophe straight or curly, and any run of spaces read
     * as one. On his Pixel on 4 November his SHEET step, changed only inside
     * its quotes, was refused whole.
     */
    private fun rx(s: String) = Regex("^" + loosely(s) + "$", RegexOption.DOT_MATCHES_ALL)

    /** [pattern] with its literal quote marks, apostrophes and spaces made loose, outside character classes. */
    internal fun loosely(pattern: String): String {
        val out = StringBuilder()
        var depth = 0
        var escaped = false
        var lastSpace = false
        for (ch in pattern) {
            if (escaped) { out.append(ch); escaped = false; lastSpace = false; continue }
            if (ch == '\\') { out.append(ch); escaped = true; lastSpace = false; continue }
            if (depth > 0) { out.append(ch); if (ch == ']') depth--; lastSpace = false; continue }
            when (ch) {
                '[' -> { depth++; out.append(ch); lastSpace = false }
                '“', '”', '"' -> { out.append(if (ch == '“') "[“\"]" else if (ch == '”') "[”\"]" else "[“”\"]"); lastSpace = false }
                '\'', '’' -> { out.append("['’]"); lastSpace = false }
                ' ' -> { if (!lastSpace) out.append("\\s+"); lastSpace = true }
                else -> { out.append(ch); lastSpace = false }
            }
        }
        return out.toString()
    }

    private val FORMS: List<Pair<Regex, (MatchResult) -> Verb>> = listOf(
        rx("""Which does THE MESSAGE ask for, by the words at cook (\d)\? Take the first answer beneath whose words match, in order; else the last\.""") to { m -> Verb.Which(m.groupValues[1]) },
        rx("""(?:Are there words of his in|Is there) ($CAP)\?""") to { m -> Verb.IsSet(m.groupValues[1]) },
        rx("""Does his shell already give her ($CAP)\?""") to { m -> Verb.NameHeld(m.groupValues[1]) },
        rx("""Does THE MESSAGE ask to change her own lines\?""") to { _ -> Verb.AsksHerLines },
        rx("""Does THE MESSAGE tell of his day: three sentences or more(?:, of| and) forty words or more(?: in all)?, or news of a card on his hand\?""") to { _ -> Verb.TellsDay(Landing.DayCheck.SINCE_09) },
        // Sundial 0.10.3: the sentence is the check: change a number or a clause, and the app checks what it now says.
        rx("""Does THE MESSAGE tell of his day: ([\w-]+) sentences? or more and ([\w-]+) words? or more in all(?:, or a word of ([\w-]+) letters? or more that a card on his hand also holds)?\?(?: Never when it ends with a question mark(?:, or opens with one of: ([^.]+))?\.)?""") to { m ->
            Verb.TellsDay(
                Landing.DayCheck(
                    sentences = number(m.groupValues[1]) ?: throw IllegalArgumentException("“${m.groupValues[1]}” isn't a number"),
                    words = number(m.groupValues[2]) ?: throw IllegalArgumentException("“${m.groupValues[2]}” isn't a number"),
                    cardLetters = m.groupValues[3].takeIf { it.isNotEmpty() }?.let { number(it) ?: throw IllegalArgumentException("“$it” isn't a number") },
                    neverQuestion = m.value.contains(" Never when it ends with a question mark"),
                    neverOpens = m.groupValues[4].takeIf { it.isNotBlank() }?.split(',')?.map { it.trim().removePrefix("or ").trim().lowercase() }?.filter { it.isNotEmpty() },
                ),
            )
        },
        rx("""Is there a block called ($CAP) already\?""") to { m -> Verb.BlockStands(m.groupValues[1]) },
        rx("""Do the words of ($CAP) say he may be in danger, by his safety words \(the block safety-words, else the app's own\)\?""") to { m -> Verb.Danger(m.groupValues[1]) },
        rx("""Lift (his words|his telling|the new block's name|the name he gives her) from THE MESSAGE: (.+?)\. Carry (?:it|them) as ($CAP)\.""") to { m ->
            Verb.Lift(m.groupValues[1], m.groupValues[2].split(Regex(""";\s*else\s+|;\s*""")).map { it.trim() }.filter { it.isNotEmpty() }, m.groupValues[3])
        },
        rx("""Find the block THE MESSAGE names to keep ($CAP) in: one of his own, never his passport, shell or history\. Carry it as ($CAP)\.""") to { m -> Verb.FindKeep(m.groupValues[2]) },
        rx("""Find the blocks THE MESSAGE names to keep ($CAP) in: his own, never his passport, shell or history; with one named, carry it as ($CAP); with none, carry his stash as ($CAP); with two or more, carry them as ($CAP)\.""") to { m ->
            Verb.FindKeep(m.groupValues[2].also { require(it == m.groupValues[3]) }, stash = true, many = m.groupValues[4])
        },
        rx("""Find the block THE MESSAGE asks about: .+? Carry it as ($CAP)\.""") to { m -> Verb.FindAsked(m.groupValues[1]) },
        rx("""Read ($CAP) at its aperture: whole when it is ([\d,]+) characters or fewer, else as a ring of its places by their opening lines(?:, each a place's first sentence, up to 200 characters)?\. Carry it as ($CAP)\.""") to { m ->
            Verb.Read(m.groupValues[1], m.groupValues[2].replace(",", "").toInt(), m.groupValues[3])
        },
        rx("""Find the place of ($CAP) his words point to: the one whose first ([\d,]+) characters share the most of his words of (\w+) letters or more, other than (.+?), the first if two share as many, and read it whole, up to ([\d,]+) characters\. Carry it as ($CAP)\.""") to { m ->
            Verb.Point(m.groupValues[1], m.groupValues[2].replace(",", "").toInt(), number(m.groupValues[3]) ?: 4, m.groupValues[4].split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet(),
                m.groupValues[5].replace(",", "").toInt(), m.groupValues[6])
        },
        rx("""Find the place of ($CAP) his words point to: the one whose first ([\d,]+) characters share the most of his words of (\w+) letters or more, other than (.+?), the first if two share as many, and read it whole when it is ([\d,]+) characters or fewer, else its first ([\d,]+) characters(?:, ending| and then) “…”\. Carry it as ($CAP)\.""") to { m ->
            Verb.Point(m.groupValues[1], m.groupValues[2].replace(",", "").toInt(), number(m.groupValues[3]) ?: 4, m.groupValues[4].split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet(),
                m.groupValues[6].replace(",", "").toInt().also { require(it == m.groupValues[5].replace(",", "").toInt()) }, m.groupValues[7], cutLonger = true)
        },
        rx("""She chooses (.+?), from the ring beneath; on E2B, or when her answer isn't whole, (the ring goes to him|nothing is chosen)\. Carry the choice as ($CAP)\.""") to { m ->
            Verb.ChooseRing(m.groupValues[1], m.groupValues[2] == "the ring goes to him", m.groupValues[3])
        },
        rx("""She chooses (.+?) among ($CAP), in the order of the ring beneath; on E2B, or when her answer isn't whole, (the ring goes to him|nothing is chosen)\. Carry the choice as ($CAP)\.""") to { m ->
            Verb.ChooseRing(m.groupValues[1], m.groupValues[3] == "the ring goes to him", m.groupValues[4], among = m.groupValues[2])
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
        rx("""Set out her name, ($CAP), at her place in his shell beneath her reply, for his yes(?:: the line “his first name calls me \1\.”(?: \(his first name: the first word of who he is, at (?:his shell's place 2|6\.2 of his shell)\))?, in place of the line there that says what he calls her, else beneath her place)?\.""") to { m -> Verb.SetName(m.groupValues[1]) },
        rx("""Set out his day beneath her reply, for his yes: ($CAP) as the day's line at today's date in his now, ($CAP), ($CAP), and each of ($CAP) beneath ($CAP) in his P6\.""") to { m ->
            Verb.SetDay(m.groupValues[1], m.groupValues[2], m.groupValues[3], m.groupValues[4], m.groupValues[5])
        },
        rx("""Set out his day beneath her reply, for his yes: ($CAP) as the day's line at today's date in his now \(the block now(?:, his clock)?\), ($CAP) on his hand \(the block ahead: and his handle\)(?:, those booked at their day in his now)?, ($CAP) at their levels of his now, and each of ($CAP) beneath ($CAP) in his P6 \(the block p6: and his handle\)\.""") to { m ->
            Verb.SetDay(m.groupValues[1], m.groupValues[2], m.groupValues[3], m.groupValues[4], m.groupValues[5])
        },
        // Sundial 0.10: the SetDay form without its P6 part.
        rx("""Set out his day beneath her reply, for his yes: ($CAP) as the day's line at today's date in his now \(the block now(?:, his clock)?\), ($CAP) on his hand \(the block ahead: and his handle\)(?:, those booked at their day in his now)?, and ($CAP) at their levels of his now\.""") to { m ->
            Verb.SetDay(m.groupValues[1], m.groupValues[2], m.groupValues[3])
        },
        // Sundial 0.10, the keep card: the grain taken as a word ("at today", "at its minute").
        rx("""Set out his day on the keep card, for his yes: ($CAP) as the day's line at (today|tomorrow) in his now, in Sundial's form, ($CAP) named last in plain words; each of ($CAP) at its (day|gathering|beat|minute|breath|second); ($CAP) on his hand \(the block ahead: and his handle\), those booked at their day in his now; and ($CAP) at their levels of his now\.""") to { m ->
            Verb.KeepDay(m.groupValues[1], m.groupValues[2], m.groupValues[3], m.groupValues[4], m.groupValues[5], m.groupValues[6], m.groupValues[7])
        },
        rx("""Carry ($CAP) as ($CAP)\.""") to { m -> Verb.Carry(m.groupValues[1], m.groupValues[2]) },
        rx("""Gather what he said today, each moment with its instant, from the first after midnight on the beach's clock\. Carry them as ($CAP)\.""") to { m -> Verb.Gather(m.groupValues[1]) },
        rx("""She names every place ($CAP) plainly touch(?:es)?, one or more, never ranked, from the ring beneath: (.+?); on E2B, or when her answer isn't whole, (nothing is named|the ring goes to him)\. Carry them as ($CAP)\.""") to { m ->
            Verb.NameMany(m.groupValues[1], m.groupValues[2], m.groupValues[3] == "the ring goes to him", m.groupValues[4])
        },
        rx("""Find the places of that ring ($CAP) leaves out\. Carry them as ($CAP)\.""") to { m -> Verb.Untouched(m.groupValues[1], m.groupValues[2]) },
        rx("""She gives her reply as ($CAP(?:, $CAP)*(?:,? and $CAP)?) through the tool call; on E2B, or when her answer isn't whole, her reply stands as she writes it\.""") to { m ->
            Verb.Parts(Regex(CAP).findAll(m.groupValues[1]).map { it.value }.toList())
        },
        // Sundial 0.10.1: two shapes, hers to choose (REFLECT or ANSWER), each its parts and when it is for.
        rx("""She gives her reply through the tool call in one of two shapes: ([A-Z]+), as ($CAP(?:(?:, |,? and )$CAP)*)(?:, [^;]+)?; or ([A-Z]+), as ($CAP(?:(?:, |,? and )$CAP)*)(?:, [^.]+)?\. On E2B, or when her answer isn't whole, her reply stands as she writes it\.""") to { m ->
            val one = HerReply.Shape(m.groupValues[1], Regex(CAP).findAll(m.groupValues[2]).map { it.value }.toList())
            val two = HerReply.Shape(m.groupValues[3], Regex(CAP).findAll(m.groupValues[4]).map { it.value }.toList())
            Verb.Parts(one.required, listOf(one, two))
        },
        rx("""She draws the day's sheet from ($CAP)( and today's line in his now)?, with this line: “(.+)” On E2B, or when her answer isn't whole, she writes it in plain lines, the title first and tomorrow last after “Tomorrow:”\. Carry it as ($CAP)\.""") to { m ->
            Verb.Sheet(m.groupValues[1], m.groupValues[2].isNotEmpty(), m.groupValues[3], m.groupValues[4])
        },
        rx("""Read his last day kept before today, whole, with its tomorrow line\. Carry it as ($CAP)\.""") to { m -> Verb.LastDay(m.groupValues[1]) },
        rx("""Read the tomorrow line of his last day kept before today\. Carry it as ($CAP)\.""") to { m -> Verb.LastTomorrow(m.groupValues[1]) },
        rx("""Read the latest (\w+) days resting in ($CAP), each whole with its date: .+?\. Carry them as ($CAP)\.""") to { m -> Verb.DaysIn(number(m.groupValues[1]) ?: 9, m.groupValues[2], m.groupValues[3]) },
        rx("""Read what he whispered at ($CAP), each with its date and the question it answered\. Carry them as ($CAP)\.""") to { m -> Verb.Whispers(m.groupValues[1], m.groupValues[2]) },
        rx("""Read ($CAP)'s three ways of listening from Sundial's block, and which was used there last, with the questions asked there before, each with its date\. Carry them as ($CAP)\.""") to { m -> Verb.Ways(m.groupValues[1], m.groupValues[2]) },
        rx("""Set out a whisper beneath her words, for his tap: his words, kept in his stash with ($CAP)'s phrase, today's date and ($CAP)\.""") to { m -> Verb.SetWhisper(m.groupValues[1], m.groupValues[2]) },
        rx("""Set out the card “(.+?)” beneath her reply, for his tap\.""") to { m -> Verb.SetCard(m.groupValues[1]) },
        rx("""On his yes, write it\.""") to { _ -> Verb.OnYes },
        rx("""On his tap, send it\.""") to { _ -> Verb.OnTapSend },
        rx("""On his tap, run cook (\d(?:\.\d)?)\.""") to { m -> Verb.OnTapRun(m.groupValues[1]) },
        rx("""Cut ($CAP) into its sentences\. Carry them as ($CAP)\.""") to { m -> Verb.Cut(m.groupValues[1], m.groupValues[2]) },
        // Sundial 0.10.3: the sentence says what CUT does first: each moment a piece at least, a line break always ending one.
        rx("""Cut ($CAP) into its pieces: each of his moments a piece of its own at least, and a line break always ending one; then each into its sentences, or, where it has fewer full stops than one in (\w+) words, as a spoken telling has, before each of the joins at cook (\d\.\d\.\d), a join matched wherever its words begin, so “and I” cuts before “and I also” and “and I've” too, keeping every word his and in order\. Carry them as ($CAP)\.""") to { m ->
            Verb.Cut(m.groupValues[1], m.groupValues[4], joins = m.groupValues[3], perStop = number(m.groupValues[2]) ?: 30)
        },
        rx("""Cut ($CAP) into its sentences; where it has fewer full stops than one in (\w+) words, as a spoken telling has, cut it instead before each of the joins at cook (\d\.\d\.\d)(?:, a join matched as whole words wherever it stands \(“and I” before “and I've” too\)|, a join matched wherever its words begin, so “and I” cuts before “and I also” and “and I've” too)?, keeping every word his and in order\. Carry them as ($CAP)\.""") to { m ->
            Verb.Cut(m.groupValues[1], m.groupValues[4], joins = m.groupValues[3], perStop = number(m.groupValues[2]) ?: 30)
        },
        rx("""Find among ($CAP) what he means to do, by the words at cook (\d\.\d)[:;,] (.+) Carry them as ($CAP)\.""") to { m ->
            val body = m.groupValues[3]
            val appointments = Regex("""every piece naming (.+?) of his with its day""").find(body)?.groupValues?.get(1)?.let { list ->
                list.split(Regex(""",\s*(?:or\s+)?|\s+or\s+""")).map { it.trim().removePrefix("a ").removePrefix("an ").trim() }.filter { it.isNotEmpty() }
            }.orEmpty()
            Verb.Intents(m.groupValues[1], m.groupValues[2], m.groupValues[4], carriesOn = "a piece cut at a join, with no I of its own, carries on the piece before it" in body, appointments = appointments,
                naming = body.startsWith("when what follows them names something to do"))
        },
        rx("""Find among ($CAP) those that name this week, this month, this season or this year\. Carry them as ($CAP), each at its level of his now\.""") to { m -> Verb.Rungs(m.groupValues[1], m.groupValues[2]) },
        rx("""Run cook (\d(?:\.\d)?)\.""") to { m -> Verb.Run(m.groupValues[1]) },
        rx("""Hand ($CAP) to the writing check\.""") to { m -> Verb.WritingCheck(m.groupValues[1]) },
        rx("""Append one entry to his history: .+""") to { _ -> Verb.Close },
        rx("""Stop\.""") to { _ -> Verb.Stop },
        rx("""walk on\.""") to { _ -> Verb.WalkOn },
    )

    private val NUMBERS = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
    private val TENS = listOf("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

    /** A number as a step writes it: in figures, or in words to ninety-nine ("three", "forty", "forty-five"); "a" or "an" is one. */
    fun number(s: String): Int? {
        val w = s.trim().lowercase()
        w.toIntOrNull()?.let { return it }
        if (w == "a" || w == "an") return 1
        NUMBERS.indexOf(w).takeIf { it >= 0 }?.let { return it + 1 }
        val parts = w.split(Regex("""[\s-]+"""))
        val tens = TENS.indexOf(parts[0]).takeIf { it >= 0 }?.let { (it + 2) * 10 } ?: return null
        return when (parts.size) {
            1 -> tens
            2 -> NUMBERS.indexOf(parts[1]).takeIf { it in 0..8 }?.let { tens + it + 1 }
            else -> null
        }
    }

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
        // Sundial 0.10.3: a sentence missing its last full stop (or question mark) is read as if it had it.
        for (tried in listOf(rest) + (if (rest.endsWith(".") || rest.endsWith("?")) emptyList() else listOf("$rest.", "$rest?")))
            for ((form, verb) in FORMS) form.matchEntire(tried)?.let { m -> runCatching { verb(m) }.getOrNull()?.let { return name to it } }
        return null
    }

    /**
     * Sundial 0.10.3: why a step couldn't be read, in plain words, beside the
     * shipped step at the same place ([shipped]): a number that isn't one, a
     * quote mark that never closes, or the first words outside the quotes that
     * aren't the app's form. His words inside the quotes are his, never the
     * reason.
     */
    fun why(text: String, shipped: String?): String {
        val t = text.trim()
        val label = LABEL.find(t)
        val rest = label?.let { t.substring(it.range.last + 1).trim() } ?: t
        for ((form, verb) in FORMS) form.matchEntire(rest)?.let { m -> runCatching { verb(m) }.exceptionOrNull()?.let { return it.message ?: "a part of it couldn't be read" } }
        val opens = t.count { it == '“' }
        val closes = t.count { it == '”' }
        if (opens > closes) return "its quoted line opens with a quote mark (“) that never closes (”)"
        if (shipped == null) return "the sentence “${clip(t, 80)}” is none of the app's forms"
        val mine = outside(t)
        val app = outside(shipped)
        val i = (0 until minOf(mine.size, app.size)).firstOrNull { k -> Telling.plain(mine[k]) != Telling.plain(app[k]) } ?: minOf(mine.size, app.size)
        if (i >= mine.size && i >= app.size) return "the sentence “${clip(t, 80)}” is none of the app's forms"
        fun said(xs: List<String>) = xs.drop(i).take(6).joinToString(" ").ifEmpty { "nothing more" }
        return "outside its quotes it reads “${said(mine)}” where the app's form reads “${said(app)}”; the words inside its quotes are yours to change"
    }

    /** The words of [text] outside every quoted span (“…”, or straight quotes), in order. */
    private fun outside(text: String): List<String> =
        text.replace(Regex("""“[^”]*”|"[^"]*""""), " ").split(Regex("""\s+""")).filter { it.isNotBlank() }

    private fun digits(o: JsObj): List<String> = (1..9).map { "$it" }.filter { o.has(it) }

    private fun step(node: Js, at: String, refused: MutableList<String>, ringOf: Boolean = false, unreadable: MutableList<Pair<String, String>>? = null): Step? {
        val text = when (node) {
            is JsStr -> node.value
            is JsObj -> (node["_"] as? JsStr)?.value ?: collectUnderscore(node).orEmpty()
            else -> ""
        }
        val read = read(text)
        if (read == null) {
            refused += "$at: “${text.take(120)}”"
            unreadable?.add(at to text)
            return null
        }
        val (label, verb) = read
        val o = node as? JsObj
        val beneath = o?.let(::digits).orEmpty()
        // A she-step's children are the ring offered her, as written; any other step's are its answers or its steps.
        if (verb is Verb.ChooseRing || verb is Verb.ChooseEach || verb is Verb.NameMany) {
            val ring = beneath.map { k -> (o!![k] as? JsStr)?.value ?: collectUnderscore(o[k]!!).orEmpty() }
            return Step(at, text, label, verb, emptyList(), ring)
        }
        val kids = beneath.mapNotNull { k -> step(o!![k]!!, "$at.$k", refused, unreadable = unreadable) }
        return Step(at, text, label, verb, kids, emptyList())
    }

    /** A top-level place whose places are procedures says so in its opening (cook 6, Sundial's ways). */
    private val SHELF = Regex("""each a procedure beneath""")

    /** His cook read: each procedure's steps, cook 9's words, and every step it couldn't read. */
    fun parse(block: Js?): Parsed {
        val refused = mutableListOf<String>()
        val unreadable = mutableListOf<Pair<String, String>>()
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
            // Sundial 0.10 (1.111178719912): a place whose opening says its places are procedures holds them, each at two digits.
            if (SHELF.containsMatchIn(opening)) {
                for (c in digits(node)) {
                    val inner = node[c] as? JsObj
                    val innerOpening = (inner?.get("_") as? JsStr)?.value
                    if (inner == null || innerOpening == null) { refused += "$k.$c: a place of procedures holds a line that isn't one"; continue }
                    val innerName = innerOpening.substringBefore(" —").substringBefore(" -").trim()
                    procedures["$k.$c"] = Procedure("$k.$c", innerName, innerOpening, digits(inner).mapNotNull { s -> step(inner[s]!!, "$k.$c.$s", refused, unreadable = unreadable) })
                }
                continue
            }
            val name = opening.substringBefore(" —").substringBefore(" -").trim()
            val steps = digits(node).mapNotNull { s -> step(node[s]!!, "$k.$s", refused, unreadable = unreadable) }
            procedures[k] = Procedure(k, name, opening, steps)
        }
        if (cues.answers.isEmpty()) refused += "cook 9: the words the app listens for are missing"
        return Parsed(procedures, cues, refused, unreadable)
    }

    private fun cues(node: JsObj): Cues {
        val answers = LinkedHashMap<String, Cue>()
        var asking = emptyList<String>()
        var intent = emptyList<String>()
        var joins = emptyList<String>()
        fun list(s: String) = s.split(',').map { it.trim().lowercase().replace('’', '\'') }.filter { it.isNotEmpty() }
        for (k in digits(node)) {
            val n = node[k]!!
            val line = (n as? JsStr)?.value ?: (n as? JsObj)?.let { (it["_"] as? JsStr)?.value } ?: collectUnderscore(n) ?: continue
            // 0.9.1: a line of its own beneath one of cook 9's, as the joins beneath TELL MY DAY's words (9.4.1).
            (n as? JsObj)?.let { o ->
                for (j in digits(o)) {
                    val inner = (o[j] as? JsStr)?.value ?: continue
                    if (inner.trim().startsWith("THE JOINS")) joins = list(inner.substringAfter(':', ""))
                }
            }
            val label = line.substringBefore(':').trim().substringBefore(',').trim()
            val body = line.substringAfter(':', "").trim()
            when {
                label.startsWith("THE JOINS") -> joins = list(body)
                label.startsWith("THE ASKING") -> asking = list(body)
                label.startsWith("WHAT HE MEANS TO DO") -> intent = list(body)
                else -> {
                    val starts = list(body.substringBefore('—', body.takeIf { '—' !in it } ?: ""))
                    val phrases = if ('—' in body) list(body.substringAfter('—')) else emptyList()
                    answers[label.uppercase()] = Cue(label.uppercase(), starts, phrases)
                }
            }
        }
        return Cues(answers, asking, intent, joins)
    }

    /**
     * The kernel read, by label: which procedure his message runs, the close,
     * and Sundial's three events (1.111178719912): the dial opened with
     * nothing said today, his tap of Keep today, a shell lifted to his ear.
     */
    class Kernel(val message: String?, val close: String?, val greet: String? = null, val keep: String? = null, val listen: String? = null) {
        /** Each event this kernel names, by its label. */
        val events: Map<String, String?> get() = mapOf(MESSAGE to message, CLOSE to close, GREET to greet, KEEP to keep, LISTEN to listen)
    }

    const val MESSAGE = "HIS MESSAGE"
    const val CLOSE = "THE CLOSE OF A CONVERSATION"
    const val GREET = "THE DIAL OPENED"
    const val KEEP = "KEEP TODAY"
    const val LISTEN = "A SHELL LIFTED"

    /** What each of Sundial's events is called when the app says it ran the shipped one. */
    val EVENT_WORDS = mapOf(GREET to "the dial opening", KEEP to "Keep today", LISTEN to "a shell lifted")

    fun kernel(block: Js?): Kernel {
        val o = block as? JsObj ?: return Kernel(null, null)
        fun run(label: String): String? = digits(o).mapNotNull { k -> (o[k] as? JsStr)?.value ?: collectUnderscore(o[k]!!) }
            .firstOrNull { it.trim().startsWith(label) }?.let { Regex("""Run cook (\d(?:\.\d)?)\.""").find(it)?.groupValues?.get(1) }
        return Kernel(run(MESSAGE), run(CLOSE), run(GREET), run(KEEP), run(LISTEN))
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
        /** Sundial 0.10: the beach's day this walk keeps (today's; yesterday's when the card offers that day first). */
        val keepDay: java.time.LocalDate get() = Keep.dayOf(now.toInstant().toEpochMilli())
        /** His moments of [keepDay], each with its instant, kept and not yet kept (1.111178719915). */
        fun moments(): List<Keep.Moment> = emptyList()
        /** His days kept, up to today, read from his now and his P6. */
        fun kept(): List<Keep.KeptDay> = runCatching {
            Keep.days(storage.read(Days.NOW)?.let(::parseJson), storage.read(Days.p6Of(handle))?.let(::parseJson), keepDay)
        }.getOrDefault(emptyList())
        /** Sundial 0.10.1: whether [words] say he may be in danger, by his safety words (Talking.unsafe), the one check the app makes. */
        fun inDanger(words: String): Boolean = runCatching { Talking.unsafe(words, pages) }.getOrElse { Talking.unsafe(words, Talking.SAFETY_WORDS) }
        /** What he whispered at a place (pool order), each with its date and the question it answered: Sundial 0.11. */
        fun whispers(pool: Int): String? = null
        /** A place's three ways of listening, which was used last, and the questions asked there: Sundial 0.11. */
        fun ways(pool: Int): String? = null
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
        /** Sundial: a whisper at a place, for his tap: his [words], the place's phrase, today's date and the question it answers. */
        data class Whisper(val pool: Int, val question: String, val words: String = "") : Card()
    }

    /** What the walker waits for. */
    sealed class Wait {
        /** Her choice from each ring: a digit, or null when her answer isn't whole. */
        data class Her(val question: String, val rings: List<Ring>) : Wait()
        data class Him(val card: Card) : Wait()
        /** Sundial: her placing, several places of one ring ([places], as she reads them, pool numbers in [pools]). */
        /**
         * Sundial 0.10.3: when what she places is his moments, [moments] are
         * they, each with the places her reply already gave it; she is asked
         * only for those without ([unplaced]), and answers with [Walk.herEach].
         */
        data class HerMany(val question: String, val about: String, val places: List<String>, val pools: List<Int>, val moments: List<Keep.Moment> = emptyList()) : Wait() {
            val unplaced: List<Keep.Moment> get() = moments.filter { it.places.isEmpty() }
        }
        /** Sundial: her sheet for the day, drawn from [material] as [line] says. */
        data class HerSheet(val line: String, val material: String) : Wait()
    }

    /** His moments, oldest first: as words, his words alone, a moment a line; as she is handed them, each with its time on the phone's clock. */
    class Gathered(val moments: List<Keep.Moment>, val zone: java.time.ZoneId) {
        override fun toString(): String = moments.sortedBy { it.atMs }.joinToString("\n") { it.words.trim() }
        fun timed(): String = moments.sortedBy { it.atMs }.joinToString("\n") { m ->
            java.time.Instant.ofEpochMilli(m.atMs).atZone(zone).toLocalTime().let { "%02d:%02d".format(it.hour, it.minute) } + " " + m.words.trim()
        }
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
        /** Sundial: the named parts her reply comes in, on E4B through the tool call ("THE REFLECTION", "THE QUESTION"); empty for a free reply. */
        var parts: List<String> = emptyList()
            private set
        /** Sundial 0.10.1: the shapes her reply may take, hers to choose; empty when it has one. */
        var shapes: List<HerReply.Shape> = emptyList()
            private set
        private val stack = ArrayDeque<Frame>()
        /** The last placing ring's words by pool number, as the cook writes them ("what he felt"). */
        private val ringWords = LinkedHashMap<Int, String>()
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
            is Mark.Sheet -> Mark.write(v)
            is List<*> -> v.joinToString("; ") { (it as? Int)?.let { n -> ringWords[n] ?: Mark.POOLS.getOrNull(n - 1)?.phrase ?: "$n" } ?: it.toString() }
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
                is Verb.TellsDay -> Landing.tellsDay(str("THE MESSAGE"), world.cards(), runCatching { Landing.names(world.pages) }.getOrDefault(emptySet()), v.check)
                    .let { yesNo(s, it, if (it) "his words tell of his day" else "his words don't tell of his day") }
                // Sundial 0.10.1: the app's danger check, the same that shows the help line beneath her reply; she is never handed an "if" to judge.
                is Verb.Danger -> world.inDanger(str(v.of)).let { yesNo(s, it, if (it) "his words say he may be in danger, by his safety words" else "his words hold none of his safety words") }
                is Verb.BlockStands -> world.pages.exists(str(v.name)).let { yesNo(s, it, if (it) "a block called ${str(v.name)} stands" else "no block called ${str(v.name)} stands") }
                is Verb.Lift -> {
                    put(v.into, lift(v))
                    note(s, "lifted ${v.what}", v.into)
                }
                is Verb.FindKeep -> {
                    val said = str("THE MESSAGE")
                    val stash = Hatch.stashOf(world.handle)
                    val named = (listOfNotNull(Regex("""\bmy\s+stash\b""", RegexOption.IGNORE_CASE).find(said)?.let { it.range.first to stash }) +
                        Keeping.named(said, world.pages).filter { !Keeping.own(it, world.pages) && it !in HerWindow.HERS }
                            .map { n -> (Regex("""(?<![\p{L}\p{N}])${Regex.escape(n.lowercase())}(?![\p{L}\p{N}])""").find(said.lowercase())?.range?.first ?: 0) to n })
                        .sortedBy { it.first }.map { it.second }.distinct()
                    when {
                        v.many == null -> {
                            val found = named.firstOrNull { it != stash } ?: named.firstOrNull()
                            put(v.into, found)
                            note(s, if (found != null) "his words name where to keep it" else "his words name no block of his to keep it in", v.into)
                        }
                        named.size >= 2 -> {
                            put(v.into, null)
                            carried[v.many] = named
                            note(s, "his words name ${named.size} of his blocks to keep it in", v.many)
                        }
                        else -> {
                            val found = named.firstOrNull() ?: stash.takeIf { v.stash }
                            put(v.into, found)
                            note(s, if (named.isNotEmpty()) "his words name where to keep it" else "his words name no block of his, so his stash", v.into)
                        }
                    }
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
                    val cut = p != null && v.cutLonger && p.text.endsWith("…")
                    if (p != null) reads += "${p.block} ${p.key}, ${if (cut) "its first ${v.max} characters" else "whole"}"
                    note(s, if (p == null) "his words point to no one place" else "his words point to place ${p.key}" + if (cut) ", longer than ${v.max}: its first ${v.max} characters, ending “…”" else "", v.into)
                }
                is Verb.ChooseRing -> {
                    @Suppress("UNCHECKED_CAST") val among = v.among?.let { carried[it] as? List<String> }
                    val places = ringPlaces(s.ring, among)
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
                            is Gathered -> x.timed().takeIf { it.isNotBlank() }
                            is List<*> -> str(name).takeIf { it.isNotBlank() }
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
                    // 0.9's form, or its P6-less one: his telling as the day's line in his own words; no P6 (Sundial 0.10).
                    @Suppress("UNCHECKED_CAST") val cards = (carried[v.cards] as? List<Days.Intent>).orEmpty()
                    @Suppress("UNCHECKED_CAST") val rungs = (carried[v.rungs] as? List<Days.Rung>).orEmpty()
                    val day = world.keepDay
                    val telling = str(v.telling).trim()
                    val sheet = telling.takeIf { it.isNotEmpty() }?.let { Mark.Sheet(it, emptyList()) }
                    val landing = Days.landing(world.handle, day, sheet, emptyList(), emptyList(), cards, rungs, world.now, his = hisLine(day), yesterday = day != todayOf())
                    setOut(s, Card.Day(landing), if (v.pieces != null) "set out his day, for his yes (its P6 part is read, and nothing is written to his P6)" else "set out his day, for his yes")
                }
                is Verb.KeepDay -> {
                    @Suppress("UNCHECKED_CAST") val cards = (carried[v.cards] as? List<Days.Intent>).orEmpty()
                    @Suppress("UNCHECKED_CAST") val rungs = (carried[v.rungs] as? List<Days.Rung>).orEmpty()
                    @Suppress("UNCHECKED_CAST") val places = (carried[v.places] as? List<Int>).orEmpty()
                    val moments = (carried[v.moments] as? Gathered)?.moments.orEmpty()
                    val day = world.keepDay
                    val sheet = carried[v.sheet] as? Mark.Sheet
                    val grain = Keep.grainOf(v.grain) ?: Keep.Grain.MINUTE
                    val landing = Days.landing(world.handle, day, sheet, places, moments, cards, rungs, world.now, his = hisLine(day), grain = grain, yesterday = day != todayOf(),
                        onHand = runCatching { world.cards().values }.getOrDefault(emptyList()))
                    setOut(s, Card.Day(landing), "set out the keep card, for his yes")
                }
                is Verb.Carry -> {
                    carried[v.from]?.let { carried[v.into] = it } ?: carried.remove(v.into)
                    note(s, "carried ${v.from} as ${v.into}", v.into)
                }
                is Verb.Gather -> {
                    val ms = world.moments()
                    if (ms.isEmpty()) carried.remove(v.into) else carried[v.into] = Gathered(ms, world.now.zone)
                    note(s, "gathered ${ms.size} moment${if (ms.size == 1) "" else "s"} of ${world.keepDay}", v.into)
                }
                is Verb.NameMany -> {
                    val pools = s.ring.mapIndexed { i, _ -> i + 1 }
                    ringWords.clear()
                    s.ring.forEachIndexed { i, w -> ringWords[i + 1] = w.trim() }
                    val gathered = carried[v.of] as? Gathered
                    if (s.ring.isEmpty() || !has(v.of)) {
                        carried.remove(v.into)
                        note(s, "nothing to place")
                    } else if (gathered != null && (!world.big || gathered.moments.all { it.places.isNotEmpty() })) {
                        // Sundial 0.10.3: his moments, each where her reply placed it; none left for her, or on E2B none asked.
                        placeMoments(s, v, gathered, emptyMap())
                    } else if (!world.big) {
                        carried.remove(v.into)
                        note(s, "nothing is named (E2B)")
                    } else {
                        pendingHer = s
                        waiting = Wait.HerMany(v.how, str(v.of), s.ring, pools, gathered?.moments.orEmpty())
                    }
                }
                is Verb.Untouched -> {
                    @Suppress("UNCHECKED_CAST") val touched = (carried[v.touched] as? List<Int>).orEmpty()
                    if (touched.isEmpty() || ringWords.isEmpty()) carried.remove(v.into)
                    else carried[v.into] = ringWords.filterKeys { it !in touched }.values.joinToString("; ").takeIf { it.isNotBlank() } ?: ""
                    if (carried[v.into] == "") carried.remove(v.into)
                    note(s, if (has(v.into)) "the places untouched" else "no places untouched to hand", v.into)
                }
                is Verb.Parts -> {
                    parts = v.names
                    shapes = v.shapes
                    note(s, (if (v.shapes.isNotEmpty()) "her reply in one of ${v.shapes.size} shapes, hers to choose (${v.shapes.joinToString(" or ") { it.name }})"
                        else "her reply in ${v.names.size} part${if (v.names.size == 1) "" else "s"}") + if (world.big) ", through the tool call" else ", as she writes it (E2B)")
                }
                is Verb.Sheet -> {
                    if (!has(v.from)) {
                        carried.remove(v.into)
                        note(s, "nothing to draw a sheet from")
                    } else {
                        val today = if (v.withToday) world.storage.read(Days.NOW)?.let { runCatching { parseJson(it) }.getOrNull() }?.let { Keep.dayText(it, world.keepDay) } else null
                        val material = buildString {
                            append((carried[v.from] as? Gathered)?.timed() ?: str(v.from))
                            today?.let { append("\n(Today's line in his now:)\n$it") }
                        }
                        pendingHer = s
                        waiting = Wait.HerSheet(fill(v.line), material)
                    }
                }
                is Verb.LastDay -> {
                    val d = Keep.lastDay(world.kept(), world.keepDay)
                    val text = d?.let { k -> listOfNotNull(k.date?.let { voiceAddress(Days.dayAddress(it)) }, k.text, k.moments.takeIf { k.text == null && it.isNotEmpty() }?.joinToString("\n") { it.words }).joinToString("\n") }
                    if (text.isNullOrBlank()) carried.remove(v.into) else carried[v.into] = text
                    note(s, if (d == null) "no day kept before today" else "read his last day kept, ${d.address}", v.into)
                }
                is Verb.LastTomorrow -> {
                    val t = Keep.tomorrowOf(Keep.lastDay(world.kept(), world.keepDay))?.trim()?.trimEnd('.')
                    if (t.isNullOrBlank()) carried.remove(v.into) else carried[v.into] = "Yesterday he wrote this line for today: \u201c$t.\u201d"
                    note(s, if (t.isNullOrBlank()) "no tomorrow line left" else "read the tomorrow line he left", v.into)
                }
                is Verb.DaysIn -> {
                    val pool = Mark.poolByPhrase(str(v.place))
                    val got = if (pool == 0) emptyList() else Keep.restingIn(world.kept(), pool, v.n)
                    if (got.isEmpty()) carried.remove(v.into)
                    else carried[v.into] = got.joinToString("\n\n") { (d, body) -> (d.date?.let { voiceAddress(Days.dayAddress(it)) } ?: d.address) + ":\n" + body }
                    note(s, "read ${got.size} day${if (got.size == 1) "" else "s"} resting there", v.into)
                }
                is Verb.Whispers -> {
                    val w = world.whispers(Mark.poolByPhrase(str(v.place)))
                    if (w.isNullOrBlank()) carried.remove(v.into) else carried[v.into] = w
                    note(s, if (w.isNullOrBlank()) "no whispers there" else "read his whispers there", v.into)
                }
                is Verb.Ways -> {
                    val w = world.ways(Mark.poolByPhrase(str(v.place)))
                    if (w.isNullOrBlank()) carried.remove(v.into) else carried[v.into] = w
                    note(s, if (w.isNullOrBlank()) "no ways of listening there yet" else "read the place's ways of listening", v.into)
                }
                is Verb.SetWhisper -> setOut(s, Card.Whisper(Mark.poolByPhrase(str(v.place)), str(v.question)), "set out a whisper, for his tap")
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
                    val joins = parsed.cues.joins
                    val names = runCatching { Landing.names(world.pages) }.getOrDefault(emptySet())
                    // Sundial 0.10.3: each of his moments a piece of its own at least, and a line break always ends one; then each
                    // into its sentences, or at its joins where it is a spoken telling. On his Pixel on 4 November his moments, joined
                    // by line breaks with no full stop between, were one piece: one card held them all, for the "I'm going to" in one.
                    val units = Cook.units(carried[v.from] as? Gathered, str(v.from))
                    var spoken = 0
                    val pieces = units.flatMap { u ->
                        if (v.joins != null && Cook.spoken(u, v.perStop)) { spoken++; Cook.cutAtJoins(u, joins) } else Telling.sentences(u, names)
                    }
                    carried[v.into] = pieces
                    note(s, "cut ${units.size} moment line${if (units.size == 1) "" else "s"} into ${pieces.size} piece${if (pieces.size == 1) "" else "s"}" +
                        (if (spoken > 0) ", $spoken a spoken telling cut at its joins" else ""), v.into)
                }
                is Verb.Intents -> {
                    @Suppress("UNCHECKED_CAST") val pieces = (carried[v.from] as? List<String>).orEmpty()
                    val intents = Days.intents(pieces, parsed.cues.intent, world.keepDay, Days.timeWords(world.storage),
                        joins = if (v.carriesOn) parsed.cues.joins else emptyList(), appointments = v.appointments, naming = v.naming)
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

        /** Sundial: her placing, the places she named (pool numbers), or null when her answer wasn't whole. */
        fun herMany(chosen: List<Int>?): Walk {
            val s = pendingHer ?: error("the walker isn't waiting for her")
            pendingHer = null
            waiting = null
            val v = s.verb as Verb.NameMany
            val named = chosen?.filter { it in 1..s.ring.size }?.distinct()?.let { Mark.inOrder(it) }
            if (named.isNullOrEmpty()) {
                carried.remove(v.into)
                note(s, "her answer wasn't whole: nothing is named")
            } else if (s.ring.size > 1 && named.size >= s.ring.size) {
                // Sundial 0.10.1: every place named tells nothing (his "I just want to talk" was placed in all six): counted as none.
                carried.remove(v.into)
                note(s, "she named all ${s.ring.size}: unreliable, counted as none")
            } else {
                carried[v.into] = named
                note(s, "she named ${named.size} place${if (named.size == 1) "" else "s"}", v.into)
            }
            return go()
        }

        /**
         * Sundial 0.10.3: her places for each of his moments not yet placed
         * ([placed], by instant; null when her answer wasn't whole). Every
         * moment keeps the places her reply gave it; a moment she named all
         * six for counts as none; the day's places are all of theirs.
         */
        fun herEach(placed: Map<Long, List<Int>>?): Walk {
            val s = pendingHer ?: error("the walker isn't waiting for her")
            pendingHer = null
            waiting = null
            val v = s.verb as Verb.NameMany
            val gathered = carried[v.of] as? Gathered ?: return herMany(placed?.values?.flatten())
            placeMoments(s, v, gathered, placed.orEmpty())
            return go()
        }

        private fun placeMoments(s: Step, v: Verb.NameMany, gathered: Gathered, placed: Map<Long, List<Int>>) {
            var six = 0
            val moments = gathered.moments.map { m ->
                if (m.places.isNotEmpty()) m
                else {
                    val named = placed[m.atMs]?.filter { it in 1..s.ring.size }?.distinct().orEmpty()
                    if (s.ring.size > 1 && named.size >= s.ring.size) { six++; m } else m.copy(places = Mark.inOrder(named))
                }
            }
            val now = Gathered(moments, gathered.zone)
            // The same moments wherever they are carried (THE MOMENTS, THE WORDS).
            for (k in carried.keys.toList()) if (carried[k] === gathered) carried[k] = now
            val union = Mark.inOrder(moments.flatMap { it.places })
            val placedHere = moments.count { it.places.isNotEmpty() }
            if (union.isEmpty()) carried.remove(v.into) else carried[v.into] = union
            note(s, "placed $placedHere of ${moments.size} moment${if (moments.size == 1) "" else "s"}" +
                (if (six > 0) "; $six named all ${s.ring.size}, counted as none" else "") +
                (if (union.isEmpty()) "" else ": ${Mark.placesWords(union)}"), v.into)
        }

        /** Sundial: her sheet for the day, or null when she gave none. */
        fun sheet(sh: Mark.Sheet?): Walk {
            val s = pendingHer ?: error("the walker isn't waiting for her")
            pendingHer = null
            waiting = null
            val v = s.verb as Verb.Sheet
            if (sh == null || sh.title.isBlank()) {
                carried.remove(v.into)
                note(s, "her sheet wasn't whole")
            } else {
                carried[v.into] = sh
                note(s, "she drew the sheet", v.into)
            }
            return go()
        }

        /** The beach's day today, by the world's clock. */
        private fun todayOf(): java.time.LocalDate = Keep.dayOf(world.now.toInstant().toEpochMilli())

        /** A line of his own already at [day] in his now, which a keep leaves as it is. */
        private fun hisLine(day: java.time.LocalDate): String? =
            world.storage.read(Days.NOW)?.let { runCatching { parseJson(it) }.getOrNull() }?.let { Keep.hisOwnLine(it, day) }

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
            is Card.Whisper -> "a whisper kept at ${Mark.POOLS.getOrNull(c.pool - 1)?.phrase ?: "a place"}"
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
            // 0.9.1: and never the blocks it is to go in ("Keep that in my book or my films").
            val places = runCatching { world.pages.shore().mine.map { it.name.lowercase() } }.getOrDefault(emptyList()).toSet() + setOf("stash", "my", "or", "and", "into", "to", "in", "with")
            val rest = if (verb != null) s.substring(at.first + verb.length).replace(Regex("""^\s*(?:in mind\s+)?(?:that|to)?\b""", RegexOption.IGNORE_CASE), "").trim()
                .takeIf { r -> words(r).count { it !in OBJECT && it !in places } >= 1 && words(r).size >= 2 } else null
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
        private fun ringPlaces(ring: List<String>, among: List<String>? = null): List<String> {
            val his = runCatching { hisBlocks() }.getOrDefault(emptyList()).let { h -> if (among == null) h else h + among.filter { it !in h } }
            val out = mutableListOf<String>()
            for (line in ring) {
                val l = line.trim().lowercase()
                when {
                    l.startsWith("his stash") -> out += Hatch.stashOf(world.handle)
                    l.startsWith("his other blocks") -> out += his.filter { it !in out }
                    else -> line.trim().trimEnd('.').let { n -> if (world.pages.exists(n) && !Keeping.own(n, world.pages)) out += n }
                }
            }
            return out.distinct().filter { among == null || it in among }.filter { world.storage.read(it) != null || it == Hatch.stashOf(world.handle) }.take(9)
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

    /** 0.9.1: a spoken telling, as dictation gives it: fewer full stops than one in [perStop] words, its last aside. */
    /**
     * Sundial 0.10.3: what CUT cuts, before it cuts at sentences or joins:
     * each of his moments ([gathered]), each line of each; else [telling], a
     * line at a time. A line break always ends a piece.
     */
    fun units(gathered: Gathered?, telling: String): List<String> =
        (gathered?.moments?.sortedBy { it.atMs }?.map { it.words } ?: listOf(telling))
            .flatMap { it.split(Regex("""\r?\n""")) }.map { it.trim() }.filter { it.isNotEmpty() }

    fun spoken(telling: String, perStop: Int): Boolean {
        val t = telling.trim().trimEnd('.', '!', '?', ' ')
        val words = t.split(Regex("""\s+""")).count { it.isNotBlank() }
        val stops = Regex("""[.!?](?=\s|$)""").findAll(t).count()
        return words >= perStop && stops * perStop < words
    }

    /** 0.9.1: a spoken telling cut before each join he speaks with (cook 9.4.1), every word his and in order. */
    fun cutAtJoins(telling: String, joins: List<String>): List<String> {
        val t = telling.trim()
        if (joins.isEmpty()) return listOf(t).filter { it.isNotEmpty() }
        fun pattern(join: String) = join.trim().split(Regex("""\s+""")).joinToString("""\s+""") { w ->
            w.map { ch -> when { ch == '\'' || ch == '’' -> "['’]"; ch.isLetterOrDigit() -> ch.toString(); else -> "\\$ch" } }.joinToString("")
        }
        val rx = Regex("""(?<![\p{L}\p{N}'’])(?:${joins.sortedByDescending { it.length }.joinToString("|", transform = ::pattern)})(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE)
        val cuts = rx.findAll(t).map { it.range.first }.filter { it > 0 }.toList()
        val pieces = mutableListOf<String>()
        var from = 0
        for (c in cuts) {
            pieces += t.substring(from, c).trim()
            from = c
        }
        pieces += t.substring(from).trim()
        return pieces.filter { it.isNotEmpty() }
    }

    private fun words(text: String): List<String> = Regex("""[\p{L}\p{N}']+""").findAll(norm(text)).map { it.value.trim('\'') }.filter { it.isNotEmpty() }.toList()
    private fun clean(words: String): String = words.trim().trim(',', ';', ':', '-', '—').trim()
    private fun clip(s: String, n: Int) = if (s.length <= n) s else s.take(n - 1).trimEnd() + "…"
    private fun firstSentence(t: String) = Regex("""^.{1,200}?[.!?](?=\s|$)""").find(t)?.value ?: clip(t, 200)
    fun shortly(s: String) = s.replace("\n", " / ").let { if (it.length <= 90) it else it.take(89).trimEnd() + "…" }

    // ── which copy runs ──

    /** The cook that runs: his own, as it stands, when it reads; else the shipped copy, and why. */
    class Loaded(val parsed: Parsed, val kernel: Kernel, val own: Boolean, val refusal: String?, val fellBack: Map<String, String> = emptyMap(), val shipped: Parsed? = null) {
        /**
         * What one of Sundial's events walks: his cook at the procedure his
         * kernel names; with none named, the shipped kernel's, in his cook
         * where it stands there, else in the shipped cook. Null when neither has it.
         */
        fun walkFor(label: String): Pair<Parsed, String>? {
            val place = kernel.events[label] ?: return null
            if (place in parsed.procedures) return parsed to place
            return shipped?.takeIf { place in it.procedures }?.let { it to place }
        }
    }

    /**
     * Sundial 0.10.3: why his cook couldn't be read, in plain words: the place,
     * its name, and which part of it isn't the app's form. On his Pixel on 4
     * November it said only "Your cook couldn't be read at 6.1.5 …".
     */
    fun refusal(parsed: Parsed, shippedCook: Js?): String {
        val (at, text) = parsed.unreadable.firstOrNull() ?: return "Your cook couldn't be read at ${parsed.refused.first()}, so the shipped copy ran."
        val shipped = places(shippedCook)[at]
        val label = LABEL.find(text.trim())?.groupValues?.get(1)?.let { " ($it)" }.orEmpty()
        return "Your cook couldn't be read at $at$label: ${why(text, shipped)}. So the shipped copy ran."
    }

    fun load(handle: String?, storage: BlockStorage, procedures: Shelf?): Loaded {
        val shipped = parse(procedures?.load(SHIPPED_COOK))
        val shippedKernel = kernel(procedures?.load(SHIPPED_KERNEL))
        if (handle == null) return Loaded(shipped, shippedKernel, false, null)
        val mine = storage.read(cookOf(handle))?.let { runCatching { parseJson(it) }.getOrNull() } ?: return Loaded(shipped, shippedKernel, false, null)
        val parsed = parse(mine)
        val kernel = storage.read(kernelOf(handle))?.let { runCatching { parseJson(it) }.getOrNull() }?.let(::kernel)?.takeIf { it.message != null } ?: shippedKernel
        val missing = (listOf(kernel.message, kernel.close).filterNotNull() - parsed.procedures.keys)
        return when {
            !parsed.ok -> Loaded(shipped, shippedKernel, false, refusal(parsed, procedures?.load(SHIPPED_COOK)))
            missing.isNotEmpty() -> Loaded(shipped, shippedKernel, false, "Your kernel names cook ${missing.first()}, which doesn't stand, so the shipped copy ran.")
            else -> {
                // Sundial's events: one his kernel names none of (or one naming a procedure his cook lacks) runs the shipped one, and says so.
                val fell = LinkedHashMap<String, String>()
                fun event(label: String, his: String?, shipped: String?): String? =
                    if (his != null && his in parsed.procedures) his
                    else shipped?.also { fell[label] = "Your kernel names nothing that stands for ${EVENT_WORDS[label]}, so the shipped one ran." }
                val greet = event(GREET, kernel.greet, shippedKernel.greet)
                val keep = event(KEEP, kernel.keep, shippedKernel.keep)
                val listen = event(LISTEN, kernel.listen, shippedKernel.listen)
                Loaded(parsed, Kernel(kernel.message, kernel.close, greet, keep, listen), true, null, fell, shipped)
            }
        }
    }

    // ── bringing them into his pocket ──

    /**
     * 0.9.1 (spine:pocket-beach 1.1111787162): one of his two blocks brought
     * up to this version, place by place. [took]: places he hadn't changed,
     * now the new shipped sentence; [added] and [removed]: places new in the
     * shipped copy, or gone from it; [yours]: places he changed, kept as he
     * has them, each with the new shipped sentence where that changed too.
     */
    class Up(val shipped: String, val block: String, val whole: Boolean, val merged: JsObj?, val took: List<String>, val added: List<String>, val removed: List<String>, val yours: List<Yours>) {
        /** Sundial 0.10: his old cook 6, retired whole, and what of it he had changed; null when cook 6 was already Sundial's. */
        var sundial: ToSundial? = null
        /** Brought in whole, or brought up: anything to write. */
        val changed: Boolean get() = whole || merged != null
        /** The trace of it, a line a place. */
        fun trace(): List<String> = took.map { "$block $it: the new sentence" } + added.map { "$block $it: added" } + removed.map { "$block $it: gone from this version" } +
            yours.map { y -> "$block ${y.at}: yours, kept" + if (y.shipped != null) " (the new one offered beside it)" else "" }
    }

    /** A place he changed: his sentence, and the new shipped one where it changed too (null when it didn't, or was taken out). */
    data class Yours(val block: String, val at: String, val his: String?, val shipped: String?, val offer: Boolean)

    /** Every place of a block by its address ("" its opening, "2.5.2"): the text standing there. */
    fun places(n: Js?): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        fun walk(x: Js?, at: String) {
            when (x) {
                is JsStr -> out[at] = x.value
                is JsObj -> {
                    (x["_"] as? JsStr)?.let { out[at] = it.value }
                    for (k in x.keys().filter { it != "_" }.sortedWith(compareBy({ it.length }, { it }))) walk(x[k], if (at.isEmpty()) k else "$at.$k")
                }
                else -> {}
            }
        }
        walk(n, "")
        return out
    }

    /** A block from its places: a place with places beneath is an object, its own text at "_"; one with none is its text. */
    fun fromPlaces(places: Map<String, String>): JsObj {
        val kids = places.keys.filter { it.isNotEmpty() }.groupBy { it.substringBeforeLast('.', "") }
        fun node(at: String): Js {
            val beneath = kids[at].orEmpty().sortedWith(compareBy({ it.length }, { it }))
            if (beneath.isEmpty() && at.isNotEmpty()) return str(places[at] ?: "")
            val o = JsObj()
            places[at]?.let { o["_"] = str(it) }
            for (k in beneath) o[k.substringAfterLast('.')] = node(k)
            return o
        }
        return node("") as JsObj
    }

    /** [mine] brought up to [shipped], place by place, against [last], what was shipped before; null [mine], brought in whole. */
    fun up(name: String, block: String, mine: Js?, shipped: JsObj, last: Js?, offered: Set<String> = emptySet()): Up {
        if (mine == null) return Up(name, block, true, null, emptyList(), places(shipped).keys.toList(), emptyList(), emptyList())
        val his = places(mine)
        val new = places(shipped)
        val old = last?.let(::places)
        val at = (his.keys + new.keys + (old?.keys ?: emptySet())).distinct().sortedWith(compareBy<String>({ a -> a.split('.').size }).thenBy { it })
        val out = LinkedHashMap<String, String>()
        val took = mutableListOf<String>()
        val added = mutableListOf<String>()
        val removed = mutableListOf<String>()
        val yours = mutableListOf<Yours>()
        for (a in at) {
            val h = his[a]
            val n = new[a]
            // With no copy of what was shipped before, every place that differs is taken as his.
            val o = if (old == null) if (h == n) h else Any() else old[a]
            when {
                h == n -> h?.let { out[a] = it }
                h == o -> {
                    if (n != null) out[a] = n
                    when {
                        h == null -> added += a
                        n == null -> removed += a
                        else -> took += a
                    }
                }
                else -> {
                    h?.let { out[a] = it }
                    val offer = n != o && n != null
                    val key = "$block $a ${n.hashCode()}"
                    yours += Yours(block, a, h, n.takeIf { offer }, offer && key !in offered)
                }
            }
        }
        // A place kept as his beneath one taken out stays; its parents stand with it.
        for (a in out.keys.toList()) {
            var p = a.substringBeforeLast('.', "")
            while (p.isNotEmpty() || a.isNotEmpty()) {
                if (p !in out && p.isNotEmpty()) out[p] = new[p] ?: his[p] ?: ""
                if (p.isEmpty()) break
                p = p.substringBeforeLast('.', "")
            }
        }
        val merged = fromPlaces(out)
        return Up(name, block, false, merged.takeIf { !deepEqual(it, mine) }, took, added, removed, yours)
    }

    /**
     * Sundial 0.10.2: her recipe for talk, the line TALK hands her first, as
     * his cook gives it: read once as her one conversation opens and put in
     * her instruction, never handed again each turn. Null when his cook has
     * no TALK, or TALK's first step hands her no line of its own.
     */
    fun talkRecipe(parsed: Parsed): String? {
        fun find(steps: List<Step>): Step? {
            for (s in steps) {
                if (s.label?.trim()?.trimEnd('.')?.uppercase() == "TALK") return s
                find(s.kids)?.let { return it }
            }
            return null
        }
        val talk = parsed.procedures.values.firstNotNullOfOrNull { find(it.steps) } ?: return null
        return (talk.kids.firstOrNull()?.verb as? Verb.Hand)?.takeIf { it.with.isEmpty() }?.line
    }

    /** The block his old cook 6 is kept in once cook 6 becomes Sundial's ways: the hidden region holds it. */
    const val RETIRED_COOK_6 = "retired-cook-6"

    /** Sundial 0.10's one-time move: [mine] with cook 6 the shipped one and his 1.1.8.2 where it now goes ([movedTo]); his old cook 6; what he had changed. */
    class ToSundial(val mine: JsObj, val retired: JsObj?, val changed: List<String>, val talkMoved: Boolean, val movedTo: String? = null) 

    /**
     * His cook brought to Sundial's shape before it is merged place by place:
     * cook 6 was TELL MY DAY's steps and is now Sundial's procedures, so it
     * can't be merged by address. His old cook 6 is kept whole (as
     * [RETIRED_COOK_6]), each place of it he had changed is named, and the
     * shipped cook 6 stands in its place. His old 1.1.8.2, the day question,
     * moves to 1.1.8.7 where he had changed it, so it stays his there. Null
     * when his cook 6 is already Sundial's.
     */
    fun toSundial(mine: JsObj, shipped: JsObj, last: Js?): ToSundial? {
        val six = mine["6"] as? JsObj ?: return null
        val opening = (six["_"] as? JsStr)?.value ?: collectUnderscore(six) ?: return null
        if (!opening.startsWith("TELL MY DAY")) return null
        val copy = parseJson(stringify(mine)!!) as JsObj
        val lastSix = places((last as? JsObj)?.get("6"))
        val changed = places(six).filter { (a, t) -> lastSix[a] != t }.keys.map { if (it.isEmpty()) "6" else "6.$it" }
        copy["6"] = parseJson(stringify(shipped["6"])!!)
        // 1.1.8.2, his day question: where he changed it, his question goes to where TALK now asks it (1.1.8.7 in 0.10; 1.1.8.3 from 0.10.1, before her places), its steps the shipped ones.
        val talk = jsGet(jsGet(jsGet(copy, "1"), "1"), "8") as? JsObj
        val lastTalk = jsGet(jsGet(jsGet(last, "1"), "1"), "8")
        val shippedTalk = jsGet(jsGet(jsGet(shipped, "1"), "1"), "8") as? JsObj
        var moved: String? = null
        val asksDay = shippedTalk?.keys()?.firstOrNull { k -> k != "_" && (shippedTalk[k] as? JsObj)?.let { collectUnderscore(it) }?.startsWith("Does THE MESSAGE tell of his day") == true }
        if (talk != null && shippedTalk != null && asksDay != null && talk["2"] != null && !deepEqual(talk["2"], jsGet(lastTalk, "2"))) {
            val his = (talk["2"] as? JsObj)?.let { collectUnderscore(it) } ?: (talk["2"] as? JsStr)?.value
            val node = parseJson(stringify(shippedTalk[asksDay])!!) as JsObj
            if (his != null) node["_"] = str(his)
            talk[asksDay] = node
            talk["2"] = parseJson(stringify(shippedTalk["2"])!!)
            moved = "1.1.8.$asksDay"
        }
        // A step he changed that runs cook 6 (TELL MY DAY) runs its successor, 6.1 KEEP TODAY; a step he left as shipped takes the new sentence in the merge.
        val shippedBefore = places(last)
        val sixRun = Regex("""([Rr]un cook 6)\.(?!\d)""")
        fun onto61(n: Js?, at: String): Js? = when (n) {
            is JsStr -> if (shippedBefore[at] != n.value && sixRun.containsMatchIn(n.value)) str(n.value.replace(sixRun, "$1.1.")) else n
            is JsObj -> JsObj().also { o -> for (k in n.keys()) o[k] = onto61(n[k], if (k == "_") at else if (at.isEmpty()) k else "$at.$k") }
            else -> n
        }
        for (k in copy.keys()) if (k != "6" && k != "_") copy[k] = onto61(copy[k], k)
        val retired = parseJson(stringify(six)!!) as JsObj
        retired["_"] = str("His cook 6 as it stood before Sundial (TELL MY DAY), kept whole when cook 6 became Sundial's ways: " + opening)
        return ToSundial(copy, retired, changed, moved != null, moved)
    }

    /**
     * A place's address as a bsp spindle in his cook (floor 1): its first
     * digit, then the rest past the one decimal point, so "1.1.8.1" is
     * "1.181". (0.9.1's offers were all one deep, so "5.6" read either way.)
     */
    fun spindleOf(at: String): String = at.split('.').let { p -> if (p.size <= 1) at else p.first() + "." + p.drop(1).joinToString("") }

    /** The key a place's offer is recorded by, once shown. */
    fun offerKey(y: Yours): String = "${y.block} ${y.at} ${y.shipped.hashCode()}"

    /**
     * His cook and kernel brought into his pocket, or up to this version place
     * by place ([lastShipped]: what was shipped before, the app's own copy, else
     * the past copy on the shelf; [offered]: the places whose new sentence was
     * already shown him). Each with the plan that writes it, when anything is to do.
     */
    fun bringUp(handle: String, pages: Pages, storage: BlockStorage, procedures: Shelf, lastShipped: (String) -> String? = { null }, offered: Set<String> = emptySet()): List<Pair<Up, Plan?>> =
        listOf(SHIPPED_COOK to cookOf(handle), SHIPPED_KERNEL to kernelOf(handle)).mapNotNull { (name, mine) ->
            val shipped = procedures.load(name) as? JsObj ?: return@mapNotNull null
            var now = storage.read(mine)?.let { runCatching { parseJson(it) }.getOrNull() }
            val last = (lastShipped(name) ?: (procedures.load("$PAST$name") as? JsObj)?.let { stringify(it) })?.let { runCatching { parseJson(it) }.getOrNull() }
            // Sundial 0.10 (1.111178719912): cook 6 replaced whole, once, his old one retired beside it; his changed 1.1.8.2 moved to 1.1.8.7.
            val sundial = if (name == SHIPPED_COOK) (now as? JsObj)?.let { toSundial(it, shipped, last) } else null
            sundial?.let { now = it.mine }
            val u = up(name, mine, now, shipped, last, offered).also { it.sundial = sundial }
            val plan = when {
                u.whole -> pages.planWhole(mine, parseJson(stringify(shipped)!!) as JsObj)
                u.merged != null -> pages.planWhole(mine, u.merged)
                sundial != null -> pages.planWhole(mine, sundial.mine)
                else -> null
            }
            u to plan
        }

    /** The shelf's copy of what the version before shipped, against which an app with no copy of its own compares his blocks. */
    const val PAST = "past-"

    /** The plans that bring the shipped cook and kernel into his pocket, or up to this version place by place. Empty when nothing is to do. */
    fun bringIn(handle: String, pages: Pages, storage: BlockStorage, procedures: Shelf, lastShipped: (String) -> String? = { null }): List<Plan> =
        bringUp(handle, pages, storage, procedures, lastShipped).mapNotNull { it.second }
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

    /** A whisper as Sundial keeps it (whispers.js:113), never sealed: the place's phrase and the date, his words at 1, the question at 2. */
    fun whisper(card: Cook.Card.Whisper, nowMs: Long): JsObj {
        val phrase = Mark.POOLS.getOrNull(card.pool - 1)?.phrase ?: ""
        val date = java.time.Instant.ofEpochMilli(nowMs).atZone(java.time.ZoneOffset.UTC).toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.UK))
        return JsObj.of("_" to str("whisper · $phrase · $date"), "1" to str(card.words.trim()), "2" to str(card.question))
    }

    fun shellOpening(pages: Pages): String = pages.position(Region.POCKET, HelloWorld.SHELL)?.text ?: HelloWorld.SHELL_OPENING

    /** Carry [card] out: null when done, else why not. His day is written whole, block by block. */
    fun carryOut(card: Cook.Card, handle: String, pages: Pages, storage: BlockStorage, shelf: Shelf, nowMs: Long): String? {
        if (card is Cook.Card.Whisper) {
            if (card.words.isBlank()) return "there were no words to keep"
            val done = pages.carryOut(pages.planEntry(Hatch.stashOf(handle), whisper(card, nowMs)))
            val problem = if (done.ok) null else done.problem ?: "the whisper couldn't be kept"
            // Sundial 0.10.3: every write on his yes has its history entry, made as it is written.
            Close.recordYes(handle, "a whisper kept at ${Mark.POOLS.getOrNull(card.pool - 1)?.phrase ?: "a place"}",
                if (problem == null) listOf("his stash: “${Cook.shortly(card.words)}”") else emptyList(), listOfNotNull(problem), pages)
            return problem
        }
        if (card is Cook.Card.Day) return keepDay(card.landing, handle, pages, storage, nowMs).problem
        val p = planned(card, handle, pages, storage, shelf, nowMs)
        val proposal = p.proposal ?: return p.problem ?: "nothing to write"
        proposal.refusal?.let { return it }
        return Asks.carryOut(proposal, pages, storage, shellOpening(pages)).problem
    }

    /** What one Keep it came to: what was written, what was skipped as on his hand already, what couldn't go, and what was refused. */
    class Kept(val written: List<String>, val already: List<String>, val over: List<String>, val refused: List<String>, val recorded: String?) {
        /** Null when every block landed; else what was refused, joined. */
        val problem: String? get() = refused.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }

    /**
     * Sundial 0.10.3: his day written on his yes, block by block: each only
     * when it changes; a card on his hand already skipped and said so, never
     * a refusal; one block refused never stops the rest; and one entry in
     * his history saying what was written and what wasn't
     * (spine:pocket-beach 1.111178719927, 1.111178719929).
     */
    fun keepDay(
        l: Days.DayCard, handle: String, pages: Pages, storage: BlockStorage, nowMs: Long,
        /** How one block is written whole: null when it landed, else why not (the tests make one refuse). */
        writeBlock: (String, JsObj) -> String? = { name, block ->
            runCatching { pages.carryOut(pages.planWhole(name, block)) }.fold({ if (it.ok) null else it.problem ?: "it couldn't be written" }, { it.message ?: "it couldn't be written" })
        },
    ): Kept {
        val written = Days.blocks(l, { name -> storage.read(name)?.let { runCatching { parseJson(it) }.getOrNull() } }, nowMs)
        val refused = mutableListOf<String>()
        val landed = mutableListOf<String>()
        for ((name, block) in written.blocks) {
            val why = writeBlock(name, block)
            if (why == null) landed += name else refused += "$name: $why"
        }
        val day = l.day.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM", java.util.Locale.UK))
        val said = mutableListOf<String>()
        if (Days.NOW in landed) {
            if (written.dayLine == Keep.DayLine.WRITTEN) said += "his now: the day's line at $day" + (l.sheet?.title?.takeIf { it.isNotBlank() }?.let { ", “${Cook.shortly(it)}”" } ?: "")
            written.moments.count { !it.already }.takeIf { it > 0 }?.let { said += "his now: $it moment${if (it == 1) "" else "s"} at ${if (it == 1) "its" else "their"} minute" }
            for (line in l.lines.filter { it.ticked && !it.already }) when (val w = line.where) {
                is Days.Where.Booking -> if (w.date != l.day) said += "his now: “${Cook.shortly(line.words)}” booked at ${w.date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM", java.util.Locale.UK))}"
                is Days.Where.Rung -> said += "his now: “${Cook.shortly(line.words)}” on this ${w.level}'s rung"
                else -> {}
            }
        }
        if (Days.aheadOf(handle) in landed) for (line in l.lines.filter { it.ticked && !it.already && it.where is Days.Where.Pile })
            if (line.words !in written.already && written.over.none { it.contains(line.words) }) said += "his hand: “${Cook.shortly(line.words)}”, ${Days.PILES.getOrElse((line.where as Days.Where.Pile).n - 1) { "a pile" }}"
        val skipped = written.already.map { "“${Cook.shortly(it)}”: already on his hand, so not written again" }
        val recorded = Close.recordYes(handle, "his day of $day kept", said + skipped, refused + written.over, pages)
        return Kept(said, written.already, written.over, refused, recorded)
    }
}
