@file:OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)

package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Backend
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
import com.phenomemental.pocketbeach.bsp.Cook
import com.phenomemental.pocketbeach.bsp.Days
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.HerReply
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.str
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sundial 0.10.1's reply tool call on the laptop's Gemma 4 E4B, as his
 * Pixel asked it on 3 November: her window, the two tools (reflect, answer),
 * his message as his cook hands it, and her conversation as the phone had it
 * ("with 2 exchanges before", each her plain words) or with none, as the
 * laptop tried it before. Whether a whole tool call comes back, and if not,
 * what she wrote instead.
 *
 *   gradlew :trial:run -Ptrial -PmainClass=com.phenomemental.pocketbeach.trial.ToolCallKt -Pprocedures=<0.10.1's assets/procedures> --args="<E4B.litertlm> <out.md> [seeds] [long]"
 *
 * With "long", his pocket as full as his Pixel's was (yesterday kept, today's
 * telling, his hand, five things kept, her greeting's whole turn before), so
 * her turn reads about as many tokens as the phone's did.
 */
fun main(args: Array<String>) {
    val out = File(args.getOrElse(1) { "trial/toolcall-0.10.1.md" })
    val seeds = args.getOrNull(2)?.split(",")?.map { it.trim().toInt() } ?: listOf(0)
    val now = ZonedDateTime.of(2026, 11, 3, 20, 10, 0, 0, ZoneId.of("Europe/London"))
    val nowMs = now.toInstant().toEpochMilli()
    val glum = "no I'm not focused at all. I'm feeling glum"
    // Two exchanges before, as her kept conversation held them on his phone: her greeting answered, then a message and her plain reply.
    val before = listOf(
        "(The app: he has opened his sundial and hasn't spoken to you yet today.)" to "What came of evaluating sundial?",
        "Not much yet, I've been going round in circles." to "You've been going round in circles. What's pulling at your attention most?",
    )
    val report = StringBuilder("# Sundial 0.10.1's reply tool call, on the laptop's Gemma 4 E4B\n\n")
    report.append("Run ${java.time.Instant.now()}, CPU, seeds ${seeds.joinToString()}. His message: “$glum”. Her window, the two tools and her turn exactly as 0.10.1 builds them; ")
    report.append("her conversation with the two exchanges before it, as his Pixel had it, and with none, as the laptop's run of 3 November had it.\n\n")
    ExperimentalFlags.enableBenchmark = true
    val engine = Engine(EngineConfig(modelPath = args[0], backend = Backend.CPU(), maxNumTokens = 8192, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("t").toString()))
    engine.initialize()
    val tally = mutableMapOf<String, Pair<Int, Int>>()
    engine.use { e ->
        val long = args.getOrNull(3) == "long"
        val cases = if (long) listOf("as long as his phone's" to before) else listOf("2 exchanges before" to before, "none before" to emptyList())
        for (seed in seeds) for ((label, shortHistory) in cases) {
            val (storage, pages) = Bench.fixture(TC_SHELF(), nowMs, Talks.SHELL_RECIPE)
            check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
            hatched(storage, pages, nowMs)
            val hand = if (!long) JsObj.of("_" to str("My hand."), "3" to JsObj.of("_" to str("This week"), "1" to str("and need to update a membership agreement and print it out")))
            else JsObj.of(
                "_" to str("My hand."),
                "2" to JsObj.of("_" to str("Today"), "1" to str("for example I need to update some meeting minutes and print them out"), "2" to str("as well as respond to emails"), "3" to str("ring the bank about the standing order")),
                "3" to JsObj.of("_" to str("This week"), "1" to str("and need to update a membership agreement and print it out"), "2" to str("finish the id checks for the new members"), "3" to str("send David the phase two proposal")),
                "4" to JsObj.of("_" to str("This month"), "1" to str("plan the membership form with the committee"), "2" to str("write up the fraud report for the board")),
            )
            check(pages.carryOut(pages.planWhole(Days.aheadOf(H), hand)).ok)
            var history = shortHistory
            if (long) {
                // As his Pixel held it: yesterday kept with its tomorrow line, today's earlier telling in his now, five things kept in his stash,
                // and her two exchanges before: her greeting's whole turn as 0.10.1 kept it, then his day message and her plain reply.
                val clock = com.phenomemental.pocketbeach.bsp.Landing.emptyClock(Days.NOW_OPENING)
                com.phenomemental.pocketbeach.bsp.Keep.dayLine(clock, now.toLocalDate().minusDays(1), com.phenomemental.pocketbeach.bsp.Mark.write(com.phenomemental.pocketbeach.bsp.Mark.Sheet("ID checks and the membership form", listOf("You did some of the ID checks.", "You furthered the membership form planning.", "You spoke with David about phase two."), "evaluate sundial", listOf(1, 6))), nowMs - 86_400_000)
                Days.voiced(clock, Days.dayAddress(now.toLocalDate()).take(8), "Busy morning. I've done some of the id checks today, furthered the membership form planning, and have responded to a fraud report, interacted with David. Later I need to ring the bank and update the meeting minutes, and I want to get the phase two proposal to David before Friday.")
                check(pages.carryOut(pages.planWhole(Days.NOW, clock)).ok)
                val stash = JsObj.of("_" to str("What Matthew keeps."))
                listOf(
                    "Natural inclusion: the space between is as alive as what it holds; keep the edges soft, and let what flows through them be the measure of the work.",
                    "The pocket beach: a phone that keeps his days in his own words, read freely and written only when he says yes, with nothing leaving it unless he sends it.",
                    "Remember the wifi code at the hall is otter-42, and the committee meets on the second Thursday of the month in the small room upstairs.",
                    "For the membership form: ask for a second contact, make the consent line plain, and keep the whole thing to one side of paper if it can be done.",
                    "Whitehead: the world is made of events rather than things; the many become one, and are increased by one.",
                ).forEachIndexed { i, t -> stash["${i + 1}"] = str(t) }
                check(pages.carryOut(pages.planWhole(com.phenomemental.pocketbeach.bsp.Hatch.stashOf(H), stash)).ok)
                val greetWorld = BenchWorld(pages, storage, now, true, emptyList())
                val gl = Cook.load(H, storage, PROCEDURES)
                val (gp, gplace) = gl.walkFor(Cook.GREET)!!
                val g = Cook.Walk(gp, greetWorld, "").start(gplace)
                val greetTurn = HerWindow.turnHanded("", now.withHour(9), pages, g.handed)
                history = listOf(
                    greetTurn to "What came of evaluating sundial?",
                    "I've done some of the id checks today, furthered the membership form planning, and have responded to a fraud report, interacted with David. All that's left today is to evaluate sundial." to
                        "You've done some of the id checks today, furthered the membership form planning, and responded to a fraud report, and interacted with David. What will evaluating sundial ask of you?",
                )
            }
            val world = BenchWorld(pages, storage, now, true, emptyList())
            val loaded = Cook.load(H, storage, PROCEDURES)
            val w = Cook.Walk(loaded.parsed, world, glum).start(loaded.kernel.message ?: "1")
            check(w.shapes.isNotEmpty()) { "this trial asks 0.10.1's tool call: run it with -Pprocedures=<0.10.1's assets/procedures>" }
            val shapes = w.shapes
            val window = HerWindow.window(H, pages, storage, PROCEDURES, now)
            val turn = HerWindow.turnHanded(glum, now, pages, w.handed)
            val tools = shapes.map { sh -> tool(object : OpenApiTool { override fun getToolDescriptionJsonString() = HerReply.toolJson(sh); override fun execute(p0: String) = "unused" }) }
            val got = e.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of("$window\n\n" + HerReply.choosing(shapes)),
                    initialMessages = history.flatMap { (a, s) -> listOf(Message.user(a), Message.model(s)) },
                    tools = tools, automaticToolCalling = false,
                    samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed), maxOutputToken = 1024,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                ),
            ).use { c ->
                val started = System.nanoTime()
                val m = c.sendMessage(turn)
                val b = runCatching { c.getBenchmarkInfo() }.getOrNull()
                Triple(m, b, (System.nanoTime() - started) / 1e9)
            }
            val (m, b, secs) = got
            val call = m.toolCalls.firstOrNull { tc -> shapes.any { it.tool == tc.name } }
            val (whole, all) = tally[label] ?: (0 to 0)
            tally[label] = (whole + if (call != null) 1 else 0) to (all + 1)
            report.append("## Seed $seed, $label\n\n")
            report.append("- Read ${b?.lastPrefillTokenCount} tokens, wrote ${b?.lastDecodeTokenCount}, ${"%.1f".format(secs)} s on this CPU\n")
            if (call == null) report.append("- **No whole tool call.** She wrote, as plain words: “${m.toString().trim()}”\n\n")
            else report.append("- **A whole tool call: ${call.name}.** ${call.arguments}\n\n")
            out.writeText(report.toString())
            println("seed $seed, $label: ${if (call != null) "tool call" else "plain words"}")
        }
    }
    report.append("## Tally\n\n")
    for ((label, t) in tally) report.append("- $label: a whole tool call ${t.first} of ${t.second}\n")
    out.writeText(report.toString())
}

private fun TC_SHELF() = com.phenomemental.pocketbeach.bsp.Shelf(com.phenomemental.pocketbeach.bsp.DirShelfSource(File(System.getProperty("pocket.shelf") ?: "shelf")))
