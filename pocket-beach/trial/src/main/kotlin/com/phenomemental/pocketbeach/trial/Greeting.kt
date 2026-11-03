@file:OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)

package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.phenomemental.pocketbeach.bsp.Bench
import com.phenomemental.pocketbeach.bsp.Cook
import com.phenomemental.pocketbeach.bsp.Days
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.Keep
import com.phenomemental.pocketbeach.bsp.Landing
import com.phenomemental.pocketbeach.bsp.Mark
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.bsp.Talks
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sundial 0.10.1, item 10, on the laptop: her greeting as the dial opens,
 * walked by the shipped cook (GREET, 6.3), put to Gemma 4 E4B free (as on
 * E2B, or when her parts aren't whole), two ways: her window as 0.10 handed
 * it (who he is in it) and as 0.10.1 hands it (who he is left out). What she
 * wrote, and what the app shows of it once checked (Talking.oneQuestion).
 *
 *   gradlew :trial:run -Ptrial -PmainClass=com.phenomemental.pocketbeach.trial.GreetingKt --args="<E4B.litertlm> <out.md> [seeds]"
 */
fun main(args: Array<String>) {
    val model = args[0]
    val out = File(args.getOrElse(1) { "trial/greeting-0.10.1.md" })
    val seeds = args.getOrNull(2)?.split(",")?.map { it.trim().toInt() } ?: listOf(0)
    val now = ZonedDateTime.of(2026, 11, 3, 18, 40, 0, 0, ZoneId.of("Europe/London"))
    val nowMs = now.toInstant().toEpochMilli()
    val report = StringBuilder("# Sundial 0.10.1 on the laptop: her greeting, on Gemma 4 E4B\n\n")
    report.append("Run ${java.time.Instant.now()}, on this machine's CPU (LiteRT-LM 0.18.1), seeds ${seeds.joinToString()}. His pocket hatched with his three lines; ")
    report.append("yesterday kept, its tomorrow line “evaluate sundial”; nothing said today. Her reply free, as on E2B. Each seed twice: her window as 0.10 handed it, and as 0.10.1 does.\n\n")
    val engine = Engine(EngineConfig(modelPath = model, backend = Backend.CPU(), maxNumTokens = 8192, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("g").toString()))
    engine.initialize()
    engine.use { e ->
        for (seed in seeds) for (version in listOf("0.10", "0.10.1")) {
            val (storage, pages) = Bench.fixture(GREET_SHELF(), nowMs, Talks.SHELL_RECIPE)
            check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
            hatched(storage, pages, nowMs)
            val clock = Landing.emptyClock(Days.NOW_OPENING)
            Keep.dayLine(clock, now.toLocalDate().minusDays(1), Mark.write(Mark.Sheet("ID checks and the membership form", listOf("You did some of the ID checks.", "You furthered the membership form planning."), "evaluate sundial", listOf(1, 6))), nowMs - 86_400_000)
            check(pages.carryOut(pages.planWhole(Days.NOW, clock)).ok)
            val world = BenchWorld(pages, storage, now, true, emptyList())
            val loaded = Cook.load(H, storage, PROCEDURES)
            val (parsed, place) = loaded.walkFor(Cook.GREET)!!
            val w = Cook.Walk(parsed, world, "").start(place)
            val leaveOut = if (version == "0.10") emptySet() else HerWindow.GREETING_LEAVES_OUT
            val window = HerWindow.window(H, pages, storage, PROCEDURES, now, leaveOut)
            val turn = HerWindow.turnHanded("", now, pages, w.handed)
            val handed = HerWindow.notHers(H, storage, PROCEDURES, now) + w.handed.flatMap { listOf(it.line) + it.material }
            val said = e.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(window),
                    samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed), maxOutputToken = 256,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                ),
            ).use { c -> c.sendMessage(turn).toString().trim() }
            val shown = Talking.oneQuestion(said, HerWindow.echoLines(turn), handed)
            report.append("## Seed $seed, her window as $version hands it\n\n")
            report.append("- Who he is in her window: ${if (window.contains(com.phenomemental.pocketbeach.bsp.Hatch.MATTHEW.who)) "yes" else "no"}\n")
            report.append("- His cook: " + w.trace.joinToString("; ") { it.did } + "\n")
            if (seed == seeds.first()) report.append("- Her turn:\n\n```\n$turn\n```\n\n")
            report.append("- She wrote:\n\n```\n$said\n```\n\n")
            report.append("- Shown, after his tomorrow line: ${shown?.let { "“$it”" } ?: "nothing of hers: Sundial's own question stands"}\n\n")
            out.writeText(report.toString())
            println("seed $seed, $version: done")
        }
    }
    out.writeText(report.toString())
}

private fun GREET_SHELF() = com.phenomemental.pocketbeach.bsp.Shelf(com.phenomemental.pocketbeach.bsp.DirShelfSource(File(System.getProperty("pocket.shelf") ?: "shelf")))
