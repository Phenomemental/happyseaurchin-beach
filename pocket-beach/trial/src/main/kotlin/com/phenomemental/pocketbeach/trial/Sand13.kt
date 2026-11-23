@file:OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)

package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
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
import com.phenomemental.pocketbeach.bsp.Chats
import com.phenomemental.pocketbeach.bsp.Cook
import com.phenomemental.pocketbeach.bsp.Days
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Keep
import com.phenomemental.pocketbeach.bsp.Landing
import com.phenomemental.pocketbeach.bsp.Mark
import com.phenomemental.pocketbeach.bsp.MemoryStorage
import com.phenomemental.pocketbeach.bsp.Pages
import com.phenomemental.pocketbeach.bsp.Placing
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.WalkTool
import com.phenomemental.pocketbeach.bsp.Walking
import com.phenomemental.pocketbeach.bsp.isoString
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.str
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sundial 0.13 on the laptop's Gemma 4 E4B (spine:pocket-beach
 * 1.1111787199996): the line in the sand on 0.13's own code, and section 9's
 * walk turns beside turns naming none. Every turn goes the way the app sends
 * it: Walking.named reads his words against the blocks of his pocket; a turn
 * naming none is her kept conversation, streamed, as 0.12; a turn naming one
 * runs as Gemma.sayWithTool runs it, in a conversation of its own with every
 * exchange so far and the walker as one native tool named for the blocks his
 * words named (WalkTool.forBlocks), thinking off, her calls walked by
 * Walking.walk (the guard first) and handed back as
 * readable text. Her kept conversation is let go and opens again with that
 * turn in it, as the app's Talker does. The walk turns run twice: on 0.13's
 * path, and with no tool, as 0.12 sends them.
 *
 *   gradlew :trial:run -Ptrial -PmainClass=com.phenomemental.pocketbeach.trial.Sand13Kt --args="<E4B.litertlm> <out.md> [seeds] [walks|sand|all]"
 */

private val LONDON13 = ZoneId.of("Europe/London")
private fun at13(d: Int, h: Int, m: Int, s: Int = 0) = ZonedDateTime.of(2026, 11, d, h, m, s, 0, LONDON13)

/** His Talk settings as his log of 4 November gives them, as every earlier line in the sand ran. */
private fun sampler13(seed: Int) = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.7, seed = seed)
private val OFF13 = ThinkingConfig(enableThinking = false)
private fun g13(x: Double) = "%.1f".format(x)
private fun line13(s: String) = s.trim().replace("\n", " / ")

/** His Pixel's rates of 4 November (an estimate only his phone can confirm). */
private fun pixelFirst13(read: Int) = 2.0 + 0.306 + read * 0.002816
private fun pixelWhole13(read: Int, wrote: Int) = 0.2 + 0.306 + read * 0.002816 + wrote / 7.9

/** One of her calls as it arrived, and what the app handed her back. */
private class Call13(val arguments: Map<String, Any?>, val walked: Walking.Walked) {
    val spindleArrived: String get() = when (val s = arguments["spindle"]) {
        null -> "none"
        is String -> "“$s” (text)"
        is Number -> "$s (a number)"
        else -> "$s (${s::class.simpleName})"
    }
}

private class Said13(
    val raw: String, val shown: String, val firstShown: Double, val whole: Double, val read: Int, val wrote: Int, val held: Int?,
    val named: List<String>, val calls: List<Call13>, val rounds: Int, val pixel: Double, val pixelFirst: Double, val offered: Boolean,
)

/**
 * Her talk as the app's Talker holds it: one kept conversation for turns naming
 * no block; a turn naming one of his blocks in a conversation of its own with
 * the walker offered (when [withTool]), the kept one let go and opened again
 * after with that turn in it.
 */
private class Talker13(val engine: Engine, val window: String, val seed: Int, val pages: Pages, val withTool: Boolean, initial: List<Pair<String, String>> = emptyList()) : AutoCloseable {
    private val done = initial.toMutableList()
    private var kept: Conversation? = null

