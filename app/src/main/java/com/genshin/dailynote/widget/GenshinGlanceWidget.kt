package com.genshin.dailynote.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.genshin.dailynote.GenshinApp
import com.genshin.dailynote.R
import com.genshin.dailynote.data.local.ExtrapolationUtils
import com.genshin.dailynote.data.local.ExtrapolationUtils.ExtrapolatedNote
import com.genshin.dailynote.data.repository.UserConfig
import com.genshin.dailynote.ui.MainActivity

import android.graphics.Bitmap
import androidx.glance.layout.ContentScale
import com.genshin.dailynote.data.repository.WidgetAppearance
import com.genshin.dailynote.util.ImageUtils

class GenshinGlanceWidget : GlanceAppWidget() {

    companion object {
        // Breakpoints matching standard launcher grid allocations:
        // 4x1: ~240x60dp
        // 2x2: ~150x110dp
        // 4x2, 3x2: ~240x110dp
        // 4x3, 4x4, 3x3: >= 210dp height
        private val SIZE_COMPACT_4X1  = DpSize(240.dp, 60.dp)
        private val SIZE_NARROW_2X2   = DpSize(150.dp, 110.dp)
        private val SIZE_STANDARD_4X2 = DpSize(240.dp, 110.dp)
        private val SIZE_TALL_4X3     = DpSize(240.dp, 210.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_COMPACT_4X1, SIZE_NARROW_2X2, SIZE_STANDARD_4X2, SIZE_TALL_4X3))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? GenshinApp ?: GenshinApp.instance
        val config = app.preferencesRepository.getUserConfig()
        val note = app.preferencesRepository.getExtrapolatedNote()
        val appearance = app.preferencesRepository.getWidgetAppearance()
        val customBgBitmap = if (appearance.bgType == "IMAGE") {
            ImageUtils.loadWidgetBackgroundBitmap(context, appearance.dimming)
        } else null

        provideContent {
            val size = LocalSize.current
            WidgetContainer(
                config = config,
                note = note,
                size = size,
                appearance = appearance,
                customBgBitmap = customBgBitmap
            )
        }
    }
}

// Color Palette for Dark Theme
private val BgColor = Color(0xFF13161F)
private val CardBg = Color(0xCC1C202E)
private val CardInnerBg = Color(0xCC242A3D)
private val DividerColor = Color(0xFF2F374E)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)
private val TextMuted = Color(0xFF64748B)

private val CyanAccent = Color(0xFF38BDF8)
private val GoldAccent = Color(0xFFFBBF24)
private val GreenAccent = Color(0xFF4ADE80)
private val PurpleAccent = Color(0xFFC084FC)
private val OrangeAccent = Color(0xFFFB923C)

@Composable
private fun WidgetContainer(
    config: UserConfig,
    note: ExtrapolatedNote?,
    size: DpSize,
    appearance: WidgetAppearance,
    customBgBitmap: Bitmap?
) {
    val isTall = size.height >= 210.dp
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(appearance.colorHex))
    } catch (e: Exception) {
        BgColor
    }

    val isImageMode = appearance.bgType == "IMAGE" && customBgBitmap != null

    val containerModifier = if (isImageMode) {
        GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
    } else {
        GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(ColorProvider(parsedColor.copy(alpha = appearance.alpha)))
            .clickable(actionStartActivity<MainActivity>())
    }

    Box(modifier = containerModifier) {
        if (isImageMode && customBgBitmap != null) {
            Image(
                provider = ImageProvider(customBgBitmap),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
        }

        // Widget Content
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = if (isTall) 8.dp else 6.dp)
        ) {
            if (note == null || config.uid.isBlank()) {
                UnconfiguredView()
            } else if (isTall) {
                TallDailyNoteView(config = config, note = note)
            } else if (size.height >= 100.dp) {
                if (size.width < 220.dp) {
                    Narrow2x2DailyNoteView(config = config, note = note)
                } else {
                    Standard4x2DailyNoteView(config = config, note = note)
                }
            } else {
                UltraCompactDailyNoteView(note = note)
            }
        }
    }
}

