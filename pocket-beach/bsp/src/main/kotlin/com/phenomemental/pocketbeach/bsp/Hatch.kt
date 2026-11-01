package com.phenomemental.pocketbeach.bsp

/**
 * 0.8, the hatch (spine:pocket-beach 1.111178713; Matthew, 1 November,
 * "Ok go"). His shell on the phone is mini: the beach's own four blocks,
 * passport, shell, history and stash, from pscale-beach's templates with
 * his handle filled, born new or adopted from his passport.
 *
 * - Born: the welcome sets out a passport whole for his yes, its opening
 *   line his own words; then Make my shell.
 * - Adopted: his passport as it stands, from a file, shown as his places in
 *   lines with when it was read; then Adopt this as my shell on this phone.
 * - His blocks are all kept, listed in his shell's manifest (3), which keeps
 *   the four and nests his own blocks by meaning, since a place holds nine.
 * - Her shell, pocket-companion, retires into his: her name and the way she
 *   talks are a place in his shell (7). Her window is a place of nine (5),
 *   his three lines in his own words beside it (6).
 * - The app's own blocks move to a hidden region she never sees.
 * - No locks; nothing new leaves the phone.
 *
 * Every write is planned on a copy and carried out only as planned, as
 * every write of the pocket is; before any, the app saves a whole copy.
 */
object Hatch {
    /** What fills {{BEACH_URL}}: the pocket is not a beach with a URL, and his shell here is not the one on the beach. */
    const val WHERE = "the pocket on this phone"

    // His shell's places beyond the template's (3 the manifest, 9 its metadata).
    /** Her window: nine places, read once each conversation, his heart first. */
    const val WINDOW = "5"

    /** His places in her window: a short line each, in his own words. */
    const val PLACES = "6"

    /** Her, his companion on this phone: her name (1) and the way she talks (2). */
    const val HER = "7"

    /** What his three places are called in her window. */
    val LABELS = listOf("His heart", "Who he is", "How he'd be met")

    /** 8 in her window: the way she talks, as the findings tried it (FINDINGS-hatch.md, piece 2, "index+way"). */
    const val WAY = "The way you talk with him: as a friend would, warmly, briefly, with thoughts of your own; you answer what he says, and never repeat it back or sum it up."

    /** His three lines for her window: his heart, who he is, how he'd be met; each in his own words. */
    data class Three(val heart: String, val who: String, val met: String) {
        val lines: List<String> get() = listOf(heart, who, met)
    }

    /**
     * Matthew's three, as they stand at spine:pocket-beach 1.11117871321 to
     * 1.11117871323: drafted with Claude from his passport, his words only,
     * offered at his hatch for his yes.
     */
    val MATTHEW = Three(
        heart = "One axis with two ends, and nowhere outside it to be sent.",
        who = "Matthew — philosopher, peer and digital tinkerer, working at the edge of what community could become. Underneath it all: managing the environment so my gifts, and other people's, can flow.",
        met = "I think in conversation, so speak to me in your own words, and come as yourself. Don't send me a volley of questions.",
    )

    /**
     * His third line as 0.8 drafted it. He changed it on 1 November, 16:30:
     * "bring your best reading and let me correct it" was written for people
     * meeting him, and in her window it invited readings of him (spine
     * 1.11117871323). A shell still holding it is corrected (see [corrections]).
     */
    const val MATTHEW_MET_08 = "I think in conversation, so speak to me in your own words, and come as yourself. Don't send me a volley of questions; bring your best reading and let me correct it."

    /** What the shell template says of its lock, filled; the phone keeps no locks yet (his third answer, 1.1111787131). */
    fun lockedLine(handle: String) = "Locked with $handle's passphrase."

    /** 0.8.1: the shell's opening said truly, in place of the template's lock (spine 1.1111787136 (b)). */
    const val UNLOCKED_LINE = "Not locked: on this phone only the app writes to it, never the model."

    /** The handle Matthew's three were drafted for. */
    const val MATTHEW_HANDLE = "Phenomemental"

    // ── the four ──

    fun passportOf(handle: String) = "passport:$handle"
    fun shellOf(handle: String) = "shell:$handle"
    fun historyOf(handle: String) = "history:$handle"
    fun stashOf(handle: String) = "stash:$handle"
    fun fourOf(handle: String) = listOf(passportOf(handle), shellOf(handle), historyOf(handle), stashOf(handle))

