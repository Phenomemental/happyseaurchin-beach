package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** The app's settings: for now, whether each answer shows its time. */
@Composable
fun SettingsScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val app = model.app
    Page(onBack = { nav.back() }, top = { Text("Settings", style = Type.name, color = ink.ink, maxLines = 1) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Show timings", style = Type.body, color = ink.ink)
                Text(
                    "Under every answer from the companion, talk, read or act: how long it took, when her first words came, " +
                        "and whether the phone's GPU or CPU ran her. Off, answers show nothing extra. " +
                        "Your ask-log block keeps the times either way.",
                    style = Type.small, color = ink.soft,
                )
            }
            Switch(
                checked = app.showTimings,
                onCheckedChange = { app.showTimings = it },
                colors = SwitchDefaults.colors(checkedTrackColor = ink.crimson),
                modifier = Modifier.testTag("show-timings"),
            )
        }
    }
}
