package com.sekusarisu.yanami.ui.screen.client

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.domain.model.calculateExpiryStatus
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.ExpiryBadge
import com.sekusarisu.yanami.ui.screen.server.AddServerScreen
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginScreen
import com.sekusarisu.yanami.ui.screen.soundClick
import com.sekusarisu.yanami.ui.theme.ThemeColor
import com.sekusarisu.yanami.ui.theme.YanamiTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

class ClientManagementScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ClientManagementViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val clipboard = LocalClipboardManager.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val lazyListState = rememberLazyListState()
        var sortModeClients by remember(state.clients) { mutableStateOf(state.clients) }
        val displayedClients = if (state.isSortMode) sortModeClients else state.filteredClients
        val reorderableLazyListState =
                rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
                    if (!state.isSortMode || state.isReordering)
                            return@rememberReorderableLazyListState
                    sortModeClients = sortModeClients.moveItem(from.index, to.index)
                }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshIfLoaded()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
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

        Scaffold(
                topBar = {
                    TopAppBar(
                            title = {
                                Text(
                                        state.serverName.ifBlank {
                                            stringResource(R.string.client_management_title)
                                        }
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
                                IconButton(
                                        onClick =
                                                soundClick {
                                                    viewModel.onEvent(
                                                            ClientManagementContract.Event.AddClicked
                                                    )
                                                }
                                ) {
                                    Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription =
                                                    stringResource(
                                                            R.string.client_management_create
                                                    )
                                    )
                                }
                            }
                    )
                }
        ) { innerPadding ->
            when {
                state.isLoading -> {
                    Column(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    AdaptiveContentPane(
                            modifier = Modifier.padding(innerPadding),
                            maxWidth = 920.dp
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text(
                                    text = state.error.orEmpty(),
                                    color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                    onClick =
                                            soundClick {
                                                viewModel.onEvent(
                                                        ClientManagementContract.Event.Retry
                                                )
                                            }
                            ) { Text(stringResource(R.string.action_retry)) }
                        }
                    }
                }
                else -> {
                    PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = {
                                viewModel.onEvent(ClientManagementContract.Event.Refresh)
                            },
                            modifier = Modifier.fillMaxSize().padding(innerPadding)
                    ) {
                        AdaptiveContentPane(modifier = Modifier.fillMaxSize(), maxWidth = 920.dp) {
                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                                OutlinedTextField(
                                        value = state.searchQuery,
                                        onValueChange = {
                                            viewModel.onEvent(
                                                    ClientManagementContract.Event.SearchChanged(it)
                                            )
                                        },
                                        enabled = !state.isSortMode,
                                        label = {
                                            Text(
                                                    stringResource(
                                                            R.string.client_management_search
                                                    )
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
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
                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                    text =
                                                            stringResource(
                                                                    R.string
                                                                            .client_management_mask_ip
                                                            ),
                                                    style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Switch(
                                                    checked = state.maskIpAddress,
                                                    onCheckedChange = {
                                                        viewModel.onEvent(
                                                                ClientManagementContract.Event
                                                                        .ToggleMaskIpAddress(it)
                                                        )
                                                    },
                                                    enabled = !state.isSortMode
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                    text =
                                                            stringResource(
                                                                    R.string
                                                                            .client_management_sort_mode
                                                            ),
                                                    style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Switch(
                                                    checked = state.isSortMode,
                                                    onCheckedChange = {
                                                        viewModel.onEvent(
                                                                ClientManagementContract.Event
                                                                        .ToggleSortMode(it)
                                                        )
                                                    }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    FilterChip(
                                            selected = state.selectedGroup == null,
                                            enabled = !state.isSortMode,
                                            onClick = {
                                                viewModel.onEvent(
                                                        ClientManagementContract.Event.GroupSelected(
                                                                null
                                                        )
                                                )
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
                                                    viewModel.onEvent(
                                                            ClientManagementContract.Event
                                                                    .GroupSelected(group)
                                                    )
                                                },
                                                label = { Text(group) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (state.isSortMode) {
                                    Text(
                                            text =
                                                    stringResource(
                                                            R.string.client_management_sort_mode_hint
                                                    ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (state.isReordering) {
                                    Text(
                                            text =
                                                    stringResource(
                                                            R.string.client_management_reordering
                                                    ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (displayedClients.isEmpty()) {
                                    Text(
                                            text =
                                                    stringResource(
                                                            R.string.client_management_empty
                                                    ),
                                            style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    LazyColumn(
                                            state = lazyListState,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        itemsIndexed(
                                                items = displayedClients,
                                                key = { _, item -> item.uuid }
                                        ) { index, client ->
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
                                                                            enabled =
                                                                                    !state
                                                                                            .isReordering,
                                                                            onDragStopped = {
                                                                                viewModel.onEvent(
                                                                                        ClientManagementContract
                                                                                                .Event
                                                                                                .CommitReorder(
                                                                                                        sortModeClients
                                                                                                                .map {
                                                                                                                    it
                                                                                                                            .uuid
                                                                                                                }
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
                                                        canMoveDown =
                                                                index < displayedClients.lastIndex,
                                                        onShowToken = {
                                                            viewModel.onEvent(
                                                                    ClientManagementContract.Event
                                                                            .ShowTokenClicked(
                                                                                    client.uuid
                                                                            )
                                                            )
                                                        },
                                                        onEdit = {
                                                            viewModel.onEvent(
                                                                    ClientManagementContract.Event
                                                                            .EditClicked(client.uuid)
                                                            )
                                                        },
                                                        onDelete = {
                                                            viewModel.onEvent(
                                                                    ClientManagementContract.Event
                                                                            .DeleteClicked(
                                                                                    client.uuid
                                                                            )
                                                            )
                                                        },
                                                        onMoveUp = {
                                                            viewModel.onEvent(
                                                                    ClientManagementContract.Event
                                                                            .MoveUpClicked(client.uuid)
                                                            )
                                                        },
                                                        onMoveDown = {
                                                            viewModel.onEvent(
                                                                    ClientManagementContract.Event
                                                                            .MoveDownClicked(
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
        }

        state.tokenDialogClient?.let { client ->
            AlertDialog(
                    onDismissRequest = {
                        viewModel.onEvent(ClientManagementContract.Event.DismissToken)
                    },
                    title = { Text(stringResource(R.string.client_management_token_title)) },
                    text = {
                        Column {
                            Text(client.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(client.uuid, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(client.token, style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    confirmButton = {
                        TextButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(client.token))
                                    Toast.makeText(
                                                    context,
                                                    context.getString(
                                                            R.string.client_management_token_copied
                                                    ),
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                }
                        ) { Text(stringResource(R.string.action_copy)) }
                    },
                    dismissButton = {
                        TextButton(
                                onClick = {
                                    viewModel.onEvent(ClientManagementContract.Event.DismissToken)
                                }
                        ) { Text(stringResource(R.string.action_close)) }
                    }
            )
        }

        state.pendingDeleteClient?.let { client ->
            AlertDialog(
                    onDismissRequest = {
                        viewModel.onEvent(ClientManagementContract.Event.DismissDelete)
                    },
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
                                onClick = {
                                    viewModel.onEvent(ClientManagementContract.Event.ConfirmDelete)
                                }
                        ) { Text(stringResource(R.string.action_delete)) }
                    },
                    dismissButton = {
                        TextButton(
                                onClick = {
                                    viewModel.onEvent(ClientManagementContract.Event.DismissDelete)
                                }
                        ) { Text(stringResource(R.string.action_cancel)) }
                    }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientCard(
        client: ManagedClient,
        maskIpAddress: Boolean,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
        onShowToken: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onMoveUp: () -> Unit,
        onMoveDown: () -> Unit,
        modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            val expiryStatus = client.calculateExpiryStatus()
            val secondaryInfo =
                    listOf(client.group, client.os, client.version)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
            ) {
                Text(
                        text = client.region + " " + client.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    if (expiryStatus != null) {
                        ExpiryBadge(expiryStatus = expiryStatus)
                    }
                    if (client.hidden) {
                        HiddenBadge()
                    }
                }
            }

            if (secondaryInfo.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = secondaryInfo,
                        style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState(), true),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (client.ipv4.isNotBlank()) {
                    AssistChip(
                            onClick = {
                                clipboard.setText(AnnotatedString(client.ipv4))
                                Toast.makeText(
                                                context,
                                                context.getString(
                                                        R.string.client_management_ip_copied
                                                ),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            },
                            label = {
                                Text(
                                        stringResource(
                                                R.string.client_management_ipv4,
                                                client.ipv4.maskedIp(maskIpAddress)
                                        )
                                )
                            }
                    )
                }
                if (client.ipv6.isNotBlank()) {
                    AssistChip(
                            onClick = {
                                clipboard.setText(AnnotatedString(client.ipv6))
                                Toast.makeText(
                                                context,
                                                context.getString(
                                                        R.string.client_management_ip_copied
                                                ),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            },
                            label = {
                                Text(
                                        stringResource(
                                                R.string.client_management_ipv6,
                                                client.ipv6.maskedIp(maskIpAddress)
                                        )
                                )
                            }
                    )
                }
//                if (client.version.isNotBlank()) {
//                    AssistChip(
//                            onClick = {},
//                            label = {
//                                Text(
//                                        stringResource(
//                                                R.string.client_management_agent_version,
//                                                client.version
//                                        )
//                                )
//                            }
//                    )
//                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                    text =
                            if (client.price < 0) {
                                stringResource(
                                        R.string.client_management_billing_free,
                                        client.billingCycle,
                                        if (client.autoRenewal)
                                                stringResource(
                                                        R.string.client_management_auto_renewal_on
                                                )
                                        else
                                                stringResource(
                                                        R.string.client_management_auto_renewal_off
                                                )
                                )
                            } else {
                                stringResource(
                                        R.string.client_management_billing_summary,
                                        client.currency,
                                        client.price.billingText(),
                                        client.billingCycle,
                                        if (client.autoRenewal)
                                                stringResource(
                                                        R.string.client_management_auto_renewal_on
                                                )
                                        else
                                                stringResource(
                                                        R.string.client_management_auto_renewal_off
                                                )
                                )
                            },
                    style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onShowToken) {
                    Icon(Icons.Default.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.client_management_show_token))
                }
                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(stringResource(R.string.action_edit))
                }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(stringResource(R.string.action_delete))
                }
            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                OutlinedButton(onClick = onMoveUp, enabled = canMoveUp) {
//                    Icon(Icons.Default.ArrowUpward, contentDescription = null)
//                }
//                OutlinedButton(onClick = onMoveDown, enabled = canMoveDown) {
//                    Icon(Icons.Default.ArrowDownward, contentDescription = null)
//                }
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Text(
//                    text =
//                            stringResource(
//                                    R.string.client_management_updated_at,
//                                    client.updatedAt
//                            ),
//                    style = MaterialTheme.typography.bodySmall
//            )
        }
    }
}

@Composable
private fun SortModeClientCard(
        client: ManagedClient,
        isDragging: Boolean,
        modifier: Modifier = Modifier
) {
    val secondaryInfo =
            listOf(client.group, client.os, client.version)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")

    OutlinedCard(
            modifier =
                    modifier
                            .fillMaxWidth()
                            .then(
                                    if (isDragging) {
                                        Modifier
                                                .background(
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer,
                                                        shape = RoundedCornerShape(12.dp)
                                                )
                                    } else {
                                        Modifier
                                    }
                            )
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = client.region + " " + client.name,
                        style = MaterialTheme.typography.titleMedium
                )
                if (secondaryInfo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                            text = secondaryInfo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun List<ManagedClient>.moveItem(fromIndex: Int, toIndex: Int): List<ManagedClient> {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) return this
    return toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
}

@Composable
private fun HiddenBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                Icons.Default.VisibilityOff,
                contentDescription =
                    stringResource(R.string.node_expiry_badge_remaining),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = stringResource(R.string.client_management_hidden_badge),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun String.maskedIp(mask: Boolean): String {
    if (!mask || isBlank()) return this
    return if (contains('.')) {
        val parts = split('.')
        if (parts.size == 4) "${parts[0]}.${parts[1]}.*.*" else this
    } else if (contains(':')) {
        maskIpv6Address()
    } else {
        this
    }
}

private fun String.maskIpv6Address(): String {
    val normalized = substringBefore('%')
    val ipv4Tail = normalized.substringAfterLast(':', "")
    if (ipv4Tail.contains('.')) {
        return this
    }

    val expanded = normalized.expandIpv6Segments() ?: return this
    return expanded.take(2).joinToString(":") { it.trimLeadingIpv6Zeros() } + "::*"
}

private fun String.expandIpv6Segments(): List<String>? {
    val value = trim().lowercase()
    if (value.isEmpty()) return null
    if (value.split("::").size > 2) return null

    val hasCompression = value.contains("::")
    val segments =
            if (hasCompression) {
                val halves = value.split("::", limit = 2)
                val left = halves[0].split(':').filter { it.isNotEmpty() }
                val right = halves[1].split(':').filter { it.isNotEmpty() }
                val missing = 8 - (left.size + right.size)
                if (missing < 1) return null
                left + List(missing) { "0" } + right
            } else {
                value.split(':')
            }

    if (segments.size != 8) return null
    if (segments.any { it.length > 4 || it.any { ch -> !ch.isDigit() && ch !in 'a'..'f' } }) {
        return null
    }

    return segments.map { it.padStart(4, '0') }
}

private fun String.trimLeadingIpv6Zeros(): String {
    val trimmed = trimStart('0')
    return if (trimmed.isEmpty()) "0" else trimmed
}

private fun Double.billingText(): String {
    val asLong = toLong()
    return if (asLong.toDouble() == this) asLong.toString() else toString()
}

@Preview(showBackground = true)
@Composable
private fun ClientCardPreviewUnmasked() {
    YanamiTheme(themeColor = ThemeColor.BLUE_MTB, darkTheme = true) {
        ClientCard(
                client = previewManagedClient(),
                maskIpAddress = false,
                canMoveUp = true,
                canMoveDown = true,
                onShowToken = {},
                onEdit = {},
                onDelete = {},
                onMoveUp = {},
                onMoveDown = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClientCardPreviewMasked() {
    YanamiTheme(themeColor = ThemeColor.BLUE_MTB, darkTheme = true) {
        ClientCard(
                client = previewManagedClient(hidden = true),
                maskIpAddress = true,
                canMoveUp = false,
                canMoveDown = true,
                onShowToken = {},
                onEdit = {},
                onDelete = {},
                onMoveUp = {},
                onMoveDown = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SortModeClientCardPreview() {
    YanamiTheme(themeColor = ThemeColor.BLUE_MTB, darkTheme = true) {
        SortModeClientCard(client = previewManagedClient(hidden = true), isDragging = false)
    }
}

@Preview(name = "Client List", showBackground = true, widthDp = 1280, heightDp = 900)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientManagementContentPreview() {
    YanamiTheme(themeColor = ThemeColor.BLUE_MTB, darkTheme = true) {
        Scaffold(
                topBar = {
                    TopAppBar(
                            title = { Text("Komari Admin") },
                            navigationIcon = {
                                IconButton(onClick = {}) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = {}) {
                                    Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null
                                    )
                                }
                            }
                    )
                }
        ) { innerPadding ->
            AdaptiveContentPane(modifier = Modifier.padding(innerPadding), maxWidth = 920.dp) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            label = { Text("搜索客户端…") },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("已显示 3 / 3")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("IP 打码")
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(checked = true, onCheckedChange = {})
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        FilterChip(selected = true, onClick = {}, label = { Text("全部") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = false, onClick = {}, label = { Text("ISIF") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = false, onClick = {}, label = { Text("PROD") })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(previewManagedClients()) { index, client ->
                            ClientCard(
                                    client = client,
                                    maskIpAddress = true,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < previewManagedClients().lastIndex,
                                    onShowToken = {},
                                    onEdit = {},
                                    onDelete = {},
                                    onMoveUp = {},
                                    onMoveDown = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Client List Sort Mode", showBackground = true, widthDp = 1280, heightDp = 900)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientManagementSortModePreview() {
    YanamiTheme(themeColor = ThemeColor.BLUE_MTB, darkTheme = true) {
        Scaffold(
                topBar = {
                    TopAppBar(
                            title = { Text("Komari Admin") },
                            navigationIcon = {
                                IconButton(onClick = {}) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = {}) {
                                    Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null
                                    )
                                }
                            }
                    )
                }
        ) { innerPadding ->
            AdaptiveContentPane(modifier = Modifier.padding(innerPadding), maxWidth = 920.dp) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            enabled = false,
                            label = { Text("搜索客户端…") },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("已显示 3 / 3")
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("IP 打码")
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(checked = false, onCheckedChange = {}, enabled = false)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("排序模式")
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(checked = true, onCheckedChange = {})
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        FilterChip(
                                selected = true,
                                onClick = {},
                                enabled = false,
                                label = { Text("全部") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                                selected = false,
                                onClick = {},
                                enabled = false,
                                label = { Text("ISIF") }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                            text = "长按卡片可拖拽排序",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(previewManagedClients()) { _, client ->
                            SortModeClientCard(client = client, isDragging = false)
                        }
                    }
                }
            }
        }
    }
}

private fun previewManagedClients(): List<ManagedClient> =
        listOf(
                previewManagedClient(),
                previewManagedClient(
                        uuid = "11111111-2222-4333-8444-555555555555",
                        name = "DEMO_SG_EDGE",
                        ipv4 = "198.51.100.24",
                        ipv6 = "",
                        version = "1.2.0-preview",
                        price = 19.9,
                        billingCycle = 30,
                        expiredAt = "2026-09-15T00:00:00Z",
                        hidden = false,
                        updatedAt = "2026-04-01T08:12:00Z"
                ),
                previewManagedClient(
                        uuid = "66666666-7777-4888-9999-aaaaaaaaaaaa",
                        name = "DEMO_JP_CORE",
                        ipv4 = "203.0.113.88",
                        ipv6 = "2001:db8:abcd:1200::42",
                        version = "1.2.1",
                        price = 29.0,
                        billingCycle = 90,
                        expiredAt = "2026-12-01T00:00:00Z",
                        hidden = true,
                        updatedAt = "2026-04-03T21:45:00Z"
                )
        )

private fun previewManagedClient(
        uuid: String = "d3b07384-d9a2-4f1a-a6ab-7c8d9e0f1122",
        name: String = "DEMO_HK_NODE_A",
        ipv4: String = "192.168.242.15",
        ipv6: String = "2001:db8:10:20::15",
        version: String = "1.2.0",
        price: Double = 12.5,
        billingCycle: Int = 30,
        expiredAt: String? = "2026-08-05T00:00:00Z",
        hidden: Boolean = false,
        updatedAt: String = "2026-04-05T09:21:50Z"
): ManagedClient =
        ManagedClient(
                uuid = uuid,
                token = "demo_token_preview_01",
                name = name,
                cpuName = "Virtual CPU",
                virtualization = "kvm",
                arch = "amd64",
                cpuCores = 2,
                os = "Debian GNU/Linux 13",
                kernelVersion = "6.12.0-demo-cloud-amd64",
                gpuName = "None",
                ipv4 = ipv4,
                ipv6 = ipv6,
                region = "Demo",
                remark = "",
                publicRemark = "",
                memTotal = 2147483648,
                swapTotal = 0,
                diskTotal = 53687091200,
                version = version,
                weight = 5,
                price = price,
                billingCycle = billingCycle,
                autoRenewal = false,
                currency = "$",
                expiredAt = expiredAt,
                group = "DEMO",
                tags = "",
                hidden = hidden,
                trafficLimit = 0,
                trafficLimitType = "max",
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = updatedAt
        )
