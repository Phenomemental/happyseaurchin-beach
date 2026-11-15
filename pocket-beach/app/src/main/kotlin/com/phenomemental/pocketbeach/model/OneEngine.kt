package com.phenomemental.pocketbeach.model

import kotlinx.coroutines.delay

/**
 * Sundial 0.10.5: her one engine.
 *
 * On his Pixel on 15 November she answered on the CPU, "another engine holds
 * the GPU": Gemma kept an engine for each model, processor and context asked
 * for, and when an engine was asked for under other settings while a
 * conversation was still open on the one holding the GPU, it made a second
 * engine on the CPU beside it. That CPU engine then stood in for hers for
 * the rest of the app's run, after the conversation had closed.
 *
 * Now there is one engine, made when she is first asked and kept for the
 * life of the app. Every conversation opens on it: her talk, a new
 * conversation, the greeting, keeping. Should it ever have to be made again
 * (her settings changed, Android short of memory, the GPU failing on an
 * answer), the old one is let go first, every conversation on it closed, so
 * the GPU is free; each conversation opens again on the new engine as it is
 * next asked, with its exchanges so far. If the GPU still can't take her,
 * it is tried once more; only then does she run on the CPU, and [Pace.SLOWER]
 * says so beneath her reply.
 *
 * Gemma calls it from its one thread; the device tests from theirs, one
 * call at a time. Its state is guarded all the same.
 */
