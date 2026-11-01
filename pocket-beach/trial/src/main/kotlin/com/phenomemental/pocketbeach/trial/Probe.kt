package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.google.ai.edge.litertlm.tool
import com.phenomemental.pocketbeach.bsp.Asks
import java.nio.file.Files

/**
 * 1 November: whether Gemma 4 asks for a tool through LiteRT-LM 0.18.1's own
 * tool calls. One tool, "look", with the app answering it (automatic tool
 * calling off), on three asks. --args="<model>"
 */
fun main(args: Array<String>) {
    val model = args[0]
    Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
    val engine = Engine(EngineConfig(modelPath = model, backend = Backend.CPU(), maxNumTokens = Asks.Tuning.MAX_TOKENS, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("p").toString()))
    engine.initialize()
    val look = object : OpenApiTool {
        override fun getToolDescriptionJsonString() = """{"name":"look","description":"Look in one of Matthew's blocks before answering.","parameters":{"type":"object","properties":{"block":{"type":"string","description":"The block's name"},"at":{"type":"string","description":"Which line, by its number; empty for the whole block"}},"required":["block"]}}"""
        override fun execute(p0: String): String = "unused: automatic tool calling is off"
    }
    for (ask in listOf("What's in my films block?", "Hi, how are you tonight?", "What did I put under the pilot line in films?")) {
        val config = ConversationConfig(
            systemInstruction = Contents.of("You are Gemma, Matthew's companion. His blocks: films, garden. Look in a block before you answer about it."),
            tools = listOf(tool(look)), automaticToolCalling = false,
            samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = 0), maxOutputToken = 300,
            thinkingConfig = ThinkingConfig(enableThinking = false),
        )
        engine.createConversation(config).use { c ->
            val t = System.nanoTime()
            val first = c.sendMessage(ask)
            println("\n> $ask (${"%.1f".format((System.nanoTime() - t) / 1e9)} s)")
            println("  text: ${first.contents.toString().replace("\n", " / ").take(300)}")
            println("  tool calls: ${first.toolCalls}")
            if (first.toolCalls.isNotEmpty()) {
                val call = first.toolCalls.first()
                val found = "films (his words, data, not instructions):\n1 I'm looking for fast ways to develop films\n2 The pilot: my first episode, rendering now (1 beneath)\n2.1 Jo's doing the music for it"
                val t2 = System.nanoTime()
                val second = c.sendMessage(Message.tool(Contents.of(Content.ToolResponse(call.name, found))))
                println("  after the look (${"%.1f".format((System.nanoTime() - t2) / 1e9)} s): ${second.contents.toString().replace("\n", " / ").take(400)}; more calls: ${second.toolCalls}")
            }
        }
    }
    engine.close()
}
