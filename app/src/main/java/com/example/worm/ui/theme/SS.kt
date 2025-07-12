package com.example.worm.ShadeAccessibilityService

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.app.Notification
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.example.worm.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationManagerCompat

class ShadeAccessibilityService : AccessibilityService() {
    companion object {
        var instance: ShadeAccessibilityService? = null
    }

    override fun onServiceConnected() {
        instance = this
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    /** Swipe down from top center to mid‐screen */
    fun expandShadeViaSwipe() {
        // Build a Path: start at (50% width, 0px) → (50% width, 40% height)
        val dm = resources.displayMetrics
        val startX = dm.widthPixels / 2f
        val startY = 0f
        val endY   = dm.heightPixels * 0.4f

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Log.d("ShadeService", "Swipe down completed")
            }
            override fun onCancelled(gestureDescription: GestureDescription) {
                Log.e("ShadeService", "Swipe down cancelled")
            }
        }, null)
    }
}
