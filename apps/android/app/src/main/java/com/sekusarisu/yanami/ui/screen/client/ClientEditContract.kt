package com.sekusarisu.yanami.ui.screen.client

import com.sekusarisu.yanami.domain.model.ManagedClientDraft
import com.sekusarisu.yanami.mvi.UiEffect
import com.sekusarisu.yanami.mvi.UiEvent
import com.sekusarisu.yanami.mvi.UiState

object ClientEditContract {

    enum class TextField {
        NAME,
        GROUP,
        TAGS,
        REMARK,
        PUBLIC_REMARK,
        WEIGHT,
        PRICE,
        BILLING_CYCLE,
        CURRENCY,
        EXPIRED_AT,
        TRAFFIC_LIMIT
    }

    data class State(
            val isLoading: Boolean = true,
            val isSaving: Boolean = false,
            val clientName: String = "",
            val draft: ManagedClientDraft = ManagedClientDraft(),
            val error: String? = null
    ) : UiState

    sealed interface Event : UiEvent {
        data class TextChanged(val field: TextField, val value: String) : Event
        data class AutoRenewalChanged(val value: Boolean) : Event
        data class HiddenChanged(val value: Boolean) : Event
        data class TrafficLimitTypeChanged(val value: String) : Event
        data object Save : Event
        data object Retry : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data object NavigateBack : Effect
        data class NavigateToServerRelogin(val serverId: Long, val forceTwoFa: Boolean) : Effect
        data class NavigateToServerEdit(val serverId: Long) : Effect
    }
}
