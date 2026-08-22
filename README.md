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

## Metadata loading

Saat metadata sedang diambil, YTDLX menampilkan panel bergaya terminal dengan command yt-dlp, URL ringkas, status proses, animasi titik, dan progress bar horizontal. Setelah metadata tersedia, aplikasi menampilkan informasi terstruktur dengan label **Judul**, **Saluran**, **Durasi**, dan **Views**, kemudian menampilkan pilihan kualitas.

Label kualitas disimpan dalam input serta output WorkManager. Karena itu, item yang diunduh sebagai **720p** tetap menampilkan `Kualitas download: 720p` setelah selesai, bukan kembali ke `Terbaik yang tersedia`.

## Floating Share panel

Saat URL YouTube dibagikan dari YouTube, Sharesheet sekarang memilih `ShareActivity` YTDLX. Activity tersebut transparan sehingga video YouTube tetap terlihat di belakang panel. Panel menampilkan terminal loading ketika metadata sedang diambil, kemudian baru menampilkan judul, channel, durasi, dan pilihan kualitas. Setelah pengguna menekan tombol download, pekerjaan masuk ke WorkManager dan panel ditutup sehingga pengguna kembali ke YouTube.

Pendekatan ini menggunakan transparent ShareActivity agar tidak meminta `SYSTEM_ALERT_WINDOW`. Android tetap menampilkan activity berbagi sebagai layar aktif, tetapi konten YouTube tetap terlihat di belakangnya. Ini lebih aman daripada meminta izin overlay global; YTDLnis digunakan sebagai referensi untuk pola ShareActivity floating.[1] [2]

Jika URL tidak valid atau metadata gagal diambil, panel menampilkan error dan tombol **Tutup**. Download Manager utama tetap dapat dibuka dari ikon download di TopAppBar.

## Settings

Ikon gerigi di kanan atas membuka Settings. Di sana pengguna dapat memilih **Ikuti sistem**, **Tema terang**, atau **Tema gelap**; pilihan disimpan secara lokal dan langsung diterapkan ke UI serta system bars. Pengaturan juga menyediakan pilihan bahasa **Bahasa Indonesia** dan **English**. Katalog string dipusatkan di `Localization.kt` sehingga bahasa baru dapat ditambahkan tanpa menyebarkan teks ke seluruh composable.

Settings juga menampilkan versi yt-dlp yang terpasang dan menyediakan tombol untuk memperbarui binary yt-dlp melalui channel stable resmi. Pada pembukaan MainActivity pertama setelah instalasi, YTDLX menjalankan update stable otomatis satu kali. Hasil update tidak dijalankan ulang pada pembukaan berikutnya; update manual tetap tersedia dari Settings.

## Splash screen and watermark

MainActivity menggunakan AndroidX SplashScreen saat aplikasi dibuka. Pada pembukaan pertama, splash screen dipertahankan selama proses update yt-dlp awal berlangsung, kemudian dilepas setelah proses selesai atau gagal. Semua halaman utama, Settings, Download Manager, dan floating Share panel memiliki watermark kecil di bagian bawah: `Developed by Sonzai X シ`.

## Navigation and exit behavior

Settings dan Download Manager sekarang merupakan halaman sekunder. Tombol back dari salah satu halaman tersebut kembali ke menu utama, sedangkan back dari menu utama menampilkan dialog konfirmasi sebelum aplikasi ditutup.

## Download Manager dan notifikasi

Download Manager menggunakan WorkManager dan mempertahankan setiap pekerjaan sebagai antrean persisten. UI menampilkan status menunggu, berjalan, selesai, gagal, atau dibatalkan; progress download; tombol **Batal** untuk pekerjaan aktif; tombol **Coba lagi** untuk pekerjaan gagal/dibatalkan; serta **Bersihkan** untuk menghapus item yang sudah selesai dari daftar kerja.

Setiap pekerjaan memiliki notifikasi progress sendiri dengan judul video dan kualitas yang dipilih. Menekan notifikasi tersebut membuka aplikasi langsung pada Download Manager. Setelah pekerjaan selesai, notifikasi progress digantikan oleh notifikasi hasil yang menampilkan judul video, status selesai atau gagal, dan kualitas download. Menekan notifikasi hasil juga membuka Download Manager.

