package com.sonzaiekkusu.ytdlx

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    private val openDownloadManagerRequest = mutableIntStateOf(0)
    private var keepSplashScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        handleIntent(intent)
        setContent { YtdlxApp(viewModel, openDownloadManagerRequest.intValue) }
        viewModel.updateYtdlpIfFirstLaunch(this) { keepSplashScreen = false }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_DOWNLOAD_MANAGER, false) == true) {
            openDownloadManagerRequest.intValue++
        }
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
        const val ACTION_OPEN_DOWNLOAD_MANAGER = "com.sonzaiekkusu.ytdlx.OPEN_DOWNLOAD_MANAGER"
        const val EXTRA_OPEN_DOWNLOAD_MANAGER = "open_download_manager"
        private const val NOTIFICATION_PERMISSION_REQUEST = 100
    }
}

@Composable
private fun YtdlxApp(viewModel: MainViewModel, openDownloadManagerRequest: Int) {
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val strings = language.strings()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var downloadManagerOpen by rememberSaveable { mutableStateOf(false) }
    var exitDialogOpen by rememberSaveable { mutableStateOf(false) }
    val activity = androidx.compose.ui.platform.LocalContext.current as? ComponentActivity

    LaunchedEffect(openDownloadManagerRequest) {
        if (openDownloadManagerRequest > 0) downloadManagerOpen = true
    }

    BackHandler {
        when {
            settingsOpen -> settingsOpen = false
            downloadManagerOpen -> downloadManagerOpen = false
            else -> exitDialogOpen = true
        }
    }

    YtdlxTheme(themeMode) {
        when {
            settingsOpen -> SettingsScreen(viewModel, strings, onBack = { settingsOpen = false })
            downloadManagerOpen -> DownloadManagerScreen(viewModel, strings, onBack = { downloadManagerOpen = false })
            else -> HomeScreen(
                viewModel = viewModel,
                strings = strings,
                onOpenSettings = { settingsOpen = true },
                onOpenDownloadManager = { downloadManagerOpen = true },
            )
        }
        if (exitDialogOpen) {
            AlertDialog(
                onDismissRequest = { exitDialogOpen = false },
                title = { Text(strings.exitTitle) },
                text = { Text(strings.exitMessage) },
                confirmButton = {
                    TextButton(onClick = { exitDialogOpen = false; activity?.finish() }) { Text(strings.exit) }
                },
                dismissButton = {
                    TextButton(onClick = { exitDialogOpen = false }) { Text(strings.cancel) }
                },
            )
        }
    }
}

