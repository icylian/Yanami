package com.sekusarisu.yanami.ui.screen.client

import com.sekusarisu.yanami.domain.model.LoadNotificationDraft
import com.sekusarisu.yanami.domain.model.LoadNotificationTask
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.domain.model.OfflineNotificationConfig
import com.sekusarisu.yanami.mvi.UiEffect
import com.sekusarisu.yanami.mvi.UiEvent
import com.sekusarisu.yanami.mvi.UiState

object NotificationManagementContract {

    data class State(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val isSaving: Boolean = false,
        val clients: List<ManagedClient> = emptyList(),
        val offlineConfigs: List<OfflineNotificationConfig> = emptyList(),
        val offlineItems: List<OfflineItem> = emptyList(),
        val filteredOfflineItems: List<OfflineItem> = emptyList(),
        val loadTasks: List<LoadNotificationTask> = emptyList(),
        val filteredLoadTasks: List<LoadNotificationTask> = emptyList(),
        val searchQuery: String = "",
        val currentView: ContentView = ContentView.OFFLINE,
        val offlineEditor: OfflineEditorState? = null,
        val offlineBatchEditor: OfflineBatchEditorState? = null,
        val loadEditor: LoadEditorState? = null,
        val pendingDeleteLoadTask: LoadNotificationTask? = null,
        val error: String? = null,
        val serverName: String = ""
    ) : UiState

    enum class ContentView {
        OFFLINE,
        LOAD
    }

    data class OfflineItem(
        val client: ManagedClient,
        val config: OfflineNotificationConfig
    )

    data class OfflineEditorState(
        val client: ManagedClient,
        val gracePeriod: String = "0"
    )

    data class OfflineBatchEditorState(
        val items: List<OfflineItem> = emptyList(),
        val selectedClientUuids: Set<String> = emptySet(),
        val enabled: Boolean = true,
        val gracePeriod: String = "300"
    )

    data class LoadEditorState(
        val mode: EditorMode,
        val taskId: Int? = null,
        val draft: LoadNotificationDraft = LoadNotificationDraft()
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
        data class OfflineEnabledChanged(val uuid: String, val enabled: Boolean) : Event
        data class EditOfflineClicked(val uuid: String) : Event
        data object BatchEditOfflineClicked : Event
        data class OfflineEditorGracePeriodChanged(val value: String) : Event
        data object DismissOfflineEditor : Event
        data object SaveOfflineEditor : Event
        data class OfflineBatchEnabledChanged(val value: Boolean) : Event
        data class OfflineBatchGracePeriodChanged(val value: String) : Event
        data class ToggleOfflineBatchClient(val uuid: String) : Event
        data object DismissOfflineBatchEditor : Event
        data object SaveOfflineBatchEditor : Event
        data object LoadAddClicked : Event
        data class LoadEditClicked(val id: Int) : Event
        data object DismissLoadEditor : Event
        data class LoadEditorNameChanged(val value: String) : Event
        data class LoadEditorMetricChanged(val value: String) : Event
        data class LoadEditorThresholdChanged(val value: String) : Event
        data class LoadEditorRatioChanged(val value: String) : Event
        data class LoadEditorIntervalChanged(val value: String) : Event
        data class ToggleLoadEditorClient(val uuid: String) : Event
        data object SaveLoadEditor : Event
        data class LoadDeleteClicked(val id: Int) : Event
        data object DismissLoadDelete : Event
        data object ConfirmLoadDelete : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data class NavigateToServerRelogin(val serverId: Long, val forceTwoFa: Boolean) : Effect
        data class NavigateToServerEdit(val serverId: Long) : Effect
    }
}
