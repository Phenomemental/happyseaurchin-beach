package com.phenomemental.pocketbeach.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.phenomemental.pocketbeach.MainActivity
import com.phenomemental.pocketbeach.PocketApp
import com.phenomemental.pocketbeach.net.ModelFetch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Keeps the model's download going while Matthew is away from the screen: a
 * foreground service, started only by his tap on "Get the model", showing
 * how far it has come in its notification, and gone when the download ends.
 * Android allows a data-sync service six hours a day; if it calls time, the
 * download stops, keeping what has come, to carry on from.
 */
class FetchService : Service() {
    companion object {
        private const val CHANNEL = "model"
        private const val NOTE = 1
        private val running = AtomicBoolean(false)

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FetchService::class.java))
        }
    }

    private var lastNote = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val models = (application as PocketApp).models
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "The companion's model", NotificationManager.IMPORTANCE_LOW),
        )
        startForeground(NOTE, note(null), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        if (running.compareAndSet(false, true)) {
            thread(name = "model-fetch") {
                try {
                    models.fetchNow { p -> show(p) }
                } finally {
                    running.set(false)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        (application as PocketApp).models.stop()
    }

    private fun show(p: ModelFetch.Progress) {
        val now = System.currentTimeMillis()
        if (now - lastNote < 1000) return
        lastNote = now
        getSystemService(NotificationManager::class.java).notify(NOTE, note(p))
    }

    private fun note(p: ModelFetch.Progress?): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val builder = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Fetching the companion's model")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (p == null) return builder.setContentText("Starting…").setProgress(0, 0, true).build()
        val percent = (p.done * 100 / ModelStore.SIZE).toInt()
        return builder
            .setContentText(p.note ?: "${ModelStore.gb(p.done)} of ${ModelStore.gb(ModelStore.SIZE)}")
            .setProgress(100, percent, false)
            .build()
    }
}
