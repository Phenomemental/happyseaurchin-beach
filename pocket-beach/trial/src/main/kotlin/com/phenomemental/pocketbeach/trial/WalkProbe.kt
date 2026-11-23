@file:OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)

package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.google.ai.edge.litertlm.tool
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sundial 0.13, the walker's own words (found by the 0.13 trial's first run,
 * 23 November): with the walker described as 0.12.1 first wrote it, she
 * never called it and told him she had no access; section 9's bsp tool, in
 * other words, was called every time. Each message in a fresh conversation,
 * her window the time alone; only whether her first answer is a call, and
 * what it carries, is read.
 *
 *   gradlew :trial:run -Ptrial -PmainClass=com.phenomemental.pocketbeach.trial.WalkProbeKt --args="<E4B.litertlm> <out.md> [seeds]"
 */

private const val PARAMS_FIRST = """"block":{"type":"string","description":"The block's name, as he named it"},""" +
    """"spindle":{"type":"string","description":"An address, written as text, with at most one point: 2, 2.3 or 1.111178719993; omit for the block's opening"},""" +
    """"pscale_attention":{"type":"integer","description":"How deep: omit to walk the path to the address; 0 with no address for every part's opening line; below the address's own depth for the lines beneath it"}"""

/** The walker's descriptions tried, by name: a description, or one made from the blocks his words named. */
private val FIXED = listOf(
    // As 0.12.1's part 3 first wrote it.
    "first" to """{"name":"bsp","description":"Walk one of his blocks, as the beach's bsp() walks a block: name the block, the address to walk to (spindle) and how deep (pscale_attention). With no address and no depth, the block's opening line and each part's opening line.","parameters":{"type":"object","properties":{$PARAMS_FIRST},"required":["block"]}}""",
    // Section 9's, word for word.
    "section 9" to """{"name":"bsp","description":"The beach's bsp(), on his phone: read one of his blocks, as the beach reads a block. Leave block empty to list his blocks. agent_id is pocket for his own blocks, pscale for the teaching blocks.","parameters":{"type":"object","properties":{""" +
        """"agent_id":{"type":"string","enum":["pocket","pscale"],"description":"pocket: his blocks; pscale: the teaching blocks"},""" +
        """"block":{"type":"string","description":"The block's name; empty to list them"},""" +
        """"spindle":{"type":"string","description":"An address, such as 1 or 2.3; omit for the root"},""" +
        """"pscale_attention":{"type":"integer","description":"How deep: omit for a walk; 0 with no spindle for every place's opening line"}},"required":[]}}""",
    // Section 9's opening words, 0.13's three parameters, nothing required.
    "read, on his phone" to """{"name":"bsp","description":"The beach's bsp(), on his phone: read one of his blocks, as the beach reads a block.","parameters":{"type":"object","properties":{""" +
        """"block":{"type":"string","description":"The block's name, as he named it"},""" +
        """"spindle":{"type":"string","description":"An address, such as 1 or 2.3; omit for the block's opening"},""" +
        """"pscale_attention":{"type":"integer","description":"How deep: omit for a walk; 0 with no spindle for every place's opening line"}},"required":[]}}""",
    // The same, the block required.
    "read, block required" to """{"name":"bsp","description":"The beach's bsp(), on his phone: read one of his blocks, as the beach reads a block.","parameters":{"type":"object","properties":{""" +
        """"block":{"type":"string","description":"The block's name, as he named it"},""" +
        """"spindle":{"type":"string","description":"An address, such as 1 or 2.3; omit for the block's opening"},""" +
        """"pscale_attention":{"type":"integer","description":"How deep: omit for a walk; 0 with no spindle for every place's opening line"}},"required":["block"]}}""",
)

private fun q(s: String) = "\"" + s.replace("\"", "\\\"") + "\""
private fun params(named: List<String>, spindle: String) = """"block":{"type":"string","enum":[${named.joinToString(",") { q(it) }}],"description":"The block his words named"},""" +
    """"spindle":{"type":"string","description":"$spindle"},""" +
    """"pscale_attention":{"type":"integer","description":"How deep: omit for a walk; 0 with no spindle for every place's opening line"}"""

