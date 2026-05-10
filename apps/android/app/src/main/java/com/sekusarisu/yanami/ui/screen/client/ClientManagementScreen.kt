package com.sekusarisu.yanami.ui.screen.client

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.server.AddServerScreen
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginScreen
import com.sekusarisu.yanami.ui.screen.soundClick
import com.sekusarisu.yanami.ui.screen.terminal.SshTerminalScreen
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// Keep section switching centralized here so notification management can be added later
// without touching the screen shell structure again.
private enum class NodeAdminSection {
    CLIENTS,
    PING
}

class ClientManagementScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val clientViewModel = koinScreenModel<ClientManagementViewModel>()
        val clientState by clientViewModel.state.collectAsState()
        val pingViewModel = koinScreenModel<PingTaskManagementViewModel>()
        val pingState by pingViewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        var selectedSection by rememberSaveable { mutableStateOf(NodeAdminSection.CLIENTS.name) }
        val activeSection = remember(selectedSection) { NodeAdminSection.valueOf(selectedSection) }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    clientViewModel.refreshIfLoaded()
                    pingViewModel.refreshIfLoaded()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(activeSection) {
            if (activeSection == NodeAdminSection.PING) {
                pingViewModel.onEvent(PingTaskManagementContract.Event.EnsureLoaded)
            }
        }

        LaunchedEffect(Unit) {
            clientViewModel.effect.collect { effect ->
                when (effect) {
                    is ClientManagementContract.Effect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is ClientManagementContract.Effect.NavigateToCreate -> {
                        navigator.push(ClientCreateScreen())
                    }
                    is ClientManagementContract.Effect.NavigateToEdit -> {
                        navigator.push(ClientEditScreen(effect.uuid))
                    }
                    is ClientManagementContract.Effect.NavigateToServerRelogin -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(
                            ServerReLoginScreen(
                                serverId = effect.serverId,
                                forceTwoFa = effect.forceTwoFa
                            )
                        )
                    }
                    is ClientManagementContract.Effect.NavigateToServerEdit -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(AddServerScreen(editServerId = effect.serverId))
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            pingViewModel.effect.collect { effect ->
                when (effect) {
                    is PingTaskManagementContract.Effect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is PingTaskManagementContract.Effect.NavigateToServerRelogin -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(
                            ServerReLoginScreen(
                                serverId = effect.serverId,
                                forceTwoFa = effect.forceTwoFa
                            )
                        )
                    }
                    is PingTaskManagementContract.Effect.NavigateToServerEdit -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(AddServerScreen(editServerId = effect.serverId))
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (activeSection) {
                                NodeAdminSection.CLIENTS ->
                                    stringResource(R.string.client_management_title)
                                NodeAdminSection.PING ->
                                    stringResource(R.string.ping_task_management_title)
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = soundClick { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    },
                    actions = {
                        when (activeSection) {
                            NodeAdminSection.CLIENTS -> {
                                IconButton(
                                    onClick = soundClick { navigator.push(ClientCreateScreen()) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription =
                                            stringResource(R.string.client_management_create)
                                    )
                                }
                            }
                            NodeAdminSection.PING -> {
                                if (
                                    pingState.currentView ==
                                        PingTaskManagementContract.ContentView.TASKS
                                ) {
                                    IconButton(
                                        onClick = soundClick {
                                            pingViewModel.onEvent(
                                                PingTaskManagementContract.Event.AddClicked
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription =
                                                stringResource(R.string.ping_task_management_create)
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = activeSection == NodeAdminSection.CLIENTS,
                        onClick = { selectedSection = NodeAdminSection.CLIENTS.name },
                        icon = {
                            Icon(Icons.Default.Storage, contentDescription = null)
                        },
                        label = {
                            Text(stringResource(R.string.client_management_nav_clients))
                        }
                    )
                    NavigationBarItem(
                        selected = activeSection == NodeAdminSection.PING,
                        onClick = { selectedSection = NodeAdminSection.PING.name },
                        icon = {
                            Icon(Icons.Default.Speed, contentDescription = null)
                        },
                        label = {
                            Text(stringResource(R.string.client_management_nav_ping))
                        }
                    )
                }
            }
        ) { innerPadding ->
            when (activeSection) {
                NodeAdminSection.CLIENTS -> {
                    ClientManagementClientsPane(
                        state = clientState,
                        onEvent = clientViewModel::onEvent,
                        onOpenTerminal = { uuid, nodeName ->
                            navigator.push(SshTerminalScreen(uuid = uuid, nodeName = nodeName))
                        },
                        onNavigateToEdit = { uuid -> navigator.push(ClientEditScreen(uuid)) },
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    )
                }
                NodeAdminSection.PING -> {
                    PingTaskManagementPane(
                        state = pingState,
                        onEvent = pingViewModel::onEvent,
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ClientManagementClientsPane(
    state: ClientManagementContract.State,
    onEvent: (ClientManagementContract.Event) -> Unit,
    onOpenTerminal: (String, String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    var sortModeClients by remember(state.clients) { mutableStateOf(state.clients) }
    val displayedClients = if (state.isSortMode) sortModeClients else state.filteredClients
    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
            if (!state.isSortMode || state.isReordering) return@rememberReorderableLazyListState
            sortModeClients = sortModeClients.moveItem(from.index, to.index)
        }

    when {
        state.isLoading -> {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }
        state.error != null -> {
            AdaptiveContentPane(modifier = modifier, maxWidth = 920.dp) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(text = state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = soundClick { onEvent(ClientManagementContract.Event.Retry) }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(ClientManagementContract.Event.Refresh) },
                modifier = modifier
            ) {
                AdaptiveContentPane(modifier = Modifier.fillMaxSize(), maxWidth = 920.dp) {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = {
                                onEvent(ClientManagementContract.Event.SearchChanged(it))
                            },
                            enabled = !state.isSortMode,
                            label = {
                                Text(stringResource(R.string.client_management_search))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text =
                                    stringResource(
                                        R.string.client_management_total_count,
                                        displayedClients.size,
                                        state.clients.size
                                    ),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.client_management_mask_ip),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = state.maskIpAddress,
                                    onCheckedChange = {
                                        onEvent(
                                            ClientManagementContract.Event.ToggleMaskIpAddress(it)
                                        )
                                    },
                                    enabled = !state.isSortMode
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.client_management_sort_mode),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = state.isSortMode,
                                    onCheckedChange = {
                                        onEvent(
                                            ClientManagementContract.Event.ToggleSortMode(it)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            FilterChip(
                                selected = state.selectedGroup == null,
                                enabled = !state.isSortMode,
                                onClick = {
                                    onEvent(ClientManagementContract.Event.GroupSelected(null))
                                },
                                label = {
                                    Text(stringResource(R.string.node_filter_all))
                                }
                            )
                            state.groups.forEach { group ->
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = state.selectedGroup == group,
                                    enabled = !state.isSortMode,
                                    onClick = {
                                        onEvent(
                                            ClientManagementContract.Event.GroupSelected(group)
                                        )
                                    },
                                    label = { Text(group) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (state.isSortMode) {
                            Text(
                                text = stringResource(R.string.client_management_sort_mode_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (state.isReordering) {
                            Text(
                                text = stringResource(R.string.client_management_reordering),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (displayedClients.isEmpty()) {
                            Text(
                                text = stringResource(R.string.client_management_empty),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(items = displayedClients, key = { _, item -> item.uuid }) { index, client ->
                                    if (state.isSortMode) {
                                        ReorderableItem(
                                            state = reorderableLazyListState,
                                            key = client.uuid
                                        ) { isDragging ->
                                            SortModeClientCard(
                                                client = client,
                                                isDragging = isDragging,
                                                modifier =
                                                    Modifier.longPressDraggableHandle(
                                                        enabled = !state.isReordering,
                                                        onDragStopped = {
                                                            onEvent(
                                                                ClientManagementContract.Event.CommitReorder(
                                                                    sortModeClients.map { it.uuid }
                                                                )
                                                            )
                                                        }
                                                    )
                                            )
                                        }
                                    } else {
                                        ClientCard(
                                            client = client,
                                            maskIpAddress = state.maskIpAddress,
                                            canMoveUp = index > 0,
                                            canMoveDown = index < displayedClients.lastIndex,
                                            onShowInstallCommand = {
                                                onEvent(
                                                    ClientManagementContract.Event.ShowInstallCommandClicked(
                                                        client.uuid
                                                    )
                                                )
                                            },
                                            onOpenTerminal = {
                                                onOpenTerminal(client.uuid, client.name)
                                            },
                                            onEdit = {
                                                onNavigateToEdit(client.uuid)
                                            },
                                            onDelete = {
                                                onEvent(
                                                    ClientManagementContract.Event.DeleteClicked(
                                                        client.uuid
                                                    )
                                                )
                                            },
                                            onMoveUp = {
                                                onEvent(
                                                    ClientManagementContract.Event.MoveUpClicked(
                                                        client.uuid
                                                    )
                                                )
                                            },
                                            onMoveDown = {
                                                onEvent(
                                                    ClientManagementContract.Event.MoveDownClicked(
                                                        client.uuid
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    state.installCommandClient?.let { client ->
        InstallCommandDialog(
            client = client,
            serverBaseUrl = state.serverBaseUrl,
            onDismiss = {
                onEvent(ClientManagementContract.Event.DismissInstallCommand)
            }
        )
    }

    state.pendingDeleteClient?.let { client ->
        AlertDialog(
            onDismissRequest = { onEvent(ClientManagementContract.Event.DismissDelete) },
            title = { Text(stringResource(R.string.client_management_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.client_management_delete_confirm,
                        client.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ClientManagementContract.Event.ConfirmDelete) }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(ClientManagementContract.Event.DismissDelete) }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
