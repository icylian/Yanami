package com.sekusarisu.yanami.ui.screen.client

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.server.AddServerScreen
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginScreen
import com.sekusarisu.yanami.ui.screen.soundClick

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
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                OutlinedTextField(
                                        value = state.searchQuery,
                                        onValueChange = {
                                            viewModel.onEvent(
                                                    ClientManagementContract.Event.SearchChanged(it)
                                            )
                                        },
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
                                                            state.filteredClients.size,
                                                            state.clients.size
                                                    ),
                                            style = MaterialTheme.typography.bodyMedium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                                text =
                                                        stringResource(
                                                                R.string.client_management_show_hidden
                                                        ),
                                                style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                                checked = state.showHidden,
                                                onCheckedChange = {
                                                    viewModel.onEvent(
                                                            ClientManagementContract.Event
                                                                    .ToggleShowHidden(it)
                                                    )
                                                }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    FilterChip(
                                            selected = state.selectedGroup == null,
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

                                if (state.filteredClients.isEmpty()) {
                                    Text(
                                            text =
                                                    stringResource(
                                                            R.string.client_management_empty
                                                    ),
                                            style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        itemsIndexed(
                                                items = state.filteredClients,
                                                key = { _, item -> item.uuid }
                                        ) { index, client ->
                                            ClientCard(
                                                    client = client,
                                                    canMoveUp = index > 0,
                                                    canMoveDown =
                                                            index < state.filteredClients.lastIndex,
                                                    onShowToken = {
                                                        viewModel.onEvent(
                                                                ClientManagementContract.Event
                                                                        .ShowTokenClicked(client.uuid)
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
                                                                        .DeleteClicked(client.uuid)
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
                                                                        .MoveDownClicked(client.uuid)
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
        canMoveUp: Boolean,
        canMoveDown: Boolean,
        onShowToken: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onMoveUp: () -> Unit,
        onMoveDown: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(client.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                            text =
                                    listOf(client.group, client.region, client.os)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium
                    )
                }
                AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                    stringResource(
                                            R.string.client_management_weight_label,
                                            client.weight
                                    )
                            )
                        }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (client.hidden) {
                    AssistChip(
                            onClick = {},
                            label = {
                                Text(stringResource(R.string.client_management_hidden_badge))
                            }
                    )
                }
                if (client.tags.isNotBlank()) {
                    AssistChip(onClick = {}, label = { Text(client.tags) })
                }
                if (client.cpuName.isNotBlank()) {
                    AssistChip(onClick = {}, label = { Text(client.cpuName) })
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onShowToken) {
                    Icon(Icons.Default.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.client_management_show_token))
                }
                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_edit))
                }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_delete))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null)
                }
                OutlinedButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text =
                            stringResource(
                                    R.string.client_management_updated_at,
                                    client.updatedAt
                            ),
                    style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