Jika beberapa video diunduh sekaligus, setiap pekerjaan memakai notification ID yang berbeda dan dikelompokkan sebagai notifikasi YTDLX. Dengan demikian, notifikasi video pertama tidak tertimpa oleh video kedua; semua pekerjaan tetap dapat dipantau satu per satu, sedangkan daftar lengkapnya tersedia di Download Manager.

Pekerjaan membutuhkan koneksi jaringan, memakai retry exponential backoff, berjalan sebagai foreground work dengan notifikasi, dan menyimpan file selesai melalui MediaStore ke `Download/YTDLX`.

## Launcher icon

YTDLX memakai adaptive launcher icon berbasis vector drawable pada `app/src/main/res/drawable/ic_launcher_foreground.xml`. Simbolnya sekarang berupa terminal `>_` kecil yang diposisikan di tengah, dengan warna foreground putih dan underscore ungu. Konfigurasi adaptive icon tersedia pada `mipmap-anydpi-v26/ic_launcher.xml` dan `ic_launcher_round.xml`.

## Dependency engine

```kotlin
implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
```

Dependency tersebut membawa runtime Android yang dibutuhkan ke dalam APK. **Python tidak perlu diinstal di komputer untuk build project ini**, dan pengguna APK juga tidak perlu memasang Python atau Termux secara terpisah.

## Build

Buka project ini menggunakan Android Studio versi terbaru dengan Android SDK 36, JDK 17 atau 21, dan Android NDK/toolchain standar yang dikelola Android Studio. Python 3.10/3.14 dan Termux tidak diperlukan untuk build YTDLX setelah refactor native ini.

Jalankan build debug dari terminal project:

```powershell
.\\gradlew.bat assembleDebug
```

APK debug akan berada di `app/build/outputs/apk/debug/app-debug.apk`. Build debug tidak menggunakan minify agar proses development lebih mudah di-debug.

Build release menggunakan R8 dan resource shrinking:

```powershell
.\\gradlew.bat assembleRelease
```

Konfigurasi release mengaktifkan `minifyEnabled true`, `shrinkResources true`, `debuggable false`, dan `proguard-android-optimize.txt` bersama `app/proguard-rules.pro`. Aturan tersebut mempertahankan entry point WorkManager, Application, serta bridge runtime yt-dlp/FFmpeg yang perlu diakses melalui reflection/JNI, sementara class dan resource yang tidak digunakan dapat dihapus oleh R8.

Branch `shrink-obfuscate-optimize` berisi rules eksperimen yang lebih selektif. R8 hanya mengoptimalkan bytecode Java/Kotlin dan resource; R8 tidak dapat mengecilkan isi native `.so` seperti Python, FFmpeg, FFprobe, dan QuickJS. Ukuran native terutama ditentukan oleh dependency runtime, ABI, dan metode packaging.

## Release keystore

Repository hanya menyimpan konfigurasi Gradle dan aturan R8. Keystore serta password tidak di-commit karena repository bersifat publik. Untuk build release bertanda tangan SonzaiX, letakkan file berikut di root project:

```text
sonzaix-release.jks
keystore.properties
```

Isi `keystore.properties` menggunakan format berikut dan sesuaikan password dengan keystore milikmu:

```properties
storeFile=../sonzaix-release.jks
storePassword=PASSWORD_KEYSTORE
keyAlias=sonzaix
keyPassword=PASSWORD_KEY
```

Jika `keystore.properties` tersedia, build release memakai keystore tersebut. Jika file belum tersedia, project masih dapat dikompilasi menggunakan debug signing untuk keperluan pemeriksaan lokal; jangan gunakan APK fallback tersebut untuk distribusi publik. Simpan backup keystore dan password di tempat aman karena APK release yang sudah didistribusikan harus terus ditandatangani dengan key yang sama untuk menerima update.

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
- [Android overlay permission reference](https://developer.android.com/reference/android/Manifest.permission)
- [YTDLnis ShareActivity overlay reference](https://github.com/deniscerri/ytdlnis/blob/main/app/src/main/java/com/deniscerri/ytdl/receiver/ShareActivity.kt)
- [WorkManager long-running workers](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running)
- [MediaStore shared storage](https://developer.android.com/training/data-storage/shared/media)

[1]: https://github.com/deniscerri/ytdlnis/blob/main/app/src/main/java/com/deniscerri/ytdl/receiver/ShareActivity.kt "YTDLnis ShareActivity reference"
[2]: https://developer.android.com/reference/android/Manifest.permission "Android permission reference"
