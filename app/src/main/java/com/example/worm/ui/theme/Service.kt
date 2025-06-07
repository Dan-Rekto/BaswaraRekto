package com.example.worm.ui.theme
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.compose.foundation.interaction.DragInteraction


class MyForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        // dipanggil saat service pertama kali dibuat
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // jalankan foreground service di sini
        when(intent?.action){

        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Kita tidak perlu binding, jadi kembalikan null
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // hentikan semua tugas, bersihkan resource
    }
}
