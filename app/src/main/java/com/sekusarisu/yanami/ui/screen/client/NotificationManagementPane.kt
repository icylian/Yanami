package com.sekusarisu.yanami.ui.screen.client

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.LoadNotificationDraft
import com.sekusarisu.yanami.domain.model.LoadNotificationMetric
import com.sekusarisu.yanami.domain.model.LoadNotificationTask
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.soundClick

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotificationManagementPane(
    state: NotificationManagementContract.State,
    onEvent: (NotificationManagementContract.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> NotificationLoadingPane(modifier)
        state.error != null -> NotificationErrorPane(state.error.orEmpty(), onEvent, modifier)
        else -> {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(NotificationManagementContract.Event.Refresh) },
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
                                onEvent(NotificationManagementContract.Event.SearchChanged(it))
                            },
                            label = {
                                Text(
                                    stringResource(
                                        if (state.currentView == NotificationManagementContract.ContentView.OFFLINE) {
                                            R.string.notification_management_search_offline
                                        } else {
                                            R.string.notification_management_search_load
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
                                    state.currentView ==
                                        NotificationManagementContract.ContentView.OFFLINE,
                                onClick = {
                                    onEvent(
                                        NotificationManagementContract.Event.ViewChanged(
                                            NotificationManagementContract.ContentView.OFFLINE
                                        )
                                    )
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text(stringResource(R.string.notification_management_view_offline))
                            }
                            SegmentedButton(
                                selected =
                                    state.currentView ==
                                        NotificationManagementContract.ContentView.LOAD,
                                onClick = {
                                    onEvent(
                                        NotificationManagementContract.Event.ViewChanged(
                                            NotificationManagementContract.ContentView.LOAD
                                        )
                                    )
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text(stringResource(R.string.notification_management_view_load))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (state.currentView) {
                            NotificationManagementContract.ContentView.OFFLINE -> {
                                OfflineNotificationSection(
                                    state = state,
                                    onEvent = onEvent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            NotificationManagementContract.ContentView.LOAD -> {
                                LoadNotificationSection(
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

    state.offlineEditor?.let { editor ->
        OfflineNotificationEditorDialog(
            editor = editor,
            isSaving = state.isSaving,
            onDismiss = { onEvent(NotificationManagementContract.Event.DismissOfflineEditor) },
            onGracePeriodChanged = {
                onEvent(NotificationManagementContract.Event.OfflineEditorGracePeriodChanged(it))
            },
            onSave = { onEvent(NotificationManagementContract.Event.SaveOfflineEditor) }
        )
    }

    state.loadEditor?.let { editor ->
        LoadNotificationEditorDialog(
            editor = editor,
            clients = state.clients,
            isSaving = state.isSaving,
            onDismiss = { onEvent(NotificationManagementContract.Event.DismissLoadEditor) },
            onNameChanged = {
                onEvent(NotificationManagementContract.Event.LoadEditorNameChanged(it))
            },
            onMetricChanged = {
                onEvent(NotificationManagementContract.Event.LoadEditorMetricChanged(it))
            },
            onThresholdChanged = {
                onEvent(NotificationManagementContract.Event.LoadEditorThresholdChanged(it))
            },
            onRatioChanged = {
                onEvent(NotificationManagementContract.Event.LoadEditorRatioChanged(it))
            },
            onIntervalChanged = {
                onEvent(NotificationManagementContract.Event.LoadEditorIntervalChanged(it))
            },
            onToggleClient = {
                onEvent(NotificationManagementContract.Event.ToggleLoadEditorClient(it))
            },
            onSave = { onEvent(NotificationManagementContract.Event.SaveLoadEditor) }
        )
    }

    state.pendingDeleteLoadTask?.let { task ->
        AlertDialog(
            onDismissRequest = { onEvent(NotificationManagementContract.Event.DismissLoadDelete) },
            title = { Text(stringResource(R.string.notification_management_load_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.notification_management_load_delete_confirm,
                        task.name
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(NotificationManagementContract.Event.ConfirmLoadDelete) }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(NotificationManagementContract.Event.DismissLoadDelete) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun OfflineNotificationSection(
    state: NotificationManagementContract.State,
    onEvent: (NotificationManagementContract.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text =
                stringResource(
                    R.string.notification_management_total_count,
                    state.filteredOfflineItems.size,
                    state.offlineItems.size
                ),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.filteredOfflineItems.isEmpty()) {
            Text(
                text = stringResource(R.string.notification_management_offline_empty),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items = state.filteredOfflineItems, key = { it.client.uuid }) { item ->
                    OfflineNotificationCard(item = item, onEvent = onEvent, isSaving = state.isSaving)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun OfflineNotificationCard(
    item: NotificationManagementContract.OfflineItem,
    onEvent: (NotificationManagementContract.Event) -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    val client = item.client
    val config = item.config
    val secondaryInfo =
        listOf(client.group, client.os, client.version).filter { it.isNotBlank() }.joinToString(" · ")

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
                        text = client.region + " " + client.name,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = {
                            onEvent(
                                NotificationManagementContract.Event.OfflineEnabledChanged(
                                    client.uuid,
                                    it
                                )
                            )
                        },
                        enabled = !isSaving
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState(), true),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    colors =
                        AssistChipDefaults.assistChipColors(MaterialTheme.colorScheme.primaryContainer),
                    onClick = {
                        onEvent(NotificationManagementContract.Event.EditOfflineClicked(client.uuid))
                    },
                    label = {
                        Text(
                            stringResource(
                                R.string.notification_management_offline_grace_period,
                                config.gracePeriod
                            )
                        )
                    }
                )
                config.lastNotified?.takeIf { it.isNotBlank() }?.let { lastNotified ->
                    AssistChip(
                        colors =
                            AssistChipDefaults.assistChipColors(
                                MaterialTheme.colorScheme.primaryContainer
                            ),
                        onClick = {},
                        label = {
                            Text(
                                stringResource(
                                    R.string.notification_management_offline_last_notified,
                                    lastNotified
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadNotificationSection(
    state: NotificationManagementContract.State,
    onEvent: (NotificationManagementContract.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text =
                stringResource(
                    R.string.notification_management_total_count,
                    state.filteredLoadTasks.size,
                    state.loadTasks.size
                ),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.filteredLoadTasks.isEmpty()) {
            Text(
                text = stringResource(R.string.notification_management_load_empty),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items = state.filteredLoadTasks, key = { it.id }) { task ->
                    LoadNotificationCard(task = task, clients = state.clients, onEvent = onEvent)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun LoadNotificationCard(
    task: LoadNotificationTask,
    clients: List<ManagedClient>,
    onEvent: (NotificationManagementContract.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val clientNameMap = clients.associateBy({ it.uuid }, { it.name.ifBlank { it.uuid } })
    val clientLabels = task.clients.map { uuid -> clientNameMap[uuid] ?: uuid }

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
                        text =
                            stringResource(
                                R.string.notification_management_load_metric_value,
                                task.metric.toLoadMetricLabel()
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NotificationBadge(
                        text =
                            stringResource(
                                R.string.notification_management_load_threshold_value,
                                task.threshold.toString()
                            )
                    )
                    NotificationBadge(
                        text =
                            stringResource(
                                R.string.notification_management_load_ratio_value,
                                task.ratio.toString()
                            )
                    )
                    NotificationBadge(
                        text =
                            stringResource(
                                R.string.notification_management_load_interval_value,
                                task.interval
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState(), true),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                clientLabels.forEach { label ->
                    AssistChip(
                        colors =
                            AssistChipDefaults.assistChipColors(
                                MaterialTheme.colorScheme.primaryContainer
                            ),
                        onClick = {},
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onEvent(NotificationManagementContract.Event.LoadEditClicked(task.id)) }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
                IconButton(onClick = { onEvent(NotificationManagementContract.Event.LoadDeleteClicked(task.id)) }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun OfflineNotificationEditorDialog(
    editor: NotificationManagementContract.OfflineEditorState,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onGracePeriodChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.notification_management_offline_edit_title,
                    editor.client.name.ifBlank { editor.client.uuid }
                )
            )
        },
        text = {
            OutlinedTextField(
                value = editor.gracePeriod,
                onValueChange = onGracePeriodChanged,
                label = { Text(stringResource(R.string.notification_management_offline_grace_period_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LoadNotificationEditorDialog(
    editor: NotificationManagementContract.LoadEditorState,
    clients: List<ManagedClient>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onNameChanged: (String) -> Unit,
    onMetricChanged: (String) -> Unit,
    onThresholdChanged: (String) -> Unit,
    onRatioChanged: (String) -> Unit,
    onIntervalChanged: (String) -> Unit,
    onToggleClient: (String) -> Unit,
    onSave: () -> Unit
) {
    val draft: LoadNotificationDraft = editor.draft
    var metricExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editor.mode == NotificationManagementContract.EditorMode.CREATE) {
                        R.string.notification_management_load_create
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
                        .heightIn(max = 520.dp)
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
                ExposedDropdownMenuBox(
                    expanded = metricExpanded,
                    onExpandedChange = { metricExpanded = it }
                ) {
                    OutlinedTextField(
                        value = draft.metric.toLoadMetricLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.notification_management_load_metric)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = metricExpanded)
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = metricExpanded,
                        onDismissRequest = { metricExpanded = false }
                    ) {
                        LoadNotificationMetric.entries.forEach { metric ->
                            DropdownMenuItem(
                                text = { Text(metric.toLabel()) },
                                onClick = {
                                    onMetricChanged(metric.apiValue)
                                    metricExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = draft.threshold,
                    onValueChange = onThresholdChanged,
                    label = { Text(stringResource(R.string.notification_management_load_threshold)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = draft.ratio,
                    onValueChange = onRatioChanged,
                    label = { Text(stringResource(R.string.notification_management_load_ratio)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = draft.interval,
                    onValueChange = onIntervalChanged,
                    label = { Text(stringResource(R.string.notification_management_load_interval)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text(
                    text = stringResource(R.string.notification_management_load_clients),
                    style = MaterialTheme.typography.labelLarge
                )
                if (clients.isEmpty()) {
                    Text(
                        text = stringResource(R.string.notification_management_load_no_clients),
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

@Composable
private fun NotificationLoadingPane(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NotificationErrorPane(
    error: String,
    onEvent: (NotificationManagementContract.Event) -> Unit,
    modifier: Modifier
) {
    AdaptiveContentPane(modifier = modifier, maxWidth = 920.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = soundClick { onEvent(NotificationManagementContract.Event.Retry) }) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
private fun NotificationBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun String.toLoadMetricLabel(): String {
    return LoadNotificationMetric.fromApiValue(this)?.toLabel() ?: this
}

private fun LoadNotificationMetric.toLabel(): String = apiValue
