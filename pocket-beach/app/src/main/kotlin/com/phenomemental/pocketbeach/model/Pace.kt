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
class Pace(val processor: String?, val whyNotGpu: String?, val loadMs: Long?) {
    class Turn(val read: Int, val readPerSecond: Double, val wrote: Int, val writePerSecond: Double, val firstTokenSeconds: Double? = null)

    val turns: MutableList<Turn> = CopyOnWriteArrayList()

    /** Beside the time: where it ran ("on the GPU"), or nothing when that isn't known. */
    val where: String get() = processor?.let { "on the $it" }.orEmpty()

    /** The rest in a line: why not the GPU, loading, reading and writing; null when nothing was counted. */
    fun detail(): String? = buildList {
        if (processor == "CPU") add(whyNotGpu?.let { "The GPU couldn't run it: $it." } ?: "The GPU couldn't run it.")
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
