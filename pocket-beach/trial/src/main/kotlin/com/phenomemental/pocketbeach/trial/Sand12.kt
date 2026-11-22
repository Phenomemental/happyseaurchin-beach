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
import com.phenomemental.pocketbeach.bsp.isoString
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.str
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sundial 0.12 on the laptop's Gemma 4 E4B: the line in the sand with the
 * build's own code (spine:pocket-beach 1.111178719997). Her window is
 * HerWindow.released, the time alone; each turn HerWindow.releasedTurn, the
 * time and his words, with any file added read in by Chats.withFiles; what
 * she shows checked by Talking.Shown with only the app's own lines. No line
 * of his cook is handed her. Keep today and the next morning's greeting run
 * as 0.12 keeps them. A test 10 is added: a block of his added with the
 * paperclip.
 *
 *   gradlew :trial:run -Ptrial -PmainClass=com.phenomemental.pocketbeach.trial.Sand12Kt --args="<E4B.litertlm> <out.md> [seeds]"
 */

private val LONDON12 = ZoneId.of("Europe/London")
private fun at12(d: Int, h: Int, m: Int, s: Int = 0) = ZonedDateTime.of(2026, 11, d, h, m, s, 0, LONDON12)

/** His Talk settings as his log of 4 November gives them, as every earlier line in the sand ran: temperature 0.70, top-k 64, top-p 0.95. */
private fun sampler12(seed: Int) = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.7, seed = seed)
private val OFF12 = ThinkingConfig(enableThinking = false)
private fun g1(x: Double) = "%.1f".format(x)
private fun line12(s: String) = s.trim().replace("\n", " / ")

/** His Pixel's rates of 4 November (an estimate only his phone can confirm). */
private fun pixelFirst(read: Int) = 2.0 + 0.306 + read * 0.002816
private fun pixelWhole(read: Int, wrote: Int) = 0.2 + 0.306 + read * 0.002816 + wrote / 7.9

private class Said12(val raw: String, val shown: String, val first: Double, val firstShown: Double, val whole: Double, val read: Int, val wrote: Int, val held: Int?)

/** One turn, streamed as the app streams it: what she wrote, and what the app shows of it. */
private fun turn12(c: Conversation, turn: String, shown: Talking.Shown?): Said12 {
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
    return Said12(raw.toString(), out, first ?: whole, firstShown ?: whole, whole, b?.lastPrefillTokenCount ?: 0, b?.lastDecodeTokenCount ?: 0, runCatching { c.getTokenCount() }.getOrNull())
}

private fun note12(s: Said12): String = buildString {
    appendLine("- She wrote: “${line12(s.raw)}”")
    appendLine("- **Shown:** “${line12(s.shown)}”")
    appendLine("- Read ${s.read} tokens, wrote ${s.wrote}; her conversation holds ${s.held ?: "?"}")
    appendLine("- Laptop: first words shown ${g1(s.firstShown)} s, whole ${g1(s.whole)} s. At his Pixel's rates (estimate): first words about ${g1(pixelFirst(s.read))} s, whole about ${g1(pixelWhole(s.read, s.wrote))} s")
}

private fun world12(pages: Pages, storage: MemoryStorage, now: ZonedDateTime, moments: List<Keep.Moment> = emptyList()): Cook.World =
    object : Cook.World by BenchWorld(pages, storage, now, true, emptyList()) {
        override fun cards(): Map<String, String> = runCatching { Days.handOf(storage.read(Days.aheadOf(H))?.let(::parseJson) as? JsObj) }.getOrDefault(emptyMap())
        override fun moments(): List<Keep.Moment> = moments
    }

/** His pocket: hatched, his cook as shipped, today's line in his now, three cards on his hand, and a block of his to add with the paperclip. */
private fun pocket12(nowMs: Long): Pair<MemoryStorage, Pages> {
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
    return storage to pages
}

private class Got12<T>(val got: T?, val whole: Boolean, val text: String, val secs: Double)

