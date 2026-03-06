package com.sekusarisu.yanami.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.soundClick
import com.sekusarisu.yanami.ui.theme.ThemeColor

/** 设置页面 */
class SettingsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SettingsViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
                topBar = {
                    TopAppBar(
                            title = { Text(stringResource(R.string.settings_title)) },
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
            Column(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(innerPadding)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp)
            ) {
                // ── 主题颜色 ──
                SettingsSectionHeader(
                        icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                        title = stringResource(R.string.settings_theme_color)
                )
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                ) {
                    ThemeColor.entries.forEach { color ->
                        // Android 12 以下跳过 DYNAMIC 选项
                        if (color == ThemeColor.DYNAMIC &&
                                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                        ) {
                            return@forEach
                        }

                        ThemeColorCircle(
                                themeColor = color,
                                isSelected = state.themeColor == color,
                                label = getThemeColorName(color),
                                onClick = soundClick { viewModel.onEvent(SettingsEvent.SetThemeColor(color)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // ── 深色模式 ──
                SettingsSectionHeader(
                        icon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        title = stringResource(R.string.settings_dark_mode)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                ) {
                    DarkModeChip(
                            label = stringResource(R.string.settings_dark_mode_system),
                            selected = state.darkMode == "system",
                            onClick = soundClick { viewModel.onEvent(SettingsEvent.SetDarkMode("system")) }
                    )
                    DarkModeChip(
                            label = stringResource(R.string.settings_dark_mode_light),
                            selected = state.darkMode == "light",
                            onClick = soundClick { viewModel.onEvent(SettingsEvent.SetDarkMode("light")) }
                    )
                    DarkModeChip(
                            label = stringResource(R.string.settings_dark_mode_dark),
                            selected = state.darkMode == "dark",
                            onClick = soundClick { viewModel.onEvent(SettingsEvent.SetDarkMode("dark")) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // ── 语言 ──
                SettingsSectionHeader(
                        icon = { Icon(Icons.Default.Language, contentDescription = null) },
                        title = stringResource(R.string.settings_language)
                )
                Spacer(modifier = Modifier.height(12.dp))

                val languages =
                        listOf(
                                "system" to stringResource(R.string.settings_language_system),
                                "zh" to "简体中文",
                                "en" to "English",
                                "ja" to "日本語"
                        )

                Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                ) {
                    languages.forEach { (key, label) ->
                        FilterChip(
                                selected = state.language == key,
                                onClick = soundClick { viewModel.onEvent(SettingsEvent.SetLanguage(key)) },
                                label = {
                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ─── 子组件 ───

@Composable
private fun SettingsSectionHeader(icon: @Composable () -> Unit, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ThemeColorCircle(
        themeColor: ThemeColor,
        isSelected: Boolean,
        label: String,
        onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
                modifier =
                        Modifier.size(48.dp)
                                .clip(CircleShape)
                                .background(themeColor.seedColor)
                                .then(
                                        if (isSelected)
                                                Modifier.border(
                                                        3.dp,
                                                        MaterialTheme.colorScheme.onSurface,
                                                        CircleShape
                                                )
                                        else Modifier
                                )
                                .clickable(onClick = soundClick { onClick() }),
                contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DarkModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label, style = MaterialTheme.typography.labelMedium) }
    )
}

@Composable
private fun getThemeColorName(color: ThemeColor): String {
    return when (color) {
        ThemeColor.DYNAMIC -> stringResource(R.string.theme_dynamic)
        ThemeColor.TEAL -> stringResource(R.string.theme_teal)
        ThemeColor.BLUE -> stringResource(R.string.theme_blue)
        ThemeColor.PURPLE -> stringResource(R.string.theme_purple)
        ThemeColor.PINK -> stringResource(R.string.theme_pink)
        ThemeColor.ORANGE -> stringResource(R.string.theme_orange)
        ThemeColor.GREEN -> stringResource(R.string.theme_green)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ThemeColorCirclePreview() {
    MaterialTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            ThemeColorCircle(themeColor = ThemeColor.BLUE, isSelected = true, label = "Blue", onClick = {})
            ThemeColorCircle(themeColor = ThemeColor.PINK, isSelected = false, label = "Pink", onClick = {})
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun DarkModeChipPreview() {
    MaterialTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            DarkModeChip(label = "System", selected = true, onClick = {})
            DarkModeChip(label = "Dark", selected = false, onClick = {})
        }
    }
}
