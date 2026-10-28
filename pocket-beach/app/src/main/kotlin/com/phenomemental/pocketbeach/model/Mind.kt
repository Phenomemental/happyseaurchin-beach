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

    interface Chat : Closeable {
        /** The core's way of speaking to it; blocking, so called off the main thread. */
        val talk: Talk

        fun stop()
    }
}
