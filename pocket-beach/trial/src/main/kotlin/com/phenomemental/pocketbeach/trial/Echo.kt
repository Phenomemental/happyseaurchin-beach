package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.phenomemental.pocketbeach.bsp.Bench
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.Shelf
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.Shells
import java.time.ZoneId
import java.time.ZonedDateTime

/** What he said on his Pixel on 29 October at 17:37, as the phone heard it ("torque" is "talk"). */
internal const val GEMMA_ASK = Bench.GEMMA_ASK

/** Two things said first, as an Ask visit might begin. */
private val BEFORE = listOf("Good evening", "What can you do for me?")

/**
 * 0.6.1: what makes her play her turn back. His message to the Talk job's
 * first settings (Edge's: temperature 1.0, top-k 64, top-p 0.95), given her
 * more and more of what 0.6 gives her, a piece at a time; then the fix, her
 * date, time and shell in her instruction, his turn his words alone. Each
 * way is asked with [seeds]; each reply is marked for copying her turn, and
 * for answering him (taking the name Gemma, or speaking to the tool).
 */
@OptIn(ExperimentalApi::class)
internal fun echo(engine: Engine, label: String, shelf: Shelf, seeds: List<Int>) {
    val now = ZonedDateTime.of(2026, 10, 29, 17, 37, 0, 0, ZoneId.of("Europe/London"))
    val (_, pages) = Bench.fixture(shelf, now.toInstant().toEpochMilli(), Talks.SHELL_RECIPE)
    val shell = HelloWorld.SHELL
    val opening = HelloWorld.SHELL_OPENING
    val instruction = Talking.instruction(shell, pages)
    // The first fix tried: 0.6's instruction with the time and her shell after it.
    val tried = instruction + "\n\nIt is Thursday 29 October 2026, 17:37.\n" + Talking.shellNow(shell, pages)
    // 0.6's instruction without its line telling her each message comes with the date, the time and her shell.
    val unprimed = instruction.replace(" Each of his messages comes with the date, the time and your shell; let the shell guide you.", "")
    check(unprimed != instruction)
    val withNow = Talking.instruction(shell, pages, now, GEMMA_ASK)
    fun sampler(seed: Int) = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed)

    /** One conversation: [system], the [history] given, then each of [messages] in turn; her replies. */
    fun talk(system: String?, history: List<Pair<String, String>>, messages: List<String>, seed: Int): List<String> =
        engine.createConversation(
            ConversationConfig(
                systemInstruction = system?.let { Contents.of(it) },
                initialMessages = history.flatMap { (a, b) -> listOf(Message.user(a), Message.model(b)) },
                samplerConfig = sampler(seed), maxOutputToken = 300, thinkingConfig = ThinkingConfig(enableThinking = false),
            ),
        ).use { c -> messages.map { m -> c.sendMessage(m).toString().trim() } }

    /** As 0.6 asks: one conversation kept, each turn the date, the time, her shell, then "Matthew:" and his words. */
    fun as06(seed: Int, before: List<String>): String {
        val turns = (before + GEMMA_ASK).map { Talking.turn(it, now, shell, pages) }
        return talk(instruction, emptyList(), turns, seed).last()
    }

    /** As a fix asks: her conversation opened afresh for each message, with [system], the earlier exchanges his words and hers. */
    fun fixed(system: String, seed: Int, before: List<String>): String {
        val history = mutableListOf<Pair<String, String>>()
        for (m in before) history += m to talk(system, history.toList(), listOf(m), seed).single()
        return talk(system, history, listOf(GEMMA_ASK), seed).single()
    }

    val ways: List<Pair<String, (Int) -> String>> = listOf(
        "A. his message alone (as Edge)" to { seed -> talk(null, emptyList(), listOf(GEMMA_ASK), seed).single() },
        "B. + her instruction" to { seed -> talk(instruction, emptyList(), listOf(GEMMA_ASK), seed).single() },
        "C. + the date and shell in his turn (0.6)" to { seed -> as06(seed, emptyList()) },
        "D. + two exchanges before, kept (0.6)" to { seed -> as06(seed, BEFORE) },
        "E. first fix tried: the date and shell after her instruction, his words alone" to { seed -> fixed(tried, seed, emptyList()) },
        "F. the first fix tried, with two exchanges before" to { seed -> fixed(tried, seed, BEFORE) },
        "G. her instruction without its line on the date, time and shell" to { seed -> talk(unprimed, emptyList(), listOf(GEMMA_ASK), seed).single() },
        "H. 0.6.1: the time and shell in her instruction, of the pocket only the blocks his words name, his words alone" to { seed -> fixed(withNow, seed, emptyList()) },
        "I. 0.6.1, with two exchanges before" to { seed -> fixed(withNow, seed, BEFORE) },
    )
    val only = System.getProperty("echo.ways")
    println("\n== $label: what makes her play her turn back ==")
    for ((name, way) in ways.filter { only == null || it.first[0] in only }) {
        var copies = 0
        var answers = 0
        println("\n-- $name")
        for (seed in seeds) {
            val start = System.nanoTime()
            val reply = way(seed)
            val copied = Talking.copied(reply, GEMMA_ASK, opening)
            val answered = Bench.answers(Bench.TALK_ASKS.first(), Talking.unechoed(reply, GEMMA_ASK, opening))
            if (copied.isNotEmpty()) copies++
            if (answered && copied.isEmpty()) answers++
            println("seed $seed (${secs(start)} s): " + (if (copied.isEmpty()) "no copy" else "COPIES ${copied.size} line(s)") + (if (answered) ", answers him" else ", doesn't answer") +
                "\n  " + reply.replace("\n", "\n  "))
        }
        println("=> $name: copies her turn in $copies of ${seeds.size}; answers him, copying nothing, in $answers of ${seeds.size}")
    }
}

