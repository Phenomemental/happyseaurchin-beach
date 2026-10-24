package life.matthewbushell.tideline

import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateUtils
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.DateFormat

/**
 * The one screen (spine:tideline 7.12): whether the phone is listening, the
 * seal's check, and in stage 6.3 the messages caught, newest first.
 */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var access: Button
    private lateinit var seal: TextView
    private lateinit var lastCatch: TextView
    private lateinit var empty: TextView
    private lateinit var list: LinearLayout

    private val main = Handler(Looper.getMainLooper())
    private val changed: () -> Unit = { main.post { show() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        status = findViewById(R.id.status)
        access = findViewById(R.id.access)
        seal = findViewById(R.id.seal)
        lastCatch = findViewById(R.id.last_catch)
        empty = findViewById(R.id.empty)
        list = findViewById(R.id.list)
        access.setOnClickListener { openAccess() }
        checkSeal()
    }

    override fun onStart() {
        super.onStart()
        Catches.watch(changed)
        show()
    }

    override fun onStop() {
        Catches.unwatch(changed)
        super.onStop()
    }

    private fun listener() = ComponentName(this, Listener::class.java)

    private fun show() {
        val granted = getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(listener())
        status.setText(
            when {
                granted && Catches.connected -> R.string.listening
                granted -> R.string.connecting
                else -> R.string.not_listening
            },
        )
        access.visibility = if (granted) View.GONE else View.VISIBLE

        val caught = Catches.all()
        lastCatch.text = if (caught.isEmpty()) {
            getString(R.string.no_catch)
        } else {
            resources.getQuantityString(R.plurals.last_catch, caught.size, time(caught.first().time), caught.size)
        }
        empty.visibility = if (caught.isEmpty()) View.VISIBLE else View.GONE
        list.removeAllViews()
        caught.forEach { list.addView(row(it)) }
    }

    private fun row(catch: Catch): View {
        val head = TextView(this).apply {
            setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Small)
            text = if (catch.conversation == null) {
                getString(R.string.row_head, catch.app, catch.sender, time(catch.time))
            } else {
                getString(R.string.row_head_in, catch.app, catch.sender, catch.conversation, time(catch.time))
            }
        }
        val words = TextView(this).apply {
            setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium)
            text = catch.text
            setTextIsSelectable(true)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val gap = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, gap, 0, gap)
            addView(head)
            addView(words)
        }
    }

    private fun time(at: Long): String =
        DateUtils.formatSameDayTime(at, System.currentTimeMillis(), DateFormat.MEDIUM, DateFormat.SHORT).toString()

    // Straight to Tideline's own switch where Android offers it (11 and later).
    private fun openAccess() {
        val intent = if (Build.VERSION.SDK_INT >= 30) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener().flattenToString())
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
        try {
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun checkSeal() {
        SealOnPhone.result?.let { showSeal(it); return }
        seal.setText(R.string.seal_checking)
        Thread {
            val result = SealOnPhone.run()
            main.post { if (!isDestroyed) showSeal(result) }
        }.start()
    }

    private fun showSeal(result: SealOnPhone.Result) {
        seal.text = if (result.passed) {
            getString(R.string.seal_passes, result.keyMillis / 1000.0)
        } else {
            getString(R.string.seal_fails)
        }
    }
}
