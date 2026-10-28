package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.LogSeverity
import com.google.ai.edge.litertlm.ModelInfo
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.DirShelfSource
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.MemoryStorage
import com.phenomemental.pocketbeach.bsp.Pages
import com.phenomemental.pocketbeach.bsp.PocketBsp
import com.phenomemental.pocketbeach.bsp.Shelf
import com.phenomemental.pocketbeach.bsp.Shells
import com.phenomemental.pocketbeach.bsp.Talk
import com.phenomemental.pocketbeach.bsp.collectUnderscore
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.parseJson
import java.io.File
import java.nio.file.Files

/** Ways Matthew might ask for the hello world, and the secret each carries. */
private val ASKS = listOf(
    "Do the hello world. My secret is: the moon hums in B flat." to "the moon hums in B flat",
    "hello world please, secret is purple walrus" to "purple walrus",
    "Make the hello world block. My fun secret: I once ate 14 pancakes." to "I once ate 14 pancakes",
    "Let's do the hello world test! Secret = otters hold hands when they sleep" to "otters hold hands when they sleep",
    "hello world my secret is bananas are berries" to "bananas are berries",
    "Can you run the hello world? The secret is that I'm afraid of geese." to "I'm afraid of geese",
    "Time for the hello world. Secret: my first car was called Dennis" to "my first car was called Dennis",
    "please do hello-world, secret: 42 is not the answer" to "42 is not the answer",
)

