package com.genshin.dailynote.data.local

import android.content.Context
import com.genshin.dailynote.R
import com.genshin.dailynote.data.model.DailyNoteData
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object ExtrapolationUtils {

    const val RESIN_RECOVERY_SECONDS_PER_POINT = 480L // 8 minutes

    private val STANDARD_REALM_COIN_SEC_RATES = listOf(
        120.0,            // 30 / hr (20000+ Adeptal Energy)
        3600.0 / 28.0,    // 28 / hr (~128.57s)
        3600.0 / 26.0,    // 26 / hr (~138.46s)
        150.0,            // 24 / hr (150s)
        3600.0 / 22.0,    // 22 / hr (~163.64s)
        180.0,            // 20 / hr (180s)
        225.0,            // 16 / hr (225s)
        300.0,            // 12 / hr (300s)
        450.0,            // 8 / hr (450s)
        900.0             // 4 / hr (900s)
    )

    data class ExtrapolatedNote(
        val resin: Int,
        val maxResin: Int,
        val resinRecoverySeconds: Long,
        val finishedTasks: Int,
        val totalTasks: Int,
        val isExtraTaskRewardReceived: Boolean,
        val bossDiscountRemaining: Int,
        val bossDiscountLimit: Int,
        val currentHomeCoin: Int,
        val maxHomeCoin: Int,
        val homeCoinRecoverySeconds: Long,
        val completedExpeditions: Int,
        val totalExpeditions: Int,
        val minExpeditionRemainingSeconds: Long?,
        val transformerReady: Boolean,
        val transformerObtained: Boolean,
        val transformerRemainingSeconds: Long?,
        val lastSyncTimestamp: Long
    )

    fun extrapolate(
        cachedData: DailyNoteData,
        lastSyncTimestamp: Long,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): ExtrapolatedNote {
        val elapsedSeconds = max(0L, (currentTimeMillis - lastSyncTimestamp) / 1000L)

        // 1. Resin Extrapolation
        val maxResin = cachedData.max_resin.takeIf { it > 0 } ?: 200
        val cachedResin = cachedData.current_resin
        val cachedRecoverySec = cachedData.resinRecoverySeconds

        val gainedResin = (elapsedSeconds / RESIN_RECOVERY_SECONDS_PER_POINT).toInt()
        val currentResin = min(maxResin, cachedResin + gainedResin)

        val remainingRecoverySec = if (currentResin >= maxResin) {
            0L
        } else {
            max(0L, cachedRecoverySec - elapsedSeconds)
        }

        // 2. Expeditions Extrapolation
        var completedExpeditions = 0
        var lowestRemainingOngoingSec: Long? = null

        cachedData.expeditions.forEach { expedition ->
            val cachedRemained = expedition.remained_time.toLongOrNull() ?: 0L
            val currentRemained = max(0L, cachedRemained - elapsedSeconds)

            if (expedition.status == "Finished" || currentRemained <= 0L) {
                completedExpeditions++
            } else {
                if (lowestRemainingOngoingSec == null || currentRemained < lowestRemainingOngoingSec!!) {
                    lowestRemainingOngoingSec = currentRemained
                }
            }
        }

        val totalExpeditions = cachedData.expeditions.size.takeIf { it > 0 }
            ?: cachedData.max_expedition_num

        // 3. Parametric Transformer Extrapolation
        val transformerInfo = cachedData.transformer
        var isTransformerReady = false
        val isTransformerObtained = transformerInfo?.obtained == true
        var transformerRemainingSec: Long? = null

        if (isTransformerObtained) {
            val rec = transformerInfo?.recovery_time
            if (rec != null) {
                if (rec.reached) {
                    isTransformerReady = true
                    transformerRemainingSec = 0L
                } else {
                    val totalSec = rec.Day * 86400L + rec.Hour * 3600L + rec.Minute * 60L + rec.Second
                    val remaining = max(0L, totalSec - elapsedSeconds)
                    if (remaining <= 0L) {
                        isTransformerReady = true
                        transformerRemainingSec = 0L
                    } else {
                        transformerRemainingSec = remaining
                    }
                }
            } else {
                isTransformerReady = true
                transformerRemainingSec = 0L
            }
        }

        // 4. Realm Currency (Home Coin) Extrapolation
        val maxHomeCoin = cachedData.max_home_coin
        val cachedHomeCoin = (cachedData.current_home_coin / 10) * 10
        val cachedHomeCoinRecoverySec = cachedData.homeCoinRecoverySeconds

        val remainingHomeCoinSec = if (cachedHomeCoin >= maxHomeCoin || cachedHomeCoinRecoverySec <= 0L) {
            0L
        } else {
            max(0L, cachedHomeCoinRecoverySec - elapsedSeconds)
        }

        val currentHomeCoin = if (cachedHomeCoinRecoverySec > 0L && maxHomeCoin > cachedHomeCoin) {
            if (remainingHomeCoinSec <= 0L) {
                maxHomeCoin
            } else {
                // In Genshin, remote coin count is reported in multiples of 10 (e.g. 300 represents [300, 309]).
                // The recovery time indicates the exact countdown until maxHomeCoin is reached.
                // We identify the standard generation rate (seconds per coin) matching remote recovery countdown.
                val targetCenter = cachedHomeCoin + 4.5
                val secPerCoin = STANDARD_REALM_COIN_SEC_RATES.minByOrNull { rate ->
                    val estCoinsAtSync = maxHomeCoin - (cachedHomeCoinRecoverySec.toDouble() / rate)
                    abs(estCoinsAtSync - targetCenter)
                } ?: 120.0

                val exactCoins = maxHomeCoin - (remainingHomeCoinSec.toDouble() / secPerCoin)
                val tensCoins = (floor(exactCoins / 10.0) * 10.0).toInt()
                tensCoins.coerceIn(cachedHomeCoin, maxHomeCoin)
            }
        } else {
            if (maxHomeCoin > 0 && cachedHomeCoin >= maxHomeCoin) maxHomeCoin else cachedHomeCoin
        }

        return ExtrapolatedNote(
            resin = currentResin,
            maxResin = maxResin,
            resinRecoverySeconds = remainingRecoverySec,
            finishedTasks = cachedData.finished_task_num,
            totalTasks = cachedData.total_task_num,
            isExtraTaskRewardReceived = cachedData.is_extra_task_reward_received,
            bossDiscountRemaining = cachedData.remain_resin_discount_num,
            bossDiscountLimit = cachedData.resin_discount_num_limit,
            currentHomeCoin = currentHomeCoin,
            maxHomeCoin = maxHomeCoin,
            homeCoinRecoverySeconds = if (currentHomeCoin >= maxHomeCoin) 0L else remainingHomeCoinSec,
            completedExpeditions = completedExpeditions,
            totalExpeditions = totalExpeditions,
            minExpeditionRemainingSeconds = lowestRemainingOngoingSec,
            transformerReady = isTransformerReady,
            transformerObtained = isTransformerObtained,
            transformerRemainingSeconds = transformerRemainingSec,
            lastSyncTimestamp = lastSyncTimestamp
        )
    }

    fun getLocalizedHomeCoinRecovery(context: Context, note: ExtrapolatedNote): String {
        if (note.maxHomeCoin <= 0) return "--"
        if (note.currentHomeCoin >= note.maxHomeCoin) {
            return context.getString(R.string.realm_currency_full)
        }
        val effectiveSeconds = note.homeCoinRecoverySeconds
        if (effectiveSeconds <= 0L) {
            return "--"
        }
        val days = effectiveSeconds / 86400
        val hours = (effectiveSeconds % 86400) / 3600
        val minutes = (effectiveSeconds % 3600) / 60
        val lang = Locale.getDefault().language
        return when (lang) {
            "zh" -> when {
                days > 0 -> "${days}天${hours}时后满"
                hours > 0 -> "${hours}小时${minutes}分后满"
                else -> "${max(1L, minutes)}分后满"
            }
            "ja" -> when {
                days > 0 -> "${days}日${hours}時間後"
                hours > 0 -> "${hours}時間${minutes}分後"
                else -> "${max(1L, minutes)}分後"
            }
            else -> when {
                days > 0 -> "${days}d ${hours}h"
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${max(1L, minutes)}m"
            }
        }
    }

    fun getLocalizedResinRecovery(context: Context, note: ExtrapolatedNote): String {
        return if (note.resin >= note.maxResin || note.resinRecoverySeconds <= 0L) {
            context.getString(R.string.resin_full)
        } else {
            val durationStr = formatDurationHoursMinutes(note.resinRecoverySeconds)
            context.getString(R.string.resin_full_in, durationStr)
        }
    }

    fun getLocalizedExpeditionStatus(context: Context, note: ExtrapolatedNote): String {
        return when {
            note.totalExpeditions == 0 -> "--"
            note.completedExpeditions >= note.totalExpeditions -> context.getString(R.string.expeditions_all_done)
            note.minExpeditionRemainingSeconds != null -> {
                val durationStr = formatDurationHoursMinutes(note.minExpeditionRemainingSeconds)
                context.getString(R.string.expeditions_next_in, durationStr)
            }
            else -> "${note.completedExpeditions}/${note.totalExpeditions}"
        }
    }

    fun getLocalizedExpeditionTime(context: Context, note: ExtrapolatedNote): String {
        return when {
            note.totalExpeditions == 0 -> "--"
            note.completedExpeditions >= note.totalExpeditions -> context.getString(R.string.expeditions_all_done)
            note.minExpeditionRemainingSeconds != null -> formatDurationHoursMinutes(note.minExpeditionRemainingSeconds)
            else -> "${note.completedExpeditions}/${note.totalExpeditions}"
        }
    }

    fun getLocalizedTransformerStatus(context: Context, note: ExtrapolatedNote): String {
        return when {
            !note.transformerObtained -> context.getString(R.string.transformer_not_obtained)
            note.transformerReady -> context.getString(R.string.transformer_ready)
            note.transformerRemainingSeconds != null -> formatCooldownDaysHours(note.transformerRemainingSeconds)
            else -> context.getString(R.string.transformer_cooldown)
        }
    }

    fun formatDurationHoursMinutes(seconds: Long): String {
        if (seconds <= 0) return "0m"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val lang = Locale.getDefault().language
        return when (lang) {
            "zh" -> if (hours > 0) "${hours}小时${minutes}分" else "${minutes}分"
            "ja" -> if (hours > 0) "${hours}時間${minutes}分" else "${minutes}分"
            else -> if (hours > 0) String.format(Locale.US, "%dh %02dm", hours, minutes) else String.format(Locale.US, "%dm", minutes)
        }
    }

    fun formatCooldownDaysHours(seconds: Long): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val lang = Locale.getDefault().language
        return when (lang) {
            "zh" -> when {
                days > 0 -> "${days}天${hours}小时"
                hours > 0 -> "${hours}小时"
                else -> "${(seconds % 3600) / 60}分"
            }
            "ja" -> when {
                days > 0 -> "${days}日${hours}時間"
                hours > 0 -> "${hours}時間"
                else -> "${(seconds % 3600) / 60}分"
            }
            else -> when {
                days > 0 -> "${days}d ${hours}h"
                hours > 0 -> "${hours}h"
                else -> "${(seconds % 3600) / 60}m"
            }
        }
    }

    fun formatSyncTime(context: Context, timestamp: Long): String {
        if (timestamp <= 0L) return "--"
        val diffSeconds = (System.currentTimeMillis() - timestamp) / 1000
        return when {
            diffSeconds < 60 -> context.getString(R.string.time_just_now)
            diffSeconds < 3600 -> context.getString(R.string.time_minutes_ago, (diffSeconds / 60).toInt())
            diffSeconds < 86400 -> context.getString(R.string.time_hours_ago, (diffSeconds / 3600).toInt())
            else -> context.getString(R.string.time_days_ago, (diffSeconds / 86400).toInt())
        }
    }
}
