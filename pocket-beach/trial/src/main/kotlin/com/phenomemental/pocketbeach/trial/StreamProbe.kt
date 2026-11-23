@file:OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)

package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.google.ai.edge.litertlm.tool
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.WalkTool
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sundial 0.13.4: what LiteRT-LM's stream carries on a turn with the walker
 * offered: each chunk's text and any tool calls, and when each came. For the
 * rule that holds her words back only while a call is still possible.
 *
 *   gradlew :trial:run -Ptrial -PmainClass=com.phenomemental.pocketbeach.trial.StreamProbeKt --args="<E4B.litertlm> <out.md>"
 */
fun main(args: Array<String>) {
    val out = File(args.getOrElse(1) { "trial/stream-probe.md" })
    val now = ZonedDateTime.of(2026, 11, 4, 11, 0, 0, 0, ZoneId.of("Europe/London"))
    val r = StringBuilder("# What the stream carries on a tool turn (raw)\n\n")
    val engine = Engine(EngineConfig(modelPath = args[0], backend = Backend.CPU(), maxNumTokens = 4096, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("sp").toString()))
    engine.initialize()
    val cases = listOf(
        "membership" to "Read me what's at 2.2 in my membership block.",
        "passport:Phenomemental" to "what does my passport tell you about how to work with me\n\n(His block passport:Phenomemental, which his words named, read by the app: its opening line, then each part's opening line beneath it:)\nMatthew Bushell — digital social innovator.\n  1  The heart.",
        "passport:Phenomemental" to "do what you think is best",
    )
    engine.use { e ->
        for (seed in listOf(0, 2)) for ((block, said) in cases) {
            val t = object : OpenApiTool { override fun getToolDescriptionJsonString() = WalkTool.forBlocks(listOf(block)); override fun execute(p0: String) = "unused" }
            r.append("## Seed $seed: “${said.lines().first()}”\n\n")
            e.createConversation(ConversationConfig(
                systemInstruction = Contents.of(HerWindow.released(now)), tools = listOf(tool(t)), automaticToolCalling = false,
                samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed), maxOutputToken = 200,
                thinkingConfig = ThinkingConfig(enableThinking = false),
            )).use { c ->
                var send: Message = Message.user(Contents.of(HerWindow.releasedTurn(said, now)))
                for (round in 1..2) {
                    val started = System.nanoTime()
                    val calls = mutableListOf<com.google.ai.edge.litertlm.ToolCall>()
                    var n = 0
                    kotlinx.coroutines.runBlocking {
                        c.sendMessageAsync(send).collect { m ->
                            n++
                            val ms = (System.nanoTime() - started) / 1_000_000
                            if (n <= 12 || m.toolCalls.isNotEmpty()) r.append("- round $round, chunk $n at $ms ms: text “${m.toString().replace("\n", "\\n")}”" + (if (m.toolCalls.isNotEmpty()) "; tool calls ${m.toolCalls.map { it.name + " " + it.arguments }}" else "") + "\n")
                            calls += m.toolCalls
                        }
                    }
                    r.append("- round $round: $n chunks, ${calls.size} calls, ${(System.nanoTime() - started) / 1_000_000} ms\n")
                    out.writeText(r.toString())
                    if (calls.isEmpty()) break
                    send = Message.tool(Contents.of(calls.map { Content.ToolResponse(it.name, "(His block $block, walked to 2.2: the path from its opening line down to it.)\nThe membership form, step by step.\n  2  ID checks\n    2.2  Log each one in the register") }))
                }
            }
            r.append("\n")
            out.writeText(r.toString())
            println("seed $seed done: ${said.lines().first()}")
        }
    }
}
