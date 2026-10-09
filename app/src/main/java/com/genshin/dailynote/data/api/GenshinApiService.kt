package com.genshin.dailynote.data.api

import com.genshin.dailynote.data.model.DailyNoteData
import com.genshin.dailynote.data.model.DailyNoteResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val exception: Throwable) : ApiResult<Nothing>()
}

class GenshinApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {

    companion object {
        const val HOYOLAB_BASE_URL = "https://bbs-api-os.hoyolab.com/game_record/app/genshin/api/dailyNote"
        const val MIYOUSHE_BASE_URL = "https://bbs-api.miyoushe.com/game_record/app/genshin/api/dailyNote"

        const val CLIENT_TYPE = "5"
        const val APP_VERSION = "1.5.0"
        const val USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148"

        // Server list definitions
        val SERVERS = listOf(
            ServerInfo("os_asia", "Asia (Global)", false),
            ServerInfo("os_usa", "America (Global)", false),
            ServerInfo("os_euro", "Europe (Global)", false),
            ServerInfo("os_cht", "TW/HK/MO (Global)", false),
            ServerInfo("cn_gf01", "Official (CN)", true),
            ServerInfo("cn_qd01", "Bilibili (CN)", true)
        )

        fun isCnServer(server: String): Boolean = server.startsWith("cn_")

        /**
         * Infers default server identifier based on UID prefix
         */
        fun guessServerByUid(uid: String): String {
            if (uid.isBlank()) return "os_asia"
            if (uid.startsWith("18")) return "os_asia"
            return when (uid.firstOrNull()) {
                '1', '2', '3' -> "cn_gf01"
                '5' -> "cn_qd01"
                '6' -> "os_usa"
                '7' -> "os_euro"
                '8' -> "os_asia"
                '9' -> "os_cht"
                else -> "os_asia"
            }
        }
    }

    data class ServerInfo(val id: String, val displayName: String, val isCn: Boolean)

    /**
     * Fetches the full daily note status from HoYoLAB / Miyoushe
     */
    suspend fun fetchDailyNote(
        uid: String,
        server: String,
        cookie: String
    ): ApiResult<DailyNoteData> = withContext(Dispatchers.IO) {
        val trimmedUid = uid.trim()
        val trimmedServer = server.trim()
        val trimmedCookie = cookie.trim()

        if (trimmedUid.isEmpty()) {
            return@withContext ApiResult.Error(-1, "UID is required")
        }
        if (trimmedCookie.isEmpty()) {
            return@withContext ApiResult.Error(-2, "Cookie is required")
        }

        val isCn = isCnServer(trimmedServer)
        val baseUrl = if (isCn) MIYOUSHE_BASE_URL else HOYOLAB_BASE_URL

        val urlBuilder = baseUrl.toHttpUrlOrNull()?.newBuilder()
            ?: return@withContext ApiResult.Error(-3, "Invalid URL configuration")

        val targetUrl = urlBuilder
            .addQueryParameter("role_id", trimmedUid)
            .addQueryParameter("server", trimmedServer)
            .build()

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .get()
            .header("Cookie", trimmedCookie)
            .header("x-rpc-client_type", CLIENT_TYPE)
            .header("x-rpc-app_version", APP_VERSION)
            .header("User-Agent", USER_AGENT)
            .header("Referer", if (isCn) "https://webstatic.mihoyo.com/" else "https://webstatic-sea.hoyolab.com/")
            .header("Accept", "application/json")

        // Include DS header for CN endpoints
        if (isCn) {
            requestBuilder.header("DS", DsTokenGenerator.generateDs(isCn = true))
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext ApiResult.Error(
                    response.code,
                    "HTTP ${response.code}: ${response.message}"
                )
            }

            val parsed = gson.fromJson(responseBody, DailyNoteResponse::class.java)

            when (parsed.retcode) {
                0 -> {
                    val data = parsed.data
                    if (data != null) {
                        ApiResult.Success(data)
                    } else {
                        ApiResult.Error(0, "Empty data received from server")
                    }
                }
                10001 -> ApiResult.Error(parsed.retcode, "Session expired (10001). Please update your Cookie.")
                10102 -> ApiResult.Error(parsed.retcode, "Daily Note is private. Enable 'Real-Time Notes' in HoYoLAB privacy settings.")
                1008 -> ApiResult.Error(parsed.retcode, "Invalid character UID or server mismatch.")
                -100 -> ApiResult.Error(parsed.retcode, "Invalid login credentials (-100). Check ltuid and ltoken.")
                else -> ApiResult.Error(
                    parsed.retcode,
                    if (parsed.message.isNotBlank()) parsed.message else "Error retcode ${parsed.retcode}"
                )
            }
        } catch (e: IOException) {
            ApiResult.NetworkError(e)
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /**
     * Queries all bound Genshin Impact game accounts/roles for the given cookie session
     */
    suspend fun fetchUserGameRoles(
        cookie: String,
        isCn: Boolean = false
    ): ApiResult<List<com.genshin.dailynote.data.model.UserGameRole>> = withContext(Dispatchers.IO) {
        val trimmedCookie = cookie.trim()
        if (trimmedCookie.isEmpty()) {
            return@withContext ApiResult.Error(-2, "Cookie is required")
        }

        val url = if (isCn) {
            "https://api-takumi.mihoyo.com/binding/api/getUserGameRolesByCookie?game_biz=hk4e_cn"
        } else {
            "https://bbs-api-os.hoyolab.com/binding/api/getUserGameRolesByCookie?game_biz=hk4e_global"
        }

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Cookie", trimmedCookie)
            .header("x-rpc-client_type", CLIENT_TYPE)
            .header("x-rpc-app_version", APP_VERSION)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext ApiResult.Error(response.code, "HTTP ${response.code}: ${response.message}")
            }

            val parsed = gson.fromJson(responseBody, com.genshin.dailynote.data.model.UserGameRolesResponse::class.java)
            if (parsed.retcode == 0) {
                val roles = parsed.data?.list ?: emptyList()
                ApiResult.Success(roles)
            } else {
                ApiResult.Error(parsed.retcode, parsed.message.ifBlank { "Failed to query game roles (${parsed.retcode})" })
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}

