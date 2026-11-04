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
import com.phenomemental.pocketbeach.bsp.Telling
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sundial 0.10.1 and 0.10.2, on the laptop: her greeting as the dial opens,
 * walked by the shipped cook (GREET, 6.3), put to Gemma 4 E4B free (as on
 * E2B, or when her parts aren't whole), two ways: her window as 0.10 handed
 * it (who he is in it) and as 0.10.1 hands it (who he is left out). What she
 * wrote, and what the app shows of it once checked (Talking.oneQuestion).
 *
 *   gradlew :trial:run -Ptrial -PmainClass=com.phenomemental.pocketbeach.trial.GreetingKt --args="<E4B.litertlm> <out.md> [seeds]"
 */
fun main(args: Array<String>) {
    val model = args[0]
    val out = File(args.getOrElse(1) { "trial/greeting-0.10.2.md" })
    val seeds = args.getOrNull(2)?.split(",")?.map { it.trim().toInt() } ?: listOf(0)
    val now = ZonedDateTime.of(2026, 11, 3, 18, 40, 0, 0, ZoneId.of("Europe/London"))
    val nowMs = now.toInstant().toEpochMilli()
    val tomorrowLine = "evaluate sundial"
    val report = StringBuilder("# Sundial 0.10.2 on the laptop: her greeting, on Gemma 4 E4B\n\n")
    report.append("Run ${java.time.Instant.now()}, on this machine's CPU (LiteRT-LM 0.18.1), seeds ${seeds.joinToString()}. His pocket hatched with his three lines; her window without who he is, as the app asks it; ")
    report.append("GREET as 0.10.2 ships it (cook 6.3: the app reads his tomorrow line and branches; her line short sentences with no branch); her question written free. ")
    report.append("Two cases: yesterday kept with its tomorrow line \u201c$tomorrowLine\u201d, and no day kept. Each question checked: spoken to him as you, never he, him or his, and with a tomorrow line, about it.\n\n")
    val engine = Engine(EngineConfig(modelPath = model, backend = Backend.CPU(), maxNumTokens = 8192, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("g").toString()))
    engine.initialize()
    val tally = mutableMapOf<String, Pair<Int, Int>>()
    engine.use { e ->
        for (seed in seeds) for (case in listOf("his tomorrow line", "no day kept")) {
            val (storage, pages) = Bench.fixture(GREET_SHELF(), nowMs, Talks.SHELL_RECIPE)
            check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
            hatched(storage, pages, nowMs)
            if (case == "his tomorrow line") {
                val clock = Landing.emptyClock(Days.NOW_OPENING)
                Keep.dayLine(clock, now.toLocalDate().minusDays(1), Mark.write(Mark.Sheet("ID checks and the membership form", listOf("You did some of the ID checks.", "You furthered the membership form planning."), tomorrowLine, listOf(1, 6))), nowMs - 86_400_000)
                check(pages.carryOut(pages.planWhole(Days.NOW, clock)).ok)
            }
            val world = BenchWorld(pages, storage, now, true, emptyList())
            val loaded = Cook.load(H, storage, PROCEDURES)
            val (parsed, place) = loaded.walkFor(Cook.GREET)!!
            val w = Cook.Walk(parsed, world, "").start(place)
            val window = HerWindow.window(H, pages, storage, PROCEDURES, now, HerWindow.GREETING_LEAVES_OUT)
            val turn = HerWindow.turnHanded("", now, pages, w.handed)
            val handed = HerWindow.notHers(H, storage, PROCEDURES, now) + w.handed.flatMap { listOf(it.line) + it.material }
            val said = e.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(window),
                    samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed), maxOutputToken = 256,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                ),
            ).use { c -> c.sendMessage(turn).toString().trim() }
            val shown = Talking.oneQuestion(said, HerWindow.echoLines(turn).filter { it.startsWith("It is ") }, handed)
            val words = Telling.plain(shown.orEmpty()).split(' ')
            val you = words.any { it in setOf("you", "your", "you're", "you've", "you'd", "yours") }
            val he = words.any { it in setOf("he", "him", "his", "he's", "he'd", "himself") }
            val honoured = case != "his tomorrow line" || words.any { it.startsWith("evaluat") || it == "sundial" }
            val named = words.any { it.startsWith("matt") }
            val ok = shown != null && you && !he && honoured && !named
            val (good, all) = tally[case] ?: (0 to 0)
            tally[case] = (good + if (ok) 1 else 0) to (all + 1)
            report.append("## Seed $seed, $case\n\n")
            if (seed == seeds.first()) report.append("- Her turn:\n\n```\n$turn\n```\n\n")
            report.append("- She wrote: \u201c${said.replace("\n", " / ")}\u201d\n")
            report.append("- Shown: ${shown?.let { "\u201c$it\u201d" } ?: "nothing of hers: Sundial's own question stands"}\n")
            report.append("- As you: ${if (you) "yes" else "no"}; he, him or his: ${if (he) "yes" else "no"}; his name: ${if (named) "yes" else "no"}" + (if (case == "his tomorrow line") "; about his tomorrow line: ${if (honoured) "yes" else "no"}" else "") + "\n\n")
            out.writeText(report.toString())
            println("seed $seed, $case: ${if (ok) "ok" else "NOT"} — $shown")
        }
    }
    report.append("## Tally\n\n")
    for ((case, t) in tally) report.append("- $case: ${t.first} of ${t.second} as you, never he, never his name, " + (if (case == "his tomorrow line") "about his tomorrow line" else "about today") + "\n")
    out.writeText(report.toString())
}

private fun GREET_SHELF() = com.phenomemental.pocketbeach.bsp.Shelf(com.phenomemental.pocketbeach.bsp.DirShelfSource(File(System.getProperty("pocket.shelf") ?: "shelf")))
