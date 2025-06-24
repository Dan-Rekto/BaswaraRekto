package com.example.worm.ui.theme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.example.worm.R

class RunningService : Service() {

    private var mediaProjection: MediaProjection? = null

    companion object {
        const val ACTION_START = "ACTION_START_PROJECTION"
        const val ACTION_STOP = "ACTION_STOP_PROJECTION"
        const val EXTRA_RESULT_CODE = "MEDIA_PROJECTION_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "MEDIA_PROJECTION_RESULT_DATA"

        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "baswara-media-projection-channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Layanan Pindai Layar Baswara"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, AppCompatActivity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

                if (resultCode == AppCompatActivity.RESULT_OK && resultData != null) {
                    startMediaProjection(resultCode, resultData)
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startMediaProjection(resultCode: Int, resultData: Intent) {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Cek Hoax")
            .setContentText("Cek Hoax Melalui Scan Layar Baswara")
            .setSmallIcon(R.drawable.logo)
            .setOngoing(true)
            .setUsesChronometer(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        mediaProjection = null
    }
}
