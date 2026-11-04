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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The companion's mind: Gemma 4 E2B, run on this phone by LiteRT-LM. It is
 * loaded when Matthew first asks something and kept (Sundial 0.10.1: never
 * let go after idle minutes), until Android is really short of memory
 * (decision 13). Its GPU is
 * tried first, the CPU if the GPU can't take it, and why the GPU couldn't is
 * kept to be shown. Each Ask's [Pace] says where it ran and how fast.
 *
 * 0.5: each job runs on the engine its settings ask for ([JobSettings]):
 * jobs that agree on the model, the processor and the context share one, and
 * a job set apart has its own, loaded beside it.
 *
 * 0.6.2: never two engines on the GPU (the app settles which job's engine
 * holds it, [Gpu]; here, should a second ever be asked for, the first is let
 * go if it is idle, else the second goes on the CPU). An answer that fails on
 * the GPU closes that engine, loads the model on the CPU and is answered once
 * more there; that engine stays on the CPU until the app starts again.
 */
class Gemma(private val stores: (String) -> ModelStore, private val cacheDir: String, private val nativeLibraryDir: String) : Mind {
    /** Gemma 4 E2B's store: the model Ask needs, and the bench's. */
    private val store: ModelStore get() = stores(ModelStore.E2B.name)

    companion object {

        /**
         * Speculative decoding: off. On Matthew's Pixel 9 Pro XL (Tensor G4,
         * GPU) it wrote 13.2 tokens a second against 13.9 without, and with
         * it on the engine can't hold a reply to a form. The bench still
         * times it, on and off.
         */
        const val SPECULATIVE = false
    }

    /** One thread for every call into the engines, so none is ever used from two at once. */
    private val thread = Executors.newSingleThreadExecutor { r -> Thread(r, "gemma").apply { isDaemon = true } }.asCoroutineDispatcher()
    private val main = Handler(Looper.getMainLooper())
    private val letGo = Runnable { release() }

    /** An engine as loaded: on which processor, and why not the GPU when the GPU was asked for and couldn't. */
    private class Loaded(val engine: Engine, val processor: String, val whyNotGpu: String?)

    /** The engines loaded now, one for each set of model, processor and context the jobs ask for. */
    private val engines = ConcurrentHashMap<EngineKey, Loaded>()

    /** Engines an answer failed on, on the GPU: they run on the CPU until the app starts again. */
    private val failedOnGpu = ConcurrentHashMap.newKeySet<EngineKey>()

    override fun gpuFailed(settings: JobSettings): Boolean = settings.engine in failedOnGpu

    /** The one processor the bench has asked for ("CPU"), or null: as each job's settings say. */
    @Volatile private var onlyOn: String? = null

    override suspend fun only(processor: String?) = withContext(thread) {
        check(synchronized(this@Gemma) { open == 0 }) { "An Ask is open; the engine changes processor only when none is." }
        main.removeCallbacks(letGo)
        closeAll()
        onlyOn = processor
    }

    /** Close every engine. Called on [thread]. */
    private fun closeAll() {
        val all = engines.values.toList()
        engines.clear()
        this.processor = null
        for (l in all) runCatching { l.engine.close() }
    }

    /** Why holding her first word failed on this phone, once it has; her replies are then free for the rest of the app's run. */
    @Volatile private var whyNotHeld: String? = null

    /** Conversations open now; the model is let go only when none is. */
    private var open = 0
    private var letGoWhenDone = false

    /** Which of the phone's processors ran the last engine asked for: "GPU" or "CPU". */
    @Volatile override var processor: String? = null
        private set

    override val loaded: Boolean get() = engines.isNotEmpty()

    /** Sundial 0.10.1: each step of the answers under way, timed. */
    override val steps = Steps()

    /** Whether the engine [settings] run on is in memory now. */
    override fun loaded(settings: JobSettings): Boolean = engines.containsKey(key(settings))

    override fun state(): ModelStore.State = store.state()

    override fun state(model: String): ModelStore.State = stores(model).state()