    private fun config(named: List<String>?) = ConversationConfig(
        systemInstruction = Contents.of(window),
        initialMessages = done.flatMap { (asked, said) -> listOf(Message.user(asked), Message.model(said)) },
        tools = if (named != null) listOf(tool(object : OpenApiTool {
            override fun getToolDescriptionJsonString() = WalkTool.forBlocks(named)
            override fun execute(p0: String): String = "unused"
        })) else emptyList(),
        automaticToolCalling = false, samplerConfig = sampler13(seed), maxOutputToken = 1024, thinkingConfig = OFF13,
    )

    fun say(said: String, turn: String, shown: Talking.Shown): Said13 {
        val named = Walking.named(said, pages.shore().mine.map { it.name }, H)
        return if (named.isEmpty() || !withTool) plain(turn, shown, named) else walking(turn, shown, named)
    }

    private fun plain(turn: String, shown: Talking.Shown, named: List<String>): Said13 {
        val c = kept ?: engine.createConversation(config(null)).also { kept = it }
        val started = System.nanoTime()
        var firstShown: Double? = null
        val raw = StringBuilder()
        kotlinx.coroutines.runBlocking {
            c.sendMessageAsync(turn).collect { m ->
                raw.append(m.toString())
                if (firstShown == null && shown.of(raw.toString()).isNotBlank()) firstShown = (System.nanoTime() - started) / 1e9
            }
        }
        val whole = (System.nanoTime() - started) / 1e9
        val b = runCatching { c.getBenchmarkInfo() }.getOrNull()
        done += turn to raw.toString()
        val read = b?.lastPrefillTokenCount ?: 0
        val wrote = b?.lastDecodeTokenCount ?: 0
        firstShown?.let { firsts13 += it }
        return Said13(raw.toString(), shown.of(raw.toString(), over = true), firstShown ?: whole, whole, read, wrote, runCatching { c.getTokenCount() }.getOrNull(), named, emptyList(), 0, pixelWhole13(read, wrote), pixelFirst13(read), false)
    }

    /** As Gemma.sayWithTool: her reply comes whole, after any walks, not streamed. */
    private fun walking(turn: String, shown: Talking.Shown, named: List<String>): Said13 {
        kept?.close()
        kept = null
        val started = System.nanoTime()
        val calls = mutableListOf<Call13>()
        var read = 0
        var wrote = 0
        var pixel = 0.0
        var rounds = 0
        val (reply, held) = engine.createConversation(config(named)).use { c ->
            fun counted() = runCatching { c.getBenchmarkInfo() }.getOrNull()?.let { b ->
                read += b.lastPrefillTokenCount; wrote += b.lastDecodeTokenCount; pixel += pixelWhole13(b.lastPrefillTokenCount, b.lastDecodeTokenCount)
            }
            var m = c.sendMessage(turn)
            counted()
            while (m.toolCalls.isNotEmpty() && rounds < 3) {
                rounds++
                val answers = m.toolCalls.map { tc ->
                    val w = Walking.walk(pages, tc.arguments, named, H)
                    calls += Call13(tc.arguments, w)
                    Content.ToolResponse(tc.name, w.text)
                }
                m = c.sendMessage(Message.tool(Contents.of(answers)))
                counted()
            }
            m.toString() to runCatching { c.getTokenCount() }.getOrNull()
        }
        val whole = (System.nanoTime() - started) / 1e9
        done += turn to reply
        return Said13(reply, shown.of(reply, over = true), whole, whole, read, wrote, held, named, calls, rounds, pixel, pixel, true)
    }

    override fun close() { kept?.close() }
}

