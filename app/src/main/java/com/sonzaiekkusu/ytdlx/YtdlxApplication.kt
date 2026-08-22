package com.sonzaiekkusu.ytdlx

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YtdlxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                YoutubeDL.getInstance().init(this@YtdlxApplication)
                FFmpeg.getInstance().init(this@YtdlxApplication)
            }.onFailure {
                Log.e(TAG, "Failed to initialize youtubedl-android", it)
            }
        }
    }

    companion object {
        private const val TAG = "YTDLX"
    }
}
