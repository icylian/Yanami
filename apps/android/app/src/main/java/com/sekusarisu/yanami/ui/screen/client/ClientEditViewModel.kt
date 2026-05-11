package com.sekusarisu.yanami.ui.screen.client

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.ManagedClientDraft
import com.sekusarisu.yanami.domain.model.ServerInstance
import com.sekusarisu.yanami.domain.model.toDraft
import com.sekusarisu.yanami.domain.repository.ClientRepository
import com.sekusarisu.yanami.domain.repository.Requires2FAException
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.domain.repository.SessionExpiredException
import com.sekusarisu.yanami.mvi.MviViewModel
import com.sekusarisu.yanami.ui.screen.isSessionAuthError
import com.sekusarisu.yanami.ui.screen.isTwoFaHint
import kotlinx.coroutines.launch

class ClientEditViewModel(
        private val uuid: String,
        private val clientRepository: ClientRepository,
        private val serverRepository: ServerRepository,
        private val context: Context
) :
        MviViewModel<ClientEditContract.State, ClientEditContract.Event, ClientEditContract.Effect>(
                ClientEditContract.State()
        ) {

    init {
        loadClient()
    }

    override fun onEvent(event: ClientEditContract.Event) {
        when (event) {
            is ClientEditContract.Event.TextChanged -> {
                setState {
                    copy(
                            draft =
                                    draft.updateTextField(
                                            field = event.field,
                                            value = event.value
                                    )
                    )
                }
            }
            is ClientEditContract.Event.AutoRenewalChanged -> {
                setState { copy(draft = draft.copy(autoRenewal = event.value)) }
            }
            is ClientEditContract.Event.HiddenChanged -> {
                setState { copy(draft = draft.copy(hidden = event.value)) }
            }
            is ClientEditContract.Event.TrafficLimitTypeChanged -> {
                setState { copy(draft = draft.copy(trafficLimitType = event.value)) }
            }
            is ClientEditContract.Event.Save -> saveClient()
            is ClientEditContract.Event.Retry -> loadClient()
        }
    }

    private fun loadClient() {
        setState { copy(isLoading = true, error = null) }
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
                val client =
                        clientRepository.getClient(
                                baseUrl = server.baseUrl,
                                sessionToken = sessionToken,
                                authType = server.authType,
                                uuid = uuid
                        )
                setState {
                    copy(
                            isLoading = false,
                            clientName = client.name,
                            draft = client.toDraft(),
                            error = null
                    )
                }
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
                setState {
                    copy(
                            isLoading = false,
                            error = context.getString(R.string.client_edit_load_failed, e.message)
                    )
                }
            }
        }
    }

    private fun saveClient() {
        setState { copy(isSaving = true) }
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
                clientRepository.updateClient(
                        baseUrl = server.baseUrl,
                        sessionToken = sessionToken,
                        authType = server.authType,
                        uuid = uuid,
                        draft = currentState.draft
                )
                setState { copy(isSaving = false) }
                sendEffect(
                        ClientEditContract.Effect.ShowToast(
                                context.getString(R.string.client_edit_saved)
                        )
                )
                sendEffect(ClientEditContract.Effect.NavigateBack)
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
                setState { copy(isSaving = false) }
                sendEffect(
                        ClientEditContract.Effect.ShowToast(
                                context.getString(R.string.client_edit_save_failed, e.message)
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
        setState { copy(isLoading = false, isSaving = false, error = null) }
        sendEffect(
                ClientEditContract.Effect.ShowToast(
                        error.message ?: context.getString(R.string.error_no_session)
                )
        )
        if (authType == AuthType.API_KEY) {
            sendEffect(ClientEditContract.Effect.NavigateToServerEdit(serverId))
        } else {
            sendEffect(
                    ClientEditContract.Effect.NavigateToServerRelogin(
                            serverId = serverId,
                            forceTwoFa = requires2fa || error.isTwoFaHint()
                    )
            )
        }
        return true
    }
}

private fun ManagedClientDraft.updateTextField(
        field: ClientEditContract.TextField,
        value: String
): ManagedClientDraft =
        when (field) {
            ClientEditContract.TextField.NAME -> copy(name = value)
            ClientEditContract.TextField.GROUP -> copy(group = value)
            ClientEditContract.TextField.TAGS -> copy(tags = value)
            ClientEditContract.TextField.REMARK -> copy(remark = value)
            ClientEditContract.TextField.PUBLIC_REMARK -> copy(publicRemark = value)
            ClientEditContract.TextField.WEIGHT -> copy(weight = value)
            ClientEditContract.TextField.PRICE -> copy(price = value)
            ClientEditContract.TextField.BILLING_CYCLE -> copy(billingCycle = value)
            ClientEditContract.TextField.CURRENCY -> copy(currency = value)
            ClientEditContract.TextField.EXPIRED_AT -> copy(expiredAt = value)
            ClientEditContract.TextField.TRAFFIC_LIMIT -> copy(trafficLimit = value)
        }
