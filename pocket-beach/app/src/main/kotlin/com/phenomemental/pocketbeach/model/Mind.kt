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

    /** Whether it is in memory now (asking it first loads it). */
    val loaded: Boolean

    /** Which of the phone's processors runs it, once loaded. */
    val processor: String?

    /** A conversation, its standing [instruction] given; close it when the Ask is over. */
    suspend fun chat(instruction: String): Chat

    /** Load it now if it isn't loaded, saying how long that took (null: it was loaded already, or there is nothing to load). */
    suspend fun warm(): Long? = null

    /**
     * 0.4: one conversation, kept while Ask is open, its [instruction]
     * processed once; [history], the last exchanges (Matthew's words, hers),
     * carried into it. Replies are streamed, nothing held.
     */
    suspend fun talker(instruction: String, history: List<Pair<String, String>>): Talker

    interface Talker : Closeable {
        val stream: com.phenomemental.pocketbeach.bsp.Talks.Stream

        /** Where and how fast it is answering: each reply a turn, with its first token. */
        val pace: Pace

        fun stop()
    }

    interface Chat : Closeable {
        /** The core's way of speaking to it; blocking, so called off the main thread. */
        val talk: Talk

        /** How its answers are being made: where, and how fast. */
        val pace: Pace

        fun stop()
    }
}
