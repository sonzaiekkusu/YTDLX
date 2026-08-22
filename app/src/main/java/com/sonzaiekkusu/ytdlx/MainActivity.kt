package com.sonzaiekkusu.ytdlx

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.work.WorkInfo
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        handleIntent(intent)
        setContent { YtdlxApp(viewModel) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: intent?.dataString
        viewModel.acceptSharedText(sharedText)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 100
    }
}

@Composable
private fun YtdlxApp(viewModel: MainViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var downloadManagerOpen by rememberSaveable { mutableStateOf(false) }

    YtdlxTheme(themeMode) {
        when {
            settingsOpen -> SettingsScreen(viewModel, onBack = { settingsOpen = false })
            downloadManagerOpen -> DownloadManagerScreen(viewModel, onBack = { downloadManagerOpen = false })
            else -> HomeScreen(
                viewModel = viewModel,
                onOpenSettings = { settingsOpen = true },
                onOpenDownloadManager = { downloadManagerOpen = true },
            )
        }
    }
}

@Composable
private fun YtdlxTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? android.app.Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenDownloadManager: () -> Unit,
) {
    val url by viewModel.url.collectAsState()
    val metadata by viewModel.metadata.collectAsState()
    val quality by viewModel.quality.collectAsState()
    val queued by viewModel.queued.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(queued) {
        if (queued) {
            snackbar.showSnackbar("Download masuk antrean. Buka Download Manager untuk melihat status.")
            viewModel.clearQueued()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YTDLX", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenDownloadManager) {
                        Icon(Icons.Default.Download, contentDescription = "Download Manager")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Download YouTube dengan kualitas yang mudah dipilih")

            OutlinedTextField(
                value = url,
                onValueChange = viewModel::setUrl,
                label = { Text("URL YouTube") },
                placeholder = { Text("https://youtu.be/...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = { viewModel.loadMetadata() },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank(),
            ) { Text("Ambil metadata") }

            when (val state = metadata) {
                MetadataState.Empty -> Text("Bagikan video dari YouTube ke YTDLX atau masukkan URL di atas.")
                MetadataState.Loading -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Mengambil metadata…")
                }
                is MetadataState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                is MetadataState.Ready -> {
                    VideoCard(state.video)
                    Text("Pilih kualitas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    QualityOption.entries.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = quality == option,
                                onClick = { viewModel.setQuality(option) },
                            )
                            Text(option.label)
                        }
                    }
                    Button(
                        onClick = { viewModel.enqueueDownload(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Download ${quality.label}") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadManagerScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val downloads by viewModel.downloads.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DownloadManagerSection(downloads, viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val updateState by viewModel.ytdlpUpdate.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshYtdlpVersion(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Tampilan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Pilih tampilan aplikasi yang paling nyaman.")
            ThemeMode.entries.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = themeMode == option,
                        onClick = { viewModel.setThemeMode(option) },
                    )
                    Text(option.label)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("yt-dlp", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Runtime yt-dlp di dalam aplikasi dapat diperbarui dari channel stable resmi.")

            when (val state = updateState) {
                YtdlpUpdateState.Idle -> Text("Versi: belum diperiksa")
                YtdlpUpdateState.Updating -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Memeriksa dan mengunduh update…")
                }
                is YtdlpUpdateState.Success -> Text("${state.message}: ${state.version ?: "tidak diketahui"}")
                is YtdlpUpdateState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.updateYtdlp(context) },
                enabled = updateState !is YtdlpUpdateState.Updating,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Update yt-dlp sekarang") }

            Text(
                "Catatan: update membutuhkan koneksi internet. Jika YouTube berubah dan muncul HTTP 403, coba update yt-dlp terlebih dahulu.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DownloadManagerSection(downloads: List<DownloadItemUi>, viewModel: MainViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Download Manager", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (downloads.any { it.state.isFinished() }) {
            OutlinedButton(onClick = viewModel::clearFinished) { Text("Bersihkan") }
        }
    }

    if (downloads.isEmpty()) {
        Text("Belum ada download. Download yang sedang berjalan akan tampil di sini.")
    } else {
        downloads.take(20).forEach { item ->
            DownloadItemCard(item, viewModel)
        }
    }
}

@Composable
private fun DownloadItemCard(item: DownloadItemUi, viewModel: MainViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title, fontWeight = FontWeight.Bold)
            Text("Kualitas: ${item.quality.label}")
            Text(downloadStateLabel(item.state))

            if (item.state == WorkInfo.State.RUNNING || item.state == WorkInfo.State.ENQUEUED) {
                LinearProgressIndicator(
                    progress = { (item.progress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("${item.progress}%")
            }

            item.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (item.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING ->
                        OutlinedButton(onClick = { viewModel.cancelDownload(item.id) }) { Text("Batal") }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED ->
                        OutlinedButton(onClick = { viewModel.retryDownload(item.id) }) { Text("Coba lagi") }
                    WorkInfo.State.SUCCEEDED -> Text("Tersimpan di Download/YTDLX")
                    else -> Unit
                }
            }
        }
    }
}

private fun WorkInfo.State.isFinished() = this == WorkInfo.State.SUCCEEDED ||
    this == WorkInfo.State.FAILED || this == WorkInfo.State.CANCELLED

private fun downloadStateLabel(state: WorkInfo.State): String = when (state) {
    WorkInfo.State.ENQUEUED -> "Menunggu koneksi / antrean"
    WorkInfo.State.RUNNING -> "Sedang mengunduh"
    WorkInfo.State.SUCCEEDED -> "Selesai"
    WorkInfo.State.FAILED -> "Gagal"
    WorkInfo.State.BLOCKED -> "Menunggu dependency"
    WorkInfo.State.CANCELLED -> "Dibatalkan"
}

@Composable
private fun VideoCard(video: VideoMetadata) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(video.title, fontWeight = FontWeight.Bold)
            Text(video.channel ?: video.uploader ?: "Channel tidak diketahui")
            Text("Durasi: ${video.duration?.let(::formatDuration) ?: "-"}")
            video.view_count?.let { Text("Views: ${String.format(Locale.getDefault(), "%,d", it)}") }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remaining = seconds % 60
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:${remaining.toString().padStart(2, '0')}"
    else "$minutes:${remaining.toString().padStart(2, '0')}"
}
