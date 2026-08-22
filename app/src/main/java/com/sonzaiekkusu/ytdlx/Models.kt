package com.sonzaiekkusu.ytdlx

import android.net.Uri

data class VideoMetadata(
    val id: String? = null,
    val title: String = "Tanpa judul",
    val channel: String? = null,
    val uploader: String? = null,
    val duration: Int? = null,
    val view_count: Long? = null,
    val upload_date: String? = null,
    val thumbnail: String? = null,
    val webpage_url: String? = null,
    val description: String? = null,
)

enum class QualityOption(val label: String, val maxHeight: Int?) {
    BEST("Terbaik yang tersedia", null),
    P360("360p", 360),
    P480("480p", 480),
    P720("720p", 720),
    P1080("1080p", 1080),
    AUDIO("Audio saja · MP3 192 kbps", null),
}

fun QualityOption.formatSelector(): String = when (this) {
    QualityOption.BEST -> "bv*[vcodec^=avc1][ext=mp4]+ba[ext=m4a]/bv*+ba/b"
    QualityOption.AUDIO -> "bestaudio/best"
    else -> {
        val limit = requireNotNull(maxHeight)
        // Prefer H.264/MP4 for Android compatibility. Fall back to VP9/AV1
        // or the closest available quality when the preferred stream is absent.
        "bv*[height<=$limit][vcodec^=avc1][ext=mp4]+ba[ext=m4a]/" +
            "bv*[height<=$limit]+ba/b[height<=$limit]/bv*+ba/b"
    }
}

fun String.asYouTubeUrls(): List<String> = split(Regex("[\\r\\n\\s]+"))
    .mapNotNull { it.asYouTubeUrl() }
    .distinct()

fun String.asYouTubeUrl(): String? {
    val candidate = Regex("https?://(?:www\\.)?(?:youtube\\.com|youtu\\.be)/[^\\s]+", RegexOption.IGNORE_CASE)
        .find(this)
        ?.value
        ?.trimEnd('.', ',', ')', ']', '}', '"', '\'')
        ?: trim()
    if (candidate.isBlank()) return null
    val uri = runCatching { Uri.parse(candidate) }.getOrNull() ?: return null
    val host = uri.host?.lowercase() ?: return null
    val isYouTube = host == "youtube.com" || host.endsWith(".youtube.com") ||
        host == "youtu.be" || host.endsWith(".youtu.be")
    return candidate.takeIf { isYouTube && (uri.scheme == "http" || uri.scheme == "https") }
}

enum class ThemeMode(val label: String) {
    SYSTEM("Ikuti sistem"),
    LIGHT("Tema terang"),
    DARK("Tema gelap"),
}
