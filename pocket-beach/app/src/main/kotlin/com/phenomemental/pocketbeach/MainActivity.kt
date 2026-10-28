package com.phenomemental.pocketbeach

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.phenomemental.pocketbeach.ui.PocketScreens
import com.phenomemental.pocketbeach.ui.PocketTheme
import com.phenomemental.pocketbeach.ui.Received

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PocketApp
        if (savedInstanceState == null) receive(intent)
        setContent { PocketTheme { PocketScreens(app) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        receive(intent)
    }

    /** A block shared in from another app waits in the inbox, as data, until Matthew looks at it. */
    private fun receive(intent: Intent?) {
        intent ?: return
        Received.from(this, intent)?.let { (application as PocketApp).inbox = it }
    }
}
