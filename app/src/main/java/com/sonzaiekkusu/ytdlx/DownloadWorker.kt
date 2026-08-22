package com.sonzaiekkusu.ytdlx

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val engine = YtdlEngine()

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val language = applicationContext.getSharedPreferences("ytdlx_settings", Context.MODE_PRIVATE)
            .getString(LANGUAGE_PREFERENCE_KEY, AppLanguage.INDONESIAN.name)
            ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.INDONESIAN
        val strings = language.strings()
        val title = inputData.getString(KEY_TITLE)
            ?: if (language == AppLanguage.ENGLISH) "YouTube video" else "Video YouTube"
        val qualityName = inputData.getString(KEY_QUALITY) ?: QualityOption.BEST.name
        val quality = runCatching { QualityOption.valueOf(qualityName) }.getOrDefault(QualityOption.BEST)
        val progressNotificationId = notificationIdFor(id, 0)
        val resultNotificationId = notificationIdFor(id, RESULT_NOTIFICATION_OFFSET)
        fun progressData(status: String, percent: Int = 0) = workDataOf(
            KEY_URL to url,
            KEY_TITLE to title,
            KEY_QUALITY to quality.name,
            KEY_STATUS to status,
            KEY_PROGRESS to percent.coerceIn(0, 100),
        )

        setForeground(createForegroundInfo(title, quality, strings, strings.downloadPreparing, notificationId = progressNotificationId))
        return try {
            val stagingDirectory = applicationContext.filesDir.resolve("staging/$id")
            stagingDirectory.deleteRecursively()
            stagingDirectory.mkdirs()

            setProgress(progressData(strings.downloadFetching))
            val stagedPath = engine.download(
                applicationContext,
                url,
                quality,
                stagingDirectory,
                id.toString(),
            ) { progress ->
                val percent = progress.toInt().coerceIn(0, 100)
                setProgressAsync(progressData(strings.downloadRunning, percent))
                setForegroundAsync(createForegroundInfo(title, quality, strings, strings.downloadRunning, percent, progressNotificationId))
            }
            val stagedFile = File(stagedPath)
            if (!stagedFile.isFile) error("File hasil download tidak ditemukan")

            setProgress(progressData(strings.downloadSaving, 100))
            val audio = quality == QualityOption.AUDIO
            val mimeType = mediaMimeType(stagedFile, audio)
            val uri = publishToDownloads(stagedFile, audio)
            stagedDirectoryCleanup(stagingDirectory)
            notifyDownloadResult(
                notificationId = resultNotificationId,
                title = title,
                quality = quality,
                strings = strings,
                outputUri = uri,
                mimeType = mimeType,
                success = true,
            )
            Result.success(
                workDataOf(
                    KEY_OUTPUT_URI to uri.toString(),
                    KEY_STATUS to "Selesai",
                    KEY_TITLE to title,
                    KEY_QUALITY to quality.name,
                ),
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            engine.cancel(id.toString())
            throw cancelled
        } catch (error: Exception) {
            notifyDownloadResult(
                notificationId = resultNotificationId,
                title = title,
                quality = quality,
                strings = strings,
                success = false,
                errorMessage = error.message,
            )
            Result.failure(
                workDataOf(
                    KEY_ERROR to (error.message ?: strings.downloadFailed),
                    KEY_TITLE to title,
                    KEY_QUALITY to quality.name,
                ),
            )
        }
    }

    private fun createForegroundInfo(
        title: String,
        quality: QualityOption,
        strings: AppStrings,
        status: String,
        progress: Int? = null,
        notificationId: Int,
    ): ForegroundInfo {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureNotificationChannels(manager)
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val notification = NotificationCompat.Builder(applicationContext, ACTIVE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(status)
            .setSubText("YTDLX · ${strings.quality(quality)}")
            .setContentIntent(openDownloadManagerPendingIntent(notificationId))
            .setOnlyAlertOnce(true)
            .setGroup(GROUP_KEY)
            .setOngoing(true)
            .apply {
                if (progress != null) setProgress(100, progress.coerceIn(0, 100), false)
            }
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, strings.cancel, cancelIntent)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun notifyDownloadResult(
        notificationId: Int,
        title: String,
        quality: QualityOption,
        strings: AppStrings,
        outputUri: Uri? = null,
        mimeType: String? = null,
        success: Boolean,
        errorMessage: String? = null,
    ) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureNotificationChannels(notificationManager)
        val content = if (success) {
            "${strings.downloadFinished} · ${strings.quality(quality)}"
        } else {
            "${strings.downloadFailed} · ${errorMessage ?: strings.retryFromManager}"
        }
        val notification = NotificationCompat.Builder(applicationContext, RESULT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(openDownloadManagerPendingIntent(notificationId))
            .setGroup(GROUP_KEY)
            .setAutoCancel(true)
            .setOngoing(false)
            .apply {
                if (success && outputUri != null && mimeType != null) {
                    addAction(
                        android.R.drawable.ic_menu_view,
                        strings.openFile,
                        openFilePendingIntent(outputUri, mimeType, notificationId),
                    )
                    addAction(
                        android.R.drawable.ic_menu_share,
                        strings.shareFile,
                        shareFilePendingIntent(outputUri, mimeType, notificationId),
                    )
                }
            }
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun openFilePendingIntent(uri: Uri, mimeType: String, requestCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return PendingIntent.getActivity(
            applicationContext,
            requestCode xor OPEN_FILE_REQUEST_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun shareFilePendingIntent(uri: Uri, mimeType: String, requestCode: Int): PendingIntent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(titleForShare(uri), uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(sendIntent, applicationContext.getString(R.string.app_name)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            applicationContext,
            requestCode xor SHARE_FILE_REQUEST_OFFSET,
            chooserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun titleForShare(uri: Uri): String = uri.lastPathSegment ?: "YTDLX"

    private fun openDownloadManagerPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_DOWNLOAD_MANAGER
            putExtra(MainActivity.EXTRA_OPEN_DOWNLOAD_MANAGER, true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureNotificationChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ACTIVE_CHANNEL_ID,
                    applicationContext.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = applicationContext.getString(R.string.notification_channel_description)
                },
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    RESULT_CHANNEL_ID,
                    applicationContext.getString(R.string.notification_result_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = applicationContext.getString(R.string.notification_result_channel_description)
                },
            )
        }
    }

    private fun mediaMimeType(source: File, audio: Boolean): String {
        val extension = source.extension.lowercase()
        return when {
            audio || extension == "mp3" -> "audio/mpeg"
            extension == "mkv" -> "video/x-matroska"
            else -> "video/mp4"
        }
    }

    private fun publishToDownloads(source: File, audio: Boolean): Uri {
        val mime = mediaMimeType(source, audio)
        val resolver = applicationContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, source.name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/YTDLX")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Tidak dapat membuat file di folder Download")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Tidak dapat membuka output file")
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null,
            )
            return uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun stagedDirectoryCleanup(directory: File) {
        directory.deleteRecursively()
    }

    companion object {
        const val DOWNLOAD_TAG = "ytdlx_download"
        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_QUALITY = "quality"
        const val KEY_STATUS = "status"
        const val KEY_PROGRESS = "progress"
        const val KEY_OUTPUT_URI = "output_uri"
        const val KEY_ERROR = "error"
        private const val ACTIVE_CHANNEL_ID = "ytdlx_downloads"
        private const val RESULT_CHANNEL_ID = "ytdlx_download_results"
        private const val GROUP_KEY = "com.sonzaiekkusu.ytdlx.DOWNLOADS"
        private const val RESULT_NOTIFICATION_OFFSET = 0x40000000
        private const val OPEN_FILE_REQUEST_OFFSET = 0x10000000
        private const val SHARE_FILE_REQUEST_OFFSET = 0x20000000

        private fun notificationIdFor(workId: java.util.UUID, offset: Int): Int =
            ((workId.hashCode() and Int.MAX_VALUE) xor offset).coerceAtLeast(1)
    }
}
