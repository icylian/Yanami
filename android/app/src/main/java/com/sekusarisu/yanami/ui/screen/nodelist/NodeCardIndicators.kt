package com.sekusarisu.yanami.ui.screen.nodelist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.TrafficLimitUsage
import com.sekusarisu.yanami.ui.traffic.formatTrafficLimitPercent
import com.sekusarisu.yanami.ui.traffic.formatTrafficLimitTypeLabel

@Composable
internal fun CircularUsageIndicator(
        label: String,
        percent: Double,
        detail: String,
        progressColor: Color,
        ringSize: Dp = 72.dp,
        strokeWidth: Dp = 6.dp,
        modifier: Modifier = Modifier
) {
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = modifier
    ) {
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
                        sweepAngle = animatedSweep,
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
                        text = String.format("%.0f%%", percent.coerceIn(0.0, 100.0)),
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
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun UptimeBadge(uptime: Long) {
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
internal fun StatusBadge(isOnline: Boolean) {
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
        Surface(shape = RoundedCornerShape(12.dp), color = bgColor) {
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

@Composable
fun getUsageColor(percent: Double): Color {
    return when {
        percent < 50 -> MaterialTheme.colorScheme.primary
        percent < 80 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
}

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

@Composable
internal fun TrafficLimitMiniIndicator(
        usage: TrafficLimitUsage,
        modifier: Modifier = Modifier
) {
    val progressColor = getUsageColor(usage.usagePercent)
    val animatedSweep by animateFloatAsState(
            targetValue = (usage.usagePercent / 100.0 * 360.0).toFloat().coerceIn(0f, 360f),
            animationSpec = tween(durationMillis = 600),
            label = "trafficLimitSweep"
    )

    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(42.dp)) {
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            Canvas(modifier = Modifier.size(42.dp)) {
                val stroke = 4.dp.toPx()
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
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                        text = formatTrafficLimitTypeLabel(usage.type),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        lineHeight = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                )
                Text(
                        text = formatTrafficLimitPercent(usage.usagePercent),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor,
                        maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
                text = formatBytes(usage.limit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
        )
    }
}
