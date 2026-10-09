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

    fun loadSourceBitmapForCropping(context: Context, uri: Uri): Bitmap? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            } ?: return null

            if (bytes.isEmpty()) return null

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            // Downsample if huge (> 1600px) to prevent OOM
            var sampleSize = 1
            val maxEditDim = 1600
            while ((origWidth / sampleSize) > (maxEditDim * 1.5) || (origHeight / sampleSize) > (maxEditDim * 1.5)) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

            // Read EXIF orientation
            try {
                val exif = android.media.ExifInterface(java.io.ByteArrayInputStream(bytes))
                val orientation = exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
                val rotationDegrees = when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }

                if (rotationDegrees != 0f) {
                    val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees) }
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (rotated != bitmap) {
                        bitmap.recycle()
                        bitmap = rotated
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveCroppedBitmap(context: Context, croppedBitmap: Bitmap): Boolean {
        return try {
            val maxSide = maxOf(croppedBitmap.width, croppedBitmap.height)
            val finalBitmap = if (maxSide > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / maxSide
                val scaled = Bitmap.createScaledBitmap(
                    croppedBitmap,
                    (croppedBitmap.width * scale).toInt().coerceAtLeast(1),
                    (croppedBitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
                scaled
            } else {
                croppedBitmap
            }

            val destFile = getBackgroundImageFile(context)
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (finalBitmap != croppedBitmap) {
                finalBitmap.recycle()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveWidgetBackgroundImage(context: Context, uri: Uri): Boolean {
        val bitmap = loadSourceBitmapForCropping(context, uri) ?: return false
        val result = saveCroppedBitmap(context, bitmap)
        bitmap.recycle()
        return result
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
