@file:OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)

package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
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
import com.phenomemental.pocketbeach.bsp.DirShelfSource
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Pages
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Shelf
import com.phenomemental.pocketbeach.bsp.WalkTool
import com.phenomemental.pocketbeach.bsp.Walking
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.bsp.stringify
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * How best to support her to read and walk his blocks (23 November): first,
 * why on his phone (log of 19:21 to 19:59, Talk at 16,384) she was offered
 * the walker for strata on six turns and walked none, even at "Now read
 * strata 3.4": his conversation rebuilt as his phone held it, beside the same
 * turn fresh, with strata read in and without, at 8,192 and 16,384. Then four
 * maps of a block, each with the walker offered, on questions needing a part
 * below the opening lines: (a) as now; (b) two levels, each part's own parts
 * by their first words and addresses; (c) (b) and the walker saying what the
 * lines are; (d) (c) and a worked example of a walk. Each turn runs as
 * Sundial 0.13.4 runs a walker turn: streamed, her calls walked by
 * Walking.walk and handed back as readable text.
 *
 *   gradlew :trial:run -Ptrial -PmainClass=com.phenomemental.pocketbeach.trial.WalkMapsKt --args="<E4B.litertlm> <blocks dir> <log> <out.md> [phone|maps|all] [seeds] [contexts]"
 */

private val LONDON_W = ZoneId.of("Europe/London")
private fun onThe23rd(h: Int, m: Int) = ZonedDateTime.of(2026, 11, 23, h, m, 0, 0, LONDON_W)
private fun gw(x: Double) = "%.1f".format(x)
private fun oneLine(s: String) = s.trim().replace("\n", " / ")

/** One turn's outcome. */
private class Walked(val reply: String, val calls: List<String>, val first: Double, val whole: Double, val read: Int, val wrote: Int)

/** A turn with the walker offered for [blocks], streamed, as Sundial 0.13.4 runs it; [toolJson] the walker as described. */
private fun walkerTurn(
    e: Engine, pages: Pages, window: String, history: List<Pair<String, String>>, message: String,
    blocks: List<String>, toolJson: String, seed: Int,
): Walked {
    val t = object : OpenApiTool { override fun getToolDescriptionJsonString() = toolJson; override fun execute(p0: String) = "unused" }
    val started = System.nanoTime()
    var first: Double? = null
    val calls = mutableListOf<String>()
    val reply = StringBuilder()
    var read = 0
    var wrote = 0
    e.createConversation(ConversationConfig(
        systemInstruction = Contents.of(window),
        initialMessages = history.flatMap { (asked, said) -> listOf(Message.user(asked), Message.model(said)) },
        tools = listOf(tool(t)), automaticToolCalling = false,
        samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed), maxOutputToken = 700,
        thinkingConfig = ThinkingConfig(enableThinking = false),
    )).use { c ->
        var send: Message = Message.user(Contents.of(message))
        var rounds = 0
        while (true) {
            val got = mutableListOf<com.google.ai.edge.litertlm.ToolCall>()
            kotlinx.coroutines.runBlocking {
                c.sendMessageAsync(send).collect { m ->
                    got += m.toolCalls
                    val p = m.toString()
                    if (p.isNotBlank() && first == null) first = (System.nanoTime() - started) / 1e9
                    reply.append(p)
                }
            }
            runCatching { c.getBenchmarkInfo() }.getOrNull()?.let { b -> read += b.lastPrefillTokenCount; wrote += b.lastDecodeTokenCount }
            if (got.isEmpty() || rounds >= 3) break
            rounds++
            val answers = got.map { tc ->
                val w = Walking.walk(pages, tc.arguments, blocks, H)
                calls += "${tc.arguments["block"]} at ${tc.arguments["spindle"] ?: "(its opening)"}" + (tc.arguments["pscale_attention"]?.let { ", pscale_attention $it" } ?: "") + if (w.found) "" else " (refused: not there)"
                Content.ToolResponse(tc.name, w.text)
            }
            send = Message.tool(Contents.of(answers))
        }
    }
    val whole = (System.nanoTime() - started) / 1e9
    return Walked(reply.toString(), calls, first ?: whole, whole, read, wrote)
}

