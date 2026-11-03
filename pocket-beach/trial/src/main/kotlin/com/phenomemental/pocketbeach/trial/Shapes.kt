@file:OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)

package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalFlags
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
 * Sundial 0.10.1 on the laptop: his three messages of 3 November, walked by
 * the shipped cook (its TALK as 0.10.1 has it), put to Gemma 4 E4B with the
 * reply's two tools exactly as the app gives them (HerReply.toolJson), her
 * window as the app builds it. Which shape she chose, her parts, and how long
 * each step took. On the CPU here, so the times are the laptop's, not his
 * Pixel's; the shape she chooses is hers either way.
 *
 *   gradlew :trial:run -Ptrial -PmainClass=com.phenomemental.pocketbeach.trial.ShapesKt --args="<E4B.litertlm> <out.md> [seeds]"
 */
fun main(args: Array<String>) {
    val model = args[0]
    val out = File(args.getOrElse(1) { "trial/shapes-0.10.1.md" })
    val seeds = args.getOrNull(2)?.split(",")?.map { it.trim().toInt() } ?: listOf(0)
    val only = args.getOrNull(3)?.split(",")?.map { it.trim() }
    val now = ZonedDateTime.of(2026, 11, 3, 16, 40, 0, 0, ZoneId.of("Europe/London"))
    val nowMs = now.toInstant().toEpochMilli()
    val messages = listOf(
        "Question" to "ok, so you'll never believe it, have you heard of Alfred North Whitehead?",
        "Day" to "I've done some of the id checks today, furthered the membership form planning, and have responded to a fraud report, interacted with David. All that's left today is to evaluate sundial.",
        "No day" to "I just want to talk",
    )
    val report = StringBuilder("# Sundial 0.10.1 on the laptop: her reply's shape, on Gemma 4 E4B\n\n")
    report.append("Run ${java.time.Instant.now()}, on this machine's CPU (LiteRT-LM 0.18.1), seeds ${seeds.joinToString()}; his three messages of 3 November exactly as he sent them; his hand as on his phone (the membership agreement from his 10:03 telling).\n\n")
    ExperimentalFlags.enableBenchmark = true
    val loadStart = System.nanoTime()
    val engine = Engine(EngineConfig(modelPath = model, backend = Backend.CPU(), maxNumTokens = 8192, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("s").toString()))
    engine.initialize()
    report.append("Gemma 4 E4B loaded: ${shapeSecs(System.nanoTime() - loadStart)}.\n\n")
    engine.use { e ->
        for (seed in seeds) for ((name, said) in messages.filter { only == null || it.first in only }) {
            val (storage, pages) = Bench.fixture(PROCEDURES_SHELF(), nowMs, Talks.SHELL_RECIPE)
            check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
            hatched(storage, pages, nowMs)
            val hand = JsObj.of("_" to str("My hand."), "3" to JsObj.of("_" to str("This week"), "1" to str("and need to update a membership agreement and print it out")))
            check(pages.carryOut(pages.planWhole(Days.aheadOf(H), hand)).ok)
            val world = object : Cook.World by BenchWorld(pages, storage, now, true, emptyList()) {
                override fun cards() = mapOf("3.1" to "and need to update a membership agreement and print it out")
            }
            val walkStart = System.nanoTime()
            val loaded = Cook.load(H, storage, PROCEDURES)
            val w = Cook.Walk(loaded.parsed, world, said).start(loaded.kernel.message ?: "1")
            val walked = System.nanoTime() - walkStart
            val window = HerWindow.window(H, pages, storage, PROCEDURES, now)
            val turn = HerWindow.turnHanded(said, now, pages, w.handed)
            val shapes = w.shapes
            val tools = shapes.map { sh ->
                tool(object : OpenApiTool {
                    override fun getToolDescriptionJsonString() = HerReply.toolJson(sh)
                    override fun execute(p0: String): String = "unused"
                })
            }
            val open = System.nanoTime()
            e.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of("$window\n\n" + HerReply.choosing(shapes)),
                    tools = tools, automaticToolCalling = false,
                    samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed), maxOutputToken = 1024,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                ),
            ).use { c ->
                val opened = System.nanoTime() - open
                val asked = System.nanoTime()
                val m = c.sendMessage(turn)
                val took = System.nanoTime() - asked
                val call = m.toolCalls.firstOrNull { tc -> shapes.any { it.tool == tc.name } }
                val b = runCatching { c.getBenchmarkInfo() }.getOrNull()
                report.append("## $name, seed $seed\n\n")
                report.append("He said: “$said”\n\n")
                report.append("- His cook: ${w.answer}; " + w.trace.joinToString("; ") { it.did } + "\n")
                report.append("- Handed her: " + w.handed.joinToString(" / ") { it.line.take(90) + if (it.line.length > 90) "…" else "" } + "\n")
                report.append("- Steps: his cook walked ${shapeSecs(walked)}; her conversation opened ${shapeSecs(opened)}; her reply ${shapeSecs(took)}" +
                    (b?.let { " (read ${it.lastPrefillTokenCount} tokens at ${"%.0f".format(it.lastPrefillTokensPerSecond)}/s, wrote ${it.lastDecodeTokenCount} at ${"%.1f".format(it.lastDecodeTokensPerSecond)}/s)" } ?: "") + "\n")
                if (call == null) report.append("- **No tool call.** She wrote: “${m.toString().trim()}”\n\n")
                else {
                    val sh = shapes.first { it.tool == call.name }
                    report.append("- **She chose ${sh.name}.**\n")
                    for (p in sh.parts) {
                        val v = call.arguments[HerReply.field(p)]
                        report.append("  - $p: “${(v as? List<*>)?.joinToString("; ") ?: v?.toString().orEmpty()}”\n")
                    }
                    report.append("\n")
                }
            }
            out.writeText(report.toString())
            println("$name, seed $seed: done")
        }
    }
    out.writeText(report.toString())
}

private fun shapeSecs(ns: Long) = "%.1f s".format(ns / 1e9)

private fun PROCEDURES_SHELF() = com.phenomemental.pocketbeach.bsp.Shelf(com.phenomemental.pocketbeach.bsp.DirShelfSource(File(System.getProperty("pocket.shelf") ?: "shelf")))
