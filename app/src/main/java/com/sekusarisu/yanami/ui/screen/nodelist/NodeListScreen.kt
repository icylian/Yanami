package com.sekusarisu.yanami.ui.screen.nodelist

import android.widget.Toast
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.nodedetail.NodeDetailScreen
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginScreen
import com.sekusarisu.yanami.ui.screen.soundClick

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
                    is NodeListContract.Effect.NavigateToServerRelogin -> {
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
                                        text =
                                                state.serverName.ifBlank {
                                                    stringResource(R.string.node_list)
                                                },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = soundClick { navigator.pop() }) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription =
                                                    stringResource(R.string.action_back)
                                    )
                                }
                            },
                            actions = {
                                // 全局展开/收缩按钮
                                IconButton(onClick = soundClick { isAllExpanded = !isAllExpanded }) {
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
                            error = state.error!!,
                            onRetry = { viewModel.onEvent(NodeListContract.Event.Retry) },
                            modifier = Modifier.fillMaxSize().padding(innerPadding)
                    )
                }
                else -> {
                    PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { viewModel.onEvent(NodeListContract.Event.Refresh) },
                            modifier = Modifier.fillMaxSize().padding(innerPadding)
                    ) {
                        NodeListContent(
                                state = state,
                                viewModel = viewModel,
                                isAllExpanded = isAllExpanded
                        )
                    }
                }
            }
        }
    }
}

// ─── 主内容 ───

@Composable
private fun NodeListContent(
        state: NodeListContract.State,
        viewModel: NodeListViewModel,
        isAllExpanded: Boolean
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
                                        viewModel.onEvent(
                                                NodeListContract.Event.GroupSelected(
                                                        allGroups[currentIndex - 1]
                                                )
                                        )
                                    }
                                } else if (totalDrag < -100f) {
                                    // 向左滑 → 下一个分组
                                    val currentIndex = allGroups.indexOf(state.selectedGroup)
                                    if (currentIndex < allGroups.size - 1) {
                                        viewModel.onEvent(
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

    LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).then(swipeModifier),
            verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 搜索栏
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SearchBar(
                    query = state.searchQuery,
                    onQueryChange = {
                        viewModel.onEvent(NodeListContract.Event.SearchQueryChanged(it))
                    }
            )
        }

        // 总览卡片
        item {
            OverviewCard(
                    onlineCount = state.onlineCount,
                    offlineCount = state.offlineCount,
                    totalCount = state.totalCount,
                    totalNetIn = state.totalNetIn,
                    totalNetOut = state.totalNetOut,
                    totalTrafficUp = state.totalTrafficUp,
                    totalTrafficDown = state.totalTrafficDown
            )
        }

        // 分组筛选
        if (state.groups.isNotEmpty()) {
            item {
                GroupFilterRow(
                        groups = state.groups,
                        selectedGroup = state.selectedGroup,
                        onGroupSelected = {
                            viewModel.onEvent(NodeListContract.Event.GroupSelected(it))
                        }
                )
            }
        }

        // 节点列表
        if (state.filteredNodes.isEmpty()) {
            item {
                EmptyNodeList(
                        hasSearchQuery = state.searchQuery.isNotBlank(),
                        hasGroupFilter = state.selectedGroup != null
                )
            }
        } else {
            items(state.filteredNodes, key = { it.uuid }) { node ->
                NodeCard(
                        node = node,
                        onClick = soundClick {
                            viewModel.onEvent(NodeListContract.Event.NodeClicked(node.uuid))
                        },
                        isExpanded = isAllExpanded
                )
            }
        }

        // 底部间距
        item { Spacer(modifier = Modifier.height(16.dp)) }
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
        totalTrafficDown: Long
) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // 节点统计行
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                        label = stringResource(R.string.node_stat_total),
                        value = totalCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatItem(
                        label = stringResource(R.string.node_stat_online),
                        value = onlineCount.toString(),
                        color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                        label = stringResource(R.string.node_stat_offline),
                        value = offlineCount.toString(),
                        color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 网络信息：纵列布局 — 实时速度一列 + 总流量一列
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
            ) {
                // 实时速度列
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                            text = stringResource(R.string.node_net_speed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                                text = formatSpeed(totalNetOut),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                                text = formatSpeed(totalNetIn),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // 总流量列
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                            text = stringResource(R.string.node_net_traffic),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                                text = formatBytes(totalTrafficUp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                                text = formatBytes(totalTrafficDown),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
private fun EmptyNodeList(hasSearchQuery: Boolean, hasGroupFilter: Boolean) {
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
                            if (hasSearchQuery || hasGroupFilter) Icons.Default.Search
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun SearchBarPreview() {
    MaterialTheme {
        SearchBar(query = "Tokyo", onQueryChange = {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
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
                totalTrafficDown = 1024L * 1024 * 1024 * 500
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
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

