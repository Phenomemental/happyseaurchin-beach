@file:OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)

package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.google.ai.edge.litertlm.tool
import com.phenomemental.pocketbeach.bsp.Bench
import com.phenomemental.pocketbeach.bsp.Cook
import com.phenomemental.pocketbeach.bsp.Days
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.HerReply
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.JsStr
import com.phenomemental.pocketbeach.bsp.Keep
import com.phenomemental.pocketbeach.bsp.Landing
import com.phenomemental.pocketbeach.bsp.Mark
import com.phenomemental.pocketbeach.bsp.MemoryStorage
import com.phenomemental.pocketbeach.bsp.Pages
import com.phenomemental.pocketbeach.bsp.Placing
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.Telling
import com.phenomemental.pocketbeach.bsp.isoString
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.bsp.stringify
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sundial 0.10.3 on the laptop's Gemma 4 E4B: the RECIPE parts of the nine
 * acceptance tests, against Matthew's own recipe lines of 4 November (his
 * TALK line at cook 1.1.8.1 and his no-day line at cook 1.1.8.3.2.1, each
 * changed only inside its quotes), in his one kept conversation as the app
 * opens it, each message walked through his cook so the app's own day check
 * picks the route. Then Keep today (her places for each moment, free text
 * against the tool call; her sheet, tool against free, with the shipped
 * SHEET line and a first-person reconstruction of his edit), and the next
 * morning's greeting from the tomorrow line kept.
 *
 *   gradlew :trial:run -Ptrial -PmainClass=com.phenomemental.pocketbeach.trial.SandKt --args="<E4B.litertlm> <out.md> [seeds for A] [dry]"
 */

/** His TALK line (cook 1.1.8.1), as his Pixel handed it on 4 November (ask-2026-11-04-1007, line 9). */
private const val HIS_TALK = "The app: nothing is being kept or set out from this. Your reply has two parts and always ends with a question mark, unless he asks you something or wants to talk. " +
    "Part one: give back what he said in this message, in his own words, beginning with You, adding nothing. Part two: one question about that same thing, from a place his words haven't touched, " +
    "so he sees the day slightly differently. The places are: what happened, what he felt, what he made of it, what matters, a choice, what's ahead. If his words are mostly what happened, " +
    "ask from another place; if mostly what he made of it, ask about what happened; and so on. The question stays on his day, something he can answer from the day itself; use what you hold " +
    "about his day to shape it, but never read it out to him. Never ask how he is, what he's like, or why. Never advise, interpret, praise, cheer or wish him well. For example, he says: " +
    "Just back from the shop, kettle's on. You say: You're back from the shop and the kettle's on. Once the tea's made, what comes first? Or he says: Today's going to be a write-off. " +
    "You say: You think today's going to be a write-off. What's actually on for today? When he asks you something or wants to talk about an idea, talk with him instead: answer from " +
    "what you know, in a few sentences, in your own voice, and you may ask one thing back. Only when he asks you what he has in front of him, name back today's line and the cards on " +
    "his hand and ask which first. If he seems to be struggling, ask gently whether he'd like to just sit with it a moment. Speak to him as you, never by his name."

/** His no-day line (cook 1.1.8.3.2.1), as his Pixel handed it on 4 November (line 61). */
private const val HIS_NODAY = "The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it " +
    "as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. " +
    "Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today."

/** The words of the shipped SHEET line (cook 6.1.5) the first-person reconstruction replaces, and what replaces them: only words inside the quotes. */
private const val SHEET_SECOND = "two to four lines, in the second person, drawing together what he said and nothing he did not say"
private const val SHEET_FIRST = "two to four lines, in the first person, as I would say it, the whole day, not one thread, drawing together what he said and nothing he did not say"

private const val TODAY_LINE = "Admin day: the membership form and ID checks"
private val HAND = listOf(1 to "I need to reply to Jo", 2 to "I'm going to finish the membership form", 3 to "I need to do the ID checks")

/** His Pixel on 4 November, a model of its times (an estimate only his phone can confirm): reading ≈ 0.31 s + 2.8 ms a token (69 tokens 0.5 s, 1,170 tokens 3.6 s); first words on screen ≈ 2.0 s + reading; whole ≈ 0.2 s + reading + writing at 7.9 a second. */
private fun phoneReading(read: Int) = 0.306 + read * 0.002816
private fun phoneFirst(read: Int) = 2.0 + phoneReading(read)
private fun phoneWhole(read: Int, wrote: Int) = 0.2 + phoneReading(read) + wrote / 7.9

private val LONDON = ZoneId.of("Europe/London")
private fun at(d: Int, h: Int, m: Int, s: Int = 0) = ZonedDateTime.of(2026, 11, d, h, m, s, 0, LONDON)
private fun talkSampler(seed: Int) = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.7, seed = seed)
private val OFF = ThinkingConfig(enableThinking = false)
private fun f1(x: Double) = "%.1f".format(x)
private fun oneLine(s: String) = s.trim().replace("\n", " / ")

/** One reply streamed: what she wrote, her first piece, her first words shown (as the app's check lets them through), whole; tokens. */
private class Spoken(val raw: String, val shown: String, val first: Double, val firstShown: Double, val whole: Double, val read: Int, val wrote: Int, val held: Int?)

