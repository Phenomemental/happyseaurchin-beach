@file:OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)

package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.phenomemental.pocketbeach.bsp.Bench
import com.phenomemental.pocketbeach.bsp.Cook
import com.phenomemental.pocketbeach.bsp.Days
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.HerReply
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.str
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sundial 0.10.2 on the laptop's Gemma 4 E4B: her one kept conversation, as
 * the app opens it (her window, the time, her recipe for talk, read once),
 * and his two messages of 3 November in turn, each turn only his new message
 * and what is new with it. Her reply written free: what she wrote, her places
 * line, the shape the split found, what is shown. For each turn, what she
 * read and wrote, and the laptop's times; and the same tokens at his Pixel's
 * own rates on 3 November (2,370 read before her first words at 7.1 s; 31
 * written in 3.7 s), an estimate only his phone can confirm.
 *
 *   gradlew :trial:run -Ptrial -PmainClass=com.phenomemental.pocketbeach.trial.KeptKt --args="<E4B.litertlm> <out.md> [seeds]"
 */
private const val PHONE_READ_PER_S = 2370.0 / 7.1
private const val PHONE_WRITE_PER_S = 31.0 / 3.7

fun main(args: Array<String>) {
    val out = File(args.getOrElse(1) { "trial/kept-0.10.2.md" })
    val seeds = args.getOrNull(2)?.split(",")?.map { it.trim().toInt() } ?: listOf(0)
    val now = ZonedDateTime.of(2026, 11, 3, 20, 10, 0, 0, ZoneId.of("Europe/London"))
    val nowMs = now.toInstant().toEpochMilli()
    val messages = listOf(
        "no I'm not focused at all. I'm feeling glum",
        "I've done some of the id checks today, furthered the membership form planning, and have responded to a fraud report, interacted with David. All that's left today is to evaluate sundial.",
    )
    val report = StringBuilder("# Sundial 0.10.2 on the laptop: her one kept conversation, on Gemma 4 E4B\n\n")
    report.append("Run ${java.time.Instant.now()}, CPU, seeds ${seeds.joinToString()}. His hand as on his phone. Each seed one conversation, opened once; his two messages of 3 November in turn. ")
    report.append("“At his phone's rates” is these tokens at the rates his Pixel's workings gave on 3 November (${"%.0f".format(PHONE_READ_PER_S)} read and ${"%.1f".format(PHONE_WRITE_PER_S)} written a second): an estimate, not a measurement.\n\n")
    ExperimentalFlags.enableBenchmark = true
    val engine = Engine(EngineConfig(modelPath = args[0], backend = Backend.CPU(), maxNumTokens = 8192, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("k").toString()))
    engine.initialize()
    engine.use { e ->
        for (seed in seeds) {
            val (storage, pages) = Bench.fixture(KEPT_SHELF(), nowMs, Talks.SHELL_RECIPE)
            check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
            hatched(storage, pages, nowMs)
            val hand = JsObj.of("_" to str("My hand."), "3" to JsObj.of("_" to str("This week"), "1" to str("and need to update a membership agreement and print it out")))
            check(pages.carryOut(pages.planWhole(Days.aheadOf(H), hand)).ok)
            val world = object : Cook.World by BenchWorld(pages, storage, now, true, emptyList()) {
                override fun cards() = mapOf("3.1" to "and need to update a membership agreement and print it out")
            }
            val loaded = Cook.load(H, storage, PROCEDURES)
            val recipe = Cook.talkRecipe(loaded.parsed)
            val opening = HerWindow.opening(HerWindow.window(H, pages, storage, PROCEDURES, now), now, recipe)
            report.append("## Seed $seed\n\n")
            e.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(opening),
                    samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed), maxOutputToken = 1024,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                ),
            ).use { c ->
                for ((i, said) in messages.withIndex()) {
                    val w = Cook.Walk(loaded.parsed, world, said).start(loaded.kernel.message ?: "1")
                    val turn = HerWindow.turnKept(said, pages, w.handed, recipe)
                    val started = System.nanoTime()
                    var first: Double? = null
                    val raw = StringBuilder()
                    kotlinx.coroutines.runBlocking {
                        c.sendMessageAsync(turn).collect { m -> if (first == null) first = (System.nanoTime() - started) / 1e9; raw.append(m.toString()) }
                    }
                    val whole = (System.nanoTime() - started) / 1e9
                    val b = runCatching { c.getBenchmarkInfo() }.getOrNull()
                    val read = b?.lastPrefillTokenCount ?: 0
                    val wrote = b?.lastDecodeTokenCount ?: 0
                    val shown = Talking.hers(raw.toString(), said, appLines = HerWindow.echoLines(turn))
                    val f = HerReply.free(raw.toString(), shown)
                    val phoneFirst = read / PHONE_READ_PER_S
                    val phoneWhole = phoneFirst + wrote / PHONE_WRITE_PER_S
                    report.append("### Message ${i + 1}: “$said”\n\n")
                    if (seed == seeds.first()) report.append("- Her turn, all of it:\n\n```\n$turn\n```\n\n")
                    report.append("- Read $read tokens, wrote $wrote; her conversation now holds ${runCatching { c.getTokenCount() }.getOrNull()} tokens\n")
                    report.append("- Laptop: first words ${"%.1f".format(first ?: 0.0)} s, whole ${"%.1f".format(whole)} s. At his phone's rates: first words about ${"%.1f".format(phoneFirst)} s, whole about ${"%.1f".format(phoneWhole)} s\n")
                    report.append("- She wrote: “${raw.toString().trim().replace("\n", " / ")}”\n")
                    report.append("- Places line: ${f.placesLine?.let { "“$it”" } ?: "none"}; the split found ${f.shape}\n")
                    report.append("- Shown: part one “${f.partOne.replace("\n", " / ")}”" + (if (f.question.isNotEmpty()) "; her question, in italic, “${f.question}”" else "") + "\n\n")
                    out.writeText(report.toString())
                    println("seed $seed, message ${i + 1}: read $read, wrote $wrote, ${f.shape}")
                }
            }
        }
    }
    out.writeText(report.toString())
}

private fun KEPT_SHELF() = com.phenomemental.pocketbeach.bsp.Shelf(com.phenomemental.pocketbeach.bsp.DirShelfSource(File(System.getProperty("pocket.shelf") ?: "shelf")))
