package com.phenomemental.pocketbeach

import android.app.DownloadManager
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phenomemental.pocketbeach.model.ModelStore
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Why the download manager leaves the model's download pending: run only when
 * asked (`-e diagnose download`), never as part of the ordinary tests. It
 * enqueues the app's own request and three variants, records what the
 * download manager says of each for a while, and what JobScheduler says of
 * the job behind it, then removes every download it made.
 */
@RunWith(AndroidJUnit4::class)
class DownloadDiagnosis {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = Fresh.app
    private val downloads = app.getSystemService(DownloadManager::class.java)

    private fun say(line: String) = Log.i(TAG, line)

    private fun shell(command: String): String =
        instrumentation.uiAutomation.executeShellCommand(command).let { fd ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use { it.readText() }
        }

    private fun row(id: Long): String = downloads.query(DownloadManager.Query().setFilterById(id))?.use { c ->
        if (!c.moveToFirst()) return@use "gone"
        fun col(name: String) = c.getColumnIndex(name).takeIf { it >= 0 }?.let { c.getString(it) }
        "status=${col(DownloadManager.COLUMN_STATUS)} reason=${col(DownloadManager.COLUMN_REASON)} " +
            "bytes=${col(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)} total=${col(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)} " +
            "local=${col(DownloadManager.COLUMN_LOCAL_URI)}"
    } ?: "no cursor"

    @Test
    fun whyTheModelWaits() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("diagnose") == "download")
        val connectivity = app.getSystemService(ConnectivityManager::class.java)
        val network = connectivity.activeNetwork
        say("network: $network caps=${connectivity.getNetworkCapabilities(network)}")
        say("restrict background: ${connectivity.restrictBackgroundStatus}")
        say("sdk=${android.os.Build.VERSION.SDK_INT} release=${android.os.Build.VERSION.RELEASE}")

        val dir = File(app.getExternalFilesDir(null), "models").apply { mkdirs() }
        val store = ModelStore(app)
        val small = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6e5c4f1e395deb959c494953478fa5cec4b8008f/README.md?download=true"
        val variants = listOf(
            "app's own request" to { store.request(File(dir, "a.part")) },
            "into external files dir" to {
                DownloadManager.Request(Uri.parse(ModelStore.URL))
                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                    .setAllowedOverMetered(false)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationInExternalFilesDir(app, null, "models/b.litertlm")
            },
            "any network" to {
                DownloadManager.Request(Uri.parse(ModelStore.URL))
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(File(dir, "c.part")))
            },
            "small file, app's flags" to {
                DownloadManager.Request(Uri.parse(small))
                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                    .setAllowedOverMetered(false)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(File(dir, "d.md")))
            },
        )
        for ((name, request) in variants) {
            val id = try { downloads.enqueue(request()) } catch (e: Exception) {
                say("[$name] enqueue threw: $e")
                continue
            }
            say("[$name] enqueued as $id")
            try {
                for (second in 0..40 step 4) {
                    say("[$name] +${second}s ${row(id)}")
                    Thread.sleep(4000)
                }
                say("[$name] jobscheduler:\n" + shell("dumpsys jobscheduler").lines().let { lines ->
                    val start = lines.indexOfFirst { it.contains("#u") && it.contains("/$id ") || it.contains(" #$id ") || it.contains("/$id\n") }
                    lines.filter { it.contains("pocketbeach") || it.contains("downloads/.DownloadJobService") }.take(40).joinToString("\n") +
                        (if (start >= 0) "\n" + lines.drop(start).take(60).joinToString("\n") else "")
                })
            } finally {
                downloads.remove(id)
            }
        }
        dir.listFiles()?.forEach { it.delete() }
    }

    companion object {
        const val TAG = "PocketDiag"
    }
}