private fun note13(s: Said13): String = buildString {
    if (s.named.isNotEmpty() && !s.offered) appendLine("- His words named ${s.named.joinToString()}; no tool, as 0.12 sends it")
    else if (s.named.isNotEmpty()) {
        appendLine("- **His words named:** ${s.named.joinToString()}; " + if (s.rounds == 0 && s.calls.isEmpty()) "the walker offered, not called" else "the walker offered, ${s.calls.size} call${if (s.calls.size == 1) "" else "s"} in ${s.rounds} round${if (s.rounds == 1) "" else "s"}")
        for (c in s.calls) {
            appendLine("  - Her call: block “${c.arguments["block"]}”, spindle ${c.spindleArrived}, pscale_attention ${c.arguments["pscale_attention"] ?: "none"} → ${if (c.walked.found) "walked" else "refused"}; chip “↳ ${c.walked.said}”")
            appendLine("    - Handed her: `${line13(c.walked.text)}`")
        }
    } else appendLine("- His words named none of his blocks: no tool")
    appendLine("- She wrote: “${line13(s.raw)}”")
    appendLine("- **Shown:** “${line13(s.shown)}”")
    if (Regex("""bsp\(|tool_code|```|"spindle"|\{\s*"_"""").containsMatchIn(s.raw)) appendLine("- **A call or JSON written into her words**")
    appendLine("- Read ${s.read} tokens, wrote ${s.wrote}; ${s.held?.let { "the conversation holds $it" } ?: "held ?"}")
    appendLine("- Laptop: first words shown ${g13(s.firstShown)} s, whole ${g13(s.whole)} s. At his Pixel's rates (estimate): first words about ${g13(s.pixelFirst)} s, whole about ${g13(s.pixel)} s")
}

private fun world13(pages: Pages, storage: MemoryStorage, now: ZonedDateTime, moments: List<Keep.Moment> = emptyList()): Cook.World =
    object : Cook.World by BenchWorld(pages, storage, now, true, emptyList()) {
        override fun cards(): Map<String, String> = runCatching { Days.handOf(storage.read(Days.aheadOf(H))?.let(::parseJson) as? JsObj) }.getOrDefault(emptyMap())
        override fun moments(): List<Keep.Moment> = moments
    }

/** A block with a line at the end of [digits] (one digit, then the rest after its point), each place on the way given a line of its own: section 9's decisions block. */
private fun deepTo13(digits: String, last: String): JsObj {
    val root = JsObj.of("_" to str("My decisions, by their places on the spine."))
    var node = root
    digits.forEachIndexed { i, d ->
        val next = if (i == digits.lastIndex) JsObj.of("_" to str(last)) else JsObj.of("_" to str("Decision ${digits.substring(0, i + 1).let { it[0] + (if (it.length > 1) "." + it.substring(1) else "") }}"))
        node["$d"] = next
        node = next
    }
    return root
}

/** His pocket as 9f's: hatched, his cook as shipped, today's line, three cards on his hand, his membership block; and section 9's decisions block. */
private fun pocket13(nowMs: Long): Pair<MemoryStorage, Pages> {
    val shelf = com.phenomemental.pocketbeach.bsp.Shelf(com.phenomemental.pocketbeach.bsp.DirShelfSource(File(System.getProperty("pocket.shelf") ?: "shelf")))
    val (storage, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
    check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
    hatched(storage, pages, nowMs)
    Cook.bringIn(H, pages, storage, PROCEDURES).forEach { check(pages.carryOut(it).ok) }
    val clock = (storage.read(Days.NOW)?.let(::parseJson) as? JsObj) ?: Landing.emptyClock(Days.NOW_OPENING)
    Keep.dayLine(clock, Keep.dayOf(nowMs), "Admin day: the membership form and ID checks", nowMs - 2 * 3_600_000L)
    check(pages.carryOut(pages.planWhole(Days.NOW, clock)).ok)
    val ahead = Days.emptyAhead(H)
    for ((pile, words) in listOf(1 to "I need to reply to Jo", 2 to "I'm going to finish the membership form", 3 to "I need to do the ID checks"))
        (ahead["$pile"] as JsObj)["1"] = JsObj.of("_" to str(words), "3" to str(isoString(nowMs - 86_400_000L)))
    check(pages.carryOut(pages.planWhole(Days.aheadOf(H), ahead)).ok)
    check(pages.carryOut(pages.planKeep("membership", JsObj.of(
        "_" to str("The membership form, step by step."),
        "1" to str("Agree the categories with David"),
        "2" to JsObj.of("_" to str("ID checks"), "1" to str("Finish the remaining checks"), "2" to str("Log each one in the register")),
        "3" to str("Print the agreement for signing"),
    ))).ok)
    check(pages.carryOut(pages.planKeep("decisions", deepTo13("1111178719993", "Keep the chat plain: no card beneath her reply."))).ok)
    return storage to pages
}

private class Got13<T>(val got: T?, val whole: Boolean, val text: String, val secs: Double)

/** Gemma.sheet with the tool on E4B, as the app asks it; else what she wrote read as plain lines. */
private fun sheet13(engine: Engine, line: String, material: String): Got13<Mark.Sheet> {
    val t = object : OpenApiTool {
        override fun getToolDescriptionJsonString() = """{"name":"sheet","description":"The day's sheet.","parameters":{"type":"object","properties":{"title":{"type":"string","description":"A short title, from his own words"},"lines":{"type":"array","items":{"type":"string"},"description":"Two to four lines, in the second person, drawn only from what he said"},"tomorrow":{"type":"string","description":"His intention for tomorrow if he voiced one, else empty"}},"required":["title","lines","tomorrow"]}}"""
        override fun execute(p0: String): String = "unused"
    }
    return engine.createConversation(ConversationConfig(systemInstruction = Contents.of("$line Call sheet with it."), tools = listOf(tool(t)), automaticToolCalling = false, samplerConfig = sampler13(0), maxOutputToken = 400, thinkingConfig = OFF13)).use { c ->
        val started = System.nanoTime()
        val m = runCatching { c.sendMessage(material) }.getOrNull()
        val secs = (System.nanoTime() - started) / 1e9
        val a = m?.toolCalls?.firstOrNull { it.name == "sheet" }?.arguments
        if (a == null) Got13(m?.toString()?.let { com.phenomemental.pocketbeach.model.Mind.sheetOf(it) }, false, m?.toString().orEmpty(), secs)
        else {
            val title = a["title"]?.toString()?.trim().orEmpty()
            val lines = (a["lines"] as? List<*>)?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }.orEmpty().take(4)
            Got13(if (title.isEmpty() || lines.isEmpty()) null else Mark.Sheet(title, lines, a["tomorrow"]?.toString()?.trim().orEmpty().removePrefix("Tomorrow:").trim()), true, a.toString(), secs)
        }
    }
}

private fun ask13(engine: Engine, instruction: String, message: String): Got13<String> =
    engine.createConversation(ConversationConfig(systemInstruction = Contents.of(instruction), samplerConfig = sampler13(0), maxOutputToken = 1024, thinkingConfig = OFF13)).use { c ->
        val started = System.nanoTime()
        val text = c.sendMessage(message).toString()
        Got13(text, true, text, (System.nanoTime() - started) / 1e9)
    }

fun main(args: Array<String>) {
    val model = args[0]
    val out = File(args.getOrElse(1) { "trial/sand-0.13.md" })
    val seeds = args.getOrNull(2)?.split(",")?.map { it.trim().toInt() } ?: listOf(0, 2)
    val what = args.getOrNull(3) ?: "all"
    val opened = at13(4, 9, 54)
    val openedMs = opened.toInstant().toEpochMilli()
    val r = StringBuilder("# Sundial 0.13 on the laptop: the line in the sand on 0.13's own code, and the walk turns, on Gemma 4 E4B (raw)\n\n")
    r.append("Run ${java.time.Instant.now()}, this machine's CPU (LiteRT-LM 0.18.1, Gemma 4 E4B, a context of 8,192). His Talk settings: temperature 0.70, top-k 64, top-p 0.95, replies up to 1,024 tokens, thinking off; seeds ${seeds.joinToString()}.\n\n")
    r.append("Her window is `HerWindow.released`; each turn `HerWindow.releasedTurn`. Each turn's words are read by `Walking.named` against his pocket's blocks; a turn naming one is run with the walker offered, named for those blocks (`WalkTool.forBlocks`), her calls walked by `Walking.walk` and handed back as readable text, as `Gemma.sayWithTool` runs it.\n\n")
    val (storage, pages) = pocket13(openedMs)
    r.append("His pocket's blocks, which his words are read against: ${pages.shore().mine.joinToString { it.name }}\n\n")
    out.writeText(r.toString())

    val a = listOf(
        "1 YOUR DAY" to ("I walked Haven this morning then spent an hour on the membership form." to at13(4, 9, 54, 0)),
        "2 STAYING WITH HIM" to ("the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling." to at13(4, 9, 54, 40)),
        "3 WHAT'S IN FRONT OF HIM" to ("What have I got in front of me?" to at13(4, 9, 55, 20)),
        "4 TOO MUCH" to ("Everything feels too much this morning." to at13(4, 9, 55, 50)),
        "6 DAY AND QUESTION TOGETHER (a)" to ("I finished the ID checks today. Do you know what a LERO is?" to at13(4, 9, 56, 30)),
        "6 DAY AND QUESTION TOGETHER (b)" to ("it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?" to at13(4, 9, 57, 10)),
        "8 TOMORROW (said before keeping)" to ("Tomorrow I want to walk somewhere new" to at13(4, 10, 1, 0)),
    )
    // Section 9's five walk messages, word for word, then 0.13's plain forms and the guard.
    val wNow = at13(4, 11, 0)
    val walks = listOf(
        "W1 HIS BLOCK, WHAT'S LEFT" to "What's left to do on my membership block?",
        "W2 A PART OF IT" to "What does my membership block say about the ID checks?",
        "W3 A SHORT ADDRESS" to "Read me what's at 2.2 in my membership block.",
        "W4 A LONG ADDRESS" to "What did I decide at 1.111178719993 in my decisions block?",
        "W5 THE TEACHING (not his block)" to "What does the whetstone say about selection shapes?",
        "W6 MY PASSPORT" to "What does my passport say about me?",
        "W7 MY SHELL" to "What's in my shell?",
        "W8 A PLACE THAT ISN'T THERE" to "What's at 2.9 in my membership block?",
    )
    fun shownFor(said: String, turn: String) = Talking.Shown(said, "", HerWindow.echoLines(turn), false, emptyList())

    ExperimentalFlags.enableBenchmark = true
    // Sundial 0.13.5: her Talk context, 8,192 unless a trial gives another; and the memory the process holds, from /proc.
    val context = System.getProperty("pocket.context")?.toInt() ?: 8192
    val memoryAtStart = memory13()
    val engine = Engine(EngineConfig(modelPath = model, backend = Backend.CPU(), maxNumTokens = context, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("s13").toString()))
    val loadStarted = System.nanoTime()
    engine.initialize()
    r.append("A context of $context. Engine made in ${g13((System.nanoTime() - loadStarted) / 1e9)} s. Memory: ${memoryAtStart} as the program began; ${memory13()} with the engine made.\n\n")
    firsts13.clear()
    engine.use { e ->
        if (what == "all" || what == "walks") for (seed in seeds) {
            for (withTool in listOf(true, false)) {
                r.append("## Walks, seed $seed, ${if (withTool) "0.13: the walker offered when his words name a block" else "no tool, as 0.12 sends them"}\n\nOne conversation, as section 9 ran them. Her window: `${HerWindow.released(wNow)}`\n\n")
                Talker13(e, HerWindow.released(wNow), seed, pages, withTool).use { t ->
                    var last: Long? = null
                    walks.forEachIndexed { i, (test, said) ->
                        val time = wNow.plusSeconds(40L * i)
                        val turn = HerWindow.releasedTurn(said, time, last)
                        last = time.toInstant().toEpochMilli()
                        val s = t.say(said, turn, shownFor(said, turn))
                        r.append("#### $test: “$said”\n\n" + note13(s) + "\n")
                        out.writeText(r.toString())
                        println("walks seed $seed ${if (withTool) "0.13" else "none"}, $test: named ${s.named}; calls ${s.calls.map { it.walked.said }}; ${line13(s.shown).take(140)} (${g13(s.whole)} s)")
                    }
                }
            }
        }
        if (what == "walks") return@use

        var moments = emptyList<Keep.Moment>()
        var t1 = ""
        for (seed in seeds) {
            r.append("## Line in the sand, seed $seed\n\n### A. His one kept conversation\n\nHer window: `${HerWindow.released(opened)}`\n\n")
            val ms = mutableListOf<Keep.Moment>()
            Talker13(e, HerWindow.released(opened), seed, pages, true).use { t ->
                var last: Long? = null
                for ((test, m) in a) {
                    val (said, time) = m
                    val turn = HerWindow.releasedTurn(Chats.withFiles(said, emptyList()), time, last)
                    if (test.startsWith("1 ")) t1 = turn
                    val s = t.say(said, turn, shownFor(said, turn))
                    ms += Keep.Moment(said, time.toInstant().toEpochMilli())
                    last = time.toInstant().toEpochMilli()
                    r.append("#### Test $test: “$said”\n\n- Her turn: `${line13(turn)}`\n" + note13(s) + "\n")
                    out.writeText(r.toString())
                    println("seed $seed, $test: named ${s.named}; calls ${s.calls.map { it.walked.said }}; ${line13(s.shown).take(140)} (${g13(s.whole)} s)")
                }
            }
            if (seed == seeds.first()) moments = ms

            val phoneT1 = "You walked Haven this morning then spent an hour on the membership form. What did the movement of that walk feel like compared to the stillness of filling out that form?\n\nPlaces: what happened, what he felt"
            r.append("### Test 2, his short answers\n\nEach in a conversation of its own: her window, his first message and her reply to it as his Pixel had it, then his answer.\n\n")
            for (short in listOf("The sound. the space.", "stiff but free")) {
                val time = at13(4, 9, 54, 40)
                val turn = HerWindow.releasedTurn(short, time, at13(4, 9, 54).toInstant().toEpochMilli())
                val s = Talker13(e, HerWindow.released(opened), seed, pages, true, listOf(t1 to phoneT1)).use { t -> t.say(short, turn, shownFor(short, turn)) }
                r.append("#### “$short”\n\n" + note13(s) + "\n")
                out.writeText(r.toString())
                println("seed $seed, 2 short “$short”: named ${s.named}; ${line13(s.shown).take(140)}")
            }

            val bNow = at13(4, 10, 10)
            r.append("### B. Test 5, ASKING HER: a fresh conversation\n\n")
            Talker13(e, HerWindow.released(bNow), seed, pages, true).use { t ->
                var last: Long? = null
                for (said in listOf("Tell me about Alfred North Whitehead", "Boole's work on the middle point. Are you familiar?")) {
                    val turn = HerWindow.releasedTurn(said, bNow, last)
                    val s = t.say(said, turn, shownFor(said, turn))
                    last = bNow.toInstant().toEpochMilli()
                    r.append("#### “$said”\n\n" + note13(s) + "\n")
                    out.writeText(r.toString())
                    println("seed $seed, 5 “$said”: named ${s.named}; ${line13(s.shown).take(140)} (${g13(s.whole)} s)")
                }
            }

            // Test 10: a block of his added with the paperclip, under 0.12.1's reworded header.
            val fNow = at13(4, 10, 20)
            val said = "What do you make of this?"
            val files = listOf(Chats.Attached("membership", pages.outlineText(Region.POCKET, "membership")!!, block = true))
            val turn = HerWindow.releasedTurn(Chats.withFiles(said, files), fNow, null)
            val s = Talker13(e, HerWindow.released(fNow), seed, pages, true).use { t -> t.say(said, turn, shownFor(said, turn)) }
            r.append("### Test 10, a block added with the paperclip\n\n- His message: “$said”, with his block membership added\n- Her turn: `${line13(turn)}`\n" + note13(s) + "\n")
            out.writeText(r.toString())
            println("seed $seed, 10 file: ${line13(s.shown).take(140)} (${g13(s.whole)} s)")
        }

        // C: Keep today after A (the first seed's moments), as 0.12 keeps it.
        val keepNow = at13(4, 10, 5)
        r.append("## C. Test 7, KEEP TODAY (after A, seed ${seeds.first()})\n\n")
        val loaded = Cook.load(H, storage, PROCEDURES)
        val (parsed, place) = loaded.walkFor(Cook.KEEP) ?: (loaded.parsed to "6.1")
        val w = Cook.Walk(parsed, world13(pages, storage, keepNow, moments), "").start(place)
        while (true) {
            when (val wait = w.waiting) {
                is Cook.Wait.HerMany -> {
                    val unplaced = wait.unplaced
                    if (unplaced.isEmpty()) { w.herEach(emptyMap()); continue }
                    val got = ask13(e, Placing.instruction(wait.question, wait.places), Placing.material(unplaced))
                    val read = Placing.read(got.text, unplaced)
                    r.append("**Her places, asked once (free text):** ${g13(got.secs)} s\n\n```\n${got.text.trim()}\n```\n\n")
                    r.append(unplaced.joinToString("\n") { m -> "- “${m.words.take(60)}” → ${read[m.atMs]?.let { Mark.placesWords(it) } ?: "unplaced"}" } + "\n\n")
                    w.herEach(read)
                }
                is Cook.Wait.HerSheet -> {
                    val sh = sheet13(e, wait.line, wait.material)
                    val sheet = sh.got ?: com.phenomemental.pocketbeach.model.Mind.sheetOf(ask13(e, "${wait.line} Write it in plain lines: the title first, then two to four lines, then “Tomorrow: …” if he voiced one, and nothing else.", wait.material).text ?: "")
                    r.append("**Her sheet** (${if (sh.whole) "a whole tool call" else "no whole tool call"}, ${g13(sh.secs)} s): " + (sheet?.let { "title “${it.title}”; lines ${it.lines.joinToString(" | ") { l -> "“$l”" }}; tomorrow “${it.tomorrow}”" } ?: "none") + "\n\n")
                    w.sheet(sheet)
                }
                else -> break
            }
        }
        val card = ((w.waiting as? Cook.Wait.Him)?.card as? Cook.Card.Day)?.landing
        if (card != null) {
            r.append("**The keep card:** ${card.describe()}\n\n- The day's line: “${card.text?.let(::line13) ?: "none"}”\n- The day's places: ${Mark.placesWords(card.places).ifEmpty { "none" }}\n\n")
            w.yes()
        } else r.append("No keep card set out: ${w.waiting}\n\n")
        out.writeText(r.toString())

        // D: the next morning, GREET, as 0.12 greets.
        val morning = at13(5, 8, 40)
        r.append("## D. Test 8, TOMORROW: the next morning (GREET)\n\n")
        val gw = world13(pages, storage, morning)
        val gl = Cook.load(H, storage, PROCEDURES)
        val (gp, gplace) = gl.walkFor(Cook.GREET)!!
        val g = Cook.Walk(gp, gw, "").start(gplace)
        val tomorrow = Keep.tomorrowOf(Keep.lastDay(gw.kept(), gw.keepDay))
        val gTurn = HerWindow.turnHanded("", morning, pages, g.handed)
        val handed = g.handed.flatMap { listOf(it.line) + it.material }
        for (seed in seeds) {
            val gs = e.createConversation(ConversationConfig(systemInstruction = Contents.of(HerWindow.released(morning)), samplerConfig = sampler13(seed), maxOutputToken = 1024, thinkingConfig = OFF13)).use { c ->
                val started = System.nanoTime()
                val text = c.sendMessage(gTurn).toString()
                text to (System.nanoTime() - started) / 1e9
            }
            val question = Talking.oneQuestion(gs.first, HerWindow.echoLines(gTurn).filter { it.startsWith("It is ") }, handed)
            r.append("### Seed $seed\n\n- The tomorrow line read: ${tomorrow?.let { "“$it”" } ?: "none"}\n- Her turn: `${line13(gTurn)}`\n- She wrote: “${line13(gs.first)}”\n")
            r.append("- **Shown:** above, the app's line ${tomorrow?.let { "“$it”" } ?: "(none)"}; beneath it, her one question: ${question?.let { "“$it”" } ?: "nothing of hers: Sundial's own question stands"}\n- Laptop whole ${g13(gs.second)} s\n\n")
            out.writeText(r.toString())
            println("D seed $seed: tomorrow “$tomorrow”; question “$question”")
        }
    }
    val sorted = firsts13.sorted()
    if (sorted.isNotEmpty()) r.append("## Time to first word and memory\n\nA context of $context. Her first words shown, laptop, ${sorted.size} streamed replies: median ${g13(sorted[sorted.size / 2])} s, longest ${g13(sorted.last())} s. Memory at the end: ${memory13()}.\n")
    out.writeText(r.toString())
}

/** Sundial 0.13.5: every streamed reply's first words shown, in seconds, for the run's summary. */
private val firsts13 = mutableListOf<Double>()

/** The process's memory now and at its height, from /proc/self/status. */
private fun memory13(): String = runCatching {
    val st = java.io.File("/proc/self/status").readLines()
    fun kb(k: String) = st.firstOrNull { it.startsWith("$k:") }?.substringAfter(":")?.trim()?.removeSuffix(" kB")?.trim()?.toLong()?.div(1024)
    "resident ${kb("VmRSS")} MB, at most ${kb("VmHWM")} MB"
}.getOrDefault("unknown")
