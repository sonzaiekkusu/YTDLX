package com.sonzaiekkusu.ytdlx

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface MetadataState {
    data object Empty : MetadataState
    data object Loading : MetadataState
    data class Ready(val video: VideoMetadata) : MetadataState
    data class Error(val message: String) : MetadataState
}

sealed interface YtdlpUpdateState {
    data object Idle : YtdlpUpdateState
    data object Updating : YtdlpUpdateState
    data class Success(val message: String, val version: String?) : YtdlpUpdateState
    data class Error(val message: String) : YtdlpUpdateState
}

data class DownloadItemUi(
    val id: UUID,
    val title: String,
    val quality: QualityOption,
    val state: WorkInfo.State,
    val progress: Int,
    val estimatedSizeBytes: Long? = null,
    val error: String? = null,
    val outputUri: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = YtdlEngine()
    private val workManager = WorkManager.getInstance(application)
    private val settings = application.getSharedPreferences("ytdlx_settings", Context.MODE_PRIVATE)

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _metadata = MutableStateFlow<MetadataState>(MetadataState.Empty)
    val metadata: StateFlow<MetadataState> = _metadata.asStateFlow()

    private val _quality = MutableStateFlow(QualityOption.BEST)
    val quality: StateFlow<QualityOption> = _quality.asStateFlow()

    private val _queued = MutableStateFlow(false)
    val queued: StateFlow<Boolean> = _queued.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadItemUi>>(emptyList())
    val downloads: StateFlow<List<DownloadItemUi>> = _downloads.asStateFlow()

    private val _themeMode = MutableStateFlow(
        settings.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _ytdlpUpdate = MutableStateFlow<YtdlpUpdateState>(YtdlpUpdateState.Idle)
    val ytdlpUpdate: StateFlow<YtdlpUpdateState> = _ytdlpUpdate.asStateFlow()

    init {
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow(DownloadWorker.DOWNLOAD_TAG).collectLatest { infos ->
                _downloads.value = infos.map { it.toDownloadItem() }
            }
        }
    }

    fun acceptSharedText(value: String?) {
        val youtubeUrl = value?.asYouTubeUrl() ?: return
        _url.value = youtubeUrl
        loadMetadata(youtubeUrl)
    }

    fun setQuality(option: QualityOption) {
        _quality.value = option
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        settings.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun refreshYtdlpVersion(context: Context) {
        viewModelScope.launch {
            val version = runCatching { engine.version(context) }.getOrNull()
            if (_ytdlpUpdate.value !is YtdlpUpdateState.Updating) {
                _ytdlpUpdate.value = YtdlpUpdateState.Success("Versi terpasang", version)
            }
        }
    }

    fun updateYtdlp(context: Context) {
        if (_ytdlpUpdate.value is YtdlpUpdateState.Updating) return
        _ytdlpUpdate.value = YtdlpUpdateState.Updating
        viewModelScope.launch {
            _ytdlpUpdate.value = runCatching {
                val message = engine.updateStable(context)
                val version = engine.version(context)
                YtdlpUpdateState.Success(message, version)
            }.getOrElse { YtdlpUpdateState.Error(it.message ?: "Update yt-dlp gagal") }
        }
    }

    fun setUrl(value: String) {
        _url.value = value
        if (_metadata.value !is MetadataState.Empty) {
            _metadata.value = MetadataState.Empty
        }
    }

    fun loadMetadata(value: String = _url.value) {
        val youtubeUrl = value.asYouTubeUrl()
        if (youtubeUrl == null) {
            _metadata.value = MetadataState.Error("Masukkan URL YouTube yang valid")
            return
        }
        _url.value = youtubeUrl
        _metadata.value = MetadataState.Loading
        viewModelScope.launch {
            _metadata.value = runCatching { engine.metadata(getApplication<Application>(), youtubeUrl) }
                .fold(
                    onSuccess = { MetadataState.Ready(it) },
                    onFailure = { MetadataState.Error(it.message ?: "Gagal mengambil metadata") },
                )
        }
    }

    fun enqueueDownload(context: Context) {
        val youtubeUrl = _url.value.asYouTubeUrl() ?: return
        val video = (_metadata.value as? MetadataState.Ready)?.video
        val request = createDownloadWork(
            url = youtubeUrl,
            title = video?.title ?: "YouTube video",
            quality = _quality.value,
            estimatedSizeBytes = video?.estimateSize(_quality.value),
        )
        workManager.enqueue(request)
        _queued.value = true
    }

    fun cancelDownload(id: UUID) {
        workManager.cancelWorkById(id)
    }

    fun retryDownload(id: UUID) {
        viewModelScope.launch(Dispatchers.IO) {
            val old = runCatching { workManager.getWorkInfoById(id).get() }.getOrNull() ?: return@launch
            val url = old.progress.getString(DownloadWorker.KEY_URL) ?: return@launch
            val title = old.progress.getString(DownloadWorker.KEY_TITLE) ?: "YouTube video"
            val quality = (old.progress.getString(DownloadWorker.KEY_QUALITY)
                ?: old.outputData.getString(DownloadWorker.KEY_QUALITY))
                ?.let { runCatching { QualityOption.valueOf(it) }.getOrNull() }
                ?: QualityOption.BEST
            val estimatedSize = old.progress.getLong(
                DownloadWorker.KEY_ESTIMATED_SIZE,
                old.outputData.getLong(DownloadWorker.KEY_ESTIMATED_SIZE, 0L),
            ).takeIf { it > 0L }
            workManager.enqueue(createDownloadWork(url, title, quality, estimatedSize))
        }
    }

    fun clearFinished() {
        workManager.pruneWork()
    }

    fun clearQueued() {
        _queued.value = false
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
    }

    private fun WorkInfo.toDownloadItem(): DownloadItemUi {
        val quality = (progress.getString(DownloadWorker.KEY_QUALITY)
            ?: outputData.getString(DownloadWorker.KEY_QUALITY))
            ?.let { runCatching { QualityOption.valueOf(it) }.getOrNull() }
            ?: QualityOption.BEST
        return DownloadItemUi(
            id = id,
            title = progress.getString(DownloadWorker.KEY_TITLE)
                ?: outputData.getString(DownloadWorker.KEY_TITLE)
                ?: "YouTube video",
            quality = quality,
            state = state,
            progress = progress.getInt(DownloadWorker.KEY_PROGRESS, 0),
            estimatedSizeBytes = progress.getLong(
                DownloadWorker.KEY_ESTIMATED_SIZE,
                outputData.getLong(DownloadWorker.KEY_ESTIMATED_SIZE, 0L),
            ).takeIf { it > 0L },
            error = outputData.getString(DownloadWorker.KEY_ERROR),
            outputUri = outputData.getString(DownloadWorker.KEY_OUTPUT_URI),
        )
    }
}
