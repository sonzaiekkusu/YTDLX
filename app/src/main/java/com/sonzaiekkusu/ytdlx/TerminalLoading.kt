package com.sonzaiekkusu.ytdlx

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TerminalLoadingPanel(label: String, url: String?) {
    var dots by remember(label, url) { mutableStateOf(0) }
    LaunchedEffect(label, url) {
        while (true) {
            delay(450)
            dots = (dots + 1) % 4
        }
    }
    val commandUrl = url?.takeLast(56) ?: "url"
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11161C)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("> yt-dlp --dump-single-json", color = Color(0xFF8BE28B), fontFamily = FontFamily.Monospace)
            Text("> $commandUrl", color = Color(0xFFB8C7D9), fontFamily = FontFamily.Monospace)
            Text("[${".".repeat(dots)}${" ".repeat(3 - dots)}] $label", color = Color(0xFFE6EDF3), fontFamily = FontFamily.Monospace)
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF67D17A),
                trackColor = Color(0xFF2B3540),
            )
        }
    }
}