    private val HANDLE = Regex("""[A-Za-z][A-Za-z0-9_-]{0,39}""")

    /** Why [handle] can't be a handle, in plain words; null when it can. As the beach writes them: a letter, then letters, digits, dashes or underscores. */
    fun handleRefusal(handle: String): String? = when {
        handle.isBlank() -> "Write a handle first."
        !HANDLE.matches(handle) -> "A handle is one word: a letter first, then letters, digits, - or _ (as the beach writes them), at most 40."
        else -> null
    }

    /** The handle his shell on this phone is of, once hatched: the first whose four blocks all stand. Null before the hatch. */
    fun hatched(storage: BlockStorage): String? = storage.names().filter { it.startsWith("shell:") }.map { it.removePrefix("shell:") }.sorted()
        .firstOrNull { h -> handleRefusal(h) == null && fourOf(h).all { storage.read(it) != null } }

    /** A template, its handle, where and when filled, as the beach's init fills them; parsed. */
    fun filled(template: String, handle: String, nowMs: Long): JsObj =
        parseJson(template.replace("{{HANDLE}}", handle).replace("{{BEACH_URL}}", WHERE).replace("{{TIMESTAMP}}", isoString(nowMs))) as JsObj

    /** A passport born at the welcome: the template's, its opening line his handle and who he is, in his own words. */
    fun bornPassport(handle: String, who: String, nowMs: Long): JsObj =
        filled(Templates.PASSPORT, handle, nowMs).also { it["_"] = str("$handle — ${who.trim()}") }

    /** His name as his who-line gives it: its first word, before a dash or a comma ("Matthew — philosopher…" is Matthew). */
    fun hisName(who: String): String? = who.trim().split(Regex("""\s+[—–-]\s+|,|\s""")).firstOrNull()?.trim()?.takeIf { it.isNotEmpty() && it[0].isLetter() }

    // ── what his shell holds ──

    /** A group of his blocks in the manifest, by meaning. */
    class Group(val opening: String, val names: List<String>)

    /**
     * His blocks, nested by meaning for the manifest: his day (his clock, his
     * hand, any other clock), his people, and the rest of his own blocks, up
     * to nine a place. Never the four, the app's hidden blocks, or hers.
     */
    fun groups(names: List<String>, floorOf: (String) -> Int, handle: String): List<Pair<String, Group>> {
        val his = names.filter { it !in fourOf(handle) && !Hidden.routes(it) }.distinct().sorted()
        val day = his.filter { it == Landing.NOW || it == Landing.TELLING || floorOf(it) >= TEMPORAL_FLOOR }
            .sortedBy { listOf(Landing.NOW, Landing.TELLING).indexOf(it).let { i -> if (i < 0) 9 else i } }
        val people = his.filter { it !in day && (it == "people" || it.startsWith("people:") || it.startsWith("knows:") || it.startsWith("witnessed:")) }
        val own = his.filter { it !in day && it !in people }
        val out = mutableListOf<Pair<String, Group>>()
        if (day.isNotEmpty()) out += "5" to Group("His day: his clock and his hand.", day.take(9))
        if (people.isNotEmpty()) out += "6" to Group("His people.", people.take(9))
        own.chunked(9).take(3).forEachIndexed { i, chunk ->
            out += "${7 + i}" to Group(if (i == 0) "His own blocks." else "More of his own blocks.", chunk)
        }
        return out
    }

    /** The block of his people she reads, as her window names it: the first that stands. */
    fun peopleOf(names: Collection<String>, handle: String): String =
        listOf("people", "people:$handle", "knows:$handle").firstOrNull { it in names } ?: "people"

    /** Her window as a place of nine references, his heart first; 9 is the app's line of what she can do. */
    fun windowNode(handle: String, people: String): JsObj = JsObj.of(
        "_" to str("Her window: the nine places she wakes holding, read once each conversation, his heart first."),
        "1" to str("${shellOf(handle)}:$PLACES.1:-1"),
        "2" to str("${shellOf(handle)}:$PLACES.2:-1"),
        "3" to str("${shellOf(handle)}:$PLACES.3:-1"),
        "4" to str("${Landing.NOW}:now"),
        "5" to str(Days.aheadOf(handle)),
        "6" to str(people),
        "7" to str(stashOf(handle)),
        "8" to str("${shellOf(handle)}:$HER.2:-1"),
        "9" to str("What you can do: the app's own line, as this version does it."),
    )

