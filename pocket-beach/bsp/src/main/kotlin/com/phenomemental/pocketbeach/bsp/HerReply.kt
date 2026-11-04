package com.phenomemental.pocketbeach.bsp

/**
 * Sundial 0.10.1: her reply in the parts his cook names (TALK's THE
 * REFLECTION and THE QUESTION), through the engine's tool call on E4B.
 *
 * On his Pixel on 3 November her reply to "I've done some of the id checks
 * today, …" showed only part two. Every attempt is now kept exactly as her
 * tool call returned it, for the workings; when part one comes back empty
 * she is asked once more; and a reply with any part still empty is never
 * shown as if whole: she is asked free instead, as his cook says ("when her
 * answer isn't whole, her reply stands as she writes it").
 */
object HerReply {
    /** [attempts]: each tool call as she returned it (null: no whole call); [parts]: the parts shown, or null when her answer isn't whole. */
    class InParts(val attempts: List<Map<String, String>?>, val parts: Map<String, String>?)

    /** The app's line when part one came back empty, added to her turn for the one more ask. */
    const val AGAIN = "The app: your reply's first part came back empty. Give both parts this time: part one gives back what he said in this message, " +
        "in his own words, beginning with You; part two is your question."

    /**
     * Ask for her reply in [asked] parts: [ask] once, and once more ([ask]
     * with `again`) when part one comes back empty. [clean] is what Ask does
     * to a part before it is shown. Whole only when every part holds words.
     */
    fun inParts(asked: List<String>, ask: (again: Boolean) -> Map<String, String>?, clean: (String) -> String = { it.trim() }): InParts {
        if (asked.isEmpty()) return InParts(emptyList(), null)
        val attempts = mutableListOf<Map<String, String>?>()
        var got = ask(false).also { attempts += it }
        if (got != null && clean(got[asked.first()].orEmpty()).isBlank()) got = ask(true).also { attempts += it }
        val cleaned = got?.let { g -> asked.associateWith { clean(g[it].orEmpty()) } }
        return InParts(attempts, cleaned?.takeIf { c -> c.values.all { it.isNotBlank() } })
    }

    /** One attempt as the workings record it: each part exactly as her tool call returned it, an empty one shown empty. */
    fun said(asked: List<String>, attempt: Map<String, String>?): String =
        attempt?.let { a -> asked.joinToString("; ") { "$it “${a[it].orEmpty()}”" } } ?: "no whole tool call came back"

    // ── Sundial 0.10.1: her reply in one of two shapes (cook 1.1.8.5) ──

    /**
     * A shape her reply may take through the tool call: REFLECT (his words
     * back, beginning "You", then one question from a place his words
     * haven't touched) or ANSWER (a sentence or two answering him). On his
     * Pixel on 3 November the one shape the tool had left her only able to
     * mirror his question back ("You've heard of Alfred North Whitehead.").
     */
    data class Shape(val name: String, val parts: List<String>) {
        /** The parts that must hold words for the reply to be whole: every one but the places, which may be none. */
        val required: List<String> get() = parts.filter { it != PLACES }
        /** The tool's name: the shape's own, in small letters. */
        val tool: String get() = name.lowercase()
    }

    /** The places his words touch, named by her in REFLECT (Sundial 0.10.1: in the reply's own call, never a call of its own). */
    const val PLACES = "THE PLACES TOUCHED"

    /** [shape]: the shape she chose (null: no whole call); [parts]: the parts shown, or null when her answer isn't whole; [placed]: the places she named, six counted as none. */
    class InShape(val attempts: List<String>, val shape: String?, val parts: Map<String, String>?, val placed: List<Int>, val allSix: Boolean)

