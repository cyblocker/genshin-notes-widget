package com.genshin.dailynote.data.local

import com.genshin.dailynote.data.model.DailyNoteData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtrapolationUtilsTest {

    @Test
    fun testRealmCoin_LowestDigitAlwaysZero_AndJumpsByTens() {
        // Assume player has max 2400, rate 30/hr (120s/coin).
        // At sync time, actual unrounded coins = 303.
        // Recovery seconds = (2400 - 303) * 120 = 2097 * 120 = 251640 seconds.
        // Remote data reports current_home_coin = 300, home_coin_recovery_time = 251640.
        val syncTimeMillis = 1_700_000_000_000L
        val data = DailyNoteData(
            current_home_coin = 300,
            max_home_coin = 2400,
            home_coin_recovery_time = "251640"
        )

        // At sync time (elapsed = 0s):
        // exact coins = 2400 - 251640 / 120 = 303.0.
        // tens = floor(303.0 / 10) * 10 = 300.
        val note0 = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis)
        assertEquals(300, note0.currentHomeCoin)
        assertEquals(251640L, note0.homeCoinRecoverySeconds)
        assertEquals(0, note0.currentHomeCoin % 10)

        // Elapsed = 500s (~8.3 min):
        // Remaining recovery sec = 251640 - 500 = 251140s.
        // exact coins = 2400 - 251140 / 120 = 307.166.
        // tens = floor(307.166 / 10) * 10 = 300.
        val note500 = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis + 500L * 1000L)
        assertEquals(300, note500.currentHomeCoin)
        assertEquals(251140L, note500.homeCoinRecoverySeconds)
        assertEquals(0, note500.currentHomeCoin % 10)

        // Elapsed = 839s (just before reaching 310 coins, since 303 + 839/120 = 309.991):
        val note839 = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis + 839L * 1000L)
        assertEquals(300, note839.currentHomeCoin)
        assertEquals(0, note839.currentHomeCoin % 10)

        // Elapsed = 840s (303 + 840/120 = 310.0 coins):
        // Directly jumps from 300 to 310!
        val note840 = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis + 840L * 1000L)
        assertEquals(310, note840.currentHomeCoin)
        assertEquals(251640L - 840L, note840.homeCoinRecoverySeconds)
        assertEquals(0, note840.currentHomeCoin % 10)

        // Elapsed = 2040s (303 + 2040/120 = 320.0 coins):
        // Jumps directly to 320!
        val note2040 = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis + 2040L * 1000L)
        assertEquals(320, note2040.currentHomeCoin)
        assertEquals(0, note2040.currentHomeCoin % 10)

        // Elapsed = 251640s (full recovery reached):
        val noteFull = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis + 251640L * 1000L)
        assertEquals(2400, noteFull.currentHomeCoin)
        assertEquals(0L, noteFull.homeCoinRecoverySeconds)
        assertEquals(0, noteFull.currentHomeCoin % 10)
    }

    @Test
    fun testRealmCoin_CountdownInSyncWithRemoteData() {
        val syncTimeMillis = 1_700_000_000_000L
        val data = DailyNoteData(
            current_home_coin = 300,
            max_home_coin = 2400,
            home_coin_recovery_time = "251640"
        )

        val note = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis + 120L * 1000L)
        // Countdown MUST be exactly remote recovery sec minus elapsed, NOT derived from (max - coin) * 120
        assertEquals(251640L - 120L, note.homeCoinRecoverySeconds)
    }

    @Test
    fun testRealmCoin_WhenFull() {
        val syncTimeMillis = 1_700_000_000_000L
        val data = DailyNoteData(
            current_home_coin = 2400,
            max_home_coin = 2400,
            home_coin_recovery_time = "0"
        )

        val note = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis + 3600L * 1000L)
        assertEquals(2400, note.currentHomeCoin)
        assertEquals(0L, note.homeCoinRecoverySeconds)
    }

    @Test
    fun testRealmCoin_NoRecoveryTime_DoesNotDeriveCountdown() {
        val syncTimeMillis = 1_700_000_000_000L
        val data = DailyNoteData(
            current_home_coin = 300,
            max_home_coin = 2400,
            home_coin_recovery_time = "0"
        )

        val note = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis + 3600L * 1000L)
        assertEquals(300, note.currentHomeCoin)
        assertEquals(0L, note.homeCoinRecoverySeconds)
    }

    @Test
    fun testRealmCoin_AlwaysMultipleOfTen_OverContinuousTime() {
        val syncTimeMillis = 1_700_000_000_000L
        val data = DailyNoteData(
            current_home_coin = 300,
            max_home_coin = 2400,
            home_coin_recovery_time = "251640"
        )

        // Check every 30 seconds for 2 hours (240 steps)
        for (sec in 0..7200 step 30) {
            val note = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis + sec * 1000L)
            assertEquals("Lowest digit must always be 0 at elapsed=${sec}s", 0, note.currentHomeCoin % 10)
            assertTrue("Coin count should be monotonic at elapsed=${sec}s", note.currentHomeCoin >= 300)
            assertTrue("Coin count should not exceed max at elapsed=${sec}s", note.currentHomeCoin <= 2400)
        }
    }

    @Test
    fun testRealmCoin_AlternativeRate_24PerHour() {
        // 24 coins / hr = 150s per coin.
        // max = 2400, actual coins at sync = 1005 (reported 1000).
        // remaining coins = 1395, recovery = 1395 * 150 = 209250 seconds.
        val syncTimeMillis = 1_700_000_000_000L
        val data = DailyNoteData(
            current_home_coin = 1000,
            max_home_coin = 2400,
            home_coin_recovery_time = "209250"
        )

        val note0 = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis)
        assertEquals(1000, note0.currentHomeCoin)
        assertEquals(209250L, note0.homeCoinRecoverySeconds)
        assertEquals(0, note0.currentHomeCoin % 10)

        // At 749s (1005 + 749/150 = 1009.993) -> still 1000
        val note749 = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis + 749L * 1000L)
        assertEquals(1000, note749.currentHomeCoin)

        // At 750s (1005 + 750/150 = 1010.0) -> jumps directly to 1010
        val note750 = ExtrapolationUtils.extrapolate(data, syncTimeMillis, syncTimeMillis + 750L * 1000L)
        assertEquals(1010, note750.currentHomeCoin)
        assertEquals(0, note750.currentHomeCoin % 10)
    }
}