/** Added after the first round: the walker described for the turn, naming the blocks his words named (the only blocks it walks). */
private val MADE: List<Pair<String, (List<String>) -> String>> = listOf(
    "named, on his phone" to { n -> """{"name":"bsp","description":"The beach's bsp(), on his phone: read his block ${n.joinToString(" or ")}, which his words named, as the beach reads a block.","parameters":{"type":"object","properties":{${params(n, "An address, such as 1 or 2.3; omit for the block's opening")}},"required":["block"]}}""" },
    "named, what it gives" to { n -> """{"name":"bsp","description":"Read his block ${n.joinToString(" or ")} on his phone, the block his words named: with no address, its opening line and each part's opening line; with an address, the line there and the lines above it. As the beach's bsp() reads a block.","parameters":{"type":"object","properties":{${params(n, "An address, written as text, with at most one point: 2, 2.3 or 1.111178719993; omit for the block's opening")}},"required":["block"]}}""" },
    "named, Sundial holds" to { n -> """{"name":"bsp","description":"Sundial holds his blocks on his phone, and his words named ${n.joinToString(" and ")}. Read it, as the beach's bsp() reads a block: no address for its opening line and each part's, or an address for the line there.","parameters":{"type":"object","properties":{${params(n, "An address, such as 1 or 2.3; omit for the block's opening")}},"required":["block"]}}""" },
)

/** Each message, and the block of his it names (as Walking.named reads it). */
private val MESSAGES = listOf(
    "What's left to do on my membership block?" to "membership",
    "Read me what's at 2.2 in my membership block." to "membership",
    "What did I decide at 1.111178719993 in my decisions block?" to "decisions",
    "What does my passport say about me?" to "passport:Phenomemental",
    "What's in my shell?" to "shell:Phenomemental",
    "Read me what's at 1.23456789 in my walk-check block." to "walk-check",
)

private val VARIANTS: List<Pair<String, (List<String>) -> String>> =
    (if (System.getProperty("pocket.only") == "made") emptyList() else FIXED.map { (n, j) -> n to { _: List<String> -> j } }) + MADE

fun main(args: Array<String>) {
    val out = File(args.getOrElse(1) { "trial/walk-probe.md" })
    val seeds = args.getOrNull(2)?.split(",")?.map { it.trim().toInt() } ?: listOf(0, 2)
    val now = ZonedDateTime.of(2026, 11, 4, 11, 0, 0, 0, ZoneId.of("Europe/London"))
    val window = com.phenomemental.pocketbeach.bsp.HerWindow.released(now)
    val r = StringBuilder("# The walker's words: does she call it? (raw)\n\nRun ${java.time.Instant.now()}, this machine's CPU, Gemma 4 E4B, thinking off, temperature 0.70, top-k 64, top-p 0.95; seeds ${seeds.joinToString()}. Her window: `$window`. Each message in a fresh conversation; her first answer read for a call.\n\n")
    val engine = Engine(EngineConfig(modelPath = args[0], backend = Backend.CPU(), maxNumTokens = 4096, cacheDir = System.getProperty("pocket.cache") ?: Files.createTempDirectory("wp").toString()))
    engine.initialize()
    val tally = linkedMapOf<String, Int>()
    engine.use { e ->
        for ((name, made) in VARIANTS) {
            r.append("## $name\n\n```\n${made(listOf("membership"))}\n```\n\n")
            for (seed in seeds) for ((said, block) in MESSAGES) {
                val json = made(listOf(block))
                val turn = com.phenomemental.pocketbeach.bsp.HerWindow.releasedTurn(said, now)
                val started = System.nanoTime()
                val m = e.createConversation(ConversationConfig(
                    systemInstruction = Contents.of(window),
                    tools = listOf(tool(object : OpenApiTool { override fun getToolDescriptionJsonString() = json; override fun execute(p0: String) = "unused" })),
                    automaticToolCalling = false, samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.7, seed = seed),
                    maxOutputToken = 80, thinkingConfig = ThinkingConfig(enableThinking = false),
                )).use { c -> runCatching { c.sendMessage(turn) } }
                val secs = (System.nanoTime() - started) / 1e9
                val call = m.getOrNull()?.toolCalls?.firstOrNull()
                if (call != null) tally[name] = (tally[name] ?: 0) + 1
                val said2 = m.fold({ x -> if (call != null) "called ${call.name} ${call.arguments.entries.joinToString { (k, v) -> "$k=${if (v is String) "“$v”" else "$v (${v?.let { it::class.simpleName }})"}" }}" else "no call: “${x.toString().trim().replace("\n", " / ").take(160)}”" }, { "the engine couldn't read it: ${it.message?.take(160)}" })
                r.append("- seed $seed, “$said”: $said2 (${"%.1f".format(secs)} s)\n")
                out.writeText(r.toString())
                println("$name, seed $seed, “$said”: $said2")
            }
            r.append("\n**Calls: ${tally[name] ?: 0} of ${seeds.size * MESSAGES.size}**\n\n")
            out.writeText(r.toString())
        }
    }
    r.append("## Calls, by wording\n\n" + VARIANTS.joinToString("\n") { (n, _) -> "- $n: ${tally[n] ?: 0} of ${seeds.size * MESSAGES.size}" } + "\n")
    out.writeText(r.toString())
}
