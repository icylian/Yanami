package com.sekusarisu.yanami.ui.screen.nodelist

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.domain.model.ServerInstance
import com.sekusarisu.yanami.domain.repository.NodeRepository
import com.sekusarisu.yanami.domain.repository.Requires2FAException
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.domain.repository.SessionExpiredException
import com.sekusarisu.yanami.mvi.MviViewModel
import com.sekusarisu.yanami.ui.screen.isSessionAuthError
import com.sekusarisu.yanami.ui.screen.isTwoFaHint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** 节点列表 ViewModel */
class NodeListViewModel(
        private val nodeRepository: NodeRepository,
        private val serverRepository: ServerRepository,
        private val context: Context
) :
        MviViewModel<NodeListContract.State, NodeListContract.Event, NodeListContract.Effect>(
                NodeListContract.State()
        ) {

    private var wsJob: Job? = null
    private var resumeStreamingJob: Job? = null
    private var isScreenStarted = false
    private var latestStreamRequest: NodeStatusStreamRequest? = null

    init {
        loadNodes()
    }

    fun onScreenStarted() {
        if (isScreenStarted) return
        isScreenStarted = true
        resumeStreamingIfNeeded()
    }

    fun onScreenStopped() {
        if (!isScreenStarted) return
        isScreenStarted = false
        resumeStreamingJob?.cancel()
        resumeStreamingJob = null
        wsJob?.cancel()
        wsJob = null
    }

    override fun onEvent(event: NodeListContract.Event) {
        when (event) {
            is NodeListContract.Event.SearchQueryChanged -> {
                setState { copy(searchQuery = event.query) }
            }
            is NodeListContract.Event.GroupSelected -> {
                setState { copy(selectedGroup = event.group) }
            }
            is NodeListContract.Event.StatusFilterSelected -> {
                setState { copy(statusFilter = event.filter) }
            }
            is NodeListContract.Event.Refresh -> refreshNodes()
            is NodeListContract.Event.Retry -> loadNodes()
            is NodeListContract.Event.NodeClicked -> {
                sendEffect(NodeListContract.Effect.NavigateToNodeDetail(event.uuid))
            }
            is NodeListContract.Event.ManageClientsClicked -> {
                sendEffect(NodeListContract.Effect.NavigateToClientManagement)
            }
        }
    }

    /** 初始加载：恢复 session / 登录 + 获取基本信息 + 启动 WebSocket */
    private fun loadNodes() {
        fetchNodes(NodeLoadMode.INITIAL)
    }

    /** 手动刷新（整体重新拉取） */
    private fun refreshNodes() {
        fetchNodes(NodeLoadMode.REFRESH)
    }

    private fun fetchNodes(mode: NodeLoadMode) {
        updateLoadingState(mode, isLoading = true)
        if (mode == NodeLoadMode.REFRESH) {
            wsJob?.cancel()
        }
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
                setState { copy(serverName = server.name) }
                val sessionToken = ensureSession(server)

                val nodes = nodeRepository.getNodeInfos(server.baseUrl, sessionToken)
                updateNodesState(nodes)
                updateLoadingState(mode, isLoading = false)

                latestStreamRequest =
                        NodeStatusStreamRequest(
                                baseUrl = server.baseUrl,
                                baseNodes = nodes,
                                serverId = server.id,
                                requires2fa = server.requires2fa,
                                authType = server.authType
                        )
                if (isScreenStarted) {
                    startWebSocketStatusFlow(
                            server.baseUrl,
                            sessionToken,
                            nodes,
                            server.id,
                            server.requires2fa,
                            server.authType
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
                handleFetchNodesError(mode, e)
            }
        }
    }

    private fun updateLoadingState(mode: NodeLoadMode, isLoading: Boolean) {
        when (mode) {
            NodeLoadMode.INITIAL -> setState { copy(isLoading = isLoading, error = null) }
            NodeLoadMode.REFRESH ->
                    setState {
                        if (isLoading) {
                            copy(isRefreshing = true)
                        } else {
                            copy(isRefreshing = false, error = null)
                        }
                    }
        }
    }

    private fun handleFetchNodesError(mode: NodeLoadMode, error: Exception) {
        when (mode) {
            NodeLoadMode.INITIAL -> {
                setState {
                    copy(
                            isLoading = false,
                            error = context.getString(R.string.node_load_failed, error.message)
                    )
                }
            }
            NodeLoadMode.REFRESH -> {
                setState { copy(isRefreshing = false) }
                sendEffect(
                        NodeListContract.Effect.ShowToast(
                                context.getString(R.string.node_refresh_failed, error.message)
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

    private fun handleSessionExpired(
            serverId: Long,
            requires2fa: Boolean,
            error: Throwable,
            authType: AuthType = AuthType.PASSWORD
    ): Boolean {
        if (!error.isSessionAuthError()) return false
        if (authType == AuthType.GUEST) return false
        wsJob?.cancel()
        setState { copy(isLoading = false, isRefreshing = false, error = null) }
        sendEffect(
                NodeListContract.Effect.ShowToast(
                        error.message ?: context.getString(R.string.error_no_session)
                )
        )
        if (authType == AuthType.API_KEY) {
            // API_KEY 模式：导航到编辑服务器页面
            sendEffect(NodeListContract.Effect.NavigateToServerEdit(serverId))
        } else {
            val forceTwoFa = requires2fa || error.isTwoFaHint()
            sendEffect(NodeListContract.Effect.NavigateToServerRelogin(serverId, forceTwoFa))
        }
        return true
    }

    private fun resumeStreamingIfNeeded() {
        val streamRequest = latestStreamRequest ?: return
        if (!isScreenStarted || currentState.isLoading || wsJob != null) return

        resumeStreamingJob?.cancel()
        resumeStreamingJob =
                screenModelScope.launch {
                    try {
                        val activeServer = serverRepository.getActive() ?: return@launch
                        if (activeServer.id != streamRequest.serverId) {
                            loadNodes()
                            return@launch
                        }
                        val sessionToken = ensureSession(activeServer)
                        startWebSocketStatusFlow(
                                baseUrl = streamRequest.baseUrl,
                                sessionToken = sessionToken,
                                baseNodes =
                                        if (currentState.nodes.isNotEmpty()) currentState.nodes
                                        else streamRequest.baseNodes,
                                serverId = streamRequest.serverId,
                                requires2fa = streamRequest.requires2fa,
                                authType = streamRequest.authType
                        )
                    } catch (e: Exception) {
                        if (!handleSessionExpired(
                                        streamRequest.serverId,
                                        streamRequest.requires2fa,
                                        e,
                                        streamRequest.authType
                                )
                        ) {
                            android.util.Log.w(
                                    "NodeListVM",
                                    "Failed to resume status flow: ${e.message}"
                            )
                        }
                    }
                }
    }

    /** 启动 WebSocket RPC 实时状态流 */
    private fun startWebSocketStatusFlow(
            baseUrl: String,
            sessionToken: String,
            baseNodes: List<Node>,
            serverId: Long,
            requires2fa: Boolean,
            authType: AuthType = AuthType.PASSWORD
    ) {
        wsJob?.cancel()
        wsJob =
                nodeRepository
                        .observeNodeStatus(baseUrl, sessionToken, baseNodes)
                        .onEach { updatedNodes -> updateNodesState(updatedNodes) }
                        .catch { e ->
                            if (!handleSessionExpired(serverId, requires2fa, e, authType)) {
                                android.util.Log.w("NodeListVM", "Status flow error: ${e.message}")
                            }
                        }
                        .launchIn(screenModelScope)
    }

    /** 更新节点数据并重新计算统计和过滤 */
    private fun updateNodesState(nodes: List<Node>) {
        val groups = nodes.map { it.group }.filter { it.isNotBlank() }.distinct().sorted()
        val onlineNodes = nodes.filter { it.isOnline }
        val onlineCount = onlineNodes.size
        val offlineCount = nodes.size - onlineCount

        setState {
            copy(
                    nodes = nodes,
                    groups = groups,
                    onlineCount = onlineCount,
                    offlineCount = offlineCount,
                    totalCount = nodes.size,
                    totalNetIn = onlineNodes.sumOf { it.netIn },
                    totalNetOut = onlineNodes.sumOf { it.netOut },
                    totalTrafficUp = onlineNodes.sumOf { it.netTotalUp },
                    totalTrafficDown = onlineNodes.sumOf { it.netTotalDown }
            )
        }
    }

    private enum class NodeLoadMode {
        INITIAL,
        REFRESH
    }

    private data class NodeStatusStreamRequest(
            val baseUrl: String,
            val baseNodes: List<Node>,
            val serverId: Long,
            val requires2fa: Boolean,
            val authType: AuthType
    )
}
