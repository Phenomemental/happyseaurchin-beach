package com.phenomemental.pocketbeach.model

import android.content.SharedPreferences

/**
 * The jobs the companion does, each with Edge's settings of its own (0.5's
 * Stage 1, spine:pocket-beach 1.111171): model, processor, temperature,
 * top-k, top-p, context and longest reply. Jobs whose model, processor and
 * context are the same share one engine; a job set apart from the others has
 * an engine of its own, loaded beside theirs.
 */
enum class Job(val title: String, val about: String) {
    TALK("Talk", "Her reply to every message in Ask."),
    WRITE("Write", "After her reply, the lines she proposes to write, for your Yes."),
}

/** A job's settings, as Edge shows them for a model. */
data class JobSettings(
    val model: String,
    /** "GPU": the phone's GPU, else its CPU; or "CPU" alone. */
    val processor: String,
    /** Tokens the engine holds, her instruction, the conversation and her reply together. */
    val context: Int,
    val temperature: Double,
    val topK: Int,
    val topP: Double,
    val longestReply: Int,
) {
    /** The engine this job runs on: jobs with the same share one. */
    val engine: EngineKey get() = EngineKey(model, processor, context)

    /** Picking the likeliest word each time: the same reply to the same words. */
    val greedy: Boolean get() = topK <= 1 || temperature <= 0.0

    companion object {
        const val GEMMA_E2B = "Gemma 4 E2B"
        val MODELS = listOf(GEMMA_E2B)
        val PROCESSORS = listOf("GPU", "CPU")
        val CONTEXTS = listOf(2048, 4096, 8192, 16384, 32768)
        val REPLIES = listOf(64, 256, 512, 1024, 2048, 4096)
        const val TEMPERATURE_MAX = 2.0
        const val TOP_K_MAX = 128

        fun of(job: Job): JobSettings = when (job) {
            // Edge's for Gemma 4 E2B: temperature 1.0, top-k 64, top-p 0.95; the context the pocket has run on since 0.3.
            Job.TALK -> JobSettings(GEMMA_E2B, "GPU", 4096, temperature = 1.0, topK = 64, topP = 0.95, longestReply = 1024)
            // 0.4's: the likeliest word each time, so the same words propose the same lines.
            Job.WRITE -> JobSettings(GEMMA_E2B, "GPU", 4096, temperature = 0.0, topK = 1, topP = 1.0, longestReply = 1024)
        }

        /** [job]'s settings as kept in [prefs], each one its default where none is kept. */
        fun read(prefs: SharedPreferences, job: Job): JobSettings {
            val d = of(job)
            val k = "job.${job.name}."
            return JobSettings(
                model = prefs.getString(k + "model", d.model)?.takeIf { it in MODELS } ?: d.model,
                processor = prefs.getString(k + "processor", d.processor)?.takeIf { it in PROCESSORS } ?: d.processor,
                context = prefs.getInt(k + "context", d.context).takeIf { it in CONTEXTS } ?: d.context,
                temperature = prefs.getFloat(k + "temperature", d.temperature.toFloat()).toDouble().coerceIn(0.0, TEMPERATURE_MAX),
                topK = prefs.getInt(k + "topK", d.topK).coerceIn(1, TOP_K_MAX),
                topP = prefs.getFloat(k + "topP", d.topP.toFloat()).toDouble().coerceIn(0.05, 1.0),
                longestReply = prefs.getInt(k + "longestReply", d.longestReply).takeIf { it in REPLIES } ?: d.longestReply,
            ).let { s -> s.copy(temperature = round(s.temperature), topP = round(s.topP)) }
        }

        fun write(prefs: SharedPreferences, job: Job, s: JobSettings) {
            val k = "job.${job.name}."
            prefs.edit()
                .putString(k + "model", s.model).putString(k + "processor", s.processor).putInt(k + "context", s.context)
                .putFloat(k + "temperature", s.temperature.toFloat()).putInt(k + "topK", s.topK).putFloat(k + "topP", s.topP.toFloat())
                .putInt(k + "longestReply", s.longestReply)
                .apply()
        }

        /** To two places, as the settings step. */
        fun round(x: Double): Double = Math.round(x * 100) / 100.0
    }
}

/** What an engine is loaded with: jobs that agree on these share it. */
data class EngineKey(val model: String, val processor: String, val context: Int)
