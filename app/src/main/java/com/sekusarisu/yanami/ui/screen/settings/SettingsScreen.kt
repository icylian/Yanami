package com.sekusarisu.yanami.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.soundClick
import com.sekusarisu.yanami.ui.theme.ThemeColor

/** 视觉与样式设置页面 */
class SettingsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SettingsViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
            Column(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(innerPadding)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp)
            ) {
                // ── 预览卡片：模拟总览 + NodeCard ──
                MockOverviewCard()

                Spacer(modifier = Modifier.height(8.dp))

                MockNodeCardPreview()

                Spacer(modifier = Modifier.height(20.dp))

                // ── 主题颜色 ──
                FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                ) {
                    ThemeColor.entries.forEach { color ->
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

                // ── 深色模式 ──
                SettingsListItem(
                        icon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        title = stringResource(R.string.settings_dark_mode)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 语言 ──
                SettingsListItem(
                        icon = { Icon(Icons.Default.Language, contentDescription = null) },
                        title = stringResource(R.string.settings_language)
                ) {
                    val languages =
                            listOf(
                                    "system" to stringResource(R.string.settings_language_system),
                                    "zh" to "简体中文",
                                    "en" to "English",
                                    "ja" to "日本語"
                            )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 字体大小 ──
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
                                onValueChange = { viewModel.onEvent(SettingsEvent.SetFontScale(it)) },
                                valueRange = 0.8f..1.4f,
                                steps = 11,
                                modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 自动进入节点列表 ──
                SettingsListItem(
                        icon = { Icon(Icons.Default.RocketLaunch, contentDescription = null) },
                        title = stringResource(R.string.settings_auto_enter_nodelist)
                ) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                                text = stringResource(R.string.settings_auto_enter_nodelist_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                                checked = state.autoEnterNodeList,
                                onCheckedChange = { viewModel.onEvent(SettingsEvent.SetAutoEnterNodeList(it)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ─── 子组件 ───

/** 设置项：leading icon + title，下方放置 content */
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

/** 模拟 OverviewCard 预览——展示总览卡片主题效果 */
@Composable
private fun MockOverviewCard() {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // 节点统计行
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                MockStatItem(
                        label = stringResource(R.string.node_stat_total),
                        value = "3",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                MockStatItem(
                        label = stringResource(R.string.node_stat_online),
                        value = "2",
                        color = MaterialTheme.colorScheme.primary
                )
                MockStatItem(
                        label = stringResource(R.string.node_stat_offline),
                        value = "1",
                        color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 网络信息
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
            ) {
                // 实时速度列
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                            text = stringResource(R.string.node_net_speed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                                text = "5.00 MB/s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                                text = "12.5 MB/s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // 总流量列
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                            text = stringResource(R.string.node_net_traffic),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                                text = "1.50 TB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                                text = "3.20 TB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

/** 模拟 NodeCard 预览——展示当前主题效果 */
@Composable
private fun MockNodeCardPreview() {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 第一行：旗帜 + 名称 + 运行时长 + 在线状态
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🇯🇵", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = "JP-Tokyo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                // 运行时长
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                            text = "5d 0h",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                // 在线状态
                Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                            text = stringResource(R.string.node_status_online),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CPU / RAM / Disk 环形进度条
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
            ) {
                MockCircularIndicator(
                        label = "CPU",
                        percent = 45.0,
                        detail = "2 Core",
                        progressColor = MaterialTheme.colorScheme.primary
                )
                MockCircularIndicator(
                        label = "RAM",
                        percent = 50.0,
                        detail = "1.00 GB",
                        progressColor = MaterialTheme.colorScheme.tertiary
                )
                MockCircularIndicator(
                        label = "DISK",
                        percent = 50.0,
                        detail = "20.0 GB",
                        progressColor = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 底部行：网络速度 + 总流量
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                            text = "2.00 MB/s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                            text = "1.00 MB/s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = "↑ 100 GB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = "↓ 200 GB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** 预览用环形进度指示器（静态，不依赖动画） */
@Composable
private fun MockCircularIndicator(
        label: String,
        percent: Double,
        detail: String,
        progressColor: Color,
) {
    val ringSize = 72.dp
    val strokeWidth = 6.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ringSize)) {
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            val sweep = (percent / 100.0 * 360.0).toFloat().coerceIn(0f, 360f)
            Canvas(modifier = Modifier.size(ringSize)) {
                val stroke = strokeWidth.toPx()
                val arcSize = size.width - stroke
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = String.format("%.0f%%", percent),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
        )
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
        ThemeColor.RED -> stringResource(R.string.theme_red)
        ThemeColor.GREEN_MTB -> stringResource(R.string.theme_green_mtb)
        ThemeColor.BLUE_MTB -> stringResource(R.string.theme_blue_mtb)
        ThemeColor.YELLOW -> stringResource(R.string.theme_yellow)
    }
}

@Composable
private fun MockStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
        )
        Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.8f)
        )
    }
}
