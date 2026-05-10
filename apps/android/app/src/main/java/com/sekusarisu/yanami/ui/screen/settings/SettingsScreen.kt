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
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.soundClick
import com.sekusarisu.yanami.ui.theme.ThemeColor

class SettingsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SettingsViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val adaptiveInfo = rememberAdaptiveLayoutInfo()

        Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LargeTopAppBar(
                            title = { Text(stringResource(R.string.settings_visual_style)) },
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
                    maxWidth = if (adaptiveInfo.isTabletLandscape) 1200.dp else 900.dp
            ) {
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp)
                ) {
                    if (adaptiveInfo.isTabletLandscape) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(0.95f)) {
                                MockOverviewCard()
                                Spacer(modifier = Modifier.height(8.dp))
                                MockNodeCardPreview()
                            }
                            Column(modifier = Modifier.weight(1.05f)) {
                                SettingsControls(state = state, onEvent = viewModel::onEvent)
                            }
                        }
                    } else {
                        MockOverviewCard()
                        Spacer(modifier = Modifier.height(8.dp))
                        MockNodeCardPreview()
                        Spacer(modifier = Modifier.height(20.dp))
                        SettingsControls(state = state, onEvent = viewModel::onEvent)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsControls(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
    ) {
        ThemeColor.entries.forEach { color ->
            if (color == ThemeColor.DYNAMIC && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return@forEach
            }

            ThemeColorCircle(
                    themeColor = color,
                    isSelected = state.themeColor == color,
                    label = getThemeColorName(color),
                    onClick = soundClick { onEvent(SettingsEvent.SetThemeColor(color)) }
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    SettingsListItem(
            icon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
            title = stringResource(R.string.settings_dark_mode)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DarkModeChip(
                    label = stringResource(R.string.settings_dark_mode_system),
                    selected = state.darkMode == "system",
                    onClick = soundClick { onEvent(SettingsEvent.SetDarkMode("system")) }
            )
            DarkModeChip(
                    label = stringResource(R.string.settings_dark_mode_light),
                    selected = state.darkMode == "light",
                    onClick = soundClick { onEvent(SettingsEvent.SetDarkMode("light")) }
            )
            DarkModeChip(
                    label = stringResource(R.string.settings_dark_mode_dark),
                    selected = state.darkMode == "dark",
                    onClick = soundClick { onEvent(SettingsEvent.SetDarkMode("dark")) }
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    SettingsListItem(
            icon = { Icon(Icons.Default.FormatSize, contentDescription = null) },
            title = stringResource(R.string.settings_font_scale)
    ) {
        Column {
            Text(
                    text = String.format("%d%%", (state.fontScale * 100).toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                    value = state.fontScale,
                    onValueChange = { onEvent(SettingsEvent.SetFontScale(it)) },
                    valueRange = 0.8f..1.4f,
                    steps = 11,
                    modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SettingsListItem(
        icon: @Composable () -> Unit,
        title: String,
        content: @Composable () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
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
                                        if (isSelected) {
                                            Modifier.border(
                                                    3.dp,
                                                    MaterialTheme.colorScheme.onSurface,
                                                    CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
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
        ThemeColor.RED -> stringResource(R.string.theme_red)
        ThemeColor.GREEN_MTB -> stringResource(R.string.theme_green_mtb)
        ThemeColor.BLUE_MTB -> stringResource(R.string.theme_blue_mtb)
        ThemeColor.YELLOW -> stringResource(R.string.theme_yellow)
    }
}
