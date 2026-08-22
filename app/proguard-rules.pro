# YTDLX R8 rules for the shrink-obfuscate-optimize experiment.
#
# Important: R8/ProGuard operates on Java/Kotlin bytecode. It cannot shrink,
# strip, or obfuscate native .so files such as libpython.so or libffmpeg.so.
# Native size is controlled by the dependency build, ABI selection, and APK/AAB
# packaging. These rules optimize the wrapper/application code without removing
# the native runtime required by yt-dlp and FFmpeg.

# Android/WorkManager component entry points are instantiated by class name.
# Keep the class and constructor, but do not keep every member unnecessarily.
-keep class com.sonzaiekkusu.ytdlx.DownloadWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

-keep class com.sonzaiekkusu.ytdlx.YtdlxApplication {
    <init>();
}

-keep class com.sonzaiekkusu.ytdlx.MainActivity {
    <init>();
}

-keep class com.sonzaiekkusu.ytdlx.ShareActivity {
    <init>();
}

# Direct runtime entry points. Allow R8 to optimize method bodies while keeping
# names and members needed by the library's JNI/reflection bridges.
-keep,allowoptimization class com.yausername.youtubedl_android.YoutubeDL { *; }
-keep,allowoptimization class com.yausername.youtubedl_android.YoutubeDLRequest { *; }
-keep,allowoptimization class com.yausername.youtubedl_android.mapper.** { *; }
-keep,allowoptimization class com.yausername.ffmpeg.FFmpeg { *; }

# Keep callback interfaces and members used by reflection in the runtime.
-keep,allowoptimization interface com.yausername.youtubedl_android.** { *; }

# Remove Android log calls from release bytecode where R8 can prove that their
# return values are unused. Do not apply this to stdout/stderr because yt-dlp
# progress/error output is still useful to the app.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Optional annotations are not required at runtime by this application.
-dontwarn javax.annotation.**
