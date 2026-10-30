package com.phenomemental.pocketbeach.model

import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * How the companion's answers in one Ask were made: on which of the phone's
 * processors, why not on its GPU when it wasn't, how long the model took to
 * load when it had to, and, exchange by exchange, how many tokens it read
 * (the instruction and Matthew's words, then each message after) and wrote,
 * and how fast, as the engine counts them.
 */
class Pace(processor: String?, val whyNotGpu: String?, val loadMs: Long?) {
    class Turn(val read: Int, val readPerSecond: Double, val wrote: Int, val writePerSecond: Double, val firstTokenSeconds: Double? = null)

    /** Where it ran; "CPU" once an answer that failed on the GPU was answered on the CPU (0.6.2). */
    @Volatile var processor: String? = processor
        private set

    val turns: MutableList<Turn> = CopyOnWriteArrayList()

    /** Plain lines on where the jobs ran, said under her reply: a job on the CPU because another holds the GPU, an answer the GPU failed (0.6.2). */
    val notes: MutableList<String> = CopyOnWriteArrayList()

    /** The GPU failed on an answer, and she answered on the CPU. */
    fun gpuFailed() {
        processor = "CPU"
        if (GPU_FAILED !in notes) notes.add(GPU_FAILED)
    }

    /** As this one, loading timed only if [loadMs]; the turns left to add. */
    fun copy(loadMs: Long? = this.loadMs): Pace = Pace(processor, whyNotGpu, loadMs).also { it.notes.addAll(notes) }

    companion object {
        const val GPU_FAILED = "The GPU failed on this answer; she answered on the CPU."
    }

    /** Beside the time: where it ran ("on the GPU"), or nothing when that isn't known. */
    val where: String get() = processor?.let { "on the $it" }.orEmpty()

    /** The rest in a line: why not the GPU, loading, reading and writing; null when nothing was counted. */
    fun detail(): String? = buildList {
        // On the CPU because the GPU couldn't, not because the CPU was asked for.
        if (processor == "CPU" && whyNotGpu != null) add("The GPU couldn't run it: $whyNotGpu.")
        addAll(notes)
        loadMs?.let { add("Loading the model: ${secs(it / 1000.0)} s.") }
        if (turns.isNotEmpty()) {
            val read = turns.sumOf { it.read }
            val wrote = turns.sumOf { it.wrote }
            val reading = turns.sumOf { if (it.readPerSecond > 0) it.read / it.readPerSecond else 0.0 }
            val writing = turns.sumOf { if (it.writePerSecond > 0) it.wrote / it.writePerSecond else 0.0 }
            add("Reading ${count(read)} tokens: ${secs(reading)} s.")
            add("Writing ${count(wrote)} tokens: ${secs(writing)} s" + (if (writing > 0) ", ${secs(wrote / writing)} a second." else "."))
        }
    }.joinToString(" ").ifEmpty { null }

    private fun secs(s: Double) = String.format(Locale.UK, "%.1f", s)

    private fun count(n: Int) = String.format(Locale.UK, "%,d", n)
}