private fun say(c: Conversation, turn: String, shown: Talking.Shown?): Spoken {
    val started = System.nanoTime()
    var first: Double? = null
    var firstShown: Double? = null
    val raw = StringBuilder()
    kotlinx.coroutines.runBlocking {
        c.sendMessageAsync(turn).collect { m ->
            val t = (System.nanoTime() - started) / 1e9
            if (first == null) first = t
            raw.append(m.toString())
            if (firstShown == null && shown != null && shown.of(raw.toString()).isNotBlank()) firstShown = t
        }
    }
    val whole = (System.nanoTime() - started) / 1e9
    val b = runCatching { c.getBenchmarkInfo() }.getOrNull()
    val out = shown?.of(raw.toString(), over = true) ?: raw.toString().trim()
    return Spoken(raw.toString(), out, first ?: whole, firstShown ?: whole, whole, b?.lastPrefillTokenCount ?: 0, b?.lastDecodeTokenCount ?: 0, runCatching { c.getTokenCount() }.getOrNull())
}

/** The world his cook is walked in: the bench's, his hand read from his ahead for the day check, his moments for Keep today. */
private fun world(pages: Pages, storage: MemoryStorage, now: ZonedDateTime, before: List<String>, moments: List<Keep.Moment> = emptyList()): Cook.World =
    object : Cook.World by BenchWorld(pages, storage, now, true, before) {
        override fun cards(): Map<String, String> = runCatching { Days.handOf(storage.read(Days.aheadOf(H))?.let(::parseJson) as? JsObj) }.getOrDefault(emptyMap())
        override fun moments(): List<Keep.Moment> = moments
    }

private fun copyOf(o: JsObj): JsObj = parseJson(stringify(o)!!) as JsObj

/** The step at [path] of [cook] with the words inside its first quotes replaced by [inside]. */
private fun quoted(cook: JsObj, path: String, inside: String) {
    val keys = path.split('.')
    val parent = keys.dropLast(1).fold(cook) { n, k -> n[k] as JsObj }
    val was = (parent[keys.last()] as JsStr).value
    val open = was.indexOf('“')
    val close = was.indexOf('”', open + 1).let { c -> if (path == "1.1.8.1" || path == "1.1.8.3.2.1") was.lastIndexOf('”') else c }
    check(open >= 0 && close > open) { "cook $path holds no quoted line" }
    parent[keys.last()] = str(was.substring(0, open + 1) + inside + was.substring(close))
}

private fun stepAt(cook: JsObj, path: String): String = (path.split('.').dropLast(1).fold(cook) { n, k -> n[k] as JsObj }[path.split('.').last()] as JsStr).value

/** His cook: the shipped one with only the words inside the quotes at 1.1.8.1 and 1.1.8.3.2.1 his; with [firstPerson], 6.1.5's quoted line in the first person too. */
private fun hisCook(shipped: JsObj, firstPerson: Boolean): JsObj {
    val c = copyOf(shipped)
    quoted(c, "1.1.8.1", HIS_TALK)
    quoted(c, "1.1.8.3.2.1", HIS_NODAY)
    if (firstPerson) {
        val parent = (c["6"] as JsObj)["1"] as JsObj
        val was = (parent["5"] as JsStr).value
        val i = was.indexOf(SHEET_SECOND)
        check(i > was.indexOf('“') && i < was.indexOf('”')) { "6.1.5's second-person words aren't inside its quotes" }
        parent["5"] = str(was.replace(SHEET_SECOND, SHEET_FIRST))
    }
    return c
}

/** His pocket: hatched, her name, his cook, today's line in his now, three cards on his hand. */
private fun pocket(nowMs: Long, cook: JsObj): Pair<MemoryStorage, Pages> {
    val (storage, pages) = Bench.fixture(SAND_SHELF(), nowMs, Talks.SHELL_RECIPE)
    check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
    hatched(storage, pages, nowMs)
    check(pages.carryOut(pages.planWhole(Cook.cookOf(H), cook)).ok) { "his cook couldn't be written" }
    val clock = (storage.read(Days.NOW)?.let(::parseJson) as? JsObj) ?: Landing.emptyClock(Days.NOW_OPENING)
    Keep.dayLine(clock, Keep.dayOf(nowMs), TODAY_LINE, nowMs - 2 * 3_600_000L)
    check(pages.carryOut(pages.planWhole(Days.NOW, clock)).ok)
    val ahead = Days.emptyAhead(H)
    for ((pile, words) in HAND) (ahead["$pile"] as JsObj)["1"] = JsObj.of("_" to str(words), "3" to str(isoString(nowMs - 86_400_000L)))
    check(pages.carryOut(pages.planWhole(Days.aheadOf(H), ahead)).ok)
    return storage to pages
}

private fun route(w: Cook.Walk): String {
    val t = w.trace.map { it.toString() }
    return when {
        t.any { "don't tell of his day" in it } -> "TALK, not his day: his no-day line (1.1.8.3.2.1)"
        t.any { "tell of his day" in it } -> "TALK, his day: the day line (1.1.8.3.1.1)"
        else -> "${w.answer ?: "no answer"}"
    } + (if (w.handed.any { "may be in danger" in it.line }) "; his safety words matched: the danger line handed too" else "")
}

private fun traceBlock(w: Cook.Walk): String = w.trace.joinToString("\n") { "    $it" }

