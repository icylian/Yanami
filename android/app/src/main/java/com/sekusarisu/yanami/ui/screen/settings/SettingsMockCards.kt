package com.sekusarisu.yanami.ui.screen.settings

import androidx.compose.runtime.Composable
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.ui.screen.nodelist.NodeCard
import com.sekusarisu.yanami.ui.screen.nodelist.NodeListContract
import com.sekusarisu.yanami.ui.screen.nodelist.OverviewCard

private val settingsPreviewNodes =
        listOf(
                Node(
                        uuid = "settings-jp-1",
                        name = "JP-Tokyo-01",
                        region = "🇯🇵",
                        group = "Asia",
                        isOnline = true,
                        cpuUsage = 45.0,
                        memUsed = 1536L * 1024 * 1024,
                        memTotal = 4L * 1024 * 1024 * 1024,
                        swapUsed = 0,
                        swapTotal = 0,
                        diskUsed = 42L * 1024 * 1024 * 1024,
                        diskTotal = 128L * 1024 * 1024 * 1024,
                        netIn = 8L * 1024 * 1024,
                        netOut = 4L * 1024 * 1024,
                        netTotalUp = 120L * 1024 * 1024 * 1024,
                        netTotalDown = 260L * 1024 * 1024 * 1024,
                        uptime = 5L * 24 * 3600,
                        os = "Ubuntu 24.04 LTS",
                        cpuName = "AMD EPYC 7B12",
                        cpuCores = 4,
                        weight = 1,
                        load1 = 0.56,
                        load5 = 0.42,
                        load15 = 0.33,
                        process = 142,
                        connectionsTcp = 91,
                        connectionsUdp = 16,
                        kernelVersion = "6.8.0",
                        virtualization = "KVM",
                        arch = "amd64",
                        gpuName = "",
                        trafficLimit = 1024L * 1024 * 1024 * 1024,
                        trafficLimitType = "sum",
                        expiredAt = "2026-05-10T12:00:00"
                ),
                Node(
                        uuid = "settings-sg-2",
                        name = "SG-Singapore-02",
                        region = "🇸🇬",
                        group = "Asia",
                        isOnline = true,
                        cpuUsage = 21.0,
                        memUsed = 896L * 1024 * 1024,
                        memTotal = 2L * 1024 * 1024 * 1024,
                        swapUsed = 0,
                        swapTotal = 0,
                        diskUsed = 18L * 1024 * 1024 * 1024,
                        diskTotal = 64L * 1024 * 1024 * 1024,
                        netIn = 3L * 1024 * 1024,
                        netOut = 6L * 1024 * 1024,
                        netTotalUp = 96L * 1024 * 1024 * 1024,
                        netTotalDown = 140L * 1024 * 1024 * 1024,
                        uptime = 9L * 24 * 3600,
                        os = "Debian 12",
                        cpuName = "Intel Xeon Platinum",
                        cpuCores = 2,
                        weight = 2,
                        load1 = 0.21,
                        load5 = 0.16,
                        load15 = 0.09,
                        process = 97,
                        connectionsTcp = 61,
                        connectionsUdp = 11,
                        kernelVersion = "6.1.0",
                        virtualization = "KVM",
                        arch = "amd64",
                        gpuName = "",
                        trafficLimit = 0,
                        trafficLimitType = "sum",
                        expiredAt = null
                ),
                Node(
                        uuid = "settings-us-3",
                        name = "US-Fremont-03",
                        region = "🇺🇸",
                        group = "America",
                        isOnline = false,
                        cpuUsage = 0.0,
                        memUsed = 0,
                        memTotal = 4L * 1024 * 1024 * 1024,
                        swapUsed = 0,
                        swapTotal = 0,
                        diskUsed = 0,
                        diskTotal = 128L * 1024 * 1024 * 1024,
                        netIn = 0,
                        netOut = 0,
                        netTotalUp = 48L * 1024 * 1024 * 1024,
                        netTotalDown = 82L * 1024 * 1024 * 1024,
                        uptime = 0,
                        os = "Ubuntu 22.04 LTS",
                        cpuName = "AMD EPYC",
                        cpuCores = 4,
                        weight = 3,
                        load1 = 0.0,
                        load5 = 0.0,
                        load15 = 0.0,
                        process = 0,
                        connectionsTcp = 0,
                        connectionsUdp = 0,
                        kernelVersion = "6.5.0",
                        virtualization = "KVM",
                        arch = "amd64",
                        gpuName = "",
                        trafficLimit = 0,
                        trafficLimitType = "sum",
                        expiredAt = null
                )
        )

@Composable
internal fun MockOverviewCard() {
    OverviewCard(
            onlineCount = settingsPreviewNodes.count { it.isOnline },
            offlineCount = settingsPreviewNodes.count { !it.isOnline },
            totalCount = settingsPreviewNodes.size,
            totalNetIn = settingsPreviewNodes.sumOf { it.netIn },
            totalNetOut = settingsPreviewNodes.sumOf { it.netOut },
            totalTrafficUp = settingsPreviewNodes.sumOf { it.netTotalUp },
            totalTrafficDown = settingsPreviewNodes.sumOf { it.netTotalDown },
            statusFilter = NodeListContract.StatusFilter.ALL,
            onStatusFilterSelected = {}
    )
}

@Composable
internal fun MockNodeCardPreview() {
    NodeCard(
            node = settingsPreviewNodes.first(),
            onClick = {},
            isExpanded = true
    )
}
