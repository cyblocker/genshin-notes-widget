package com.genshin.dailynote.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genshin.dailynote.R
import com.genshin.dailynote.data.api.ApiResult
import com.genshin.dailynote.data.api.GenshinApiService
import com.genshin.dailynote.data.local.ExtrapolationUtils
import com.genshin.dailynote.ui.theme.AccentGreen
import com.genshin.dailynote.ui.theme.AccentOrange
import com.genshin.dailynote.ui.theme.AccentPurple
import com.genshin.dailynote.ui.theme.BgDark
import com.genshin.dailynote.ui.theme.BorderDark
import com.genshin.dailynote.ui.theme.CardDark
import com.genshin.dailynote.ui.theme.CardDarkElevated
import com.genshin.dailynote.ui.theme.GenshinDailyNotesTheme
import com.genshin.dailynote.ui.theme.PrimaryCyan
import com.genshin.dailynote.ui.theme.SecondaryGold
import com.genshin.dailynote.ui.theme.TextMuted
import com.genshin.dailynote.ui.theme.TextPrimary
import com.genshin.dailynote.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GenshinDailyNotesTheme {
                val uiState by viewModel.uiState.collectAsState()
                val context = LocalContext.current

                val loginLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val data = result.data
                        val uid = data?.getStringExtra(LoginActivity.EXTRA_UID)
                        val server = data?.getStringExtra(LoginActivity.EXTRA_SERVER)
                        val cookie = data?.getStringExtra(LoginActivity.EXTRA_COOKIE)

                        if (!uid.isNullOrBlank()) viewModel.onUidChanged(uid)
                        if (!server.isNullOrBlank()) viewModel.onServerChanged(server)
                        if (!cookie.isNullOrBlank()) viewModel.onCookieChanged(cookie)

                        viewModel.saveAndSync()
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = stringResource(R.string.title_config_sync),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
                        )
                    },
                    containerColor = BgDark
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // Credentials Configuration Card
                        ConfigCard(
                            uiState = uiState,
                            onUidChange = viewModel::onUidChanged,
                            onServerChange = viewModel::onServerChanged,
                            onCookieChange = viewModel::onCookieChanged,
                            onTestConnection = viewModel::testConnection,
                            onSaveAndSync = viewModel::saveAndSync,
                            onAutoLogin = {
                                loginLauncher.launch(Intent(context, LoginActivity::class.java))
                            }
                        )

                        // Status / Notification Banner
                        uiState.statusMessage?.let { msg ->
                            StatusBanner(
                                message = msg,
                                isError = uiState.testResult is ApiResult.Error || uiState.testResult is ApiResult.NetworkError
                            )
                        }

                        // Live Widget Preview
                        WidgetPreviewCard(uiState = uiState)

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigCard(
    uiState: MainUiState,
    onUidChange: (String) -> Unit,
    onServerChange: (String) -> Unit,
    onCookieChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSaveAndSync: () -> Unit,
    onAutoLogin: () -> Unit
) {
    val context = LocalContext.current
    var serverMenuExpanded by remember { mutableStateOf(false) }
    var showCookieGuideDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.credentials_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            // Auto-Login Button
            Button(
                onClick = onAutoLogin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF26324D),
                    contentColor = PrimaryCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.auto_login_button), fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderDark))
                Text(
                    text = "  ${stringResource(R.string.or_manual_input)}  ",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderDark))
            }

            // UID Input
            OutlinedTextField(
                value = uiState.uid,
                onValueChange = onUidChange,
                label = { Text(stringResource(R.string.label_uid)) },
                placeholder = { Text(stringResource(R.string.placeholder_uid)) },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryCyan)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            // Server Selector
            ExposedDropdownMenuBox(
                expanded = serverMenuExpanded,
                onExpandedChange = { serverMenuExpanded = it }
            ) {
                val currentServerInfo = GenshinApiService.SERVERS.find { it.id == uiState.server }
                    ?: GenshinApiService.SERVERS.first()

                OutlinedTextField(
                    value = currentServerInfo.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_server)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serverMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = textFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = serverMenuExpanded,
                    onDismissRequest = { serverMenuExpanded = false },
                    modifier = Modifier.background(CardDarkElevated)
                ) {
                    GenshinApiService.SERVERS.forEach { server ->
                        DropdownMenuItem(
                            text = { Text(server.displayName, color = TextPrimary) },
                            onClick = {
                                onServerChange(server.id)
                                serverMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Cookie Input
            OutlinedTextField(
                value = uiState.cookie,
                onValueChange = onCookieChange,
                label = { Text(stringResource(R.string.label_cookie)) },
                placeholder = { Text("ltoken_v2=...; ltuid_v2=...;") },
                leadingIcon = {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = SecondaryGold)
                },
                trailingIcon = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                        if (!clip.isNullOrBlank()) {
                            onCookieChange(clip)
                            Toast.makeText(context, context.getString(R.string.toast_cookie_pasted), Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = PrimaryCyan)
                    }
                },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            // How to get cookie button (on-demand guide)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showCookieGuideDialog = true },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = PrimaryCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.btn_cookie_guide),
                        fontSize = 12.sp,
                        color = PrimaryCyan
                    )
                }
            }

            if (showCookieGuideDialog) {
                AlertDialog(
                    onDismissRequest = { showCookieGuideDialog = false },
                    title = {
                        Text(
                            stringResource(R.string.cookie_guide_title),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            stringResource(R.string.cookie_guide_content),
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showCookieGuideDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = BgDark)
                        ) {
                            Text(stringResource(R.string.dialog_close), fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = CardDark,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = !uiState.isTesting && !uiState.isSavingAndSyncing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCyan)
                ) {
                    if (uiState.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(stringResource(R.string.btn_test_api))
                }

                Button(
                    onClick = onSaveAndSync,
                    enabled = !uiState.isTesting && !uiState.isSavingAndSyncing,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = BgDark)
                ) {
                    if (uiState.isSavingAndSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BgDark, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(stringResource(R.string.btn_save_sync), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(message: String, isError: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isError) Color(0xFF3B1E2B) else Color(0xFF1E382B))
            .border(1.dp, if (isError) Color(0xFFF87171) else AccentGreen, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) Color(0xFFF87171) else AccentGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun WidgetPreviewCard(uiState: MainUiState) {
    val note = uiState.extrapolatedNote
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (note != null) stringResource(R.string.preview_extrapolate_active) else stringResource(R.string.preview_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (note != null) AccentGreen else TextMuted
                )
            }

            if (note == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDarkElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.preview_empty_hint),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                val resinRecovery = ExtrapolationUtils.getLocalizedResinRecovery(context, note)
                val syncTime = ExtrapolationUtils.formatSyncTime(context, note.lastSyncTimestamp)

                // Render Widget Visual Simulation (Matches the refined 4x2 layout)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF13161F))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF242A3D))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("GENSHIN", color = PrimaryCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("UID ${uiState.uid}", color = TextMuted, fontSize = 10.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(syncTime, color = TextSecondary, fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }

                        // Resin Row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1C202E))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.genshin_resin),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${note.resin}", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("/${note.maxResin}", color = TextSecondary, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                LinearProgressIndicator(
                                    progress = { (note.resin.toFloat() / note.maxResin).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = PrimaryCyan,
                                    trackColor = BorderDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    resinRecovery,
                                    color = if (note.resin >= note.maxResin) AccentGreen else PrimaryCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Compact 5 Metrics Row
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PreviewCompactBadge(
                                value = "${note.finishedTasks}/${note.totalTasks}",
                                subtext = when {
                                    note.isExtraTaskRewardReceived -> stringResource(R.string.commissions_claimed)
                                    note.finishedTasks >= note.totalTasks -> stringResource(R.string.commissions_claim_bonus)
                                    else -> stringResource(R.string.commissions_pending)
                                },
                                modifier = Modifier.weight(1f),
                                accentColor = if (note.isExtraTaskRewardReceived) AccentGreen else SecondaryGold
                            )
                            PreviewCompactBadge(
                                value = "${note.bossDiscountRemaining}/${note.bossDiscountLimit}",
                                subtext = stringResource(R.string.boss_discount_sub),
                                modifier = Modifier.weight(1f),
                                accentColor = AccentOrange
                            )
                            PreviewCompactBadge(
                                value = "${note.currentHomeCoin}",
                                subtext = "/${note.maxHomeCoin}",
                                modifier = Modifier.weight(1f),
                                accentColor = SecondaryGold
                            )
                            PreviewCompactBadge(
                                value = if (note.transformerReady) stringResource(R.string.transformer_ready) else ExtrapolationUtils.getLocalizedTransformerStatus(context, note),
                                subtext = stringResource(R.string.transformer_title),
                                modifier = Modifier.weight(1f),
                                accentColor = if (note.transformerReady) AccentPurple else TextSecondary
                            )
                            PreviewCompactBadge(
                                value = ExtrapolationUtils.getLocalizedExpeditionTime(context, note),
                                subtext = stringResource(R.string.expeditions_title),
                                modifier = Modifier.weight(1f),
                                accentColor = if (note.completedExpeditions >= note.totalExpeditions && note.totalExpeditions > 0) AccentGreen else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCompactBadge(
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1C202E))
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtext, color = accentColor, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryCyan,
    unfocusedBorderColor = BorderDark,
    focusedLabelColor = PrimaryCyan,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = PrimaryCyan
)
