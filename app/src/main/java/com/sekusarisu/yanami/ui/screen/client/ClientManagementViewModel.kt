package com.sekusarisu.yanami.ui.screen.client

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.domain.model.ServerInstance
import com.sekusarisu.yanami.domain.repository.ClientRepository
import com.sekusarisu.yanami.domain.repository.Requires2FAException
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.domain.repository.SessionExpiredException
import com.sekusarisu.yanami.mvi.MviViewModel
import com.sekusarisu.yanami.ui.screen.isSessionAuthError
import com.sekusarisu.yanami.ui.screen.isTwoFaHint
import kotlinx.coroutines.launch

class ClientManagementViewModel(
        private val clientRepository: ClientRepository,
        private val serverRepository: ServerRepository,
        private val context: Context
) :
        MviViewModel<
                ClientManagementContract.State,
                ClientManagementContract.Event,
                ClientManagementContract.Effect>(ClientManagementContract.State()) {

    private var hasLoadedOnce = false

    init {
        loadClients()
    }

    override fun onEvent(event: ClientManagementContract.Event) {
        when (event) {
            is ClientManagementContract.Event.Refresh -> refreshClients()
            is ClientManagementContract.Event.Retry -> loadClients()
            is ClientManagementContract.Event.SearchChanged -> {
                setState { copy(searchQuery = event.query) }
                applyFilters()
            }
            is ClientManagementContract.Event.GroupSelected -> {
                setState { copy(selectedGroup = event.group) }
                applyFilters()
            }
            is ClientManagementContract.Event.ToggleMaskIpAddress -> {
                setState { copy(maskIpAddress = event.enabled) }
            }
            is ClientManagementContract.Event.ToggleSortMode -> toggleSortMode(event.enabled)
            is ClientManagementContract.Event.AddClicked -> {
                sendEffect(ClientManagementContract.Effect.NavigateToCreate)
            }
            is ClientManagementContract.Event.EditClicked -> {
                sendEffect(ClientManagementContract.Effect.NavigateToEdit(event.uuid))
            }
            is ClientManagementContract.Event.DeleteClicked -> {
                val target = currentState.clients.firstOrNull { it.uuid == event.uuid } ?: return
                setState { copy(pendingDeleteClient = target) }
            }
            is ClientManagementContract.Event.ConfirmDelete -> deletePendingClient()
            is ClientManagementContract.Event.DismissDelete -> {
                setState { copy(pendingDeleteClient = null) }
            }
            is ClientManagementContract.Event.ShowInstallCommandClicked -> {
                val target = currentState.clients.firstOrNull { it.uuid == event.uuid } ?: return
                setState { copy(installCommandClient = target) }
            }
            is ClientManagementContract.Event.DismissInstallCommand -> {
                setState { copy(installCommandClient = null) }
            }
            is ClientManagementContract.Event.MoveUpClicked -> moveClient(event.uuid, -1)
            is ClientManagementContract.Event.MoveDownClicked -> moveClient(event.uuid, 1)
            is ClientManagementContract.Event.CommitReorder -> {
                commitReorder(event.orderedUuids)
            }
        }
    }

    fun refreshIfLoaded() {
        if (!hasLoadedOnce) return
        if (currentState.isLoading || currentState.isRefreshing || currentState.isReordering) return
        refreshClients()
    }

    private fun loadClients() {
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
                                                    R.string.client_management_load_failed,
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
                hasLoadedOnce = true
                updateClientsState(clients)
                setState {
                    copy(
                            isLoading = false,
                            error = null,
                            serverName = server.name,
                            serverBaseUrl = server.baseUrl
                    )
                }
            }
        }
    }

    private fun refreshClients() {
        setState { copy(isRefreshing = true) }
        screenModelScope.launch {
            withServerSession(
                    onAuthError = ::handleSessionExpired,
                    onError = { e ->
                        setState { copy(isRefreshing = false) }
                        sendEffect(
                                ClientManagementContract.Effect.ShowToast(
                                        context.getString(
                                                R.string.client_management_refresh_failed,
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
                hasLoadedOnce = true
                updateClientsState(clients)
                setState {
                    copy(
                            isRefreshing = false,
                            error = null,
                            serverName = server.name,
                            serverBaseUrl = server.baseUrl
                    )
                }
            }
        }
    }

    private fun toggleSortMode(enabled: Boolean) {
        if (enabled &&
                        (currentState.searchQuery.isNotBlank() ||
                                currentState.selectedGroup != null)
        ) {
            sendEffect(
                    ClientManagementContract.Effect.ShowToast(
                            context.getString(R.string.client_management_sort_mode_requires_all)
                    )
            )
            return
        }
        setState { copy(isSortMode = enabled) }
    }

    private fun deletePendingClient() {
        val target = currentState.pendingDeleteClient ?: return
        setState { copy(pendingDeleteClient = null) }
        screenModelScope.launch {
            withServerSession(
                    onAuthError = ::handleSessionExpired,
                    onError = { e ->
                        sendEffect(
                                ClientManagementContract.Effect.ShowToast(
                                        context.getString(
                                                R.string.client_management_delete_failed,
                                                e.message
                                        )
                                )
                        )
                    }
            ) { server, sessionToken ->
                clientRepository.deleteClient(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType,
                        uuid = target.uuid
                )
                sendEffect(
                        ClientManagementContract.Effect.ShowToast(
                                context.getString(R.string.client_management_deleted, target.name)
                        )
                )
                refreshClients()
            }
        }
    }

    private fun moveClient(uuid: String, direction: Int) {
        val clients = currentState.clients
        val index = clients.indexOfFirst { it.uuid == uuid }
        if (index == -1) return
        val targetIndex = index + direction
        if (targetIndex !in clients.indices) return
        val current = clients[index]
        val target = clients[targetIndex]

        setState { copy(isReordering = true) }
        screenModelScope.launch {
            withServerSession(
                    onAuthError = ::handleSessionExpired,
                    onError = { e ->
                        setState { copy(isReordering = false) }
                        sendEffect(
                                ClientManagementContract.Effect.ShowToast(
                                        context.getString(
                                                R.string.client_management_reorder_failed,
                                                e.message
                                        )
                                )
                        )
                    }
            ) { server, sessionToken ->
                clientRepository.reorderClients(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType,
                        weights = mapOf(current.uuid to target.weight, target.uuid to current.weight)
                )
                setState { copy(isReordering = false) }
                refreshClients()
            }
        }
    }

    private fun commitReorder(orderedUuids: List<String>) {
        val currentClients = currentState.clients
        val currentOrder = currentClients.map { it.uuid }
        if (orderedUuids == currentOrder) return
        if (orderedUuids.size != currentClients.size) return
        if (orderedUuids.toSet() != currentOrder.toSet()) return

        val reorderedWeights =
                orderedUuids.mapIndexed { index, uuid ->
                    uuid to (index + 1) * 10
                }

        setState { copy(isReordering = true) }
        screenModelScope.launch {
            withServerSession(
                    onAuthError = ::handleSessionExpired,
                    onError = { e ->
                        setState { copy(isReordering = false) }
                        sendEffect(
                                ClientManagementContract.Effect.ShowToast(
                                        context.getString(
                                                R.string.client_management_reorder_failed,
                                                e.message
                                        )
                                )
                        )
                    }
            ) { server, sessionToken ->
                clientRepository.reorderClients(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType,
                        weights = reorderedWeights.toMap()
                )
                setState { copy(isReordering = false) }
                refreshClients()
            }
        }
    }

    private fun updateClientsState(clients: List<ManagedClient>) {
        val groups = clients.map { it.group }.filter { it.isNotBlank() }.distinct().sorted()
        setState { copy(clients = clients, groups = groups) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = currentState
        val query = state.searchQuery.trim().lowercase()
        val filtered =
                state.clients.filter { client ->
                    val matchesSearch =
                            query.isBlank() ||
                                    client.name.lowercase().contains(query) ||
                                    client.group.lowercase().contains(query) ||
                                    client.region.lowercase().contains(query) ||
                                    client.os.lowercase().contains(query) ||
                                    client.cpuName.lowercase().contains(query) ||
                                    client.tags.lowercase().contains(query) ||
                                    client.remark.lowercase().contains(query)
                    val matchesGroup =
                            state.selectedGroup == null || client.group == state.selectedGroup
                    matchesSearch && matchesGroup
                }
        setState { copy(filteredClients = filtered) }
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
            if (activeServerId != null &&
                            onAuthError(
                                    activeServerId,
                                    activeRequires2fa,
                                    e,
                                    activeAuthType
                            )
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
            throw SessionExpiredException(e.message ?: context.getString(R.string.error_no_session))
        } catch (e: Exception) {
            if (e.isSessionAuthError()) {
                throw SessionExpiredException(e.message ?: context.getString(R.string.error_no_session))
            }
            throw e
        }
    }

    private fun ensureAdminAccess(server: ServerInstance) {
        if (server.authType == AuthType.GUEST) {
            throw IllegalStateException(context.getString(R.string.client_management_guest_unsupported))
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
        setState { copy(isLoading = false, isRefreshing = false, isReordering = false, error = null) }
        sendEffect(
                ClientManagementContract.Effect.ShowToast(
                        error.message ?: context.getString(R.string.error_no_session)
                )
        )
        if (authType == AuthType.API_KEY) {
            sendEffect(ClientManagementContract.Effect.NavigateToServerEdit(serverId))
        } else {
            sendEffect(
                    ClientManagementContract.Effect.NavigateToServerRelogin(
                            serverId = serverId,
                            forceTwoFa = requires2fa || error.isTwoFaHint()
                    )
            )
        }
        return true
    }
}
