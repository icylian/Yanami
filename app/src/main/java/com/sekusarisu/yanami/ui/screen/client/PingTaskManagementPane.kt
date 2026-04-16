package com.sekusarisu.yanami.ui.screen.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.AdminPingTask
import com.sekusarisu.yanami.domain.model.AdminPingTaskDraft
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.domain.model.PingTaskType
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.soundClick
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PingTaskManagementPane(
    state: PingTaskManagementContract.State,
    onEvent: (PingTaskManagementContract.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> LoadingPane(modifier)
        state.error != null -> ErrorPane(state.error.orEmpty(), onEvent, modifier)
        else -> {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(PingTaskManagementContract.Event.Refresh) },
                modifier = modifier.fillMaxSize()
            ) {
                AdaptiveContentPane(modifier = Modifier.fillMaxSize(), maxWidth = 920.dp) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = {
                                onEvent(PingTaskManagementContract.Event.SearchChanged(it))
                            },
                            enabled =
                                !(state.currentView == PingTaskManagementContract.ContentView.TASKS &&
                                    state.isSortMode),
                            label = {
                                Text(
                                    stringResource(
                                        if (state.currentView == PingTaskManagementContract.ContentView.TASKS) {
                                            R.string.ping_task_management_search
                                        } else {
                                            R.string.ping_task_management_server_search
                                        }
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected =
                                    state.currentView == PingTaskManagementContract.ContentView.TASKS,
                                onClick = {
                                    onEvent(
                                        PingTaskManagementContract.Event.ViewChanged(
                                            PingTaskManagementContract.ContentView.TASKS
                                        )
                                    )
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text(stringResource(R.string.ping_task_management_task_view))
                            }
                            SegmentedButton(
                                selected =
                                    state.currentView ==
                                        PingTaskManagementContract.ContentView.SERVERS,
                                onClick = {
                                    onEvent(
                                        PingTaskManagementContract.Event.ViewChanged(
                                            PingTaskManagementContract.ContentView.SERVERS
                                        )
                                    )
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text(stringResource(R.string.ping_task_management_server_view))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (state.currentView) {
                            PingTaskManagementContract.ContentView.TASKS -> {
                                TaskViewSection(
                                    state = state,
                                    onEvent = onEvent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            PingTaskManagementContract.ContentView.SERVERS -> {
                                ServerViewSection(
                                    state = state,
                                    onEvent = onEvent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    state.editor?.let { editor ->
        PingTaskEditorDialog(
            editor = editor,
            clients = state.clients,
            isSaving = state.isSaving,
            onDismiss = { onEvent(PingTaskManagementContract.Event.DismissEditor) },
            onNameChanged = { onEvent(PingTaskManagementContract.Event.EditorNameChanged(it)) },
            onTargetChanged = {
                onEvent(PingTaskManagementContract.Event.EditorTargetChanged(it))
            },
            onTypeChanged = { onEvent(PingTaskManagementContract.Event.EditorTypeChanged(it)) },
            onIntervalChanged = {
                onEvent(PingTaskManagementContract.Event.EditorIntervalChanged(it))
            },
            onToggleClient = {
                onEvent(PingTaskManagementContract.Event.ToggleEditorClient(it))
            },
            onSave = { onEvent(PingTaskManagementContract.Event.SaveEditor) }
        )
    }

    state.serverBindingEditor?.let { editor ->
        ServerBindingEditorDialog(
            editor = editor,
            tasks = state.tasks,
            isSaving = state.isSaving,
            onDismiss = { onEvent(PingTaskManagementContract.Event.DismissServerBinding) },
            onToggleTask = { onEvent(PingTaskManagementContract.Event.ToggleServerTask(it)) },
            onSave = { onEvent(PingTaskManagementContract.Event.SaveServerBinding) }
        )
    }

    state.pendingDeleteTask?.let { task ->
        AlertDialog(
            onDismissRequest = { onEvent(PingTaskManagementContract.Event.DismissDelete) },
            title = { Text(stringResource(R.string.ping_task_management_delete_title)) },
            text = {
                Text(stringResource(R.string.ping_task_management_delete_confirm, task.name))
            },
            confirmButton = {
                TextButton(onClick = { onEvent(PingTaskManagementContract.Event.ConfirmDelete) }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(PingTaskManagementContract.Event.DismissDelete) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskViewSection(
    state: PingTaskManagementContract.State,
    onEvent: (PingTaskManagementContract.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    var sortModeTasks by remember(state.tasks) { mutableStateOf(state.tasks) }
    val displayedTasks = if (state.isSortMode) sortModeTasks else state.filteredTasks
    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
            if (!state.isSortMode || state.isReordering) return@rememberReorderableLazyListState
            sortModeTasks = sortModeTasks.moveItem(from.index, to.index)
        }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text =
                    stringResource(
                        R.string.ping_task_management_total_count,
                        displayedTasks.size,
                        state.tasks.size
                    ),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.ping_task_management_sort_mode),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = state.isSortMode,
                    onCheckedChange = {
                        onEvent(PingTaskManagementContract.Event.ToggleSortMode(it))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedType == null,
                enabled = !state.isSortMode,
                onClick = {
                    onEvent(PingTaskManagementContract.Event.TypeFilterChanged(null))
                },
                label = { Text(stringResource(R.string.node_filter_all)) }
            )
            PingTaskType.entries.forEach { type ->
                FilterChip(
                    selected = state.selectedType == type,
                    enabled = !state.isSortMode,
                    onClick = {
                        onEvent(PingTaskManagementContract.Event.TypeFilterChanged(type))
                    },
                    label = { Text(type.toLabel()) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isSortMode) {
            Text(
                text = stringResource(R.string.ping_task_management_sort_mode_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.isReordering) {
            Text(
                text = stringResource(R.string.ping_task_management_reordering),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (displayedTasks.isEmpty()) {
            Text(
                text = stringResource(R.string.ping_task_management_empty),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(items = displayedTasks, key = { _, task -> task.id }) { _, task ->
                    if (state.isSortMode) {
                        ReorderableItem(state = reorderableLazyListState, key = task.id) { isDragging ->
                            SortModePingTaskCard(
                                task = task,
                                clients = state.clients,
                                isDragging = isDragging,
                                modifier =
                                    Modifier.longPressDraggableHandle(
                                        enabled = !state.isReordering,
                                        onDragStopped = {
                                            onEvent(
                                                PingTaskManagementContract.Event.CommitReorder(
                                                    sortModeTasks.map { it.id }
                                                )
                                            )
                                        }
                                    )
                            )
                        }
                    } else {
                        PingTaskCard(
                            task = task,
                            onEdit = {
                                onEvent(PingTaskManagementContract.Event.EditClicked(task.id))
                            },
                            onDelete = {
                                onEvent(PingTaskManagementContract.Event.DeleteClicked(task.id))
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ServerViewSection(
    state: PingTaskManagementContract.State,
    onEvent: (PingTaskManagementContract.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.filteredClients.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            Text(
                text =
                    stringResource(
                        R.string.ping_task_management_server_total_count,
                        0,
                        state.clients.size
                    ),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.ping_task_management_server_empty),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text =
                    stringResource(
                        R.string.ping_task_management_server_total_count,
                        state.filteredClients.size,
                        state.clients.size
                    ),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        items(items = state.filteredClients, key = { it.uuid }) { client ->
            val boundCount = state.tasks.count { task -> client.uuid in task.clients }
            ServerBindingCard(
                client = client,
                boundCount = boundCount,
                onEdit = {
                    onEvent(
                        PingTaskManagementContract.Event.EditServerBindingClicked(client.uuid)
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PingTaskCard(
    task: AdminPingTask,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.target,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                AssistChip(
                    colors =
                        AssistChipDefaults.assistChipColors(
                            MaterialTheme.colorScheme.primaryContainer
                        ),
                    onClick = {},
                    label = { Text(task.type.toLabel()) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    colors =
                        AssistChipDefaults.assistChipColors(
                            MaterialTheme.colorScheme.primaryContainer
                        ),
                    onClick = {},
                    label = { Text("${task.interval} s") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text =
                    stringResource(R.string.ping_task_management_clients) + ": " + task.clients.size,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ServerBindingCard(
    client: ManagedClient,
    boundCount: Int,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = client.name.ifBlank { client.uuid }
    val secondaryInfo =
        listOf(client.region, client.group, client.os, client.version)
            .filter { it.isNotBlank() }
            .joinToString(" · ")

    OutlinedCard(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
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
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text =
                    stringResource(R.string.ping_task_management_server_bound_count, boundCount),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SortModePingTaskCard(
    task: AdminPingTask,
    clients: List<ManagedClient>,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    val clientNameMap = clients.associateBy({ it.uuid }, { it.name.ifBlank { it.uuid } })
    val previewClients =
        task.clients.take(3).map { uuid -> clientNameMap[uuid] ?: uuid }.joinToString(" · ")

    OutlinedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (isDragging) {
                        Modifier.background(
                            color = MaterialTheme.colorScheme.primaryContainer,
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
                Text(text = task.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${task.type.toLabel()} · ${task.target}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (previewClients.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = previewClients,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PingTaskEditorDialog(
    editor: PingTaskManagementContract.EditorState,
    clients: List<ManagedClient>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onNameChanged: (String) -> Unit,
    onTargetChanged: (String) -> Unit,
    onTypeChanged: (PingTaskType) -> Unit,
    onIntervalChanged: (String) -> Unit,
    onToggleClient: (String) -> Unit,
    onSave: () -> Unit
) {
    val draft: AdminPingTaskDraft = editor.draft

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editor.mode == PingTaskManagementContract.EditorMode.CREATE) {
                        R.string.ping_task_management_create
                    } else {
                        R.string.action_edit
                    }
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChanged,
                    label = { Text(stringResource(R.string.client_edit_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.target,
                    onValueChange = onTargetChanged,
                    label = { Text(stringResource(R.string.ping_task_management_target)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.interval,
                    onValueChange = onIntervalChanged,
                    label = { Text(stringResource(R.string.ping_task_management_interval_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text(
                    text = stringResource(R.string.ping_task_management_type),
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PingTaskType.entries.forEach { type ->
                        FilterChip(
                            selected = draft.type == type,
                            onClick = { onTypeChanged(type) },
                            label = { Text(type.toLabel()) }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.ping_task_management_clients),
                    style = MaterialTheme.typography.labelLarge
                )
                if (clients.isEmpty()) {
                    Text(
                        text = stringResource(R.string.ping_task_management_no_clients),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        clients.forEach { client ->
                            FilterChip(
                                selected = draft.selectedClientUuids.contains(client.uuid),
                                onClick = { onToggleClient(client.uuid) },
                                label = { Text(client.name.ifBlank { client.uuid }) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !isSaving) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServerBindingEditorDialog(
    editor: PingTaskManagementContract.ServerBindingEditorState,
    tasks: List<AdminPingTask>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onToggleTask: (Int) -> Unit,
    onSave: () -> Unit
) {
    val clientLabel = editor.client.name.ifBlank { editor.client.uuid }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.ping_task_management_server_binding_title,
                    clientLabel
                )
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ping_task_management_server_binding_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (tasks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.ping_task_management_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tasks.forEach { task ->
                            FilterChip(
                                selected = task.id in editor.selectedTaskIds,
                                onClick = { onToggleTask(task.id) },
                                label = { Text(task.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !isSaving) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun LoadingPane(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorPane(
    error: String,
    onEvent: (PingTaskManagementContract.Event) -> Unit,
    modifier: Modifier
) {
    AdaptiveContentPane(modifier = modifier, maxWidth = 920.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = soundClick { onEvent(PingTaskManagementContract.Event.Retry) }) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
private fun PingTaskType.toLabel(): String {
    return stringResource(
        when (this) {
            PingTaskType.ICMP -> R.string.ping_task_type_icmp
            PingTaskType.TCP -> R.string.ping_task_type_tcp
            PingTaskType.HTTP -> R.string.ping_task_type_http
        }
    )
}

private fun List<AdminPingTask>.moveItem(fromIndex: Int, toIndex: Int): List<AdminPingTask> {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) return this
    return toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}