/**
 * 0.6.1's two fixed asks for the Talk job, as Ask puts them now: his
 * message of 29 October, and "I'd like to call you Gemma". Her reply with
 * the Talk job's first settings (seed 0, as the app draws), replies of up to
 * 1024 tokens; then, when his words may ask for a write, the Write job's
 * proposal, drawn greedily as the app's Write job is.
 */
@OptIn(ExperimentalApi::class)
internal fun talkAsks(engine: Engine, label: String, shelf: Shelf) {
    val now = ZonedDateTime.of(2026, 10, 29, 17, 37, 0, 0, ZoneId.of("Europe/London"))
    val nowMs = now.toInstant().toEpochMilli()
    println("\n== $label: the Talk job's fixed asks ==")
    for (ask in Bench.TALK_ASKS) {
        val (storage, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        val start = System.nanoTime()
        val reply = engine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(Talking.instruction(HelloWorld.SHELL, pages, now, ask.question)),
                samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = 0), maxOutputToken = 1024,
                thinkingConfig = ThinkingConfig(enableThinking = false),
            ),
        ).use { it.sendMessage(Talking.said(ask.question)).toString().trim() }
        val talked = (System.nanoTime() - start) / 1_000_000
        val proposal = if (!Talking.mayWrite(ask.question)) null else {
            val worn = Shells(storage, shelf).wear(HelloWorld.SHELL, nowMs)
            engine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(Talks.instruction(HelloWorld.SHELL, worn.window, Asks.plainWindow(pages, HelloWorld.SHELL), pages, examplesInText = false)),
                    initialMessages = Talks.opening(emptyList()).flatMap { (a, b) -> listOf(Message.user(a), Message.model(b)) },
                    samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0, seed = 0), maxOutputToken = 1024,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                ),
            ).use { c ->
                val stream = Talks.Stream { message, piece -> c.sendMessage(message).toString().also(piece) }
                val a = Talks.ask(ask.question, stream, pages, storage, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), { nowMs }, answerReads = false)
                a.exchanges.forEach { (_, r) -> println("    | Write job: " + r.replace("\n", " / ").take(240)) }
                a.planned?.proposal
            }
        }
        println(Bench.talkLine(ask, reply, proposal, talked))
        println("  Her reply whole:\n  " + reply.replace("\n", "\n  "))
    }
}