@Composable
private fun UnconfiguredView() {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(R.drawable.genshin_resin),
            contentDescription = "Genshin Resin",
            modifier = GlanceModifier.size(36.dp)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = context.getString(R.string.widget_not_configured),
            style = TextStyle(
                color = ColorProvider(TextPrimary),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = context.getString(R.string.widget_tap_to_config),
            style = TextStyle(
                color = ColorProvider(CyanAccent),
                fontSize = 11.sp
            )
        )
    }
}

/**
 * Standard View for 4x2, 3x2, and wide 2-row widgets.
 * Height strictly structured into 2 content rows (Resin + 5 Badges) to guarantee ZERO clipping.
 */
@Composable
private fun Standard4x2DailyNoteView(
    config: UserConfig,
    note: ExtrapolatedNote
) {
    val context = LocalContext.current
    val resinRecovery = ExtrapolationUtils.getLocalizedResinRecovery(context, note)
    val syncTime = ExtrapolationUtils.formatSyncTime(context, note.lastSyncTimestamp)

    Column(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // 1. Header (UID, Last Sync, Refresh)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .background(ColorProvider(CardInnerBg))
                    .cornerRadius(4.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "GENSHIN",
                    style = TextStyle(
                        color = ColorProvider(CyanAccent),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            Text(
                text = "UID ${config.uid}",
                style = TextStyle(
                    color = ColorProvider(TextMuted),
                    fontSize = 10.5.sp
                )
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                text = syncTime,
                style = TextStyle(
                    color = ColorProvider(TextSecondary),
                    fontSize = 10.sp
                )
            )

            Spacer(modifier = GlanceModifier.width(6.dp))

            Box(
                modifier = GlanceModifier
                    .size(22.dp)
                    .cornerRadius(11.dp)
                    .background(ColorProvider(CardInnerBg))
                    .clickable(actionRunCallback<RefreshActionCallback>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_refresh),
                    contentDescription = "Refresh",
                    modifier = GlanceModifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // 2. Resin Hero Card (Crescent moon icon, progress bar, countdown)
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(CardBg))
                .cornerRadius(10.dp)
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.genshin_resin),
                    contentDescription = "Resin",
                    modifier = GlanceModifier.size(24.dp)
                )

                Spacer(modifier = GlanceModifier.width(6.dp))

                Text(
                    text = "${note.resin}",
                    style = TextStyle(
                        color = ColorProvider(TextPrimary),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = "/${note.maxResin}",
                    style = TextStyle(
                        color = ColorProvider(TextSecondary),
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                val progress = (note.resin.toFloat() / note.maxResin).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = progress,
                    modifier = GlanceModifier.defaultWeight().height(5.dp).cornerRadius(2.5.dp),
                    color = ColorProvider(CyanAccent),
                    backgroundColor = ColorProvider(DividerColor)
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Text(
                    text = resinRecovery,
                    style = TextStyle(
                        color = ColorProvider(if (note.resin >= note.maxResin) GreenAccent else CyanAccent),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // 3. Single Row with 5 Metric Cards (NEVER cut off)
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Commissions
            StandardMetricCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_commission,
                value = "${note.finishedTasks}/${note.totalTasks}",
                subtext = when {
                    note.isExtraTaskRewardReceived -> context.getString(R.string.commissions_claimed)
                    note.finishedTasks >= note.totalTasks -> context.getString(R.string.commissions_claim_bonus)
                    else -> context.getString(R.string.commissions_title)
                },
                accentColor = when {
                    note.isExtraTaskRewardReceived -> GreenAccent
                    note.finishedTasks >= note.totalTasks -> GoldAccent
                    else -> TextSecondary
                }
            )

            Spacer(modifier = GlanceModifier.width(3.dp))

            // Expeditions
            StandardMetricCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_expedition,
                value = ExtrapolationUtils.getLocalizedExpeditionTime(context, note),
                subtext = context.getString(R.string.expeditions_title),
                accentColor = if (note.completedExpeditions >= note.totalExpeditions && note.totalExpeditions > 0) GreenAccent else TextSecondary
            )

            Spacer(modifier = GlanceModifier.width(3.dp))

            // Realm Currency
            StandardMetricCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_realm,
                value = "${note.currentHomeCoin}",
                subtext = context.getString(R.string.realm_coin_title),
                accentColor = if (note.currentHomeCoin >= note.maxHomeCoin) GoldAccent else TextSecondary
            )

            Spacer(modifier = GlanceModifier.width(3.dp))

            // Trounce Domain Boss Discount
            StandardMetricCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_domain,
                value = "${note.bossDiscountRemaining}/${note.bossDiscountLimit}",
                subtext = context.getString(R.string.boss_discount_title),
                accentColor = if (note.bossDiscountRemaining > 0) OrangeAccent else TextMuted
            )

            Spacer(modifier = GlanceModifier.width(3.dp))

            // Parametric Transformer
            StandardMetricCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_transformer,
                value = if (note.transformerReady) context.getString(R.string.transformer_ready) else ExtrapolationUtils.getLocalizedTransformerStatus(context, note),
                subtext = context.getString(R.string.transformer_title),
                accentColor = if (note.transformerReady) PurpleAccent else TextSecondary
            )
        }
    }
}

