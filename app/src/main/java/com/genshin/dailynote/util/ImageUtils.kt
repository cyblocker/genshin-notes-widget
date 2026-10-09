package com.genshin.dailynote.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    private const val BG_IMAGE_FILENAME = "custom_widget_bg.jpg"
    private const val MAX_DIMENSION = 1024

    fun getBackgroundImageFile(context: Context): File {
        return File(context.filesDir, BG_IMAGE_FILENAME)
    }

    fun hasBackgroundImage(context: Context): Boolean {
        val file = getBackgroundImageFile(context)
        return file.exists() && file.length() > 0
    }

    fun saveWidgetBackgroundImage(context: Context, uri: Uri): Boolean {
        return try {
            // 1. Measure dimensions without full memory allocation
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return false

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return false

            // 2. Compute inSampleSize power of 2
            var sampleSize = 1
            while ((origWidth / sampleSize) > (MAX_DIMENSION * 1.5) || (origHeight / sampleSize) > (MAX_DIMENSION * 1.5)) {
                sampleSize *= 2
            }

            // 3. Decode sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return false

            // 4. Exact scale if still exceeding MAX_DIMENSION to preserve Binder limit
            val maxSide = maxOf(sampledBitmap.width, sampledBitmap.height)
            val finalBitmap = if (maxSide > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / maxSide
                val scaled = Bitmap.createScaledBitmap(
                    sampledBitmap,
                    (sampledBitmap.width * scale).toInt(),
                    (sampledBitmap.height * scale).toInt(),
                    true
                )
                if (scaled != sampledBitmap) {
                    sampledBitmap.recycle()
                }
                scaled
            } else {
                sampledBitmap
            }

            // 5. Compress into private app internal storage
            val destFile = getBackgroundImageFile(context)
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            finalBitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadWidgetBackgroundBitmap(context: Context): Bitmap? {
        val file = getBackgroundImageFile(context)
        if (!file.exists() || file.length() <= 0) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteBackgroundImage(context: Context): Boolean {
        val file = getBackgroundImageFile(context)
        return if (file.exists()) file.delete() else true
    }
}
