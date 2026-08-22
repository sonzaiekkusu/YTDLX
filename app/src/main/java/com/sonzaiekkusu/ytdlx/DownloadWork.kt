package com.sonzaiekkusu.ytdlx

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import java.util.concurrent.TimeUnit

fun createDownloadWork(
    url: String,
    title: String,
    quality: QualityOption,
): OneTimeWorkRequest {
    val inputData = Data.Builder()
        .putString(DownloadWorker.KEY_URL, url)
        .putString(DownloadWorker.KEY_TITLE, title)
        .putString(DownloadWorker.KEY_QUALITY, quality.name)
        .build()

    return OneTimeWorkRequestBuilder<DownloadWorker>()
        .setInputData(inputData)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
        .addTag(DownloadWorker.DOWNLOAD_TAG)
        .build()
}