@OptIn(ExperimentalApi::class)
fun main(args: Array<String>) {
    val model = args.getOrNull(0) ?: error("Give the model file: --args=\"<model.litertlm> [runs]\"")
    val runs = args.getOrNull(1)?.toInt() ?: 4
    val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "shelf")))
    Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
    val cache = Files.createTempDirectory("litertlm").toString()
    if (args.getOrNull(2) == "timing") {
        ModelInfo.from(model).use { info ->
            println("model: ${info.modelType}; context ${info.maxContextTokens()}; " + ((info as? ModelInfo.Llm)?.let { "speculative decoding: ${it.hasSpeculativeDecodingSupport()}" } ?: ""))
        }
        ExperimentalFlags.enableBenchmark = true
        ExperimentalFlags.enableSpeculativeDecoding = args.getOrNull(3) == "mtp"
        println("speculative decoding asked: ${ExperimentalFlags.enableSpeculativeDecoding}")
        val engine = Engine(EngineConfig(modelPath = model, backend = Backend.CPU(), maxNumTokens = Asks.Tuning.MAX_TOKENS, cacheDir = cache))
        val loadStart = System.nanoTime()
        engine.initialize()
        println("Loaded in ${secs(loadStart)} s.")
        if (args.getOrNull(3) != "mtp") timing(engine, shelf, formatted = true)
        timing(engine, shelf, formatted = false)
        engine.close()
        return
    }
    // "existing": each run starts with a hello-world already made by hand; "mtp": speculative decoding, the answer's form not held.
    val flags = args.drop(2).toSet()
    val mtp = "mtp" in flags
    ExperimentalFlags.enableBenchmark = true
    ExperimentalFlags.enableSpeculativeDecoding = mtp
    val loadStart = System.nanoTime()
    val engine = Engine(EngineConfig(modelPath = model, backend = Backend.CPU(), maxNumTokens = Asks.Tuning.MAX_TOKENS, cacheDir = cache))
    engine.initialize()
    println("Loaded in ${secs(loadStart)} s." + (if (mtp) " Speculative decoding, the form not held." else "") + (if ("existing" in flags) " Each run begins with a hello-world made before." else ""))
    var passed = 0
    var exchanges = 0
    for (run in 1..runs) {
        val (question, secret) = ASKS[(run - 1) % ASKS.size]
        var now = 1_793_178_000_000L + run * 60_000L
        val storage = MemoryStorage()
        val pages = Pages(storage, shelf, PocketBsp(storage, shelf) { now }) { now }
        HelloWorld.makeShell(pages)
        if ("existing" in flags) {
            HelloWorld.makeByHand(pages, storage, HelloWorld.SHELL_OPENING, "an old secret", HelloWorld.seal())
            now += 60_000
        }
        val worn = Shells(storage, shelf).wear(HelloWorld.SHELL, now)
        val system = Asks.instruction(HelloWorld.SHELL, worn.window, Asks.plainWindow(pages, HelloWorld.SHELL), pages)
        val seal = HelloWorld.seal()
        val config = ConversationConfig(
            systemInstruction = Contents.of(system),
            samplerConfig = SamplerConfig(topK = Asks.Tuning.TOP_K, topP = Asks.Tuning.TOP_P, temperature = Asks.Tuning.TEMPERATURE, seed = run),
            maxOutputToken = Asks.Tuning.MAX_OUTPUT,
            thinkingConfig = ThinkingConfig(enableThinking = false),
            enableResponseFormat = !mtp,
        )
        println("\n── run $run: $question")
        if (run == 1) println("instruction (${system.length} chars):\n$system\n")
        val start = System.nanoTime()
        val problems = engine.createConversation(config).use { conversation ->
            val talk = Talk { message ->
                val t = System.nanoTime()
                val reply = (if (mtp) conversation.sendMessage(message) else conversation.sendMessage(message, responseFormat = ResponseFormat.json(Asks.SCHEMA))).toString()
                val b = conversation.getBenchmarkInfo()
                println("exchange: ${secs(t)} s; read ${b.lastPrefillTokenCount} tokens at %.0f/s, wrote ${b.lastDecodeTokenCount} at %.1f/s".format(b.lastPrefillTokensPerSecond, b.lastDecodeTokensPerSecond))
                reply
            }
            val asked = Asks.ask(question, talk, pages, storage, shelf, HelloWorld.SHELL_OPENING, seal) { now }
            exchanges += asked.exchanges.size
            for ((i, e) in asked.exchanges.withIndex()) println("answer ${i + 1}: " + e.second.replace(Regex("\\s*\\n\\s*"), " ").trim())
            asked.proposal?.writes?.forEach { w -> println("  ${if (w.step.byApp) "app" else "she"}: ${w.step.act} ${w.plan.block} at ${w.plan.landsAt}: ${w.shown}") }
            val proposal = asked.proposal ?: return@use listOf(asked.problem ?: "no proposal")
            proposal.refusal?.let { return@use listOf("refused: $it") }
            now += 5_000
            val carried = Asks.carryOut(proposal, pages, storage, HelloWorld.SHELL_OPENING)
            carried.problem?.let { return@use listOf("not carried: $it") }
            val stored = storage.read(HelloWorld.BLOCK)
            val found = HelloWorld.check(stored, HelloWorld.SHELL_OPENING, seal).toMutableList()
            val block = stored?.let { parseJson(it) as JsObj }
            val secretLine = block?.let { b -> b.keys().mapNotNull { collectUnderscore(b[it]) }.firstOrNull { it.startsWith("Secret: ") } }
            if (secretLine != null && !secretLine.contains(secret, ignoreCase = true)) found += "the secret line reads \"$secretLine\""
            val post = carried.post
            if (post == null) found += "no post offered (${carried.postRefusal ?: proposal.postRefusal ?: "none"})"
            else {
                val print = carried.landed.lastOrNull { it.print != null }?.print ?: ""
                found += HelloWorld.checkMessage(post.message, HelloWorld.SHELL_OPENING, seal, print)
                if (post.message != HelloWorld.messageFor(stored)) found += "the message isn't the block's own"
                if (!post.message.contains(secret, ignoreCase = true)) found += "the message doesn't carry the secret"
                println("post to pool:${post.pool}: ${post.message}")
            }
            println("block: $stored")
            found
        }
        println("took ${secs(start)} s: " + if (problems.isEmpty()) "PASS" else "FAIL — " + problems.joinToString("; "))
        if (problems.isEmpty()) passed++
    }
    println("\n$passed of $runs passed, in $exchanges exchanges.")
    if ("ordinary" in flags) ordinary(engine, shelf, mtp)
    engine.close()
}

