package com.sekusarisu.yanami.ui.screen.nodedetail

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.sekusarisu.yanami.data.local.preferences.UserPreferences
import com.sekusarisu.yanami.data.local.preferences.UserPreferencesRepository
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.ui.screen.soundClick
import com.sekusarisu.yanami.domain.model.LoadRecord
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.domain.model.PingRecord
import com.sekusarisu.yanami.domain.model.PingTask
import com.sekusarisu.yanami.domain.model.calculateTrafficLimitUsage
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.nodelist.formatBytes
import com.sekusarisu.yanami.ui.screen.nodelist.formatUptime
import com.sekusarisu.yanami.ui.screen.server.AddServerScreen
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginScreen
import com.sekusarisu.yanami.ui.screen.terminal.SshTerminalScreen
import com.sekusarisu.yanami.ui.traffic.formatTrafficLimitDetail
import com.sekusarisu.yanami.ui.traffic.formatTrafficLimitTypeLabel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.ZoneId


@Composable
internal fun NodeDetailContent(
        state: NodeDetailContract.State,
        chartAnimationEnabled: Boolean,
        isTabletLandscape: Boolean,
        onLoadHoursChanged: (Int) -> Unit,
        onPingHoursChanged: (Int) -> Unit
) {
    val node = state.node ?: return

    val loadRecords =
            if (state.selectedLoadHours == 0) state.realtimeLoadRecords
            else state.loadRecords
    val loadIsLoading =
            if (state.selectedLoadHours == 0) false else state.isLoadRecordsLoading

    AdaptiveContentPane(maxWidth = if (isTabletLandscape) 1440.dp else 920.dp) {
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item { ServerInfoCard(node) }

            item {
                ChartSectionHeader(
                        title = stringResource(R.string.node_detail_load_chart),
                        selectedHours = state.selectedLoadHours,
                        onHoursChanged = onLoadHoursChanged,
                        showRealtime = true
                )
            }

            if (loadIsLoading) {
                item {
                    ChartSectionSurface {
                        Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(modifier = Modifier.size(32.dp)) }
                    }
                }
            } else if (loadRecords.isEmpty()) {
                item {
                    ChartSectionSurface {
                        Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center
                        ) {
                            Text(
                                    text =
                                            if (state.selectedLoadHours == 0) {
                                                stringResource(R.string.node_detail_loading)
                                            } else {
                                                stringResource(R.string.node_detail_no_records)
                                            },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val times = loadRecords.map { it.time }
                if (isTabletLandscape) {
                    item {
                        WideChartRow(
                                first = {
                                    ChartCard(
                                            title = stringResource(R.string.node_detail_cpu_usage),
                                            data = loadRecords.map { it.cpu },
                                            times = times,
                                            color = MaterialTheme.colorScheme.primary,
                                            suffix = "%",
                                            chartAnimationEnabled = chartAnimationEnabled
                                    )
                                },
                                second = {
                                    ChartCard(
                                            title = stringResource(R.string.node_detail_ram),
                                            data = loadRecords.map { it.ramPercent },
                                            times = times,
                                            color = MaterialTheme.colorScheme.primary,
                                            suffix = "%",
                                            chartAnimationEnabled = chartAnimationEnabled
                                    )
                                }
                        )
                    }

                    item {
                        WideChartRow(
                                first = {
                                    NetworkChartCard(
                                            title = stringResource(R.string.node_detail_net_speed),
                                            netInData = loadRecords.map { it.netIn.toDouble() },
                                            netOutData = loadRecords.map { it.netOut.toDouble() },
                                            times = times,
                                            chartAnimationEnabled = chartAnimationEnabled
                                    )
                                },
                                second = {
                                    ConnectionChartCard(
                                            title = stringResource(R.string.node_detail_connections),
                                            tcpData = loadRecords.map { it.connections },
                                            udpData = loadRecords.map { it.connectionsUdp },
                                            times = times,
                                            suffix = "",
                                            chartAnimationEnabled = chartAnimationEnabled
                                    )
                                }
                        )
                    }

                    item {
                        ChartSectionSurface {
                            ChartCard(
                                    title = stringResource(R.string.node_detail_process),
                                    data = loadRecords.map { it.process.toDouble() },
                                    times = times,
                                    color = MaterialTheme.colorScheme.primary,
                                    suffix = "",
                                    chartAnimationEnabled = chartAnimationEnabled
                            )
                        }
                    }
                } else {
                    item {
                        ChartSectionSurface {
                            ChartCard(
                                    title = stringResource(R.string.node_detail_cpu_usage),
                                    data = loadRecords.map { it.cpu },
                                    times = times,
                                    color = MaterialTheme.colorScheme.primary,
                                    suffix = "%",
                                    chartAnimationEnabled = chartAnimationEnabled
                            )
                        }
                    }

                    item {
                        ChartSectionSurface {
                            ChartCard(
                                    title = stringResource(R.string.node_detail_ram),
                                    data = loadRecords.map { it.ramPercent },
                                    times = times,
                                    color = MaterialTheme.colorScheme.primary,
                                    suffix = "%",
                                    chartAnimationEnabled = chartAnimationEnabled
                            )
                        }
                    }

                    item {
                        ChartSectionSurface {
                            NetworkChartCard(
                                    title = stringResource(R.string.node_detail_net_speed),
                                    netInData = loadRecords.map { it.netIn.toDouble() },
                                    netOutData = loadRecords.map { it.netOut.toDouble() },
                                    times = times,
                                    chartAnimationEnabled = chartAnimationEnabled
                            )
                        }
                    }

                    item {
                        ChartSectionSurface {
                            ConnectionChartCard(
                                    title = stringResource(R.string.node_detail_connections),
                                    tcpData = loadRecords.map { it.connections },
                                    udpData = loadRecords.map { it.connectionsUdp },
                                    times = times,
                                    suffix = "",
                                    chartAnimationEnabled = chartAnimationEnabled
                            )
                        }
                    }

                    item {
                        ChartSectionSurface {
                            ChartCard(
                                    title = stringResource(R.string.node_detail_process),
                                    data = loadRecords.map { it.process.toDouble() },
                                    times = times,
                                    color = MaterialTheme.colorScheme.primary,
                                    suffix = "",
                                    chartAnimationEnabled = chartAnimationEnabled
                            )
                        }
                    }
                }
            }

            item {
                ChartSectionHeader(
                        title = stringResource(R.string.node_detail_ping_chart),
                        selectedHours = state.selectedPingHours,
                        onHoursChanged = onPingHoursChanged,
                        showRealtime = false
                )
            }

            if (state.isPingRecordsLoading) {
                item {
                    ChartSectionSurface {
                        Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(modifier = Modifier.size(32.dp)) }
                    }
                }
            } else if (state.pingRecords.isEmpty()) {
                item {
                    ChartSectionSurface {
                        Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center
                        ) {
                            Text(
                                    text = stringResource(R.string.node_detail_no_records),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val pingCharts =
                        state.pingTasks.mapNotNull { task ->
                            val taskRecords = state.pingRecords.filter { it.taskId == task.id }
                            val taskValues = taskRecords.map { it.value }
                            val taskTimes = taskRecords.map { it.time }
                            if (taskValues.size >= 2) Triple(task.id, task, taskValues to taskTimes)
                            else null
                        }

                if (isTabletLandscape) {
                    pingCharts.chunked(2).forEachIndexed { index, chunk ->
                        item(key = "ping_row_$index") {
                            WideChartRow(
                                    first = {
                                        val (_, task, records) = chunk[0]
                                        PingTaskChart(
                                                task = task,
                                                values = records.first,
                                                times = records.second,
                                                chartAnimationEnabled = chartAnimationEnabled
                                        )
                                    },
                                    second =
                                            chunk.getOrNull(1)?.let { secondChart ->
                                                {
                                                    PingTaskChart(
                                                            task = secondChart.second,
                                                            values = secondChart.third.first,
                                                            times = secondChart.third.second,
                                                            chartAnimationEnabled = chartAnimationEnabled
                                                    )
                                                }
                                            }
                            )
                        }
                    }
                } else {
                    pingCharts.forEach { (taskId, task, records) ->
                        item(key = "ping_$taskId") {
                            ChartSectionSurface {
                                PingTaskChart(
                                        task = task,
                                        values = records.first,
                                        times = records.second,
                                        chartAnimationEnabled = chartAnimationEnabled
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ─── 4.1 服务器信息总览 ───

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
            // 标题行：地区 + 名称 + 状态
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

// ─── 图表通用容器 ───

/** 图表分区标题卡片：标题 + 时间范围选择器 */
@Composable
internal fun ChartSectionHeader(
        title: String,
        selectedHours: Int,
        onHoursChanged: (Int) -> Unit,
        showRealtime: Boolean = false
) {
    Surface(
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            tonalElevation = 1.dp
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )
            TimeRangeSelector(
                    selectedHours = selectedHours,
                    onHoursChanged = onHoursChanged,
                    showRealtime = showRealtime
            )
        }
    }
}

/** 单个图表项的 Surface 包装 */
@Composable
internal fun ChartSectionSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun WideChartRow(
        first: @Composable () -> Unit,
        second: (@Composable () -> Unit)? = null
) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
    ) {
        ChartSectionSurface(modifier = Modifier.weight(1f), content = first)
        if (second != null) {
            ChartSectionSurface(modifier = Modifier.weight(1f), content = second)
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// ─── 4.3 Ping 单任务图表 ───

@Composable
internal fun PingTaskChart(
        task: PingTask,
        values: List<Double>,
        times: List<String>,
        chartAnimationEnabled: Boolean = true
) {
    Column {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = task.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                    text = "%.0fms".format(task.latest) + " / "
                            + "%.1f%%".format(task.loss),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        val modelProducer = remember { CartesianChartModelProducer() }
        LaunchedEffect(values) {
            modelProducer.runTransaction { lineSeries { series(values) } }
        }

        val yAxisFormatter = remember {
            CartesianValueFormatter { _, value, _ -> "%.0fms".format(value) }
        }
        val xAxisFormatter =
                remember(times) {
                    CartesianValueFormatter { _, value, _ ->
                        val index = value.toInt().coerceIn(0, times.size - 1)
                        if (index in times.indices) parseTimeLabel(times[index]) else ""
                    }
                }

        val pingLine = rememberThemedLine(MaterialTheme.colorScheme.tertiary)
        CartesianChartHost(
                chart =
                        rememberCartesianChart(
                                rememberLineCartesianLayer(LineCartesianLayer.LineProvider.series(pingLine)),
                                startAxis =
                                        VerticalAxis.rememberStart(
                                                valueFormatter = yAxisFormatter
                                        ),
                                bottomAxis =
                                        HorizontalAxis.rememberBottom(
                                                valueFormatter = xAxisFormatter
                                        )
                        ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(140.dp),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                zoomState =
                        rememberVicoZoomState(
                                zoomEnabled = false,
                                initialZoom = Zoom.Content
                        ),
                animationSpec = if (chartAnimationEnabled) tween(durationMillis = 500) else null,
                animateIn = chartAnimationEnabled
        )
    }
}

/** 解析 ISO 时间字符串为 HH:mm 格式 */
private fun parseTimeLabel(isoTime: String): String {
    return try {
        val instant = Instant.parse(isoTime)
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        "%02d:%02d".format(localTime.hour, localTime.minute)
    } catch (_: Exception) {
        ""
    }
}

/** 将字节/秒格式化为人可读速度 */
private fun formatChartSpeed(bytesPerSec: Double): String {
    return when {
        bytesPerSec >= 1_073_741_824 -> "%.1f GB/s".format(bytesPerSec / 1_073_741_824)
        bytesPerSec >= 1_048_576 -> "%.1f MB/s".format(bytesPerSec / 1_048_576)
        bytesPerSec >= 1024 -> "%.0f KB/s".format(bytesPerSec / 1024)
        else -> "%.0f B/s".format(bytesPerSec)
    }
}

@Composable
internal fun ChartCard(
        title: String,
        data: List<Double>,
        times: List<String>,
        color: Color,
        suffix: String,
        chartAnimationEnabled: Boolean = true
) {
    val themedLine = rememberThemedLine(color)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "%.0f".format(data.last()) + suffix,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        if (data.size >= 2) {
            val modelProducer = remember { CartesianChartModelProducer() }
            LaunchedEffect(data) { modelProducer.runTransaction { lineSeries { series(data) } } }

            // Y 轴 formatter
            val yAxisFormatter =
                    remember(suffix) {
                        CartesianValueFormatter { _, value, _ -> "%.0f".format(value) + suffix }
                    }

            // X 轴 formatter（时间）
            val xAxisFormatter =
                    remember(times) {
                        CartesianValueFormatter { _, value, _ ->
                            val index = value.toInt().coerceIn(0, times.size - 1)
                            if (index in times.indices) parseTimeLabel(times[index]) else ""
                        }
                    }

            CartesianChartHost(
                    chart =
                            rememberCartesianChart(
                                    rememberLineCartesianLayer(
                                            LineCartesianLayer.LineProvider.series(themedLine)
                                    ),
                                    startAxis =
                                            VerticalAxis.rememberStart(
                                                    valueFormatter = yAxisFormatter
                                            ),
                                    bottomAxis =
                                            HorizontalAxis.rememberBottom(
                                                    valueFormatter = xAxisFormatter
                                            )
                            ),
                    modelProducer = modelProducer,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    scrollState = rememberVicoScrollState(scrollEnabled = false),
                    zoomState =
                            rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
                    animationSpec = if (chartAnimationEnabled) tween(durationMillis = 500) else null,
                    animateIn = chartAnimationEnabled
            )
        } else {
            Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
            ) {
                Text(
                        text = stringResource(R.string.node_detail_insufficient_data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun ConnectionChartCard(
    title: String,
    tcpData: List<Int>,
    udpData: List<Int>,
    times: List<String>,
    suffix: String,
    chartAnimationEnabled: Boolean = true
) {
    val tcpLine = rememberThemedLine(MaterialTheme.colorScheme.primary)
    val udpLine = rememberThemedLine(MaterialTheme.colorScheme.tertiary)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("TCP " + tcpData.last().toString() + suffix)
                    }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(" / ")
                    }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                        append("UDP " + udpData.last().toString() + suffix)
                    }
                },
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        if (tcpData.size >= 2) {
            val modelProducer = remember { CartesianChartModelProducer() }
            LaunchedEffect(tcpData, udpData) {
                modelProducer.runTransaction {
                    lineSeries {
                        series(tcpData) // tcp连接数
                        series(udpData) // dup连接数
                    }
                }
            }

            // Y 轴 formatter
            val yAxisFormatter =
                remember(suffix) {
                    CartesianValueFormatter { _, value, _ -> "%.0f".format(value) + suffix }
                }

            // X 轴 formatter（时间）
            val xAxisFormatter =
                remember(times) {
                    CartesianValueFormatter { _, value, _ ->
                        val index = value.toInt().coerceIn(0, times.size - 1)
                        if (index in times.indices) parseTimeLabel(times[index]) else ""
                    }
                }

            CartesianChartHost(
                chart =
                    rememberCartesianChart(
                        rememberLineCartesianLayer(LineCartesianLayer.LineProvider.series(tcpLine, udpLine)),
                        startAxis =
                            VerticalAxis.rememberStart(
                                valueFormatter = yAxisFormatter
                            ),
                        bottomAxis =
                            HorizontalAxis.rememberBottom(
                                valueFormatter = xAxisFormatter
                            )
                    ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(160.dp),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                zoomState =
                    rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
                animationSpec = if (chartAnimationEnabled) tween(durationMillis = 500) else null,
                animateIn = chartAnimationEnabled
            )

            // 图例
//            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
//                Text(
//                    text = "${stringResource(R.string.tcp_connections)}",
//                    style = MaterialTheme.typography.labelSmall,
//                    color = MaterialTheme.colorScheme.primary
//                )
//                Spacer(modifier = Modifier.width(16.dp))
//                Text(
//                    text = "${stringResource(R.string.udp_connections)}",
//                    style = MaterialTheme.typography.labelSmall,
//                    color = MaterialTheme.colorScheme.secondary
//                )
//            }
        }
    }
}

@Composable
internal fun NetworkChartCard(
        title: String,
        netInData: List<Double>,
        netOutData: List<Double>,
        times: List<String>,
        chartAnimationEnabled: Boolean = true
) {
    val upLine = rememberThemedLine(MaterialTheme.colorScheme.primary)
    val downLine = rememberThemedLine(MaterialTheme.colorScheme.tertiary)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                        append("↓ " + formatChartSpeed(netInData.last()))
                    }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(" / ")
                    }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("↑ " + formatChartSpeed(netOutData.last()))
                    }
                },
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        if (netInData.size >= 2) {
            val modelProducer = remember { CartesianChartModelProducer() }
            LaunchedEffect(netInData, netOutData) {
                modelProducer.runTransaction {
                    lineSeries {
                        series(netOutData) // 上行
                        series(netInData) // 下行
                    }
                }
            }

            // Y 轴 formatter（网络速度）
            val yAxisFormatter = remember {
                CartesianValueFormatter { _, value, _ -> formatChartSpeed(value) }
            }

            // X 轴 formatter（时间）
            val xAxisFormatter =
                    remember(times) {
                        CartesianValueFormatter { _, value, _ ->
                            val index = value.toInt().coerceIn(0, times.size - 1)
                            if (index in times.indices) parseTimeLabel(times[index]) else ""
                        }
                    }

            CartesianChartHost(
                    chart =
                            rememberCartesianChart(
                                    rememberLineCartesianLayer(LineCartesianLayer.LineProvider.series(upLine, downLine)),
                                    startAxis =
                                            VerticalAxis.rememberStart(
                                                    valueFormatter = yAxisFormatter
                                            ),
                                    bottomAxis =
                                            HorizontalAxis.rememberBottom(
                                                    valueFormatter = xAxisFormatter
                                            )
                            ),
                    modelProducer = modelProducer,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    scrollState = rememberVicoScrollState(scrollEnabled = false),
                    zoomState =
                            rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
                    animationSpec = if (chartAnimationEnabled) tween(durationMillis = 500) else null,
                    animateIn = chartAnimationEnabled
            )

            // 图例
//            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
//                Text(
//                        text = "↑ ${stringResource(R.string.node_upload)}",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.primary
//                )
//                Spacer(modifier = Modifier.width(16.dp))
//                Text(
//                        text = "↓ ${stringResource(R.string.node_download)}",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.secondary
//                )
//            }
        }
    }
}

// ─── 通用组件 ───

@Composable
private fun TimeRangeSelector(
        selectedHours: Int,
        onHoursChanged: (Int) -> Unit,
        showRealtime: Boolean = false
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showRealtime) {
            FilterChip(
                    selected = selectedHours == 0,
                    onClick = soundClick { onHoursChanged(0) },
                    label = {
                        Text(
                                text = stringResource(R.string.node_detail_realtime),
                                style = MaterialTheme.typography.labelSmall
                        )
                    }
            )
        }
        listOf(1, 6, 24).forEach { hours ->
            FilterChip(
                    selected = selectedHours == hours,
                    onClick = soundClick { onHoursChanged(hours) },
                    label = {
                        Text(text = "${hours}h", style = MaterialTheme.typography.labelSmall)
                    }
            )
        }
    }
}

@Composable
internal fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                    text = stringResource(R.string.node_detail_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun ErrorContent(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = soundClick { onRetry() }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

// ─── 图表线段样式 ───

/**
 * 创建跟随 MD3 主题色的折线样式：实色线段 + 线下渐变阴影。
 * 渐变由 [color]（32% 透明度）渐变至透明，符合 Material 3 图表规范。
 */
@Composable
private fun rememberThemedLine(color: Color): LineCartesianLayer.Line =
        LineCartesianLayer.rememberLine(
                fill = LineCartesianLayer.LineFill.single(Fill(color)),
                areaFill = LineCartesianLayer.AreaFill.single(
                        Fill(Brush.verticalGradient(
                                listOf(color.copy(alpha = 0.32f), Color.Transparent)
                        ))
                )
        )

// ─── 图表工具函数 ───

@Composable
private fun getUsageColor(percent: Double): Color = when {
    percent < 60 -> MaterialTheme.colorScheme.primary
    percent < 85 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

