# YTDLX

YTDLX adalah aplikasi Android native Kotlin untuk menerima URL YouTube dari Android Sharesheet, menampilkan metadata, memungkinkan pengguna memilih kualitas sederhana, lalu menjadwalkan download dengan notifikasi.

> Status: initial MVP scaffold. Target pertama adalah perangkat Android `arm64-v8a` dan URL YouTube yang dibagikan sebagai teks.

## Alur pengguna

1. Buka video di aplikasi YouTube.
2. Pilih **Share** lalu pilih **YTDLX**.
3. Aplikasi mengambil metadata video.
4. Pilih **Terbaik**, **360p**, **480p**, **720p**, **1080p**, atau **Audio MP3**.
5. Download berjalan di background dan hasil dipublikasikan ke `Download/YTDLX`.

## Arsitektur

- **Kotlin + Jetpack Compose:** UI, Share Intent, quality picker, dan state.
- **Chaquopy:** menjalankan Python tertanam untuk memanggil yt-dlp.
- **WorkManager foreground worker:** pekerjaan download panjang, notifikasi, dan pembatalan.
- **MediaStore:** menyimpan file ke folder Download tanpa meminta akses penuh ke storage pada Android 10+.
- **Python bridge:** `app/src/main/python/ytdlx_engine.py` mengembalikan metadata dan menjalankan yt-dlp.

## Status dan batasan awal

Project ini mengunci ABI ke `arm64-v8a`, sesuai perangkat Android ARM64 modern. Integrasi runtime Python/yt-dlp masih memerlukan verifikasi build pada Android Studio. Dukungan JavaScript EJS/Deno dan FFmpeg perlu dipaketkan atau diintegrasikan secara khusus untuk dukungan YouTube penuh; binary Termux tidak dapat dipakai langsung oleh aplikasi Android yang berdiri sendiri karena sandbox aplikasi.

Pada tahap awal, format selector mengutamakan H.264/MP4 + M4A agar kompatibel dengan pemutar Android, lalu melakukan fallback ke codec yang tersedia. VP9 adalah codec video, bukan audio codec, dan tidak perlu dipilih manual oleh pengguna biasa.

## Build

Buka project ini menggunakan Android Studio versi terbaru dengan Android SDK 36 dan JDK 17. Jalankan:

```bash
./gradlew assembleDebug
```

APK debug akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

## Konfigurasi Python

Chaquopy mengambil dependency Python yang didefinisikan di `app/build.gradle.kts`. Versi Python saat ini ditetapkan ke 3.10. Untuk rilis production, engine YouTube, EJS JavaScript runtime, dan FFmpeg perlu diuji pada perangkat target serta ditinjau lisensi dan ukuran APK-nya.

## Legal and privacy

Gunakan aplikasi hanya untuk konten yang memang berhak kamu simpan dan sesuai ketentuan layanan platform. URL dibagikan ke engine lokal di dalam aplikasi; project ini tidak memiliki server backend atau akun pengguna. Jangan menambahkan cookie browser atau kredensial ke source code.

## Referensi

- [Android Sharesheet and ACTION_SEND](https://developer.android.com/develop/ui/compose/sharing/send)
- [WorkManager long-running workers](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running)
- [MediaStore shared storage](https://developer.android.com/training/data-storage/shared/media)
- [yt-dlp installation and dependencies](https://github.com/yt-dlp/yt-dlp/wiki/Installation)
- [Chaquopy Android documentation](https://chaquo.com/chaquopy/doc/current/android.html)
