package com.example.whatsapp.utils.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.yalantis.ucrop.UCrop
import java.io.File

object ImageCropperHelper {

    private const val CROP_IMAGE_REQUEST_CODE = UCrop.REQUEST_CROP

    fun startCrop(activity: Activity, sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(activity.cacheDir, "cropped_image.jpg"))

        val options = UCrop.Options().apply {
            setCircleDimmedLayer(true) // Oval kırpma
            setCompressionQuality(80)
            setShowCropGrid(false)
            setHideBottomControls(true)
        }

        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(512, 512)
            .withOptions(options)
            .start(activity) // 🔥 Fragment yerine Activity kullan!
    }


    fun handleCropResult(requestCode: Int, resultCode: Int, data: Intent?): Uri? {
        return try {
            if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
                UCrop.getOutput(data!!)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ImageCropperHelper", "Error getting cropped image URI", e)
            null
        }
    }

}
