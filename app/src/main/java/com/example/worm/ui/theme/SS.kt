package com.example.worm.ui.theme

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.example.worm.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import okhttp3.internal.notify

// ScreenshotProxyActivity.kt
class ScreenshotProxyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var judul = "AI Semakin Mengganas!"
        var hasil = "Faktual"
        // 1. Buat NotificationChannel (hanya perlu sekali, biasanya di awal atau saat app launch)
        val CHANNEL_ID = "my_channel_id"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "My Channel"
            val descriptionText = "Channel for my notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Daftarkan channel ke sistem
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

// 2. Buat Notifikasi
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo2)
            .setContentTitle(judul)
            .setContentText(hasil)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)

        super.onCreate(savedInstanceState)
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(1001, builder.build())
        // Selesai, keluarkan activity ini segera
        finish()
    }
}
