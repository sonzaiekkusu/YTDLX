package com.sonzaiekkusu.ytdlx

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MetadataState {
    data object Empty : MetadataState
    data object Loading : MetadataState
    data class Ready(val video: VideoMetadata) : MetadataState
    data class Error(val message: String) : MetadataState
}

class MainViewModel : ViewModel() {
    private val bridge = YtdlpBridge()
    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _metadata = MutableStateFlow<MetadataState>(MetadataState.Empty)
    val metadata: StateFlow<MetadataState> = _metadata.asStateFlow()

    private val _quality = MutableStateFlow(QualityOption.BEST)
    val quality: StateFlow<QualityOption> = _quality.asStateFlow()

    private val _queued = MutableStateFlow(false)
    val queued: StateFlow<Boolean> = _queued.asStateFlow()

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
            _metadata.value = runCatching { bridge.metadata(youtubeUrl) }
                .fold(
                    onSuccess = { MetadataState.Ready(it) },
                    onFailure = { MetadataState.Error(it.message ?: "Gagal mengambil metadata") },
                )
        }
    }

    fun enqueueDownload(context: Context) {
        val youtubeUrl = _url.value.asYouTubeUrl() ?: return
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_URL to youtubeUrl,
                    DownloadWorker.KEY_QUALITY to _quality.value.name,
                ),
            )
            .build()
        WorkManager.getInstance(context).enqueue(request)
        _queued.value = true
    }

    fun clearQueued() {
        _queued.value = false
    }
}
