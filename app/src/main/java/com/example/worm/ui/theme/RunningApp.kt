package com.example.worm.ui.theme
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.example.worm.R
class RunningApp : Application() {
    override fun onCreate(){
        super.onCreate()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "running_channel",
            "Running Notif",
            NotificationManager.IMPORTANCE_HIGH
        )
            val notifmanager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifmanager.createNotificationChannel(channel)
        }
    }
}