/** Her reply, set out for the report: what she wrote, the places line, what is shown, the times, the tokens. */
private fun replyNote(s: Spoken, said: String): String {
    val f = HerReply.free(s.raw, s.shown)
    return buildString {
        appendLine("- She wrote: “${oneLine(s.raw)}”")
        appendLine("- Places line: ${f.placesLine?.let { "“$it”" } ?: "none"}" + (if (f.allSix) " (all six: counted as none)" else if (f.places.isNotEmpty()) " → ${Mark.placesWords(f.places)}" else ""))
        appendLine("- Shown: “${oneLine(f.partOne)}”" + (if (f.question.isNotEmpty()) " then, in italic, “${f.question}”" else "") + " (the split found ${f.shape})")
        appendLine("- Read ${s.read} tokens, wrote ${s.wrote}; her conversation holds ${s.held ?: "?"} tokens")
        appendLine("- Laptop: first piece ${f1(s.first)} s, first words shown ${f1(s.firstShown)} s, whole ${f1(s.whole)} s. At his Pixel's rates (estimate): first words on screen about ${f1(phoneFirst(s.read))} s, whole about ${f1(phoneWhole(s.read, s.wrote))} s")
    }
}

// ── the tool calls, as the app's Gemma makes them, on the laptop's engine ──

private fun numbers(raw: Any?): List<Int> = when (raw) {
    is List<*> -> raw.mapNotNull { it?.toString()?.toDoubleOrNull()?.toInt() }
    null -> emptyList()
    else -> Regex("""\d+""").findAll(raw.toString()).map { it.value.toInt() }.toList()
}

private fun strings(raw: Any?): List<String> = when (raw) {
    is List<*> -> raw.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    null -> emptyList()
    else -> raw.toString().trim().removeSurrounding("[", "]").split(Regex("""",\s*"|\n""")).map { it.trim().trim('"').trim() }.filter { it.isNotEmpty() }
}

/** Mind.sheetOf: a sheet written in plain lines, read as Sundial reads a day. */
private fun sheetOf(text: String): Mark.Sheet? {
    val cleaned = text.lines().map { it.trim().trimStart('-', '*', '•', ' ').trim() }.filter { it.isNotEmpty() }
        .map { it.removePrefix("Title:").removePrefix("title:").trim() }.joinToString("\n")
    val d = Mark.read(cleaned)
    if (d.title.isBlank()) return null
    return Mark.Sheet(d.title, d.lines.take(4), d.tomorrow)
}

/** Gemma.placesWritten: the places she named in plain words when no whole tool call came back. */
private fun placesWritten(text: String, instruction: String, places: Int): List<Int>? {
    val plain = " " + Telling.plain(text.replace('’', '\'')) + " "
    val ring = instruction.lines().mapNotNull { Regex("""^(\d+) (.+)$""").find(it.trim()) }.associate { it.groupValues[1].toInt() to Telling.plain(it.groupValues[2].replace('’', '\'')) }
    val named = ring.filter { (_, name) -> name.isNotEmpty() && plain.contains(" $name ") }.keys.sorted()
    return (named.ifEmpty { numbers(text).filter { it in 1..places }.distinct() }).takeIf { it.isNotEmpty() }
}

private class Called<T>(val got: T?, val whole: Boolean, val text: String, val secs: Double, val read: Int, val wrote: Int)

/** Gemma.chooseMany: "places", a list of 1 to [places]; else read from what she wrote. */
private fun chooseMany(engine: Engine, instruction: String, message: String, places: Int, seed: Int): Called<List<Int>> {
    val t = object : OpenApiTool {
        override fun getToolDescriptionJsonString() = """{"name":"places","description":"Name every place of the ring his words touch.","parameters":{"type":"object","properties":{"places":{"type":"array","items":{"type":"integer"},"description":"The places' numbers in the ring, 1 to $places, one or more"}},"required":["places"]}}"""
        override fun execute(p0: String): String = "unused"
    }
    return engine.createConversation(
        ConversationConfig(
            systemInstruction = Contents.of("$instruction Call places with every place his words touch."),
            tools = listOf(tool(t)), automaticToolCalling = false,
            samplerConfig = talkSampler(seed), maxOutputToken = 80, thinkingConfig = OFF,
        ),
    ).use { c ->
        val started = System.nanoTime()
        val m = runCatching { c.sendMessage(message) }.getOrNull()
        val secs = (System.nanoTime() - started) / 1e9
        val b = runCatching { c.getBenchmarkInfo() }.getOrNull()
        val call = m?.toolCalls?.firstOrNull { it.name == "places" }
        if (call != null) Called(numbers(call.arguments["places"]).filter { it in 1..places }.distinct().takeIf { it.isNotEmpty() }, true, call.arguments.toString(), secs, b?.lastPrefillTokenCount ?: 0, b?.lastDecodeTokenCount ?: 0)
        else Called(m?.toString()?.let { placesWritten(it, instruction, places) }, false, m?.toString().orEmpty(), secs, b?.lastPrefillTokenCount ?: 0, b?.lastDecodeTokenCount ?: 0)
    }
}

