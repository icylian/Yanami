package com.sekusarisu.yanami.ui.screen.nodelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.ui.theme.ThemeColor
import com.sekusarisu.yanami.ui.theme.YanamiTheme

@Preview(showBackground = true)
@Composable
private fun SearchBarPreview() {
    MaterialTheme {
        SearchBar(query = "Tokyo", onQueryChange = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun OverviewCardPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            OverviewCard(
                    onlineCount = 8,
                    offlineCount = 2,
                    totalCount = 10,
                    totalNetIn = 1024L * 1024 * 5,
                    totalNetOut = 1024L * 1024 * 10,
                    totalTrafficUp = 1024L * 1024 * 1024 * 100,
                    totalTrafficDown = 1024L * 1024 * 1024 * 500,
                    statusFilter = NodeListContract.StatusFilter.ALL,
                    onStatusFilterSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupFilterRowPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GroupFilterRow(
                    groups = listOf("Asia", "Europe", "America"),
                    selectedGroup = "Asia",
                    onGroupSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StateContentPreview() {
    MaterialTheme {
        Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LoadingContent()
            ErrorContent(error = "Network Error", onRetry = {})
            EmptyNodeList(hasSearchQuery = true, hasGroupFilter = false)
        }
    }
}

@Preview(name = "NodeList Tablet", showBackground = true, widthDp = 1280, heightDp = 900)
@Composable
private fun NodeListTabletPreview() {
    YanamiTheme(themeColor = ThemeColor.BLUE_MTB, darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(modifier = Modifier.fillMaxSize()) {
                NodeListTabletPreviewRail()
                NodeListScaffoldContent(
                        state = nodeListTabletPreviewState(),
                        isAllExpanded = true,
                        isTabletLandscape = true,
                        onBackClick = {},
                        onManageClientsClick = {},
                        onToggleExpandClick = {},
                        onRefresh = {},
                        onRetry = {},
                        onEvent = {},
                        modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NodeListTabletPreviewRail() {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
        Spacer(modifier = Modifier.weight(1f))
        NavigationRailItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Default.Dns, contentDescription = null) },
                label = { Text("Server") }
        )
        NavigationRailItem(
                selected = true,
                onClick = {},
                icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                label = { Text("Nodes") }
        )
        NavigationRailItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("Settings") }
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun nodeListTabletPreviewState(): NodeListContract.State {
    val nodes =
            listOf(
                    previewNode(1, "JP-Tokyo-01", "🇯🇵", "Asia", true),
                    previewNode(2, "SG-Singapore-02", "🇸🇬", "Asia", true),
                    previewNode(3, "DE-Frankfurt-03", "🇩🇪", "Europe", true),
                    previewNode(4, "FR-Paris-04", "🇫🇷", "Europe", true),
                    previewNode(5, "US-Fremont-05", "🇺🇸", "America", true),
                    previewNode(6, "CA-Toronto-06", "🇨🇦", "America", true),
                    previewNode(7, "JP-Tokyo-02", "🇯🇵", "Asia", true),
                    previewNode(8, "SG-Singapore-03", "🇸🇬", "Asia", true),
                    previewNode(9, "DE-Frankfurt-04", "🇩🇪", "Europe", true),
                    previewNode(10, "FR-Paris-05", "🇫🇷", "Europe", true),
                    previewNode(11, "US-Fremont-06", "🇺🇸", "America", false),
                    previewNode(12, "CA-Toronto-07", "🇨🇦", "America", false),
            )

    return NodeListContract.State(
            isLoading = false,
            isRefreshing = false,
            nodes = nodes,
            searchQuery = "",
            groups = listOf("Asia", "Europe", "America"),
            selectedGroup = null,
            statusFilter = NodeListContract.StatusFilter.ALL,
            error = null,
            onlineCount = nodes.count { it.isOnline },
            offlineCount = nodes.count { !it.isOnline },
            totalCount = nodes.size,
            totalNetIn = nodes.sumOf { it.netIn },
            totalNetOut = nodes.sumOf { it.netOut },
            totalTrafficUp = nodes.sumOf { it.netTotalUp },
            totalTrafficDown = nodes.sumOf { it.netTotalDown },
            serverName = "Yanami Edge Cluster"
    )
}

private fun previewNode(
        index: Int,
        name: String,
        region: String,
        group: String,
        isOnline: Boolean
): Node =
        Node(
                uuid = "preview-node-$index",
                name = name,
                region = region,
                group = group,
                isOnline = isOnline,
                cpuUsage = 18.0 + index * 9,
                memUsed = (index * 768L) * 1024 * 1024,
                memTotal = 8L * 1024 * 1024 * 1024,
                swapUsed = index * 64L * 1024 * 1024,
                swapTotal = 2L * 1024 * 1024 * 1024,
                diskUsed = (28L + index * 11L) * 1024 * 1024 * 1024,
                diskTotal = 256L * 1024 * 1024 * 1024,
                netIn = if (isOnline) index * 7L * 1024 * 1024 else 0,
                netOut = if (isOnline) index * 5L * 1024 * 1024 else 0,
                netTotalUp = (120L + index * 35L) * 1024 * 1024 * 1024,
                netTotalDown = (220L + index * 48L) * 1024 * 1024 * 1024,
                uptime = if (isOnline) (index * 2L + 1L) * 86_400 else 0,
                os = "Ubuntu 24.04 LTS",
                cpuName = "AMD EPYC 7B12",
                cpuCores = 4 + index,
                weight = index,
                load1 = 0.2 * index,
                load5 = 0.16 * index,
                load15 = 0.12 * index,
                process = 120 + index * 6,
                connectionsTcp = 80 + index * 11,
                connectionsUdp = 12 + index * 2,
                kernelVersion = "6.8.0",
                virtualization = "KVM",
                arch = "amd64",
                gpuName = "",
                trafficLimit = if (index % 2 == 0) (900L + index * 50L) * 1024 * 1024 * 1024 else 0,
                trafficLimitType =
                        when (index % 5) {
                            0 -> "sum"
                            1 -> "max"
                            2 -> "min"
                            3 -> "up"
                            else -> "down"
                        },
                expiredAt =
                        if (index % 3 == 0) "2026-04-${10 + index}T12:00:00"
                        else if (index % 4 == 0) "2026-03-23 08:00:00"
                        else null
        )
