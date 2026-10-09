package com.genshin.dailynote.data.model

import com.google.gson.annotations.SerializedName

data class DailyNoteResponse(
    @SerializedName("retcode") val retcode: Int = 0,
    @SerializedName("message") val message: String = "",
    @SerializedName("data") val data: DailyNoteData? = null
)

data class DailyNoteData(
    @SerializedName("current_resin") val current_resin: Int = 0,
    @SerializedName("max_resin") val max_resin: Int = 200,
    @SerializedName("resin_recovery_time") val resin_recovery_time: String = "0", // Seconds remaining (String)
    @SerializedName("finished_task_num") val finished_task_num: Int = 0,
    @SerializedName("total_task_num") val total_task_num: Int = 4,
    @SerializedName("is_extra_task_reward_received") val is_extra_task_reward_received: Boolean = false,
    @SerializedName("remain_resin_discount_num") val remain_resin_discount_num: Int = 0,
    @SerializedName("resin_discount_num_limit") val resin_discount_num_limit: Int = 3,
    @SerializedName("current_home_coin") val current_home_coin: Int = 0,
    @SerializedName("max_home_coin") val max_home_coin: Int = 2400,
    @SerializedName("home_coin_recovery_time") val home_coin_recovery_time: String = "0", // Seconds until full (String)
    @SerializedName("current_expedition_num") val current_expedition_num: Int = 0,
    @SerializedName("max_expedition_num") val max_expedition_num: Int = 5,
    @SerializedName("expeditions") val expeditions: List<ExpeditionItem> = emptyList(),
    @SerializedName("transformer") val transformer: TransformerInfo? = null
) {
    val resinRecoverySeconds: Long
        get() = resin_recovery_time.toLongOrNull() ?: 0L

    val homeCoinRecoverySeconds: Long
        get() = home_coin_recovery_time.toLongOrNull() ?: 0L

    val completedExpeditionCount: Int
        get() = expeditions.count { it.status == "Finished" || (it.remained_time.toLongOrNull() ?: 0L) <= 0L }

    val minExpeditionRemainingSeconds: Long?
        get() = expeditions
            .filter { it.status == "Ongoing" }
            .mapNotNull { it.remained_time.toLongOrNull() }
            .filter { it > 0L }
            .minOrNull()
}

data class ExpeditionItem(
    @SerializedName("avatar_side_icon") val avatar_side_icon: String = "",
    @SerializedName("status") val status: String = "Ongoing", // "Ongoing" or "Finished"
    @SerializedName("remained_time") val remained_time: String = "0" // Remaining seconds as String
)

data class TransformerInfo(
    @SerializedName("obtained") val obtained: Boolean = false,
    @SerializedName("recovery_time") val recovery_time: TransformerRecoveryTime? = null
)

data class TransformerRecoveryTime(
    @SerializedName("Day") val Day: Int = 0,
    @SerializedName("Hour") val Hour: Int = 0,
    @SerializedName("Minute") val Minute: Int = 0,
    @SerializedName("Second") val Second: Int = 0,
    @SerializedName("reached") val reached: Boolean = false // true = ready to use
)

data class UserGameRolesResponse(
    @SerializedName("retcode") val retcode: Int = 0,
    @SerializedName("message") val message: String = "",
    @SerializedName("data") val data: UserGameRolesData? = null
)

data class UserGameRolesData(
    @SerializedName("list") val list: List<UserGameRole> = emptyList()
)

data class UserGameRole(
    @SerializedName("game_biz") val game_biz: String = "",
    @SerializedName("region") val region: String = "",
    @SerializedName("game_uid") val game_uid: String = "",
    @SerializedName("nickname") val nickname: String = "",
    @SerializedName("level") val level: Int = 0,
    @SerializedName("is_chosen") val is_chosen: Boolean = false,
    @SerializedName("region_name") val region_name: String = ""
)
