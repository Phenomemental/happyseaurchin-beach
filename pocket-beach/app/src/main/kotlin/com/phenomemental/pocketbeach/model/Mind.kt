package com.phenomemental.pocketbeach.model

import com.phenomemental.pocketbeach.bsp.Talk
import java.io.Closeable

/**
 * What the Ask screen asks: on the phone, Gemma; in the device tests, a
 * stand-in that answers as the shell's recipe does, since no test can run
 * the model.
 */
interface Mind {
    /** Whether it can be asked yet: the model's state on this phone. */
    fun state(): ModelStore.State

    /** The state of the model named [model] on this phone (Gemma 4 E2B or E4B). */
    fun state(model: String): ModelStore.State = state()

    /** One reply to one message: her words, how long they took, and the engine's count, where it ran. */
    class Answer(val text: String, val ms: Long, val turn: Pace.Turn?, val processor: String?, val whyNotGpu: String?, val loadMs: Long?, val gpuFailed: Boolean = false)

    /** One message in a conversation of its own, opened and closed for it: as the Sort job asks a piece at a time (0.6). */
    suspend fun ask(instruction: String?, message: String, settings: JobSettings): Answer

    /**
     * Rockpool: her choice from a ring of at most nine (a step of his cook
     * that names "she"): the place's number, or null when her answer isn't
     * whole. Only her digit is taken; her words are never shown. Gemma makes
     * it through the engine's own tool call; this, the free digit.
     */
    suspend fun choose(instruction: String, message: String, places: Int, settings: JobSettings): Int? =
        Regex("""^\D{0,3}([1-9])""").find(ask("$instruction Answer with the place's digit first.", message, settings).text.trim())?.groupValues?.get(1)?.toInt()?.takeIf { it in 1..places }

    /**
     * Sundial 0.10: her placing, every place of a ring of [places] his words
     * touch (pool numbers 1 to [places]), through the engine's tool call; null
     * when her answer isn't whole. The free mind names none.
     */
    suspend fun chooseMany(instruction: String, message: String, places: Int, settings: JobSettings): List<Int>? = null

    /**
     * Sundial 0.10: the day's sheet, drawn from [material] as [instruction]
     * says: through the tool call when [tool] (E4B), else written in plain
     * lines, the title first and tomorrow last after "Tomorrow:", and read as
     * Sundial reads a day (mark.js:101-126). Null when she gave no title.
     */
    suspend fun sheet(instruction: String, material: String, settings: JobSettings, tool: Boolean): com.phenomemental.pocketbeach.bsp.Mark.Sheet? =
        sheetOf(ask("$instruction Write it in plain lines: the title first, then two to four lines, then “Tomorrow: …” if he voiced one, and nothing else.", material, settings).text)

    /** Whether it is in memory now (asking it first loads it). */
    val loaded: Boolean

    /** Which of the phone's processors runs it, once loaded. */
    val processor: String?

    /** A conversation, its standing [instruction] given; close it when the Ask is over. */
    suspend fun chat(instruction: String): Chat

    /** Whether the engine [settings] run on is in memory now. */
    fun loaded(settings: JobSettings): Boolean = loaded

    /** Load the engine [settings] run on now if it isn't loaded, saying how long that took (null: it was loaded already, or there is nothing to load). */
    suspend fun warm(settings: JobSettings = JobSettings.of(Job.WRITE)): Long? = null

    /** Whether an answer on the GPU failed for the engine [settings] run on, so it runs on the CPU until the app starts again (0.6.2). */
    fun gpuFailed(settings: JobSettings): Boolean = false

    /** Run it on [processor] alone ("CPU"), or as ever (null: the GPU, else the CPU); it is let go, to load that way next. For the bench. */
    suspend fun only(processor: String?) {}

    /**
     * 0.4: one conversation, kept while Ask is open, its [instruction]
     * processed once; [history], the last exchanges (Matthew's words, hers),
     * carried into it. Replies are streamed; if [held], each is held by the
     * engine to begin with her route word
     * ([com.phenomemental.pocketbeach.bsp.Talks.HELD]) where it allows. Ask's
     * is free: on Matthew's Pixel (the bench of 0.4.1) held and free gave the
     * same answers, and free was never slower (holding cost about 2.8 s an
     * ask when each had a conversation of its own). 0.5: drawn, and run on
     * the engine, as [settings] say: a job's settings.
     */
    suspend fun talker(instruction: String, history: List<Pair<String, String>>, held: Boolean = false, settings: JobSettings = JobSettings.of(Job.WRITE)): Talker

    interface Talker : Closeable {
        val stream: com.phenomemental.pocketbeach.bsp.Talks.Stream

        /** Where and how fast it is answering: each reply a turn, with its first token. */
        val pace: Pace

        /** How her first word is chosen: held by the engine, or free and why. */
        val route: String get() = "free"

        /** Stop the reply under way; the conversation can't be asked again after, so it is let go. */
        fun stop()

        /**
         * Sundial 0.10: her reply to [message] in its named [parts] ("THE
         * REFLECTION", "THE QUESTION"), through the engine's tool call; null
         * when her answer isn't whole, and the caller asks her free.
         */
        fun parts(message: String, parts: List<String>): Map<String, String>? = null
    }

    companion object {
        /** A sheet written in plain lines, read as Sundial reads a day: the title, the lines, "Tomorrow: …"; a places line or the own line dropped. */
        fun sheetOf(text: String): com.phenomemental.pocketbeach.bsp.Mark.Sheet? {
            val cleaned = text.lines().map { it.trim().trimStart('-', '*', '•', ' ').trim() }.filter { it.isNotEmpty() }
                .map { it.removePrefix("Title:").removePrefix("title:").trim() }.joinToString("\n")
            val d = com.phenomemental.pocketbeach.bsp.Mark.read(cleaned)
            if (d.title.isBlank()) return null
            return com.phenomemental.pocketbeach.bsp.Mark.Sheet(d.title, d.lines.take(4), d.tomorrow)
        }

        /** A part's field name in a tool, from its name in his cook: "THE REFLECTION" is "reflection". */
        fun field(part: String): String = part.removePrefix("THE ").trim().lowercase().replace(' ', '_')
    }

    interface Chat : Closeable {
        /** The core's way of speaking to it; blocking, so called off the main thread. */
        val talk: Talk

        /** How its answers are being made: where, and how fast. */
        val pace: Pace

        fun stop()
    }
}