    /**
     * Ask for her reply in one of [shapes] ([ask]: the shape she chose and
     * each part as returned; null when no whole call came back). When she
     * chose the first shape (REFLECT) and its part one came back empty, she
     * is asked once more; a reply with any required part empty is never
     * whole. ANSWER is never asked again: an empty answer is no answer.
     */
    fun inShapes(shapes: List<Shape>, ask: (again: Boolean) -> Pair<String, Map<String, String>>?, clean: (String) -> String = { it.trim() }): InShape {
        if (shapes.isEmpty()) return InShape(emptyList(), null, null, emptyList(), false)
        val attempts = mutableListOf<String>()
        fun shapeOf(got: Pair<String, Map<String, String>>?) = got?.let { g -> shapes.firstOrNull { it.name == g.first || it.tool == g.first } }
        fun record(got: Pair<String, Map<String, String>>?) {
            val sh = shapeOf(got)
            attempts += if (got == null || sh == null) "no whole tool call came back" else "${sh.name}: " + said(sh.parts, got.second)
        }
        var got = ask(false).also(::record)
        val first = shapes.first()
        if (shapeOf(got) == first && clean(got!!.second[first.required.first()].orEmpty()).isBlank()) got = ask(true).also(::record)
        val sh = shapeOf(got) ?: return InShape(attempts, null, null, emptyList(), false)
        val g = got!!.second
        val named = placesOf(g[PLACES].orEmpty())
        val allSix = named.size == Mark.POOLS.size
        val cleaned = sh.parts.associateWith { if (it == PLACES) "" else clean(g[it].orEmpty()) }
        val whole = sh.required.all { cleaned[it].orEmpty().isNotBlank() }
        return InShape(attempts, sh.name, cleaned.takeIf { whole }, if (allSix) emptyList() else named, allSix)
    }

    /** The places she named, by their phrases (cook 6.2's ring, or Sundial's own), in P6's order. */
    fun placesOf(text: String): List<Int> {
        val plain = Telling.plain(text.replace('’', '\''))
        val names = listOf("what happened", "what he felt", "what he made of it", "what matters", "a choice", "what's ahead")
        val found = Mark.POOLS.filter { p ->
            val own = Telling.plain(p.phrase.replace('’', '\''))
            val cook = Telling.plain(names[p.n - 1])
            " $plain ".contains(" $own ") || " $plain ".contains(" $cook ") || " $plain ".contains(" ${p.name.lowercase()} ")
        }.map { it.n }
        return Mark.inOrder(found)
    }

    /** What each part of a shape is, as her tool call is told. */
    fun describe(part: String): String = when (part) {
        PLACES -> "The places his words plainly touch, by name, from: what happened, what he felt, what he made of it, what matters, a choice, what's ahead. None when his words hold no day."
        "THE REFLECTION" -> "Part one: what he said, given back in his own words, beginning with You"
        "THE QUESTION" -> "Part two: one question, from a place his words haven't touched, ending with a question mark"
        "THE ANSWER" -> "Your answer to what he asked, in a sentence or two; it may end with a question"
        else -> part
    }

    /** What each shape is for, and when, as her tool call is told (laptop, 3 November: without the when, she mirrored his question two seeds in three). */
    fun purpose(shape: Shape): String = when (shape.name) {
        "REFLECT" -> "Reflect, when he tells you something (what happened, what he did, felt or thinks): give back what he said in his own words, beginning with You, then ask one question from a place his words haven't touched."
        "ANSWER" -> "Answer, when he asks you something (a question put to you, such as \"have you heard of …?\" or \"what do you think …?\"): answer it in a sentence or two, in your own words, and never give his question back to him. You may end with a question."
        else -> "Your reply, in its parts."
    }

    /** The line beneath her instruction when her reply comes through the tool call: which tool, when. */
    fun choosing(shapes: List<Shape>): String =
        "Give each reply by calling one of your tools: " + shapes.joinToString("; ") { sh ->
            when (sh.name) {
                "REFLECT" -> "${sh.tool}, when he tells you something"
                "ANSWER" -> "${sh.tool}, when he asks you something"
                else -> sh.tool
            }
        } + "."

