package com.example.worm.ui.theme

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.worm.MainActivity
import com.example.worm.R
import com.example.worm.sw
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import kotlin.math.log

// Data classes for Gemini API
private data class GeminiRequest(val prompt: String, val maxTokens: Int)
private data class Choice(val text: String)
private data class GeminiResponse(val choices: List<Choice>)

class RunningService : Service() {
    companion object {
        var jalan: Boolean = false
        const val ACTION_START = "ACTION_START_PROJECTION"
        const val ACTION_STOP = "ACTION_STOP_PROJECTION"
        const val ACTION_SCREEN = "ACTION_SCREENSHOT"

       const val EXTRA_RESULT_CODE = "MEDIA_PROJECTION_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "MEDIA_PROJECTION_RESULT_DATA"

        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "baswara-media-projection-channel-v2"
        const val CHANNEL_NAME = "Layanan Pindai Layar Baswara"
       const val GEMINI_NOTIF_ID = 6969
    }

    private var mediaProjection: MediaProjection? = null
    private lateinit var imageReader: ImageReader
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        jalan = true
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultData != null) {
                    startMediaProjection(resultCode, resultData)
                } else {
                    stopSelf()
                }
            }
            ACTION_SCREEN -> {
                handleCapture()
                Log.d("BaswaraService", "ACTION_SCREEN received (SCAN button pressed)")
            }
            ACTION_STOP -> {
                Log.d("BaswaraService", "ACTION_STOP received")
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startMediaProjection(resultCode: Int, resultData: Intent) {
        createNotificationChannel()

        // Build pending intents
        val stopIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_STOP
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "stop_and_home")
        }
        val stopPending = PendingIntent.getActivity(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val screenIntent = Intent(this, RunningService::class.java).apply {
            action = ACTION_SCREEN
        }
        val screenPending = PendingIntent.getService(
            this, 2, screenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        // Build and start as foreground
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Baswara")
            .setContentText("Cek Hoax Melalui Scan Layar")
            .setSmallIcon(R.drawable.logo2)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.tab, "Stop", stopPending)
            .addAction(R.drawable.home_svgrepo_com, "Buka", openPending)
            .addAction(R.drawable.glass, "Scan", screenPending)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }

        // Initialize projection
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mgr.getMediaProjection(resultCode, resultData)

        // Setup imageReader & display
        val dm = resources.displayMetrics
        imageReader = ImageReader.newInstance(dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "RunningServiceCapture",
            dm.widthPixels, dm.heightPixels, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, Handler(Looper.getMainLooper())
        )
    }

    private fun handleCapture() {
        Log.d("BaswaraService", "handleCapture(): membuat OneShot VirtualDisplay")

        // (1) Buat ulang VirtualDisplay
        virtualDisplay?.release()
        val dm = resources.displayMetrics
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "OneShotCapture",
            dm.widthPixels, dm.heightPixels, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, Handler(Looper.getMainLooper())
        )

        // (2) Buang frame lama (jika ada)
        while (imageReader.acquireLatestImage() != null) { /* buang */ }

        // (3) Pasang listener untuk frame berikutnya
        imageReader.setOnImageAvailableListener({ reader ->
            Log.d("BaswaraService", "ImageAvailable—mengolah bitmap")
            val img = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val plane = img.planes[0]; val buf = plane.buffer
            val pixelStride = plane.pixelStride; val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * dm.widthPixels

            val bmp = Bitmap.createBitmap(
                dm.widthPixels + rowPadding / pixelStride,
                dm.heightPixels,
                Bitmap.Config.ARGB_8888
            )
            bmp.copyPixelsFromBuffer(buf)
            img.close()

            // (4) Proses OCR atau kirim ke Gemini
            val input = InputImage.fromBitmap(bmp, 0)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(input)
                .addOnSuccessListener {
                    Log.d("BaswaraService", "OCR sukses: ${it.text.take(50)}")
                    sendNotification("OCR: ${it.text.take(100)}")
                }
                .addOnFailureListener {
                    Log.e("BaswaraService", "OCR gagal: ${it.message}")
                }

            // (5) Bebaskan VirtualDisplay agar siap scan berikutnya
            virtualDisplay?.release()
            virtualDisplay = null
        }, Handler(Looper.getMainLooper()))
    }

    private fun sendNotification(content: String) {
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo2)
            .setContentTitle("Gemini Response")
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(GEMINI_NOTIF_ID, notif)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            chan.enableVibration(true)
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(chan)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        jalan = false
    }
}