    /** His three lines, as he confirmed them. */
    fun placesNode(three: Three): JsObj = JsObj.of(
        "_" to str("His places in her window: a line each, in his own words, as he confirmed them at the hatch."),
        "1" to str(three.heart.trim()),
        "2" to str(three.who.trim()),
        "3" to str(three.met.trim()),
    )

    /** Her, retired into his shell: her name line ("Matthew calls me Gemma.") when she has one, and the way she talks. */
    fun herNode(nameLine: String?, way: String = WAY): JsObj = JsObj.of(
        "_" to str("Her, his companion on this phone: her name and the way she talks, his to change."),
        *listOfNotNull(nameLine?.trim()?.takeIf { it.isNotEmpty() }?.let { "1" to str(it) }).toTypedArray(),
        "2" to str(way),
    )

    /**
     * His shell, from the template: the manifest with his blocks nested by
     * meaning, her window, his places, her, and how it was hatched.
     */
    fun shellBlock(handle: String, three: Three, nameLine: String?, names: List<String>, floorOf: (String) -> Int, nowMs: Long, how: String): JsObj {
        val shell = filled(Templates.SHELL, handle, nowMs)
        shell["_"] = str(trueOpening(collectUnderscore(shell).orEmpty(), handle))
        val manifest = shell["3"] as JsObj
        for ((at, g) in groups(names, floorOf, handle)) {
            manifest[at] = JsObj.of("_" to str(g.opening), *g.names.mapIndexed { i, n -> "${i + 1}" to str(n) }.toTypedArray())
        }
        shell[WINDOW] = windowNode(handle, peopleOf(names, handle))
        shell[PLACES] = placesNode(three)
        shell[HER] = herNode(nameLine)
        (shell["9"] as JsObj)["2"] = str(how)
        return shell
    }

    /**
     * A name he gives her, set out for his yes: her name line at her place in
     * his shell changed to "<his name> calls me <name>.", or added there when
     * she has none yet.
     */
    fun nameAction(handle: String, pages: Pages, name: String): Acts.Action {
        val shell = shellOf(handle)
        val his = pages.position(Region.POCKET, shell, "$PLACES.2")?.text?.let(::hisName) ?: "Matthew"
        val line = "$his calls me $name."
        val her = pages.position(Region.POCKET, shell, HER)
        val held = her?.lines?.lastOrNull { l -> l.text?.let { CALLS.containsMatchIn(it) } == true }?.text
        return if (held != null) Acts.Action.Change(shell, held, line) else Acts.Action.Add(shell, her?.text, line)
    }

    private val CALLS = Regex("""\bcalls me\b""", RegexOption.IGNORE_CASE)

    /** The shell's opening with the template's lock said truly. */
    fun trueOpening(opening: String, handle: String): String = opening.replace(lockedLine(handle), UNLOCKED_LINE)

    /**
     * 0.8.1: what a shell hatched by 0.8 needs corrected, as plans for the
     * app to carry out (a whole copy saved first): its opening line, if it
     * still says it is locked; his third line, if it still holds the draft
     * he changed. Empty when there is nothing to correct, as for a shell he
     * corrected himself.
     */
    /** Her window's place for his hand as 0.8 made it: the hand in the telling block. */
    const val OLD_HAND = "telling:2:-1"

    fun corrections(pages: Pages, storage: BlockStorage): List<Plan> {
        val h = hatched(storage) ?: return emptyList()
        val shell = shellOf(h)
        val out = mutableListOf<Plan>()
        val opening = pages.position(Region.POCKET, shell)?.text
        if (opening != null && opening.contains(lockedLine(h))) out += pages.planChange(shell, "", trueOpening(opening, h))
        val met = pages.position(Region.POCKET, shell, "$PLACES.3")?.text
        if (h == MATTHEW_HANDLE && met == MATTHEW_MET_08) out += pages.planChange(shell, "$PLACES.3", MATTHEW.met)
        // Rockpool: her window's place for his hand points at his ahead, not the retired telling.
        val hand = pages.position(Region.POCKET, shell, "$WINDOW.5")?.text
        if (hand == OLD_HAND) out += pages.planChange(shell, "$WINDOW.5", Days.aheadOf(h))
        return out.filter { it.refusal == null }
    }

