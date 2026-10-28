package com.phenomemental.pocketbeach.model

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Modality
import com.google.ai.edge.litertlm.ModelInfo
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.Talk
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The companion's mind: Gemma 4 E2B, run on this phone by LiteRT-LM. It is
 * loaded only when Matthew asks something, and let go after a few idle
 * minutes or whenever Android asks for memory back (decision 13). Its GPU is
 * tried first, the CPU if the GPU can't take it, and why the GPU couldn't is
 * kept to be shown. Each Ask's [Pace] says where it ran and how fast.
 */
class Gemma(private val store: ModelStore, private val cacheDir: String, private val nativeLibraryDir: String) : Mind {
    companion object {
        const val IDLE_MS = 3 * 60_000L

        /**
         * Speculative decoding: off. On Matthew's Pixel 9 Pro XL (Tensor G4,
         * GPU) it wrote 13.2 tokens a second against 13.9 without, and with
         * it on the engine can't hold a reply to a form. The bench still
         * times it, on and off.
         */
        const val SPECULATIVE = false
    }

    /** One thread for every call into the engine, so it is never used from two at once. */
    private val thread = Executors.newSingleThreadExecutor { r -> Thread(r, "gemma").apply { isDaemon = true } }.asCoroutineDispatcher()
    private val main = Handler(Looper.getMainLooper())
    private val letGo = Runnable { release() }

    @Volatile private var engine: Engine? = null

    /** Why holding her first word failed on this phone, once it has; her replies are then free for the rest of the app's run. */
    @Volatile private var whyNotHeld: String? = null

    /** Conversations open now; the model is let go only when none is. */
    private var open = 0
    private var letGoWhenDone = false

    /** Which of the phone's processors runs it, once loaded: "GPU" or "CPU". */
    @Volatile override var processor: String? = null
        private set

    /** Why the GPU couldn't run it, when it runs on the CPU. */
    @Volatile private var whyNotGpu: String? = null

    override val loaded: Boolean get() = engine != null

    override fun state(): ModelStore.State = store.state()

    override suspend fun warm(): Long? = withContext(thread) {
        if (engine != null) return@withContext null
        engineNow().second
    }

    /** The engine, loaded if it isn't; how long loading took, if it had to. */
    private fun engineNow(): Pair<Engine, Long?> {
        engine?.let { return it to null }
        val started = System.nanoTime()
        val e = load(SPECULATIVE)
        engine = e
        return e to (System.nanoTime() - started) / 1_000_000
    }

    override suspend fun talker(instruction: String, history: List<Pair<String, String>>, held: Boolean): Mind.Talker = withContext(thread) {
        main.removeCallbacks(letGo)
        val (e, loadMs) = engineNow()
        val holding = held && whyNotHeld == null
        synchronized(this@Gemma) { open++ }
        Talker(e, instruction, history, holding, conversation(e, instruction, history, holding), Pace(processor, whyNotGpu, loadMs))
    }

    /** A conversation of 0.4's: the instruction and the earlier exchanges given, her replies held to begin with the route word if [held]. */
    private fun conversation(e: Engine, instruction: String, history: List<Pair<String, String>>, held: Boolean): Conversation = e.createConversation(
        ConversationConfig(
            systemInstruction = Contents.of(instruction),
            initialMessages = history.flatMap { (asked, said) -> listOf(Message.user(asked), Message.model(said)) },
            samplerConfig = SamplerConfig(topK = Asks.Tuning.TOP_K, topP = Asks.Tuning.TOP_P, temperature = Asks.Tuning.TEMPERATURE, seed = 0),
            maxOutputToken = Asks.Tuning.MAX_OUTPUT,
            thinkingConfig = ThinkingConfig(enableThinking = false),
            enableResponseFormat = held,
        ),
    )

