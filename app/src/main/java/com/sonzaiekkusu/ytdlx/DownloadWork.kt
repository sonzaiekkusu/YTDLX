package com.sonzaiekkusu.ytdlx

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

fun createDownloadWork(url: String, title: String, quality: QualityOption): OneTimeWorkRequest =
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