@Composable
private fun StandardMetricCard(
    modifier: GlanceModifier,
    iconRes: Int,
    value: String,
    subtext: String,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(ColorProvider(CardBg))
            .cornerRadius(8.dp)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                modifier = GlanceModifier.size(19.dp)
            )

            Spacer(modifier = GlanceModifier.height(2.dp))

            Text(
                text = value,
                style = TextStyle(
                    color = ColorProvider(TextPrimary),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )

            Text(
                text = subtext,
                style = TextStyle(
                    color = ColorProvider(accentColor),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * 2x2 Layout for square 2-column, 2-row widgets.
 */
@Composable
private fun Narrow2x2DailyNoteView(
    config: UserConfig,
    note: ExtrapolatedNote
) {
    val context = LocalContext.current
    val resinRecovery = ExtrapolationUtils.getLocalizedResinRecovery(context, note)

    Column(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UID ${config.uid}",
                style = TextStyle(color = ColorProvider(TextMuted), fontSize = 10.sp)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .size(20.dp)
                    .cornerRadius(10.dp)
                    .background(ColorProvider(CardInnerBg))
                    .clickable(actionRunCallback<RefreshActionCallback>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_refresh),
                    contentDescription = "Refresh",
                    modifier = GlanceModifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(3.dp))

        // Resin Box
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(CardBg))
                .cornerRadius(8.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.genshin_resin),
                    contentDescription = "Resin",
                    modifier = GlanceModifier.size(20.dp)
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "${note.resin}",
                    style = TextStyle(color = ColorProvider(TextPrimary), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "/${note.maxResin}",
                    style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 10.5.sp)
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = resinRecovery,
                    style = TextStyle(color = ColorProvider(CyanAccent), fontSize = 9.sp),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(3.dp))

        // 2x2 Grid of Badges
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            StandardMetricCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_commission,
                value = "${note.finishedTasks}/${note.totalTasks}",
                subtext = context.getString(R.string.commissions_title),
                accentColor = if (note.finishedTasks >= note.totalTasks) GoldAccent else TextSecondary
            )
            Spacer(modifier = GlanceModifier.width(3.dp))
            StandardMetricCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_realm,
                value = "${note.currentHomeCoin}",
                subtext = context.getString(R.string.realm_coin_title),
                accentColor = if (note.currentHomeCoin >= note.maxHomeCoin) GoldAccent else TextSecondary
            )
        }

        Spacer(modifier = GlanceModifier.height(3.dp))

        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            StandardMetricCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_expedition,
                value = ExtrapolationUtils.getLocalizedExpeditionTime(context, note),
                subtext = context.getString(R.string.expeditions_title),
                accentColor = if (note.completedExpeditions >= note.totalExpeditions && note.totalExpeditions > 0) GreenAccent else TextSecondary
            )
            Spacer(modifier = GlanceModifier.width(3.dp))
            StandardMetricCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_transformer,
                value = if (note.transformerReady) context.getString(R.string.transformer_ready) else ExtrapolationUtils.getLocalizedTransformerStatus(context, note),
                subtext = context.getString(R.string.transformer_title),
                accentColor = if (note.transformerReady) PurpleAccent else TextSecondary
            )
        }
    }
}

