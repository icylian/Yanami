package com.sekusarisu.yanami.ui.screen.nodelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.domain.model.calculateExpiryStatus
import com.sekusarisu.yanami.domain.model.calculateTrafficLimitUsage
import com.sekusarisu.yanami.ui.screen.ExpiryBadge
import com.sekusarisu.yanami.ui.screen.soundClick

@Composable
fun NodeCard(node: Node, onClick: () -> Unit, isExpanded: Boolean, modifier: Modifier = Modifier) {
    Card(
            onClick = soundClick { onClick() },
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val expiryStatus = node.calculateExpiryStatus()
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = node.region.ifBlank { "🌐" },
                        style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = node.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                )
                if (node.isOnline) {
                    Spacer(modifier = Modifier.width(6.dp))
                    UptimeBadge(uptime = node.uptime)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                if (expiryStatus != null) {
                    ExpiryBadge(expiryStatus = expiryStatus)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                StatusBadge(isOnline = node.isOnline)
            }

            if (!node.isOnline) {
//                Spacer(modifier = Modifier.height(4.dp))
//                Text(
//                        text = node.os,
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                )
                return@Card
            }

            AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    val memPercent =
                            if (node.memTotal > 0) {
                                node.memUsed.toDouble() / node.memTotal * 100
                            } else {
                                0.0
                            }
                    val diskPercent =
                            if (node.diskTotal > 0) {
                                node.diskUsed.toDouble() / node.diskTotal * 100
                            } else {
                                0.0
                            }
                    val trafficLimitUsage = node.calculateTrafficLimitUsage()

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
                        if (trafficLimitUsage != null) {
                            TrafficLimitMiniIndicator(
                                    usage = trafficLimitUsage,
                                    modifier = Modifier.align(Alignment.Bottom)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = stringResource(R.string.node_upload),
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                    text = formatSpeed(node.netOut),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = stringResource(R.string.node_download),
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                    text = formatSpeed(node.netIn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                        text = "↑ ${formatBytes(node.netTotalUp)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = "↓ ${formatBytes(node.netTotalDown)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
