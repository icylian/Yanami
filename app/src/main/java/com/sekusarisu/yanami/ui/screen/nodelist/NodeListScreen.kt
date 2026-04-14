package com.sekusarisu.yanami.ui.screen.nodelist

import android.widget.Toast
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.client.ClientManagementScreen
import com.sekusarisu.yanami.ui.screen.nodedetail.NodeDetailScreen
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.server.AddServerScreen
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginScreen
import com.sekusarisu.yanami.ui.screen.soundClick

class NodeListScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<NodeListViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val adaptiveInfo = rememberAdaptiveLayoutInfo()
        var isAllExpanded by remember { mutableStateOf(true) }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NodeListScaffoldContent(
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
                                                stringResource(R.string.client_management_title)
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
    val allGroups: List<String?> = listOf(null) + state.groups
    val swipeModifier =
            if (allGroups.size > 1) {
                Modifier.pointerInput(allGroups, state.selectedGroup) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = {
                                if (totalDrag > 100f) {
                                    val currentIndex = allGroups.indexOf(state.selectedGroup)
                                    if (currentIndex > 0) {
                                        onEvent(
                                                NodeListContract.Event.GroupSelected(
                                                        allGroups[currentIndex - 1]
                                                )
                                        )
                                    }
                                } else if (totalDrag < -100f) {
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
                            onClick =
                                    soundClick {
                                        onEvent(NodeListContract.Event.NodeClicked(node.uuid))
                                    },
                            isExpanded = isAllExpanded
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
