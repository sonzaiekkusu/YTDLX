package com.sonzaiekkusu.ytdlx

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.work.WorkManager

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
            val language = getSharedPreferences("ytdlx_settings", MODE_PRIVATE)
                .getString(LANGUAGE_PREFERENCE_KEY, AppLanguage.INDONESIAN.name)
                ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.INDONESIAN
            val strings = language.strings()
            YtdlxTheme(themeMode) {
                ShareFloatingPanel(
                    url = sharedUrl,
                    engine = engine,
                    strings = strings,
                    onClose = { finish() },
                    onDownload = { url, title, quality ->
                        WorkManager.getInstance(applicationContext).enqueue(
                            createDownloadWork(url, title, quality),
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
    strings: AppStrings,
    onClose: () -> Unit,
    onDownload: (String, String, QualityOption) -> Unit,
) {
    var state by remember(url) {
        mutableStateOf<ShareMetadataState>(
            if (url == null) ShareMetadataState.Error(strings.invalidSharedUrl) else ShareMetadataState.Loading,
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
                    onFailure = { ShareMetadataState.Error(it.message ?: strings.metadataLoading) },
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
                    Text(strings.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = onClose) { Text(strings.close) }
                }

                when (val current = state) {
                    ShareMetadataState.Loading -> TerminalLoadingPanel(strings.metadataLoading, url)
                    is ShareMetadataState.Error -> {
                        Text(current.message, color = MaterialTheme.colorScheme.error)
                        Text(strings.inaccessibleYoutubeUrl)
                    }
                    is ShareMetadataState.Ready -> {
                        Text("${strings.title}: ${current.video.title}", fontWeight = FontWeight.Bold)
                        Text("${strings.channel}: ${current.video.channel ?: current.video.uploader ?: strings.unknownChannel}")
                        Text("${strings.duration}: ${current.video.duration?.let(::shareFormatDuration) ?: "-"}")
                        Text(strings.chooseQuality, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        QualityOption.entries.forEach { option ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = quality == option,
                                    onClick = { quality = option },
                                )
                                Text(strings.quality(option))
                            }
                        }
                        Button(
                            onClick = { onDownload(url!!, current.video.title, quality) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(strings.downloadButton(quality)) }
                    }
                }
                YtdlxWatermark(strings)
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
