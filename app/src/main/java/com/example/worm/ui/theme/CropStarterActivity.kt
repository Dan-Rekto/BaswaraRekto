package com.example.worm.ui.theme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.worm.R
import android.net.Uri
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

class CropStarterActivity : AppCompatActivity() {
    private lateinit var cropImageView: CropImageView
    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent
            val intent = Intent(this, RunningService::class.java).apply {
                action = RunningService.ACTION_CROP_SUCCESS
                data = uri
            }
            startService(intent)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_starter)

        cropImageView = findViewById(R.id.cropImageView)
        val cropbtn = findViewById<Button>(R.id.cropButton)
        val cancelbtn = findViewById<Button>(R.id.cancelButton)

        val sourceUri = intent.data
        if (sourceUri != null) {
            cropImageView.setImageUriAsync(sourceUri)

            // Configure the CropImageView
            cropImageView.guidelines = CropImageView.Guidelines.ON
                cropImageView.setPadding(12, 12, 12, 12)
            cropbtn.setOnClickListener {
                val croppedImage = cropImageView.getCroppedImage()
                if (croppedImage != null) {
                    // Save bitmap to cache file
                    val tempFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(tempFile).use { out ->
                        croppedImage.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }

                    // Get URI from file
                    val uri = Uri.fromFile(tempFile)
                    val intent = Intent(this, RunningService::class.java).apply {
                        action = RunningService.ACTION_CROP_SUCCESS
                        data = uri
                    }
                    startService(intent)
                    finish()
                }
            }

            cancelbtn.setOnClickListener {
                val intent = Intent("ACTION_CANCEL_CAPTURE")
                sendBroadcast(intent)
                finish()
            }

        } else {
            finish()
        }
    }
}