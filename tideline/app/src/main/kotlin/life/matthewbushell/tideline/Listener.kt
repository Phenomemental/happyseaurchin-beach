package life.matthewbushell.tideline

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * The pair of hands (spine:tideline 1, 7.12 a). Android hands it every
 * notification on the phone; it opens only those from the apps the rules
 * name, shows each catch on the screen, and hands it on to be sealed and
 * landed in the catch.
 */
class Listener : NotificationListenerService() {
    override fun onListenerConnected() {
        Catches.connected = true
    }

    override fun onListenerDisconnected() {
        Catches.connected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in Rules.listenTo) return
        val catch = catchOf(Shown.of(sbn)) ?: return
        if (Catches.add(catch)) Hands.take(this, listOf(catch))
    }
}
