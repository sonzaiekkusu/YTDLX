package com.sonzaiekkusu.ytdlx

import android.net.Uri
data class FormatSize(
    val height: Int = 0,
    val hasVideo: Boolean,
    val hasAudio: Boolean,
    val sizeBytes: Long? = null,
    val bitrateKbps: Int? = null,
)

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
    val formats: List<FormatSize> = emptyList(),
)

fun VideoMetadata.estimateSize(quality: QualityOption): Long? {
    if (formats.isEmpty()) return null
    val videos = formats.filter { it.hasVideo && it.height > 0 }
    val audios = formats.filter { it.hasAudio && !it.hasVideo }
    if (quality == QualityOption.AUDIO) {
        return audios.maxByOrNull { it.bitrateKbps ?: 0 }?.estimatedBytes(duration)
    }
    val video = when {
        quality == QualityOption.BEST -> videos.maxByOrNull { it.height }
        else -> {
            val limit = requireNotNull(quality.maxHeight)
            videos.filter { it.height <= limit }.maxByOrNull { it.height }
                ?: videos.minByOrNull { kotlin.math.abs(it.height - limit) }
        }
    } ?: return null
    val videoSize = video.estimatedBytes(duration)
    if (video.hasAudio) return videoSize
    val audioSize = audios.maxByOrNull { it.bitrateKbps ?: 0 }?.estimatedBytes(duration)
    return when {
        videoSize != null && audioSize != null -> videoSize + audioSize
        else -> videoSize ?: audioSize
    }
}

private fun FormatSize.estimatedBytes(durationSeconds: Int?): Long? {
    sizeBytes?.takeIf { it > 0 }?.let { return it }
    val bitrate = bitrateKbps?.takeIf { it > 0 } ?: return null
    val duration = durationSeconds?.takeIf { it > 0 } ?: return null
    return bitrate.toLong() * 1000L / 8L * duration
}

fun Long?.formatFileSize(): String = when {
    this == null || this <= 0L -> "Ukuran belum diketahui"
    this < 1024L * 1024L -> String.format(java.util.Locale.getDefault(), "%.1f KB", this / 1024.0)
    this < 1024L * 1024L * 1024L -> String.format(java.util.Locale.getDefault(), "%.1f MB", this / (1024.0 * 1024.0))
    else -> String.format(java.util.Locale.getDefault(), "%.1f GB", this / (1024.0 * 1024.0 * 1024.0))
}

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
