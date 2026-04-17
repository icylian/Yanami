package com.sekusarisu.yanami.ui.screen.terminal

import android.util.Log
import cafe.adriel.voyager.core.model.screenModelScope
import com.sekusarisu.yanami.data.local.preferences.UserPreferencesRepository
import com.sekusarisu.yanami.data.remote.buildKomariWebSocketEndpoint
import com.sekusarisu.yanami.data.remote.runKomariWebSocketLifecycle
import com.sekusarisu.yanami.data.remote.SessionManager
import com.sekusarisu.yanami.domain.model.TerminalSnippet
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.mvi.MviViewModel
import com.sekusarisu.yanami.ui.screen.isSessionAuthError
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

/**
 * SSH 终端 ViewModel
 *
 * 管理 WebSocket 连接生命周期，以及 [WsTerminalBridge] 与 WebSocket 之间的双向数据路由：
 * - 用户按键 → [TerminalInputCapture] → [sendInput] → [sendQueue] → WebSocket Binary Frame
 * - WebSocket Binary Frame → [WsTerminalBridge.feedOutput] → TerminalEmulator → 重绘
 * - 终端尺寸变化 → [sendResize] → [sendQueue] → WebSocket Text Frame (resize JSON)
 * - 定时心跳 → WebSocket Text Frame (heartbeat JSON)
 */
