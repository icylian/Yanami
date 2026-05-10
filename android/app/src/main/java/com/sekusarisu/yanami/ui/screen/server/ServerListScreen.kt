package com.sekusarisu.yanami.ui.screen.server

import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.ServerInstance
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.nodelist.NodeListScreen
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.settings.SettingsHubScreen
import com.sekusarisu.yanami.ui.screen.soundClick

/**
 * 服务端实例列表页面
 *
 * 展示所有已保存的 Komari 服务端实例，支持添加、切换、删除操作。
 */
class ServerListScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ServerListViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val adaptiveInfo = rememberAdaptiveLayoutInfo()

        // 处理副作用
        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is ServerContract.Effect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is ServerContract.Effect.NavigateToAddServer -> {
                        navigator.push(AddServerScreen())
                    }
                    is ServerContract.Effect.NavigateToNodeList -> {
                        navigator.push(NodeListScreen())
                    }
                    else -> {}
                }
            }
        }

        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LargeTopAppBar(
                            title = { Text(stringResource(R.string.server_management)) },
                            scrollBehavior = scrollBehavior,
                            actions = {
                                IconButton(onClick = soundClick { navigator.push(SettingsHubScreen()) }) {
                                    Icon(
                                            Icons.Default.Settings,
                                            contentDescription =
                                                    stringResource(R.string.action_settings)
                                    )
                                }
                            }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = soundClick { navigator.push(AddServerScreen()) }) {
                        Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.server_add_fab_desc)
                        )
                    }
                }
        ) { innerPadding ->
            when {
                state.isLoading -> {
                    Box(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
                state.servers.isEmpty() -> {
                    AdaptiveContentPane(
                            modifier = Modifier.padding(innerPadding),
                            maxWidth = 760.dp
                    ) {
                        EmptyState(modifier = Modifier.fillMaxSize())
                    }
                }
                else -> {
                    ServerList(
                            servers = state.servers,
                            onSelect = { viewModel.onEvent(ServerContract.Event.SelectServer(it)) },
                            onDelete = { viewModel.onEvent(ServerContract.Event.DeleteServer(it)) },
                            onEdit = { navigator.push(AddServerScreen(it)) },
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            isTabletLandscape = adaptiveInfo.isTabletLandscape
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
                text = stringResource(R.string.server_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = stringResource(R.string.server_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ServerList(
        servers: List<ServerInstance>,
        onSelect: (Long) -> Unit,
        onDelete: (Long) -> Unit,
        onEdit: (Long) -> Unit,
        modifier: Modifier = Modifier,
        isTabletLandscape: Boolean
) {
    AdaptiveContentPane(modifier = modifier, maxWidth = if (isTabletLandscape) 1280.dp else 840.dp) {
        LazyVerticalGrid(
                columns =
                        if (isTabletLandscape) GridCells.Adaptive(minSize = 320.dp)
                        else GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(4.dp)) }
            items(servers, key = { it.id }) { server ->
                ServerCard(
                        server = server,
                        onSelect = { onSelect(server.id) },
                        onDelete = { onDelete(server.id) },
                        onEdit = { onEdit(server.id) }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ServerCard(
        server: ServerInstance,
        onSelect: () -> Unit,
        onDelete: () -> Unit,
        onEdit: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
            onClick = soundClick { onSelect() },
            modifier = Modifier.fillMaxWidth(),
            colors =
                    if (server.isActive) {
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        CardDefaults.cardColors()
                    }
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint =
                            if (server.isActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleMedium,
                        color =
                                if (server.isActive) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                Text(
                        text = server.baseUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                                if (server.isActive) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
            }
            if (server.isActive) {
                Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.server_active_badge),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = soundClick { onEdit() }) {
                Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = soundClick { showDeleteDialog = true }) {
                Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.server_delete_title)) },
                text = { Text(stringResource(R.string.server_delete_confirm, server.name)) },
                confirmButton = {
                    TextButton(
                            onClick = soundClick {
                                showDeleteDialog = false
                                onDelete()
                            }
                    ) {
                        Text(
                                stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = soundClick { showDeleteDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ServerListScreenEmptyPreview() {
    MaterialTheme {
        EmptyState()
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ServerCardPreview() {
    val sampleServer = ServerInstance(
        id = 1,
        name = "My Server",
        baseUrl = "https://example.com/api",
        username = "admin",
        password = "password",
        isActive = true
    )
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ServerCard(server = sampleServer, onSelect = {}, onDelete = {}, onEdit = {})
            ServerCard(server = sampleServer.copy(id = 2, name = "Offline Server", isActive = false), onSelect = {}, onDelete = {}, onEdit = {})
        }
    }
}