    /**
     * 0.4's conversation: each reply streamed, timed from the message to its
     * first piece, and held to begin with her route word. Should holding
     * fail before anything comes, she is asked again free, in a conversation
     * opened afresh with the exchanges so far, and stays free for the rest of
     * the app's run.
     */
    inner class Talker(
        private val engine: Engine,
        private val instruction: String,
        private val history: List<Pair<String, String>>,
        private var held: Boolean,
        private var conversation: Conversation,
        override val pace: Pace,
    ) : Mind.Talker {
        private val done = mutableListOf<Pair<String, String>>()

        override val route: String get() = if (held) "held by the engine" else "free" + (whyNotHeld?.let { ": holding it failed on this phone ($it)" } ?: "")

        override val stream = com.phenomemental.pocketbeach.bsp.Talks.Stream { message, piece ->
            kotlinx.coroutines.runBlocking(thread) {
                val started = System.nanoTime()
                var first: Double? = null
                val whole = StringBuilder()
                suspend fun send() {
                    val flow = if (held) conversation.sendMessageAsync(message, responseFormat = ResponseFormat.regex(com.phenomemental.pocketbeach.bsp.Talks.HELD))
                    else conversation.sendMessageAsync(message)
                    flow.collect { m ->
                        if (first == null) first = (System.nanoTime() - started) / 1e9
                        val text = m.toString()
                        whole.append(text)
                        piece(text)
                    }
                }
                try {
                    send()
                } catch (t: Throwable) {
                    if (!held || first != null || t is kotlinx.coroutines.CancellationException) throw t
                    Log.w("Gemma", "Holding her first word failed; she answers free from now on.", t)
                    whyNotHeld = firstLine(t)
                    held = false
                    runCatching { conversation.close() }
                    conversation = conversation(engine, instruction, history + done, held = false)
                    send()
                }
                counted(conversation)?.let { t -> pace.turns.add(Pace.Turn(t.read, t.readPerSecond, t.wrote, t.writePerSecond, first)) }
                whole.toString().also { done += message to it }
            }
        }

        override fun stop() { runCatching { conversation.cancelProcess() } }

        override fun close() {
            kotlinx.coroutines.runBlocking(thread) { runCatching { conversation.close() } }
            val now = synchronized(this@Gemma) {
                open--
                letGoWhenDone && open == 0
            }
            if (now) release() else main.postDelayed(letGo, IDLE_MS)
        }
    }

