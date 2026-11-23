package com.phenomemental.pocketbeach.bsp

import kotlin.test.Test
import kotlin.test.assertEquals

/** Sundial 0.13.5: a block's shape, worked out by the app, as readable text. */
class BlockShapeTest {
    @Test
    fun theShapeIsCountedAndSaidPlainly() {
        val nine = JsObj().also { o -> o["_"] = str("Nine of them."); for (d in 1..9) o["$d"] = str("Item $d") }
        val block = JsObj.of(
            "_" to str("The orchard."),
            "1" to JsObj.of("_" to str("Autumn"), "1" to str("pick the apples"), "2" to JsObj.of("_" to str("store them"), "1" to str("in the shed"))),
            "2" to str(""),
            "3" to nine,
        )
        assertEquals(
            "(The shape of his block orchard, worked out by the app:)\n" +
                "It goes 3 levels deep beneath its opening line, with 15 parts in all.\n" +
                "At each depth: 3 parts at the first level, 11 parts at the second level, 1 part at the third level.\n" +
                "Each part beneath the opening line, and how deep it goes:\n" +
                "  1  goes 2 levels deeper, 3 parts beneath it\n" +
                "  2  nothing beneath it\n" +
                "  3  goes 1 level deeper, 9 parts beneath it\n" +
                "Full at nine, with no room for a tenth beside them: beneath 3.\n" +
                "Empty, with nothing written there: 2.\n" +
                "It has grown most at 3: 9 parts beneath it, 1 level deep.",
            BlockShape.of("orchard", block),
        )
        assertEquals("(The shape of his block notes, worked out by the app:)\nIt is its opening line alone, with no parts beneath it.", BlockShape.of("notes", JsObj.of("_" to str("Notes."))))
    }
}
