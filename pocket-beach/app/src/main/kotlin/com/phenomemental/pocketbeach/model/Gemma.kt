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
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.Talk
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
class Gemma(private val store: ModelStore, private val cacheDir: String) : Mind {
    companion object {
        const val IDLE_MS = 3 * 60_000L
    }

    /** One thread for every call into the engine, so it is never used from two at once. */
    private val thread = Executors.newSingleThreadExecutor { r -> Thread(r, "gemma").apply { isDaemon = true } }.asCoroutineDispatcher()
    private val main = Handler(Looper.getMainLooper())
    private val letGo = Runnable { release() }

    @Volatile private var engine: Engine? = null

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

    override suspend fun chat(instruction: String): Mind.Chat = withContext(thread) {
        main.removeCallbacks(letGo)
        var loadMs: Long? = null
        val e = engine ?: run {
            val started = System.nanoTime()
            load().also { engine = it; loadMs = (System.nanoTime() - started) / 1_000_000 }
        }
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
        Pace.Turn(b.lastPrefillTokenCount, b.lastPrefillTokensPerSecond, b.lastDecodeTokenCount, b.lastDecodeTokensPerSecond)
    }.getOrNull()?.takeIf { it.read > 0 || it.wrote > 0 }

    @OptIn(ExperimentalApi::class)
    private fun load(): Engine {
        val path = store.file.path
        fun open(backend: Backend, counting: Boolean): Engine {
            // The engine counts each exchange's tokens and speed only if told to as it is made.
            ExperimentalFlags.enableBenchmark = counting
            val e = Engine(EngineConfig(modelPath = path, backend = backend, maxNumTokens = Asks.Tuning.MAX_TOKENS, cacheDir = cacheDir))
            try {
                e.initialize()
            } catch (t: Throwable) {
                runCatching { e.close() }
                throw t
            } finally {
                ExperimentalFlags.enableBenchmark = false
            }
            return e
        }
        return try {
            open(Backend.GPU(), counting = true).also { processor = "GPU"; whyNotGpu = null }
        } catch (t: Throwable) {
            Log.w("Gemma", "The GPU couldn't load the model.", t)
            // Counting is the one thing asked of the GPU beyond what 0.3.1 asked, so it is tried once more without it before the CPU.
            runCatching { open(Backend.GPU(), counting = false) }.getOrNull()?.also { processor = "GPU"; whyNotGpu = null }
                ?: open(Backend.CPU(), counting = true).also {
                    processor = "CPU"
                    whyNotGpu = t.message?.lineSequence()?.firstOrNull { line -> line.isNotBlank() }?.trim()?.take(240) ?: t::class.java.simpleName
                }
        }
    }

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
