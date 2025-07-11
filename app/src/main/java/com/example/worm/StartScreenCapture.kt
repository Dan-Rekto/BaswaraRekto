package com.example.screencapture

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log

class ScreenCaptureService : Service() {

    val metrics = resources.displayMetrics
    val density = metrics.densityDpi
    val width = metrics.widthPixels
    val height = metrics.heightPixels

    // Create an ImageReader to capture the screen content.
    val imageReader1 = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private var currentImageReader: ImageReader? = null // Store the ImageReader

    fun initializeImageReader(imageReader: ImageReader) {
        this.currentImageReader = imageReader
        Log.d("ScreenCaptureService", "ImageReader initialized.")
    }

    fun getLatestImageReader(): ImageReader? {
        return currentImageReader
    }

    override fun onCreate() {
        super.onCreate()
        // Any other initialization for your application
    }

    // Call this method after obtaining permission from the user in an Activity.
    fun setMediaProjection(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        startCapture()
    }

    private fun startCapture() {
        // Get screen dimensions
        val metrics = resources.displayMetrics
        val density = metrics.densityDpi
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        // Create an ImageReader to capture the screen content.
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        // Create a VirtualDisplay which outputs to the ImageReader surface.
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            Handler()
        )

        // Set an OnImageAvailableListener on the ImageReader to receive frames.
        imageReader?.setOnImageAvailableListener({ reader ->
            // When an image is available, process it here. For example:
            val image = reader.acquireLatestImage()
            if (image != null) {
                // Process and convert to Bitmap or save directly.
                // Remember to close the image after you're done.
                image.close()
            }
        }, Handler())
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        super.onDestroy()
    }
}