package com.sekusarisu.yanami.ui.screen.client

import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.mvi.UiEffect
import com.sekusarisu.yanami.mvi.UiEvent
import com.sekusarisu.yanami.mvi.UiState

object ClientManagementContract {

    data class State(
            val isLoading: Boolean = true,
            val isRefreshing: Boolean = false,
            val isReordering: Boolean = false,
            val clients: List<ManagedClient> = emptyList(),
            val filteredClients: List<ManagedClient> = emptyList(),
            val searchQuery: String = "",
            val groups: List<String> = emptyList(),
            val selectedGroup: String? = null,
            val maskIpAddress: Boolean = true,
            val isSortMode: Boolean = false,
            val installCommandClient: ManagedClient? = null,
            val pendingDeleteClient: ManagedClient? = null,
            val error: String? = null,
            val serverName: String = "",
            val serverBaseUrl: String = ""
    ) : UiState

    sealed interface Event : UiEvent {
        data object Refresh : Event
        data object Retry : Event
        data class SearchChanged(val query: String) : Event
        data class GroupSelected(val group: String?) : Event
        data class ToggleMaskIpAddress(val enabled: Boolean) : Event
        data class ToggleSortMode(val enabled: Boolean) : Event
        data object AddClicked : Event
        data class EditClicked(val uuid: String) : Event
        data class DeleteClicked(val uuid: String) : Event
        data object ConfirmDelete : Event
        data object DismissDelete : Event
        data class ShowInstallCommandClicked(val uuid: String) : Event
        data object DismissInstallCommand : Event
        data class MoveUpClicked(val uuid: String) : Event
        data class MoveDownClicked(val uuid: String) : Event
        data class CommitReorder(val orderedUuids: List<String>) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data object NavigateToCreate : Effect
        data class NavigateToEdit(val uuid: String) : Effect
        data class NavigateToServerRelogin(val serverId: Long, val forceTwoFa: Boolean) : Effect
        data class NavigateToServerEdit(val serverId: Long) : Effect
    }
}
