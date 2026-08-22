package com.sonzaiekkusu.ytdlx

enum class AppLanguage(val label: String, val localeTag: String) {
    INDONESIAN("Bahasa Indonesia", "id"),
    ENGLISH("English", "en"),
}

data class AppStrings(
    val appName: String,
    val homeDescription: String,
    val youtubeUrl: String,
    val youtubeUrlPlaceholder: String,
    val batchInputHint: String,
    val fetchMetadata: String,
    val shareHint: String,
    val chooseQuality: String,
    val title: String,
    val channel: String,
    val unknownChannel: String,
    val duration: String,
    val views: String,
    val close: String,
    val openFile: String,
    val shareFile: String,
    val invalidSharedUrl: String,
    val noValidUrls: String,
    val metadataFailed: String,
    val updateFailed: String,
    val inaccessibleYoutubeUrl: String,
    val downloadManager: String,
    val settings: String,
    val back: String,
    val queuedMessage: String,
    val exitTitle: String,
    val exitMessage: String,
    val exit: String,
    val cancel: String,
    val appearance: String,
    val appearanceDescription: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val language: String,
    val languageDescription: String,
    val ytDlp: String,
    val ytDlpDescription: String,
    val versionNotChecked: String,
    val checkingUpdate: String,
    val installedVersion: String,
    val updateNow: String,
    val updateNote: String,
    val updateAlreadyLatest: String,
    val updateCompleted: String,
    val updateUnknown: String,
    val downloadManagerHeading: String,
    val clear: String,
    val noDownloads: String,
    val downloadQuality: String,
    val statusQueued: String,
    val statusRunning: String,
    val statusCompleted: String,
    val statusFailed: String,
    val statusBlocked: String,
    val statusCancelled: String,
    val savedToDownloads: String,
    val cancelDownload: String,
    val retry: String,
    val watermark: String,
    val metadataLoading: String,
    val terminalUrlFallback: String,
    val downloadPreparing: String,
    val downloadFetching: String,
    val downloadRunning: String,
    val downloadSaving: String,
    val downloadFinished: String,
    val downloadFailed: String,
    val retryFromManager: String,
) {
    fun quality(option: QualityOption): String = when (option) {
        QualityOption.BEST -> if (this === ENGLISH_STRINGS) "Best available" else "Terbaik yang tersedia"
        QualityOption.P360 -> "360p"
        QualityOption.P480 -> "480p"
        QualityOption.P720 -> "720p"
        QualityOption.P1080 -> "1080p"
        QualityOption.AUDIO -> if (this === ENGLISH_STRINGS) "Audio only · MP3 192 kbps" else "Audio saja · MP3 192 kbps"
    }

    fun theme(mode: ThemeMode): String = when (mode) {
        ThemeMode.SYSTEM -> themeSystem
        ThemeMode.LIGHT -> themeLight
        ThemeMode.DARK -> themeDark
    }

    fun downloadButton(option: QualityOption, count: Int = 1): String = if (count > 1) {
        if (this === ENGLISH_STRINGS) {
            "Download $count items · ${quality(option)}"
        } else {
            "Download $count item · ${quality(option)}"
        }
    } else {
        "${downloadQualityWord()} ${quality(option)}"
    }

    fun queuedMessage(count: Int): String = if (this === ENGLISH_STRINGS) {
        "$count download${if (count == 1) "" else "s"} added to the queue."
    } else {
        "$count download masuk ke antrean."
    }

    fun genericVideo(index: Int): String = if (this === ENGLISH_STRINGS) {
        "YouTube video #$index"
    } else {
        "Video YouTube #$index"
    }

    private fun downloadQualityWord(): String = if (this === ENGLISH_STRINGS) "Download" else "Download"

    companion object {
        val INDONESIAN_STRINGS = AppStrings(
            appName = "YTDLX",
            homeDescription = "Download YouTube dengan kualitas yang mudah dipilih",
            youtubeUrl = "URL YouTube",
            youtubeUrlPlaceholder = "https://youtu.be/...",
            batchInputHint = "Masukkan satu atau beberapa URL, satu URL per baris",
            fetchMetadata = "Ambil metadata",
            shareHint = "Bagikan video dari YouTube ke YTDLX atau masukkan URL di atas.",
            chooseQuality = "Pilih kualitas",
            title = "Judul",
            channel = "Saluran",
            unknownChannel = "Saluran tidak diketahui",
            duration = "Durasi",
            views = "Views",
            close = "Tutup",
            openFile = "Buka",
            shareFile = "Bagikan",
            invalidSharedUrl = "URL YouTube tidak ditemukan",
            noValidUrls = "Tidak ada URL YouTube yang valid",
            metadataFailed = "Gagal mengambil metadata",
            updateFailed = "Update yt-dlp gagal",
            inaccessibleYoutubeUrl = "Pastikan URL berasal dari video YouTube yang dapat diakses.",
            downloadManager = "Download Manager",
            settings = "Pengaturan",
            back = "Kembali",
            queuedMessage = "Download masuk antrean. Buka Download Manager untuk melihat status.",
            exitTitle = "Keluar dari YTDLX?",
            exitMessage = "Apakah kamu yakin ingin keluar dari aplikasi?",
            exit = "Keluar",
            cancel = "Batal",
            appearance = "Tampilan",
            appearanceDescription = "Pilih tampilan aplikasi yang paling nyaman.",
            themeSystem = "Ikuti sistem",
            themeLight = "Tema terang",
            themeDark = "Tema gelap",
            language = "Bahasa",
            languageDescription = "Pilih bahasa yang digunakan oleh antarmuka YTDLX.",
            ytDlp = "yt-dlp",
            ytDlpDescription = "Runtime yt-dlp di dalam aplikasi dapat diperbarui dari channel stable resmi.",
            versionNotChecked = "Versi: belum diperiksa",
            checkingUpdate = "Memeriksa dan mengunduh update…",
            installedVersion = "Versi terpasang",
            updateNow = "Update yt-dlp sekarang",
            updateNote = "Catatan: update membutuhkan koneksi internet. Jika YouTube berubah dan muncul HTTP 403, coba update yt-dlp terlebih dahulu.",
            updateAlreadyLatest = "yt-dlp sudah versi terbaru",
            updateCompleted = "yt-dlp berhasil diperbarui",
            updateUnknown = "Status update tidak diketahui",
            downloadManagerHeading = "Download Manager",
            clear = "Bersihkan",
            noDownloads = "Belum ada download. Download yang sedang berjalan akan tampil di sini.",
            downloadQuality = "Kualitas download",
            statusQueued = "Menunggu koneksi / antrean",
            statusRunning = "Sedang mengunduh",
            statusCompleted = "Selesai",
            statusFailed = "Gagal",
            statusBlocked = "Menunggu dependency",
            statusCancelled = "Dibatalkan",
            savedToDownloads = "Tersimpan di Download/YTDLX",
            cancelDownload = "Batal",
            retry = "Coba lagi",
            watermark = "Developed by Sonzai X シ",
            metadataLoading = "Mengambil metadata YouTube",
            terminalUrlFallback = "url",
            downloadPreparing = "Menyiapkan download…",
            downloadFetching = "Mengambil dan mengunduh media…",
            downloadRunning = "Mengunduh…",
            downloadSaving = "Menyimpan ke folder Download…",
            downloadFinished = "Download selesai",
            downloadFailed = "Download gagal",
            retryFromManager = "coba lagi dari Download Manager",
        )

        val ENGLISH_STRINGS = AppStrings(
            appName = "YTDLX",
            homeDescription = "Download YouTube videos with easy-to-understand quality options",
            youtubeUrl = "YouTube URL",
            youtubeUrlPlaceholder = "https://youtu.be/...",
            batchInputHint = "Enter one or more URLs, one URL per line",
            fetchMetadata = "Get metadata",
            shareHint = "Share a video from YouTube to YTDLX or enter a URL above.",
            chooseQuality = "Choose quality",
            title = "Title",
            channel = "Channel",
            unknownChannel = "Unknown channel",
            duration = "Duration",
            views = "Views",
            close = "Close",
            openFile = "Open",
            shareFile = "Share",
            invalidSharedUrl = "YouTube URL not found",
            noValidUrls = "No valid YouTube URL found",
            metadataFailed = "Failed to fetch metadata",
            updateFailed = "yt-dlp update failed",
            inaccessibleYoutubeUrl = "Make sure the URL is an accessible YouTube video.",
            downloadManager = "Download Manager",
            settings = "Settings",
            back = "Back",
            queuedMessage = "Download added to the queue. Open Download Manager to see its status.",
            exitTitle = "Exit YTDLX?",
            exitMessage = "Are you sure you want to exit the app?",
            exit = "Exit",
            cancel = "Cancel",
            appearance = "Appearance",
            appearanceDescription = "Choose the appearance that feels most comfortable.",
            themeSystem = "Follow system",
            themeLight = "Light theme",
            themeDark = "Dark theme",
            language = "Language",
            languageDescription = "Choose the language used by the YTDLX interface.",
            ytDlp = "yt-dlp",
            ytDlpDescription = "The yt-dlp runtime inside the app can be updated from the official stable channel.",
            versionNotChecked = "Version: not checked",
            checkingUpdate = "Checking and downloading update…",
            installedVersion = "Installed version",
            updateNow = "Update yt-dlp now",
            updateNote = "Note: updating requires an internet connection. If YouTube changes and HTTP 403 appears, update yt-dlp first.",
            updateAlreadyLatest = "yt-dlp is already up to date",
            updateCompleted = "yt-dlp updated successfully",
            updateUnknown = "Update status is unknown",
            downloadManagerHeading = "Download Manager",
            clear = "Clear",
            noDownloads = "No downloads yet. Active downloads will appear here.",
            downloadQuality = "Download quality",
            statusQueued = "Waiting for connection / queue",
            statusRunning = "Downloading",
            statusCompleted = "Completed",
            statusFailed = "Failed",
            statusBlocked = "Waiting for dependency",
            statusCancelled = "Cancelled",
            savedToDownloads = "Saved in Download/YTDLX",
            cancelDownload = "Cancel",
            retry = "Retry",
            watermark = "Developed by Sonzai X シ",
            metadataLoading = "Fetching YouTube metadata",
            terminalUrlFallback = "url",
            downloadPreparing = "Preparing download…",
            downloadFetching = "Fetching and downloading media…",
            downloadRunning = "Downloading…",
            downloadSaving = "Saving to the Download folder…",
            downloadFinished = "Download completed",
            downloadFailed = "Download failed",
            retryFromManager = "retry from Download Manager",
        )
    }
}

const val LANGUAGE_PREFERENCE_KEY = "language"

fun AppLanguage.strings(): AppStrings = when (this) {
    AppLanguage.INDONESIAN -> AppStrings.INDONESIAN_STRINGS
    AppLanguage.ENGLISH -> AppStrings.ENGLISH_STRINGS
}

fun AppStrings.updateMessage(result: YtdlpUpdateResult): String = when (result) {
    YtdlpUpdateResult.INSTALLED -> installedVersion
    YtdlpUpdateResult.ALREADY_UP_TO_DATE -> updateAlreadyLatest
    YtdlpUpdateResult.DONE -> updateCompleted
    YtdlpUpdateResult.UNKNOWN -> updateUnknown
}

enum class YtdlpUpdateResult {
    INSTALLED,
    ALREADY_UP_TO_DATE,
    DONE,
    UNKNOWN,
}