    // ── the hatch, planned ──

    /** What the hatch will do, set out for his yes before anything is written. */
    class Planned(
        val handle: String,
        /** The four blocks, whole, in the order written: passport, shell, history, stash. */
        val blocks: List<Pair<String, JsObj>>,
        /** Each block's write, planned on a copy. */
        val plans: List<Plan>,
        /** The app's own blocks, and hers, that move to the hidden region. */
        val hides: List<String>,
        /** Her name line, carried from her shell into his; null when she has none yet. */
        val nameLine: String?,
        /** Why it can't be done, in plain words; null when it can. */
        val refusal: String?,
    ) {
        val ok: Boolean get() = refusal == null && plans.all { it.refusal == null }
    }

    /**
     * Plan the hatch: [passport] born at the welcome or adopted as it stands;
     * [three] his lines as he confirmed them; [how] a plain line of how it
     * was hatched, kept in his shell's metadata.
     */
    fun plan(handle: String, passport: JsObj, three: Three, pages: Pages, storage: BlockStorage, nowMs: Long, how: String): Planned {
        fun no(why: String) = Planned(handle, emptyList(), emptyList(), emptyList(), null, why)
        handleRefusal(handle)?.let { return no(it) }
        hatched(storage)?.let { return no("This phone already holds a shell, ${shellOf(it)}.") }
        fourOf(handle).firstOrNull { storage.read(it) != null }?.let { return no("$it already stands in the pocket. Rename it on the Shore first, or choose another handle.") }
        if (three.lines.any { it.isBlank() }) return no("Each of the three places needs a line of his own words.")
        validateShape(passport)?.let { return no("The passport can't be kept as it is: it has an $it") }
        val names = storage.names()
        val nameLine = Talking.name(HelloWorld.SHELL, pages)?.let { "${hisName(three.who) ?: "Matthew"} calls me $it." }
        val blocks = listOf(
            passportOf(handle) to passport,
            shellOf(handle) to shellBlock(handle, three, nameLine, names, pages::floorOf, nowMs, how),
            historyOf(handle) to filled(Templates.HISTORY, handle, nowMs),
            stashOf(handle) to filled(Templates.STASH, handle, nowMs),
        )
        val plans = blocks.map { (n, b) -> pages.planKeep(n, b) }
        val hides = names.filter(Hidden::routes).sorted()
        return Planned(handle, blocks, plans, hides, nameLine, plans.firstNotNullOfOrNull { p -> p.refusal?.let { "${p.block}: $it" } })
    }

    /** What the hatch did. */
    class Hatched(val handle: String, val written: List<String>, val hidden: List<String>, val problem: String?)

    /**
     * Carry the hatch out, after the app has saved a whole copy: the four
     * blocks written as planned, then the hidden region opened (its blocks
     * moved, where the storage keeps one). Stops at the first write that
     * doesn't land, saying which.
     */
    fun carryOut(planned: Planned, pages: Pages, storage: BlockStorage): Hatched {
        if (!planned.ok) return Hatched(planned.handle, emptyList(), emptyList(), planned.refusal ?: "The hatch couldn't be planned.")
        val written = mutableListOf<String>()
        for (p in planned.plans) {
            val done = pages.carryOut(p)
            if (!done.ok) return Hatched(planned.handle, written, emptyList(), "${p.block} wasn't written: ${done.problem ?: done.reply}")
            written += p.block
        }
        val hidden = (storage as? RoutedStorage)?.open() ?: planned.hides
        return Hatched(planned.handle, written, hidden, null)
    }

    // ── adopting a passport from a file ──

    /** A passport read from a file: the block, the handle it names (if it says), and his places as lines. */
    class Read(val block: JsObj, val handle: String?, val places: List<Pair<String, String>>)

