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
    data class Success(val result: YtdlpUpdateResult, val version: String?) : YtdlpUpdateState
    data class Error(val message: String) : YtdlpUpdateState
}

data class DownloadItemUi(
    val id: UUID,
    val title: String,
    val quality: QualityOption,
    val state: WorkInfo.State,
    val progress: Int,
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

    private val _queuedCount = MutableStateFlow(0)
    val queuedCount: StateFlow<Int> = _queuedCount.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadItemUi>>(emptyList())
    val downloads: StateFlow<List<DownloadItemUi>> = _downloads.asStateFlow()

    private val _language = MutableStateFlow(
        settings.getString(LANGUAGE_PREFERENCE_KEY, AppLanguage.INDONESIAN.name)
            ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.INDONESIAN,
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

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

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        settings.edit().putString(LANGUAGE_PREFERENCE_KEY, language.name).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        settings.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun refreshYtdlpVersion(context: Context) {
        viewModelScope.launch {
            val version = runCatching { engine.version(context) }.getOrNull()
            if (_ytdlpUpdate.value !is YtdlpUpdateState.Updating) {
                _ytdlpUpdate.value = YtdlpUpdateState.Success(YtdlpUpdateResult.INSTALLED, version)
            }
        }
    }

    fun updateYtdlpIfFirstLaunch(context: Context, onComplete: () -> Unit = {}) {
        if (settings.getBoolean(KEY_FIRST_LAUNCH_UPDATE_DONE, false)) {
            onComplete()
            return
        }
        settings.edit().putBoolean(KEY_FIRST_LAUNCH_UPDATE_DONE, true).apply()
        updateYtdlp(context, onComplete)
    }

    fun updateYtdlp(context: Context, onComplete: () -> Unit = {}) {
        if (_ytdlpUpdate.value is YtdlpUpdateState.Updating) return
        _ytdlpUpdate.value = YtdlpUpdateState.Updating
        viewModelScope.launch {
            _ytdlpUpdate.value = runCatching {
                val result = engine.updateStable(context)
                val version = engine.version(context)
                YtdlpUpdateState.Success(result, version)
            }.getOrElse { YtdlpUpdateState.Error(it.message ?: _language.value.strings().updateFailed) }
            onComplete()

        }
    }

    fun setUrl(value: String) {
        _url.value = value
        if (_metadata.value !is MetadataState.Empty) {
            _metadata.value = MetadataState.Empty
        }
    }

    fun loadMetadata(value: String = _url.value) {
        val urls = value.asYouTubeUrls()
        val youtubeUrl = urls.firstOrNull()
        if (youtubeUrl == null) {
            _metadata.value = MetadataState.Error(_language.value.strings().noValidUrls)
            return
        }
        if (urls.size == 1) {
            _url.value = youtubeUrl
        }
        _metadata.value = MetadataState.Loading
        viewModelScope.launch {
            _metadata.value = runCatching { engine.metadata(getApplication<Application>(), youtubeUrl) }
                .fold(
                    onSuccess = { MetadataState.Ready(it) },
                    onFailure = { MetadataState.Error(it.message ?: _language.value.strings().metadataFailed) },
                )
        }
    }

    fun enqueueDownload() {
        val urls = _url.value.asYouTubeUrls()
        if (urls.isEmpty()) return
        val strings = _language.value.strings()
        val metadataVideo = (_metadata.value as? MetadataState.Ready)?.video
        urls.forEachIndexed { index, youtubeUrl ->
            val title = if (urls.size == 1) {
                metadataVideo?.title ?: strings.genericVideo(1)
            } else {
                strings.genericVideo(index + 1)
            }
            workManager.enqueue(createDownloadWork(youtubeUrl, title, _quality.value))
        }
        _queuedCount.value = urls.size
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
            workManager.enqueue(createDownloadWork(url, title, quality))
        }
    }

    fun clearFinished() {
        workManager.pruneWork()
    }

    fun clearQueued() {
        _queuedCount.value = 0
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_FIRST_LAUNCH_UPDATE_DONE = "first_launch_update_done"
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
            error = outputData.getString(DownloadWorker.KEY_ERROR),
            outputUri = outputData.getString(DownloadWorker.KEY_OUTPUT_URI),
        )
    }
}