/**
 * Ultra-compact single-row view for 4x1 or 2x1 widgets (height < 100dp).
 */
@Composable
private fun UltraCompactDailyNoteView(
    note: ExtrapolatedNote
) {
    val context = LocalContext.current
    val resinRecovery = ExtrapolationUtils.getLocalizedResinRecovery(context, note)

    Column(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // Line 1: Header + Resin count
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.genshin_resin),
                contentDescription = "Resin",
                modifier = GlanceModifier.size(16.dp)
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = "${note.resin}/${note.maxResin}",
                style = TextStyle(color = ColorProvider(TextPrimary), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))

            val progress = (note.resin.toFloat() / note.maxResin).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = progress,
                modifier = GlanceModifier.defaultWeight().height(3.dp).cornerRadius(1.5.dp),
                color = ColorProvider(CyanAccent),
                backgroundColor = ColorProvider(DividerColor)
            )

            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = resinRecovery,
                style = TextStyle(color = ColorProvider(CyanAccent), fontSize = 9.5.sp),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(2.dp))

        // Line 2: 5 ultra compact badges
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UltraCompactBadge(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_commission,
                value = "${note.finishedTasks}/${note.totalTasks}",
                accentColor = if (note.finishedTasks >= note.totalTasks) GoldAccent else TextSecondary
            )
            Spacer(modifier = GlanceModifier.width(2.dp))
            UltraCompactBadge(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_expedition,
                value = "${note.completedExpeditions}/${note.totalExpeditions}",
                accentColor = CyanAccent
            )
            Spacer(modifier = GlanceModifier.width(2.dp))
            UltraCompactBadge(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_realm,
                value = "${note.currentHomeCoin}",
                accentColor = if (note.currentHomeCoin >= note.maxHomeCoin) GoldAccent else TextSecondary
            )
            Spacer(modifier = GlanceModifier.width(2.dp))
            UltraCompactBadge(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_domain,
                value = "${note.bossDiscountRemaining}",
                accentColor = if (note.bossDiscountRemaining > 0) OrangeAccent else TextMuted
            )
            Spacer(modifier = GlanceModifier.width(2.dp))
            UltraCompactBadge(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_transformer,
                value = if (note.transformerReady) "OK" else ExtrapolationUtils.getLocalizedTransformerStatus(context, note),
                accentColor = if (note.transformerReady) PurpleAccent else TextSecondary
            )
        }
    }
}