    override suspend fun chat(instruction: String): Mind.Chat = withContext(thread) {
        main.removeCallbacks(letGo)
        val (e, loadMs) = engineNow()
        val conversation = e.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(instruction),
                samplerConfig = SamplerConfig(topK = Asks.Tuning.TOP_K, topP = Asks.Tuning.TOP_P, temperature = Asks.Tuning.TEMPERATURE, seed = 0),
                maxOutputToken = Asks.Tuning.MAX_OUTPUT,
                thinkingConfig = ThinkingConfig(enableThinking = false),
                enableResponseFormat = true,
            ),
        )
        synchronized(this@Gemma) { open++ }
        Chat(conversation, Pace(processor, whyNotGpu, loadMs))
    }

    inner class Chat(private val conversation: Conversation, override val pace: Pace) : Mind.Chat {
        override val talk = Talk { message ->
            kotlinx.coroutines.runBlocking(thread) {
                conversation.sendMessage(message, responseFormat = ResponseFormat.json(Asks.SCHEMA)).toString()
                    .also { counted(conversation)?.let(pace.turns::add) }
            }
        }

        override fun stop() { runCatching { conversation.cancelProcess() } }

        override fun close() {
            kotlinx.coroutines.runBlocking(thread) { runCatching { conversation.close() } }
            val now = synchronized(this@Gemma) {
                open--
                letGoWhenDone && open == 0
            }
            if (now) release() else main.postDelayed(letGo, IDLE_MS)
        }
    }

    /** The last exchange as the engine counted it: tokens read and written, and how fast; null when it didn't count. */
    @OptIn(ExperimentalApi::class)
    private fun counted(conversation: Conversation): Pace.Turn? = runCatching {
        val b = conversation.getBenchmarkInfo()
        Pace.Turn(b.lastPrefillTokenCount, b.lastPrefillTokensPerSecond, b.lastDecodeTokenCount, b.lastDecodeTokensPerSecond, b.timeToFirstTokenInSecond)
    }.getOrNull()?.takeIf { it.read > 0 || it.wrote > 0 }

    /** An engine on [backend], counting each exchange if [counting], with speculative decoding asked for or not ([speculative] null: the engine's default). */
    @OptIn(ExperimentalApi::class)
    private fun open(backend: Backend, counting: Boolean, speculative: Boolean? = null): Engine {
        // Both are read only as the engine is made.
        ExperimentalFlags.enableBenchmark = counting
        @Suppress("DEPRECATION")
        ExperimentalFlags.enableSpeculativeDecoding = speculative
        val e = Engine(EngineConfig(modelPath = store.file.path, backend = backend, maxNumTokens = Asks.Tuning.MAX_TOKENS, cacheDir = cacheDir))
        try {
            e.initialize()
        } catch (t: Throwable) {
            runCatching { e.close() }
            throw t
        } finally {
            ExperimentalFlags.enableBenchmark = false
            @Suppress("DEPRECATION")
            ExperimentalFlags.enableSpeculativeDecoding = null
        }
        return e
    }

    private fun load(speculative: Boolean = false): Engine {
        return try {
            open(Backend.GPU(), counting = true, speculative = speculative).also { processor = "GPU"; whyNotGpu = null }
        } catch (t: Throwable) {
            Log.w("Gemma", "The GPU couldn't load the model.", t)
            // Counting is the one thing asked of the GPU beyond what 0.3.1 asked, so it is tried once more without it before the CPU.
            runCatching { open(Backend.GPU(), counting = false, speculative = speculative) }.getOrNull()?.also { processor = "GPU"; whyNotGpu = null }
                ?: open(Backend.CPU(), counting = true, speculative = speculative).also {
                    processor = "CPU"
                    whyNotGpu = t.message?.lineSequence()?.firstOrNull { line -> line.isNotBlank() }?.trim()?.take(240) ?: t::class.java.simpleName
                }
        }
    }

    /**
     * What the bench asks of the engine itself, a line each: what the model
     * file says it runs on; a plain answer timed on the GPU (or the CPU) with
     * speculative decoding off, then on; and the NPU, where the file has a
     * build for it. (Whether her first word can be held, the bench's 0.4
     * run says.) The model is let go first and after, so the next
     * Ask loads it as ever. [step] says what is being tried.
     */
    suspend fun probes(step: (String) -> Unit): List<String> = withContext(thread) {
        check(synchronized(this@Gemma) { open == 0 }) { "An Ask is open; the bench tries the engine only when none is." }
        main.removeCallbacks(letGo)
        engine?.let { runCatching { it.close() } }
        engine = null
        processor = null
        val lines = mutableListOf<String>()
        val info = runCatching { ModelInfo.from(store.file.path) }.getOrNull()
        val backends = info?.let { runCatching { it.supportedBackends(Modality.TEXT) }.getOrNull() }.orEmpty()
        val speculativeInFile = (info as? ModelInfo.Llm)?.let { runCatching { it.hasSpeculativeDecodingSupport() }.getOrNull() }
        lines += "The model file runs on: ${backends.joinToString { it.name }.ifEmpty { "(it doesn't say)" }}; " +
            "speculative decoding ${if (speculativeInFile == true) "built in" else "not built in"}" +
            (info?.let { i -> runCatching { i.socName(Modality.TEXT) }.getOrNull()?.takeIf { it.isNotBlank() }?.let { "; built for $it" } }.orEmpty()) + "."
        runCatching { info?.close() }

        val gpuFirst = listOf("GPU" to { Backend.GPU() }, "CPU" to { Backend.CPU() })
        for (speculative in listOf(false, true)) {
            if (speculative && speculativeInFile != true) break
            step(if (speculative) "Timing a plain answer, speculative decoding on" else "Timing a plain answer, speculative decoding off")
            var made: Pair<String, Engine>? = null
            var why: String? = null
            val loadStarted = System.nanoTime()
            for ((name, backend) in gpuFirst) {
                made = runCatching { name to open(backend(), counting = true, speculative = speculative) }.onFailure { why = firstLine(it) }.getOrNull()
                if (made != null) break
            }
            val loadMs = (System.nanoTime() - loadStarted) / 1_000_000
            val (where, e) = made ?: run {
                lines += "Speculative decoding ${if (speculative) "on" else "off"}: the model wouldn't load ($why)."
                continue
            }
            lines += "Speculative decoding ${if (speculative) "on" else "off"}, on the $where (loaded in ${secs(loadMs)} s): " + plainAnswer(e)
            runCatching { e.close() }
        }

        step("Trying the NPU")
        lines += if (backends.any { it.name == "NPU" }) {
            val loadStarted = System.nanoTime()
            runCatching { open(Backend.NPU(nativeLibraryDir), counting = true) }.fold(
                onSuccess = { e -> ("The NPU (loaded in ${secs((System.nanoTime() - loadStarted) / 1_000_000)} s): " + plainAnswer(e)).also { runCatching { e.close() } } },
                onFailure = { "The NPU wouldn't load the model: ${firstLine(it)}." },
            )
        } else {
            "The NPU: this model file has no NPU build. Builds of it for the NPU exist only for Google Tensor G5 and G6; " +
                "this phone's chip is ${android.os.Build.SOC_MANUFACTURER} ${android.os.Build.SOC_MODEL}."
        }
        lines
    }

    /** A plain paragraph, streamed, timed from the ask to its first piece and to its end, with the engine's own count. */
    @OptIn(ExperimentalApi::class)
    private suspend fun plainAnswer(e: Engine): String = runCatching {
        e.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of("You are a companion on a phone. Answer in plain words."),
                samplerConfig = SamplerConfig(topK = Asks.Tuning.TOP_K, topP = Asks.Tuning.TOP_P, temperature = Asks.Tuning.TEMPERATURE, seed = 0),
                maxOutputToken = 300,
                thinkingConfig = ThinkingConfig(enableThinking = false),
            ),
        ).use { c ->
            val started = System.nanoTime()
            var first: Long? = null
            var pieces = 0
            c.sendMessageAsync("Tell me about the sea at night, in one plain paragraph of about 120 words.").collect {
                if (first == null) first = (System.nanoTime() - started) / 1_000_000
                pieces++
            }
            val total = (System.nanoTime() - started) / 1_000_000
            val b = c.getBenchmarkInfo()
            "first word after ${secs(first ?: total)} s, ${b.lastDecodeTokenCount} tokens written at ${one(b.lastDecodeTokensPerSecond)} a second, " +
                "${secs(total)} s in all, in $pieces pieces."
        }
    }.getOrElse { "it failed: ${firstLine(it)}." }

    private fun firstLine(t: Throwable) = t.message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()?.take(240) ?: t::class.java.simpleName

    private fun secs(ms: Long) = String.format(Locale.UK, "%.1f", ms / 1000.0)

    private fun one(x: Double) = String.format(Locale.UK, "%.1f", x)

    /** Let the model go, freeing its memory, once no conversation is open; it loads again on the next Ask. */
    fun release() {
        main.removeCallbacks(letGo)
        synchronized(this) {
            if (open > 0) {
                letGoWhenDone = true
                return
            }
            letGoWhenDone = false
        }
        val e = engine ?: return
        engine = null
        processor = null
        thread.executor.execute { runCatching { e.close() } }
    }
}
