package com.sekusarisu.yanami.ui.screen.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.soundClick

class SettingsHubScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val viewModel = koinScreenModel<SettingsViewModel>()
        val state by viewModel.state.collectAsState()
        var showLanguageDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val adaptiveInfo = rememberAdaptiveLayoutInfo()
        val exportLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
                    uri -> if (uri != null) viewModel.exportConfig(uri)
                }
        val importLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) viewModel.importConfig(uri)
                }

        val languages = listOf(
                "system" to stringResource(R.string.settings_language_system),
                "zh" to "简体中文",
                "en" to "English",
                "ja" to "日本語"
        )
        val currentLanguageLabel = languages.firstOrNull { it.first == state.language }?.second ?: ""

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is SettingsEffect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LargeTopAppBar(
                            title = { Text(stringResource(R.string.settings_title)) },
                            scrollBehavior = scrollBehavior,
                            navigationIcon = {
                                IconButton(onClick = soundClick { navigator.pop() }) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription =
                                                    stringResource(R.string.action_back)
                                    )
                                }
                            }
                    )
                }
        ) { innerPadding ->
            AdaptiveContentPane(
                    modifier = Modifier.padding(innerPadding),
                    maxWidth = if (adaptiveInfo.isTabletLandscape) 900.dp else 840.dp
            ) {
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                ) {
                    // ── 分组标题: 通用 ──
                    SectionHeader(title = stringResource(R.string.settings_general))

                // ── 自动进入节点列表 ──
                SettingsToggleItem(
                    icon = Icons.Default.RocketLaunch,
                    title = stringResource(R.string.settings_auto_enter_nodelist),
                    subtitle = stringResource(R.string.settings_auto_enter_nodelist_desc),
                    checked = state.autoEnterNodeList,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.SetAutoEnterNodeList(it)) }
                )

                // ── 分组标题: 安全 ──
                SectionHeader(title = stringResource(R.string.settings_security))

                // ── 生物识别 ──
                SettingsToggleItem(
                    icon = Icons.Default.Fingerprint,
                    title = stringResource(R.string.settings_biometric),
                    subtitle = stringResource(R.string.settings_biometric_desc),
                    checked = state.biometricEnabled,
                    onCheckedChange = { newValue ->
                        authenticateWithBiometric(
                            activity = context as FragmentActivity,
                            title = context.getString(R.string.biometric_prompt_title),
                            subtitle = context.getString(R.string.biometric_prompt_subtitle),
                            onSuccess = {
                                viewModel.onEvent(SettingsEvent.SetBiometricEnabled(newValue))
                            }
                        )
                    }
                )

                // ── 分组标题: UI ──
                SectionHeader(title = stringResource(R.string.settings_ui))

                // ── 图表动画 ──
                SettingsToggleItem(
                    icon = Icons.Default.Animation,
                    title = stringResource(R.string.settings_chart_animation),
                    subtitle = stringResource(R.string.settings_chart_animation_desc),
                    checked = state.chartAnimationEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.SetChartAnimation(it)) }
                )

                // ── 视觉与样式 ──
                SettingsNavItem(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.settings_visual_style),
                        subtitle = stringResource(R.string.settings_visual_style_desc),
                        onClick = soundClick { navigator.push(SettingsScreen()) }
                )

                // ── 语言 ──
                SettingsNavItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = currentLanguageLabel,
                    onClick = soundClick { showLanguageDialog = true }
                )

                // ── 分组标题: 备份 ──
                SectionHeader(title = stringResource(R.string.settings_backup))

                SettingsNavItem(
                        icon = Icons.Default.Upload,
                        title = stringResource(R.string.settings_export_config),
                        subtitle = stringResource(R.string.settings_export_config_desc),
                        onClick = soundClick {
                            exportLauncher.launch("yanami-config-backup.json")
                        }
                )

                SettingsNavItem(
                        icon = Icons.Default.Download,
                        title = stringResource(R.string.settings_import_config),
                        subtitle = stringResource(R.string.settings_import_config_desc),
                        onClick = soundClick {
                            importLauncher.launch(arrayOf("application/json", "text/plain"))
                        }
                )

                // ── 分组标题: 其他 ──
                SectionHeader(title = stringResource(R.string.settings_others))

                // ── 关于 ──
                SettingsNavItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_about),
                        subtitle = stringResource(R.string.settings_about_desc),
                        onClick = soundClick { navigator.push(AboutScreen()) }
                )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // ── 语言选择对话框 ──
        if (showLanguageDialog) {
            AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    title = { Text(stringResource(R.string.settings_language)) },
                    text = {
                        Column {
                            languages.forEach { (key, label) ->
                                Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.onEvent(SettingsEvent.SetLanguage(key))
                                                showLanguageDialog = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                            selected = state.language == key,
                                            onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {}
            )
        }
    }
}

/** 导航/可点击设置项：图标 + 标题/副标题 */
@Composable
private fun SettingsNavItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit
) {
    Row(
            modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClick)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 分组标题：主题色文本 */
@Composable
private fun SectionHeader(title: String) {
    Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 56.dp, top = 24.dp, bottom = 8.dp)
    )
}

/**
 * 弹出生物识别/设备密码验证弹窗，成功后回调 [onSuccess]。
 * 若设备不支持任何认证方式，直接调用 [onSuccess]（降级放行）。
 */
private fun authenticateWithBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit
) {
    val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val canAuth = BiometricManager.from(activity).canAuthenticate(authenticators)
    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
        // 设备不支持或未注册凭据，直接放行
        onSuccess()
        return
    }
    val prompt =
            BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(activity),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                                result: BiometricPrompt.AuthenticationResult
                        ) {
                            onSuccess()
                        }
                        // onAuthenticationError / onAuthenticationFailed → 不做任何操作，开关保持原值
                    }
            )
    prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(authenticators)
                    .build()
    )
}

/** 开关设置项：图标 + 标题/描述 + Switch */@Composable
private fun SettingsToggleItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
) {
    Row(
            modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onCheckedChange(!checked) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
                checked = checked,
                onCheckedChange = null
        )
    }
}
