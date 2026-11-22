package com.phenomemental.pocketbeach.model

import android.content.SharedPreferences

/** Sundial 0.12: how the phone keeps each job's settings; the settings themselves are the core's ([JobSettings]). */
object JobPrefs {
    /** [job]'s settings as kept in [prefs], each one its default where none is kept. */
    fun read(prefs: SharedPreferences, job: Job): JobSettings {
        val d = JobSettings.of(job)
        val k = "job.${job.name}."
        return JobSettings(
            model = prefs.getString(k + "model", d.model)?.takeIf { it in JobSettings.MODELS } ?: d.model,
            processor = prefs.getString(k + "processor", d.processor)?.takeIf { it in JobSettings.PROCESSORS } ?: d.processor,
            context = prefs.getInt(k + "context", d.context).takeIf { it in JobSettings.CONTEXTS } ?: d.context,
            temperature = prefs.getFloat(k + "temperature", d.temperature.toFloat()).toDouble().coerceIn(0.0, JobSettings.TEMPERATURE_MAX),
            topK = prefs.getInt(k + "topK", d.topK).coerceIn(1, JobSettings.TOP_K_MAX),
            topP = prefs.getFloat(k + "topP", d.topP.toFloat()).toDouble().coerceIn(0.05, 1.0),
            longestReply = prefs.getInt(k + "longestReply", d.longestReply).takeIf { it in JobSettings.REPLIES } ?: d.longestReply,
        ).let { s -> s.copy(temperature = JobSettings.round(s.temperature), topP = JobSettings.round(s.topP)) }
    }

    fun write(prefs: SharedPreferences, job: Job, s: JobSettings) {
        val k = "job.${job.name}."
        prefs.edit()
            .putString(k + "model", s.model).putString(k + "processor", s.processor).putInt(k + "context", s.context)
            .putFloat(k + "temperature", s.temperature.toFloat()).putInt(k + "topK", s.topK).putFloat(k + "topP", s.topP.toFloat())
            .putInt(k + "longestReply", s.longestReply)
            .apply()
    }
}
