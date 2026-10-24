package life.matthewbushell.tideline

import android.app.Notification
import android.app.Person
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification

/**
 * One caught message, as spine:tideline 4.4 lays it down: the app, the sender
 * and the conversation as shown, the text, the time on the phone, and the
 * conversation key the phone will use to find the reply button.
 */
data class Catch(
    val app: String,
    val sender: String,
    val conversation: String?,
    val text: String,
    val time: Long,
    val key: String,
) {
    /** The same message posted again (WhatsApp re-posts its whole bundle) has the same mark. */
    val mark: String get() = "$key\n$time\n$text"
}

/** The last line of a MessagingStyle notification. */
class Line(val sender: String?, val text: String?, val time: Long)

/** What one notification shows, read out of it once (spine:tideline 7.12 a). */
class Shown(
    val pkg: String,
    val groupSummary: Boolean,
    val category: String?,
    val ongoing: Boolean,
    val title: String?,
    val text: String?,
    val conversationTitle: String?,
    val shortcutId: String?,
    val lastLine: Line?,
    val shownAt: Long,
    val postedAt: Long,
) {
    companion object {
        /** Read a notification. Call only for a package the rules name: nothing else is opened. */
        fun of(sbn: StatusBarNotification): Shown {
            val n = sbn.notification
            val x = n.extras
            return Shown(
                pkg = sbn.packageName,
                groupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
                category = n.category,
                ongoing = n.flags and Notification.FLAG_ONGOING_EVENT != 0,
                title = x.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                text = x.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                conversationTitle = x.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString(),
                shortcutId = n.shortcutId,
                lastLine = lastLine(x),
                shownAt = n.`when`,
                postedAt = sbn.postTime,
            )
        }

        // MessagingStyle keeps its lines as bundles under EXTRA_MESSAGES, each
        // with "text", "time" and the sender as "sender" or "sender_person". A
        // line with neither is one the phone's owner wrote.
        private fun lastLine(x: Bundle): Line? {
            val lines = if (Build.VERSION.SDK_INT >= 33) {
                x.getParcelableArray(Notification.EXTRA_MESSAGES, Parcelable::class.java)
            } else {
                @Suppress("DEPRECATION")
                x.getParcelableArray(Notification.EXTRA_MESSAGES)
            }
            val last = lines?.lastOrNull() as? Bundle ?: return null
            val person = if (Build.VERSION.SDK_INT >= 33) {
                last.getParcelable("sender_person", Person::class.java)
            } else {
                @Suppress("DEPRECATION")
                last.getParcelable("sender_person")
            }
            val sender = last.getCharSequence("sender")?.toString() ?: person?.name?.toString()
            return Line(sender, last.getCharSequence("text")?.toString(), last.getLong("time"))
        }
    }
}

/** The catch a notification makes under the rules, or null when the rules pass it by. */
fun catchOf(
    shown: Shown,
    listenTo: Map<String, String> = Rules.listenTo,
    ignoreSenders: Set<String> = Rules.ignoreSenders,
): Catch? {
    val app = listenTo[shown.pkg] ?: return null
    // 3.2 IGNORE: group summaries ('3 new messages') ...
    if (shown.groupSummary) return null
    // ... calls, which ring as ongoing notifications as well as by category ...
    if (shown.ongoing || shown.category == Notification.CATEGORY_CALL || shown.category == CATEGORY_MISSED_CALL) return null
    // ... and anything without a sender and text. A MessagingStyle notification
    // says both in its last line; any other in its title and text.
    val line = shown.lastLine
    val sender = (if (line != null) line.sender else shown.title)?.trim()
    val text = (if (line != null) line.text else shown.text)?.trim()
    if (sender.isNullOrEmpty() || text.isNullOrEmpty()) return null
    if (sender in ignoreSenders) return null
    // The time is the message's own, so the same message posted again is the
    // same catch (3.2's repeated bundles), where the posting time would not be.
    val time = line?.time?.takeIf { it > 0 } ?: shown.shownAt.takeIf { it > 0 } ?: shown.postedAt
    val conversation = shown.conversationTitle?.trim()?.takeIf { it.isNotEmpty() }
    val key = shown.pkg + "|" + (shown.shortcutId ?: conversation ?: sender)
    return Catch(app, sender, conversation, text, time, key)
}

// Notification.CATEGORY_MISSED_CALL, by value, since it arrived after Android 10.
private const val CATEGORY_MISSED_CALL = "missed_call"
