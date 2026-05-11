package com.sekusarisu.yanami.ui.screen.server

import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.CustomHeader
import com.sekusarisu.yanami.domain.model.ServerInstance
import com.sekusarisu.yanami.mvi.UiEffect
import com.sekusarisu.yanami.mvi.UiEvent
import com.sekusarisu.yanami.mvi.UiState

/** 服务端实例管理 — MVI 契约 */
object ServerContract {

    // ─── 实例列表 State ───
    data class ListState(
            val servers: List<ServerInstance> = emptyList(),
            val isLoading: Boolean = true
    ) : UiState

    // ─── 添加实例 State ───
    data class AddState(
            val name: String = "",
            val baseUrl: String = "https://",
            val username: String = "",
            val password: String = "",
            val twoFaCode: String = "",
            val show2faField: Boolean = false,
            val isTesting: Boolean = false,
            val isSaving: Boolean = false,
            val testResult: String? = null,
            val testError: String? = null,
            val authType: AuthType = AuthType.PASSWORD,
            val apiKey: String = "",
            val customHeaders: List<CustomHeader> = emptyList()
    ) : UiState

    // ─── 事件 ───
    sealed interface Event : UiEvent {
        // 列表事件
        data class SelectServer(val id: Long) : Event
        data class DeleteServer(val id: Long) : Event

        // 添加实例事件
        data class UpdateName(val name: String) : Event
        data class UpdateBaseUrl(val url: String) : Event
        data class UpdateUsername(val username: String) : Event
        data class UpdatePassword(val password: String) : Event
        data class UpdateTwoFaCode(val code: String) : Event
        data class UpdateAuthType(val authType: AuthType) : Event
        data class UpdateApiKey(val apiKey: String) : Event
        data object AddCustomHeader : Event
        data class RemoveCustomHeader(val index: Int) : Event
        data class UpdateCustomHeaderName(val index: Int, val name: String) : Event
        data class UpdateCustomHeaderValue(val index: Int, val value: String) : Event
        data object TestConnection : Event
        data object SaveServer : Event
    }

    // ─── 副作用 ───
    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data object NavigateToAddServer : Effect
        data object NavigateBack : Effect
        data object ServerSaved : Effect
        data object ServerUpdated : Effect
        data object NavigateToNodeList : Effect
    }
}
