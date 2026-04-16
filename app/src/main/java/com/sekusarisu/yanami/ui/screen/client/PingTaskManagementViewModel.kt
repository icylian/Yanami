package com.sekusarisu.yanami.ui.screen.client

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.AdminPingTask
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.domain.model.ServerInstance
import com.sekusarisu.yanami.domain.model.applyTo
import com.sekusarisu.yanami.domain.model.toDraft
import com.sekusarisu.yanami.domain.repository.ClientRepository
import com.sekusarisu.yanami.domain.repository.PingTaskRepository
import com.sekusarisu.yanami.domain.repository.Requires2FAException
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.domain.repository.SessionExpiredException
import com.sekusarisu.yanami.mvi.MviViewModel
import com.sekusarisu.yanami.ui.screen.isSessionAuthError
import com.sekusarisu.yanami.ui.screen.isTwoFaHint
import kotlinx.coroutines.launch

class PingTaskManagementViewModel(
    private val pingTaskRepository: PingTaskRepository,
    private val clientRepository: ClientRepository,
    private val serverRepository: ServerRepository,
    private val context: Context
) :
    MviViewModel<
        PingTaskManagementContract.State,
        PingTaskManagementContract.Event,
        PingTaskManagementContract.Effect
    >(PingTaskManagementContract.State()) {

    private var hasLoadedOnce = false

    override fun onEvent(event: PingTaskManagementContract.Event) {
        when (event) {
            is PingTaskManagementContract.Event.EnsureLoaded -> ensureLoaded()
            is PingTaskManagementContract.Event.Refresh -> refreshTasks()
            is PingTaskManagementContract.Event.Retry -> loadTasks()
            is PingTaskManagementContract.Event.SearchChanged -> {
                setState { copy(searchQuery = event.query) }
                applyFilters()
            }
            is PingTaskManagementContract.Event.ViewChanged -> {
                setState {
                    copy(
                        currentView = event.view,
                        isSortMode =
                            if (event.view == PingTaskManagementContract.ContentView.TASKS) {
                                isSortMode
                            } else {
                                false
                            }
                    )
                }
                applyFilters()
            }
            is PingTaskManagementContract.Event.TypeFilterChanged -> {
                setState { copy(selectedType = event.type) }
                applyFilters()
            }
            is PingTaskManagementContract.Event.ToggleSortMode -> toggleSortMode(event.enabled)
            is PingTaskManagementContract.Event.AddClicked -> {
                setState {
                    copy(
                        editor =
                            PingTaskManagementContract.EditorState(
                                mode = PingTaskManagementContract.EditorMode.CREATE
                            )
                    )
                }
            }
            is PingTaskManagementContract.Event.EditClicked -> openEditor(event.id)
            is PingTaskManagementContract.Event.DismissEditor -> {
                setState { copy(editor = null) }
            }
            is PingTaskManagementContract.Event.EditorNameChanged -> updateEditor {
                copy(draft = draft.copy(name = event.value))
            }
            is PingTaskManagementContract.Event.EditorTargetChanged -> updateEditor {
                copy(draft = draft.copy(target = event.value))
            }
            is PingTaskManagementContract.Event.EditorTypeChanged -> updateEditor {
                copy(draft = draft.copy(type = event.value))
            }
            is PingTaskManagementContract.Event.EditorIntervalChanged -> updateEditor {
                copy(draft = draft.copy(interval = event.value))
            }
            is PingTaskManagementContract.Event.ToggleEditorClient -> updateEditor {
                val nextSelection =
                    draft.selectedClientUuids.toMutableSet().apply {
                        if (!add(event.uuid)) {
                            remove(event.uuid)
                        }
                    }
                copy(draft = draft.copy(selectedClientUuids = nextSelection))
            }
            is PingTaskManagementContract.Event.SaveEditor -> saveEditor()
            is PingTaskManagementContract.Event.DeleteClicked -> {
                val target = currentState.tasks.firstOrNull { it.id == event.id } ?: return
                setState { copy(pendingDeleteTask = target) }
            }
            is PingTaskManagementContract.Event.DismissDelete -> {
                setState { copy(pendingDeleteTask = null) }
            }
            is PingTaskManagementContract.Event.ConfirmDelete -> deletePendingTask()
            is PingTaskManagementContract.Event.EditServerBindingClicked -> {
                openServerBindingEditor(event.uuid)
            }
            is PingTaskManagementContract.Event.ToggleServerTask -> {
                updateServerBindingEditor {
                    val nextSelection =
                        selectedTaskIds.toMutableSet().apply {
                            if (!add(event.id)) {
                                remove(event.id)
                            }
                        }
                    copy(selectedTaskIds = nextSelection)
                }
            }
            is PingTaskManagementContract.Event.DismissServerBinding -> {
                setState { copy(serverBindingEditor = null) }
            }
            is PingTaskManagementContract.Event.SaveServerBinding -> saveServerBinding()
            is PingTaskManagementContract.Event.CommitReorder -> commitReorder(event.orderedIds)
            is PingTaskManagementContract.Event.MoveUpClicked -> moveTask(event.id, -1)
            is PingTaskManagementContract.Event.MoveDownClicked -> moveTask(event.id, 1)
        }
    }

    fun refreshIfLoaded() {
        if (!hasLoadedOnce) return
        if (currentState.isLoading || currentState.isRefreshing || currentState.isSaving || currentState.isReordering) {
            return
        }
        refreshTasks()
    }

    private fun ensureLoaded() {
        if (hasLoadedOnce || currentState.isLoading) return
        loadTasks()
    }

    private fun loadTasks() {
        setState { copy(isLoading = true, error = null) }
        screenModelScope.launch {
            withServerSession(
                onAuthError = ::handleSessionExpired,
                onError = { e ->
                    setState {
                        copy(
                            isLoading = false,
                            error =
                                context.getString(
                                    R.string.ping_task_management_load_failed,
                                    e.message
                                )
                        )
                    }
                }
            ) { server, sessionToken ->
                val clients =
                    clientRepository.listClients(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType
                    )
                val tasks =
                    pingTaskRepository.listPingTasks(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType
                    )
                hasLoadedOnce = true
                updateLoadedState(server, clients, tasks)
                setState { copy(isLoading = false, error = null) }
            }
        }
    }

    private fun refreshTasks() {
        setState { copy(isRefreshing = true) }
        screenModelScope.launch {
            withServerSession(
                onAuthError = ::handleSessionExpired,
                onError = { e ->
                    setState { copy(isRefreshing = false) }
                    sendEffect(
                        PingTaskManagementContract.Effect.ShowToast(
                            context.getString(
                                R.string.ping_task_management_refresh_failed,
                                e.message
                            )
                        )
                    )
                }
            ) { server, sessionToken ->
                val clients =
                    clientRepository.listClients(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType
                    )
                val tasks =
                    pingTaskRepository.listPingTasks(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType
                    )
                hasLoadedOnce = true
                updateLoadedState(server, clients, tasks)
                setState { copy(isRefreshing = false, error = null) }
            }
        }
    }

    private fun openEditor(id: Int) {
        val task = currentState.tasks.firstOrNull { it.id == id } ?: return
        setState {
            copy(
                editor =
                    PingTaskManagementContract.EditorState(
                        mode = PingTaskManagementContract.EditorMode.EDIT,
                        taskId = task.id,
                        draft = task.toDraft()
                    )
            )
        }
    }

    private fun openServerBindingEditor(uuid: String) {
        val client = currentState.clients.firstOrNull { it.uuid == uuid } ?: return
        val selectedTaskIds =
            currentState.tasks.filter { task -> uuid in task.clients }.map { it.id }.toSet()
        setState {
            copy(
                serverBindingEditor =
                    PingTaskManagementContract.ServerBindingEditorState(
                        client = client,
                        selectedTaskIds = selectedTaskIds
                    )
            )
        }
    }

    private fun toggleSortMode(enabled: Boolean) {
        if (enabled && (currentState.searchQuery.isNotBlank() || currentState.selectedType != null)) {
            sendEffect(
                PingTaskManagementContract.Effect.ShowToast(
                    context.getString(R.string.ping_task_management_sort_mode_requires_all)
                )
            )
            return
        }
        setState { copy(isSortMode = enabled) }
    }

    private fun updateEditor(
        reducer: PingTaskManagementContract.EditorState.() -> PingTaskManagementContract.EditorState
    ) {
        val editor = currentState.editor ?: return
        setState { copy(editor = editor.reducer()) }
    }

    private fun updateServerBindingEditor(
        reducer:
            PingTaskManagementContract.ServerBindingEditorState.() ->
                PingTaskManagementContract.ServerBindingEditorState
    ) {
        val editor = currentState.serverBindingEditor ?: return
        setState { copy(serverBindingEditor = editor.reducer()) }
    }

    private fun saveEditor() {
        val editor = currentState.editor ?: return
        setState { copy(isSaving = true) }
        screenModelScope.launch {
            withServerSession(
                onAuthError = ::handleSessionExpired,
                onError = { e ->
                    setState { copy(isSaving = false) }
                    sendEffect(
                        PingTaskManagementContract.Effect.ShowToast(
                            context.getString(
                                R.string.ping_task_management_save_failed,
                                e.message
                            )
                        )
                    )
                }
            ) { server, sessionToken ->
                when (editor.mode) {
                    PingTaskManagementContract.EditorMode.CREATE -> {
                        pingTaskRepository.addPingTask(
                            baseUrl = server.baseUrl,
                            sessionToken = sessionToken,
                            authType = server.authType,
                            draft = editor.draft
                        )
                        sendEffect(
                            PingTaskManagementContract.Effect.ShowToast(
                                context.getString(R.string.ping_task_management_created)
                            )
                        )
                    }
                    PingTaskManagementContract.EditorMode.EDIT -> {
                        val taskId = editor.taskId ?: throw IllegalArgumentException("缺少 task id")
                        val originalTask =
                            currentState.tasks.firstOrNull { it.id == taskId }
                                ?: throw IllegalArgumentException("未找到目标任务")
                        pingTaskRepository.updatePingTasks(
                            baseUrl = server.baseUrl,
                            sessionToken = sessionToken,
                            authType = server.authType,
                            tasks = listOf(editor.draft.applyTo(originalTask))
                        )
                        sendEffect(
                            PingTaskManagementContract.Effect.ShowToast(
                                context.getString(R.string.ping_task_management_saved)
                            )
                        )
                    }
                }
                setState { copy(isSaving = false, editor = null) }
                refreshTasks()
            }
        }
    }

    private fun deletePendingTask() {
        val target = currentState.pendingDeleteTask ?: return
        setState { copy(pendingDeleteTask = null, isSaving = true) }
        screenModelScope.launch {
            withServerSession(
                onAuthError = ::handleSessionExpired,
                onError = { e ->
                    setState { copy(isSaving = false) }
                    sendEffect(
                        PingTaskManagementContract.Effect.ShowToast(
                            context.getString(
                                R.string.ping_task_management_delete_failed,
                                e.message
                            )
                        )
                    )
                }
            ) { server, sessionToken ->
                pingTaskRepository.deletePingTasks(
                    baseUrl = server.baseUrl,
                    sessionToken = sessionToken,
                    authType = server.authType,
                    ids = listOf(target.id)
                )
                setState { copy(isSaving = false) }
                sendEffect(
                    PingTaskManagementContract.Effect.ShowToast(
                        context.getString(R.string.ping_task_management_deleted, target.name)
                    )
                )
                refreshTasks()
            }
        }
    }

    private fun saveServerBinding() {
        val editor = currentState.serverBindingEditor ?: return
        val clientUuid = editor.client.uuid
        val originalSelectedIds =
            currentState.tasks.filter { task -> clientUuid in task.clients }.map { it.id }.toSet()
        val nextSelectedIds = editor.selectedTaskIds
        if (originalSelectedIds == nextSelectedIds) {
            setState { copy(serverBindingEditor = null) }
            return
        }

        val updatedTasks =
            currentState.tasks.mapNotNull { task ->
                val shouldContain = task.id in nextSelectedIds
                val currentlyContains = clientUuid in task.clients
                if (shouldContain == currentlyContains) {
                    null
                } else {
                    task.copy(
                        clients =
                            if (shouldContain) {
                                (task.clients + clientUuid).distinct().sorted()
                            } else {
                                task.clients.filterNot { it == clientUuid }
                            }
                    )
                }
            }

        setState { copy(isSaving = true) }
        screenModelScope.launch {
            withServerSession(
                onAuthError = ::handleSessionExpired,
                onError = { e ->
                    setState { copy(isSaving = false) }
                    sendEffect(
                        PingTaskManagementContract.Effect.ShowToast(
                            context.getString(
                                R.string.ping_task_management_server_binding_save_failed,
                                e.message
                            )
                        )
                    )
                }
            ) { server, sessionToken ->
                if (updatedTasks.isNotEmpty()) {
                    pingTaskRepository.updatePingTasks(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType,
                        tasks = updatedTasks
                    )
                }
                setState { copy(isSaving = false, serverBindingEditor = null) }
                sendEffect(
                    PingTaskManagementContract.Effect.ShowToast(
                        context.getString(
                            R.string.ping_task_management_server_binding_saved,
                            editor.client.name.ifBlank { editor.client.uuid }
                        )
                    )
                )
                refreshTasks()
            }
        }
    }

    private fun commitReorder(orderedIds: List<Int>) {
        val currentTasks = currentState.tasks
        val currentOrder = currentTasks.map { it.id }
        if (orderedIds == currentOrder) return
        if (orderedIds.size != currentTasks.size) return
        if (orderedIds.toSet() != currentOrder.toSet()) return

        val reorderedWeights =
            orderedIds.mapIndexed { index, id ->
                id to (index + 1) * 10
            }

        setState { copy(isReordering = true) }
        screenModelScope.launch {
            withServerSession(
                onAuthError = ::handleSessionExpired,
                onError = { e ->
                    setState { copy(isReordering = false) }
                    sendEffect(
                        PingTaskManagementContract.Effect.ShowToast(
                            context.getString(
                                R.string.ping_task_management_reorder_failed,
                                e.message
                            )
                        )
                    )
                }
            ) { server, sessionToken ->
                pingTaskRepository.reorderPingTasks(
                    baseUrl = server.baseUrl,
                    sessionToken = sessionToken,
                    authType = server.authType,
                    weights = reorderedWeights.toMap()
                )
                setState { copy(isReordering = false) }
                refreshTasks()
            }
        }
    }

    private fun moveTask(id: Int, direction: Int) {
        val tasks = currentState.tasks
        val index = tasks.indexOfFirst { it.id == id }
        if (index == -1) return
        val targetIndex = index + direction
        if (targetIndex !in tasks.indices) return
        val current = tasks[index]
        val target = tasks[targetIndex]

        setState { copy(isReordering = true) }
        screenModelScope.launch {
            withServerSession(
                onAuthError = ::handleSessionExpired,
                onError = { e ->
                    setState { copy(isReordering = false) }
                    sendEffect(
                        PingTaskManagementContract.Effect.ShowToast(
                            context.getString(
                                R.string.ping_task_management_reorder_failed,
                                e.message
                            )
                        )
                    )
                }
            ) { server, sessionToken ->
                pingTaskRepository.reorderPingTasks(
                    baseUrl = server.baseUrl,
                    sessionToken = sessionToken,
                    authType = server.authType,
                    weights = mapOf(current.id to target.weight, target.id to current.weight)
                )
                setState { copy(isReordering = false) }
                refreshTasks()
            }
        }
    }

    private fun updateLoadedState(
        server: ServerInstance,
        clients: List<ManagedClient>,
        tasks: List<AdminPingTask>
    ) {
        setState {
            copy(
                tasks = tasks,
                clients = clients,
                filteredClients = clients,
                serverName = server.name
            )
        }
        applyFilters()
    }

    private fun applyFilters() {
        val state = currentState
        val query = state.searchQuery.trim().lowercase()
        val filteredTasks =
            state.tasks.filter { task ->
                val matchesQuery =
                    query.isBlank() ||
                        task.name.lowercase().contains(query) ||
                        task.target.lowercase().contains(query) ||
                        task.type.apiValue.contains(query) ||
                        task.clients.any { it.lowercase().contains(query) }
                val matchesType = state.selectedType == null || task.type == state.selectedType
                matchesQuery && matchesType
            }
        val filteredClients =
            state.clients.filter { client ->
                query.isBlank() ||
                    client.name.lowercase().contains(query) ||
                    client.group.lowercase().contains(query) ||
                    client.region.lowercase().contains(query) ||
                    client.os.lowercase().contains(query) ||
                    client.tags.lowercase().contains(query) ||
                    client.remark.lowercase().contains(query)
            }
        setState {
            copy(
                filteredTasks = filteredTasks,
                filteredClients = filteredClients
            )
        }
    }

    private suspend fun withServerSession(
        onAuthError: (Long, Boolean, Throwable, AuthType) -> Boolean,
        onError: (Throwable) -> Unit,
        block: suspend (ServerInstance, String) -> Unit
    ) {
        var activeServerId: Long? = null
        var activeRequires2fa = false
        var activeAuthType = AuthType.PASSWORD
        try {
            val server =
                serverRepository.getActive()
                    ?: throw Exception(context.getString(R.string.error_no_server_selected))
            activeServerId = server.id
            activeRequires2fa = server.requires2fa
            activeAuthType = server.authType
            ensureAdminAccess(server)
            val sessionToken = ensureSession(server)
            block(server, sessionToken)
        } catch (e: Exception) {
            if (
                activeServerId != null &&
                    onAuthError(activeServerId, activeRequires2fa, e, activeAuthType)
            ) {
                return
            }
            onError(e)
        }
    }

    private suspend fun ensureSession(server: ServerInstance): String {
        return try {
            serverRepository.ensureSessionToken(server)
        } catch (e: Requires2FAException) {
            serverRepository.updateRequires2fa(server.id, true)
            throw SessionExpiredException(
                e.message ?: context.getString(R.string.error_no_session)
            )
        } catch (e: Exception) {
            if (e.isSessionAuthError()) {
                throw SessionExpiredException(
                    e.message ?: context.getString(R.string.error_no_session)
                )
            }
            throw e
        }
    }

    private fun ensureAdminAccess(server: ServerInstance) {
        if (server.authType == AuthType.GUEST) {
            throw IllegalStateException(context.getString(R.string.ping_task_management_guest_unsupported))
        }
    }

    private fun handleSessionExpired(
        serverId: Long,
        requires2fa: Boolean,
        error: Throwable,
        authType: AuthType
    ): Boolean {
        if (!error.isSessionAuthError()) return false
        if (authType == AuthType.GUEST) return false
        setState {
            copy(
                isLoading = false,
                isRefreshing = false,
                isSaving = false,
                isReordering = false,
                error = null
            )
        }
        sendEffect(
            PingTaskManagementContract.Effect.ShowToast(
                error.message ?: context.getString(R.string.error_no_session)
            )
        )
        if (authType == AuthType.API_KEY) {
            sendEffect(PingTaskManagementContract.Effect.NavigateToServerEdit(serverId))
        } else {
            sendEffect(
                PingTaskManagementContract.Effect.NavigateToServerRelogin(
                    serverId = serverId,
                    forceTwoFa = requires2fa || error.isTwoFaHint()
                )
            )
        }
        return true
    }
}
