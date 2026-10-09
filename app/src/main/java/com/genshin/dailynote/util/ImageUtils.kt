package com.genshin.dailynote.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    private const val BG_IMAGE_FILENAME = "custom_widget_bg.jpg"
    // Keep max dimension <= 480 to strictly guarantee total bitmap size in RAM is < 300 KB,
    // avoiding Android RemoteViews TransactionTooLargeException (1 MB Binder limit).
    private const val MAX_DIMENSION = 480

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

            // 3. Decode sampled bitmap in RGB_565 (2 bytes per pixel)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return false

            // 4. Exact scale if still exceeding MAX_DIMENSION
            val maxSide = maxOf(sampledBitmap.width, sampledBitmap.height)
            val finalBitmap = if (maxSide > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / maxSide
                val scaled = Bitmap.createScaledBitmap(
                    sampledBitmap,
                    (sampledBitmap.width * scale).toInt().coerceAtLeast(1),
                    (sampledBitmap.height * scale).toInt().coerceAtLeast(1),
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

    fun loadWidgetBackgroundBitmap(context: Context, dimming: Float = 0f): Bitmap? {
        val file = getBackgroundImageFile(context)
        if (!file.exists() || file.length() <= 0) return null
        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
                inMutable = true
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
            val workingBitmap = if (decoded.isMutable) {
                decoded
            } else {
                val copy = decoded.copy(Bitmap.Config.RGB_565, true)
                decoded.recycle()
                copy ?: return null
            }

            // Pre-apply dimming directly onto bitmap to avoid extra Glance RemoteViews overlays
            if (dimming > 0.05f) {
                val canvas = Canvas(workingBitmap)
                val paint = Paint().apply {
                    color = Color.BLACK
                    alpha = (dimming * 255).toInt().coerceIn(0, 255)
                }
                canvas.drawRect(0f, 0f, workingBitmap.width.toFloat(), workingBitmap.height.toFloat(), paint)
            }

            workingBitmap
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
