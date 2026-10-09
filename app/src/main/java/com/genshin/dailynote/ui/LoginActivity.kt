package com.genshin.dailynote.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.genshin.dailynote.GenshinApp
import com.genshin.dailynote.R
import com.genshin.dailynote.data.api.ApiResult
import com.genshin.dailynote.data.model.UserGameRole
import com.genshin.dailynote.ui.theme.AccentGreen
import com.genshin.dailynote.ui.theme.BgDark
import com.genshin.dailynote.ui.theme.BorderDark
import com.genshin.dailynote.ui.theme.CardDark
import com.genshin.dailynote.ui.theme.GenshinDailyNotesTheme
import com.genshin.dailynote.ui.theme.PrimaryCyan
import com.genshin.dailynote.ui.theme.TextMuted
import com.genshin.dailynote.ui.theme.TextPrimary
import com.genshin.dailynote.ui.theme.TextSecondary
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    companion object {
        const val EXTRA_UID = "extra_uid"
        const val EXTRA_SERVER = "extra_server"
        const val EXTRA_COOKIE = "extra_cookie"

        const val HOYOLAB_URL = "https://m.hoyolab.com/"
        const val MIYOUSHE_URL = "https://m.bbs.mihoyo.com/"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as GenshinApp
        val apiService = app.apiService
        val preferences = app.preferencesRepository

        setContent {
            GenshinDailyNotesTheme {
                val coroutineScope = rememberCoroutineScope()
                var isCn by remember { mutableStateOf(false) }
                var webViewRef by remember { mutableStateOf<WebView?>(null) }
                var pageProgress by remember { mutableFloatStateOf(0f) }
                val defaultWaitingText = stringResource(R.string.login_subtitle_waiting)
                val detectedText = stringResource(R.string.login_cookie_detected)
                var statusText by remember { mutableStateOf(defaultWaitingText) }
                var isDetected by remember { mutableStateOf(false) }
                var isExtracting by remember { mutableStateOf(false) }

                fun checkCookies(url: String) {
                    if (isDetected) return
                    val cookieManager = CookieManager.getInstance()
                    val cookieStr = cookieManager.getCookie(url) ?: return

                    val hasToken = cookieStr.contains("ltoken_v2") || cookieStr.contains("ltoken")
                    val hasUid = cookieStr.contains("ltuid_v2") || cookieStr.contains("ltuid") || cookieStr.contains("account_id")

                    if (hasToken && hasUid) {
                        isDetected = true
                        isExtracting = true
                        statusText = detectedText

                        val formattedCookie = extractTargetCookies(cookieStr)

                        coroutineScope.launch {
                            val roleResult = apiService.fetchUserGameRoles(formattedCookie, isCn = isCn)
                            isExtracting = false

                            when (roleResult) {
                                is ApiResult.Success -> {
                                    val roles = roleResult.data
                                    val primaryRole = roles.firstOrNull()

                                    if (primaryRole != null) {
                                        Toast.makeText(
                                            this@LoginActivity,
                                            "${primaryRole.nickname} (${primaryRole.game_uid})",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        preferences.saveCredentials(
                                            uid = primaryRole.game_uid,
                                            server = primaryRole.region,
                                            cookie = formattedCookie
                                        )

                                        val intent = Intent().apply {
                                            putExtra(EXTRA_UID, primaryRole.game_uid)
                                            putExtra(EXTRA_SERVER, primaryRole.region)
                                            putExtra(EXTRA_COOKIE, formattedCookie)
                                        }
                                        setResult(Activity.RESULT_OK, intent)
                                        finish()
                                    } else {
                                        val intent = Intent().apply {
                                            putExtra(EXTRA_COOKIE, formattedCookie)
                                        }
                                        setResult(Activity.RESULT_OK, intent)
                                        finish()
                                    }
                                }
                                is ApiResult.Error -> {
                                    val intent = Intent().apply {
                                        putExtra(EXTRA_COOKIE, formattedCookie)
                                    }
                                    setResult(Activity.RESULT_OK, intent)
                                    finish()
                                }
                                is ApiResult.NetworkError -> {
                                    val intent = Intent().apply {
                                        putExtra(EXTRA_COOKIE, formattedCookie)
                                    }
                                    setResult(Activity.RESULT_OK, intent)
                                    finish()
                                }
                            }
                        }
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(stringResource(R.string.login_title), color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                                    Text(statusText, color = if (isDetected) AccentGreen else TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                                }
                            },
                            actions = {
                                IconButton(onClick = { webViewRef?.reload() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = PrimaryCyan)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
                        )
                    },
                    containerColor = BgDark
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // Region toggle row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardDark)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(stringResource(R.string.login_region_label), color = TextMuted, fontSize = 12.sp)

                            FilterChip(
                                selected = !isCn,
                                onClick = {
                                    if (isCn) {
                                        isCn = false
                                        webViewRef?.loadUrl(HOYOLAB_URL)
                                    }
                                },
                                label = { Text(stringResource(R.string.login_region_global), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryCyan,
                                    selectedLabelColor = BgDark
                                )
                            )

                            FilterChip(
                                selected = isCn,
                                onClick = {
                                    if (!isCn) {
                                        isCn = true
                                        webViewRef?.loadUrl(MIYOUSHE_URL)
                                    }
                                },
                                label = { Text(stringResource(R.string.login_region_cn), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryCyan,
                                    selectedLabelColor = BgDark
                                )
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            if (isExtracting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryCyan, strokeWidth = 2.dp)
                            }
                        }

                        if (pageProgress in 0.01f..0.99f) {
                            LinearProgressIndicator(
                                progress = { pageProgress },
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = PrimaryCyan,
                                trackColor = BorderDark
                            )
                        }

                        // Embedded WebView
                        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        setupWebView(this) { url -> checkCookies(url) }
                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                pageProgress = newProgress / 100f
                                            }
                                        }
                                        webViewRef = this
                                        loadUrl(if (isCn) MIYOUSHE_URL else HOYOLAB_URL)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView, onUrlChanged: (String) -> Unit) {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148"
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { onUrlChanged(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { onUrlChanged(it) }
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                view?.url?.let { onUrlChanged(it) }
            }
        }
    }

    private fun extractTargetCookies(rawCookie: String): String {
        val pairs = rawCookie.split(";").map { it.trim() }
        val targetKeys = setOf(
            "ltuid_v2", "ltoken_v2", "cookie_token_v2", "account_mid_v2",
            "account_id", "ltuid", "ltoken", "cookie_token"
        )

        val filtered = pairs.filter { pair ->
            val key = pair.substringBefore("=").trim()
            targetKeys.contains(key)
        }

        return if (filtered.isNotEmpty()) {
            filtered.joinToString("; ") + ";"
        } else {
            rawCookie
        }
    }
}
