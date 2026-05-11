package com.sekusarisu.yanami.ui.screen.nodedetail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.domain.model.calculateTrafficLimitUsage
import com.sekusarisu.yanami.ui.screen.nodelist.formatBytes
import com.sekusarisu.yanami.ui.screen.nodelist.formatUptime
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.traffic.formatTrafficLimitDetail
import com.sekusarisu.yanami.ui.traffic.formatTrafficLimitTypeLabel

@Composable
internal fun ServerInfoCard(node: Node) {
    val adaptiveInfo = rememberAdaptiveLayoutInfo()
    val trafficLimitUsage = node.calculateTrafficLimitUsage()
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = node.region, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = node.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(node.isOnline)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            if (adaptiveInfo.isTabletLandscape) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        InfoRow(stringResource(R.string.node_detail_os), node.os)
                        if (node.kernelVersion.isNotBlank()) {
                            InfoRow(stringResource(R.string.node_detail_kernel), node.kernelVersion)
                        }
                        if (node.arch.isNotBlank()) {
                            InfoRow(
                                    stringResource(R.string.node_detail_platform),
                                    "${node.virtualization} / ${node.arch}"
                            )
                        }
                        InfoRow(
                                stringResource(R.string.node_detail_cpu),
                                "${node.cpuName} (${node.cpuCores} ${stringResource(R.string.node_detail_cores)})"
                        )
                        InfoRow(
                                stringResource(R.string.node_detail_load),
                                "%.2f / %.2f / %.2f".format(node.load1, node.load5, node.load15)
                        )
                        InfoRow(
                                stringResource(R.string.node_detail_net_traffic),
                                "↑ ${formatBytes(node.netTotalUp)}  ↓ ${formatBytes(node.netTotalDown)}"
                        )
                        InfoRow(stringResource(R.string.node_detail_uptime), formatUptime(node.uptime))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        UsageBar(
                                label = stringResource(R.string.node_detail_cpu_usage),
                                percent = node.cpuUsage,
                                detail = "%.1f%%".format(node.cpuUsage)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UsageBar(
                                label = stringResource(R.string.node_detail_ram),
                                percent =
                                        if (node.memTotal > 0) {
                                            node.memUsed.toDouble() / node.memTotal * 100
                                        } else {
                                            0.0
                                        },
                                detail = "${formatBytes(node.memUsed)} / ${formatBytes(node.memTotal)}"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (node.swapTotal > 0) {
                            UsageBar(
                                    label = "Swap",
                                    percent = node.swapUsed.toDouble() / node.swapTotal * 100,
                                    detail = "${formatBytes(node.swapUsed)} / ${formatBytes(node.swapTotal)}"
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        UsageBar(
                                label = stringResource(R.string.node_detail_disk),
                                percent =
                                        if (node.diskTotal > 0) {
                                            node.diskUsed.toDouble() / node.diskTotal * 100
                                        } else {
                                            0.0
                                        },
                                detail = "${formatBytes(node.diskUsed)} / ${formatBytes(node.diskTotal)}"
                        )
                        if (trafficLimitUsage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            UsageBar(
                                    label =
                                            "${stringResource(R.string.node_detail_traffic_limit)} · ${formatTrafficLimitTypeLabel(trafficLimitUsage.type)}",
                                    percent = trafficLimitUsage.usagePercent,
                                    detail = formatTrafficLimitDetail(trafficLimitUsage)
                            )
                        }
                    }
                }
            } else {
                InfoRow(stringResource(R.string.node_detail_os), node.os)
                if (node.kernelVersion.isNotBlank()) {
                    InfoRow(stringResource(R.string.node_detail_kernel), node.kernelVersion)
                }
                if (node.arch.isNotBlank()) {
                    InfoRow(
                            stringResource(R.string.node_detail_platform),
                            "${node.virtualization} / ${node.arch}"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                        stringResource(R.string.node_detail_cpu),
                        "${node.cpuName} (${node.cpuCores} ${stringResource(R.string.node_detail_cores)})"
                )
                UsageBar(
                        label = stringResource(R.string.node_detail_cpu_usage),
                        percent = node.cpuUsage,
                        detail = "%.1f%%".format(node.cpuUsage)
                )
                Spacer(modifier = Modifier.height(6.dp))
                UsageBar(
                        label = stringResource(R.string.node_detail_ram),
                        percent =
                                if (node.memTotal > 0) node.memUsed.toDouble() / node.memTotal * 100
                                else 0.0,
                        detail = "${formatBytes(node.memUsed)} / ${formatBytes(node.memTotal)}"
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (node.swapTotal > 0) {
                    UsageBar(
                            label = "Swap",
                            percent = node.swapUsed.toDouble() / node.swapTotal * 100,
                            detail = "${formatBytes(node.swapUsed)} / ${formatBytes(node.swapTotal)}"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                UsageBar(
                        label = stringResource(R.string.node_detail_disk),
                        percent =
                                if (node.diskTotal > 0) node.diskUsed.toDouble() / node.diskTotal * 100
                                else 0.0,
                        detail = "${formatBytes(node.diskUsed)} / ${formatBytes(node.diskTotal)}"
                )
                if (trafficLimitUsage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    UsageBar(
                            label =
                                    "${stringResource(R.string.node_detail_traffic_limit)} · ${formatTrafficLimitTypeLabel(trafficLimitUsage.type)}",
                            percent = trafficLimitUsage.usagePercent,
                            detail = formatTrafficLimitDetail(trafficLimitUsage)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                        stringResource(R.string.node_detail_net_traffic),
                        "↑ ${formatBytes(node.netTotalUp)}  ↓ ${formatBytes(node.netTotalDown)}"
                )
                InfoRow(
                        stringResource(R.string.node_detail_load),
                        "%.2f / %.2f / %.2f".format(node.load1, node.load5, node.load15)
                )
                InfoRow(stringResource(R.string.node_detail_uptime), formatUptime(node.uptime))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.35f)
        )
        Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.65f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
        )
    }
}

@Composable
internal fun UsageBar(label: String, percent: Double, detail: String) {
    val animatedProgress by animateFloatAsState(
            targetValue = (percent / 100).toFloat().coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 600),
            label = "progress"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = getUsageColor(percent),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
private fun StatusChip(isOnline: Boolean) {
    Surface(
            shape = RoundedCornerShape(12.dp),
            color =
                    if (isOnline) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
                text =
                        if (isOnline) stringResource(R.string.node_status_online)
                        else stringResource(R.string.node_status_offline),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color =
                        if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun getUsageColor(percent: Double): Color = when {
    percent < 60 -> MaterialTheme.colorScheme.primary
    percent < 85 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}