/** The walker for [block], as Sundial gives it, with [more] added to its description. */
private fun walkerFor(block: String, more: String): String {
    val j = parseJson(WalkTool.forBlocks(listOf(block))) as JsObj
    if (more.isNotEmpty()) j["description"] = str((j["description"] as com.phenomemental.pocketbeach.bsp.JsStr).value + " " + more)
    return stringify(j)!!
}

private const val SAY_SUMMARIES = "The lines beneath each part are only their first words: a part with more beneath it holds the detail, and walking to its address gives it whole."
private const val SAY_EXAMPLE = "For example, to read what stands at 2.1 of a block named garden, call it with block \"garden\" and spindle \"2.1\"."

/** The first [n] words of [text]. */
private fun firstWords(text: String?, n: Int = 8): String {
    val w = (text ?: "").replace("\n", " ").split(' ').filter { it.isNotEmpty() }
    return if (w.size <= n) w.joinToString(" ") else w.take(n).joinToString(" ") + "…"
}

/** Map (b): the block's opening line, each part's opening line, and beneath each part its own parts by their first words and addresses, as the beach's bsp lists "beneath". */
private fun twoLevels(pages: Pages, name: String): String = buildString {
    val top = pages.position(Region.POCKET, name) ?: return@buildString
    append(top.text ?: "(no opening line)")
    for (l in top.lines) {
        append("\n  ${l.address}  ${l.text ?: "(nothing written here)"}")
        val under = pages.position(Region.POCKET, name, l.address)?.lines.orEmpty()
        for (c in under) append("\n      ${c.address}  ${firstWords(c.text)}" + if (c.beneath > 0) "  (${c.beneath} beneath)" else "")
    }
}

/** His turn with a map of [name] beside his words, as the app hands a block it read: [map] "a" as now, else two levels. */
private fun withMap(said: String, pages: Pages, name: String, map: String): String =
    if (map == "a") Chats.withFiles(said, listOf(Chats.readByApp(pages, Region.POCKET, name)!!))
    else said.trim() + "\n\n(His block $name, which his words named, read by the app: its opening line, each part's opening line, and beneath each part the first words of its own parts, with their addresses:)\n" + twoLevels(pages, name)

/** Her logged replies, in order, from his phone's log. */
private fun loggedReplies(log: File): List<String> =
    Regex("""(?ms)^\d\d:\d\d Matthew: .*?\nCompanion: (.*?)\n  The time she was given:""").findAll(log.readText()).map { it.groupValues[1].trim() }.toList()

/** A question needing a part below the opening lines: the block, his words, the part it needs, and words that show her answer came from it. */
private class Question(val key: String, val block: String, val said: String, val part: String, val shows: List<String>)

private val QUESTIONS = listOf(
    Question("S1", "strata", "In strata, when many minds share one shell, what gets added and never edited, and why?", "3.4", listOf("append", "unbounded")),
    Question("S2", "strata", "Read strata 3.4 and tell me what it says.", "3.4", listOf("append", "unbounded")),
    Question("S3", "strata", "Now read strata 3.4 and you'll get a better answer", "3.4", listOf("append", "unbounded")),
    Question("P5", "passport:Phenomemental", "what does my passport tell you about how to work with me", "5", listOf("plain words", "slot number", "place it", "doubt", "never becoming me", "stay yourself")),
    Question("P6", "passport:Phenomemental", "In my passport, what does PUBLIC mean, and what must you check with me before anything personal goes public?", "6", listOf("anyone on the beach", "encrypted", "health, family", "check with me")),
    Question("W12", "wholeless", "In my wholeless block, how did the day of my exams go after that night?", "1.2", listOf("without a hitch", "wrote steadily", "self doubt", "self-doubt", "self consciousness")),
    Question("W2", "wholeless", "According to my wholeless block, how should the meaning seed be handed to a model?", "2.2", listOf("exactly as it stands", "nothing reworded", "preamble")),
)

