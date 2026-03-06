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
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.LoadRecord
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.domain.model.PingRecord
import com.sekusarisu.yanami.domain.model.PingTask
import com.sekusarisu.yanami.ui.screen.nodelist.formatBytes
import com.sekusarisu.yanami.ui.screen.nodelist.formatUptime
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginScreen
import com.sekusarisu.yanami.ui.screen.terminal.SshTerminalScreen
import org.koin.core.parameter.parametersOf

/**
 * 节点详情页面
 *
 * 展示服务器信息总览、负载历史图表、Ping 延迟监控。
 */
class NodeDetailScreen(private val uuid: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<NodeDetailViewModel> { parametersOf(uuid) }
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is NodeDetailContract.Effect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is NodeDetailContract.Effect.NavigateToServerRelogin -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(
                                ServerReLoginScreen(
                                        serverId = effect.serverId,
                                        forceTwoFa = effect.forceTwoFa
                                )
                        )
                    }
                }
            }
        }

        Scaffold(
                topBar = {
                    TopAppBar(
                            title = {
                                Text(
                                        text = state.node?.name
                                                        ?: stringResource(
                                                                R.string.node_detail_title
                                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { navigator.pop() }) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription =
                                                    stringResource(R.string.action_back)
                                    )
                                }
                            },
                            actions = {
                                // SSH 终端入口：仅节点在线时可用
                                val isOnline = state.node?.isOnline ?: false
                                IconButton(
                                        onClick = {
                                            navigator.push(
                                                    SshTerminalScreen(
                                                            uuid = uuid,
                                                            nodeName = state.node?.name ?: uuid
                                                    )
                                            )
                                        },
                                        enabled = isOnline
                                ) {
                                    Icon(
                                            imageVector = Icons.Filled.Terminal,
                                            contentDescription =
                                                    stringResource(R.string.action_ssh_terminal)
                                    )
                                }
                            },
                            colors =
                                    TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                    )
                    )
                }
        ) { innerPadding ->
            when {
                state.isLoading -> {
                    LoadingContent(modifier = Modifier.fillMaxSize().padding(innerPadding))
                }
                state.error != null -> {
                    ErrorContent(
                            error = state.error!!,
                            onRetry = { viewModel.onEvent(NodeDetailContract.Event.Retry) },
                            modifier = Modifier.fillMaxSize().padding(innerPadding)
                    )
                }
                state.node != null -> {
                    PullToRefreshBox(
                            isRefreshing = false,
                            onRefresh = { viewModel.onEvent(NodeDetailContract.Event.Refresh) },
                            modifier = Modifier.fillMaxSize().padding(innerPadding)
                    ) {
                        NodeDetailContent(
                                state = state,
                                onLoadHoursChanged = {
                                    viewModel.onEvent(NodeDetailContract.Event.LoadHoursChanged(it))
                                },
                                onPingHoursChanged = {
                                    viewModel.onEvent(NodeDetailContract.Event.PingHoursChanged(it))
                                }
                        )
                    }
                }
            }
        }
    }
}

// ─── 主内容 ───

