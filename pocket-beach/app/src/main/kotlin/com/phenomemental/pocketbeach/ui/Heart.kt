package com.phenomemental.pocketbeach.ui

/**
 * Sundial 0.10: Sundial's heart, drawn exactly as sundial.html draws it
 * (sundial.html:2244-2253, the heart night-watchman.html made): the open ring
 * and the long line with its small loop as two filled outlines in ink, the
 * flow line along the stroke, and the crimson centre, in the heart's own
 * units (viewBox 17 -75 430 430, the dial's centre at 232,140). And the small
 * heart of Keep today and the mark (sundial.html:2321), stroked.
 */
object Heart {
    /** The open ring, a filled outline in ink. */
    const val RING_INK = com.phenomemental.pocketbeach.bsp.HeartInk.RING

    /** The long line with its small loop, a filled outline in ink. */
    const val LINE_INK = com.phenomemental.pocketbeach.bsp.HeartInk.LINE

    /** The flow line along the stroke: the colour inside the ink, the pulse while she writes, the sweep on a keep. */
    const val FLOW = com.phenomemental.pocketbeach.bsp.HeartInk.FLOW

    /** The small heart (sundial.html:2321): the open ring, stroked at 24, in its own viewBox 0 10 390 325. */
    const val SMALL_RING = "M 311.7,49.6 A 124,118 0 1 0 353.3,115.4"

    /** The small heart's line with its loop, stroked at 16. */
    const val SMALL_LINE = "M 375,25 C 320,45 262,78 215,102 C 196,113 186,138 192,156 C 199,172 232,180 262,174 C 300,166 304,122 284,106 C 264,92 240,96 222,108 C 195,118 172,165 150,212 C 122,262 68,300 14,324"

    /** The crimson centre: an ellipse at 250,138, 40 by 24, turned -18 degrees, in both. */
    const val CENTRE_X = 250f
    const val CENTRE_Y = 138f
    const val CENTRE_RX = 40f
    const val CENTRE_RY = 24f
    const val CENTRE_TURN = -18f

    /** The dial's viewBox: x, y, width, height. */
    val VIEW = floatArrayOf(17f, -75f, 430f, 430f)
}
