package com.sonzaiekkusu.ytdlx

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed interface MetadataState {
    data object Empty : MetadataState
    data object Loading : MetadataState
    data class Ready(val video: VideoMetadata) : MetadataState
    data class Error(val message: String) : MetadataState
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
        val request = newDownloadRequest(
            url = youtubeUrl,
            title = video?.title ?: "YouTube video",
            quality = _quality.value,
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
            val url = old.inputData.getString(DownloadWorker.KEY_URL) ?: return@launch
            val title = old.inputData.getString(DownloadWorker.KEY_TITLE) ?: "YouTube video"
            val quality = old.inputData.getString(DownloadWorker.KEY_QUALITY)
                ?.let { runCatching { QualityOption.valueOf(it) }.getOrNull() }
                ?: QualityOption.BEST
            workManager.enqueue(newDownloadRequest(url, title, quality))
        }
    }

    fun clearFinished() {
        workManager.pruneWork()
    }

    fun clearQueued() {
        _queued.value = false
    }

    private fun newDownloadRequest(url: String, title: String, quality: QualityOption) =
        OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_URL to url,
                    DownloadWorker.KEY_TITLE to title,
                    DownloadWorker.KEY_QUALITY to quality.name,
                ),
            )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(DownloadWorker.DOWNLOAD_TAG)
            .build()

    private fun WorkInfo.toDownloadItem(): DownloadItemUi {
        val quality = inputData.getString(DownloadWorker.KEY_QUALITY)
            ?.let { runCatching { QualityOption.valueOf(it) }.getOrNull() }
            ?: QualityOption.BEST
        return DownloadItemUi(
            id = id,
            title = inputData.getString(DownloadWorker.KEY_TITLE) ?: "YouTube video",
            quality = quality,
            state = state,
            progress = progress.getInt(DownloadWorker.KEY_PROGRESS, 0),
            error = outputData.getString(DownloadWorker.KEY_ERROR),
            outputUri = outputData.getString(DownloadWorker.KEY_OUTPUT_URI),
        )
    }
}
