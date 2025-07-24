package com.example.worm.ui.theme

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.worm.R

class CropStarterActivity : AppCompatActivity() {

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

        val sourceUri = intent.data
        if (sourceUri != null) {
            val cropOptions = CropImageContractOptions(
                uri = sourceUri,
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    initialCropWindowPaddingRatio = 0.4f
                )
            )
            cropImage.launch(cropOptions)
        } else {
            finish()
        }
    }
}