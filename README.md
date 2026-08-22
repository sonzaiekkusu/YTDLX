# YTDLX

YTDLX adalah aplikasi Android native Kotlin untuk menerima URL YouTube dari Android Sharesheet, menampilkan metadata, memilih kualitas sederhana, dan menjalankan download di background.

> Status: initial native-engine refactor. Target pertama adalah perangkat Android `arm64-v8a` dan URL YouTube yang dibagikan sebagai teks.

## Alur pengguna

1. Buka video di aplikasi YouTube.
2. Pilih **Share** lalu pilih **YTDLX**.
3. Aplikasi mengambil metadata video.
4. Pilih **Terbaik**, **360p**, **480p**, **720p**, **1080p**, atau **Audio MP3**.
5. Download berjalan di background dan hasil dipublikasikan ke `Download/YTDLX`.

## Arsitektur

- **Kotlin + Jetpack Compose:** UI, Share Intent, quality picker, dan state.
- **youtubedl-android:** wrapper Android untuk yt-dlp dengan Python runtime Android yang telah dipaketkan.
- **FFmpeg Android:** merge stream video/audio dan ekstraksi audio.
- **WorkManager foreground worker:** pekerjaan download panjang, notifikasi, dan pembatalan.
- **MediaStore:** menyimpan file ke folder Download tanpa meminta akses penuh ke storage pada Android 10+.

YTDLX mengikuti pola runtime dari [YTDLnis](https://github.com/deniscerri/ytdlnis), yang menginisialisasi runtime Android untuk Python, yt-dlp, FFmpeg, dan JavaScript lalu menjalankan yt-dlp sebagai proses lokal. YTDLX menggunakan library `youtubedl-android` sebagai dependency langsung, bukan menyalin source YTDLnis.

## Settings

Ikon gerigi di kanan atas membuka Settings. Di sana pengguna dapat memilih **Ikuti sistem**, **Tema terang**, atau **Tema gelap**; pilihan disimpan secara lokal dan langsung diterapkan ke UI serta system bars. Settings juga menampilkan versi yt-dlp yang terpasang dan menyediakan tombol untuk memperbarui binary yt-dlp melalui channel stable resmi.

## Download Manager

Download Manager menggunakan WorkManager dan mempertahankan setiap pekerjaan sebagai antrean persisten. UI menampilkan status menunggu, berjalan, selesai, gagal, atau dibatalkan; progress download; tombol **Batal** untuk pekerjaan aktif; tombol **Coba lagi** untuk pekerjaan gagal/dibatalkan; serta **Bersihkan** untuk menghapus item yang sudah selesai dari daftar kerja.

Pekerjaan membutuhkan koneksi jaringan, memakai retry exponential backoff, berjalan sebagai foreground work dengan notifikasi, dan menyimpan file selesai melalui MediaStore ke `Download/YTDLX`.

## Launcher icon

YTDLX memakai adaptive launcher icon berbasis vector drawable pada `app/src/main/res/drawable/ic_launcher_foreground.xml`, dengan simbol panah download dan tray. Konfigurasi adaptive icon tersedia pada `mipmap-anydpi-v26/ic_launcher.xml` dan `ic_launcher_round.xml`.

## Dependency engine

```kotlin
implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
```

Dependency tersebut membawa runtime Android yang dibutuhkan ke dalam APK. **Python tidak perlu diinstal di komputer untuk build project ini**, dan pengguna APK juga tidak perlu memasang Python atau Termux secara terpisah.

## Build

Buka project ini menggunakan Android Studio versi terbaru dengan Android SDK 36, JDK 17 atau 21, dan Android NDK/toolchain standar yang dikelola Android Studio. Python 3.10/3.14 dan Termux tidak diperlukan untuk build YTDLX setelah refactor native ini.

Jalankan build dari terminal project:

```powershell
.\\gradlew.bat assembleDebug
```

APK debug akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

Workflow GitHub Actions ada di `.github/workflows/android.yml` dan hanya bisa dijalankan manual melalui **Actions → Android build → Run workflow** pada branch `main`.

## Catatan format

Menu kualitas menggunakan resolusi yang mudah dipahami. Selector akan mengutamakan H.264/MP4 + M4A untuk kompatibilitas Android, lalu memakai fallback codec yang tersedia. VP9 dan AV1 tidak perlu dipilih manual oleh pengguna biasa.

## Referensi dan lisensi

YTDLX memakai library [youtubedl-android](https://github.com/yausername/youtubedl-android) melalui artifact `io.github.junkfood02.youtubedl-android`. Library tersebut berlisensi GPL-3.0. [YTDLnis](https://github.com/deniscerri/ytdlnis) dan [ytdlnis-packages](https://github.com/deniscerri/ytdlnis-packages) digunakan sebagai **referensi arsitektur runtime Android**, khususnya cara menginisialisasi Python/yt-dlp, FFmpeg, JavaScript runtime, dan helper packages. Source YTDLnis tidak disalin ke project ini.

Sebelum distribusi publik, tinjau kembali kewajiban GPL-3.0, lisensi dependency transitif, attribution, nama aplikasi, dan ketentuan layanan platform. Gunakan aplikasi hanya untuk konten yang memang berhak kamu simpan.

## Referensi teknis

- [youtubedl-android README dan API](https://github.com/yausername/youtubedl-android)
- [YTDLnis RuntimeManager](https://github.com/deniscerri/ytdlnis/blob/main/app/src/main/java/com/deniscerri/ytdl/core/RuntimeManager.kt)
- [YTDLnis source repository](https://github.com/deniscerri/ytdlnis)
- [YTDLnis Android runtime packages](https://github.com/deniscerri/ytdlnis-packages)
- [Android Sharesheet](https://developer.android.com/develop/ui/compose/sharing/send)
- [WorkManager long-running workers](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running)
- [MediaStore shared storage](https://developer.android.com/training/data-storage/shared/media)
