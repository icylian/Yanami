package com.sekusarisu.yanami.ui.screen.nodelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sekusarisu.yanami.domain.model.Node

@Preview(showBackground = true)
@Composable
private fun NodeCardPreview() {
    val sampleNode =
            Node(
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
                    gpuName = "",
                    trafficLimit = 1_000L * 1024 * 1024 * 1024,
                    trafficLimitType = "sum",
                    expiredAt = "2026-04-15T12:00:00"
            )
    MaterialTheme {
        Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
        ) {
            NodeCard(node = sampleNode, onClick = {}, isExpanded = false)
            NodeCard(node = sampleNode, onClick = {}, isExpanded = true)
            NodeCard(node = sampleNode.copy(isOnline = false), onClick = {}, isExpanded = false)
        }
    }
}
