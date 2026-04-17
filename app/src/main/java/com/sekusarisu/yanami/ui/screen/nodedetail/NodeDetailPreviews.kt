package com.sekusarisu.yanami.ui.screen.nodedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sekusarisu.yanami.domain.model.LoadRecord
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.domain.model.PingRecord
import com.sekusarisu.yanami.domain.model.PingTask

@Preview
@Composable
private fun ServerInfoCardPreview() {
    val sampleNode =
            Node(
                    uuid = "123",
                    name = "Sample Node",
                    region = "JP",
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
                    gpuName = "",
                    trafficLimit = 64L * 1024 * 1024 * 1024,
                    trafficLimitType = "sum",
                    expiredAt = "2026-04-15T12:00:00"
            )
    MaterialTheme {
        ServerInfoCard(node = sampleNode)
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadChartSectionPreview() {
    val sampleRecords =
            listOf(
                    LoadRecord(
                            time = "2026-03-01T10:00:00Z",
                            cpu = 40.0,
                            ramPercent = 50.0,
                            diskPercent = 60.0,
                            netIn = 1024,
                            netOut = 2048,
                            load = 1.0,
                            process = 100,
                            connections = 50,
                            connectionsUdp = 10
                    ),
                    LoadRecord(
                            time = "2026-03-01T10:05:00Z",
                            cpu = 45.0,
                            ramPercent = 52.0,
                            diskPercent = 60.0,
                            netIn = 2048,
                            netOut = 4096,
                            load = 1.2,
                            process = 105,
                            connections = 60,
                            connectionsUdp = 12
                    ),
                    LoadRecord(
                            time = "2026-03-01T10:10:00Z",
                            cpu = 35.0,
                            ramPercent = 48.0,
                            diskPercent = 60.0,
                            netIn = 512,
                            netOut = 1024,
                            load = 0.9,
                            process = 95,
                            connections = 45,
                            connectionsUdp = 8
                    )
            )
    val times = sampleRecords.map { it.time }
    MaterialTheme {
        Column {
            ChartSectionHeader(
                    title = "负载历史",
                    selectedHours = 1,
                    onHoursChanged = {},
                    showRealtime = true
            )
            ChartSectionSurface {
                ChartCard(
                        title = "CPU Usage",
                        data = sampleRecords.map { it.cpu },
                        times = times,
                        color = MaterialTheme.colorScheme.primary,
                        suffix = "%"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PingChartSectionPreview() {
    val sampleTasks =
            listOf(
                    PingTask(
                            id = 1,
                            name = "Google",
                            interval = 60,
                            latest = 15.0,
                            min = 10.0,
                            max = 20.0,
                            avg = 14.0,
                            loss = 0.0,
                            p50 = 14.0,
                            p99 = 19.0
                    )
            )
    val sampleRecords =
            listOf(
                    PingRecord(
                            taskId = 1,
                            taskName = "Google",
                            time = "2026-03-01T10:00:00Z",
                            value = 15.0
                    ),
                    PingRecord(
                            taskId = 1,
                            taskName = "Google",
                            time = "2026-03-01T10:05:00Z",
                            value = 16.0
                    ),
                    PingRecord(
                            taskId = 1,
                            taskName = "Google",
                            time = "2026-03-01T10:10:00Z",
                            value = 14.0
                    )
            )
    MaterialTheme {
        ChartSectionSurface {
            PingTaskChart(
                    task = sampleTasks.first(),
                    values = sampleRecords.map { it.value },
                    times = sampleRecords.map { it.time }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChartComponentsPreview() {
    val times = listOf("2026-03-01T10:00:00Z", "2026-03-01T10:05:00Z", "2026-03-01T10:10:00Z")
    MaterialTheme {
        Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
