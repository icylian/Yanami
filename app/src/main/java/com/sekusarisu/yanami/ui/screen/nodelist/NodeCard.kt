package com.sekusarisu.yanami.ui.screen.nodelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.Node

/**
 * 节点卡片组件
 *
 * 显示节点名称、地区、在线状态（含运行时长）、CPU/RAM/Disk 环形进度条、 网络速度和上下传总流量。支持全局 compact/expand 模式。
 */
@Composable
fun NodeCard(node: Node, onClick: () -> Unit, isExpanded: Boolean, modifier: Modifier = Modifier) {
        Card(
                onClick = onClick,
                modifier = modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        // ── 第一行：旗帜 + 名称 + 运行时长 + 在线状态 ──
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // 地区旗帜
                                Text(
                                        text = node.region.ifBlank { "🌐" },
                                        style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // 节点名称
                                Text(
                                        text = node.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                )
                                // 运行时长（在线时显示）
                                if (node.isOnline) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        UptimeBadge(uptime = node.uptime)
                                        Spacer(modifier = Modifier.width(6.dp))
                                }
                                // 在线状态标识
                                StatusBadge(isOnline = node.isOnline)
                        }

                        if (!node.isOnline) {
                                // 离线节点不显示详细数据
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = node.os,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                                return@Card
                        }

                        // ── 可展开区域 ──
                        AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                        ) {
                                Column {
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // ── CPU / RAM / Disk 环形进度条 ──
                                        val memPercent =
                                                if (node.memTotal > 0)
                                                        (node.memUsed.toDouble() / node.memTotal *
                                                                100)
                                                else 0.0
                                        val diskPercent =
                                                if (node.diskTotal > 0)
                                                        (node.diskUsed.toDouble() / node.diskTotal *
                                                                100)
                                                else 0.0

                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.Top
                                        ) {
                                                CircularUsageIndicator(
                                                        label = "CPU",
                                                        percent = node.cpuUsage,
                                                        detail = "${node.cpuCores} Core",
                                                        progressColor = getUsageColor(node.cpuUsage)
                                                )
                                                CircularUsageIndicator(
                                                        label = "RAM",
                                                        percent = memPercent,
                                                        detail = formatBytes(node.memTotal),
                                                        progressColor = getUsageColor(memPercent)
                                                )
                                                CircularUsageIndicator(
                                                        label = "DISK",
                                                        percent = diskPercent,
                                                        detail = formatBytes(node.diskTotal),
                                                        progressColor = getUsageColor(diskPercent)
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // ── 底部行：网络速度 + 总流量 ──
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                // 实时网速
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Icon(
                                                                Icons.Default.ArrowUpward,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string.node_upload
                                                                        ),
                                                                modifier = Modifier.size(14.dp),
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                                        Text(
                                                                text = formatSpeed(node.netOut),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                                Icons.Default.ArrowDownward,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string
                                                                                        .node_download
                                                                        ),
                                                                modifier = Modifier.size(14.dp),
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .secondary
                                                        )
                                                        Text(
                                                                text = formatSpeed(node.netIn),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }

                                                // 总流量
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text =
                                                                        "↑ ${formatBytes(node.netTotalUp)}",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                                text =
                                                                        "↓ ${formatBytes(node.netTotalDown)}",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

// ─── 子组件 ───

/** 环形进度指示器 */
@Composable
private fun CircularUsageIndicator(
        label: String,
        percent: Double,
        detail: String,
        progressColor: Color,
        ringSize: Dp = 72.dp,
        strokeWidth: Dp = 6.dp,
        modifier: Modifier = Modifier
) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ringSize)) {
                        val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        val animatedSweep by animateFloatAsState(
                                targetValue = (percent / 100.0 * 360.0).toFloat().coerceIn(0f, 360f),
                                animationSpec = tween(durationMillis = 600),
                                label = "sweep"
                        )
                        Canvas(modifier = Modifier.size(ringSize)) {
                                val stroke = strokeWidth.toPx()
                                val arcSize = size.width - stroke
                                val topLeft = Offset(stroke / 2f, stroke / 2f)
                                // 背景轨道
                                drawArc(
                                        color = trackColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = Size(arcSize, arcSize),
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                // 进度弧（带动画）
                                drawArc(
                                        color = progressColor,
                                        startAngle = -90f,
                                        sweepAngle = animatedSweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = Size(arcSize, arcSize),
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                        }
                        // 环内文字
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                        text =
                                                String.format(
                                                        "%.0f%%",
                                                        percent.coerceIn(0.0, 100.0)
                                                ),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                        }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // 底部详情
                Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
        }
}