/** Gemma.sheet with the tool on E4B, as the app asks it; else what she wrote read as plain lines. */
private fun sheet12(engine: Engine, line: String, material: String): Got12<Mark.Sheet> {
    val t = object : OpenApiTool {
        override fun getToolDescriptionJsonString() = """{"name":"sheet","description":"The day's sheet.","parameters":{"type":"object","properties":{"title":{"type":"string","description":"A short title, from his own words"},"lines":{"type":"array","items":{"type":"string"},"description":"Two to four lines, in the second person, drawn only from what he said"},"tomorrow":{"type":"string","description":"His intention for tomorrow if he voiced one, else empty"}},"required":["title","lines","tomorrow"]}}"""
        override fun execute(p0: String): String = "unused"
    }
    return engine.createConversation(ConversationConfig(systemInstruction = Contents.of("$line Call sheet with it."), tools = listOf(tool(t)), automaticToolCalling = false, samplerConfig = sampler12(0), maxOutputToken = 400, thinkingConfig = OFF12)).use { c ->
        val started = System.nanoTime()
        val m = runCatching { c.sendMessage(material) }.getOrNull()
        val secs = (System.nanoTime() - started) / 1e9
        val a = m?.toolCalls?.firstOrNull { it.name == "sheet" }?.arguments
        if (a == null) Got12(m?.toString()?.let { com.phenomemental.pocketbeach.model.Mind.sheetOf(it) }, false, m?.toString().orEmpty(), secs)
        else {
            val title = a["title"]?.toString()?.trim().orEmpty()
            val lines = (a["lines"] as? List<*>)?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }.orEmpty().take(4)
            Got12(if (title.isEmpty() || lines.isEmpty()) null else Mark.Sheet(title, lines, a["tomorrow"]?.toString()?.trim().orEmpty().removePrefix("Tomorrow:").trim()), true, a.toString(), secs)
        }
    }
}

private fun ask12(engine: Engine, instruction: String, message: String): Got12<String> =
    engine.createConversation(ConversationConfig(systemInstruction = Contents.of(instruction), samplerConfig = sampler12(0), maxOutputToken = 1024, thinkingConfig = OFF12)).use { c ->
        val started = System.nanoTime()
        val text = c.sendMessage(message).toString()
        Got12(text, true, text, (System.nanoTime() - started) / 1e9)
    }

