package com.phenomemental.pocketbeach.model

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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext

/**
 * The companion's mind: Gemma 4, run on this phone by LiteRT-LM. It is
 * loaded when Matthew first asks something and kept (Sundial 0.10.1: never
 * let go after idle minutes), until Android is really short of memory
 * (decision 13). Each Ask's [Pace] says where it ran and how fast.
 *
 * Sundial 0.10.5: one engine ([OneEngine]), kept for the life of the app;
 * every conversation is opened on it: her talk, a new conversation, the
 * greeting, keeping. Should it have to be made again, the old one is let go
 * first, so the GPU is free. Its GPU is tried first, and tried once more if
 * it can't take her; only then the CPU, said plainly beneath her reply. An
 * answer that fails on the GPU lets the engine go and is answered once more
 * on one made afresh on the GPU; should that fail too, on the CPU, where she
 * stays until the app starts again.
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

    /** One thread for every call into the engine, so none is ever used from two at once. */
    private val thread = Executors.newSingleThreadExecutor { r -> Thread(r, "gemma").apply { isDaemon = true } }.asCoroutineDispatcher()

    /** Sundial 0.10.1: each step of the answers under way, timed. */
    override val steps = Steps()

    /**
     * Sundial 0.10.5: her one engine. On the GPU's second try counting is
     * left off, the one thing asked of the GPU beyond what 0.3.1 asked.
     */
    private val one = OneEngine<Engine>(
        make = { key, gpu, again ->
            open(if (gpu) Backend.GPU() else Backend.CPU(), counting = !(gpu && again), speculative = SPECULATIVE, context = key.context, model = key.model)
        },
        shut = { it.close() },
        said = { steps.note(it) },
    )

    override fun gpuFailed(settings: JobSettings): Boolean = one.noGpu(key(settings)) != null

    /** The one processor the bench has asked for ("CPU"), or null: as each job's settings say. */
    @Volatile private var onlyOn: String? = null

    override suspend fun only(processor: String?) = withContext(thread) {
        one.letGoAll()
        onlyOn = processor
    }

    /** Why holding her first word failed on this phone, once it has; her replies are then free for the rest of the app's run. */
    @Volatile private var whyNotHeld: String? = null

    /** Which of the phone's processors runs her engine: "GPU" or "CPU"; null while none is in memory. */
    override val processor: String? get() = one.held?.processor

    override val loaded: Boolean get() = one.held != null

    /** Whether her engine is in memory now, made for [settings]. */
    override fun loaded(settings: JobSettings): Boolean = one.held?.key == key(settings)

    override fun state(): ModelState = store.state()

    override fun state(model: String): ModelState = stores(model).state()

    override suspend fun warm(settings: JobSettings): Long? = withContext(thread) {
        engine(key(settings)).second
    }

    /** The engine [settings] run on, the bench's one processor taking the place of theirs. */
    private fun key(settings: JobSettings): EngineKey = settings.engine.let { k -> onlyOn?.let { k.copy(processor = it) } ?: k }

    /** Her engine for [key], made if it isn't (the one before let go first); how long making it took, if it had to be made. Called on [thread]. */
    private suspend fun engine(key: EngineKey): Pair<OneEngine.Loaded<Engine>, Long?> {
        if (one.held?.key != key) check(stores(key.model).state() is ModelState.Ready) { "${key.model} isn't on this phone yet: get it in Settings." }
        return one.engine(key).also { (l, ms) -> ms?.let { steps.add("${key.model} loaded on the ${l.processor}", it) } }
    }

    override suspend fun talker(instruction: String, history: List<Pair<String, String>>, held: Boolean, settings: JobSettings): Mind.Talker {
        val caller = currentCoroutineContext()[kotlinx.coroutines.Job]
        return withContext(thread) {
            val key = key(settings)
            val (l, loadMs) = engine(key)
            if (loadMs == null) steps.note("${key.model} already in memory, on the ${l.processor}")
            val holding = held && whyNotHeld == null
            val c = conversation(l.engine, instruction, history, holding, settings)
            // Asked for and then given up (he stopped her as it opened): closed now, never left open on the engine.
            if (caller?.isCancelled == true) {
                runCatching { c.close() }
                throw kotlinx.coroutines.CancellationException("Her conversation was given up as it opened.")
            }
            Talker(key, l, instruction, history, holding, settings, c, Pace(l.processor, l.whyNotGpu, loadMs)).also { one.opened(it) }
        }
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
     * A kept conversation on her engine: each reply streamed, timed from the
     * message to its first piece, and, if asked, held to begin with her
     * route word. Should holding fail before anything comes, she is asked
     * again free, in a conversation opened afresh with the exchanges so far,
     * and stays free for the rest of the app's run. Sundial 0.10.5: when her
     * engine is let go, the conversation is closed with it, and opened again
     * on her engine, with the exchanges so far, as it is next asked.
     */
    inner class Talker(
        private val key: EngineKey,
        @Volatile private var at: OneEngine.Loaded<Engine>,
        private val instruction: String,
        private val history: List<Pair<String, String>>,
        @Volatile private var held: Boolean,
        private val settings: JobSettings,
        @Volatile private var conversation: Conversation?,
        override val pace: Pace,
    ) : Mind.Talker, OneEngine.Kept {
        private val done = mutableListOf<Pair<String, String>>()

        /** He stopped her: what fails now is his stop, never the GPU. */
        @Volatile private var stopping = false

        override val route: String get() = if (held) "held by the engine" else "free" + (whyNotHeld?.let { ": holding it failed on this phone ($it)" } ?: "")

        /** Her engine is going: the conversation closes now. On [thread]. */
        override fun letGo() {
            conversation?.let { runCatching { it.close() } }
            conversation = null
        }

        /** The conversation, opened again on her engine, with every exchange so far, if it was let go. On [thread]. */
        private suspend fun ready(): Conversation {
            conversation?.let { return it }
            val (l, _) = engine(key)
            at = l
            pace.ranOn(l.processor, l.whyNotGpu)
            return conversation(l.engine, instruction, history + done, held, settings).also { conversation = it }
        }

        override val stream = com.phenomemental.pocketbeach.bsp.Talks.Stream { message, piece ->
            kotlinx.coroutines.runBlocking(thread) {
                stopping = false
                val started = System.nanoTime()
                var first: Double? = null
                val whole = StringBuilder()
                suspend fun send(c: Conversation) {
                    val flow = if (held) c.sendMessageAsync(message, responseFormat = ResponseFormat.regex(com.phenomemental.pocketbeach.bsp.Talks.HELD))
                    else c.sendMessageAsync(message)
                    flow.collect { m ->
                        if (first == null) first = (System.nanoTime() - started) / 1e9
                        val text = m.toString()
                        whole.append(text)
                        piece(text)
                    }
                }
                var gpuFailures = 0
                var used: Conversation
                while (true) {
                    val c = ready()
                    try {
                        one.busy { send(c) }
                        used = c
                        break
                    } catch (t: Throwable) {
                        if (stopping || first != null || t is kotlinx.coroutines.CancellationException) throw t
                        whole.clear()
                        runCatching { c.close() }
                        conversation = null
                        when {
                            // The GPU failed on this answer: her engine is let go and made again, the GPU once more, then the CPU.
                            at.processor == "GPU" && gpuFailures < 2 -> {
                                Log.w("Gemma", "An answer failed on the GPU; her engine is made again.", t)
                                gpuFailures++
                                one.gpuFailed(key, OneEngine.firstLine(t))
                            }
                            held -> {
                                Log.w("Gemma", "Holding her first word failed; she answers free from now on.", t)
                                whyNotHeld = OneEngine.firstLine(t)
                                held = false
                            }
                            else -> throw t
                        }
                    }
                }
                if (gpuFailures > 0 && at.processor == "CPU") pace.gpuFailed()
                val c = used
                counted(c)?.let { t -> pace.turns.add(Pace.Turn(t.read, t.readPerSecond, t.wrote, t.writePerSecond, first)) }
                first?.let { steps.add("her reply's first words", (it * 1000).toLong()) }
                steps.add("her whole reply, written free" + (counted(c)?.let { t -> " (read ${t.read} tokens, wrote ${t.wrote})" } ?: ""), Steps.since(started))
                whole.toString().also { done += message to it }
            }
        }

        override fun stop() {
            stopping = true
            runCatching { conversation?.cancelProcess() }
        }

        /**
         * Sundial 0.13: one turn with the walker offered. Tools are set when a
         * conversation opens, so the turn runs in a conversation of its own
         * with every exchange so far and the tool, thinking off; her kept
         * conversation is let go, and opens again with this turn in it at his
         * next message. On her own engine: Sundial 0.13.2 took out the switch
         * to the CPU, which swapped engines mid-conversation and was too slow
         * on his Pixel.
         */
        override fun sayWithTool(message: String, toolJson: String, call: (Mind.Called) -> String, piece: (String) -> Unit): Mind.ToolTurn =
            kotlinx.coroutines.runBlocking(thread) {
                stopping = false
                val started = System.nanoTime()
                letGo()
                val (l, _) = engine(key)
                at = l
                pace.ranOn(l.processor, l.whyNotGpu)
                val tool = object : com.google.ai.edge.litertlm.OpenApiTool {
                    override fun getToolDescriptionJsonString() = toolJson
                    override fun execute(p0: String): String = "unused"
                }
                val calls = mutableListOf<Pair<Mind.Called, String>>()
                val reply = one.busy {
                    l.engine.createConversation(
                        ConversationConfig(
                            systemInstruction = Contents.of(instruction),
                            initialMessages = (history + done).flatMap { (asked, said) -> listOf(Message.user(asked), Message.model(said)) },
                            tools = listOf(com.google.ai.edge.litertlm.tool(tool)), automaticToolCalling = false,
                            samplerConfig = sampler(settings), maxOutputToken = settings.longestReply,
                            thinkingConfig = ThinkingConfig(enableThinking = false),
                        ),
                    ).use { c ->
                        var m = c.sendMessage(message)
                        var rounds = 0
                        while (m.toolCalls.isNotEmpty() && rounds < 3) {
                            rounds++
                            val answers = m.toolCalls.map { tc ->
                                val called = Mind.Called(tc.name, tc.arguments, m.toString())
                                val text = call(called)
                                calls += called to text
                                com.google.ai.edge.litertlm.Content.ToolResponse(tc.name, text)
                            }
                            m = c.sendMessage(Message.tool(Contents.of(answers)))
                        }
                        counted(c)?.let { t -> pace.turns.add(Pace.Turn(t.read, t.readPerSecond, t.wrote, t.writePerSecond, null)) }
                        m.toString()
                    }
                }
                steps.add("her reply with the walker offered, on the ${l.processor}, ${calls.size} walk${if (calls.size == 1) "" else "s"}", Steps.since(started))
                piece(reply)
                done += message to reply
                Mind.ToolTurn(reply, calls)
            }

        /** Sundial 0.10.2: what her kept conversation holds now, in tokens; null when it was let go. */
        override fun tokens(): Int? = kotlinx.coroutines.runBlocking(thread) { conversation?.let { c -> runCatching { c.getTokenCount() }.getOrNull() } }

        override fun close() {
            kotlinx.coroutines.runBlocking(thread) {
                letGo()
                one.closed(this@Talker)
            }
        }
    }

    /**
     * One message in a conversation of its own, opened and closed for it, as
     * the Sort job asks a piece at a time (0.6): her whole reply, how long it
     * took, and the engine's count. Should the GPU fail on it, her engine is
     * made again and it is asked once more: the GPU, then the CPU.
     */
    @OptIn(ExperimentalApi::class)
    override suspend fun ask(instruction: String?, message: String, settings: JobSettings): Mind.Answer = withContext(thread) {
        val key = key(settings)
        var loadMs: Long? = null
        var failed = false
        while (true) {
            val (l, ms) = engine(key)
            if (loadMs == null) loadMs = ms
            try {
                return@withContext one.busy {
                    l.engine.createConversation(
                        ConversationConfig(
                            systemInstruction = instruction?.let { Contents.of(it) },
                            samplerConfig = sampler(settings),
                            maxOutputToken = settings.longestReply,
                            thinkingConfig = ThinkingConfig(enableThinking = false),
                        ),
                    ).use { c ->
                        val started = System.nanoTime()
                        val text = c.sendMessage(message).toString()
                        Mind.Answer(text, (System.nanoTime() - started) / 1_000_000, counted(c), l.processor, l.whyNotGpu, loadMs, gpuFailed = failed && l.processor == "CPU")
                    }
                }
            } catch (t: Throwable) {
                if (l.processor != "GPU" || t is kotlinx.coroutines.CancellationException) throw t
                Log.w("Gemma", "An answer failed on the GPU; her engine is made again.", t)
                failed = true
                one.gpuFailed(key, OneEngine.firstLine(t))
            }
        }
        @Suppress("UNREACHABLE_CODE")
        error("unreachable")
    }

    /** Rockpool: her ring choice through the engine's own tool call ("choose", a place 1 to [places]); her words beside it are never kept. */
    override suspend fun choose(instruction: String, message: String, places: Int, settings: JobSettings): Int? = withContext(thread) {
        val (l, _) = engine(key(settings))
        val tool = object : com.google.ai.edge.litertlm.OpenApiTool {
            override fun getToolDescriptionJsonString() = """{"name":"choose","description":"Choose the one place in the ring.","parameters":{"type":"object","properties":{"place":{"type":"integer","description":"The place's number in the ring, 1 to $places"}},"required":["place"]}}"""
            override fun execute(p0: String): String = "unused"
        }
        one.busy {
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
        }
    }

    /** Sundial 0.10: her placing, through the engine's tool call ("places", a list of 1 to [places]); her words beside it never kept. */
    override suspend fun chooseMany(instruction: String, message: String, places: Int, settings: JobSettings): List<Int>? = withContext(thread) {
        val (l, _) = engine(key(settings))
        val tool = object : com.google.ai.edge.litertlm.OpenApiTool {
            override fun getToolDescriptionJsonString() = """{"name":"places","description":"Name every place of the ring his words touch.","parameters":{"type":"object","properties":{"places":{"type":"array","items":{"type":"integer"},"description":"The places' numbers in the ring, 1 to $places, one or more"}},"required":["places"]}}"""
            override fun execute(p0: String): String = "unused"
        }
        one.busy {
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
                        steps.add("the places call: no whole tool call; read from what she wrote, “${text.trim().take(80)}”", Steps.since(started))
                        placesWritten(text, instruction, places)
                    }
                }
            }.onFailure { Log.w("Gemma", "Her placing's tool call failed; nothing is named.", it) }.getOrNull()
        }
    }

    /** Sundial 0.12.1: one tool call through the engine, its arguments as the engine handed them; null when she made none. */
    override suspend fun callTool(instruction: String, message: String, toolJson: String, settings: JobSettings): Mind.Called? = withContext(thread) {
        val (l, _) = engine(key(settings))
        val tool = object : com.google.ai.edge.litertlm.OpenApiTool {
            override fun getToolDescriptionJsonString() = toolJson
            override fun execute(p0: String): String = "unused"
        }
        one.busy {
            runCatching {
                l.engine.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(instruction),
                        tools = listOf(com.google.ai.edge.litertlm.tool(tool)), automaticToolCalling = false,
                        samplerConfig = sampler(settings), maxOutputToken = 120,
                        thinkingConfig = ThinkingConfig(enableThinking = false),
                    ),
                ).use { c ->
                    val m = c.sendMessage(message)
                    m.toolCalls.firstOrNull()?.let { Mind.Called(it.name, it.arguments, m.toString()) }
                }
            }.onFailure { Log.w("Gemma", "Her tool call failed.", it) }.getOrNull()
        }
    }

    override suspend fun converse(instruction: String, messages: List<String>, settings: JobSettings, longest: Int): Mind.Conversed? = withContext(thread) {
        val (l, _) = engine(key(settings))
        one.busy {
            l.engine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(instruction),
                    samplerConfig = sampler(settings), maxOutputToken = longest,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                ),
            ).use { c -> Mind.Conversed(l.processor, l.whyNotGpu, messages.map { c.sendMessage(it).toString() }) }
        }
    }

    /** Sundial 0.10: the day's sheet through the tool call on E4B ("sheet": title, lines, tomorrow); else, or when it isn't whole, written free. */
    override suspend fun sheet(instruction: String, material: String, settings: JobSettings, tool: Boolean): com.phenomemental.pocketbeach.bsp.Mark.Sheet? {
        if (tool) {
            val got = withContext(thread) {
                val (l, _) = engine(key(settings))
                val t = object : com.google.ai.edge.litertlm.OpenApiTool {
                    override fun getToolDescriptionJsonString() = """{"name":"sheet","description":"The day's sheet.","parameters":{"type":"object","properties":{"title":{"type":"string","description":"A short title, from his own words"},"lines":{"type":"array","items":{"type":"string"},"description":"Two to four lines, in the second person, drawn only from what he said"},"tomorrow":{"type":"string","description":"His intention for tomorrow if he voiced one, else empty"}},"required":["title","lines","tomorrow"]}}"""
                    override fun execute(p0: String): String = "unused"
                }
                one.busy {
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
        val plain = " " + com.phenomemental.pocketbeach.bsp.Telling.plain(text.replace('’', '\'')) + " "
        val ring = instruction.lines().mapNotNull { Regex("""^(\d+) (.+)$""").find(it.trim()) }.associate { it.groupValues[1].toInt() to com.phenomemental.pocketbeach.bsp.Telling.plain(it.groupValues[2].replace('’', '\'')) }
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
        val settings = JobSettings.of(Job.WRITE)
        val key = key(settings)
        val (l, loadMs) = engine(key)
        Chat(key, instruction, l.engine.let { chatOn(it, instruction, settings) }, Pace(l.processor, l.whyNotGpu, loadMs)).also { one.opened(it) }
    }

    private fun chatOn(e: Engine, instruction: String, settings: JobSettings): Conversation = e.createConversation(
        ConversationConfig(
            systemInstruction = Contents.of(instruction),
            samplerConfig = sampler(settings),
            maxOutputToken = settings.longestReply,
            thinkingConfig = ThinkingConfig(enableThinking = false),
            enableResponseFormat = true,
        ),
    )

    /** 0.3's conversation answered in a form, on her engine; let go with it, and opened again on it as it is next asked. */
    inner class Chat(private val key: EngineKey, private val instruction: String, @Volatile private var conversation: Conversation?, override val pace: Pace) : Mind.Chat, OneEngine.Kept {
        override fun letGo() {
            conversation?.let { runCatching { it.close() } }
            conversation = null
        }

        override val talk = Talk { message ->
            kotlinx.coroutines.runBlocking(thread) {
                val c = conversation ?: chatOn(engine(key).first.engine, instruction, JobSettings.of(Job.WRITE)).also { conversation = it }
                one.busy { c.sendMessage(message, responseFormat = ResponseFormat.json(Asks.SCHEMA)).toString() }
                    .also { counted(c)?.let(pace.turns::add) }
            }
        }

        override fun stop() { runCatching { conversation?.cancelProcess() } }

        override fun close() {
            kotlinx.coroutines.runBlocking(thread) {
                letGo()
                one.closed(this@Chat)
            }
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

    /**
     * What the bench asks of the engine itself, a line each: what the model
     * file says it runs on; a plain answer timed on the GPU (or the CPU) with
     * speculative decoding off, then on; and the NPU, where the file has a
     * build for it. (Whether her first word can be held, the bench's 0.4
     * run says.) Her engine is let go first, so the GPU is free, and each
     * engine tried here is closed before the next is made; the next Ask
     * makes hers as ever. [step] says what is being tried.
     */
    suspend fun probes(step: (String) -> Unit): List<String> = withContext(thread) {
        one.letGoAll()
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
                made = runCatching { name to open(backend(), counting = true, speculative = speculative) }.onFailure { why = OneEngine.firstLine(it) }.getOrNull()
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
                onFailure = { "The NPU wouldn't load the model: ${OneEngine.firstLine(it)}." },
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
    }.getOrElse { "it failed: ${OneEngine.firstLine(it)}." }

    private fun secs(ms: Long) = String.format(Locale.UK, "%.1f", ms / 1000.0)

    private fun one(x: Double) = String.format(Locale.UK, "%.1f", x)

    /**
     * Android is really short of memory: her engine goes, freeing its memory,
     * now if nothing is under way on it, else as soon as nothing is. Each
     * conversation opens again on it, with its exchanges so far, as it is
     * next asked.
     */
    fun release() {
        thread.executor.execute { one.release() }
    }
}
