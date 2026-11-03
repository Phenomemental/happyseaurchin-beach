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
 *   gradlew :trial:run -Ptrial -PmainClass=com.phenomemental.pocketbeach.trial.ToolCallKt --args="<E4B.litertlm> <out.md> [seeds]"
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
        for (seed in seeds) for ((label, history) in listOf("2 exchanges before" to before, "none before" to emptyList())) {
            val (storage, pages) = Bench.fixture(TC_SHELF(), nowMs, Talks.SHELL_RECIPE)
            check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
            hatched(storage, pages, nowMs)
            val hand = JsObj.of("_" to str("My hand."), "3" to JsObj.of("_" to str("This week"), "1" to str("and need to update a membership agreement and print it out")))
            check(pages.carryOut(pages.planWhole(Days.aheadOf(H), hand)).ok)
            val world = BenchWorld(pages, storage, now, true, emptyList())
            val loaded = Cook.load(H, storage, PROCEDURES)
            val w = Cook.Walk(loaded.parsed, world, glum).start(loaded.kernel.message ?: "1")
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