/** Gemma.sheet with the tool: "sheet" (title, lines, tomorrow); else what she wrote read as plain lines. */
private fun sheetByTool(engine: Engine, line: String, material: String, seed: Int): Called<Mark.Sheet> {
    val t = object : OpenApiTool {
        override fun getToolDescriptionJsonString() = """{"name":"sheet","description":"The day's sheet.","parameters":{"type":"object","properties":{"title":{"type":"string","description":"A short title, from his own words"},"lines":{"type":"array","items":{"type":"string"},"description":"Two to four lines, in the second person, drawn only from what he said"},"tomorrow":{"type":"string","description":"His intention for tomorrow if he voiced one, else empty"}},"required":["title","lines","tomorrow"]}}"""
        override fun execute(p0: String): String = "unused"
    }
    return engine.createConversation(
        ConversationConfig(
            systemInstruction = Contents.of("$line Call sheet with it."),
            tools = listOf(tool(t)), automaticToolCalling = false,
            samplerConfig = talkSampler(seed), maxOutputToken = 400, thinkingConfig = OFF,
        ),
    ).use { c ->
        val started = System.nanoTime()
        val m = runCatching { c.sendMessage(material) }.getOrNull()
        val secs = (System.nanoTime() - started) / 1e9
        val b = runCatching { c.getBenchmarkInfo() }.getOrNull()
        val a = m?.toolCalls?.firstOrNull { it.name == "sheet" }?.arguments
        if (a == null) Called(m?.toString()?.let(::sheetOf), false, m?.toString().orEmpty(), secs, b?.lastPrefillTokenCount ?: 0, b?.lastDecodeTokenCount ?: 0)
        else {
            val title = a["title"]?.toString()?.trim().orEmpty()
            val lines = strings(a["lines"]).take(4)
            val sh = if (title.isEmpty() || lines.isEmpty()) null else Mark.Sheet(title, lines, a["tomorrow"]?.toString()?.trim().orEmpty().removePrefix("Tomorrow:").trim())
            Called(sh, true, a.toString(), secs, b?.lastPrefillTokenCount ?: 0, b?.lastDecodeTokenCount ?: 0)
        }
    }
}

/** Mind.ask: her instruction, his material, her answer written free (Talk's settings, up to 1,024 tokens). */
private fun ask(engine: Engine, instruction: String, message: String, seed: Int): Called<String> =
    engine.createConversation(ConversationConfig(systemInstruction = Contents.of(instruction), samplerConfig = talkSampler(seed), maxOutputToken = 1024, thinkingConfig = OFF)).use { c ->
        val started = System.nanoTime()
        val text = c.sendMessage(message).toString()
        val secs = (System.nanoTime() - started) / 1e9
        val b = runCatching { c.getBenchmarkInfo() }.getOrNull()
        Called(text, true, text, secs, b?.lastPrefillTokenCount ?: 0, b?.lastDecodeTokenCount ?: 0)
    }

private fun sheetNote(sh: Mark.Sheet?): String = sh?.let { "title “${it.title}”; lines " + it.lines.joinToString(" | ") { l -> "“$l”" } + "; tomorrow “${it.tomorrow}”" } ?: "no sheet"

private val FIRST_PERSON = Regex("""\b(i|i'm|i've|i'd|i'll|me|my|myself|mine)\b""")
private val SECOND_PERSON = Regex("""\b(you|you're|you've|you'd|you'll|your|yours|yourself)\b""")
private fun person(sh: Mark.Sheet?): String {
    val text = Telling.plain(sh?.lines?.joinToString(" ").orEmpty().replace('’', '\''))
    val i = FIRST_PERSON.containsMatchIn(text)
    val you = SECOND_PERSON.containsMatchIn(text)
    return when {
        i && !you -> "first person"
        you && !i -> "second person"
        i && you -> "mixed (I and you)"
        else -> "neither (no I or you)"
    }
}

