package com.sekusarisu.yanami.ui.screen.client

import com.sekusarisu.yanami.domain.model.ManagedClientCreateResult
import com.sekusarisu.yanami.mvi.UiEffect
import com.sekusarisu.yanami.mvi.UiEvent
import com.sekusarisu.yanami.mvi.UiState

object ClientCreateContract {

    data class State(
            val name: String = "",
            val isSaving: Boolean = false,
            val createdResult: ManagedClientCreateResult? = null,
            val error: String? = null
    ) : UiState

    sealed interface Event : UiEvent {
        data class NameChanged(val value: String) : Event
        data object Save : Event
        data object DismissCreatedResult : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data object NavigateBack : Effect
        data class NavigateToServerRelogin(val serverId: Long, val forceTwoFa: Boolean) : Effect
        data class NavigateToServerEdit(val serverId: Long) : Effect
    }
}
