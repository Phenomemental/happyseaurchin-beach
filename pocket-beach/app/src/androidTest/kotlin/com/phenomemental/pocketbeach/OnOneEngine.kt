package com.phenomemental.pocketbeach

import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.model.EngineKey
import com.phenomemental.pocketbeach.model.JobSettings
import com.phenomemental.pocketbeach.model.Mind
import com.phenomemental.pocketbeach.model.OneEngine
import com.phenomemental.pocketbeach.model.Pace

/**
 * Sundial 0.10.5: the stand-in's replies, on her one engine as Gemma keeps
 * it ([OneEngine]), its engines pretend. The pretend phone's GPU takes one
 * engine at a time, as his Pixel's does: an engine asked of it while another
 * is on it is refused, so a second engine made while the first still held
 * the GPU would land on the CPU, and the plain line would say so.
 *
 * Each call goes to the engine as Gemma's does: her engine asked for with
 * the call's settings, every conversation kept on it let go when it goes and
 * opened again on it as it is next asked.
 */
class OnOneEngine(private val replies: StandIn) : Mind by replies {
    /** Engines on the pretend GPU now, the most there ever were at once, and every engine made. */
    @Volatile var onGpu = 0
    @Volatile var mostOnGpu = 0
    val made = java.util.Collections.synchronizedList(mutableListOf<String>())

    /** How often a conversation kept on her engine was let go with it. */
    @Volatile var letGo = 0

    val one = OneEngine<String>(
        make = { key, gpu, _ ->
            synchronized(this) {
                if (gpu) {
                    check(onGpu == 0) { "another engine holds the GPU" }
                    onGpu++
                    mostOnGpu = maxOf(mostOnGpu, onGpu)
                }
                "${key.model} at ${key.context} on the ${if (gpu) "GPU" else "CPU"} (${made.size + 1})".also { made += it }
            }
        },
        shut = { e -> synchronized(this) { if (e.contains("on the GPU")) onGpu-- } },
        pause = {},
    )

    override val loaded: Boolean get() = one.held != null
    override val processor: String? get() = one.held?.processor
    override fun loaded(settings: JobSettings): Boolean = one.held?.key == settings.engine
    override fun gpuFailed(settings: JobSettings): Boolean = one.noGpu(settings.engine) != null

    override suspend fun warm(settings: JobSettings): Long? = one.engine(settings.engine).second

    override suspend fun talker(instruction: String, history: List<Pair<String, String>>, held: Boolean, settings: JobSettings): Mind.Talker {
        val key: EngineKey = settings.engine
        val (l, ms) = one.engine(key)
        val inner = replies.talker(instruction, history, held, settings)
        val kept = object : Mind.Talker, OneEngine.Kept {
            @Volatile private var open = true
            override val pace = Pace(l.processor, l.whyNotGpu, ms)

            override fun letGo() {
                open = false
                letGo++
            }

            override val stream = Talks.Stream { message, piece ->
                kotlinx.coroutines.runBlocking {
                    if (!open) {
                        val (again, _) = one.engine(key)
                        pace.ranOn(again.processor, again.whyNotGpu)
                        open = true
                    }
                    one.busy { inner.stream.say(message, piece) }
                }
            }

            override fun stop() = inner.stop()

            override fun close() {
                inner.close()
                one.closed(this)
            }
        }
        one.opened(kept)
        return kept
    }

    override suspend fun ask(instruction: String?, message: String, settings: JobSettings): Mind.Answer {
        val (l, ms) = one.engine(settings.engine)
        val a = one.busy { replies.ask(instruction, message, settings) }
        return Mind.Answer(a.text, a.ms, a.turn, l.processor, l.whyNotGpu, ms)
    }

    override suspend fun choose(instruction: String, message: String, places: Int, settings: JobSettings): Int? {
        one.engine(settings.engine)
        return one.busy { replies.choose(instruction, message, places, settings) }
    }

    override suspend fun chooseMany(instruction: String, message: String, places: Int, settings: JobSettings): List<Int>? {
        one.engine(settings.engine)
        return one.busy { replies.chooseMany(instruction, message, places, settings) }
    }

    override suspend fun sheet(instruction: String, material: String, settings: JobSettings, tool: Boolean): com.phenomemental.pocketbeach.bsp.Mark.Sheet? {
        one.engine(settings.engine)
        return one.busy { replies.sheet(instruction, material, settings, tool) }
    }
}
