package com.sekusarisu.yanami.ui.screen.nodelist

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers.BLUE_DOMINATED_EXAMPLE
import androidx.compose.ui.tooling.preview.Wallpapers.GREEN_DOMINATED_EXAMPLE
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.client.ClientManagementScreen
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.nodedetail.NodeDetailScreen
import com.sekusarisu.yanami.ui.screen.server.AddServerScreen
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginScreen
import com.sekusarisu.yanami.ui.screen.soundClick
import com.sekusarisu.yanami.ui.theme.ThemeColor
import com.sekusarisu.yanami.ui.theme.YanamiTheme

/**
 * 节点列表主页面
 *
 * 展示当前服务器的所有节点，包含搜索、统计总览、分组筛选、节点卡片列表。 支持下拉刷新、全局展开/收缩节点卡片、左右滑动切换分组。
 */
class NodeListScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<NodeListViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val adaptiveInfo = rememberAdaptiveLayoutInfo()

        // 全局展开/收缩状态
        var isAllExpanded by remember { mutableStateOf(true) }

        // 处理副作用
        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is NodeListContract.Effect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is NodeListContract.Effect.NavigateToNodeDetail -> {
                        navigator.push(NodeDetailScreen(effect.uuid))
                    }
                    is NodeListContract.Effect.NavigateToClientManagement -> {
                        navigator.push(ClientManagementScreen())
                    }
                    is NodeListContract.Effect.NavigateToServerRelogin -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(
                                ServerReLoginScreen(
                                        serverId = effect.serverId,
                                        forceTwoFa = effect.forceTwoFa
                                )
                        )
                    }
                    is NodeListContract.Effect.NavigateToServerEdit -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(AddServerScreen(editServerId = effect.serverId))
                    }
                }
            }
        }

        NodeListScaffoldContent(
                state = state,
                isAllExpanded = isAllExpanded,
                isTabletLandscape = adaptiveInfo.isTabletLandscape,
                onBackClick = soundClick { navigator.pop() },
                onManageClientsClick = soundClick {
                    viewModel.onEvent(NodeListContract.Event.ManageClientsClicked)
                },
                onToggleExpandClick = soundClick { isAllExpanded = !isAllExpanded },
                onRefresh = { viewModel.onEvent(NodeListContract.Event.Refresh) },
                onRetry = { viewModel.onEvent(NodeListContract.Event.Retry) },
                onEvent = viewModel::onEvent
        )
    }
}

