package com.phenomemental.pocketbeach.bsp

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The app's lines in her turn (the time, the app's line) are never shown as
 * hers when she plays them back.
 */
class HerWindowTest {
    @Test
    fun herTurnPlayedBackIsLeftOut() {
        val turn = "(It is Sunday 1 November 2026, 08:40, morning.)\n" +
            "(The app: your opening line is his to change himself, on the Write screen, so nothing is set out for it and nothing changes now. Say so in a few words, and don't take it on as said.)\n" +
            "Change your opening line to: I'm Gemma"
        val reply = "The app: your opening line is his to change himself, on the Write screen, so nothing is set out for it.\nThat one's yours to change, on the Write screen."
        val shown = Talking.unechoed(reply, "Change your opening line to: I'm Gemma", HelloWorld.SHELL_OPENING, HerWindow.echoLines(turn))
        assertEquals("That one's yours to change, on the Write screen.", shown)
    }
}