    override suspend fun warm(settings: JobSettings): Long? = withContext(thread) {
        engineNow(key(settings)).second
    }

    /** The engine [settings] run on, the bench's one processor taking the place of theirs, and the CPU once the GPU has failed it. */
    private fun key(settings: JobSettings): EngineKey = settings.engine.let { k -> onlyOn?.let { k.copy(processor = it) } ?: k }
        .let { k -> if (k.processor == "GPU" && k in failedOnGpu) k.copy(processor = "CPU") else k }

    /** The engine for [key], loaded if it isn't; how long loading took, if it had to. Called on [thread]. */
    private fun engineNow(key: EngineKey): Pair<Loaded, Long?> {
        engines[key]?.let { processor = it.processor; return it to null }
        val started = System.nanoTime()
        val onGpu = engines.filter { (k, l) -> k != key && l.processor == "GPU" }
        val l = when {
            key.processor != "GPU" || onGpu.isEmpty() -> load(key, SPECULATIVE)
            // Never two on the GPU: the one there goes first if nothing is open on it, else this one runs on the CPU.
            synchronized(this) { open == 0 } -> {
                for ((k, other) in onGpu) { engines.remove(k); runCatching { other.engine.close() } }
                load(key, SPECULATIVE)
            }
            else -> Loaded(open(Backend.CPU(), counting = true, speculative = SPECULATIVE, context = key.context, model = key.model), "CPU", "another engine holds the GPU")
        }
        engines[key] = l
        processor = l.processor
        val ms = (System.nanoTime() - started) / 1_000_000
        steps.add("${key.model} loaded on the ${l.processor}", ms)
        return l to ms
    }

    override suspend fun talker(instruction: String, history: List<Pair<String, String>>, held: Boolean, settings: JobSettings): Mind.Talker = withContext(thread) {
        main.removeCallbacks(letGo)
        val key = key(settings)
        val (l, loadMs) = engineNow(key)
        if (loadMs == null) steps.note("${key.model} already in memory, on the ${l.processor}")
        val holding = held && whyNotHeld == null
        val c = conversation(l.engine, instruction, history, holding, settings)
        synchronized(this@Gemma) { open++ }
        Talker(key, l.engine, l.processor, instruction, history, holding, settings, c, Pace(l.processor, l.whyNotGpu, loadMs))
    }

    /**
     * An answer failed on the GPU (0.6.2): that engine is closed, and the
     * model loaded on the CPU, where it stays until the app starts again.
     * Called on [thread], with nothing of the engine's still being written.
     */
    private fun ontoCpu(key: EngineKey): Loaded {
        failedOnGpu += key
        engines.remove(key)?.let { runCatching { it.engine.close() } }
        return engineNow(key.copy(processor = "CPU")).first
    }

