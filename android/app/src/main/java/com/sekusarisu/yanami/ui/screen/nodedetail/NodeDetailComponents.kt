package com.sekusarisu.yanami.ui.screen.nodedetail

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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.soundClick

@Composable
internal fun NodeDetailContent(
        state: NodeDetailContract.State,
        chartAnimationEnabled: Boolean,
        isTabletLandscape: Boolean,
        onLoadHoursChanged: (Int) -> Unit,
        onPingHoursChanged: (Int) -> Unit
) {
    val node = state.node ?: return
    val loadChartData =
            if (state.selectedLoadHours == 0) {
                state.realtimeLoadChartData
            } else {
                state.loadChartData
            }
    val loadIsLoading = state.selectedLoadHours != 0 && state.isLoadRecordsLoading

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
            } else if (loadChartData.timeLabels.isEmpty()) {
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
                if (isTabletLandscape) {
                    item {
                        WideChartRow(
                                first = {
                                    ChartCard(
                                            title = stringResource(R.string.node_detail_cpu_usage),
                                            data = loadChartData.cpuSeries,
                                            times = loadChartData.timeLabels,
                                            color = MaterialTheme.colorScheme.primary,
                                            suffix = "%",
                                            chartAnimationEnabled = chartAnimationEnabled
                                    )
                                },
                                second = {
                                    ChartCard(
                                            title = stringResource(R.string.node_detail_ram),
                                            data = loadChartData.ramSeries,
                                            times = loadChartData.timeLabels,
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
                                            netInData = loadChartData.netInSeries,
                                            netOutData = loadChartData.netOutSeries,
                                            times = loadChartData.timeLabels,
                                            chartAnimationEnabled = chartAnimationEnabled
                                    )
                                },
                                second = {
                                    ConnectionChartCard(
                                            title = stringResource(R.string.node_detail_connections),
                                            tcpData = loadChartData.tcpSeries,
                                            udpData = loadChartData.udpSeries,
                                            times = loadChartData.timeLabels,
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
                                    data = loadChartData.processSeries,
                                    times = loadChartData.timeLabels,
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
                                    data = loadChartData.cpuSeries,
                                    times = loadChartData.timeLabels,
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
                                    data = loadChartData.ramSeries,
                                    times = loadChartData.timeLabels,
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
                                    netInData = loadChartData.netInSeries,
                                    netOutData = loadChartData.netOutSeries,
                                    times = loadChartData.timeLabels,
                                    chartAnimationEnabled = chartAnimationEnabled
                            )
                        }
                    }
                    item {
                        ChartSectionSurface {
                            ConnectionChartCard(
                                    title = stringResource(R.string.node_detail_connections),
                                    tcpData = loadChartData.tcpSeries,
                                    udpData = loadChartData.udpSeries,
                                    times = loadChartData.timeLabels,
                                    suffix = "",
                                    chartAnimationEnabled = chartAnimationEnabled
                            )
                        }
                    }
                    item {
                        ChartSectionSurface {
                            ChartCard(
                                    title = stringResource(R.string.node_detail_process),
                                    data = loadChartData.processSeries,
                                    times = loadChartData.timeLabels,
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
                        onHoursChanged = onPingHoursChanged
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
            } else if (state.pingTasks.isEmpty()) {
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
                state.pingTasks.forEach { task ->
                    val taskId = task.id
                    val records = state.pingChartByTaskId[taskId]
                    if (records == null || records.values.isEmpty()) {
                        item(key = "ping_empty_$taskId") {
                            ChartSectionSurface {
                                Box(
                                        modifier = Modifier.fillMaxWidth().height(100.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                            text = stringResource(R.string.node_detail_no_records),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color =
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "ping_$taskId") {
                            ChartSectionSurface {
                                PingTaskChart(
                                        task = task,
                                        values = records.values,
                                        times = records.times,
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

@Composable
internal fun ChartSectionHeader(
        title: String,
        selectedHours: Int,
        onHoursChanged: (Int) -> Unit,
        showRealtime: Boolean = false
) {
    Surface(
            modifier = Modifier.height(60.dp).fillMaxWidth(),
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

@Composable
internal fun ChartSectionSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun WideChartRow(first: @Composable () -> Unit, second: (@Composable () -> Unit)? = null) {
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
