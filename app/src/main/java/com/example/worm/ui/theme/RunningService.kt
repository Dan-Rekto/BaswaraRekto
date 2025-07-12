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
import android.support.v4.app.INotificationSideChannel
import android.util.Log
import androidx.compose.material3.darkColorScheme
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.worm.MainActivity
import com.example.worm.R
import com.example.worm.ShadeAccessibilityService.ShadeAccessibilityService
import com.example.worm.sw
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.gson.GsonBuilder
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.BuildConfig
import com.google.firebase.ai.LiveGenerativeModel
import com.google.firebase.ai.Chat
import com.google.firebase.ai.type.GenerativeBackend

class RunningService : Service() {


    companion object {
        var jalan: Boolean = false
        const val ACTION_START = "ACTION_START_PROJECTION"
        const val ACTION_STOP = "ACTION_STOP_PROJECTION"
        const val ACTION_SCREEN = "ACTION_SCREENSHOT"

        const val EXTRA_RESULT_CODE = "MEDIA_PROJECTION_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "MEDIA_PROJECTION_RESULT_DATA"

        // Single channel for all notifications
        private const val CHANNEL_ID = "baswara_media_projection"
        private const val CHANNEL_NAME = "Baswara Screen Capture"

        // Generate a unique ID for each notification
        private val idCounter = java.util.concurrent.atomic.AtomicInteger(0)
        fun nextNotifyId(): Int = idCounter.incrementAndGet()

    }
    private var mediaProjection: MediaProjection? = null
    private lateinit var imageReader: ImageReader
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FirebaseApp.initializeApp(this)
        jalan = true
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultData != null) {
                    createNotificationChannel()
                    startMediaProjection(resultCode, resultData)
                } else {
                    stopSelf()
                }
            }
            ACTION_SCREEN -> {
                Log.d("BaswaraService", "ACTION_SCREEN received (SCAN pressed)")
                // Optional: expand shade via AccessibilityService
                ShadeAccessibilityService.instance?.expandShadeViaSwipe()
                Handler(Looper.getMainLooper()).postDelayed({ handleCapture() }, 1000)
            }
            ACTION_STOP -> {
                Log.d("BaswaraService", "ACTION_STOP received")
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startMediaProjection(resultCode: Int, resultData: Intent) {
        // Build pending intents
        val stopPending = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPending = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val screenPending = PendingIntent.getService(
            this, 2,
            Intent(this, RunningService::class.java).apply { action = ACTION_SCREEN },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Foreground notification
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Baswara")
            .setContentText("Cek Hoax via Scan Layar")
            .setSmallIcon(R.drawable.logo2)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addAction(R.drawable.tab, "Stop", stopPending)
            .addAction(R.drawable.home_svgrepo_com, "Buka", openPending)
            .addAction(R.drawable.glass, "Scan", screenPending)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(nextNotifyId(), notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(nextNotifyId(), notif)
        }

        // Initialize MediaProjection & ImageReader
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mgr.getMediaProjection(resultCode, resultData)
        val dm = resources.displayMetrics
        imageReader = ImageReader.newInstance(dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "RunningServiceCapture", dm.widthPixels, dm.heightPixels, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, Handler(Looper.getMainLooper())
        )
    }

    private fun handleCapture() {
        Log.d("BaswaraService", "handleCapture(): creating one-shot VirtualDisplay")
        val dm = resources.displayMetrics

        // Recreate VirtualDisplay for one-shot
        virtualDisplay?.release()
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "OneShotCapture", dm.widthPixels, dm.heightPixels, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, Handler(Looper.getMainLooper())
        )

        // Clear old frames
        while (imageReader.acquireLatestImage() != null) { }

        imageReader.setOnImageAvailableListener({ reader ->
            Log.d("BaswaraService", "ImageAvailable—processing bitmap")
            val img = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val plane = img.planes[0]; val buf = plane.buffer
            val pixelStride = plane.pixelStride; val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * dm.widthPixels
            val bmp = Bitmap.createBitmap(
                dm.widthPixels + rowPadding / pixelStride,
                dm.heightPixels, Bitmap.Config.ARGB_8888
            )
            bmp.copyPixelsFromBuffer(buf)
            img.close()

            // OCR
            val input = InputImage.fromBitmap(bmp, 0)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(input)
                .addOnSuccessListener {
                    Log.d("BaswaraService", "OCR success: ${it.text.take(50)}")
                    Handler(Looper.getMainLooper()).postDelayed({ sendNotification("SALAH, ${it.text}") }, 0)
                }
                .addOnFailureListener {
                    Log.e("BaswaraService", "OCR failed: ${it.message}")
                }

            // Release for next scan
            virtualDisplay?.release()
            virtualDisplay = null
        }, Handler(Looper.getMainLooper()))
    }

    /** Posts notifications with unique IDs so you can spam many */
    private fun sendNotification(content: String) {
        val id = nextNotifyId()
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo2)
            .setContentTitle("Baswara Response")
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_MAX)

            .build()
        NotificationManagerCompat.from(this).notify(10009, notif)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            chan.enableVibration(true)
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(chan)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        jalan = false
    }
}

