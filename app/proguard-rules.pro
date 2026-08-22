# YTDLX release shrinking rules.
# Keep only classes that are loaded by Android/WorkManager or accessed by the
# native yt-dlp/FFmpeg runtime. The bundled native libraries remain packaged.

# WorkManager restores workers by their class name after process death.
-keep class com.sonzaiekkusu.ytdlx.DownloadWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
    *;
}

# Application class is declared in AndroidManifest.xml and initializes the
# bundled youtubedl-android runtime.
-keep class com.sonzaiekkusu.ytdlx.YtdlxApplication {
    <init>();
    *;
}

# Keep the public runtime entry points used by the native engine and its
# reflection/JNI bridges. Consumer rules from the dependency remain active.
-keep class com.yausername.youtubedl_android.YoutubeDL { *; }
-keep class com.yausername.youtubedl_android.YoutubeDLRequest { *; }
-keep class com.yausername.youtubedl_android.mapper.** { *; }
-keep class com.yausername.ffmpeg.FFmpeg { *; }

# Do not fail shrinking because of optional annotations used by dependencies.
-dontwarn javax.annotation.**
