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

private enum class CropAspect(val ratio: Float, val labelRes: Int) {
    WIDGET_2_1(2.0f, R.string.crop_aspect_widget),
    WIDE_16_9(16f / 9f, R.string.crop_aspect_wide),
    SQUARE_1_1(1.0f, R.string.crop_aspect_square)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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

                    // Compute crop box dimensions based on aspect ratio
                    val targetAspect = selectedAspect.ratio
                    val maxBoxWidth = containerWidthPx * 0.95f
                    val maxBoxHeight = containerHeightPx * 0.95f

                    val (cropBoxWidthPx, cropBoxHeightPx) = if (maxBoxWidth / targetAspect <= maxBoxHeight) {
                        maxBoxWidth to (maxBoxWidth / targetAspect)
                    } else {
                        (maxBoxHeight * targetAspect) to maxBoxHeight
                    }

                    val cropBoxWidthDp = with(density) { cropBoxWidthPx.toDp() }
                    val cropBoxHeightDp = with(density) { cropBoxHeightPx.toDp() }

                    // Base scale so image covers the crop box
                    val baseScale = max(
                        cropBoxWidthPx / workingBitmap.width.toFloat(),
                        cropBoxHeightPx / workingBitmap.height.toFloat()
                    )
                    val totalScale = baseScale * userZoom

                    val displayedWidth = workingBitmap.width * totalScale
                    val displayedHeight = workingBitmap.height * totalScale

                    val maxPanX = max(0f, (displayedWidth - cropBoxWidthPx) / 2f)
                    val maxPanY = max(0f, (displayedHeight - cropBoxHeightPx) / 2f)

                    val clampedPanX = panOffset.x.coerceIn(-maxPanX, maxPanX)
                    val clampedPanY = panOffset.y.coerceIn(-maxPanY, maxPanY)

                    // Crop box container
                    Box(
                        modifier = Modifier
                            .size(cropBoxWidthDp, cropBoxHeightDp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, PrimaryCyan, RoundedCornerShape(14.dp))
                            .clipToBounds()
                            .pointerInput(selectedAspect, workingBitmap, userZoom) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    userZoom = (userZoom * zoom).coerceIn(1.0f, 3.5f)
                                    panOffset = Offset(
                                        x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                        y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = workingBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .size(
                                    width = with(density) { displayedWidth.toDp() },
                                    height = with(density) { displayedHeight.toDp() }
                                )
                                .graphicsLayer {
                                    translationX = clampedPanX
                                    translationY = clampedPanY
                                }
                        )

                        // Visual gridlines inside crop frame
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
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

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(
                        Icons.Default.ZoomIn,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Slider(
                        value = userZoom,
                        onValueChange = { userZoom = it },
                        valueRange = 1.0f..3.5f,
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.crop_btn_cancel))
                    }

                    Button(
                        onClick = {
                            // Perform Crop Calculation in workingBitmap coordinate space
                            val targetAspect = selectedAspect.ratio

                            // Derive src rectangle
                            val aspectW: Float
                            val aspectH: Float
                            if (workingBitmap.width.toFloat() / workingBitmap.height.toFloat() > targetAspect) {
                                aspectH = workingBitmap.height.toFloat()
                                aspectW = aspectH * targetAspect
                            } else {
                                aspectW = workingBitmap.width.toFloat()
                                aspectH = aspectW / targetAspect
                            }

                            val cropW = (aspectW / userZoom).coerceIn(1f, workingBitmap.width.toFloat())
                            val cropH = (aspectH / userZoom).coerceIn(1f, workingBitmap.height.toFloat())

                            // Account for normalized pan
                            val maxShiftX = (workingBitmap.width - cropW) / 2f
                            val maxShiftY = (workingBitmap.height - cropH) / 2f

                            val shiftFracX = if (userZoom > 1f) (panOffset.x / 1000f).coerceIn(-1f, 1f) else 0f
                            val shiftFracY = if (userZoom > 1f) (panOffset.y / 1000f).coerceIn(-1f, 1f) else 0f

                            val centerX = workingBitmap.width / 2f - (shiftFracX * maxShiftX)
                            val centerY = workingBitmap.height / 2f - (shiftFracY * maxShiftY)

                            val srcLeft = (centerX - cropW / 2f).coerceIn(0f, workingBitmap.width - cropW).toInt()
                            val srcTop = (centerY - cropH / 2f).coerceIn(0f, workingBitmap.height - cropH).toInt()
                            val finalW = cropW.toInt().coerceAtMost(workingBitmap.width - srcLeft)
                            val finalH = cropH.toInt().coerceAtMost(workingBitmap.height - srcTop)

                            try {
                                val cropped = Bitmap.createBitmap(workingBitmap, srcLeft, srcTop, finalW, finalH)
                                onCropConfirmed(cropped)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                onCropConfirmed(workingBitmap)
                            }
                        },
                        modifier = Modifier.weight(1.4f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = BgDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.crop_btn_apply), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
