# Rencana adaptasi fitur YTDLnis untuk YTDLX

## Prinsip

YTDLX hanya akan mengadaptasi konsep dan perilaku umum yang relevan. Source code YTDLnis tidak disalin. Implementasi tetap menggunakan Kotlin, Compose, WorkManager, MediaStore, dan API youtubedl-android yang sudah dipakai YTDLX.

## Fitur prioritas yang diimplementasikan di branch eksperimen

1. Batch URL YouTube: input multiline yang memproses beberapa URL valid menjadi beberapa pekerjaan WorkManager. Setiap URL tetap divalidasi dan dibuat sebagai work item terpisah.
2. Queue/download paralel: antrean memanfaatkan WorkManager dengan unique work ID per URL. Download Manager menampilkan semua item dan notifikasi tidak saling menimpa.
3. Aksi hasil notifikasi: notifikasi selesai menyediakan aksi membuka file media dan berbagi file jika URI MediaStore tersedia, selain tap utama yang membuka Download Manager.
4. Informasi metadata detail: judul, saluran, durasi, dan status tetap konsisten untuk item batch.
5. Error isolation: URL yang invalid tidak boleh membatalkan URL valid lainnya; status error disimpan per item.

## Fitur yang ditunda

Cookies/login, custom yt-dlp command, scheduler berbasis tanggal, SponsorBlock, chapter splitting, subtitle embedding, arbitrary website support, helper APK runtime Python/FFmpeg, dan playlist item editor membutuhkan API, UI, atau arsitektur runtime yang lebih besar. Fitur tersebut tidak akan dipalsukan hanya dengan menambahkan ProGuard rules.

## Kontrak data

Setiap work item menyimpan URL, judul, kualitas, dan status melalui WorkManager. Result notification menerima URI output dari MediaStore. Batch enqueue mengirim work item satu per satu agar retry/cancel/history tetap terisolasi.
