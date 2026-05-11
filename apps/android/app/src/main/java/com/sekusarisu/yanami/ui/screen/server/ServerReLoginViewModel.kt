package com.sekusarisu.yanami.ui.screen.server

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.ServerInstance
import com.sekusarisu.yanami.domain.repository.Requires2FAException
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.mvi.MviViewModel
import kotlinx.coroutines.launch

/** 失效会话重登录 ViewModel */
class ServerReLoginViewModel(
        private val serverId: Long,
        private val forceTwoFa: Boolean,
        private val repository: ServerRepository,
        private val context: Context
) :
        MviViewModel<
                ServerReLoginContract.State,
                ServerReLoginContract.Event,
                ServerReLoginContract.Effect
                >(ServerReLoginContract.State()) {

    private var server: ServerInstance? = null

    init {
        loadServer()
    }

    override fun onEvent(event: ServerReLoginContract.Event) {
        when (event) {
            is ServerReLoginContract.Event.UsernameChanged ->
                    setState { copy(username = event.username) }
            is ServerReLoginContract.Event.PasswordChanged ->
                    setState { copy(password = event.password) }
            is ServerReLoginContract.Event.TwoFaCodeChanged ->
                    setState { copy(twoFaCode = event.code) }
            is ServerReLoginContract.Event.Submit -> submit()
        }
    }

    private fun loadServer() {
        setState { copy(isLoading = true, error = null) }
        screenModelScope.launch {
            val target = repository.getById(serverId) ?: repository.getActive()
            if (target == null) {
                setState {
                    copy(
                            isLoading = false,
                            error = context.getString(R.string.error_no_server_selected)
                    )
                }
                return@launch
            }

            server = target
            setState {
                copy(
                        isLoading = false,
                        serverName = target.name,
                        baseUrl = target.baseUrl,
                        username = target.username,
                        password = target.password,
                        requires2fa = forceTwoFa || target.requires2fa
                )
            }
        }
    }

    private fun submit() {
        val currentServer = server
        val state = currentState
        if (currentServer == null) {
            sendEffect(
                    ServerReLoginContract.Effect.ShowToast(
                            context.getString(R.string.error_no_server_selected)
                    )
            )
            return
        }

        val username = state.username.trim()
        val password = state.password
        val twoFaCode = state.twoFaCode.trim().ifBlank { null }

        if (username.isBlank() || password.isBlank()) {
            sendEffect(
                    ServerReLoginContract.Effect.ShowToast(
                            context.getString(R.string.add_server_fill_required)
                    )
            )
            return
        }
        if (state.requires2fa && twoFaCode == null) {
            sendEffect(
                    ServerReLoginContract.Effect.ShowToast(
                            context.getString(R.string.server_relogin_need_2fa)
                    )
            )
            return
        }

        setState { copy(isSubmitting = true, error = null) }
        screenModelScope.launch {
            try {
                val loginTarget =
                        currentServer.copy(
                                username = username,
                                password = password,
                                requires2fa = state.requires2fa
                        )
                repository.login(loginTarget, twoFaCode)
                repository.updateAuthInfo(
                        id = currentServer.id,
                        username = username,
                        password = password,
                        requires2fa = state.requires2fa
                )

                setState { copy(isSubmitting = false, twoFaCode = "") }
                sendEffect(
                        ServerReLoginContract.Effect.ShowToast(
                                context.getString(R.string.server_relogin_success)
                        )
                )
                sendEffect(ServerReLoginContract.Effect.NavigateToNodeList)
            } catch (e: Requires2FAException) {
                repository.updateRequires2fa(currentServer.id, true)
                setState {
                    copy(
                            isSubmitting = false,
                            requires2fa = true,
                            twoFaCode = "",
                            error = context.getString(R.string.server_relogin_need_2fa)
                    )
                }
            } catch (e: Exception) {
                setState {
                    copy(
                            isSubmitting = false,
                            error = context.getString(R.string.server_relogin_failed, e.message)
                    )
                }
            }
        }
    }
}