@Composable
private fun NodeDetailContent(
        state: NodeDetailContract.State,
        onLoadHoursChanged: (Int) -> Unit,
        onPingHoursChanged: (Int) -> Unit
) {
    val node = state.node ?: return

    LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 4.1 服务器信息总览
        item { ServerInfoCard(node) }

        // 4.2 负载图表
        item {
            val records =
                    if (state.selectedLoadHours == 0) state.realtimeLoadRecords
                    else state.loadRecords
            LoadChartSection(
                    records = records,
                    isLoading =
                            if (state.selectedLoadHours == 0) false else state.isLoadRecordsLoading,
                    selectedHours = state.selectedLoadHours,
                    onHoursChanged = onLoadHoursChanged
            )
        }

        // 4.3 Ping 延迟监控
        item {
            PingChartSection(
                    tasks = state.pingTasks,
                    records = state.pingRecords,
                    isLoading = state.isPingRecordsLoading,
                    selectedHours = state.selectedPingHours,
                    onHoursChanged = onPingHoursChanged
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─── 4.1 服务器信息总览 ───

@Composable
private fun ServerInfoCard(node: Node) {
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

            // 系统信息
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

            // CPU
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

            // RAM
            UsageBar(
                    label = stringResource(R.string.node_detail_ram),
                    percent =
                            if (node.memTotal > 0) node.memUsed.toDouble() / node.memTotal * 100
                            else 0.0,
                    detail = "${formatBytes(node.memUsed)} / ${formatBytes(node.memTotal)}"
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Swap
            if (node.swapTotal > 0) {
                UsageBar(
                        label = "Swap",
                        percent = node.swapUsed.toDouble() / node.swapTotal * 100,
                        detail = "${formatBytes(node.swapUsed)} / ${formatBytes(node.swapTotal)}"
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Disk
            UsageBar(
                    label = stringResource(R.string.node_detail_disk),
                    percent =
                            if (node.diskTotal > 0) node.diskUsed.toDouble() / node.diskTotal * 100
                            else 0.0,
                    detail = "${formatBytes(node.diskUsed)} / ${formatBytes(node.diskTotal)}"
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // 网络（仅总流量，速度已在图表中展示）
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
private fun UsageBar(label: String, percent: Double, detail: String) {
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

// ─── 4.2 负载图表 ───

@Composable
private fun LoadChartSection(
        records: List<LoadRecord>,
        isLoading: Boolean,
        selectedHours: Int,
        onHoursChanged: (Int) -> Unit
) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = stringResource(R.string.node_detail_load_chart),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                TimeRangeSelector(
                        selectedHours = selectedHours,
                        onHoursChanged = onHoursChanged,
                        showRealtime = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(modifier = Modifier.size(32.dp)) }
            } else if (records.isEmpty()) {
                Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            text =
                                    if (selectedHours == 0)
                                            stringResource(R.string.node_detail_loading)
                                    else stringResource(R.string.node_detail_no_records),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val times = records.map { it.time }

                // CPU 使用率图表
                ChartCard(
                        title = stringResource(R.string.node_detail_cpu_usage),
                        data = records.map { it.cpu },
                        times = times,
                        color = MaterialTheme.colorScheme.primary,
                        suffix = "%"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // RAM 使用率图表
                ChartCard(
                        title = stringResource(R.string.node_detail_ram),
                        data = records.map { it.ramPercent },
                        times = times,
                        color = MaterialTheme.colorScheme.primary,
                        suffix = "%"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 网络 IO 图表
                NetworkChartCard(
                        title = stringResource(R.string.node_detail_net_speed),
                        netInData = records.map { it.netIn.toDouble() },
                        netOutData = records.map { it.netOut.toDouble() },
                        times = times
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 连接数图表
                ConnectionChartCard(
                        title = stringResource(R.string.node_detail_connections),
                        tcpData = records.map { it.connections },
                        udpData = records.map { it.connectionsUdp },
                        times = times,
                        suffix = ""
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 进程数图表
                ChartCard(
                        title = stringResource(R.string.node_detail_process),
                        data = records.map { it.process.toDouble() },
                        times = times,
                        color = MaterialTheme.colorScheme.primary,
                        suffix = ""
                )
            }
        }
    }
}

/** 解析 ISO 时间字符串为 HH:mm 格式 */
private fun parseTimeLabel(isoTime: String): String {
    return try {
        val instant = java.time.Instant.parse(isoTime)
        val localTime = instant.atZone(java.time.ZoneId.systemDefault()).toLocalTime()
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
private fun ChartCard(
        title: String,
        data: List<Double>,
        times: List<String>,
        color: Color,
        suffix: String
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
                            rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content)
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
private fun ConnectionChartCard(
    title: String,
    tcpData: List<Int>,
    udpData: List<Int>,
    times: List<String>,
    suffix: String
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
                    rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content)
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
private fun NetworkChartCard(
        title: String,
        netInData: List<Double>,
        netOutData: List<Double>,
        times: List<String>
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
                            rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content)
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

// ─── 4.3 Ping 延迟监控 ───

@Composable
private fun PingChartSection(
        tasks: List<PingTask>,
        records: List<PingRecord>,
        isLoading: Boolean,
        selectedHours: Int,
        onHoursChanged: (Int) -> Unit
) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = stringResource(R.string.node_detail_ping_chart),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                TimeRangeSelector(
                        selectedHours = selectedHours,
                        onHoursChanged = onHoursChanged,
                        showRealtime = false
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(modifier = Modifier.size(32.dp)) }
            } else if (records.isEmpty()) {
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
            } else {
                // 按任务分组
                tasks.forEach { task ->
                    val taskLatest = task.latest
//                    val taskMdev = task.p99 / task.p50
                    val taskLoss = task.loss
                    val taskRecords = records.filter { it.taskId == task.id }
                    val taskValues = taskRecords.map { it.value }
                    val taskTimes = taskRecords.map { it.time }
                    if (taskValues.size >= 2) {
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
                                text = "%.0fms".format(taskLatest) + " / "
                                        + "%.1f%%".format(taskLoss),
//                                        + "%.1f".format(taskMdev),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        val modelProducer = remember { CartesianChartModelProducer() }
                        LaunchedEffect(taskValues) {
                            modelProducer.runTransaction { lineSeries { series(taskValues) } }
                        }

                        val yAxisFormatter = remember {
                            CartesianValueFormatter { _, value, _ -> "%.0fms".format(value) }
                        }
                        val xAxisFormatter =
                                remember(taskTimes) {
                                    CartesianValueFormatter { _, value, _ ->
                                        val index = value.toInt().coerceIn(0, taskTimes.size - 1)
                                        if (index in taskTimes.indices)
                                                parseTimeLabel(taskTimes[index])
                                        else ""
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
                                        )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
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
                    onClick = { onHoursChanged(0) },
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
                    onClick = { onHoursChanged(hours) },
                    label = {
                        Text(text = "${hours}h", style = MaterialTheme.typography.labelSmall)
                    }
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
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
private fun ErrorContent(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
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
            OutlinedButton(onClick = onRetry) {
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
                fill = LineCartesianLayer.LineFill.single(fill(color)),
                areaFill = LineCartesianLayer.AreaFill.single(
                        fill(ShaderProvider.verticalGradient(
                                arrayOf(color.copy(alpha = 0.32f), Color.Transparent)
                        ))
                )
        )

// ─── 图表工具函数 ───

private fun getUsageColor(percent: Double): Color {
    return when {
        percent < 60 -> Color(0xFF4CAF50) // 绿
        percent < 85 -> Color(0xFFFFC107) // 黄
        else -> Color(0xFFF44336) // 红
    }
}

@Preview
@Composable
fun ServerInfoCardPreview() {
    val sampleNode = Node(
        uuid = "123",
        name = "Sample Node",
        region = "🇯🇵",
        group = "Default",
        isOnline = true,
        cpuUsage = 45.0,
        memUsed = 1024 * 1024 * 1024,
        memTotal = 4 * 1024 * 1024 * 1024L,
        swapUsed = 0,
        swapTotal = 0,
        diskUsed = 20 * 1024 * 1024 * 1024L,
        diskTotal = 100 * 1024 * 1024 * 1024L,
        netIn = 1024 * 1024,
        netOut = 2 * 1024 * 1024,
        netTotalUp = 10L * 1024 * 1024 * 1024,
        netTotalDown = 20L * 1024 * 1024 * 1024,
        uptime = 3600 * 24 * 2,
        os = "Ubuntu 22.04",
        cpuName = "Intel Xeon",
        cpuCores = 2,
        weight = 0,
        load1 = 0.5,
        load5 = 0.4,
        load15 = 0.3,
        process = 100,
        connectionsTcp = 50,
        connectionsUdp = 10,
        kernelVersion = "5.15.0",
        virtualization = "KVM",
        arch = "x86_64",
        gpuName = ""
    )
    MaterialTheme {
        ServerInfoCard(node = sampleNode)
    }
}

@Preview(showBackground = true)
@Composable
fun LoadChartSectionPreview() {
    val sampleRecords = listOf(
        LoadRecord(time = "2026-03-01T10:00:00Z", cpu = 40.0, ramPercent = 50.0, diskPercent = 60.0, netIn = 1024, netOut = 2048, load = 1.0, process = 100, connections = 50, connectionsUdp = 10),
        LoadRecord(time = "2026-03-01T10:05:00Z", cpu = 45.0, ramPercent = 52.0, diskPercent = 60.0, netIn = 2048, netOut = 4096, load = 1.2, process = 105, connections = 60, connectionsUdp = 12),
        LoadRecord(time = "2026-03-01T10:10:00Z", cpu = 35.0, ramPercent = 48.0, diskPercent = 60.0, netIn = 512, netOut = 1024, load = 0.9, process = 95, connections = 45, connectionsUdp = 8)
    )
    MaterialTheme {
        LoadChartSection(
            records = sampleRecords,
            isLoading = false,
            selectedHours = 1,
            onHoursChanged = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PingChartSectionPreview() {
    val sampleTasks = listOf(
        PingTask(id = 1, name = "Google", interval = 60, latest = 15.0, min = 10.0, max = 20.0, avg = 14.0, loss = 0.0, p50 = 14.0, p99 = 19.0)
    )
    val sampleRecords = listOf(
        PingRecord(taskId = 1, taskName = "Google", time = "2026-03-01T10:00:00Z", value = 15.0),
        PingRecord(taskId = 1, taskName = "Google", time = "2026-03-01T10:05:00Z", value = 16.0),
        PingRecord(taskId = 1, taskName = "Google", time = "2026-03-01T10:10:00Z", value = 14.0)
    )
    MaterialTheme {
        PingChartSection(
            tasks = sampleTasks,
            records = sampleRecords,
            isLoading = false,
            selectedHours = 1,
            onHoursChanged = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChartComponentsPreview() {
    val times = listOf("2026-03-01T10:00:00Z", "2026-03-01T10:05:00Z", "2026-03-01T10:10:00Z")
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            UsageBar(label = "CPU", percent = 45.0, detail = "45.0%")
            ChartCard(
                title = "CPU Usage",
                data = listOf(40.0, 45.0, 35.0),
                times = times,
                color = MaterialTheme.colorScheme.primary,
                suffix = "%"
            )
            ConnectionChartCard(
                title = "Connections",
                tcpData = listOf(50, 60, 45),
                udpData = listOf(10, 12, 8),
                times = times,
                suffix = ""
            )
            NetworkChartCard(
                title = "Network",
                netInData = listOf(1024.0, 2048.0, 512.0),
                netOutData = listOf(2048.0, 4096.0, 1024.0),
                times = times
            )
        }
    }
}
