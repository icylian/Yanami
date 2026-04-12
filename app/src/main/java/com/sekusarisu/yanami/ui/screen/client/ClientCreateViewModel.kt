package com.sekusarisu.yanami.ui.screen.client

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.ServerInstance
import com.sekusarisu.yanami.domain.repository.ClientRepository
import com.sekusarisu.yanami.domain.repository.Requires2FAException
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.domain.repository.SessionExpiredException
import com.sekusarisu.yanami.mvi.MviViewModel
import com.sekusarisu.yanami.ui.screen.isSessionAuthError
import com.sekusarisu.yanami.ui.screen.isTwoFaHint
import kotlinx.coroutines.launch

class ClientCreateViewModel(
        private val clientRepository: ClientRepository,
        private val serverRepository: ServerRepository,
        private val context: Context
) :
        MviViewModel<ClientCreateContract.State, ClientCreateContract.Event, ClientCreateContract.Effect>(
                ClientCreateContract.State()
        ) {

    override fun onEvent(event: ClientCreateContract.Event) {
        when (event) {
            is ClientCreateContract.Event.NameChanged -> setState { copy(name = event.value) }
            is ClientCreateContract.Event.Save -> saveClient()
            is ClientCreateContract.Event.DismissCreatedResult -> {
                setState { copy(createdResult = null) }
                sendEffect(ClientCreateContract.Effect.NavigateBack)
            }
        }
    }

    private fun saveClient() {
        setState { copy(isSaving = true, error = null) }
        screenModelScope.launch {
            var activeServerId: Long? = null
            var activeRequires2fa = false
            var activeAuthType = AuthType.PASSWORD
            try {
                val server =
                        serverRepository.getActive()
                                ?: throw Exception(
                                        context.getString(R.string.error_no_server_selected)
                                )
                activeServerId = server.id
                activeRequires2fa = server.requires2fa
                activeAuthType = server.authType
                ensureAdminAccess(server)
                val sessionToken = ensureSession(server)
                val result =
                        clientRepository.addClient(
                                baseUrl = server.baseUrl,
                                sessionToken = sessionToken,
                                authType = server.authType,
                                name = currentState.name.trim().ifEmpty { null }
                        )
                setState { copy(isSaving = false, createdResult = result) }
            } catch (e: Exception) {
                if (activeServerId != null &&
                                handleSessionExpired(
                                        activeServerId,
                                        activeRequires2fa,
                                        e,
                                        activeAuthType
                                )
                ) {
                    return@launch
                }
                setState { copy(isSaving = false, error = e.message) }
                sendEffect(
                        ClientCreateContract.Effect.ShowToast(
                                context.getString(R.string.client_create_failed, e.message)
                        )
                )
            }
        }
    }

    private suspend fun ensureSession(server: ServerInstance): String {
        return try {
            serverRepository.ensureSessionToken(server)
        } catch (e: Requires2FAException) {
            serverRepository.updateRequires2fa(server.id, true)
            throw SessionExpiredException(e.message ?: context.getString(R.string.error_no_session))
        } catch (e: Exception) {
            if (e.isSessionAuthError()) {
                throw SessionExpiredException(e.message ?: context.getString(R.string.error_no_session))
            }
            throw e
        }
    }

    private fun ensureAdminAccess(server: ServerInstance) {
        if (server.authType == AuthType.GUEST) {
            throw IllegalStateException(context.getString(R.string.client_management_guest_unsupported))
        }
    }

    private fun handleSessionExpired(
            serverId: Long,
            requires2fa: Boolean,
            error: Throwable,
            authType: AuthType
    ): Boolean {
        if (!error.isSessionAuthError()) return false
        if (authType == AuthType.GUEST) return false
        setState { copy(isSaving = false, error = null) }
        sendEffect(
                ClientCreateContract.Effect.ShowToast(
                        error.message ?: context.getString(R.string.error_no_session)
                )
        )
        if (authType == AuthType.API_KEY) {
            sendEffect(ClientCreateContract.Effect.NavigateToServerEdit(serverId))
        } else {
            sendEffect(
                    ClientCreateContract.Effect.NavigateToServerRelogin(
                            serverId = serverId,
                            forceTwoFa = requires2fa || error.isTwoFaHint()
                    )
            )
        }
        return true
    }
}
