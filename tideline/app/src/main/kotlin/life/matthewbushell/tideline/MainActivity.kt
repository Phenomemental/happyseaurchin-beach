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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat

/**
 * The one screen (spine:tideline 7.12): whether the phone is listening, the
 * two keys entered once, how landing is going (last landed, how many wait,
 * the last count note), the seal's check, and the messages caught since the
 * app started, newest first.
 */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var access: Button
    private lateinit var landing: TextView
    private lateinit var note: TextView
    private lateinit var keysKept: TextView
    private lateinit var changeKeys: Button
    private lateinit var keyForm: LinearLayout
    private lateinit var latchField: EditText
    private lateinit var readingField: EditText
    private lateinit var seal: TextView
    private lateinit var lastCatch: TextView
    private lateinit var empty: TextView
    private lateinit var list: LinearLayout

    private val main = Handler(Looper.getMainLooper())
    private val changed: () -> Unit = { main.post { show() } }
    private var changing = false

    // The landing worker writes its news where the screen cannot watch it, so
    // the screen looks again every few seconds while it is open.
    private val again = object : Runnable {
        override fun run() {
            show()
            main.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        status = findViewById(R.id.status)
        access = findViewById(R.id.access)
        landing = findViewById(R.id.landing)
        note = findViewById(R.id.note)
        keysKept = findViewById(R.id.keys_kept)
        changeKeys = findViewById(R.id.change_keys)
        keyForm = findViewById(R.id.key_form)
        latchField = findViewById(R.id.latch)
        readingField = findViewById(R.id.reading)
        seal = findViewById(R.id.seal)
        lastCatch = findViewById(R.id.last_catch)
        empty = findViewById(R.id.empty)
        list = findViewById(R.id.list)
        access.setOnClickListener { openAccess() }
        changeKeys.setOnClickListener { changing = true; show() }
        findViewById<Button>(R.id.keep_keys).setOnClickListener { keepKeys() }
        checkSeal()
    }

    override fun onStart() {
        super.onStart()
        Catches.watch(changed)
        main.post(again)
    }

    override fun onStop() {
        main.removeCallbacks(again)
        Catches.unwatch(changed)
        super.onStop()
    }

    // The two words go into encrypted storage and the fields are emptied. What
    // was caught before the keys were entered is sealed and landed now.
    private fun keepKeys() {
        val latch = latchField.text.toString().trim()
        val reading = readingField.text.toString().trim()
        if (latch.isEmpty() || reading.isEmpty()) {
            Toast.makeText(this, R.string.keys_both, Toast.LENGTH_SHORT).show()
            return
        }
        Keys.save(this, latch, reading)
        latchField.text.clear()
        readingField.text.clear()
        changing = false
        Hands.take(this, Catches.all().reversed())
        LandWorker.start(this)
        show()
    }

    private fun showLanding() {
        val haveKeys = Keys.present(this)
        keyForm.visibility = if (!haveKeys || changing) View.VISIBLE else View.GONE
        keysKept.visibility = if (haveKeys && !changing) View.VISIBLE else View.GONE
        changeKeys.visibility = keysKept.visibility

        val waiting = Hands.queue(this).size()
        val landed = Status.lastLanded(this)
        val problem = Status.landingProblem(this)
        landing.text = when {
            !haveKeys -> getString(R.string.landing_no_keys)
            problem != null -> resources.getQuantityString(R.plurals.landing_refused, waiting, problem, waiting)
            landed == 0L -> resources.getQuantityString(R.plurals.landing_ready, waiting, waiting)
            else -> resources.getQuantityString(R.plurals.landing_landed, waiting, time(landed), waiting)
        }
        val noted = Status.lastNoted(this)
        val noteProblem = Status.noteProblem(this)
        note.text = when {
            noteProblem != null -> getString(R.string.note_refused, noteProblem)
            noted != 0L -> getString(R.string.note_sent, time(noted))
            else -> ""
        }
        note.visibility = if (note.text.isEmpty()) View.GONE else View.VISIBLE
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
        showLanding()

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
