package com.example.worm.ui.theme
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
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
import android.os.Bundle
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.worm.HomeFragment
import com.example.worm.MainActivity
import com.example.worm.MainActivity.LogFragment.Log3
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

import android.content.BroadcastReceiver // Tambahkan import ini
import android.content.IntentFilter // Tambahkan import ini
import android.graphics.Rect // Tambahkan import ini
import android.service.autofill.Validators.or
import com.example.worm.ui.theme.CropStarterActivity // Tambahkan import ini
import androidx.core.content.FileProvider
import com.example.worm.APIkeRunning
import com.example.worm.ModelKeRunning
import kotlinx.coroutines.delay


var OCRTextKeMain = "test"
var answerTextKMain: String = "test"
var aikita = APIkeRunning
var modell = ModelKeRunning

class RunningService : Service() {
    companion object {
        var jalan: Boolean = false
        const val ACTION_START = "ACTION_START_PROJECTION"
        const val ACTION_STOP = "stop_and_home"
        const val ACTION_SCREEN = "ACTION_SCREENSHOT"
        const val ACTION_CROP_SUCCESS = "ACTION_CROP_SUCCESS" // Aksi baru

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
    private var hasGenerated = false
    private lateinit var imageReader: ImageReader
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var notificationCounter = 1
    private var cropReceiver: BroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        cropReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_CROP_SUCCESS) {
                    val croppedUri = intent.data
                    if (croppedUri != null) {
                        processCroppedImage(croppedUri)
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            this,
            cropReceiver,
            IntentFilter(ACTION_CROP_SUCCESS),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }



    override fun onStartCommand(intent: Intent?, flagss: Int, startId: Int): Int {
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
                Log.d("BaswaraService", "ACTION_SCREEN diterima, menunggu 1 detik...")

                // --- TAMBAHAN BARU: JEDA 1 DETIK ---
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d("BaswaraService", "Jeda selesai, memulai proses capture.")
                    handleCaptureAndStartCrop()
                }, 1000) // 1000 milidetik

                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d("BaswaraService", "Dast ist skibiditoilet()")
                    skibiditoilet()
                }, 1000) // 1000 milidetik
            }
            ACTION_CROP_SUCCESS -> {
                Log.d("BaswaraService", "ACTION_CROP_SUCCESS diterima.")
                val croppedUri = intent.data
                if (croppedUri != null) {
                    processCroppedImage(croppedUri)
                }
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

        val scanIntent = Intent(this, RunningService::class.java).apply {
            action = ACTION_SCREEN
        }
        val scanPending = PendingIntent.getService(
            this, 2, scanIntent,
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
            .addAction(R.drawable.glass, "Scan", scanPending)
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

    fun sendOCR(OCR: String) {
        var OCRText = OCR
        OCRTextKeMain = OCR
    }
    fun skibiditoilet(){
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("BaswaraService", "OTW Superman")
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("navigate_to", ACTION_SCREEN)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }.let { frontIntent ->
                PendingIntent.getActivity(
                    this, 42, frontIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ).send()
            }
        }, 3300) // 1000 milidetik

    }

    private fun handleCaptureAndStartCrop() {
        val img = imageReader.acquireLatestImage()
        if (img == null) {
            Log.e("BaswaraService", "Gagal mengambil gambar dari layar.")
            return
        }

        try {
            val plane = img.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val dm = resources.displayMetrics
            val rowPadding = rowStride - pixelStride * dm.widthPixels

            val fullBitmap = Bitmap.createBitmap(
                dm.widthPixels + rowPadding / pixelStride,
                dm.heightPixels,
                Bitmap.Config.ARGB_8888
            )
            fullBitmap.copyPixelsFromBuffer(buffer)

            val scaleFactor = 0.7f
            val scaledWidth = (fullBitmap.width * scaleFactor).toInt()
            val scaledHeight = (fullBitmap.height * scaleFactor).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(fullBitmap, scaledWidth, scaledHeight, true)

            val tempFile = File(cacheDir, "screenshot_for_crop.png")
            FileOutputStream(tempFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val sourceUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                tempFile)

            val cropIntent = Intent(this, CropStarterActivity::class.java).apply {
                data = sourceUri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val cropPendingIntent = PendingIntent.getActivity(
                this, 3, cropIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val cropNotif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screenshot Ready!")
                .setContentText("Tap to crop your screenshot")
                .setSmallIcon(R.drawable.logo2)
                .setContentIntent(cropPendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(this).notify(200009, cropNotif)


            ShadeAccessibilityService.instance?.expandShadeViaSwipe()

            Handler(Looper.getMainLooper()).postDelayed({
                val cropIntent = Intent(this, CropStarterActivity::class.java).apply {
                    data = sourceUri
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                grantUriPermission(
                    "com.canhub.cropper",
                    sourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                startActivity(cropIntent)
            }, 500)

        } catch (e: Exception) {
            Log.e("BaswaraService", "Error saat menangkap & menyimpan screenshot", e)
        } finally {
            img.close()
        }
    }


    private fun processCroppedImage(croppedUri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, croppedUri)

            // Di sini kita gunakan logika OCR dari fungsi handleCapture() lama Anda
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(inputImage)
                .addOnSuccessListener { result ->
                    Log.d("BaswaraService", "OCR success: ${result.text.take(50)}")
                    sendOCR(result.text)
                    waitYa("Memproses Hasil Scan Gambar", "Memproses...")
                    val userText = result.text
                    val prompt = """
      Kamu adalah seorang AI pendeteksi hoax.
      Mulai sekarang, awali kata-katamu dengan kata "FAKTA✅", "HOAKS❌", "TIDAK DIKETAHUI".
      KAMU MERUPAKAN AGEN AI PENDETEKSI HOAX DARI APLIKASI BASWARA.
      TUGASMU HANYA MENDETEKSI HOAX, DAN JUGA MEMBERIKAN PENJELASAN MENGENAI HOAX TERSEBUT. CARILAH INFORMASI DI GOOGLE DAN BERI TAHU KE PENGGUNA MENGAPA INFORMASI INI PALSU ATAUPUN ASLI.
      BERTINGKAHLAH FORMAL DAN PROFESSIONAL. MAKSIMAL 3 KALIMAT TANPA TANDA TANDA SEPERTI "/" "*". 1 KALIMAT YANG TEGAS DAN LUGAS SESUAI BERITA DI GOOGLE, SESUAIKAN DENGAN BERITA YANG TERSEDIA DI GOOGLE, UTAMAKAN KEEBENARAN DIBANDING SUBJEKTIVITAS
      
      Berikut berita untuk dianalisis:
      $userText
    """.trimIndent()

                    generateContentManually(aikita, prompt) { result, err ->
                        val content = err?.localizedMessage ?: result ?: "No response"
                        Log.d("BaswaraService", "AI response: $content")

                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BaswaraService", "OCR failed: ${e.message}")
                    waitYa("Kesalahan karna: ${e.message.toString().take(40)}...", "Terjadi Kesalahan")
                }
        } catch (e: IOException) {
            Log.e("BaswaraService", "Gagal mengubah URI hasil crop menjadi Bitmap", e)
        }
    }

    fun waitYa(textnya: String, isinya: String) {
        // Build & fire the notification
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(isinya)
            .setContentText(textnya)
            .setSmallIcon(R.drawable.logo2)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setTimeoutAfter(2400)
            .build()

        NotificationManagerCompat.from(this)
            .notify(900009, notif)  // Use different notification IDs
    }

    /** Posts notifications with unique IDs and cycles through LOG 1-3 */
    private fun sendNotification(aiResponse: String) {
        // Full-screen intent (for heads-up)
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val fsPending = PendingIntent.getActivity(
            this, 100, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap intent: carries both the navigation command and the AI response
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "GO_TO_LOG1")
        }
        val tapPending = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build & fire the notification
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Baswara Response")
            .setContentText("${aiResponse.take(34)}... Baca Selengkapnya")
            .setSmallIcon(R.drawable.logo2)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(tapPending, true)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(100009, notif)
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
        if (cropReceiver != null) {
            unregisterReceiver(cropReceiver)
        }

        virtualDisplay?.release()
        mediaProjection?.stop()
        jalan = false
    }

    // Fixed data classes for proper JSON parsing
    data class ContentPart(
        @Json(name = "text") val text: String
    )

    data class Content(
        @Json(name = "parts") val parts: List<ContentPart>
    )

    data class ContentItem(
        @Json(name = "parts") val parts: List<ContentPart>
    )

    data class ContentRequest(
        @Json(name = "contents") val contents: List<ContentItem>
    )

    data class ContentCandidate(
        @Json(name = "content") val content: Content
    )

    data class ContentResponse(
        @Json(name = "candidates") val candidates: List<ContentCandidate>
    )

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
            .url("https://generativelanguage.googleapis.com/v1beta/models/$modell:generateContent?key=$apiKey")
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

                try {
                    // Parse using JSONObject (more reliable for this structure)
                    val json = JSONObject(respBody)
                    val candidates = json.getJSONArray("candidates")

                    if (candidates.length() > 0) {
                        val first = candidates.getJSONObject(0)
                        val content = first.getJSONObject("content")
                        val parts = content.getJSONArray("parts")

                        if (parts.length() > 0) {
                            val answerText = parts.getJSONObject(0).getString("text")
                            answerTextKMain = answerText
                            sendNotification(answerText)
                            callback(answerText, null)
                        } else {
                            callback(null, Exception("No parts found in response"))
                        }
                    } else {
                        callback(null, Exception("No candidates found in response"))
                    }
                } catch (e: Exception) {
                    Log.e("BaswaraService", "JSON parsing error", e)
                    callback(null, e)
                }
            }
        })
    }
}
