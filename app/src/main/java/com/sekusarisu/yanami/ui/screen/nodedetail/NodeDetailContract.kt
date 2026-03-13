package com.sekusarisu.yanami.ui.screen.nodedetail

import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.LoadRecord
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.domain.model.PingRecord
import com.sekusarisu.yanami.domain.model.PingTask
import com.sekusarisu.yanami.mvi.UiEffect
import com.sekusarisu.yanami.mvi.UiEvent
import com.sekusarisu.yanami.mvi.UiState

/** 节点详情 — MVI 契约 */
object NodeDetailContract {

    data class State(
            val isLoading: Boolean = true,
            val node: Node? = null,
            val loadRecords: List<LoadRecord> = emptyList(),
            val realtimeLoadRecords: List<LoadRecord> = emptyList(),
            val pingTasks: List<PingTask> = emptyList(),
            val pingRecords: List<PingRecord> = emptyList(),
            val selectedLoadHours: Int = 0,
            val selectedPingHours: Int = 1,
            val isLoadRecordsLoading: Boolean = false,
            val isPingRecordsLoading: Boolean = false,
            val error: String? = null,
            val authType: AuthType = AuthType.PASSWORD
    ) : UiState

    sealed interface Event : UiEvent {
        data object Refresh : Event
        data object Retry : Event
        data class LoadHoursChanged(val hours: Int) : Event
        data class PingHoursChanged(val hours: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data class NavigateToServerRelogin(val serverId: Long, val forceTwoFa: Boolean) : Effect
        data class NavigateToServerEdit(val serverId: Long) : Effect
    }
}
