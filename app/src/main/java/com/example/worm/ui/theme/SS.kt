package com.example.worm.ui.theme

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// ScreenshotProxyActivity.kt
class ScreenshotProxyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Panggil service untuk screenshot
        Intent(this, RunningService::class.java).apply {
            action = RunningService.ACTION_SCREEN
        }.also { startService(it) }
        // Selesai, keluarkan activity ini segera
        finish()
    }
}