// ─── 主内容 ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeListScaffoldContent(
        state: NodeListContract.State,
        isAllExpanded: Boolean,
        isTabletLandscape: Boolean,
        onBackClick: () -> Unit,
        onManageClientsClick: () -> Unit,
        onToggleExpandClick: () -> Unit,
        onRefresh: () -> Unit,
        onRetry: () -> Unit,
        onEvent: (NodeListContract.Event) -> Unit,
        modifier: Modifier = Modifier
) {
    Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                        title = {
                            Text(
                                    text =
                                            state.serverName.ifBlank {
                                                stringResource(R.string.node_list)
                                            },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onManageClientsClick) {
                                Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription =
                                                stringResource(
                                                        R.string.client_management_title
                                                )
                                )
                            }
                            IconButton(onClick = onToggleExpandClick) {
                                Icon(
                                        imageVector =
                                                if (isAllExpanded) Icons.Default.UnfoldLess
                                                else Icons.Default.UnfoldMore,
                                        contentDescription =
                                                if (isAllExpanded)
                                                        stringResource(R.string.node_collapse)
                                                else stringResource(R.string.node_expand)
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
                        error = state.error,
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                )
            }
            else -> {
                PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) {
                    NodeListContent(
                            state = state,
                            onEvent = onEvent,
                            isAllExpanded = isAllExpanded,
                            isTabletLandscape = isTabletLandscape
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeListContent(
        state: NodeListContract.State,
        onEvent: (NodeListContract.Event) -> Unit,
        isAllExpanded: Boolean,
        isTabletLandscape: Boolean
) {
    // 构建分组列表（null 表示"全部"）
    val allGroups: List<String?> = listOf(null) + state.groups

    // 滑动切换分组
    val swipeModifier =
            if (allGroups.size > 1) {
                Modifier.pointerInput(allGroups, state.selectedGroup) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = {
                                if (totalDrag > 100f) {
                                    // 向右滑 → 上一个分组
                                    val currentIndex = allGroups.indexOf(state.selectedGroup)
                                    if (currentIndex > 0) {
                                        onEvent(
                                                NodeListContract.Event.GroupSelected(
                                                        allGroups[currentIndex - 1]
                                                )
                                        )
                                    }
                                } else if (totalDrag < -100f) {
                                    // 向左滑 → 下一个分组
                                    val currentIndex = allGroups.indexOf(state.selectedGroup)
                                    if (currentIndex < allGroups.size - 1) {
                                        onEvent(
                                                NodeListContract.Event.GroupSelected(
                                                        allGroups[currentIndex + 1]
                                                )
                                        )
                                    }
                                }
                            },
                            onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
                    )
                }
            } else {
                Modifier
            }

    AdaptiveContentPane(
            modifier = Modifier.then(swipeModifier),
            maxWidth = if (isTabletLandscape) 1400.dp else 920.dp
    ) {
        LazyVerticalGrid(
                columns =
                        if (isTabletLandscape) GridCells.Adaptive(minSize = 360.dp)
                        else GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(4.dp))
                SearchBar(
                        query = state.searchQuery,
                        onQueryChange = { onEvent(NodeListContract.Event.SearchQueryChanged(it)) }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                OverviewCard(
                        onlineCount = state.onlineCount,
                        offlineCount = state.offlineCount,
                        totalCount = state.totalCount,
                        totalNetIn = state.totalNetIn,
                        totalNetOut = state.totalNetOut,
                        totalTrafficUp = state.totalTrafficUp,
                        totalTrafficDown = state.totalTrafficDown,
                        statusFilter = state.statusFilter,
                        onStatusFilterSelected = {
                            onEvent(NodeListContract.Event.StatusFilterSelected(it))
                        }
                )
            }

            if (state.groups.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GroupFilterRow(
                            groups = state.groups,
                            selectedGroup = state.selectedGroup,
                            onGroupSelected = { onEvent(NodeListContract.Event.GroupSelected(it)) }
                    )
                }
            }

            if (state.filteredNodes.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyNodeList(
                            hasSearchQuery = state.searchQuery.isNotBlank(),
                            hasGroupFilter = state.selectedGroup != null,
                            hasStatusFilter =
                                    state.statusFilter != NodeListContract.StatusFilter.ALL
                    )
                }
            } else {
                items(state.filteredNodes, key = { it.uuid }) { node ->
                    NodeCard(
                            node = node,
                            onClick = soundClick { onEvent(NodeListContract.Event.NodeClicked(node.uuid)) },
                            isExpanded = isAllExpanded
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ─── 搜索栏 ───

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
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

// ─── 总览卡片 ───

@Composable
private fun OverviewCard(
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
        color: androidx.compose.ui.graphics.Color,
        isSelected: Boolean = false,
        onClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier
) {
    val bgColor =
            if (isSelected) color.copy(alpha = 0.15f)
            else androidx.compose.ui.graphics.Color.Transparent
    val contentModifier =
            if (onClick != null) {
                Modifier
                        .clip(RoundedCornerShape(12.dp))
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

// ─── 分组筛选 ───

@Composable
private fun GroupFilterRow(
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

// ─── 状态页面 ───

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
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
            OutlinedButton(onClick = soundClick { onRetry() }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
private fun EmptyNodeList(hasSearchQuery: Boolean, hasGroupFilter: Boolean, hasStatusFilter: Boolean = false) {
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

@Preview(showBackground = true)
@Composable
fun SearchBarPreview() {
    MaterialTheme {
        SearchBar(query = "Tokyo", onQueryChange = {})
    }
}

@Preview(showBackground = true)
@Composable
fun OverviewCardPreview() {
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
fun GroupFilterRowPreview() {
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
fun StateContentPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LoadingContent()
            ErrorContent(error = "Network Error", onRetry = {})
            EmptyNodeList(hasSearchQuery = true, hasGroupFilter = false)
        }
    }
}

@Preview(name = "NodeList Tablet", showBackground = true, widthDp = 1280, heightDp = 900)
@Preview(name = "NodeList Tablet", showBackground = true)
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
                label = { Text(stringResource(R.string.server_management)) }
        )
        NavigationRailItem(
                selected = true,
                onClick = {},
                icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                label = { Text(stringResource(R.string.node_list)) }
        )
        NavigationRailItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text(stringResource(R.string.settings_title)) }
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun nodeListTabletPreviewState(): NodeListContract.State {
    val nodes =
            listOf(
                    previewNode(
                            index = 1,
                            name = "JP-Tokyo-01",
                            region = "🇯🇵",
                            group = "Asia",
                            isOnline = true
                    ),
                    previewNode(
                            index = 2,
                            name = "SG-Singapore-02",
                            region = "🇸🇬",
                            group = "Asia",
                            isOnline = true
                    ),
                    previewNode(
                            index = 3,
                            name = "DE-Frankfurt-03",
                            region = "🇩🇪",
                            group = "Europe",
                            isOnline = true
                    ),
                    previewNode(
                            index = 4,
                            name = "FR-Paris-04",
                            region = "🇫🇷",
                            group = "Europe",
                            isOnline = true
                    ),
                    previewNode(
                            index = 5,
                            name = "US-Fremont-05",
                            region = "🇺🇸",
                            group = "America",
                            isOnline = true
                    ),
                    previewNode(
                            index = 6,
                            name = "CA-Toronto-06",
                            region = "🇨🇦",
                            group = "America",
                            isOnline = true
                    ),
                    previewNode(
                        index = 7,
                        name = "JP-Tokyo-02",
                        region = "🇯🇵",
                        group = "Asia",
                        isOnline = true
                    ),
                    previewNode(
                        index = 8,
                        name = "SG-Singapore-03",
                        region = "🇸🇬",
                        group = "Asia",
                        isOnline = true
                    ),
                    previewNode(
                        index = 9,
                        name = "DE-Frankfurt-04",
                        region = "🇩🇪",
                        group = "Europe",
                        isOnline = true
                    ),
                    previewNode(
                        index = 10,
                        name = "FR-Paris-05",
                        region = "🇫🇷",
                        group = "Europe",
                        isOnline = true
                    ),
                    previewNode(
                        index = 11,
                        name = "US-Fremont-06",
                        region = "🇺🇸",
                        group = "America",
                        isOnline = false
                    ),
                    previewNode(
                        index = 12,
                        name = "CA-Toronto-07",
                        region = "🇨🇦",
                        group = "America",
                        isOnline = false
                    )
            )

    return NodeListContract.State(
            isLoading = false,
            isRefreshing = false,
            nodes = nodes,
            filteredNodes = nodes,
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
