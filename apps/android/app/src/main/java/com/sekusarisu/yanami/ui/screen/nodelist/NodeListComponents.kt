package com.sekusarisu.yanami.ui.screen.nodelist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.soundClick

@Composable
internal fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.node_search_hint)) },
            leadingIcon = {
                Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.action_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors =
                    OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor =
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor =
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
    )
}

@Composable
internal fun OverviewCard(
        onlineCount: Int,
        offlineCount: Int,
        totalCount: Int,
        totalNetIn: Long,
        totalNetOut: Long,
        totalTrafficUp: Long,
        totalTrafficDown: Long,
        statusFilter: NodeListContract.StatusFilter = NodeListContract.StatusFilter.ALL,
        onStatusFilterSelected: (NodeListContract.StatusFilter) -> Unit = {}
) {
    val adaptiveInfo = rememberAdaptiveLayoutInfo()
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            tonalElevation = 1.dp
    ) {
        if (adaptiveInfo.isTabletLandscape) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                        label = stringResource(R.string.node_stat_total),
                        value = totalCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        isSelected = statusFilter == NodeListContract.StatusFilter.ALL,
                        onClick = soundClick { onStatusFilterSelected(NodeListContract.StatusFilter.ALL) },
                        modifier = Modifier.weight(1f)
                )
                StatItem(
                        label = stringResource(R.string.node_stat_online),
                        value = onlineCount.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        isSelected = statusFilter == NodeListContract.StatusFilter.ONLINE,
                        onClick = soundClick { onStatusFilterSelected(NodeListContract.StatusFilter.ONLINE) },
                        modifier = Modifier.weight(1f)
                )
                StatItem(
                        label = stringResource(R.string.node_stat_offline),
                        value = offlineCount.toString(),
                        color = MaterialTheme.colorScheme.error,
                        isSelected = statusFilter == NodeListContract.StatusFilter.OFFLINE,
                        onClick = soundClick { onStatusFilterSelected(NodeListContract.StatusFilter.OFFLINE) },
                        modifier = Modifier.weight(1f)
                )
                OverviewMetricItem(
                        label = stringResource(R.string.node_net_speed),
                        primaryText = "↑ ${formatSpeed(totalNetOut)}",
                        secondaryText = "↓ ${formatSpeed(totalNetIn)}",
                        modifier = Modifier.weight(1.35f)
                )
                OverviewMetricItem(
                        label = stringResource(R.string.node_net_traffic),
                        primaryText = "↑ ${formatBytes(totalTrafficUp)}",
                        secondaryText = "↓ ${formatBytes(totalTrafficDown)}",
                        modifier = Modifier.weight(1.55f)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(
                            label = stringResource(R.string.node_stat_total),
                            value = totalCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            isSelected = false,
                            onClick = soundClick { onStatusFilterSelected(NodeListContract.StatusFilter.ALL) }
                    )
                    StatItem(
                            label = stringResource(R.string.node_stat_online),
                            value = onlineCount.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            isSelected = statusFilter == NodeListContract.StatusFilter.ONLINE,
                            onClick = soundClick { onStatusFilterSelected(NodeListContract.StatusFilter.ONLINE) }
                    )
                    StatItem(
                            label = stringResource(R.string.node_stat_offline),
                            value = offlineCount.toString(),
                            color = MaterialTheme.colorScheme.error,
                            isSelected = statusFilter == NodeListContract.StatusFilter.OFFLINE,
                            onClick = soundClick { onStatusFilterSelected(NodeListContract.StatusFilter.OFFLINE) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                ) {
                    OverviewMetricItem(
                            label = stringResource(R.string.node_net_speed),
                            primaryText = "↑ ${formatSpeed(totalNetOut)}",
                            secondaryText = "↓ ${formatSpeed(totalNetIn)}"
                    )
                    OverviewMetricItem(
                            label = stringResource(R.string.node_net_traffic),
                            primaryText = "↑ ${formatBytes(totalTrafficUp)}",
                            secondaryText = "↓ ${formatBytes(totalTrafficDown)}"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
        label: String,
        value: String,
        color: Color,
        isSelected: Boolean = false,
        onClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent
    val contentModifier =
            if (onClick != null) {
                Modifier.clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClick)
                        .background(bgColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
            } else {
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            }
    Column(
            modifier = modifier.then(contentModifier),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
        )
        Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun OverviewMetricItem(
        label: String,
        primaryText: String,
        secondaryText: String,
        modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
                text = primaryText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
                text = secondaryText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1
        )
    }
}

@Composable
internal fun GroupFilterRow(
        groups: List<String>,
        selectedGroup: String?,
        onGroupSelected: (String?) -> Unit
) {
    Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
                selected = selectedGroup == null,
                onClick = soundClick { onGroupSelected(null) },
                label = { Text(stringResource(R.string.node_filter_all)) }
        )
        groups.forEach { group ->
            FilterChip(
                    selected = selectedGroup == group,
                    onClick = soundClick { onGroupSelected(if (selectedGroup == group) null else group) },
                    label = { Text(group) }
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
                    text = stringResource(R.string.node_loading),
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

@Composable
internal fun EmptyNodeList(
        hasSearchQuery: Boolean,
        hasGroupFilter: Boolean,
        hasStatusFilter: Boolean = false
) {
    Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
    ) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Icon(
                    imageVector =
                            if (hasSearchQuery || hasGroupFilter || hasStatusFilter) Icons.Default.Search
                            else Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                    text =
                            when {
                                hasSearchQuery -> stringResource(R.string.node_no_match)
                                hasGroupFilter -> stringResource(R.string.node_group_empty)
                                else -> stringResource(R.string.node_no_data)
                            },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