@Composable
private fun UltraCompactBadge(
    modifier: GlanceModifier,
    iconRes: Int,
    value: String,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(ColorProvider(CardBg))
            .cornerRadius(4.dp)
            .padding(horizontal = 1.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                modifier = GlanceModifier.size(11.dp)
            )
            Spacer(modifier = GlanceModifier.width(1.dp))
            Text(
                text = value,
                style = TextStyle(color = ColorProvider(accentColor), fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
    }
}

/**
 * Expanded view strictly for 4x3 or 4x4 (height >= 210dp).
 * Distributes vertical space across 3 distinct card rows evenly.
 */
@Composable
private fun TallDailyNoteView(
    config: UserConfig,
    note: ExtrapolatedNote
) {
    val context = LocalContext.current
    val resinRecovery = ExtrapolationUtils.getLocalizedResinRecovery(context, note)
    val syncTime = ExtrapolationUtils.formatSyncTime(context, note.lastSyncTimestamp)

    Column(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .background(ColorProvider(CardInnerBg))
                    .cornerRadius(6.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "GENSHIN",
                    style = TextStyle(
                        color = ColorProvider(CyanAccent),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            Text(
                text = "UID ${config.uid}",
                style = TextStyle(
                    color = ColorProvider(TextMuted),
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                text = syncTime,
                style = TextStyle(
                    color = ColorProvider(TextSecondary),
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = GlanceModifier.width(6.dp))

            Box(
                modifier = GlanceModifier
                    .size(24.dp)
                    .cornerRadius(12.dp)
                    .background(ColorProvider(CardInnerBg))
                    .clickable(actionRunCallback<RefreshActionCallback>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_refresh),
                    contentDescription = "Refresh",
                    modifier = GlanceModifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Resin Hero Box
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(CardBg))
                .cornerRadius(12.dp)
                .padding(8.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.genshin_resin),
                        contentDescription = "Resin",
                        modifier = GlanceModifier.size(20.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = context.getString(R.string.resin_title),
                        style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = resinRecovery,
                        style = TextStyle(
                            color = ColorProvider(if (note.resin >= note.maxResin) GreenAccent else CyanAccent),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${note.resin}",
                        style = TextStyle(color = ColorProvider(TextPrimary), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = " / ${note.maxResin}",
                        style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 13.sp)
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                val progress = (note.resin.toFloat() / note.maxResin).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = progress,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
                    color = ColorProvider(CyanAccent),
                    backgroundColor = ColorProvider(DividerColor)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Row 1 of badges: Commissions & Expeditions
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            TallStatusCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_commission,
                label = context.getString(R.string.commissions_title),
                value = "${note.finishedTasks}/${note.totalTasks}",
                subtext = when {
                    note.isExtraTaskRewardReceived -> context.getString(R.string.commissions_claimed)
                    note.finishedTasks >= note.totalTasks -> context.getString(R.string.commissions_claim_bonus)
                    else -> context.getString(R.string.commissions_pending)
                },
                accentColor = when {
                    note.isExtraTaskRewardReceived -> GreenAccent
                    note.finishedTasks >= note.totalTasks -> GoldAccent
                    else -> TextSecondary
                }
            )

            Spacer(modifier = GlanceModifier.width(6.dp))

            TallStatusCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_expedition,
                label = context.getString(R.string.expeditions_title),
                value = ExtrapolationUtils.getLocalizedExpeditionTime(context, note),
                subtext = "${note.completedExpeditions}/${note.totalExpeditions}",
                accentColor = if (note.completedExpeditions >= note.totalExpeditions && note.totalExpeditions > 0) GreenAccent else CyanAccent
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Row 2 of badges: Realm Coin, Boss Discount, Transformer
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            TallStatusCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_realm,
                label = context.getString(R.string.realm_coin_title),
                value = "${note.currentHomeCoin}",
                subtext = "/${note.maxHomeCoin}",
                accentColor = if (note.currentHomeCoin >= note.maxHomeCoin) GoldAccent else TextSecondary
            )

            Spacer(modifier = GlanceModifier.width(6.dp))

            TallStatusCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_domain,
                label = context.getString(R.string.boss_discount_title),
                value = "${note.bossDiscountRemaining}/${note.bossDiscountLimit}",
                subtext = context.getString(R.string.boss_discount_sub),
                accentColor = if (note.bossDiscountRemaining > 0) OrangeAccent else TextMuted
            )

            Spacer(modifier = GlanceModifier.width(6.dp))

            TallStatusCard(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = R.drawable.genshin_transformer,
                label = context.getString(R.string.transformer_title),
                value = if (note.transformerReady) context.getString(R.string.transformer_ready) else ExtrapolationUtils.getLocalizedTransformerStatus(context, note),
                subtext = if (note.transformerReady) context.getString(R.string.transformer_ready) else context.getString(R.string.transformer_cooldown),
                accentColor = if (note.transformerReady) PurpleAccent else TextSecondary
            )
        }
    }
}

@Composable
private fun TallStatusCard(
    modifier: GlanceModifier,
    iconRes: Int,
    label: String,
    value: String,
    subtext: String,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(ColorProvider(CardBg))
            .cornerRadius(10.dp)
            .padding(6.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = label,
                    modifier = GlanceModifier.size(16.dp)
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = label,
                    style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 10.sp),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                text = value,
                style = TextStyle(color = ColorProvider(TextPrimary), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )

            Text(
                text = subtext,
                style = TextStyle(color = ColorProvider(accentColor), fontSize = 9.sp),
                maxLines = 1
            )
        }
    }
}
