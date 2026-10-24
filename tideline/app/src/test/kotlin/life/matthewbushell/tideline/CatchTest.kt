package life.matthewbushell.tideline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/* The rules at spine:tideline 3.1 to 3.3, as the phone applies them. */
class CatchTest {
    private fun shown(
        pkg: String = "com.whatsapp",
        groupSummary: Boolean = false,
        category: String? = "msg",
        ongoing: Boolean = false,
        title: String? = "Alice",
        text: String? = "Are you in tonight?",
        conversationTitle: String? = null,
        shortcutId: String? = "447700900123@s.whatsapp.net",
        lastLine: Line? = null,
        shownAt: Long = 1_761_340_000_000,
        postedAt: Long = 1_761_340_000_500,
    ) = Shown(pkg, groupSummary, category, ongoing, title, text, conversationTitle, shortcutId, lastLine, shownAt, postedAt)

    @Test
    fun `a plain message is caught with its sender, text and time`() {
        val c = catchOf(shown())!!
        assertEquals("WhatsApp", c.app)
        assertEquals("Alice", c.sender)
        assertEquals("Are you in tonight?", c.text)
        assertNull(c.conversation)
        assertEquals(1_761_340_000_000, c.time)
        assertEquals("com.whatsapp|447700900123@s.whatsapp.net", c.key)
    }

    @Test
    fun `each of the five apps is named, and nothing else is caught`() {
        assertEquals("WhatsApp Business", catchOf(shown(pkg = "com.whatsapp.w4b"))!!.app)
        assertEquals("Messenger", catchOf(shown(pkg = "com.facebook.orca"))!!.app)
        assertEquals("Instagram", catchOf(shown(pkg = "com.instagram.android"))!!.app)
        assertEquals("Signal", catchOf(shown(pkg = "org.thoughtcrime.securesms"))!!.app)
        assertNull(catchOf(shown(pkg = "com.google.android.gm")))
        assertNull(catchOf(shown(pkg = "org.telegram.messenger")))
    }

    @Test
    fun `a MessagingStyle notification is read from its last line`() {
        val c = catchOf(
            shown(
                title = "Family: Bea",
                text = "2 new messages",
                conversationTitle = "Family",
                shortcutId = "family@g.us",
                lastLine = Line("Bea", "Dinner at seven", 1_761_339_990_000),
            ),
        )!!
        assertEquals("Bea", c.sender)
        assertEquals("Dinner at seven", c.text)
        assertEquals("Family", c.conversation)
        assertEquals(1_761_339_990_000, c.time)
        assertEquals("com.whatsapp|family@g.us", c.key)
    }

    @Test
    fun `group summaries, calls and ongoing notices are ignored`() {
        assertNull(catchOf(shown(groupSummary = true, text = "3 new messages")))
        assertNull(catchOf(shown(category = "call")))
        assertNull(catchOf(shown(category = "missed_call", text = "Missed voice call")))
        assertNull(catchOf(shown(category = null, ongoing = true, text = "Ongoing voice call")))
    }

    @Test
    fun `anything without a sender and text is ignored`() {
        assertNull(catchOf(shown(title = null)))
        assertNull(catchOf(shown(title = "  ")))
        assertNull(catchOf(shown(text = null)))
        assertNull(catchOf(shown(text = "")))
        // A last line with no sender is one the phone's owner wrote.
        assertNull(catchOf(shown(lastLine = Line(null, "On my way", 1L))))
        assertNull(catchOf(shown(lastLine = Line("Bea", null, 1L))))
    }

    @Test
    fun `an ignored sender is not caught`() {
        assertNull(catchOf(shown(), ignoreSenders = setOf("Alice")))
        assertEquals("Alice", catchOf(shown(), ignoreSenders = setOf("Bea"))!!.sender)
    }

    @Test
    fun `without a message time the shown time is used, then the posting time`() {
        assertEquals(1_761_340_000_000, catchOf(shown(lastLine = Line("Bea", "Hi", 0)))!!.time)
        assertEquals(1_761_340_000_500, catchOf(shown(shownAt = 0))!!.time)
    }

    @Test
    fun `the conversation key falls back to its title, then the sender`() {
        assertEquals("com.whatsapp|Family", catchOf(shown(shortcutId = null, conversationTitle = "Family"))!!.key)
        assertEquals("com.whatsapp|Alice", catchOf(shown(shortcutId = null))!!.key)
    }

    @Test
    fun `the same message posted again is kept once`() {
        val first = catchOf(shown(text = "Kept once", postedAt = 1))!!
        val again = catchOf(shown(text = "Kept once", postedAt = 2))!!
        assertTrue(Catches.add(first))
        assertFalse(Catches.add(again))
        assertTrue(Catches.add(catchOf(shown(text = "Kept once", shownAt = 1_761_340_060_000))!!))
        assertEquals("Kept once", Catches.all().first().text)
    }
}