    /** A conversation: the instruction and the earlier exchanges given, drawn as [settings] say, her replies held to begin with the route word if [held]. */
    private fun conversation(e: Engine, instruction: String, history: List<Pair<String, String>>, held: Boolean, settings: JobSettings): Conversation {
        val started = System.nanoTime()
        return e.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(instruction),
                initialMessages = history.flatMap { (asked, said) -> listOf(Message.user(asked), Message.model(said)) },
                samplerConfig = sampler(settings),
                maxOutputToken = settings.longestReply,
                thinkingConfig = ThinkingConfig(enableThinking = false),
                enableResponseFormat = held,
            ),
        ).also { steps.add("her conversation opened, with ${history.size} exchange${if (history.size == 1) "" else "s"} before", Steps.since(started)) }
    }

    /** As Edge draws: the seed left at 0, as Edge leaves it. */
    private fun sampler(s: JobSettings) =
        if (s.greedy) SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0, seed = 0) else SamplerConfig(topK = s.topK, topP = s.topP, temperature = s.temperature, seed = 0)

    /**
     * A kept conversation: each reply streamed, timed from the message to its
     * first piece, and, if asked, held to begin with her route word. Should
     * holding fail before anything comes, she is asked again free, in a
     * conversation opened afresh with the exchanges so far, and stays free
     * for the rest of the app's run.
     */
    inner class Talker(
        private val key: EngineKey,
        private var engine: Engine,
        private var on: String,
        private val instruction: String,
        private val history: List<Pair<String, String>>,
        private var held: Boolean,
        private val settings: JobSettings,
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
                    if (first != null || t is kotlinx.coroutines.CancellationException) throw t
                    if (on == "GPU") {
                        // 0.6.2: the GPU failed on this answer: she answers once more on the CPU.
                        Log.w("Gemma", "An answer failed on the GPU; that engine runs on the CPU from now on.", t)
                        runCatching { conversation.close() }
                        val cpu = ontoCpu(key)
                        engine = cpu.engine
                        on = cpu.processor
                        pace.gpuFailed()
                        conversation = conversation(engine, instruction, history + done, held, settings)
                        whole.clear()
                        try {
                            send()
                        } catch (u: Throwable) {
                            if (!held || first != null || u is kotlinx.coroutines.CancellationException) throw u
                            whyNotHeld = firstLine(u)
                            held = false
                            runCatching { conversation.close() }
                            conversation = conversation(engine, instruction, history + done, held = false, settings)
                            send()
                        }
                    } else {
                        if (!held) throw t
                        Log.w("Gemma", "Holding her first word failed; she answers free from now on.", t)
                        whyNotHeld = firstLine(t)
                        held = false
                        runCatching { conversation.close() }
                        conversation = conversation(engine, instruction, history + done, held = false, settings)
                        send()
                    }
                }
                counted(conversation)?.let { t -> pace.turns.add(Pace.Turn(t.read, t.readPerSecond, t.wrote, t.writePerSecond, first)) }
                first?.let { steps.add("her reply's first words", (it * 1000).toLong()) }
                steps.add("her whole reply, written free" + (counted(conversation)?.let { t -> " (read ${t.read} tokens, wrote ${t.wrote})" } ?: ""), Steps.since(started))
                whole.toString().also { done += message to it }
            }
        }

        override fun stop() { runCatching { conversation.cancelProcess() } }

        /** Sundial 0.10.2: what her kept conversation holds now, in tokens. */
        override fun tokens(): Int? = kotlinx.coroutines.runBlocking(thread) { runCatching { conversation.getTokenCount() }.getOrNull() }

        override fun close() {
            kotlinx.coroutines.runBlocking(thread) { runCatching { conversation.close() } }
            done()
        }
    }

    /**
     * A conversation has closed: the model goes now only if it was asked to
     * go. Sundial 0.10.1: never after idle minutes. On his Pixel the first
     * message after a few minutes away took 60 s, 52 s of it loading E4B
     * again; the model is loaded once and kept.
     */
    private fun done() {
        val now = synchronized(this@Gemma) {
            open--
            letGoWhenDone && open == 0
        }
        if (now) release()
    }

    /**
     * One message in a conversation of its own, opened and closed for it, as
     * the Sort job asks a piece at a time (0.6): her whole reply, how long it
     * took, and the engine's count.
     */
    @OptIn(ExperimentalApi::class)
    override suspend fun ask(instruction: String?, message: String, settings: JobSettings): Mind.Answer = withContext(thread) {
        main.removeCallbacks(letGo)
        val key = key(settings)
        val (l, loadMs) = engineNow(key)
        synchronized(this@Gemma) { open++ }
        try {
            fun on(e: Engine, processor: String, whyNotGpu: String?, failed: Boolean): Mind.Answer = e.createConversation(
                ConversationConfig(
                    systemInstruction = instruction?.let { Contents.of(it) },
                    samplerConfig = sampler(settings),
                    maxOutputToken = settings.longestReply,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                ),
            ).use { c ->
                val started = System.nanoTime()
                val text = c.sendMessage(message).toString()
                Mind.Answer(text, (System.nanoTime() - started) / 1_000_000, counted(c), processor, whyNotGpu, loadMs, gpuFailed = failed)
            }
            try {
                on(l.engine, l.processor, l.whyNotGpu, failed = false)
            } catch (t: Throwable) {
                if (l.processor != "GPU" || t is kotlinx.coroutines.CancellationException) throw t
                // 0.6.2: the GPU failed on this answer: it is asked once more on the CPU.
                Log.w("Gemma", "An answer failed on the GPU; that engine runs on the CPU from now on.", t)
                val cpu = ontoCpu(key)
                on(cpu.engine, cpu.processor, null, failed = true)
            }
        } finally {
            done()
        }
    }

    /** Rockpool: her ring choice through the engine's own tool call ("choose", a place 1 to [places]); her words beside it are never kept. */
    override suspend fun choose(instruction: String, message: String, places: Int, settings: JobSettings): Int? = withContext(thread) {
        main.removeCallbacks(letGo)
        val key = key(settings)
        val (l, _) = engineNow(key)
        synchronized(this@Gemma) { open++ }
        val tool = object : com.google.ai.edge.litertlm.OpenApiTool {
            override fun getToolDescriptionJsonString() = """{"name":"choose","description":"Choose the one place in the ring.","parameters":{"type":"object","properties":{"place":{"type":"integer","description":"The place's number in the ring, 1 to $places"}},"required":["place"]}}"""
            override fun execute(p0: String): String = "unused"
        }
        try {
            l.engine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of("$instruction Call choose with the place."),
                    tools = listOf(com.google.ai.edge.litertlm.tool(tool)), automaticToolCalling = false,
                    samplerConfig = sampler(settings), maxOutputToken = 60,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                ),
            ).use { c ->
                val m = runCatching { c.sendMessage(message) }.getOrNull()
                m?.toolCalls?.firstOrNull { it.name == "choose" }?.arguments?.get("place")?.toString()?.toDoubleOrNull()?.toInt()?.takeIf { it in 1..places }
                    ?: m?.contents?.toString()?.trim()?.let { Regex("""^\D{0,3}([1-9])""").find(it)?.groupValues?.get(1)?.toInt()?.takeIf { d -> d in 1..places } }
            }
        } finally {
            done()
        }
    }

    /** Sundial 0.10: her placing, through the engine's tool call ("places", a list of 1 to [places]); her words beside it never kept. */
    override suspend fun chooseMany(instruction: String, message: String, places: Int, settings: JobSettings): List<Int>? = withContext(thread) {
        main.removeCallbacks(letGo)
        val (l, _) = engineNow(key(settings))
        synchronized(this@Gemma) { open++ }
        val tool = object : com.google.ai.edge.litertlm.OpenApiTool {
            override fun getToolDescriptionJsonString() = """{"name":"places","description":"Name every place of the ring his words touch.","parameters":{"type":"object","properties":{"places":{"type":"array","items":{"type":"integer"},"description":"The places' numbers in the ring, 1 to $places, one or more"}},"required":["places"]}}"""
            override fun execute(p0: String): String = "unused"
        }
        try {
            runCatching {
                l.engine.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of("$instruction Call places with every place his words touch."),
                        tools = listOf(com.google.ai.edge.litertlm.tool(tool)), automaticToolCalling = false,
                        samplerConfig = sampler(settings), maxOutputToken = 80,
                        thinkingConfig = ThinkingConfig(enableThinking = false),
                    ),
                ).use { c ->
                    val started = System.nanoTime()
                    val m = c.sendMessage(message)
                    val call = m.toolCalls.firstOrNull { it.name == "places" }
                    // Sundial 0.10.2: a whole tool call is taken; else what she wrote in the same call is read for the ring's places, never asked again.
                    if (call != null) {
                        steps.add("the places call: a whole tool call", Steps.since(started))
                        numbers(call.arguments["places"]).filter { it in 1..places }.distinct().takeIf { it.isNotEmpty() }
                    } else {
                        val text = m.toString()
                        steps.add("the places call: no whole tool call; read from what she wrote, \u201c${text.trim().take(80)}\u201d", Steps.since(started))
                        placesWritten(text, instruction, places)
                    }
                }
            }.onFailure { Log.w("Gemma", "Her placing's tool call failed; nothing is named.", it) }.getOrNull()
        } finally {
            done()
        }
    }

    /** Sundial 0.10: the day's sheet through the tool call on E4B ("sheet": title, lines, tomorrow); else, or when it isn't whole, written free. */
    override suspend fun sheet(instruction: String, material: String, settings: JobSettings, tool: Boolean): com.phenomemental.pocketbeach.bsp.Mark.Sheet? {
        if (tool) {
            val got = withContext(thread) {
                main.removeCallbacks(letGo)
                val (l, _) = engineNow(key(settings))
                synchronized(this@Gemma) { open++ }
                val t = object : com.google.ai.edge.litertlm.OpenApiTool {
                    override fun getToolDescriptionJsonString() = """{"name":"sheet","description":"The day's sheet.","parameters":{"type":"object","properties":{"title":{"type":"string","description":"A short title, from his own words"},"lines":{"type":"array","items":{"type":"string"},"description":"Two to four lines, in the second person, drawn only from what he said"},"tomorrow":{"type":"string","description":"His intention for tomorrow if he voiced one, else empty"}},"required":["title","lines","tomorrow"]}}"""
                    override fun execute(p0: String): String = "unused"
                }
                try {
                    runCatching {
                        l.engine.createConversation(
                            ConversationConfig(
                                systemInstruction = Contents.of("$instruction Call sheet with it."),
                                tools = listOf(com.google.ai.edge.litertlm.tool(t)), automaticToolCalling = false,
                                samplerConfig = sampler(settings), maxOutputToken = 400,
                                thinkingConfig = ThinkingConfig(enableThinking = false),
                            ),
                        ).use { c ->
                            val started = System.nanoTime()
                            val m = c.sendMessage(material)
                            val a = m.toolCalls.firstOrNull { it.name == "sheet" }?.arguments
                            // Sundial 0.10.2: no whole tool call: what she wrote in the same call is read as plain lines, as her free sheet is.
                            if (a == null) {
                                val text = m.toString()
                                steps.add("the card's sheet: no whole tool call; read from what she wrote", Steps.since(started))
                                return@use Mind.sheetOf(text)
                            }
                            steps.add("the card's sheet: a whole tool call", Steps.since(started))
                            val title = a["title"]?.toString()?.trim().orEmpty()
                            val lines = strings(a["lines"]).take(4)
                            if (title.isEmpty() || lines.isEmpty()) null
                            else com.phenomemental.pocketbeach.bsp.Mark.Sheet(title, lines, a["tomorrow"]?.toString()?.trim().orEmpty().removePrefix("Tomorrow:").trim())
                        }
                    }.onFailure { Log.w("Gemma", "Her sheet's tool call failed; she is asked free.", it) }.getOrNull()
                } finally {
                    done()
                }
            }
            if (got != null) return got
        }
        return super.sheet(instruction, material, settings, tool = false)
    }

    /**
     * Sundial 0.10.2: the places she named in plain words when no whole tool
     * call came back: each place of the ring ([instruction]'s "N name" lines)
     * whose name her words hold; else the ring's numbers she wrote.
     */
    private fun placesWritten(text: String, instruction: String, places: Int): List<Int>? {
        val plain = " " + com.phenomemental.pocketbeach.bsp.Telling.plain(text.replace('\u2019', '\'')) + " "
        val ring = instruction.lines().mapNotNull { Regex("""^(\d+) (.+)$""").find(it.trim()) }.associate { it.groupValues[1].toInt() to com.phenomemental.pocketbeach.bsp.Telling.plain(it.groupValues[2].replace('\u2019', '\'')) }
        val named = ring.filter { (_, name) -> name.isNotEmpty() && plain.contains(" $name ") }.keys.sorted()
        return (named.ifEmpty { numbers(text).filter { it in 1..places }.distinct() }).takeIf { it.isNotEmpty() }
    }

    /** A tool argument read as numbers, however the engine hands a list over. */
    private fun numbers(raw: Any?): List<Int> = when (raw) {
        is List<*> -> raw.mapNotNull { it?.toString()?.toDoubleOrNull()?.toInt() }
        null -> emptyList()
        else -> Regex("""\d+""").findAll(raw.toString()).map { it.value.toInt() }.toList()
    }

    /** A tool argument read as strings. */
    private fun strings(raw: Any?): List<String> = when (raw) {
        is List<*> -> raw.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
        null -> emptyList()
        else -> raw.toString().trim().removeSurrounding("[", "]").split(Regex("""",\s*"|\n""")).map { it.trim().trim('"').trim() }.filter { it.isNotEmpty() }
    }

    override suspend fun chat(instruction: String): Mind.Chat = withContext(thread) {
        main.removeCallbacks(letGo)
        val settings = JobSettings.of(Job.WRITE)
        val (l, loadMs) = engineNow(key(settings))
        val conversation = l.engine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(instruction),
                samplerConfig = sampler(settings),
                maxOutputToken = settings.longestReply,
                thinkingConfig = ThinkingConfig(enableThinking = false),
                enableResponseFormat = true,
            ),
        )
        synchronized(this@Gemma) { open++ }
        Chat(conversation, Pace(l.processor, l.whyNotGpu, loadMs))
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
            done()
        }
    }

    /** The last exchange as the engine counted it: tokens read and written, and how fast; null when it didn't count. */
    @OptIn(ExperimentalApi::class)
    private fun counted(conversation: Conversation): Pace.Turn? = runCatching {
        val b = conversation.getBenchmarkInfo()
        Pace.Turn(b.lastPrefillTokenCount, b.lastPrefillTokensPerSecond, b.lastDecodeTokenCount, b.lastDecodeTokensPerSecond, b.timeToFirstTokenInSecond)
    }.getOrNull()?.takeIf { it.read > 0 || it.wrote > 0 }

    /** An engine on [backend] holding [context] tokens, counting each exchange if [counting], with speculative decoding asked for or not ([speculative] null: the engine's default). */
    @OptIn(ExperimentalApi::class)
    private fun open(backend: Backend, counting: Boolean, speculative: Boolean? = null, context: Int = Asks.Tuning.MAX_TOKENS, model: String = ModelStore.E2B.name): Engine {
        // Both are read only as the engine is made.
        ExperimentalFlags.enableBenchmark = counting
        @Suppress("DEPRECATION")
        ExperimentalFlags.enableSpeculativeDecoding = speculative
        val e = Engine(EngineConfig(modelPath = stores(model).file.path, backend = backend, maxNumTokens = context, cacheDir = cacheDir))
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

    /** The engine [key] asks for: on the CPU if it asks for the CPU; else on the GPU, falling back to the CPU, and why. */
    private fun load(key: EngineKey, speculative: Boolean = false): Loaded {
        check(stores(key.model).state() is ModelStore.State.Ready) { "${key.model} isn't on this phone yet: get it in Settings." }
        if (key.processor == "CPU") return Loaded(open(Backend.CPU(), counting = true, speculative = speculative, context = key.context, model = key.model), "CPU", null)
        return try {
            Loaded(open(Backend.GPU(), counting = true, speculative = speculative, context = key.context, model = key.model), "GPU", null)
        } catch (t: Throwable) {
            Log.w("Gemma", "The GPU couldn't load the model.", t)
            // Counting is the one thing asked of the GPU beyond what 0.3.1 asked, so it is tried once more without it before the CPU.
            runCatching { open(Backend.GPU(), counting = false, speculative = speculative, context = key.context, model = key.model) }.getOrNull()?.let { Loaded(it, "GPU", null) }
                ?: Loaded(open(Backend.CPU(), counting = true, speculative = speculative, context = key.context, model = key.model), "CPU", firstLine(t))
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
        closeAll()
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

    /** Let the model go, every engine of it, freeing its memory, once no conversation is open; it loads again on the next Ask. */
    fun release() {
        main.removeCallbacks(letGo)
        synchronized(this) {
            if (open > 0) {
                letGoWhenDone = true
                return
            }
            letGoWhenDone = false
        }
        if (engines.isEmpty()) return
        thread.executor.execute { closeAll() }
    }
}
