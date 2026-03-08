package com.sekusarisu.yanami.ui.screen.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import cafe.adriel.voyager.core.model.screenModelScope
import com.sekusarisu.yanami.data.remote.UpdateCheckService
import com.sekusarisu.yanami.data.remote.dto.UpdateInfoDto
import com.sekusarisu.yanami.mvi.MviViewModel
import com.sekusarisu.yanami.mvi.UiEffect
import com.sekusarisu.yanami.mvi.UiEvent
import com.sekusarisu.yanami.mvi.UiState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class AboutState(
        val isCheckingUpdate: Boolean = false,
        val updateInfo: UpdateInfoDto? = null,
        val showUpdateDialog: Boolean = false,
        val currentVersionCode: Int = 0,
        val currentVersionName: String = "unknown"
) : UiState

sealed interface AboutEvent : UiEvent {
    data object CheckForUpdate : AboutEvent
    data object DismissUpdateDialog : AboutEvent
}

sealed interface AboutEffect : UiEffect {
    data class ShowToast(val message: String) : AboutEffect
}

class AboutViewModel(
        private val updateCheckService: UpdateCheckService,
        context: Context
) : MviViewModel<AboutState, AboutEvent, AboutEffect>(AboutState()) {

    init {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toInt() ?: 0
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode ?: 0
        }
        setState {
            copy(
                    currentVersionCode = versionCode,
                    currentVersionName = packageInfo?.versionName ?: "unknown"
            )
        }

        updateCheckService.latestUpdate
                .onEach { info ->
                    if (info != null) {
                        setState { copy(updateInfo = info, showUpdateDialog = true) }
                    }
                }
                .launchIn(screenModelScope)
    }

    override fun onEvent(event: AboutEvent) {
        when (event) {
            is AboutEvent.CheckForUpdate -> checkUpdate()
            is AboutEvent.DismissUpdateDialog -> {
                setState { copy(showUpdateDialog = false) }
            }
        }
    }

    private fun checkUpdate() {
        setState { copy(isCheckingUpdate = true) }
        screenModelScope.launch {
            val info = updateCheckService.checkForUpdate()
            if (info == null) {
                setState { copy(isCheckingUpdate = false) }
                sendEffect(AboutEffect.ShowToast("update_check_failed"))
            } else if (info.versionCode > currentState.currentVersionCode) {
                setState {
                    copy(
                            isCheckingUpdate = false,
                            updateInfo = info,
                            showUpdateDialog = true
                    )
                }
            } else {
                setState { copy(isCheckingUpdate = false) }
                sendEffect(AboutEffect.ShowToast("update_already_latest"))
            }
        }
    }
}
