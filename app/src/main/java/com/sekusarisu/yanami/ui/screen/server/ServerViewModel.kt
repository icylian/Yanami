package com.sekusarisu.yanami.ui.screen.server

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.ServerInstance
import com.sekusarisu.yanami.domain.repository.Requires2FAException
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** 实例列表 ViewModel */
class ServerListViewModel(private val repository: ServerRepository, private val context: Context) :
        MviViewModel<ServerContract.ListState, ServerContract.Event, ServerContract.Effect>(
                ServerContract.ListState()
        ) {

    init {
        observeServers()
    }

    private fun observeServers() {
        repository
                .getAllFlow()
                .onEach { servers -> setState { copy(servers = servers, isLoading = false) } }
                .launchIn(screenModelScope)
    }

    override fun onEvent(event: ServerContract.Event) {
        when (event) {
            is ServerContract.Event.SelectServer -> activateServer(event.id)
            is ServerContract.Event.DeleteServer -> deleteServer(event.id)
            else -> {
                /* 其他事件在 AddServerViewModel 处理 */
            }
        }
    }

    private fun activateServer(id: Long) {
        screenModelScope.launch {
            try {
                repository.setActive(id)
                sendEffect(
                        ServerContract.Effect.ShowToast(context.getString(R.string.server_switched))
                )
                sendEffect(ServerContract.Effect.NavigateToNodeList)
            } catch (e: Exception) {
                sendEffect(
                        ServerContract.Effect.ShowToast(
                                context.getString(R.string.server_switch_failed, e.message)
                        )
                )
            }
        }
    }

    private fun deleteServer(id: Long) {
        screenModelScope.launch {
            try {
                repository.remove(id)
                sendEffect(
                        ServerContract.Effect.ShowToast(context.getString(R.string.server_deleted))
                )
            } catch (e: Exception) {
                sendEffect(
                        ServerContract.Effect.ShowToast(
                                context.getString(R.string.server_delete_failed, e.message)
                        )
                )
            }
        }
    }
}

