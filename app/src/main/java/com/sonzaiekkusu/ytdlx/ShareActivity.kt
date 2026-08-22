package com.sonzaiekkusu.ytdlx

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import kotlinx.coroutines.launch

private sealed interface ShareMetadataState {
    data object Loading : ShareMetadataState
    data class Ready(val video: VideoMetadata) : ShareMetadataState
    data class Error(val message: String) : ShareMetadataState
}

class ShareActivity : ComponentActivity() {
    private val engine = YtdlEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rawText = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: intent?.dataString
        val sharedUrl = rawText?.asYouTubeUrl()

        setContent {
            val themeMode = remember {
                getSharedPreferences("ytdlx_settings", MODE_PRIVATE)
                    .getString("theme_mode", ThemeMode.SYSTEM.name)
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM
            }
            YtdlxTheme(themeMode) {
                ShareFloatingPanel(
                    url = sharedUrl,
                    engine = engine,
                    onClose = { finish() },
                    onDownload = { url, title, quality, estimatedSizeBytes ->
                        WorkManager.getInstance(applicationContext).enqueue(
                            createDownloadWork(url, title, quality, estimatedSizeBytes),
                        )
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun ShareFloatingPanel(
    url: String?,
    engine: YtdlEngine,
    onClose: () -> Unit,
    onDownload: (String, String, QualityOption, Long?) -> Unit,
) {
    var state by remember(url) {
        mutableStateOf<ShareMetadataState>(
            if (url == null) ShareMetadataState.Error("URL YouTube tidak ditemukan") else ShareMetadataState.Loading,
        )
    }
    var quality by rememberSaveable { mutableStateOf(QualityOption.BEST) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(url) {
        if (url != null) {
            state = ShareMetadataState.Loading
            state = runCatching { engine.metadata(context, url) }
                .fold(
                    onSuccess = { ShareMetadataState.Ready(it) },
                    onFailure = { ShareMetadataState.Error(it.message ?: "Gagal mengambil metadata") },
                )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("YTDLX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = onClose) { Text("Tutup") }
                }

                when (val current = state) {
                    ShareMetadataState.Loading -> TerminalLoadingPanel("Mengambil metadata YouTube", url)
                    is ShareMetadataState.Error -> {
                        Text(current.message, color = MaterialTheme.colorScheme.error)
                        Text("Pastikan URL berasal dari video YouTube yang dapat diakses.")
                    }
                    is ShareMetadataState.Ready -> {
                        Text(current.video.title, fontWeight = FontWeight.Bold)
                        Text(current.video.channel ?: current.video.uploader ?: "Channel tidak diketahui")
                        Text("Durasi: ${current.video.duration?.let(::shareFormatDuration) ?: "-"}")
                        Text("Pilih kualitas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        QualityOption.entries.forEach { option ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = quality == option,
                                    onClick = { quality = option },
                                )
                                Text(option.label)
                            }
                        }
                        val estimatedSizeBytes = current.video.estimateSize(quality)
                        Text("Perkiraan ukuran: ${estimatedSizeBytes.formatFileSize()}")
                        Button(
                            onClick = { onDownload(url!!, current.video.title, quality, estimatedSizeBytes) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Download ${quality.label}") }
                    }
                }
            }
        }
    }
}

private fun shareFormatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remaining = seconds % 60
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:${remaining.toString().padStart(2, '0')}"
    else "$minutes:${remaining.toString().padStart(2, '0')}"
}