@Composable
fun YtdlxTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
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
    strings: AppStrings,
    onOpenSettings: () -> Unit,
    onOpenDownloadManager: () -> Unit,
) {
    val url by viewModel.url.collectAsState()
    val metadata by viewModel.metadata.collectAsState()
    val quality by viewModel.quality.collectAsState()
    val queuedCount by viewModel.queuedCount.collectAsState()
    val urlCount = remember(url) { url.asYouTubeUrls().size }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(queuedCount) {
        if (queuedCount > 0) {
            snackbar.showSnackbar(strings.queuedMessage(queuedCount))
            viewModel.clearQueued()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YTDLX", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenDownloadManager) {
                        Icon(Icons.Default.Download, contentDescription = strings.downloadManager)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = strings.settings)
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
            Text(strings.homeDescription)

            OutlinedTextField(
                value = url,
                onValueChange = viewModel::setUrl,
                label = { Text(strings.youtubeUrl) },
                placeholder = { Text(strings.youtubeUrlPlaceholder) },
                supportingText = { Text(strings.batchInputHint) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 6,
            )
            Button(
                onClick = { viewModel.loadMetadata() },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank(),
            ) { Text(strings.fetchMetadata) }

            when (val state = metadata) {
                MetadataState.Empty -> Text(strings.shareHint)
                MetadataState.Loading -> TerminalLoadingPanel(strings.metadataLoading, url)
                is MetadataState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                is MetadataState.Ready -> {
                    VideoCard(state.video, strings)
                    Text(strings.chooseQuality, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    QualityOption.entries.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = quality == option,
                                onClick = { viewModel.setQuality(option) },
                            )
                            Text(strings.quality(option))
                        }
                    }
                    Button(
                        onClick = { viewModel.enqueueDownload() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(strings.downloadButton(quality, urlCount)) }
                }
            }
            YtdlxWatermark(strings)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadManagerScreen(viewModel: MainViewModel, strings: AppStrings, onBack: () -> Unit) {
    val downloads by viewModel.downloads.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.downloadManager) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
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
            DownloadManagerSection(downloads, viewModel, strings)
            YtdlxWatermark(strings)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(viewModel: MainViewModel, strings: AppStrings, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val updateState by viewModel.ytdlpUpdate.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshYtdlpVersion(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settings) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
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
            Text(strings.appearance, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(strings.appearanceDescription)
            ThemeMode.entries.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = themeMode == option,
                        onClick = { viewModel.setThemeMode(option) },
                    )
                    Text(strings.theme(option))
                }
            }

            Text(strings.language, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(strings.languageDescription)
            AppLanguage.entries.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = viewModel.language.value == option,
                        onClick = { viewModel.setLanguage(option) },
                    )
                    Text(option.label)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(strings.ytDlp, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(strings.ytDlpDescription)

            when (val state = updateState) {
                YtdlpUpdateState.Idle -> Text(strings.versionNotChecked)
                YtdlpUpdateState.Updating -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(strings.checkingUpdate)
                }
                is YtdlpUpdateState.Success -> Text("${strings.updateMessage(state.result)}: ${state.version ?: "-"}")
                is YtdlpUpdateState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.updateYtdlp(context) },
                enabled = updateState !is YtdlpUpdateState.Updating,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(strings.updateNow) }

            Text(strings.updateNote, style = MaterialTheme.typography.bodySmall)
            YtdlxWatermark(strings)
        }
    }
}

@Composable
private fun DownloadManagerSection(downloads: List<DownloadItemUi>, viewModel: MainViewModel, strings: AppStrings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(strings.downloadManagerHeading, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (downloads.any { it.state.isFinished() }) {
            OutlinedButton(onClick = viewModel::clearFinished) { Text(strings.clear) }
        }
    }

    if (downloads.isEmpty()) {
        Text(strings.noDownloads)
    } else {
        downloads.take(20).forEach { item ->
            DownloadItemCard(item, viewModel, strings)
        }
    }
}

@Composable
private fun DownloadItemCard(item: DownloadItemUi, viewModel: MainViewModel, strings: AppStrings) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${strings.title}: ${item.title}", fontWeight = FontWeight.Bold)
            Text("${strings.downloadQuality}: ${strings.quality(item.quality)}")
            Text(downloadStateLabel(item.state, strings))

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
                        OutlinedButton(onClick = { viewModel.cancelDownload(item.id) }) { Text(strings.cancelDownload) }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED ->
                        OutlinedButton(onClick = { viewModel.retryDownload(item.id) }) { Text(strings.retry) }
                    WorkInfo.State.SUCCEEDED -> Text(strings.savedToDownloads)
                    else -> Unit
                }
            }
        }
    }
}

private fun WorkInfo.State.isFinished() = this == WorkInfo.State.SUCCEEDED ||
    this == WorkInfo.State.FAILED || this == WorkInfo.State.CANCELLED

private fun downloadStateLabel(state: WorkInfo.State, strings: AppStrings): String = when (state) {
    WorkInfo.State.ENQUEUED -> strings.statusQueued
    WorkInfo.State.RUNNING -> strings.statusRunning
    WorkInfo.State.SUCCEEDED -> strings.statusCompleted
    WorkInfo.State.FAILED -> strings.statusFailed
    WorkInfo.State.BLOCKED -> strings.statusBlocked
    WorkInfo.State.CANCELLED -> strings.statusCancelled
}

@Composable
private fun VideoCard(video: VideoMetadata, strings: AppStrings) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("${strings.title}: ${video.title}", fontWeight = FontWeight.Bold)
            Text("${strings.channel}: ${video.channel ?: video.uploader ?: strings.unknownChannel}")
            Text("${strings.duration}: ${video.duration?.let(::formatDuration) ?: "-"}")
            video.view_count?.let { Text("${strings.views}: ${String.format(Locale.getDefault(), "%,d", it)}") }
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