/** 添加/编辑实例 ViewModel */
class AddServerViewModel(
        private val editServerId: Long?,
        private val repository: ServerRepository,
        private val context: Context
) :
        MviViewModel<ServerContract.AddState, ServerContract.Event, ServerContract.Effect>(
                ServerContract.AddState()
        ) {

    private var editingServer: ServerInstance? = null

    init {
        if (editServerId != null) {
            loadEditingServer(editServerId)
        }
    }

    override fun onEvent(event: ServerContract.Event) {
        when (event) {
            is ServerContract.Event.UpdateName -> setState { copy(name = event.name) }
            is ServerContract.Event.UpdateBaseUrl -> setState { copy(baseUrl = event.url) }
            is ServerContract.Event.UpdateUsername -> setState { copy(username = event.username) }
            is ServerContract.Event.UpdatePassword -> setState { copy(password = event.password) }
            is ServerContract.Event.UpdateTwoFaCode -> setState { copy(twoFaCode = event.code) }
            is ServerContract.Event.UpdateAuthType ->
                    setState {
                        copy(
                                authType = event.authType,
                                testResult = null,
                                testError = null
                        )
                    }
            is ServerContract.Event.UpdateApiKey -> setState { copy(apiKey = event.apiKey) }
            is ServerContract.Event.TestConnection -> testConnection()
            is ServerContract.Event.SaveServer -> saveServer()
            else -> {
                /* 其他事件不处理 */
            }
        }
    }

    private fun testConnection() {
        val state = currentState

        when (state.authType) {
            AuthType.GUEST -> {
                if (state.baseUrl.isBlank()) {
                    sendEffect(
                            ServerContract.Effect.ShowToast(
                                    context.getString(R.string.add_server_fill_guest_url)
                            )
                    )
                    return
                }
            }
            AuthType.API_KEY -> {
                if (state.baseUrl.isBlank() || state.apiKey.isBlank()) {
                    sendEffect(
                            ServerContract.Effect.ShowToast(
                                    context.getString(R.string.add_server_fill_api_key)
                            )
                    )
                    return
                }
            }
            AuthType.PASSWORD -> {
                if (state.baseUrl.isBlank() ||
                                state.username.isBlank() ||
                                state.password.isBlank()
                ) {
                    sendEffect(
                            ServerContract.Effect.ShowToast(
                                    context.getString(R.string.add_server_fill_required)
                            )
                    )
                    return
                }
            }
        }

        setState { copy(isTesting = true, testResult = null, testError = null) }

        screenModelScope.launch {
            try {
                val version =
                        when (state.authType) {
                            AuthType.GUEST ->
                                    repository.testConnectionAsGuest(state.baseUrl)
                            AuthType.API_KEY ->
                                    repository.testConnectionWithApiKey(
                                            state.baseUrl,
                                            state.apiKey
                                    )
                            AuthType.PASSWORD -> {
                                val twoFaCode = state.twoFaCode.ifBlank { null }
                                repository.testConnection(
                                        state.baseUrl,
                                        state.username,
                                        state.password,
                                        twoFaCode
                                )
                            }
                        }
                setState {
                    copy(
                            isTesting = false,
                            testResult =
                                    context.getString(R.string.add_server_test_success, version),
                            testError = null,
                            show2faField = false
                    )
                }
            } catch (e: Requires2FAException) {
                setState {
                    copy(
                            isTesting = false,
                            testResult = null,
                            testError = e.message,
                            show2faField = true
                    )
                }
            } catch (e: Exception) {
                setState {
                    copy(
                            isTesting = false,
                            testResult = null,
                            testError = context.getString(R.string.add_server_test_fail, e.message)
                    )
                }
            }
        }
    }

    private fun saveServer() {
        val state = currentState

        when (state.authType) {
            AuthType.GUEST -> {
                if (state.name.isBlank() || state.baseUrl.isBlank()) {
                    sendEffect(
                            ServerContract.Effect.ShowToast(
                                    context.getString(R.string.add_server_fill_all)
                            )
                    )
                    return
                }
            }
            AuthType.API_KEY -> {
                if (state.name.isBlank() || state.baseUrl.isBlank() || state.apiKey.isBlank()) {
                    sendEffect(
                            ServerContract.Effect.ShowToast(
                                    context.getString(R.string.add_server_fill_all)
                            )
                    )
                    return
                }
            }
            AuthType.PASSWORD -> {
                if (state.name.isBlank() ||
                                state.baseUrl.isBlank() ||
                                state.username.isBlank() ||
                                state.password.isBlank()
                ) {
                    sendEffect(
                            ServerContract.Effect.ShowToast(
                                    context.getString(R.string.add_server_fill_all)
                            )
                    )
                    return
                }
            }
        }

        setState { copy(isSaving = true) }

        screenModelScope.launch {
            try {
                val normalizedInstance =
                        ServerInstance(
                                name = state.name.trim(),
                                baseUrl = state.baseUrl.trim().trimEnd('/'),
                                username = state.username.trim(),
                                password = state.password,
                                requires2fa = state.show2faField,
                                authType = state.authType,
                                apiKey = state.apiKey.ifBlank { null }
                        )

                if (editingServer != null) {
                    val original = editingServer!!
                    val authChanged =
                            original.baseUrl != normalizedInstance.baseUrl ||
                                    original.username != normalizedInstance.username ||
                                    original.password != normalizedInstance.password ||
                                    original.authType != normalizedInstance.authType ||
                                    original.apiKey != normalizedInstance.apiKey

                    val updated =
                            original.copy(
                                    name = normalizedInstance.name,
                                    baseUrl = normalizedInstance.baseUrl,
                                    username = normalizedInstance.username,
                                    password = normalizedInstance.password,
                                    sessionToken = if (authChanged) null else original.sessionToken,
                                    requires2fa = normalizedInstance.requires2fa,
                                    authType = normalizedInstance.authType,
                                    apiKey = normalizedInstance.apiKey
                            )
                    repository.update(updated)
                    setState { copy(isSaving = false) }
                    sendEffect(ServerContract.Effect.ServerUpdated)
                } else {
                    val id = repository.add(normalizedInstance)
                    repository.setActive(id)

                    // 尝试登录获取 session_token 并持久化（API_KEY 模式下设置 session）
                    try {
                        val savedInstance = normalizedInstance.copy(id = id)
                        val twoFaCode = state.twoFaCode.ifBlank { null }
                        repository.login(savedInstance, twoFaCode)
                    } catch (e: Exception) {
                        android.util.Log.w("AddServerVM", "Initial login failed: ${e.message}")
                    }

                    setState { copy(isSaving = false) }
                    sendEffect(ServerContract.Effect.ServerSaved)
                }
            } catch (e: Exception) {
                setState { copy(isSaving = false) }
                sendEffect(
                        ServerContract.Effect.ShowToast(
                                context.getString(R.string.add_server_save_failed, e.message)
                        )
                )
            }
        }
    }

    private fun loadEditingServer(id: Long) {
        screenModelScope.launch {
            val server = repository.getById(id) ?: return@launch
            editingServer = server
            setState {
                copy(
                        name = server.name,
                        baseUrl = server.baseUrl,
                        username = server.username,
                        password = server.password,
                        show2faField = server.requires2fa,
                        authType = server.authType,
                        apiKey = server.apiKey ?: ""
                )
            }
        }
    }
}