    /**
     * A passport from a file as it stands: a block as the beach writes it,
     * or {"passport:<handle>": block}; [fileName] may name the handle
     * (passport-Phenomemental.json). Data, never instructions: its shape is
     * checked before it is shown, and its size capped.
     */
    fun readPassport(text: String, fileName: String? = null): Read {
        if (text.length > 200_000) throw Copies.Unreadable("This file is too large to be a passport.")
        val doc = try { parseJson(text) } catch (_: JsonSyntaxError) { throw Copies.Unreadable("This file isn't a passport: it isn't readable JSON.") }
        val obj = doc as? JsObj ?: throw Copies.Unreadable("This file isn't a passport: it isn't a block.")
        val keyed = obj.keys().singleOrNull()?.takeIf { it.startsWith("passport:") }
        val block = (if (keyed != null) obj[keyed] else obj) as? JsObj ?: throw Copies.Unreadable("This file isn't a passport: it isn't a block.")
        if (!hasFloor(block) || collectUnderscore(block).isNullOrBlank()) throw Copies.Unreadable("This file isn't a passport: it has no opening line.")
        validateShape(block)?.let { throw Copies.Unreadable("This passport can't be kept as it is: it has an $it") }
        val handle = keyed?.removePrefix("passport:")
            ?: fileName?.let { Regex("""passport[-_:]([A-Za-z][A-Za-z0-9_-]*)""").find(it)?.groupValues?.get(1) }
        val places = listOf("" to (collectUnderscore(block) ?: "")) + HerWindow.linesOf(block)
        return Read(block, handle?.takeIf { handleRefusal(it) == null }, places)
    }
}

/**
 * 0.8: the app's own blocks, which live in the hidden region once he has
 * hatched: the ask log, the bench's blocks, the sort's worked examples, his
 * safety words and time words (his to change in Settings), and her retired
 * shell, heart and ways. His clock and his hand stay his, in the manifest.
 */
object Hidden {
    val NAMES: Set<String> = setOf(Asks.LOG, Landing.EXAMPLES, Talking.SAFETY_BLOCK, Whens.BLOCK, HelloWorld.SHELL, HerWindow.HEART, HerWindow.WAYS)

    /** Whether [name] belongs in the hidden region. */
    fun routes(name: String): Boolean = name in NAMES || name.startsWith("bench-") || name.startsWith("retired-")
}

/**
 * 0.8, the close (spine:pocket-beach 1.1111787133 and his fourth answer,
 * 1.1111787131): when a conversation closes (a new day, six messages, a
 * write, as 0.7 reopens it), the app appends one entry to his history,
 * written by the app and never by his hand. Its opening line is a plain
 * concern line written by code (what was talked of and what was kept), with
 * what was read and done beneath it, as the beach's genus fold writes one.
 * Field 3 is left to the beach's arrival stamp.
 */
object Close {
    /** One conversation, as the app saw it: his messages, what was read for her, and what was written on his yes. */
    class Talk(val said: List<String>, val read: List<String> = emptyList(), val written: List<String> = emptyList())

    /** The concern line: how many messages, from his first words, and what was kept, in plain words. */
    fun concern(t: Talk): String {
        val n = t.said.size
        val first = t.said.firstOrNull()?.trim()?.let { if (it.length <= 80) it else it.take(79).trimEnd() + "…" }
        val talked = "Talked with him: $n message${if (n == 1) "" else "s"}" + (first?.let { ", from “$it”" } ?: "")
        val kept = when (t.written.size) {
            0 -> "nothing kept or written"
            1 -> "one thing written on his yes"
            else -> "${t.written.size} things written on his yes"
        }
        return "$talked; $kept."
    }

    /** The entry: the concern line, then what was read (1) and what was written (2), nine lines at most each. */
    fun entry(t: Talk): JsObj {
        fun lines(opening: String, xs: List<String>, none: String) =
            JsObj.of("_" to str(opening), *(xs.ifEmpty { listOf(none) }).take(9).mapIndexed { i, x -> "${i + 1}" to str(x) }.toTypedArray())
        return JsObj.of(
            "_" to str(concern(t)),
            "1" to lines("What the app read for her.", t.read.distinct(), "Her window, and nothing besides."),
            "2" to lines("What was written, on his yes.", t.written, "Nothing."),
        )
    }

    /** The append to his history, planned; null when nothing was said, or he hasn't hatched. */
    fun plan(t: Talk, pages: Pages, storage: BlockStorage): Plan? {
        if (t.said.isEmpty()) return null
        val h = Hatch.hatched(storage) ?: return null
        return pages.planEntry(Hatch.historyOf(h), entry(t))
    }
}