/** 运行时长标记 */
@Composable
private fun UptimeBadge(uptime: Long) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        Icons.Default.Schedule,
                        contentDescription = stringResource(R.string.node_uptime),
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                        text = formatUptime(uptime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }
}

@Composable
private fun StatusBadge(isOnline: Boolean) {
        val bgColor =
                if (isOnline) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
        val textColor =
                if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer
        val text =
                if (isOnline) stringResource(R.string.node_status_online)
                else stringResource(R.string.node_status_offline)

        Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).then(Modifier.padding(0.dp)),
                contentAlignment = Alignment.Center
        ) {
                androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                ) {
                        Text(
                                text = text,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                }
        }
}

// ─── 工具函数 ───

@Composable
fun getUsageColor(percent: Double): Color {
        return when {
                percent < 50 -> MaterialTheme.colorScheme.primary
                percent < 80 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
        }
}

/** 格式化字节数（自动选择 KB/MB/GB/TB） */
fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
                value /= 1024
                unitIndex++
        }
        return if (value >= 100 || unitIndex == 0) {
                String.format("%.0f %s", value, units[unitIndex])
        } else if (value >= 10) {
                String.format("%.1f %s", value, units[unitIndex])
        } else {
                String.format("%.2f %s", value, units[unitIndex])
        }
}

/** 格式化网络速度 */
fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 B/s"
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
        var value = bytesPerSec.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
                value /= 1024
                unitIndex++
        }
        return if (value >= 100) {
                String.format("%.0f %s", value, units[unitIndex])
        } else if (value >= 10) {
                String.format("%.1f %s", value, units[unitIndex])
        } else {
                String.format("%.2f %s", value, units[unitIndex])
        }
}

/** 格式化运行时长 */
fun formatUptime(seconds: Long): String {
        if (seconds <= 0) return "0s"
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val mins = (seconds % 3600) / 60

        return when {
                days > 0 -> "${days}d ${hours}h"
                hours > 0 -> "${hours}h ${mins}m"
                else -> "${mins}m"
        }
}

@Preview(showBackground = true)
@Composable
fun NodeCardPreview() {
    val sampleNode = Node(
        uuid = "1",
        name = "JP-Tokyo",
        region = "🇯🇵",
        group = "Default",
        isOnline = true,
        cpuUsage = 45.0,
        memUsed = 512L * 1024 * 1024,
        memTotal = 1024L * 1024 * 1024,
        swapUsed = 0,
        swapTotal = 0,
        diskUsed = 10L * 1024 * 1024 * 1024,
        diskTotal = 20L * 1024 * 1024 * 1024,
        netIn = 1024 * 1024,
        netOut = 2048 * 1024,
        netTotalUp = 100L * 1024 * 1024 * 1024,
        netTotalDown = 200L * 1024 * 1024 * 1024,
        uptime = 3600 * 24 * 5,
        os = "Ubuntu",
        cpuName = "Intel Xeon",
        cpuCores = 2,
        weight = 0,
        load1 = 0.5,
        load5 = 0.4,
        load15 = 0.3,
        process = 100,
        connectionsTcp = 50,
        connectionsUdp = 10,
        kernelVersion = "5.4.0",
        virtualization = "KVM",
        arch = "amd64",
        gpuName = ""
    )
    MaterialTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            NodeCard(node = sampleNode, onClick = {}, isExpanded = false)
            NodeCard(node = sampleNode, onClick = {}, isExpanded = true)
            NodeCard(node = sampleNode.copy(isOnline = false), onClick = {}, isExpanded = false)
        }
    }
}
