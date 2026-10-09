package com.genshin.dailynote.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.genshin.dailynote.R
import com.genshin.dailynote.ui.theme.BgDark
import com.genshin.dailynote.ui.theme.BorderDark
import com.genshin.dailynote.ui.theme.CardDark
import com.genshin.dailynote.ui.theme.CardDarkElevated
import com.genshin.dailynote.ui.theme.PrimaryCyan
import com.genshin.dailynote.ui.theme.TextPrimary
import com.genshin.dailynote.ui.theme.TextSecondary
import kotlin.math.max
import kotlin.math.roundToInt

private enum class CropAspect(val ratio: Float, val labelRes: Int) {
    WIDGET_2_1(2.0f, R.string.crop_aspect_widget),
    WIDE_16_9(16f / 9f, R.string.crop_aspect_wide),
    SQUARE_1_1(1.0f, R.string.crop_aspect_square),
    ORIGINAL(-1f, R.string.crop_aspect_original)
}

@Composable
fun ImageCropDialog(
    initialBitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropConfirmed: (Bitmap) -> Unit
) {
    var workingBitmap by remember { mutableStateOf(initialBitmap) }
    var selectedAspect by remember { mutableStateOf(CropAspect.WIDGET_2_1) }
    var userZoom by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var displayedCropBoxWidthPx by remember { mutableFloatStateOf(1f) }
    var displayedCropBoxHeightPx by remember { mutableFloatStateOf(1f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = CardDark,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.crop_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Aspect Ratio Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CropAspect.values().forEach { aspect ->
                        val isSelected = selectedAspect == aspect
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedAspect = aspect
                                userZoom = 1.0f
                                panOffset = Offset.Zero
                            },
                            label = {
                                Text(
                                    text = stringResource(aspect.labelRes),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryCyan,
                                selectedLabelColor = BgDark,
                                containerColor = CardDarkElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                // Interactive Crop Viewport
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0B0D13)),
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val containerWidthPx = with(density) { maxWidth.toPx() }
                    val containerHeightPx = with(density) { maxHeight.toPx() }

                    val bmpWidth = workingBitmap.width.toFloat()
                    val bmpHeight = workingBitmap.height.toFloat().coerceAtLeast(1f)

                    // Compute crop box aspect ratio
                    val targetAspect = if (selectedAspect == CropAspect.ORIGINAL) {
                        bmpWidth / bmpHeight
                    } else {
                        selectedAspect.ratio
                    }

                    val maxBoxWidth = containerWidthPx * 0.95f
                    val maxBoxHeight = containerHeightPx * 0.95f

                    val (cropBoxWidthPx, cropBoxHeightPx) = if (maxBoxWidth / targetAspect <= maxBoxHeight) {
                        maxBoxWidth to (maxBoxWidth / targetAspect)
                    } else {
                        (maxBoxHeight * targetAspect) to maxBoxHeight
                    }

                    displayedCropBoxWidthPx = cropBoxWidthPx
                    displayedCropBoxHeightPx = cropBoxHeightPx

                    val cropBoxWidthDp = with(density) { cropBoxWidthPx.toDp() }
                    val cropBoxHeightDp = with(density) { cropBoxHeightPx.toDp() }

                    // Crop box container
                    Box(
                        modifier = Modifier
                            .size(cropBoxWidthDp, cropBoxHeightDp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, PrimaryCyan, RoundedCornerShape(14.dp))
                            .clipToBounds()
                            .pointerInput(workingBitmap, selectedAspect) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    userZoom = (userZoom * zoom).coerceIn(1.0f, 3.5f)
                                    panOffset = Offset(
                                        x = panOffset.x + pan.x,
                                        y = panOffset.y + pan.y
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val boxW = size.width
                            val boxH = size.height

                            val bWidth = workingBitmap.width.toFloat()
                            val bHeight = workingBitmap.height.toFloat().coerceAtLeast(1f)

                            // Base scale so image fills the larger needed dimension (never squished, uniform scale)
                            val bScale = max(boxW / bWidth, boxH / bHeight)
                            val tScale = bScale * userZoom

                            val sW = bWidth * tScale
                            val sH = bHeight * tScale

                            val mPanX = max(0f, (sW - boxW) / 2f)
                            val mPanY = max(0f, (sH - boxH) / 2f)

                            val cPanX = panOffset.x.coerceIn(-mPanX, mPanX)
                            val cPanY = panOffset.y.coerceIn(-mPanY, mPanY)

                            val dstX = (boxW - sW) / 2f + cPanX
                            val dstY = (boxH - sH) / 2f + cPanY

                            drawImage(
                                image = workingBitmap.asImageBitmap(),
                                dstOffset = androidx.compose.ui.unit.IntOffset(
                                    dstX.roundToInt(),
                                    dstY.roundToInt()
                                ),
                                dstSize = androidx.compose.ui.unit.IntSize(
                                    sW.roundToInt(),
                                    sH.roundToInt()
                                )
                            )
                        }

                        // Subtle boundary guide
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        )
                    }
                }

                // Controls Row: Rotate 90 & Zoom Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val matrix = Matrix().apply { postRotate(90f) }
                            val rotated = Bitmap.createBitmap(
                                workingBitmap, 0, 0,
                                workingBitmap.width, workingBitmap.height,
                                matrix, true
                            )
                            workingBitmap = rotated
                            panOffset = Offset.Zero
                            userZoom = 1.0f
                        },
                        modifier = Modifier
                            .background(CardDarkElevated, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = stringResource(R.string.crop_btn_rotate),
                            tint = PrimaryCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        Icons.Default.ZoomIn,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = java.lang.String.format(java.util.Locale.US, "%.1fx", userZoom),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp)
                    )
                    Slider(
                        value = userZoom,
                        onValueChange = { userZoom = it },
                        valueRange = 1.0f..3.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryCyan,
                            activeTrackColor = PrimaryCyan,
                            inactiveTrackColor = BorderDark
                        )
                    )
                }

                Text(
                    text = stringResource(R.string.crop_hint),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.crop_btn_cancel), fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onCropConfirmed(workingBitmap)
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.crop_btn_use_original), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val boxW = displayedCropBoxWidthPx.coerceAtLeast(1f)
                            val boxH = displayedCropBoxHeightPx.coerceAtLeast(1f)

                            val bWidth = workingBitmap.width.toFloat()
                            val bHeight = workingBitmap.height.toFloat().coerceAtLeast(1f)

                            // Same uniform scale formula as the Canvas renderer
                            val bScale = max(boxW / bWidth, boxH / bHeight)
                            val tScale = bScale * userZoom

                            val sW = bWidth * tScale
                            val sH = bHeight * tScale

                            val mPanX = max(0f, (sW - boxW) / 2f)
                            val mPanY = max(0f, (sH - boxH) / 2f)

                            val cPanX = panOffset.x.coerceIn(-mPanX, mPanX)
                            val cPanY = panOffset.y.coerceIn(-mPanY, mPanY)

                            // Image top-left relative to crop box top-left
                            val dstX = (boxW - sW) / 2f + cPanX
                            val dstY = (boxH - sH) / 2f + cPanY

                            // Map crop box viewport (0, 0, boxW, boxH) back into bitmap pixel space
                            val cropLeft = ((-dstX) / tScale).coerceIn(0f, bWidth - 1f)
                            val cropTop = ((-dstY) / tScale).coerceIn(0f, bHeight - 1f)
                            val cropWidth = (boxW / tScale).coerceIn(1f, bWidth - cropLeft)
                            val cropHeight = (boxH / tScale).coerceIn(1f, bHeight - cropTop)

                            val left = cropLeft.toInt().coerceIn(0, workingBitmap.width - 1)
                            val top = cropTop.toInt().coerceIn(0, workingBitmap.height - 1)
                            val width = cropWidth.toInt().coerceIn(1, workingBitmap.width - left)
                            val height = cropHeight.toInt().coerceIn(1, workingBitmap.height - top)

                            try {
                                val cropped = Bitmap.createBitmap(workingBitmap, left, top, width, height)
                                onCropConfirmed(cropped)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                onCropConfirmed(workingBitmap)
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = BgDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.crop_btn_apply), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
