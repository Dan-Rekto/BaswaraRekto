package com.example.worm.ui.theme
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat


class RunningService : Service() {

    override fun onCreate() {
        super.onCreate()
        // dipanggil saat service pertama kali dibuat
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // jalankan foreground service di sini
        when(intent?.action){
        Actions.START.toString() -> start()
            Actions.STOP.toString() -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }
    enum class Actions{
        START, STOP
    }
    private fun start(){
        val notification = NotificationCompat.Builder(this, "running-channeled")
            .setContentTitle("Cek Hoax")
            .setContentText("Cek Hoax Melalui Scan Layar Baswara")
        startForeground(1, notification)
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
