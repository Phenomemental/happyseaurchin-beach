package com.phenomemental.pocketbeach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.phenomemental.pocketbeach.ui.PocketScreens
import com.phenomemental.pocketbeach.ui.PocketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PocketApp
        setContent { PocketTheme { PocketScreens(app) } }
    }
}
