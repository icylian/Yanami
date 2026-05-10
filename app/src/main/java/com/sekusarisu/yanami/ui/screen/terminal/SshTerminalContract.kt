package com.sekusarisu.yanami.ui.screen.terminal

import com.sekusarisu.yanami.domain.model.TerminalSnippet
import com.sekusarisu.yanami.mvi.UiEffect
import com.sekusarisu.yanami.mvi.UiEvent
import com.sekusarisu.yanami.mvi.UiState

/** SSH 终端 — MVI 契约 */
object SshTerminalContract {

    data class State(
            val isConnecting: Boolean = true,
            val isConnected: Boolean = false,
            val error: String? = null,
            val fontSize: Int = 20,
            val ctrlActive: Boolean = false,
            val altActive: Boolean = false,
            val fnMode: Boolean = false,
            val isSnippetsPanelOpen: Boolean = false,
            val snippets: List<TerminalSnippet> = emptyList()
    ) : UiState

    sealed interface Event : UiEvent {
        data object Disconnect : Event
        data class Resize(val cols: Int, val rows: Int) : Event
        data class FontSizeChanged(val delta: Int) : Event
        data object ToggleCtrl : Event
        data object ToggleAlt : Event
        data object ToggleFn : Event
        data object ToggleSnippetsPanel : Event
        data class SetSnippetsPanelOpen(val open: Boolean) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data object NavigateBack : Effect
    }
}
