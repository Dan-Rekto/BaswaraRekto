package  com.example.worm.ui.theme

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.service.autofill.Validators.or
import android.support.v4.app.INotificationSideChannel
import android.util.Log
import androidx.compose.material3.darkColorScheme
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.worm.MainActivity
import com.example.worm.R
import com.example.worm.ShadeAccessibilityService.ShadeAccessibilityService
import com.example.worm.sw
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONObject
import java.io.IOException


const val aikita = "AIzaSyC8wJ_8GNj33Xp-pGC6vD6S0JlYB5eg07Y"
class RunningService : Service() {
    companion object {
        var jalan: Boolean = false
        const val ACTION_START = "ACTION_START_PROJECTION"
        const val ACTION_STOP = "stop_and_home"
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
        // 1) STOP → MainActivity with your navigate_to extra
        val stopIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", ACTION_STOP)  // "stop_and_home"
        }
        val stopPending = PendingIntent.getActivity(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2) BUKA → simply bring your existing MainActivity to front
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 1, openIntent,
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

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d("RunningService", "MediaProjection stopped; releasing display")
                virtualDisplay?.release()
                virtualDisplay = null
            }
        }, Handler(Looper.getMainLooper()))

        val dm = resources.displayMetrics
        imageReader = ImageReader.newInstance(dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 2)

        // Create VirtualDisplay ONCE and keep it
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "RunningServiceCapture", dm.widthPixels, dm.heightPixels, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, Handler(Looper.getMainLooper())
        )
    }

    private fun handleCapture() {
        Log.d("BaswaraService", "handleCapture(): using existing VirtualDisplay")

        // Don't recreate VirtualDisplay - just use the existing one
        if (virtualDisplay == null) {
            Log.e("BaswaraService", "VirtualDisplay is null, cannot capture")
            return
        }

        // Clear old frames
        while (true) {
            val old = imageReader.acquireLatestImage()
                ?: break
            old.close()
        }



        // 3) Listen for the next available image
        imageReader.setOnImageAvailableListener({ reader ->
            // Acquire the new image
            val img = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            try {
                // Convert to bitmap
                val plane = img.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val dm = resources.displayMetrics
                val rowPadding = rowStride - pixelStride * dm.widthPixels

                val bitmap = Bitmap.createBitmap(
                    dm.widthPixels + rowPadding / pixelStride,
                    dm.heightPixels,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                // OCR processing
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(inputImage)
                    .addOnSuccessListener { result ->
                        Log.d("BaswaraService", "OCR success: ${result.text.take(50)}")

                        val userText = result.text
                        val prompt = """
      Kamu adalah seorang AI pendeteksi hoax.
      Mulai sekarang, awali kata-katamu dengan kata "YA" ATAU "TIDAK".
      KAMU MERUPAKAN AGEN AI PENDETEKSI HOAX DARI APLIKASI BASWARA.
      TUGASMU HANYA MENDETEKSI HOAX, DAN JUGA MEMBERIKAN PENJELASAN MENGENAI HOAX TERSEBUT.
      BERTINGKAHLAH FORMAL DAN PROFESSIONAL. MAKSIMAL 1 KALIMAT TANPA TANDA TANDA SEPERTI "/" "*". 1 KALIMAT YANG TEGAS DAN LUGAS SESUAI BERITA DI GOOGLE, SESUAIKAN DENGAN BERITA YANG TERSEDIA DI GOOGLE
      
      Berikut berita untuk dianalisis:
      $userText
    """.trimIndent()

                        generateContentManually(aikita, prompt) { result, err ->
                            val content = err?.localizedMessage ?: result ?: "No response"
                            Log.d("BaswaraService", "OCR response: $content")

                        }

                    }
                    .addOnFailureListener { e ->
                        Log.e("BaswaraService", "OCR failed: ${e.message}")
                    }

            } finally {
                // 4) Always close the image when done
                img.close()
                // 5) Stop listening to avoid repeated calls
                reader.setOnImageAvailableListener(null, null)
            }
        }, Handler(Looper.getMainLooper()))

        // 0) If we've lost the display, rebuild it
        if (virtualDisplay == null) {
            mediaProjection?.let { mp ->
                val dm = resources.displayMetrics
                virtualDisplay = mp.createVirtualDisplay(
                    "RunningServiceCapture",
                    dm.widthPixels, dm.heightPixels, dm.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface, null, Handler(Looper.getMainLooper())
                )
                Log.d("BaswaraService", "Re-created VirtualDisplay for capture")
            } ?: run {
                Log.e("BaswaraService", "MediaProjection gone, cannot recapture")
                return
            }
        }
    }

    /** Posts notifications with unique IDs so you can spam many */
    private fun sendNotification(content: String) {
        val id = nextNotifyId()
        // 1. Build an Intent to your Activity
        val fsIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
// 2. Wrap it
        val fsPending = PendingIntent.getActivity(
            this, 0, fsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

// 3. In your builder, add:
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Baswara Response")
            .setContentText(content)
            .setSmallIcon(R.drawable.logo2)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fsPending, true)   // ← force heads-up
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
        virtualDisplay?.release()
        mediaProjection?.stop()
        jalan = false
    }
    data class ContentPart(
        @Json(name = "text") val text: String
    )
    data class ContentItem(
        @Json(name = "parts") val parts: List<ContentPart>
    )
    data class ContentRequest(
        @Json(name = "contents") val contents: List<ContentItem>
    )

    data class ContentCandidate(
        @Json(name = "output") val output: String
    )
    data class ContentResponse(
        @Json(name = "candidates") val candidates: List<ContentCandidate>
    )


    // --- Then your function becomes: ---
    fun generateContentManually(
        apiKey: String,
        userPrompt: String,
        callback: (String?, Exception?) -> Unit
    ) {
        val moshi = Moshi.Builder().build()

        // Build request
        val reqObj = ContentRequest(
            contents = listOf(ContentItem(parts = listOf(ContentPart(userPrompt))))
        )
        val json = moshi.adapter(ContentRequest::class.java).toJson(reqObj)
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        // HTTP call
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("BaswaraService", "HTTP failure", e)
                callback(null, e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val respBody = response.body?.string().orEmpty()
                Log.d("BaswaraService", "HTTP ${response.code}: $respBody")

                if (!response.isSuccessful) {
                    callback(null, Exception("HTTP ${response.code}: ${response.message}"))
                    return
                }

                // Parse and return
                val respObj = moshi.adapter(ContentResponse::class.java).fromJson(respBody)
                val answer = respObj?.candidates?.firstOrNull()?.output
                callback(answer, null)

                val json = JSONObject(respBody)
                val candidates = json
                    .getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val first = candidates.getJSONObject(0)
                    val content = first.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        val answerText = parts.getJSONObject(0).getString("text")
                        // now send just that:
                        sendNotification(answerText)
                    }
                }
            }
        })
    }
}
