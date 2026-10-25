package life.matthewbushell.tideline

import android.content.Context

/*
 * What the screen says about landing (spine:tideline 7.12): when a line last
 * landed, when the last count note went, and the beach's own words when it
 * refused something. The beach's words name blocks and positions, never a
 * message; nothing of a message is kept here.
 */
object Status {
    private const val PREFS = "status"

    fun landed(context: Context, at: Long) =
        prefs(context).edit().putLong("landed", at).remove("landingProblem").apply()

    fun landingProblem(context: Context, reason: String) =
        prefs(context).edit().putString("landingProblem", reason).apply()

    fun noted(context: Context, at: Long) =
        prefs(context).edit().putLong("noted", at).remove("noteProblem").apply()

    fun noteProblem(context: Context, reason: String) =
        prefs(context).edit().putString("noteProblem", reason).apply()

    fun lastLanded(context: Context): Long = prefs(context).getLong("landed", 0)
    fun lastNoted(context: Context): Long = prefs(context).getLong("noted", 0)
    fun landingProblem(context: Context): String? = prefs(context).getString("landingProblem", null)
    fun noteProblem(context: Context): String? = prefs(context).getString("noteProblem", null)

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