    // ── Sundial 0.10.2: her reply written free, read by the app ──

    /**
     * Her reply as she wrote it, read as Sundial shows it. On his Pixel's E4B
     * the reply's tool call came back with no call at all (she wrote her
     * reply as plain words), so every turn ran her twice. Now she writes
     * freely: [raw] as she wrote it; her last line, "Places:" and the places
     * her words touch ([placesLine], [places], six named counting as none),
     * taken off before showing; then of what is shown, a closing sentence
     * ending in "?" is her [question], in italic, and what comes before it
     * [partOne]. [shape]: REFLECT (part one, then her question), QUESTION
     * (her question alone), ANSWER (no closing question: plain words).
     */
    class Free(val raw: String, val placesLine: String?, val places: List<Int>, val allSix: Boolean, val partOne: String, val question: String) {
        val shape: String get() = when {
            question.isEmpty() -> "ANSWER"
            partOne.isEmpty() -> "QUESTION"
            else -> "REFLECT"
        }
    }

    /** What the places line opens with, as her line and the app's parse have it. */
    const val PLACES_LINE = "Places:"

    private val PLACES_AT = Regex("""^[\s*_"“(\[]*places?(?:\s+touched)?\s*:\s*(.*?)[\s*_"”)\].]*$""", RegexOption.IGNORE_CASE)

    /** The places a piece of her reply names, when it is her places line ("Places: what happened, what matters"); null when it isn't one. */
    fun placesLine(piece: String): String? = PLACES_AT.find(piece.trim())?.groupValues?.get(1)

    /**
     * Her free reply read: the places line her last line gives, if any, and
     * the split of [shown] (what the app's checks leave of [raw], the places
     * line among what they take off) into part one and her closing question.
     */
    fun free(raw: String, shown: String): Free {
        val last = raw.trimEnd().lines().lastOrNull { it.isNotBlank() }?.let { Talking.piecesOf(it).lastOrNull { p -> p.isNotBlank() } }
        val line = last?.let { placesLine(it) }
        val named = line?.let { placesOf(it) }.orEmpty()
        val allSix = named.size == Mark.POOLS.size
        val (one, q) = split(shown)
        return Free(raw, line?.let { last.trim() }, if (allSix) emptyList() else named, allSix, one, q)
    }

    /** [text] as part one and a closing question: the last sentence when it ends in "?", in italic; the question alone when nothing comes before it; else no question. */
    fun split(text: String): Pair<String, String> {
        val t = text.trim()
        val lastLine = t.lines().lastOrNull { it.isNotBlank() } ?: return "" to ""
        val q = Talking.piecesOf(lastLine).lastOrNull { it.isNotBlank() }?.trim() ?: return t to ""
        val bare = q.trim('*', '_', ' ')
        if (!bare.trimEnd('”', '"', '’', ')').endsWith("?")) return t to ""
        val before = t.substring(0, t.lastIndexOf(q)).trim()
        return before to bare
    }

    /** A part's field in the tool call: "THE PLACES TOUCHED" is "places_touched". */
    fun field(part: String): String = part.removePrefix("THE ").trim().lowercase().replace(' ', '_')

    /** The tool for [shape], as the engine is given it (OpenAPI JSON): the app's and the laptop's the same. */
    fun toolJson(shape: Shape): String {
        val props = shape.parts.joinToString(",") { p ->
            val type = if (p == PLACES) """"type":"array","items":{"type":"string"}""" else """"type":"string""""
            """"${field(p)}":{$type,"description":${stringify(str(describe(p)))}}"""
        }
        val required = shape.required.joinToString(",") { "\"${field(it)}\"" }
        return """{"name":"${shape.tool}","description":${stringify(str(purpose(shape)))},"parameters":{"type":"object","properties":{$props},"required":[$required]}}"""
    }
}