fun main(args: Array<String>) {
    val model = args[0]
    val dir = File(args[1])
    val log = File(args[2])
    val out = File(args.getOrElse(3) { "trial/walk-maps.md" })
    val what = args.getOrElse(4) { "all" }
    val seeds = args.getOrNull(5)?.split(",")?.map { it.trim().toInt() } ?: listOf(0, 2)
    val contexts = args.getOrNull(6)?.split(",")?.map { it.trim().toInt() } ?: listOf(8192, 16384)
    val r = StringBuilder("# Reading and walking his blocks (raw)\n\nRun ${java.time.Instant.now()}, this machine's CPU, Gemma 4 E4B, his phone's Talk settings (temperature 1.00, top-k 64, top-p 0.95), thinking off, replies up to 700 tokens; seeds ${seeds.joinToString()}.\n\n")

    // His pocket for the trial: strata as the app's shelf has it (as she quoted it on his phone), his passport and wholeless shortened.
    val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "shelf")))
    val (_, pages) = Bench.fixture(shelf, onThe23rd(19, 0).toInstant().toEpochMilli())
    for ((name, file) in listOf("strata" to "strata.json", "passport:Phenomemental" to "passport.json", "wholeless" to "wholeless.json")) {
        val b = parseJson(File(dir, file).readText()) as JsObj
        check(pages.carryOut(if (pages.exists(name)) pages.planWhole(name, b) else pages.planKeep(name, b)).ok) { "$name couldn't be kept" }
    }
    val replies = loggedReplies(log)
    check(replies.size == 6) { "expected his log's six replies, found ${replies.size}" }
    val file = Chats.Attached("02-bsp-walker.md", File(dir, "02-bsp-walker.md").readText(), block = false)
    out.writeText(r.toString())

    ExperimentalFlags.enableBenchmark = true
    val summary = StringBuilder()
    for (context in contexts.takeIf { what != "maps" } ?: emptyList()) {
        val engine = Engine(EngineConfig(modelPath = model, backend = Backend.CPU(), maxNumTokens = context, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("wm").toString()))
        engine.initialize()
        engine.use { e ->
            r.append("## Part 1, his phone's turns, at a context of $context\n\n")
            // His turns of 19:21 to 19:59, as Sundial 0.13.3 sent them: the file with the first; strata read in, its disc, with each naming it.
            val times = listOf(onThe23rd(19, 21), onThe23rd(19, 32), onThe23rd(19, 33), onThe23rd(19, 36), onThe23rd(19, 42), onThe23rd(19, 59))
            val his = listOf(
                "Read the attached. Learn it.",
                "I'm going to give you a block called strata. Read part 3 only and tell me what it says.",
                "What does strata say about how blocks work together?",
                "What does strata say about people sharing an imagination?",
                "In strata, when many minds share one shell, what gets added and never edited, and why?",
                "Now read strata 3.4 and you'll get a better answer",
            )
            val window = HerWindow.released(times[0])
            fun turn(i: Int, read: Boolean): String {
                val files = if (i == 0) listOf(file) else if (read) listOf(Chats.readByApp(pages, Region.POCKET, "strata")!!) else emptyList()
                return HerWindow.releasedTurn(Chats.withFiles(his[i], files), times[i], if (i == 0) null else times[i - 1].toInstant().toEpochMilli())
            }
            val tool = WalkTool.forBlocks(listOf("strata"))
            val arms = listOf(
                Triple("LONG: his conversation as his phone held it at 19:59 (his five turns, strata read in each, her logged replies), then his 19:59 turn", (0..4).map { turn(it, true) to replies[it] }, turn(5, true)),
                Triple("FRESH: his 19:59 turn alone, strata read in beside it", emptyList(), turn(5, true)),
                Triple("FRESH: his 19:59 turn alone, nothing read in", emptyList(), turn(5, false)),
                Triple("FRESH: his 19:42 turn alone, strata read in beside it", emptyList(), turn(4, true)),
                Triple("FRESH: his 19:42 turn alone, nothing read in", emptyList(), turn(4, false)),
            )
            for ((name, history, message) in arms) for (seed in seeds) {
                val w = walkerTurn(e, pages, window, history, message, listOf("strata"), tool, seed)
                val right = listOf("append", "unbounded").any { w.reply.contains(it, ignoreCase = true) }
                r.append("### $name, seed $seed\n\n- Walks: ${w.calls.ifEmpty { listOf("none") }.joinToString("; ")}\n- Her answer from 3.4: ${if (right) "yes" else "no"}\n- Read ${w.read} tokens, wrote ${w.wrote}; first words ${gw(w.first)} s, whole ${gw(w.whole)} s\n- She wrote: “${oneLine(w.reply).take(1200)}”\n\n")
                summary.append("| $context | ${name.substringBefore(":")}: ${name.substringAfter(": ").take(60)} | $seed | ${w.calls.size} | ${w.calls.joinToString("; ").ifEmpty { "–" }} | ${if (right) "yes" else "no"} | ${w.read} | ${gw(w.whole)} |\n")
                out.writeText(r.toString())
                println("part 1, $context, ${name.take(40)}, seed $seed: walks ${w.calls}; 3.4 ${if (right) "yes" else "no"}; read ${w.read}")
            }
        }
    }
    if (summary.isNotEmpty()) r.append("## Part 1 in one table\n\n| Context | Arm | Seed | Walks | To | Answer from 3.4 | Read | s |\n|---:|---|---:|---:|---|---|---:|---:|\n").append(summary).append("\n")
    out.writeText(r.toString())

    if (what == "phone") return
    // Part 2: the four maps, at his phone's context, 16,384.
    val maps = listOf("a" to "", "b" to "", "c" to SAY_SUMMARIES, "d" to "$SAY_SUMMARIES $SAY_EXAMPLE")
    val table = StringBuilder()
    val engine = Engine(EngineConfig(modelPath = model, backend = Backend.CPU(), maxNumTokens = 16384, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("wm").toString()))
    engine.initialize()
    engine.use { e ->
        r.append("## Part 2, the four maps, at a context of 16,384\n\n")
        for ((m, more) in maps) {
            val example = QUESTIONS.first()
            r.append("### Map ($m)\n\nThe walker as described: `${walkerFor(example.block, more)}`\n\nThe map of strata, as handed:\n\n```\n${withMap("…", pages, "strata", m)}\n```\n\n")
            for (q in QUESTIONS) for (seed in seeds) {
                val now = onThe23rd(20, 30)
                val message = HerWindow.releasedTurn(withMap(q.said, pages, q.block, m), now, null)
                val w = walkerTurn(e, pages, HerWindow.released(now), emptyList(), message, listOf(q.block), walkerFor(q.block, more), seed)
                val toPart = w.calls.any { it.startsWith("${q.block} at ${q.part}") && !it.contains("refused") }
                val right = q.shows.any { w.reply.contains(it, ignoreCase = true) }
                r.append("#### (${m}) ${q.key} “${q.said}” (needs ${q.part}), seed $seed\n\n- Walks: ${w.calls.ifEmpty { listOf("none") }.joinToString("; ")}\n- Her answer shows the part: ${if (right) "yes" else "no"}\n- Read ${w.read} tokens, wrote ${w.wrote}; first words ${gw(w.first)} s, whole ${gw(w.whole)} s\n- She wrote: “${oneLine(w.reply).take(900)}”\n\n")
                table.append("| ($m) | ${q.key} | ${q.part} | $seed | ${w.calls.size} | ${w.calls.joinToString("; ").ifEmpty { "–" }} | ${if (toPart) "yes" else "no"} | ${if (right) "yes" else "no"} | ${w.read} | ${gw(w.first)} | ${gw(w.whole)} |\n")
                out.writeText(r.toString())
                println("part 2, map $m, ${q.key}, seed $seed: walks ${w.calls}; to ${q.part} $toPart; shows $right; read ${w.read}; ${gw(w.whole)} s")
            }
        }
    }
    r.append("## Part 2 in one table\n\n| Map | Question | Needs | Seed | Walks | To | To the part | Answer shows it | Read | First s | Whole s |\n|---|---|---|---:|---:|---|---|---|---:|---:|---:|\n").append(table)
    out.writeText(r.toString())
}
