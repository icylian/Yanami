package com.sekusarisu.yanami.ui.screen.nodelist

import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.mvi.UiEffect
import com.sekusarisu.yanami.mvi.UiEvent
import com.sekusarisu.yanami.mvi.UiState

/** 节点列表 — MVI 契约 */
object NodeListContract {

    /** 状态筛选 */
    enum class StatusFilter { ALL, ONLINE, OFFLINE }

    data class State(
            val isLoading: Boolean = true,
            val isRefreshing: Boolean = false,
            val nodes: List<Node> = emptyList(),
            val searchQuery: String = "",
            val groups: List<String> = emptyList(),
            val selectedGroup: String? = null, // null = 全部
            val statusFilter: StatusFilter = StatusFilter.ALL,
            val error: String? = null,
            val onlineCount: Int = 0,
            val offlineCount: Int = 0,
            val totalCount: Int = 0,
            val totalNetIn: Long = 0,
            val totalNetOut: Long = 0,
            val totalTrafficUp: Long = 0,
            val totalTrafficDown: Long = 0,
            val serverName: String = ""
    ) : UiState

    sealed interface Event : UiEvent {
        data class SearchQueryChanged(val query: String) : Event
        data class GroupSelected(val group: String?) : Event
        data class StatusFilterSelected(val filter: StatusFilter) : Event
        data object Refresh : Event
        data object Retry : Event
        data class NodeClicked(val uuid: String) : Event
        data object ManageClientsClicked : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data class NavigateToNodeDetail(val uuid: String) : Effect
        data object NavigateToClientManagement : Effect
        data class NavigateToServerRelogin(val serverId: Long, val forceTwoFa: Boolean) : Effect
        data class NavigateToServerEdit(val serverId: Long) : Effect
    }
}
