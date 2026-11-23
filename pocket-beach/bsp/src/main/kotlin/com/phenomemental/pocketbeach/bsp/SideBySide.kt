package com.phenomemental.pocketbeach.bsp

import java.time.ZonedDateTime

/**
 * Sundial 0.13.1, the check on his phone, widened (spine:pocket-beach
 * 1.1111787199993): the same short conversation run on the GPU and on the
 * CPU, side by side, so he sees whether her stutters (dates garbled as
 * "2222 November 206", doubled letters as "Phenomomomemental", one word
 * over and over) are the GPU's. Her window is the time alone, as her talk
 * has it; his three messages ask for the date and his handle written back,
 * then a longer reply, where a loop shows.
 */
object SideBySide {
    /** Her longest reply in the check, in tokens: long enough for a loop to show, short enough for the CPU. */
    const val LONGEST = 300

    /** Her window: the time alone, as 0.12 gives it. */
    fun window(now: ZonedDateTime): String = HerWindow.released(now)

    /** His three messages, each stamped with the time as her talk stamps it. */
    fun messages(now: ZonedDateTime, handle: String): List<String> {
        val said = listOf(
            "Good morning. What's today's date, written out in full?",
            "My handle is $handle. Write it back to me, then tell me what it might mean.",
            "Tell me, in a paragraph or two, what a walk by the sea in late November is like.",
        )
        return said.mapIndexed { i, s ->
            HerWindow.releasedTurn(s, now.plusSeconds(30L * i), if (i == 0) null else now.plusSeconds(30L * (i - 1)).toInstant().toEpochMilli())
        }
    }
}
