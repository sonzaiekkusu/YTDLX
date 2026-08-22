# Temuan perbandingan YTDLnis

Tanggal riset: 2026-08-22.

Sumber resmi repository YTDLnis: https://github.com/deniscerri/ytdlnis

README repository menyebut YTDLnis memiliki plugin/package yang dapat di-upgrade atau downgrade untuk Python, JavaScript runtimes seperti NodeJS/Deno, FFmpeg, dan Aria2c. Package dapat diinstal dari repository ytdlnis-packages atau melalui bagian update aplikasi. Ini menunjukkan komponen runtime dapat dikelola terpisah dari APK utama.

Sumber build resmi: https://raw.githubusercontent.com/deniscerri/ytdlnis/main/app/build.gradle

Konfigurasi YTDLnis yang terlihat di source:

- ABI splits aktif untuk x86, x86_64, armeabi-v7a, dan arm64-v8a.
- `universalApk true` juga diaktifkan, sehingga build universal dapat lebih besar, sedangkan APK per-ABI lebih kecil.
- `packagingOptions.jniLibs.useLegacyPackaging true` aktif.
- Release memakai `minifyEnabled true`, `shrinkResources true`, dan `proguard-android-optimize.txt`.
- Runtime memakai `io.github.junkfood02.youtubedl-android:library:0.18.1` dan Aria2c sebagai dependency langsung.
- README repository menyebut runtime/package dapat diperbarui di luar APK melalui plugin/package system.

Interpretasi awal: angka sekitar 40 MB yang dilihat pengguna kemungkinan adalah APK per-ABI tertentu, bukan universal APK, atau APK yang tidak membawa semua runtime terbaru sekaligus. Fitur aplikasi yang banyak tidak menentukan ukuran sebesar binary native Python/FFmpeg/QuickJS. R8 mengoptimalkan class/resource, tetapi tidak mengecilkan `.so`, binary, atau archive runtime native. YTDLX saat ini hanya membawa arm64-v8a, tetapi dependency library/FFmpeg tetap menjadi sumber utama ukuran. Perbandingan berikutnya perlu memeriksa asset package YTDLnis, jenis file release yang diunduh, dan apakah runtime dipasang terpisah/diunduh saat runtime.

## Temuan release dan helper APK

Sumber release resmi YTDLnis: https://github.com/deniscerri/ytdlnis/releases

Release v1.8.9.1 menyediakan artifact terpisah untuk `foss-arm64-v8a`, `foss-armeabi-v7a`, `foss-x86`, `foss-x86_64`, dan `foss-universal`, serta varian `github-*` per ABI dan universal. Halaman release menampilkan 13 asset, sehingga angka ukuran sekitar 40 MB perlu dipastikan berasal dari artifact per-ABI, bukan universal.

Sumber package resmi: https://github.com/deniscerri/ytdlnis-packages

README package menjelaskan bahwa tiap package adalah helper APK yang hanya berisi `jniLibs`; YTDLnis mengekstrak `libxxx.so` dari helper APK dan mengakses archive `libxxx.zip.so` untuk menyalin/mengekstrak isi runtime ke filesystem aplikasi. Mekanisme helper APK ini memungkinkan Python/FFmpeg/JS runtime dipasang atau diperbarui terpisah dari APK utama dan juga melewati batas SDK28 yang dijelaskan oleh project tersebut.

Repository package memiliki folder terpisah untuk `python_package`, `ffmpeg_package`, `deno_package`, dan `nodejs_package`, serta release package sendiri. README YTDLnis juga menyebut plugin/package dapat di-upgrade atau downgrade dari aplikasi.

Kesimpulan yang diperkuat: YTDLnis dapat terlihat sekitar 40 MB karena artifact yang diunduh bisa berupa APK per-ABI dan runtime native besar tidak seluruhnya berada di APK utama—sebagian ada di helper APK/package yang diinstal atau diambil terpisah. YTDLX saat ini memasukkan runtime youtubedl-android + FFmpeg langsung sebagai dependency APK, sehingga R8 tidak dapat mengurangi native `.so`/archive. Menambah ProGuard rules tidak akan membuat YTDLX setara ukuran YTDLnis; perubahan arsitektur helper runtime atau dynamic package diperlukan.

## Temuan F-Droid metadata

Sumber: https://gitlab.com/fdroid/fdroiddata/-/raw/master/metadata/com.deniscerri.ytdl.yml

F-Droid metadata membangun dan mempublikasikan YTDLnis sebagai beberapa APK terpisah: `armeabi-v7a`, `x86`, `x86_64`, dan `arm64-v8a`. Untuk versi 1.8.9.1, output berada pada flavor `foss` dengan pola nama per-ABI, bukan hanya satu APK universal. Metadata juga menyebut binary release URL masing-masing ABI. Ini mendukung hipotesis bahwa angka ukuran yang dilihat pengguna adalah ukuran varian per-ABI.

F-Droid metadata juga menunjukkan YTDLnis menggunakan source repository yang sama dan menjalankan update yt-dlp melalui GitHub API sebagai dependency/non-free-network feature. Ukuran fitur UI tidak dapat dibandingkan langsung dengan ukuran runtime native karena komponen runtime dipisah melalui helper package.

## Ukuran APK terverifikasi

Sumber F-Droid: https://f-droid.org/en/packages/com.deniscerri.ytdl/

F-Droid mencantumkan YTDLnis 1.8.9.1 untuk `arm64-v8a` dengan ukuran **47 MiB** dan `x86_64` dengan ukuran **49 MiB**. Versi x86 pada halaman yang sama tercantum **63 MiB**. Jadi klaim YTDLnis sekitar 40 MB memang sesuai untuk varian arm64 tertentu, tetapi bukan berarti APK universal atau semua varian berukuran 40 MB.

Sumber issue resmi: https://github.com/deniscerri/ytdlnis/issues/969

Dalam diskusi issue tersebut terdapat pernyataan bahwa setelah FFmpeg membesar, ukuran aplikasi naik dari sekitar **40 MB menjadi 70 MB**. Ini menjadi bukti praktis bahwa runtime native, khususnya FFmpeg, dapat mengubah ukuran APK secara drastis meskipun fitur aplikasi dan R8 tidak berubah.

Kesimpulan akhir: YTDLnis arm64 47 MiB dan YTDLX sekitar 55 MB berada pada kelas ukuran yang sama. Selisih tersebut kemungkinan berasal dari versi/build runtime native, flavor/package set, dan perbedaan app dependencies—bukan jumlah fitur UI atau kegagalan R8. Untuk perbandingan adil, bandingkan APK arm64-v8a dengan APK arm64-v8a pada versi dependency/runtime yang sama dan cek `lib/arm64-v8a/` menggunakan APK Analyzer.