/** Ordinary asks of a small pocket, printed as answered and as planned, for reading by eye. */
private fun ordinary(engine: Engine, shelf: Shelf, mtp: Boolean) {
    val asks = listOf(
        "What's in my garden block?",
        "Add a line to garden: buy more compost.",
        "In garden, under the autumn line, note that the tulips came from Holland.",
        "What does the whetstone say at line 2?",
    )
    for (question in asks) {
        val now = 1_793_178_000_000L
        val storage = MemoryStorage()
        val pages = Pages(storage, shelf, PocketBsp(storage, shelf) { now }) { now }
        HelloWorld.makeShell(pages)
        pages.carryOut(pages.planNewBlock("garden"))
        pages.carryOut(pages.planChange("garden", "", "The garden, through the year"))
        pages.carryOut(pages.planAdd("garden", "", "Autumn: plant the bulbs"))
        pages.carryOut(pages.planAdd("garden", "", "Winter: prune the roses"))
        val worn = Shells(storage, shelf).wear(HelloWorld.SHELL, now)
        val system = Asks.instruction(HelloWorld.SHELL, worn.window, Asks.plainWindow(pages, HelloWorld.SHELL), pages)
        val config = ConversationConfig(
            systemInstruction = Contents.of(system),
            samplerConfig = SamplerConfig(topK = Asks.Tuning.TOP_K, topP = Asks.Tuning.TOP_P, temperature = Asks.Tuning.TEMPERATURE, seed = 1),
            maxOutputToken = Asks.Tuning.MAX_OUTPUT,
            thinkingConfig = ThinkingConfig(enableThinking = false),
            enableResponseFormat = !mtp,
        )
        println("\n── ordinary: $question")
        val start = System.nanoTime()
        engine.createConversation(config).use { conversation ->
            val talk = Talk { message -> (if (mtp) conversation.sendMessage(message) else conversation.sendMessage(message, responseFormat = ResponseFormat.json(Asks.SCHEMA))).toString() }
            val asked = Asks.ask(question, talk, pages, storage, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal()) { now }
            for ((i, e) in asked.exchanges.withIndex()) println("answer ${i + 1}: " + e.second.replace(Regex("\\s*\\n\\s*"), " ").trim())
            asked.problem?.let { println("problem: $it") }
            asked.proposal?.let { p ->
                p.writes.forEach { w -> println("would write: ${w.plan.block} at ${w.plan.landsAt}: ${w.shown}") }
                p.refusal?.let { println("refused: $it") }
            }
        }
        println("took ${secs(start)} s")
    }
}

/** One hello world, timed exchange by exchange from the engine's own count: how long the prompt is, and how fast it is read and answered. */
@OptIn(ExperimentalApi::class)
private fun timing(engine: Engine, shelf: Shelf, formatted: Boolean) {
    val (question, _) = ASKS[0]
    val now = 1_793_178_000_000L
    val storage = MemoryStorage()
    val pages = Pages(storage, shelf, PocketBsp(storage, shelf) { now }) { now }
    HelloWorld.makeShell(pages)
    val worn = Shells(storage, shelf).wear(HelloWorld.SHELL, now)
    val system = Asks.instruction(HelloWorld.SHELL, worn.window, Asks.plainWindow(pages, HelloWorld.SHELL), pages)
    val config = ConversationConfig(
        systemInstruction = Contents.of(system),
        samplerConfig = SamplerConfig(topK = Asks.Tuning.TOP_K, topP = Asks.Tuning.TOP_P, temperature = Asks.Tuning.TEMPERATURE, seed = 1),
        maxOutputToken = Asks.Tuning.MAX_OUTPUT,
        thinkingConfig = ThinkingConfig(enableThinking = false),
        enableResponseFormat = formatted,
    )
    println("\n── timing, ${if (formatted) "held to the answer's form" else "free"}: instruction ${system.length} chars")
    val start = System.nanoTime()
    engine.createConversation(config).use { conversation ->
        val talk = Talk { message ->
            val t = System.nanoTime()
            val reply = (if (formatted) conversation.sendMessage(message, responseFormat = ResponseFormat.json(Asks.SCHEMA)) else conversation.sendMessage(message)).toString()
            val b = conversation.getBenchmarkInfo()
            println("exchange: ${secs(t)} s; read ${b.lastPrefillTokenCount} tokens at %.0f/s, wrote ${b.lastDecodeTokenCount} at %.1f/s, first token after %.2f s; ${conversation.getTokenCount()} tokens so far"
                .format(b.lastPrefillTokensPerSecond, b.lastDecodeTokensPerSecond, b.timeToFirstTokenInSecond))
            reply
        }
        val asked = Asks.ask(question, talk, pages, storage, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal()) { now }
        for ((i, e) in asked.exchanges.withIndex()) println("answer ${i + 1}: " + e.second.replace(Regex("\\s*\\n\\s*"), " ").trim())
        println("problem: ${asked.problem}; refusal: ${asked.proposal?.refusal}")
    }
    println("took ${secs(start)} s")
}

private fun secs(since: Long) = "%.1f".format((System.nanoTime() - since) / 1e9)