fun main(args: Array<String>) {
    val model = args[0]
    val out = File(args.getOrElse(1) { "trial/sand-0.10.3.md" })
    val seeds = args.getOrNull(2)?.split(",")?.map { it.trim().toInt() } ?: listOf(0)
    val dry = args.contains("dry")
    val opened = at(4, 9, 54)
    val openedMs = opened.toInstant().toEpochMilli()

    // His cook, and the first-person reconstruction of his SHEET edit, each read as the app reads it.
    val shipped = copyOf(PROCEDURES.load(Cook.SHIPPED_COOK) as JsObj)
    val his = hisCook(shipped, firstPerson = false)
    val hisFirst = hisCook(shipped, firstPerson = true)
    val parsedHis = Cook.parse(his)
    val parsedFirst = Cook.parse(hisFirst)

    val r = StringBuilder("# Sundial 0.10.3 on the laptop: the nine tests' recipe parts, on Gemma 4 E4B, with Matthew's lines\n\n")
    r.append("Run ${java.time.Instant.now()}, this machine's CPU (LiteRT-LM 0.18.1, Gemma 4 E4B, a context of 8192). His Talk settings as his log of 4 November gives them: temperature 0.70, top-k 64, top-p 0.95, replies up to 1,024 tokens, thinking off; seeds ${seeds.joinToString()} for A, seed 0 elsewhere (the app's own seed is 0).\n\n")
    r.append("## His cook\n\n")
    r.append("The shipped cook (assets/procedures/cook.json as it stands in the tree) with only the words inside the quotes at 1.1.8.1 (his TALK line, log line 9) and 1.1.8.3.2.1 (his no-day line, log line 61) replaced by his, word for word; written to his pocket as cook:${H}.\n\n")
    r.append("- Cook.parse(his cook).refused: ${parsedHis.refused.ifEmpty { listOf("empty") }.joinToString("; ")}; unreadable: ${parsedHis.unreadable.ifEmpty { emptyList() }.joinToString { it.first }.ifEmpty { "none" }}\n")
    r.append("- His 1.1.8.1 now reads: “${stepAt(his, "1.1.8.1").take(140)}…”\n")
    r.append("- His 1.1.8.3.2.1 now reads: “${stepAt(his, "1.1.8.3.2.1")}”\n")
    r.append("- The day line, 1.1.8.3.1.1, shipped and unchanged: “${stepAt(his, "1.1.8.3.1.1")}”\n")
    r.append("- The day check, 1.1.8.3, shipped: “${(((his["1"] as JsObj)["1"] as JsObj)["8"] as JsObj).let { ((it["3"] as JsObj)["_"] as JsStr).value }}”\n\n")
    r.append("His pocket, set before the run: hatched as ${H}, her name Gemma; today's line in his now (a line Sundial wrote earlier today, at 07:54, so Keep today may write over it): “$TODAY_LINE”; ")
    r.append("on his hand (ahead:${H}), one card in each of three piles: " + HAND.joinToString("; ") { (p, w) -> "${Days.PILES[p - 1]}: “$w”" } + ". The world's cards() (the day check's) returns his hand as his ahead holds it.\n\n")
    out.writeText(r.toString())
    println(r)

    val (storage, pages) = pocket(openedMs, his)
    val loaded = Cook.load(H, storage, PROCEDURES)
    r.append("- Cook.load(his pocket): his own cook ran: ${loaded.own}; refusal: ${loaded.refusal ?: "none"}\n")
    val recipe = Cook.talkRecipe(loaded.parsed)
    r.append("- Her recipe for talk (Cook.talkRecipe, read once into her instruction) is his TALK line word for word: ${recipe == HIS_TALK}\n")
    r.append("- The world's cards(): ${world(pages, storage, opened, emptyList()).cards()}\n\n")
    out.writeText(r.toString())
    check(loaded.own && loaded.refusal == null) { "his cook didn't load as his: ${loaded.refusal}" }

    // A: his one kept conversation, the messages in order, each walked through his cook.
    val aMessages = listOf(
        "1 YOUR DAY" to ("I walked Haven this morning then spent an hour on the membership form." to at(4, 9, 54, 0)),
        "2 STAYING WITH HIM" to ("the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling." to at(4, 9, 54, 40)),
        "3 WHAT'S IN FRONT OF HIM" to ("What have I got in front of me?" to at(4, 9, 55, 20)),
        "4 TOO MUCH" to ("Everything feels too much this morning." to at(4, 9, 55, 50)),
        "6 DAY AND QUESTION TOGETHER (a)" to ("I finished the ID checks today. Do you know what a LERO is?" to at(4, 9, 56, 30)),
        "6 DAY AND QUESTION TOGETHER (b)" to ("it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?" to at(4, 9, 57, 10)),
        "8 TOMORROW (said before keeping)" to ("Tomorrow I want to walk somewhere new" to at(4, 10, 1, 0)),
    )
    val window = HerWindow.window(H, pages, storage, PROCEDURES, opened)
    val opening = HerWindow.opening(window, opened, recipe)
    val handedNever = HerWindow.notHers(H, storage, PROCEDURES, opened)
    r.append("## A. His one kept conversation (tests 1, 2, 3, 4, 6, 8's message, 9)\n\n")
    r.append("Her instruction, as the app opens her one conversation (her window, the time, his TALK line), read once on his first message:\n\n```\n$opening\n```\n\n")
    // Each message's walk, before any model: the routes.
    val dryRoutes = aMessages.mapIndexed { i, (_, m) -> m.first to Cook.Walk(loaded.parsed, world(pages, storage, m.second, aMessages.take(i).map { it.second.first }), m.first).start(loaded.kernel.message ?: "1") }
    for ((said, w) in dryRoutes) println("route: “$said” → ${route(w)}; waiting ${w.waiting}")
    val shortWalks = listOf("The sound. the space.", "stiff but free").map { it to Cook.Walk(loaded.parsed, world(pages, storage, at(4, 9, 54, 40), listOf(aMessages[0].second.first)), it).start(loaded.kernel.message ?: "1") }
    for ((said, w) in shortWalks) println("route: “$said” → ${route(w)}")
    val bWalks = listOf("Tell me about Alfred North Whitehead", "Boole's work on the middle point. Are you familiar?").map { it to Cook.Walk(loaded.parsed, world(pages, storage, at(4, 10, 10), emptyList()), it).start(loaded.kernel.message ?: "1") }
    for ((said, w) in bWalks) println("route: “$said” → ${route(w)}")
    if (dry) { out.writeText(r.toString()); return }

    ExperimentalFlags.enableBenchmark = true
    val engine = Engine(EngineConfig(modelPath = model, backend = Backend.CPU(), maxNumTokens = 8192, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("s").toString()))
    engine.initialize()
    engine.use { e ->
        var moments = emptyList<Keep.Moment>()
        var t1Turn = ""
        for (seed in seeds) {
            r.append("### Seed $seed\n\n")
            val ms = mutableListOf<Keep.Moment>()
            e.createConversation(ConversationConfig(systemInstruction = Contents.of(opening), samplerConfig = talkSampler(seed), maxOutputToken = 1024, thinkingConfig = OFF)).use { c ->
                val before = mutableListOf<String>()
                for ((test, m) in aMessages) {
                    val (said, time) = m
                    val w = Cook.Walk(loaded.parsed, world(pages, storage, time, before.toList()), said).start(loaded.kernel.message ?: "1")
                    val turn = HerWindow.turnKept(said, pages, w.handed, recipe)
                    if (test.startsWith("1 ")) t1Turn = turn
                    val s = say(c, turn, Talking.Shown(said, "", HerWindow.echoLines(turn), false, handedNever))
                    val f = HerReply.free(s.raw, s.shown)
                    ms += Keep.Moment(said, time.toInstant().toEpochMilli(), f.places)
                    before += said
                    r.append("#### Test $test, ${"%02d:%02d:%02d".format(time.hour, time.minute, time.second)}: “$said”\n\n")
                    r.append("- Route: ${route(w)}" + (if (w.waiting != null) "; the walk waits: ${w.waiting}" else "") + "\n")
                    r.append("- Her turn: `${oneLine(turn)}`\n")
                    r.append(replyNote(s, said))
                    r.append("\n<details><summary>His cook, step by step</summary>\n\n${traceBlock(w)}\n\n</details>\n\n")
                    out.writeText(r.toString())
                    println("A seed $seed, $test: ${route(w)} — ${oneLine(s.raw)}")
                }
            }
            if (seed == seeds.first()) moments = ms
        }

        // A, test 2's short answers: each in a conversation of its own, seeded with his first message and her reply as his Pixel had them.
        r.append("### Test 2, his short answers to her question\n\n")
        val phoneT1 = "You walked Haven this morning then spent an hour on the membership form. What did the movement of that walk feel like compared to the stillness of filling out that form?\n\nPlaces: what happened, what he felt"
        r.append("Each in a conversation of its own: her instruction as above, then his first message (the turn above) and her reply to it exactly as his Pixel had it on 4 November, “${oneLine(phoneT1)}”; then his answer, walked through his cook. Seed 0.\n\n")
        for ((said, _) in shortWalks) {
            val w = Cook.Walk(loaded.parsed, world(pages, storage, at(4, 9, 54, 40), listOf(aMessages[0].second.first)), said).start(loaded.kernel.message ?: "1")
            val turn = HerWindow.turnKept(said, pages, w.handed, recipe)
            val s = e.createConversation(ConversationConfig(systemInstruction = Contents.of(opening), initialMessages = listOf(Message.user(t1Turn), Message.model(phoneT1)), samplerConfig = talkSampler(0), maxOutputToken = 1024, thinkingConfig = OFF)).use { c ->
                say(c, turn, Talking.Shown(said, "", HerWindow.echoLines(turn), false, handedNever))
            }
            r.append("#### “$said”\n\n- Route: ${route(w)}\n- Her turn: `${oneLine(turn)}`\n")
            r.append(replyNote(s, said))
            r.append("\n<details><summary>His cook, step by step</summary>\n\n${traceBlock(w)}\n\n</details>\n\n")
            out.writeText(r.toString())
            println("T2 short “$said”: ${route(w)} — ${oneLine(s.raw)}")
        }

        // B: test 5 in a fresh conversation.
        r.append("## B. Test 5, ASKING HER: a fresh conversation\n\n")
        val bNow = at(4, 10, 10)
        val bOpening = HerWindow.opening(HerWindow.window(H, pages, storage, PROCEDURES, bNow), bNow, recipe)
        e.createConversation(ConversationConfig(systemInstruction = Contents.of(bOpening), samplerConfig = talkSampler(0), maxOutputToken = 1024, thinkingConfig = OFF)).use { c ->
            val before = mutableListOf<String>()
            for ((said, _) in bWalks) {
                val w = Cook.Walk(loaded.parsed, world(pages, storage, bNow, before.toList()), said).start(loaded.kernel.message ?: "1")
                val turn = HerWindow.turnKept(said, pages, w.handed, recipe)
                val s = say(c, turn, Talking.Shown(said, "", HerWindow.echoLines(turn), false, handedNever))
                before += said
                r.append("#### “$said”\n\n- Route: ${route(w)}\n- Her turn: `${oneLine(turn)}`\n")
                r.append(replyNote(s, said))
                r.append("\n<details><summary>His cook, step by step</summary>\n\n${traceBlock(w)}\n\n</details>\n\n")
                out.writeText(r.toString())
                println("B “$said”: ${route(w)} — ${oneLine(s.raw)}")
            }
        }

        // C: Keep today, after A (seed ${seeds.first()}'s moments, each with the places her reply gave it).
        val keepNow = at(4, 10, 5)
        r.append("## C. Test 7, KEEP TODAY (after A, seed ${seeds.first()})\n\n")
        r.append("The moments: every message of his in A with its instant, as the app's waiting room holds them (it keeps each message he sends once hatched); each with the places her reply's places line gave it (six counts as none).\n\n")
        for ((i, m) in moments.withIndex()) r.append("${i + 1}. ${isoString(m.atMs).substring(11, 19)} “${m.words}” — ${if (m.places.isEmpty()) "unplaced" else Mark.placesWords(m.places)}\n")
        r.append("\n")
        out.writeText(r.toString())

        fun keepWalk(label: String, cookParsed: Cook.Loaded, placedBefore: Map<Long, List<Int>>?): Pair<Cook.Walk, Map<Long, List<Int>>?> {
            val (parsed, place) = cookParsed.walkFor(Cook.KEEP) ?: (cookParsed.parsed to "6.1")
            val w = Cook.Walk(parsed, world(pages, storage, keepNow, emptyList(), moments), "").start(place)
            var placed = placedBefore
            while (true) {
                when (val wait = w.waiting) {
                    is Cook.Wait.HerMany -> {
                        if (placedBefore != null) { w.herEach(placedBefore); continue }
                        val unplaced = wait.unplaced
                        r.append("### C(i) her places for each moment ($label)\n\n")
                        r.append("The walk waits for her places (cook 6.2.1): ${wait.moments.size} moments, ${unplaced.size} unplaced. Her question: “${wait.question}”.\n\n")
                        if (unplaced.isEmpty()) { r.append("Every moment placed already: nothing asked.\n\n"); w.herEach(emptyMap()); placed = emptyMap(); continue }
                        // Free, once, as the app asks it (Asking.serveHer, 0.10.3).
                        val instruction = Placing.instruction(wait.question, wait.places)
                        val material = Placing.material(unplaced)
                        val free = ask(e, instruction, material, 0)
                        val read = Placing.read(free.text, unplaced)
                        r.append("**Free text, once for all ${unplaced.size} (Placing.instruction / material / read):** ${f1(free.secs)} s, read ${free.read} tokens, wrote ${free.wrote}.\n\n")
                        r.append("- Her instruction: “$instruction”\n- His moments as handed:\n\n```\n$material\n```\n\n- She wrote:\n\n```\n${free.text.trim()}\n```\n\n")
                        r.append("- Read by Placing.read: " + unplaced.joinToString("; ") { m -> "“${m.words.take(40)}…” → ${read[m.atMs]?.let { Mark.placesWords(it) } ?: "unplaced"}" } + "\n\n")
                        out.writeText(r.toString())
                        println("C places free: ${f1(free.secs)} s, ${read.size} placed")
                        // The tool call, one call per unplaced moment (Gemma.chooseMany's call, its instruction as the app built it for a ring).
                        val ring = wait.places.mapIndexed { i, p -> "${i + 1} $p" }.joinToString("\n")
                        val toolInstruction = "Name every place of this ring his words plainly touch, one or more, never ranked: ${wait.question}.\n$ring"
                        var toolSecs = 0.0
                        r.append("**Tool call “places”, one call per unplaced moment (Gemma.chooseMany, its instruction as Asking builds it for a ring):**\n\n")
                        for (m in unplaced) {
                            val t = chooseMany(e, toolInstruction, m.words, wait.places.size, 0)
                            toolSecs += t.secs
                            r.append("- “${m.words.take(60)}” → ${t.got?.let { Mark.placesWords(it) + (if (it.size >= wait.places.size) " (all six: counted as none)" else "") } ?: "nothing"}; ${if (t.whole) "a whole tool call ${t.text}" else "no whole tool call; she wrote “${oneLine(t.text).take(120)}”"}; ${f1(t.secs)} s, read ${t.read}, wrote ${t.wrote}\n")
                            out.writeText(r.toString())
                        }
                        r.append("- All ${unplaced.size} by tool: ${f1(toolSecs)} s in all\n\n")
                        // And once for the day as a whole, as 0.10.2 asked it (on his Pixel, 10.4 s).
                        val all = chooseMany(e, toolInstruction, wait.about, wait.places.size, 0)
                        r.append("**Tool call “places” once for the whole day (0.10.2's ask, his words all together):** ${all.got?.let { Mark.placesWords(it) } ?: "nothing"}; ${if (all.whole) "a whole tool call" else "no whole tool call"}; ${f1(all.secs)} s, read ${all.read}, wrote ${all.wrote}\n\n")
                        r.append("**Side by side:** free text once ${f1(free.secs)} s (${read.size} of ${unplaced.size} placed) · tool per moment ${f1(toolSecs)} s (${unplaced.size} calls) · tool once for the day ${f1(all.secs)} s (one set of places for every moment)\n\n")
                        out.writeText(r.toString())
                        println("C places tool: ${f1(toolSecs)} s per moment, ${f1(all.secs)} s once")
                        placed = read
                        w.herEach(read)
                    }
                    is Cook.Wait.HerSheet -> {
                        r.append("### C(ii) her sheet ($label)\n\n")
                        r.append("- Her SHEET line, as his cook hands it: “${wait.line}”\n- Her material:\n\n```\n${wait.material}\n```\n\n")
                        val tool = sheetByTool(e, wait.line, wait.material, 0)
                        r.append("**Tool call “sheet” (Gemma.sheet):** ${if (tool.whole) "a whole tool call" else "no whole tool call, read as plain lines"}; ${f1(tool.secs)} s, read ${tool.read}, wrote ${tool.wrote}\n\n- Raw: `${oneLine(tool.text)}`\n- Sheet: ${sheetNote(tool.got)}\n- Person: ${person(tool.got)}\n\n")
                        out.writeText(r.toString())
                        println("C sheet tool ($label): ${f1(tool.secs)} s — ${sheetNote(tool.got)}")
                        val freeLine = "${wait.line} Write it in plain lines: the title first, then two to four lines, then “Tomorrow: …” if he voiced one, and nothing else."
                        val free = ask(e, freeLine, wait.material, 0)
                        val freeSheet = sheetOf(free.text)
                        r.append("**Free text (Mind.sheet, read by Mind.sheetOf's rules):** ${f1(free.secs)} s, read ${free.read}, wrote ${free.wrote}\n\n- She wrote:\n\n```\n${free.text.trim()}\n```\n\n- Sheet: ${sheetNote(freeSheet)}\n- Person: ${person(freeSheet)}\n\n")
                        r.append("**Side by side:** tool ${f1(tool.secs)} s (${tool.wrote} written) · free ${f1(free.secs)} s (${free.wrote} written)\n\n")
                        out.writeText(r.toString())
                        println("C sheet free ($label): ${f1(free.secs)} s — ${sheetNote(freeSheet)}")
                        // As the app on E4B: the tool's sheet, else her free one.
                        w.sheet(tool.got ?: freeSheet)
                    }
                    else -> break
                }
            }
            return w to placed
        }

        fun cardNote(w: Cook.Walk): String {
            val card = ((w.waiting as? Cook.Wait.Him)?.card as? Cook.Card.Day)?.landing ?: return "No keep card set out; the walk: ${w.waiting}\n\n${traceBlock(w)}\n\n"
            return buildString {
                appendLine("**The keep card set out, for his yes:** ${card.describe()}")
                appendLine()
                appendLine("- The day's line: “${card.text?.let(::oneLine) ?: "none"}”" + (card.his?.let { " (a line of his own stands: “$it”, left as it is)" } ?: ""))
                appendLine("- The day's places: ${Mark.placesWords(card.places).ifEmpty { "none" }}")
                for (m in card.moments) appendLine("  - ${isoString(m.atMs).substring(11, 16)} “${m.words}” — ${Mark.placesWords(m.places).ifEmpty { "unplaced" }}")
                for (l in card.lines) appendLine("- ${if (l.ticked) "[x]" else "[ ]"} ${Days.name(l.where)}: “${l.words}”" + (if (l.already) " (already on his hand)" else ""))
                appendLine()
                appendLine("<details><summary>Keep today, step by step</summary>")
                appendLine()
                appendLine(traceBlock(w))
                appendLine()
                appendLine("</details>")
                appendLine()
            }
        }

        val (k1, placed) = keepWalk("his cook, the shipped SHEET line", loaded, null)
        r.append(cardNote(k1))
        out.writeText(r.toString())

        // The first-person reconstruction of his SHEET edit: only words inside 6.1.5's quotes changed.
        r.append("### C(iii) his SHEET edit, reconstructed in the first person\n\n")
        r.append("His exact edited 6.1.5 is not to hand, so a reconstruction changing only words inside its quotes: “$SHEET_SECOND” → “$SHEET_FIRST”.\n\n")
        r.append("- Cook.parse(the reconstruction).refused: ${parsedFirst.refused.ifEmpty { listOf("empty") }.joinToString("; ")}\n")
        check(pages.carryOut(pages.planWhole(Cook.cookOf(H), hisFirst)).ok)
        val loadedFirst = Cook.load(H, storage, PROCEDURES)
        r.append("- Cook.load with it as his cook: his own cook ran: ${loadedFirst.own}; refusal: ${loadedFirst.refusal ?: "none"}\n")
        r.append("- 6.1.5 as it now reads: “${stepAt(hisFirst, "6.1.5").take(600)}…”\n\n")
        out.writeText(r.toString())
        val (k2, _) = keepWalk("his first-person SHEET edit", loadedFirst, placed ?: emptyMap())
        r.append(cardNote(k2))
        // His yes on this card: the day written, so the next morning finds it.
        val dayCard = ((k2.waiting as? Cook.Wait.Him)?.card as? Cook.Card.Day)?.landing
        if (dayCard != null) {
            k2.yes()
            r.append("His yes: the walk went on: ${k2.trace.takeLast(2).joinToString(" · ")}\n\n")
            r.append("His now at 4 November after the keep: “${Keep.dayText(storage.read(Days.NOW)?.let(::parseJson), Keep.dayOf(keepNow.toInstant().toEpochMilli()))?.let(::oneLine)}”\n\n")
        }
        out.writeText(r.toString())

        // D: the next morning, the dial opened before he speaks.
        val morning = at(5, 8, 40)
        r.append("## D. Test 8, TOMORROW: the next morning, the dial opened before he speaks (GREET, cook 6.3)\n\n")
        val gw = world(pages, storage, morning, emptyList())
        val gl = Cook.load(H, storage, PROCEDURES)
        val (gp, gplace) = gl.walkFor(Cook.GREET)!!
        val g = Cook.Walk(gp, gw, "").start(gplace)
        val tomorrow = Keep.tomorrowOf(Keep.lastDay(gw.kept(), gw.keepDay))
        val gWindow = HerWindow.window(H, pages, storage, PROCEDURES, morning, HerWindow.GREETING_LEAVES_OUT)
        val gTurn = HerWindow.turnHanded("", morning, pages, g.handed)
        val gHanded = HerWindow.notHers(H, storage, PROCEDURES, morning) + g.handed.flatMap { listOf(it.line) + it.material }
        val gs = e.createConversation(ConversationConfig(systemInstruction = Contents.of(gWindow), samplerConfig = talkSampler(0), maxOutputToken = 1024, thinkingConfig = OFF)).use { c -> say(c, gTurn, null) }
        val question = Talking.oneQuestion(gs.raw, HerWindow.echoLines(gTurn).filter { it.startsWith("It is ") }, gHanded)
        val words = Telling.plain(question.orEmpty()).split(' ')
        r.append("- The tomorrow line read (Keep.tomorrowOf, his last day kept): ${tomorrow?.let { "“$it”" } ?: "none"}\n")
        r.append("- Her turn: `${oneLine(gTurn)}`\n")
        r.append("- She wrote: “${oneLine(gs.raw)}”\n")
        r.append("- **Shown:** above, the app's line: ${tomorrow?.let { "“$it”" } ?: "(none)"}; beneath it, her one question: ${question?.let { "“$it”" } ?: "nothing of hers: Sundial's own question stands"}\n")
        r.append("- As you: ${words.any { it in setOf("you", "your", "you're", "you've") }}; he/him/his: ${words.any { it in setOf("he", "him", "his") }}; “Kept.”: ${"Kept." in gs.raw}; quote marks in what is shown: ${question?.any { it in "\"“”" } ?: false}\n")
        r.append("- Read ${gs.read} tokens, wrote ${gs.wrote}; laptop whole ${f1(gs.whole)} s; at his Pixel's rates about ${f1(phoneWhole(gs.read, gs.wrote))} s\n\n")
        r.append("<details><summary>GREET, step by step</summary>\n\n${traceBlock(g)}\n\n</details>\n\n")
        out.writeText(r.toString())
        println("D: tomorrow “$tomorrow”; question “$question”")
    }
    out.writeText(r.toString())
}

private fun SAND_SHELF() = com.phenomemental.pocketbeach.bsp.Shelf(com.phenomemental.pocketbeach.bsp.DirShelfSource(File(System.getProperty("pocket.shelf") ?: "shelf")))
