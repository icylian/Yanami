package com.sekusarisu.yanami.ui.screen.client

import com.sekusarisu.yanami.domain.model.AdminPingTask
import com.sekusarisu.yanami.domain.model.AdminPingTaskDraft
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.domain.model.PingTaskType
import com.sekusarisu.yanami.mvi.UiEffect
import com.sekusarisu.yanami.mvi.UiEvent
import com.sekusarisu.yanami.mvi.UiState

object PingTaskManagementContract {

    data class State(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val isSaving: Boolean = false,
        val isReordering: Boolean = false,
        val tasks: List<AdminPingTask> = emptyList(),
        val filteredTasks: List<AdminPingTask> = emptyList(),
        val clients: List<ManagedClient> = emptyList(),
        val filteredClients: List<ManagedClient> = emptyList(),
        val searchQuery: String = "",
        val selectedType: PingTaskType? = null,
        val isSortMode: Boolean = false,
        val currentView: ContentView = ContentView.TASKS,
        val editor: EditorState? = null,
        val serverBindingEditor: ServerBindingEditorState? = null,
        val pendingDeleteTask: AdminPingTask? = null,
        val error: String? = null,
        val serverName: String = ""
    ) : UiState

    enum class ContentView {
        TASKS,
        SERVERS
    }

    data class EditorState(
        val mode: EditorMode,
        val taskId: Int? = null,
        val draft: AdminPingTaskDraft = AdminPingTaskDraft()
    )

    data class ServerBindingEditorState(
        val client: ManagedClient,
        val selectedTaskIds: Set<Int> = emptySet()
    )

    enum class EditorMode {
        CREATE,
        EDIT
    }

    sealed interface Event : UiEvent {
        data object EnsureLoaded : Event
        data object Refresh : Event
        data object Retry : Event
        data class SearchChanged(val query: String) : Event
        data class ViewChanged(val view: ContentView) : Event
        data class TypeFilterChanged(val type: PingTaskType?) : Event
        data class ToggleSortMode(val enabled: Boolean) : Event
        data object AddClicked : Event
        data class EditClicked(val id: Int) : Event
        data object DismissEditor : Event
        data class EditorNameChanged(val value: String) : Event
        data class EditorTargetChanged(val value: String) : Event
        data class EditorTypeChanged(val value: PingTaskType) : Event
        data class EditorIntervalChanged(val value: String) : Event
        data class ToggleEditorClient(val uuid: String) : Event
        data object SaveEditor : Event
        data class DeleteClicked(val id: Int) : Event
        data object DismissDelete : Event
        data object ConfirmDelete : Event
        data class EditServerBindingClicked(val uuid: String) : Event
        data class ToggleServerTask(val id: Int) : Event
        data object DismissServerBinding : Event
        data object SaveServerBinding : Event
        data class CommitReorder(val orderedIds: List<Int>) : Event
        data class MoveUpClicked(val id: Int) : Event
        data class MoveDownClicked(val id: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data class NavigateToServerRelogin(val serverId: Long, val forceTwoFa: Boolean) : Effect
        data class NavigateToServerEdit(val serverId: Long) : Effect
    }
}