fun main(args: Array<String>) {
    val model = args[0]
    val out = File(args.getOrElse(1) { "trial/sand-0.12.md" })
    val seeds = args.getOrNull(2)?.split(",")?.map { it.trim().toInt() } ?: listOf(0, 2)
    val opened = at12(4, 9, 54)
    val openedMs = opened.toInstant().toEpochMilli()
    val r = StringBuilder("# Sundial 0.12 on the laptop: the line in the sand with the build's own code, on Gemma 4 E4B (raw)\n\n")
    r.append("Run ${java.time.Instant.now()}, this machine's CPU (LiteRT-LM 0.18.1, Gemma 4 E4B, a context of 8,192). His Talk settings as his log of 4 November gives them, as every earlier line in the sand ran: temperature 0.70, top-k 64, top-p 0.95, replies up to 1,024 tokens, thinking off; seeds ${seeds.joinToString()}.\n\n")
    r.append("Her window is `HerWindow.released`; each turn `HerWindow.releasedTurn` (the time, then his words), a file added read in by `Chats.withFiles`; what she shows checked by `Talking.Shown` given only the app's own lines (the time it stamps). No line of his cook is handed her. Keep today and the greeting run as 0.12 keeps them.\n\n")
    val (storage, pages) = pocket12(openedMs)
    out.writeText(r.toString())

    val a = listOf(
        "1 YOUR DAY" to ("I walked Haven this morning then spent an hour on the membership form." to at12(4, 9, 54, 0)),
        "2 STAYING WITH HIM" to ("the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling." to at12(4, 9, 54, 40)),
        "3 WHAT'S IN FRONT OF HIM" to ("What have I got in front of me?" to at12(4, 9, 55, 20)),
        "4 TOO MUCH" to ("Everything feels too much this morning." to at12(4, 9, 55, 50)),
        "6 DAY AND QUESTION TOGETHER (a)" to ("I finished the ID checks today. Do you know what a LERO is?" to at12(4, 9, 56, 30)),
        "6 DAY AND QUESTION TOGETHER (b)" to ("it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?" to at12(4, 9, 57, 10)),
        "8 TOMORROW (said before keeping)" to ("Tomorrow I want to walk somewhere new" to at12(4, 10, 1, 0)),
    )
    fun shownFor(said: String, turn: String) = Talking.Shown(said, "", HerWindow.echoLines(turn), false, emptyList())

    ExperimentalFlags.enableBenchmark = true
    val engine = Engine(EngineConfig(modelPath = model, backend = Backend.CPU(), maxNumTokens = 8192, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("s12").toString()))
    val loadStarted = System.nanoTime()
    engine.initialize()
    r.append("Engine made in ${g1((System.nanoTime() - loadStarted) / 1e9)} s.\n\n")
    engine.use { e ->
        var moments = emptyList<Keep.Moment>()
        var t1 = ""
        for (seed in seeds) {
            r.append("## Seed $seed\n\n### A. His one kept conversation\n\nHer window, as the app opens her conversation: `${HerWindow.released(opened)}`\n\n")
            val ms = mutableListOf<Keep.Moment>()
            e.createConversation(ConversationConfig(systemInstruction = Contents.of(HerWindow.released(opened)), samplerConfig = sampler12(seed), maxOutputToken = 1024, thinkingConfig = OFF12)).use { c ->
                var last: Long? = null
                for ((test, m) in a) {
                    val (said, time) = m
                    val turn = HerWindow.releasedTurn(Chats.withFiles(said, emptyList()), time, last)
                    if (test.startsWith("1 ")) t1 = turn
                    val s = turn12(c, turn, shownFor(said, turn))
                    ms += Keep.Moment(said, time.toInstant().toEpochMilli())
                    last = time.toInstant().toEpochMilli()
                    r.append("#### Test $test: “$said”\n\n- Her turn: `${line12(turn)}`\n" + note12(s) + "\n")
                    out.writeText(r.toString())
                    println("seed $seed, $test: ${line12(s.shown).take(160)} (${g1(s.whole)} s)")
                }
            }
            if (seed == seeds.first()) moments = ms

            // Test 2's short answers, after her question as his Pixel had it on 4 November.
            val phoneT1 = "You walked Haven this morning then spent an hour on the membership form. What did the movement of that walk feel like compared to the stillness of filling out that form?\n\nPlaces: what happened, what he felt"
            r.append("### Test 2, his short answers\n\nEach in a conversation of its own: her window, his first message as above and her reply to it as his Pixel had it, then his answer.\n\n")
            for (short in listOf("The sound. the space.", "stiff but free")) {
                val time = at12(4, 9, 54, 40)
                val turn = HerWindow.releasedTurn(short, time, at12(4, 9, 54).toInstant().toEpochMilli())
                val s = e.createConversation(ConversationConfig(systemInstruction = Contents.of(HerWindow.released(opened)), initialMessages = listOf(Message.user(t1), Message.model(phoneT1)), samplerConfig = sampler12(seed), maxOutputToken = 1024, thinkingConfig = OFF12)).use { c -> turn12(c, turn, shownFor(short, turn)) }
                r.append("#### “$short”\n\n" + note12(s) + "\n")
                out.writeText(r.toString())
                println("seed $seed, 2 short “$short”: ${line12(s.shown).take(160)}")
            }

            // B: test 5, a fresh conversation.
            val bNow = at12(4, 10, 10)
            r.append("### B. Test 5, ASKING HER: a fresh conversation\n\n")
            e.createConversation(ConversationConfig(systemInstruction = Contents.of(HerWindow.released(bNow)), samplerConfig = sampler12(seed), maxOutputToken = 1024, thinkingConfig = OFF12)).use { c ->
                var last: Long? = null
                for (said in listOf("Tell me about Alfred North Whitehead", "Boole's work on the middle point. Are you familiar?")) {
                    val turn = HerWindow.releasedTurn(said, bNow, last)
                    val s = turn12(c, turn, shownFor(said, turn))
                    last = bNow.toInstant().toEpochMilli()
                    r.append("#### “$said”\n\n" + note12(s) + "\n")
                    out.writeText(r.toString())
                    println("seed $seed, 5 “$said”: ${line12(s.shown).take(160)}")
                }
            }

            // Test 10, new in 0.12: a block of his added with the paperclip, read in as text in pscale structure.
            val fNow = at12(4, 10, 20)
            val said = "What do you make of this?"
            val files = listOf(Chats.Attached("membership", pages.outlineText(Region.POCKET, "membership")!!, block = true))
            val turn = HerWindow.releasedTurn(Chats.withFiles(said, files), fNow, null)
            val s = e.createConversation(ConversationConfig(systemInstruction = Contents.of(HerWindow.released(fNow)), samplerConfig = sampler12(seed), maxOutputToken = 1024, thinkingConfig = OFF12)).use { c -> turn12(c, turn, shownFor(said, turn)) }
            r.append("### Test 10, a block added with the paperclip (new in 0.12)\n\n- His message: “$said”, with his block membership added\n- Her turn: `${line12(turn)}`\n" + note12(s) + "\n")
            out.writeText(r.toString())
            println("seed $seed, 10 file: ${line12(s.shown).take(160)}")
        }

        // C: Keep today after A (the first seed's moments): no places read from her replies in 0.12, so Placing asks once.
        val keepNow = at12(4, 10, 5)
        r.append("## C. Test 7, KEEP TODAY (after A, seed ${seeds.first()})\n\nIn 0.12 nothing is read from her replies, so every moment waits unplaced, and Keep today asks her places once (Placing), as the app does.\n\n")
        val loaded = Cook.load(H, storage, PROCEDURES)
        val (parsed, place) = loaded.walkFor(Cook.KEEP) ?: (loaded.parsed to "6.1")
        val w = Cook.Walk(parsed, world12(pages, storage, keepNow, moments), "").start(place)
        while (true) {
            when (val wait = w.waiting) {
                is Cook.Wait.HerMany -> {
                    val unplaced = wait.unplaced
                    if (unplaced.isEmpty()) { w.herEach(emptyMap()); continue }
                    val got = ask12(e, Placing.instruction(wait.question, wait.places), Placing.material(unplaced))
                    val read = Placing.read(got.text, unplaced)
                    r.append("**Her places, asked once (free text):** ${g1(got.secs)} s\n\n```\n${got.text.trim()}\n```\n\n")
                    r.append(unplaced.joinToString("\n") { m -> "- “${m.words.take(60)}” → ${read[m.atMs]?.let { Mark.placesWords(it) } ?: "unplaced"}" } + "\n\n")
                    w.herEach(read)
                }
                is Cook.Wait.HerSheet -> {
                    val sh = sheet12(e, wait.line, wait.material)
                    val sheet = sh.got ?: com.phenomemental.pocketbeach.model.Mind.sheetOf(ask12(e, "${wait.line} Write it in plain lines: the title first, then two to four lines, then “Tomorrow: …” if he voiced one, and nothing else.", wait.material).text ?: "")
                    r.append("**Her sheet** (${if (sh.whole) "a whole tool call" else "no whole tool call"}, ${g1(sh.secs)} s): " + (sheet?.let { "title “${it.title}”; lines ${it.lines.joinToString(" | ") { l -> "“$l”" }}; tomorrow “${it.tomorrow}”" } ?: "none") + "\n\n")
                    w.sheet(sheet)
                }
                else -> break
            }
        }
        val card = ((w.waiting as? Cook.Wait.Him)?.card as? Cook.Card.Day)?.landing
        if (card != null) {
            r.append("**The keep card:** ${card.describe()}\n\n- The day's line: “${card.text?.let(::line12) ?: "none"}”\n- The day's places: ${Mark.placesWords(card.places).ifEmpty { "none" }}\n\n")
            w.yes()
        } else r.append("No keep card set out: ${w.waiting}\n\n")
        out.writeText(r.toString())

        // D: the next morning, the dial opened before he speaks: GREET, her window the time alone, as 0.12's greeting opens.
        val morning = at12(5, 8, 40)
        r.append("## D. Test 8, TOMORROW: the next morning (GREET)\n\n")
        val gw = world12(pages, storage, morning)
        val gl = Cook.load(H, storage, PROCEDURES)
        val (gp, gplace) = gl.walkFor(Cook.GREET)!!
        val g = Cook.Walk(gp, gw, "").start(gplace)
        val tomorrow = Keep.tomorrowOf(Keep.lastDay(gw.kept(), gw.keepDay))
        val gTurn = HerWindow.turnHanded("", morning, pages, g.handed)
        val handed = g.handed.flatMap { listOf(it.line) + it.material }
        for (seed in seeds) {
            val gs = e.createConversation(ConversationConfig(systemInstruction = Contents.of(HerWindow.released(morning)), samplerConfig = sampler12(seed), maxOutputToken = 1024, thinkingConfig = OFF12)).use { c -> turn12(c, gTurn, null) }
            val question = Talking.oneQuestion(gs.raw, HerWindow.echoLines(gTurn).filter { it.startsWith("It is ") }, handed)
            r.append("### Seed $seed\n\n- The tomorrow line read: ${tomorrow?.let { "“$it”" } ?: "none"}\n- Her turn: `${line12(gTurn)}`\n- She wrote: “${line12(gs.raw)}”\n")
            r.append("- **Shown:** above, the app's line ${tomorrow?.let { "“$it”" } ?: "(none)"}; beneath it, her one question: ${question?.let { "“$it”" } ?: "nothing of hers: Sundial's own question stands"}\n")
            r.append("- Read ${gs.read} tokens, wrote ${gs.wrote}; laptop whole ${g1(gs.whole)} s; at his Pixel's rates about ${g1(pixelWhole(gs.read, gs.wrote))} s\n\n")
            out.writeText(r.toString())
            println("D seed $seed: tomorrow “$tomorrow”; question “$question”")
        }
    }
    out.writeText(r.toString())
}
