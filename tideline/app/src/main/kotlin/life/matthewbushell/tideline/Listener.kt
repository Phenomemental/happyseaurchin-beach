package life.matthewbushell.tideline

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * The pair of hands (spine:tideline 1, 7.12 a). Android hands it every
 * notification on the phone; it opens only those from the apps the rules
 * name, and in stage 6.3 it only keeps what it catches for the screen.
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
        catchOf(Shown.of(sbn))?.let { Catches.add(it) }
    }
}
