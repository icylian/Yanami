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

    init {
        loadNodes()
    }

    override fun onEvent(event: NodeListContract.Event) {
        when (event) {
            is NodeListContract.Event.SearchQueryChanged -> {
                setState { copy(searchQuery = event.query) }
                applyFilters()
            }
            is NodeListContract.Event.GroupSelected -> {
                setState { copy(selectedGroup = event.group) }
                applyFilters()
            }
            is NodeListContract.Event.StatusFilterSelected -> {
                setState { copy(statusFilter = event.filter) }
                applyFilters()
            }
            is NodeListContract.Event.Refresh -> refreshNodes()
            is NodeListContract.Event.Retry -> loadNodes()
            is NodeListContract.Event.NodeClicked -> {
                sendEffect(NodeListContract.Effect.NavigateToNodeDetail(event.uuid))
            }
        }
    }

    /** 初始加载：恢复 session / 登录 + 获取基本信息 + 启动 WebSocket */
    private fun loadNodes() {
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
                setState { copy(serverName = server.name) }

                val sessionToken = ensureSession(server)

                // 1. HTTP POST 获取节点基本信息 + 初始状态
                val nodes = nodeRepository.getNodeInfos(server.baseUrl, sessionToken)
                updateNodesState(nodes)
                setState { copy(isLoading = false, error = null) }

                // 2. 启动 WebSocket 实时状态流
                startWebSocketStatusFlow(server.baseUrl, sessionToken, nodes, server.id, server.requires2fa, server.authType)
            } catch (e: Exception) {
                if (activeServerId != null && handleSessionExpired(activeServerId, activeRequires2fa, e, activeAuthType)) {
                    return@launch
                }
                setState {
                    copy(
                            isLoading = false,
                            error = context.getString(R.string.node_load_failed, e.message)
                    )
                }
            }
        }
    }

    /** 手动刷新（整体重新拉取） */
    private fun refreshNodes() {
        setState { copy(isRefreshing = true) }
        wsJob?.cancel()
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
                val sessionToken = ensureSession(server)

                val nodes = nodeRepository.getNodeInfos(server.baseUrl, sessionToken)
                updateNodesState(nodes)
                setState { copy(isRefreshing = false, error = null) }

                // 重新启动 WebSocket
                startWebSocketStatusFlow(server.baseUrl, sessionToken, nodes, server.id, server.requires2fa, server.authType)
            } catch (e: Exception) {
                if (activeServerId != null && handleSessionExpired(activeServerId, activeRequires2fa, e, activeAuthType)) {
                    return@launch
                }
                setState { copy(isRefreshing = false) }
                sendEffect(
                        NodeListContract.Effect.ShowToast(
                                context.getString(R.string.node_refresh_failed, e.message)
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
        applyFilters()
    }

    /** 根据搜索、分组和状态条件过滤节点 */
    private fun applyFilters() {
        val state = currentState
        val filtered =
                state.nodes.filter { node ->
                    val matchesSearch =
                            if (state.searchQuery.isBlank()) true
                            else {
                                val query = state.searchQuery.lowercase()
                                node.name.lowercase().contains(query) ||
                                        node.region.lowercase().contains(query) ||
                                        node.os.lowercase().contains(query) ||
                                        node.cpuName.lowercase().contains(query)
                            }

                    val matchesGroup =
                            if (state.selectedGroup == null) true
                            else node.group == state.selectedGroup

                    val matchesStatus =
                            when (state.statusFilter) {
                                NodeListContract.StatusFilter.ALL -> true
                                NodeListContract.StatusFilter.ONLINE -> node.isOnline
                                NodeListContract.StatusFilter.OFFLINE -> !node.isOnline
                            }

                    matchesSearch && matchesGroup && matchesStatus
                }

        setState { copy(filteredNodes = filtered) }
    }
}
