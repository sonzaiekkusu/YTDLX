package com.sonzaiekkusu.ytdlx

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class YtdlEngine {
    private val initMutex = Mutex()
    @Volatile
    private var initialized = false

    suspend fun ensureInitialized(context: Context) {
        if (initialized) return
        initMutex.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().init(context.applicationContext)
                FFmpeg.getInstance().init(context.applicationContext)
                initialized = true
            }
        }
    }

    suspend fun version(context: Context): String? {
        ensureInitialized(context)
        return withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().version(context.applicationContext)
        }
    }

    suspend fun updateStable(context: Context): String {
        ensureInitialized(context)
        return withContext(Dispatchers.IO) {
            val status = YoutubeDL.getInstance().updateYoutubeDL(
                context.applicationContext,
                YoutubeDL.UpdateChannel.STABLE,
            )
            when (status) {
                YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "yt-dlp sudah versi terbaru"
                YoutubeDL.UpdateStatus.DONE -> "yt-dlp berhasil diperbarui"
                null -> "Status update tidak diketahui"
            }
        }
    }

    suspend fun metadata(context: Context, url: String): VideoMetadata = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        val info = YoutubeDL.getInstance().getInfo(url)
        info.toMetadata()
    }

    suspend fun download(
        context: Context,
        url: String,
        quality: QualityOption,
        outputDirectory: File,
        processId: String,
        onProgress: (Float) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        outputDirectory.mkdirs()
        val before = outputDirectory.listFiles()?.map { it.absolutePath }?.toSet().orEmpty()
        val outputTemplate = File(outputDirectory, "%(title)s [%(id)s].%(ext)s").absolutePath
        val request = YoutubeDLRequest(url)
            .addOption("-o", outputTemplate)
            .addOption("--no-mtime")
            .addOption("--no-playlist")
            .addOption("--newline")
            .addOption("-f", quality.formatSelector())

        if (quality == QualityOption.AUDIO) {
            request
                .addOption("--extract-audio")
                .addOption("--audio-format", "mp3")
                .addOption("--audio-quality", "192K")
        } else {
            request.addOption("--merge-output-format", "mp4/mkv")
        }

        try {
            YoutubeDL.getInstance().execute(request, processId) { progress, _, _ ->
                onProgress(progress)
            }
        } catch (error: Exception) {
            throw error
        }

        val newFiles = outputDirectory.listFiles()
            ?.filter { it.isFile && it.absolutePath !in before }
            .orEmpty()
        newFiles.maxByOrNull { it.lastModified() }?.absolutePath
            ?: error("Download selesai tetapi file hasil tidak ditemukan")
    }

    fun cancel(processId: String) {
        YoutubeDL.getInstance().destroyProcessById(processId)
    }

    private fun VideoInfo.toMetadata() = VideoMetadata(
        id = id,
        title = title ?: fulltitle ?: "Tanpa judul",
        channel = uploader,
        uploader = uploader,
        duration = duration.takeIf { it > 0 },
        view_count = viewCount?.toLongOrNull(),
        upload_date = uploadDate,
        thumbnail = thumbnail,
        webpage_url = webpageUrl,
        description = description,
        formats = (formats.orEmpty() + requestedFormats.orEmpty()).map { format ->
            FormatSize(
                height = format.height,
                hasVideo = !format.vcodec.isNullOrBlank() && format.vcodec != "none",
                hasAudio = !format.acodec.isNullOrBlank() && format.acodec != "none",
                sizeBytes = (format.fileSize.takeIf { it > 0 } ?: format.fileSizeApproximate.takeIf { it > 0 }),
                bitrateKbps = (format.tbr.takeIf { it > 0 } ?: format.abr.takeIf { it > 0 }),
            )
        },
    )
}