class SshTerminalViewModel(
        val uuid: String,
        private val serverRepository: ServerRepository,
        private val sessionManager: SessionManager,
        private val httpClient: HttpClient,
        private val userPreferencesRepository: UserPreferencesRepository
) :
        MviViewModel<SshTerminalContract.State, SshTerminalContract.Event, SshTerminalContract.Effect>(
                SshTerminalContract.State()
        ) {

    companion object {
        private const val TAG = "SshTerminalVM"
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
    }

    /** 消息队列，承载所有需要通过 WebSocket 发送的数据 */
    private val sendQueue = Channel<WsOutMessage>(Channel.BUFFERED)

    /**
     * TerminalSession 客户端桥接，Screen 创建 TerminalView 后通过
     * [TerminalClientBridge.onTextChanged] 注册重绘回调。
     */
    val clientBridge = TerminalClientBridge()

    /** WebSocket ↔ TerminalEmulator 桥接器，向 Screen 暴露 session 供 TerminalView 附加 */
    val terminalBridge = WsTerminalBridge(clientBridge)

    /** 缓存 onSizeChanged 最后报告的终端尺寸，建连后作为首次 resize 发送 */
    private var lastCols: Int = 80
    private var lastRows: Int = 24

    private var wsJob: Job? = null

    init {
        // 从 DataStore 读取上次保存的字号，恢复用户偏好
        screenModelScope.launch {
            userPreferencesRepository.terminalFontSize.collect { size ->
                setState { copy(fontSize = size) }
            }
        }
        screenModelScope.launch {
            userPreferencesRepository.terminalSnippets.collect { snippets ->
                setState { copy(snippets = snippets) }
            }
        }
        connect()
    }

    override fun onEvent(event: SshTerminalContract.Event) {
        when (event) {
            is SshTerminalContract.Event.Disconnect -> {
                wsJob?.cancel()
                sendEffect(SshTerminalContract.Effect.NavigateBack)
            }
            is SshTerminalContract.Event.FontSizeChanged -> {
                val newSize = (currentState.fontSize + event.delta).coerceIn(8, 32)
                setState { copy(fontSize = newSize) }
                screenModelScope.launch { userPreferencesRepository.setTerminalFontSize(newSize) }
            }
            is SshTerminalContract.Event.ToggleCtrl ->
                    setState { copy(ctrlActive = !ctrlActive, altActive = false) }
            is SshTerminalContract.Event.ToggleAlt ->
                    setState { copy(altActive = !altActive, ctrlActive = false) }
            is SshTerminalContract.Event.ToggleFn -> setState { copy(fnMode = !fnMode) }
            is SshTerminalContract.Event.ToggleSnippetsPanel ->
                    setState { copy(isSnippetsPanelOpen = !isSnippetsPanelOpen) }
            is SshTerminalContract.Event.SetSnippetsPanelOpen ->
                    setState { copy(isSnippetsPanelOpen = event.open) }
            is SshTerminalContract.Event.Resize -> {
                // 由 sendResize() 直接处理
            }
        }
    }

    /** 将用户按键字节路由到 WebSocket（由 TerminalInputCapture 调用）
     *
     * 若 CTRL/ALT 修饰键激活，自动应用修饰后发送，并将修饰键重置为未激活状态。
     * - CTRL + 单字节字符 → 该字符 & 0x1F（控制码）
     * - ALT + 任意字节 → ESC 前缀 + 原始字节
     */
    fun sendInput(bytes: ByteArray) {
        val ctrl = currentState.ctrlActive
        val alt = currentState.altActive
        if (ctrl) setState { copy(ctrlActive = false) }
        if (alt) setState { copy(altActive = false) }
        val modifiedBytes = when {
            ctrl && bytes.size == 1 -> byteArrayOf((bytes[0].toInt() and 0x1f).toByte())
            alt -> byteArrayOf(27) + bytes
            else -> bytes
        }
        screenModelScope.launch {
            sendQueue.send(WsOutMessage.Binary(modifiedBytes))
        }
    }

    /** 直接将原始字节发送到 WebSocket（不应用 CTRL/ALT 一次性修饰） */
    fun sendRawInput(bytes: ByteArray) {
        screenModelScope.launch {
            sendQueue.send(WsOutMessage.Binary(bytes))
        }
    }

    fun sendSnippet(snippet: TerminalSnippet) {
        val normalizedContent = normalizeSnippetContent(snippet.content)
        if (normalizedContent.isEmpty()) return
        val payload =
                buildString {
                    append(normalizedContent.replace("\n", "\r"))
                    if (snippet.appendEnter && !normalizedContent.endsWith("\n")) {
                        append('\r')
                    }
                }
        sendRawInput(payload.toByteArray(Charsets.UTF_8))
    }

    fun saveSnippet(
            snippetId: String?,
            title: String,
            content: String,
            appendEnter: Boolean
    ): Boolean {
        val normalizedTitle = title.trim()
        val normalizedContent = normalizeSnippetContent(content)
        if (normalizedTitle.isEmpty()) return false
        if (normalizedContent.isBlank()) return false

        val snippet =
                TerminalSnippet(
                        id = snippetId ?: UUID.randomUUID().toString(),
                        title = normalizedTitle,
                        content = normalizedContent,
                        appendEnter = appendEnter
                )
        val updatedSnippets =
                currentState.snippets
                        .filterNot { it.id == snippet.id }
                        .plus(snippet)
                        .sortedBy { it.title.lowercase() }
        persistSnippets(updatedSnippets)
        return true
    }

    fun deleteSnippet(snippetId: String) {
        persistSnippets(currentState.snippets.filterNot { it.id == snippetId })
    }

    /** 将终端尺寸变化发送到 WebSocket（由 Screen 层 Modifier.onSizeChanged 触发）
     *
     * 若 WebSocket 尚未建连，仅缓存最新尺寸；建连后立即作为初始 resize 发送。
     */
    fun sendResize(cols: Int, rows: Int) {
        lastCols = cols
        lastRows = rows
        if (!currentState.isConnected) return
        val json =
                buildJsonObject {
                    put("type", "resize")
                    put("cols", cols)
                    put("rows", rows)
                }
                        .toString()
        screenModelScope.launch { sendQueue.send(WsOutMessage.Text(json)) }
    }

    private fun connect() {
        wsJob =
                screenModelScope.launch {
                    val server = serverRepository.getActive()
                    if (server == null) {
                        setState { copy(isConnecting = false, error = "未选择服务器") }
                        return@launch
                    }

                    // GUEST 模式不支持 SSH 终端
                    if (server.authType == AuthType.GUEST) {
                        setState { copy(isConnecting = false, error = "游客模式不支持 SSH 终端") }
                        return@launch
                    }

                    val sessionToken = sessionManager.getSessionToken()
                    if (sessionToken == null) {
                        setState { copy(isConnecting = false, error = "无法获取 session_token，请重新登录") }
                        return@launch
                    }

                    val endpoint =
                            buildKomariWebSocketEndpoint(
                                    server.baseUrl,
                                    "/api/admin/client/$uuid/terminal"
                            )
                    val authType = sessionManager.getAuthType() ?: AuthType.PASSWORD

                    Log.d(TAG, "Preparing terminal WebSocket (authType=$authType)")

                    try {
                        val wsBlock: suspend DefaultClientWebSocketSession.() -> Unit = {
                            Log.d(TAG, "WebSocket connected")
                            val wsSession = this
                            setState { copy(isConnecting = false, isConnected = true) }

                            // 建连后立即发送初始 resize（使用 onSizeChanged 已缓存的实际尺寸）
                            val initResize =
                                    buildJsonObject {
                                        put("type", "resize")
                                        put("cols", lastCols)
                                        put("rows", lastRows)
                                    }
                                            .toString()
                            wsSession.send(Frame.Text(initResize))
                            Log.d(TAG, "Sent initial resize: ${lastCols}x${lastRows}")

                            // 心跳协程：每 30 秒发送一次 heartbeat
                            val heartbeatJob = launch {
                                while (isActive) {
                                    delay(HEARTBEAT_INTERVAL_MS)
                                    val heartbeat =
                                            buildJsonObject {
                                                        put("type", "heartbeat")
                                                        put("timestamp", Instant.now().toString())
                                                    }
                                                    .toString()
                                    wsSession.send(Frame.Text(heartbeat))
                                }
                            }

                            // 发送协程：从 sendQueue 取出消息发往 WebSocket
                            val senderJob = launch {
                                for (msg in sendQueue) {
                                    if (!isActive) break
                                    when (msg) {
                                        is WsOutMessage.Binary ->
                                                wsSession.send(Frame.Binary(true, msg.data))
                                        is WsOutMessage.Text -> wsSession.send(Frame.Text(msg.text))
                                    }
                                }
                            }

                            // 接收循环：将 Binary Frame 写入 TerminalEmulator
                            try {
                                for (frame in incoming) {
                                    when (frame) {
                                        is Frame.Binary -> terminalBridge.feedOutput(frame.data)
                                        else -> {}
                                    }
                                }
                            } finally {
                                heartbeatJob.cancel()
                                senderJob.cancel()
                            }
                        }

                        httpClient.runKomariWebSocketLifecycle(
                                endpoint = endpoint,
                                sessionToken = sessionToken,
                                authType = authType,
                                loggerTag = TAG,
                                block = wsBlock
                        )
                        // 连接正常关闭
                        sendEffect(SshTerminalContract.Effect.NavigateBack)
                    } catch (e: Exception) {
                        Log.w(TAG, "WebSocket error: ${e.message}")
                        if (e.isSessionAuthError()) {
                            setState {
                                copy(isConnecting = false, isConnected = false, error = "鉴权失败，请重新登录")
                            }
                            sendEffect(SshTerminalContract.Effect.ShowToast("SSH 连接被拒绝，请重新登录"))
                        } else {
                            setState {
                                copy(
                                        isConnecting = false,
                                        isConnected = false,
                                        error = e.message ?: "连接失败"
                                )
                            }
                            sendEffect(
                                    SshTerminalContract.Effect.ShowToast("SSH 连接失败: ${e.message}")
                            )
                        }
                    }
                }
    }

    override fun onDispose() {
        super.onDispose()
        terminalBridge.session.finishIfRunning()
        sendQueue.close()
    }

    private fun persistSnippets(snippets: List<TerminalSnippet>) {
        setState { copy(snippets = snippets) }
        screenModelScope.launch { userPreferencesRepository.setTerminalSnippets(snippets) }
    }

    private fun normalizeSnippetContent(content: String): String =
            content.replace("\r\n", "\n").replace('\r', '\n')

    // ─── 内部类型 ───

    private sealed interface WsOutMessage {
        data class Binary(val data: ByteArray) : WsOutMessage
        data class Text(val text: String) : WsOutMessage
    }

    /**
     * TerminalSession 客户端桥接器
     *
     * [onTextChanged] 由 Screen 在创建 TerminalView 后注册，触发 View 重绘。
     */
    class TerminalClientBridge : TerminalSessionClient {

        var onTextChanged: () -> Unit = {}

        override fun onTextChanged(changedSession: TerminalSession) = onTextChanged()

        override fun onTitleChanged(updatedSession: TerminalSession) {}

        override fun onSessionFinished(finishedSession: TerminalSession) {}

        override fun onCopyTextToClipboard(session: TerminalSession, text: String) {}

        override fun onPasteTextFromClipboard(session: TerminalSession?) {}

        override fun onBell(session: TerminalSession) {}

        override fun onColorsChanged(session: TerminalSession) {}

        override fun onTerminalCursorStateChange(state: Boolean) {}

        override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}

        override fun getTerminalCursorStyle(): Int = 0

        override fun logError(tag: String?, message: String?) {}

        override fun logWarn(tag: String?, message: String?) {}

        override fun logInfo(tag: String?, message: String?) {}

        override fun logDebug(tag: String?, message: String?) {}

        override fun logVerbose(tag: String?, message: String?) {}

        override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}

        override fun logStackTrace(tag: String?, e: Exception?) {}
    }
}
