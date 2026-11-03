package com.phenomemental.pocketbeach.bsp

/**
 * Sundial 0.10.1: her reply in the parts his cook names (TALK's THE
 * REFLECTION and THE QUESTION), through the engine's tool call on E4B.
 *
 * On his Pixel on 3 November her reply to "I've done some of the id checks
 * today, …" showed only part two. Every attempt is now kept exactly as her
 * tool call returned it, for the workings; when part one comes back empty
 * she is asked once more; and a reply with any part still empty is never
 * shown as if whole: she is asked free instead, as his cook says ("when her
 * answer isn't whole, her reply stands as she writes it").
 */
object HerReply {
    /** [attempts]: each tool call as she returned it (null: no whole call); [parts]: the parts shown, or null when her answer isn't whole. */
    class InParts(val attempts: List<Map<String, String>?>, val parts: Map<String, String>?)

    /** The app's line when part one came back empty, added to her turn for the one more ask. */
    const val AGAIN = "The app: your reply's first part came back empty. Give both parts this time: part one gives back what he said in this message, " +
        "in his own words, beginning with You; part two is your question."

    /**
     * Ask for her reply in [asked] parts: [ask] once, and once more ([ask]
     * with `again`) when part one comes back empty. [clean] is what Ask does
     * to a part before it is shown. Whole only when every part holds words.
     */
    fun inParts(asked: List<String>, ask: (again: Boolean) -> Map<String, String>?, clean: (String) -> String = { it.trim() }): InParts {
        if (asked.isEmpty()) return InParts(emptyList(), null)
        val attempts = mutableListOf<Map<String, String>?>()
        var got = ask(false).also { attempts += it }
        if (got != null && clean(got[asked.first()].orEmpty()).isBlank()) got = ask(true).also { attempts += it }
        val cleaned = got?.let { g -> asked.associateWith { clean(g[it].orEmpty()) } }
        return InParts(attempts, cleaned?.takeIf { c -> c.values.all { it.isNotBlank() } })
    }

    /** One attempt as the workings record it: each part exactly as her tool call returned it, an empty one shown empty. */
    fun said(asked: List<String>, attempt: Map<String, String>?): String =
        attempt?.let { a -> asked.joinToString("; ") { "$it “${a[it].orEmpty()}”" } } ?: "no whole tool call came back"
}
