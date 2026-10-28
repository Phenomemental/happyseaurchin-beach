package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.LogSeverity
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.SessionConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import java.nio.file.Files

/**
 * What the engine allows, tried on the laptop before building on it:
 * speculative decoding asked for one session only, a route held to one word
 * by a regex, and both on one engine. --args="<model> <engineFlag: on|off|null>"
 */
@OptIn(ExperimentalApi::class)
fun main(args: Array<String>) {
    val model = args[0]
    Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
    ExperimentalFlags.enableBenchmark = true
    ExperimentalFlags.enableSpeculativeDecoding = when (args.getOrNull(1)) { "on" -> true; "off" -> false; else -> null }
    val engine = Engine(EngineConfig(modelPath = model, backend = Backend.CPU(), maxNumTokens = 4096, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("e").toString()))
    var t = System.nanoTime()
    engine.initialize()
    println("engine flag ${ExperimentalFlags.enableSpeculativeDecoding}; loaded in ${s(t)} s")
    val sampler = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0, seed = 0)
    val story = "Write a plain paragraph of about 150 words about the sea at night."

    for (spec in if (args.getOrNull(3) == "nosession") emptyList() else listOf(false, true)) {
        val session = engine.createSession(SessionConfig(sampler, null, spec))
        t = System.nanoTime()
        val out = try { session.generateContent(listOf(InputData.Text(story))) } catch (e: Exception) { "FAILED: ${e.message?.take(200)}" }
        println("session spec=$spec: ${s(t)} s, ${out.length} chars: ${out.replace("\n", " ").take(160)}")
        session.close()
    }

    val conversation = engine.createConversation(ConversationConfig(
        systemInstruction = Contents.of("You are a companion on a phone. Before anything else, choose how to answer: talk, read or act."),
        samplerConfig = sampler, maxOutputToken = 64, thinkingConfig = ThinkingConfig(enableThinking = false), enableResponseFormat = true,
    ))
    for (q in listOf("What's your name?", "What's in my garden block?", "Add a line to garden: buy compost.")) {
        t = System.nanoTime()
        val format = if (args.getOrNull(2) == "json") ResponseFormat.json("""{"type":"object","properties":{"route":{"type":"string","enum":["talk","read","act"]}},"required":["route"]}""")
        else ResponseFormat.regex("(talk|read|act)")
        val r = try { conversation.sendMessage("Matthew asks: $q\nOne word, how to answer: talk, read or act.", responseFormat = format).toString() } catch (e: Exception) { "FAILED: ${e.message?.lines()?.take(2)}" }
        println("route ${s(t)} s: $q -> $r")
    }
    t = System.nanoTime()
    val free = try { conversation.sendMessage("Now say hello in one short sentence.").toString() } catch (e: Exception) { "FAILED: ${e.message?.take(200)}" }
    println("free turn after: ${s(t)} s: ${free.take(120)}")
    conversation.close()
    engine.close()
}

private fun s(since: Long) = "%.1f".format((System.nanoTime() - since) / 1e9)
