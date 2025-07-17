# Baswara: AI Hoax Detector 🚀

### Deteksi Hoax di Ujung Jari Anda dengan Bantuan AI

Baswara adalah sebuah aplikasi Android inovatif yang dirancang untuk memerangi penyebaran misinformasi dan hoax. Dengan sekali tekan, aplikasi ini dapat memindai konten di layar Anda, mengekstrak teksnya, dan mengirimkannya ke Google Gemini AI untuk analisis mendalam, memberikan Anda respons cepat mengenai potensi kebenaran informasi tersebut.

---

## 📸 Tampilan Aplikasi

| Tampilan Notifikasi Kontrol | Tampilan Hasil Analisis |
| :---: | :---: |
| ![Screenshot Notifikasi](https://via.placeholder.com/250x500.png?text=Notifikasi+Kontrol) | ![Screenshot Hasil](https://via.placeholder.com/250x500.png?text=Notifikasi+Hasil) |

---

## ✨ Fitur Utama

* **Scan Layar Sekali Klik**: Menganalisis konten apa pun di layar Anda (artikel berita, chat, media sosial) melalui notifikasi persisten yang mudah diakses.
* **Analisis Hoax dengan Gemini AI**: Memanfaatkan model AI canggih dari Google untuk mengevaluasi teks dan memberikan indikasi apakah informasi tersebut berpotensi hoax atau tidak, lengkap dengan penjelasannya.
* **Ekstraksi Teks Otomatis (OCR)**: Menggunakan Google ML Kit untuk secara akurat mengenali dan mengekstrak teks dari gambar hasil tangkapan layar.
* **Notifikasi Hasil Cerdas**: Hasil analisis dari AI ditampilkan secara langsung melalui notifikasi prioritas tinggi, sehingga Anda tidak perlu meninggalkan aplikasi yang sedang digunakan.
* **Kontrol Penuh**: Mudah untuk memulai, menghentikan, dan memindai kapan saja melalui action button pada notifikasi.

---

## ⚙️ Cara Kerja

Aplikasi ini berjalan sebagai *foreground service* untuk memastikan fungsionalitas yang andal. Berikut alur kerjanya:

1.  **Mulai Layanan**: Pengguna memberikan izin `MediaProjection` (perekaman layar) satu kali.
2.  **Kontrol via Notifikasi**: Sebuah notifikasi persisten muncul dengan tombol "Scan" dan "Stop".
3.  **Pindai & Ekstrak**: Saat tombol "Scan" ditekan, `RunningService` mengambil tangkapan layar. Gambar ini kemudian diproses oleh **ML Kit Text Recognition** untuk mengekstrak semua teks yang terlihat.
4.  **Kirim ke AI**: Teks yang sudah diekstrak kemudian dibungkus dalam sebuah *prompt* khusus dan dikirim ke **Google Gemini API** melalui panggilan REST.
5.  **Tampilkan Hasil**: Respons dari Gemini AI diterima dan ditampilkan sebagai notifikasi baru, memberitahu pengguna hasil analisis hoax.

---

## 🔧 Teknologi & Pustaka yang Digunakan

* **Bahasa**: [Kotlin](https://kotlinlang.org/)
* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Arsitektur**: Berbasis Service (Android `ForegroundService`)
* **Perekaman Layar**: [MediaProjection API](https://developer.android.com/reference/android/media/projection/MediaProjection)
* **OCR**: [Google ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition)
* **AI Model**: [Google Gemini API](https://ai.google.dev/)
* **Networking**: [OkHttp](https://square.github.io/okhttp/)
* **JSON Parsing**: [Moshi](https://github.com/square/moshi)

---

## 🛠️ Setup & Instalasi

Untuk menjalankan proyek ini di mesin lokal Anda, ikuti langkah-langkah berikut:

1.  **Clone Repositori**
    ```sh
    git clone [https://github.com/Dan-Rekto/BaswaraRekto.git](https://github.com/Dan-Rekto/BaswaraRekto.git)
    ```

2.  **Buka di Android Studio**
    Buka proyek yang telah di-clone dengan Android Studio versi terbaru.

3.  **Dapatkan Kunci API Gemini**
    Aplikasi ini membutuhkan kunci API dari Google AI Studio untuk berfungsi.
    * Kunjungi [Google AI Studio](https://aistudio.google.com/app/apikey) untuk membuat kunci API Anda.

4.  **Build dan Jalankan Aplikasi**
    Sinkronkan Gradle, lalu build dan jalankan aplikasi pada perangkat Android fisik atau emulator.

---

## 📜 Lisensi

Lihat `LICENSE.md` untuk informasi lebih lanjut.

---

## 📬 Kontak

Dan-Rekto - [https://github.com/Dan-Rekto](https://github.com/Dan-Rekto)

For OPSI 2025
