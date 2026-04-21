package com.sekusarisu.yanami.ui.screen.client

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.LoadNotificationTask
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.domain.model.OfflineNotificationConfig
import com.sekusarisu.yanami.domain.model.ServerInstance
import com.sekusarisu.yanami.domain.model.applyTo
import com.sekusarisu.yanami.domain.model.toDraft
import com.sekusarisu.yanami.domain.repository.ClientRepository
import com.sekusarisu.yanami.domain.repository.NotificationRepository
import com.sekusarisu.yanami.domain.repository.Requires2FAException
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.domain.repository.SessionExpiredException
import com.sekusarisu.yanami.mvi.MviViewModel
import com.sekusarisu.yanami.ui.screen.isSessionAuthError
import com.sekusarisu.yanami.ui.screen.isTwoFaHint
import kotlinx.coroutines.launch

class NotificationManagementViewModel(
    private val notificationRepository: NotificationRepository,
    private val clientRepository: ClientRepository,
    private val serverRepository: ServerRepository,
    private val context: Context
) :
    MviViewModel<
        NotificationManagementContract.State,
        NotificationManagementContract.Event,
        NotificationManagementContract.Effect
    >(NotificationManagementContract.State()) {

    private var hasLoadedOnce = false

    override fun onEvent(event: NotificationManagementContract.Event) {
        when (event) {
            is NotificationManagementContract.Event.EnsureLoaded -> ensureLoaded()
            is NotificationManagementContract.Event.Refresh -> refreshNotifications()
            is NotificationManagementContract.Event.Retry -> loadNotifications()
            is NotificationManagementContract.Event.SearchChanged -> {
                setState { copy(searchQuery = event.query) }
                applyFilters()
            }
            is NotificationManagementContract.Event.ViewChanged -> {
                setState { copy(currentView = event.view) }
                applyFilters()
            }
            is NotificationManagementContract.Event.OfflineEnabledChanged -> {
                setOfflineEnabled(event.uuid, event.enabled)
            }
            is NotificationManagementContract.Event.EditOfflineClicked -> openOfflineEditor(event.uuid)
            is NotificationManagementContract.Event.OfflineEditorGracePeriodChanged -> {
                val editor = currentState.offlineEditor ?: return
                setState { copy(offlineEditor = editor.copy(gracePeriod = event.value)) }
            }
            is NotificationManagementContract.Event.DismissOfflineEditor -> {
                setState { copy(offlineEditor = null) }
            }
            is NotificationManagementContract.Event.SaveOfflineEditor -> saveOfflineEditor()
            is NotificationManagementContract.Event.LoadAddClicked -> {
                setState {
                    copy(
                        loadEditor =
                            NotificationManagementContract.LoadEditorState(
                                mode = NotificationManagementContract.EditorMode.CREATE
                            )
                    )
                }
            }
            is NotificationManagementContract.Event.LoadEditClicked -> openLoadEditor(event.id)
            is NotificationManagementContract.Event.DismissLoadEditor -> {
                setState { copy(loadEditor = null) }
            }
            is NotificationManagementContract.Event.LoadEditorNameChanged -> updateLoadEditor {
                copy(draft = draft.copy(name = event.value))
            }
            is NotificationManagementContract.Event.LoadEditorMetricChanged -> updateLoadEditor {
                copy(draft = draft.copy(metric = event.value))
            }
            is NotificationManagementContract.Event.LoadEditorThresholdChanged -> updateLoadEditor {
                copy(draft = draft.copy(threshold = event.value))
            }
            is NotificationManagementContract.Event.LoadEditorRatioChanged -> updateLoadEditor {
                copy(draft = draft.copy(ratio = event.value))
            }
            is NotificationManagementContract.Event.LoadEditorIntervalChanged -> updateLoadEditor {
                copy(draft = draft.copy(interval = event.value))
            }
            is NotificationManagementContract.Event.ToggleLoadEditorClient -> updateLoadEditor {
                val nextSelection =
                    draft.selectedClientUuids.toMutableSet().apply {
                        if (!add(event.uuid)) {
                            remove(event.uuid)
                        }
                    }
                copy(draft = draft.copy(selectedClientUuids = nextSelection))
            }
            is NotificationManagementContract.Event.SaveLoadEditor -> saveLoadEditor()
            is NotificationManagementContract.Event.LoadDeleteClicked -> {
                val target = currentState.loadTasks.firstOrNull { it.id == event.id } ?: return
                setState { copy(pendingDeleteLoadTask = target) }
            }
            is NotificationManagementContract.Event.DismissLoadDelete -> {
                setState { copy(pendingDeleteLoadTask = null) }
            }
            is NotificationManagementContract.Event.ConfirmLoadDelete -> deletePendingLoadTask()
        }
    }

    fun refreshIfLoaded() {
        if (!hasLoadedOnce) return
        if (currentState.isLoading || currentState.isRefreshing || currentState.isSaving) return
        refreshNotifications()
    }

    private fun ensureLoaded() {
        if (hasLoadedOnce || currentState.isLoading) return
        loadNotifications()
    }

    private fun loadNotifications() {
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
                                    R.string.notification_management_load_failed,
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
                val offlineConfigs =
                    notificationRepository.listOfflineNotifications(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType
                    )
                val loadTasks =
                    notificationRepository.listLoadNotifications(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType
                    )
                hasLoadedOnce = true
                updateLoadedState(server, clients, offlineConfigs, loadTasks)
                setState { copy(isLoading = false, error = null) }
            }
        }
    }

    private fun refreshNotifications() {
        setState { copy(isRefreshing = true) }
        screenModelScope.launch {
            withServerSession(
                onAuthError = ::handleSessionExpired,
                onError = { e ->
                    setState { copy(isRefreshing = false) }
                    sendEffect(
                        NotificationManagementContract.Effect.ShowToast(
                            context.getString(
                                R.string.notification_management_refresh_failed,
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
                val offlineConfigs =
                    notificationRepository.listOfflineNotifications(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType
                    )
                val loadTasks =
                    notificationRepository.listLoadNotifications(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType
                    )
                hasLoadedOnce = true
                updateLoadedState(server, clients, offlineConfigs, loadTasks)
                setState { copy(isRefreshing = false, error = null) }
            }
        }
    }

    private fun openOfflineEditor(uuid: String) {
        val client = currentState.clients.firstOrNull { it.uuid == uuid } ?: return
        val config = currentState.offlineConfigs.firstOrNull { it.clientUuid == uuid }
        setState {
            copy(
                offlineEditor =
                    NotificationManagementContract.OfflineEditorState(
                        client = client,
                        gracePeriod = (config?.gracePeriod ?: 0).toString()
                    )
            )
        }
    }

    private fun saveOfflineEditor() {
        val editor = currentState.offlineEditor ?: return
        val currentConfig =
            currentState.offlineConfigs.firstOrNull { it.clientUuid == editor.client.uuid }
                ?: OfflineNotificationConfig(
                    clientUuid = editor.client.uuid,
                    enabled = false,
                    gracePeriod = 0
                )

        setState { copy(isSaving = true) }
        screenModelScope.launch {
            withServerSession(
                onAuthError = ::handleSessionExpired,
                onError = { e ->
                    setState { copy(isSaving = false) }
                    sendEffect(
                        NotificationManagementContract.Effect.ShowToast(
                            context.getString(
                                R.string.notification_management_offline_save_failed,
                                e.message
                            )
                        )
                    )
                }
            ) { server, sessionToken ->
                val gracePeriod =
                    editor.gracePeriod.trim().toIntOrNull()
                        ?: throw IllegalArgumentException("grace_period 必须是整数")
                require(gracePeriod >= 0) { "grace_period 不能为负数" }
                notificationRepository.updateOfflineNotifications(
                    baseUrl = server.baseUrl,
                    sessionToken = sessionToken,
                    authType = server.authType,
                    configs =
                        listOf(
                            currentConfig.copy(gracePeriod = gracePeriod)
                        )
                )
                setState { copy(isSaving = false, offlineEditor = null) }
                sendEffect(
                    NotificationManagementContract.Effect.ShowToast(
                        context.getString(
                            R.string.notification_management_offline_saved,
                            editor.client.name.ifBlank { editor.client.uuid }
                        )
                    )
                )
                refreshNotifications()
            }
        }
    }

    private fun setOfflineEnabled(uuid: String, enabled: Boolean) {
        setState { copy(isSaving = true) }
        screenModelScope.launch {
            withServerSession(
                onAuthError = ::handleSessionExpired,
                onError = { e ->
                    setState { copy(isSaving = false) }
                    sendEffect(
                        NotificationManagementContract.Effect.ShowToast(
                            context.getString(
                                R.string.notification_management_offline_toggle_failed,
                                e.message
                            )
                        )
                    )
                }
            ) { server, sessionToken ->
                notificationRepository.setOfflineNotificationsEnabled(
                    baseUrl = server.baseUrl,
                    sessionToken = sessionToken,
                    authType = server.authType,
                    uuids = listOf(uuid),
                    enabled = enabled
                )
                setState { copy(isSaving = false) }
                refreshNotifications()
            }
        }
    }

    private fun openLoadEditor(id: Int) {
        val task = currentState.loadTasks.firstOrNull { it.id == id } ?: return
        setState {
            copy(
                loadEditor =
                    NotificationManagementContract.LoadEditorState(
                        mode = NotificationManagementContract.EditorMode.EDIT,
                        taskId = task.id,
                        draft = task.toDraft()
                    )
            )
        }
    }

    private fun updateLoadEditor(
        reducer:
            NotificationManagementContract.LoadEditorState.() ->
                NotificationManagementContract.LoadEditorState
    ) {
        val editor = currentState.loadEditor ?: return
        setState { copy(loadEditor = editor.reducer()) }
    }

    private fun saveLoadEditor() {
        val editor = currentState.loadEditor ?: return
        setState { copy(isSaving = true) }
        screenModelScope.launch {
            withServerSession(
                onAuthError = ::handleSessionExpired,
                onError = { e ->
                    setState { copy(isSaving = false) }
                    sendEffect(
                        NotificationManagementContract.Effect.ShowToast(
                            context.getString(
                                R.string.notification_management_load_save_failed,
                                e.message
                            )
                        )
                    )
                }
            ) { server, sessionToken ->
                when (editor.mode) {
                    NotificationManagementContract.EditorMode.CREATE -> {
                        notificationRepository.addLoadNotification(
                            baseUrl = server.baseUrl,
                            sessionToken = sessionToken,
                            authType = server.authType,
                            draft = editor.draft
                        )
                        sendEffect(
                            NotificationManagementContract.Effect.ShowToast(
                                context.getString(R.string.notification_management_load_created)
                            )
                        )
                    }
                    NotificationManagementContract.EditorMode.EDIT -> {
                        val taskId = editor.taskId ?: throw IllegalArgumentException("缺少 task id")
                        val originalTask =
                            currentState.loadTasks.firstOrNull { it.id == taskId }
                                ?: throw IllegalArgumentException("未找到目标任务")
                        notificationRepository.updateLoadNotifications(
                            baseUrl = server.baseUrl,
                            sessionToken = sessionToken,
                            authType = server.authType,
                            tasks = listOf(editor.draft.applyTo(originalTask))
                        )
                        sendEffect(
                            NotificationManagementContract.Effect.ShowToast(
                                context.getString(R.string.notification_management_load_saved)
                            )
                        )
                    }
                }
                setState { copy(isSaving = false, loadEditor = null) }
                refreshNotifications()
            }
        }
    }

    private fun deletePendingLoadTask() {
        val target = currentState.pendingDeleteLoadTask ?: return
        setState { copy(pendingDeleteLoadTask = null, isSaving = true) }
        screenModelScope.launch {
            withServerSession(
                onAuthError = ::handleSessionExpired,
                onError = { e ->
                    setState { copy(isSaving = false) }
                    sendEffect(
                        NotificationManagementContract.Effect.ShowToast(
                            context.getString(
                                R.string.notification_management_load_delete_failed,
                                e.message
                            )
                        )
                    )
                }
            ) { server, sessionToken ->
                notificationRepository.deleteLoadNotifications(
                    baseUrl = server.baseUrl,
                    sessionToken = sessionToken,
                    authType = server.authType,
                    ids = listOf(target.id)
                )
                setState { copy(isSaving = false) }
                sendEffect(
                    NotificationManagementContract.Effect.ShowToast(
                        context.getString(
                            R.string.notification_management_load_deleted,
                            target.name
                        )
                    )
                )
                refreshNotifications()
            }
        }
    }

    private fun updateLoadedState(
        server: ServerInstance,
        clients: List<ManagedClient>,
        offlineConfigs: List<OfflineNotificationConfig>,
        loadTasks: List<LoadNotificationTask>
    ) {
        val offlineItems = buildOfflineItems(clients, offlineConfigs)
        setState {
            copy(
                clients = clients,
                offlineConfigs = offlineConfigs,
                offlineItems = offlineItems,
                filteredOfflineItems = offlineItems,
                loadTasks = loadTasks,
                filteredLoadTasks = loadTasks,
                serverName = server.name
            )
        }
        applyFilters()
    }

    private fun applyFilters() {
        val state = currentState
        val query = state.searchQuery.trim().lowercase()
        val offlineItems =
            state.offlineItems.filter { item ->
                query.isBlank() ||
                    item.client.name.lowercase().contains(query) ||
                    item.client.group.lowercase().contains(query) ||
                    item.client.region.lowercase().contains(query) ||
                    item.client.os.lowercase().contains(query) ||
                    item.client.tags.lowercase().contains(query) ||
                    item.client.remark.lowercase().contains(query)
            }
        val loadTasks =
            state.loadTasks.filter { task ->
                query.isBlank() ||
                    task.name.lowercase().contains(query) ||
                    task.metric.lowercase().contains(query) ||
                    task.clients.any { it.lowercase().contains(query) }
            }
        setState {
            copy(
                filteredOfflineItems = offlineItems,
                filteredLoadTasks = loadTasks
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
            throw IllegalStateException(
                context.getString(R.string.notification_management_guest_unsupported)
            )
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
                error = null
            )
        }
        sendEffect(
            NotificationManagementContract.Effect.ShowToast(
                error.message ?: context.getString(R.string.error_no_session)
            )
        )
        if (authType == AuthType.API_KEY) {
            sendEffect(NotificationManagementContract.Effect.NavigateToServerEdit(serverId))
        } else {
            sendEffect(
                NotificationManagementContract.Effect.NavigateToServerRelogin(
                    serverId = serverId,
                    forceTwoFa = requires2fa || error.isTwoFaHint()
                )
            )
        }
        return true
    }
}

private fun buildOfflineItems(
    clients: List<ManagedClient>,
    offlineConfigs: List<OfflineNotificationConfig>
): List<NotificationManagementContract.OfflineItem> {
    val configMap = offlineConfigs.associateBy { it.clientUuid }
    return clients.map { client ->
        NotificationManagementContract.OfflineItem(
            client = client,
            config =
                configMap[client.uuid]
                    ?: OfflineNotificationConfig(
                        clientUuid = client.uuid,
                        enabled = false,
                        gracePeriod = 0,
                        lastNotified = null
                    )
        )
    }
}
