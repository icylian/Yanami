package com.sekusarisu.yanami.ui.screen.server

import com.sekusarisu.yanami.mvi.UiEffect
import com.sekusarisu.yanami.mvi.UiEvent
import com.sekusarisu.yanami.mvi.UiState

/** 实例重登录 — MVI 契约 */
object ServerReLoginContract {

    data class State(
            val isLoading: Boolean = true,
            val isSubmitting: Boolean = false,
            val serverName: String = "",
            val baseUrl: String = "",
            val username: String = "",
            val password: String = "",
            val twoFaCode: String = "",
            val requires2fa: Boolean = false,
            val error: String? = null
    ) : UiState

    sealed interface Event : UiEvent {
        data class UsernameChanged(val username: String) : Event
        data class PasswordChanged(val password: String) : Event
        data class TwoFaCodeChanged(val code: String) : Event
        data object Submit : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data object NavigateToNodeList : Effect
    }
}