class OneEngine<E : Any>(
    /** Make an engine for [EngineKey] on the GPU (true) or the CPU; on the GPU's second try, [again]. Throws when it can't. */
    private val make: (key: EngineKey, gpu: Boolean, again: Boolean) -> E,
    /** Close an engine, freeing its memory and the GPU. */
    private val shut: (E) -> Unit,
    /** A plain line for the workings: an engine let go, the GPU tried again. */
    private val said: (String) -> Unit = {},
    /** Between the GPU's two tries: time for what was let go to be freed. */
    private val pause: () -> Unit = { System.gc(); Thread.sleep(PAUSE_MS) },
) {
    /** An engine as made: for which settings, on which processor ("GPU" or "CPU"), and why not the GPU when the GPU was asked for and couldn't. */
    class Loaded<E>(val key: EngineKey, val engine: E, val processor: String, val whyNotGpu: String?)

    /** A conversation open on the engine. When the engine goes it is let go (closed), and it opens again on the engine as it is next asked. */
    fun interface Kept {
        fun letGo()
    }

    private val lock = Any()

    /** Her one engine; null until she is first asked, and while let go. */
    @Volatile var held: Loaded<E>? = null
        private set

    /** How many engines have been made in this run of the app. */
    @Volatile var made = 0
        private set

    /** The conversations open on her engine. */
    private val kept = LinkedHashSet<Kept>()

    /** Calls under way on her engine now: it is never let go under one. */
    private var busy = 0
    private var letGoWhenIdle = false

    /** Settings the GPU couldn't take on its second try: on the CPU until the app starts again, and why. Read from any thread. */
    private val noGpu = java.util.concurrent.ConcurrentHashMap<EngineKey, String>()

    /** Settings an answer failed on the GPU for once: once more and they stay on the CPU. */
    private val failedOnce = HashSet<EngineKey>()

    /** Why [key] runs on the CPU though it asks for the GPU, until the app starts again; null when it doesn't. */
    fun noGpu(key: EngineKey): String? = noGpu[key]

    /** How many conversations are open on her engine. */
    val open: Int get() = synchronized(lock) { kept.size }

    /** A conversation opened on her engine. */
    fun opened(k: Kept) = synchronized(lock) { kept += k }

    /** A conversation closed for good. */
    fun closed(k: Kept) = synchronized(lock) { kept -= k }

    /**
     * Her engine for [key]: the one in memory when it was made for [key],
     * else made now, the one in memory let go first (once nothing is under
     * way on it); how long making it took, when it had to be made.
     */
    suspend fun engine(key: EngineKey): Pair<Loaded<E>, Long?> {
        while (true) {
            synchronized(lock) {
                held?.takeIf { it.key == key }?.let { return it to null }
                if (busy == 0) {
                    held?.let { letGoNow("${it.key.model} on the ${it.processor} let go first, so the GPU is free") }
                    val started = System.nanoTime()
                    val l = load(key)
                    held = l
                    return l to (System.nanoTime() - started) / 1_000_000
                }
            }
            // Something is under way on the engine in memory: it can't be let go until it ends.
            delay(20)
        }
    }

    /** [block], a call under way on her engine: it is never let go under one. */
    suspend fun <T> busy(block: suspend () -> T): T {
        synchronized(lock) { busy++ }
        try {
            return block()
        } finally {
            synchronized(lock) {
                busy--
                if (busy == 0 && letGoWhenIdle) {
                    letGoWhenIdle = false
                    letGoNow(SHORT_OF_MEMORY)
                }
            }
        }
    }

    /**
     * An answer failed on the GPU before anything of it came: her engine is
     * let go, to be made again on the GPU once more as it is next asked;
     * should the GPU fail again, on the CPU until the app starts again.
     * Called with nothing under way on it.
     */
    fun gpuFailed(key: EngineKey, why: String) = synchronized(lock) {
        if (!failedOnce.add(key)) noGpu[key] = "an answer failed on the GPU twice ($why)"
        held?.takeIf { it.key == key }?.let { letGoNow("the GPU failed on an answer: ${it.key.model} let go, to be made again" + if (noGpu.containsKey(key)) " on the CPU" else " on the GPU once more") }
    }

    /** Android is short of memory: her engine goes now if nothing is under way on it, else as soon as nothing is. */
    fun release() = synchronized(lock) {
        if (busy > 0) letGoWhenIdle = true else letGoNow(SHORT_OF_MEMORY)
    }

    /** Let her engine go now, once nothing is under way on it: as the bench begins. */
    suspend fun letGoAll() {
        while (true) {
            synchronized(lock) {
                if (busy == 0) {
                    letGoNow(null)
                    return
                }
            }
            delay(20)
        }
    }

    /** Every conversation on her engine let go, then the engine. Holding [lock], with nothing under way. */
    private fun letGoNow(why: String?) {
        val h = held ?: return
        held = null
        for (k in kept.toList()) runCatching { k.letGo() }
        runCatching { shut(h.engine) }
        why?.let(said)
    }

    /** The engine [key] asks for, made now: on the CPU if it asks for the CPU; else on the GPU, tried twice, and only then on the CPU, with why. */
    private fun load(key: EngineKey): Loaded<E> {
        if (key.processor != "GPU") return Loaded(key, made(key, gpu = false, again = false), "CPU", null)
        noGpu[key]?.let { why -> return Loaded(key, made(key, gpu = false, again = false), "CPU", why) }
        val first = try {
            return Loaded(key, made(key, gpu = true, again = false), "GPU", null)
        } catch (t: Throwable) {
            t
        }
        // The GPU couldn't take her. Nothing of the app's is on it now (the engine before was let go first): it is tried once more.
        said("the GPU couldn't take ${key.model} (${firstLine(first)}); tried once more")
        runCatching { pause() }
        try {
            return Loaded(key, made(key, gpu = true, again = true), "GPU", null)
        } catch (_: Throwable) {
        }
        val why = firstLine(first)
        noGpu[key] = why
        said("the GPU couldn't take ${key.model} twice: on the CPU until Sundial is opened again")
        return Loaded(key, made(key, gpu = false, again = false), "CPU", why)
    }

    private fun made(key: EngineKey, gpu: Boolean, again: Boolean): E = make(key, gpu, again).also { made++ }

    companion object {
        const val PAUSE_MS = 500L
        const val SHORT_OF_MEMORY = "let go: Android is short of memory"

        fun firstLine(t: Throwable) = t.message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()?.take(240) ?: t::class.java.simpleName
    }
}
