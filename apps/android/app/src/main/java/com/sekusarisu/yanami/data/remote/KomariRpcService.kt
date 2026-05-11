package com.sekusarisu.yanami.data.remote

import android.util.Log
import com.sekusarisu.yanami.data.remote.dto.LoadRecordsResponseDto
import com.sekusarisu.yanami.data.remote.dto.NodeInfoDto
import com.sekusarisu.yanami.data.remote.dto.NodeStatusDto
import com.sekusarisu.yanami.data.remote.dto.PingRecordsResponseDto
import com.sekusarisu.yanami.data.remote.dto.RecentStatusItemDto
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.CustomHeader
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Komari RPC 服务
 *
 * 提供两种调用方式：
 * - HTTP POST `/api/rpc2`：用于一次性请求（如获取节点列表）
 * - WebSocket `/api/rpc2`：通过 HTTP Upgrade 实时流式获取节点状态
 *
 * 所有请求通过 Cookie: session_token=xxx 进行认证。
 */
class KomariRpcService(
        private val httpClient: HttpClient,
        private val sessionManager: SessionManager
) {

    companion object {
        private const val TAG = "KomariRpc"
        /** WebSocket 轮询间隔（毫秒） */
        const val WS_POLL_INTERVAL_MS = 2_000L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // ─── HTTP POST RPC2（一次性请求）───

    /**
     * 获取所有节点基本信息（HTTP POST）
     * @return Map<UUID, NodeInfoDto>
     */
    suspend fun getNodes(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType = AuthType.PASSWORD,
            customHeaders: List<CustomHeader>? = null
    ): Map<String, NodeInfoDto> {
        val envelope =
                callRpcHttp(
                        baseUrl,
                        sessionToken,
                        "common:getNodes",
                        authType = authType,
                        customHeaders = customHeaders
                )
        return decodeOptionalRpcResult(
                envelope,
                MapSerializer(String.serializer(), NodeInfoDto.serializer()),
                emptyMap()
        )
    }

    /**
     * 获取所有节点最新状态（HTTP POST，单次）
     * @return Map<UUID, NodeStatusDto>
     */
    suspend fun getNodesLatestStatus(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType = AuthType.PASSWORD,
            customHeaders: List<CustomHeader>? = null
    ): Map<String, NodeStatusDto> {
        val envelope =
                callRpcHttp(
                        baseUrl,
                        sessionToken,
                        "common:getNodesLatestStatus",
                        authType = authType,
                        customHeaders = customHeaders
                )
        return decodeOptionalRpcResult(
                envelope,
                MapSerializer(String.serializer(), NodeStatusDto.serializer()),
                emptyMap()
        )
    }

    /**
     * 获取服务端版本（HTTP POST）
     * @return 版本号字符串
     */
    suspend fun getVersion(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType = AuthType.PASSWORD,
            customHeaders: List<CustomHeader>? = null
    ): String {
        val envelope =
                callRpcHttp(
                        baseUrl,
                        sessionToken,
                        "common:getVersion",
                        authType = authType,
                        customHeaders = customHeaders
                )
        val result = decodeRpcResult(envelope, RpcVersionResult.serializer())
        return result.version.ifBlank { throw RpcException(-1, "Invalid version response") }
    }

    /**
     * 获取节点最近 1 分钟未降采样状态数据（GET /api/recent/{uuid}）
     *
     * 返回约 60 条 1 秒粒度数据，适合用作实时图表的 seed。
     * @return 嵌套结构的实时状态列表（时间由旧到新）
     */
    suspend fun getNodeRecentStatus(
            baseUrl: String,
            sessionToken: String,
            uuid: String,
            authType: AuthType = AuthType.PASSWORD,
            customHeaders: List<CustomHeader>? = null
    ): List<RecentStatusItemDto> {
        val url = baseUrl.trimEnd('/') + "/api/recent/$uuid"
        val response =
                httpClient.get(url) {
                    if (customHeaders != null) {
                        skipSessionInterceptor()
                    }
                    applyCustomHeaders(resolveCustomHeaders(customHeaders))
                    applyAuth(sessionToken, authType)
                }
        val responseText = response.bodyAsText()
        return json.decodeFromString(RecentStatusResponse.serializer(), responseText).data
    }

    // ─── WebSocket RPC（通过 HTTP Upgrade 建立 WebSocket 连接）───

    sealed interface KomariWsEvent {
        data class Status(val statusMap: Map<String, NodeStatusDto>) : KomariWsEvent
        data class LoadRecords(val records: LoadRecordsResponseDto) : KomariWsEvent
        data class PingRecords(val records: PingRecordsResponseDto) : KomariWsEvent
    }

    /**
     * 通过 WebSocket 持续获取节点最新状态与历史记录（多路复用）
     *
     * 使用 Cookie: session_token=xxx 进行认证。
     */
    fun observeNodesLatestStatus(
            baseUrl: String,
            sessionToken: String,
            detailUuid: String? = null,
            loadHours: Int? = null,
            pingHours: Int? = null,
            authType: AuthType = AuthType.PASSWORD,
            customHeaders: List<CustomHeader>? = null
    ): Flow<KomariWsEvent> = channelFlow {
        val endpoint = buildKomariWebSocketEndpoint(baseUrl, "/api/rpc2")
        val resolvedCustomHeaders = resolveCustomHeaders(customHeaders)

        var requestId = 1

        val wsBlock:
                suspend io.ktor.client.plugins.websocket.DefaultClientWebSocketSession.() -> Unit =
                {
                    Log.d(TAG, "WebSocket connected successfully")

                    var isFirstPoll = true
                    while (isActive && !isClosedForSend) {
                        var expectedCount = 0
                        val reqStatusId = requestId++
                        expectedCount++

                        val rpcRequest = buildJsonObject {
                            put("jsonrpc", "2.0")
                            put("method", "common:getNodesLatestStatus")
                            put("id", reqStatusId)
                        }
                        send(Frame.Text(rpcRequest.toString()))

                        var reqLoadId = -1
                        if (isFirstPoll && detailUuid != null && loadHours != null) {
                            reqLoadId = requestId++
                            expectedCount++
                            val loadReq = buildJsonObject {
                                put("jsonrpc", "2.0")
                                put("method", "common:getRecords")
                                put(
                                        "params",
                                        buildJsonObject {
                                            put("uuid", detailUuid)
                                            put("type", "load")
                                            put("hours", loadHours)
                                        }
                                )
                                put("id", reqLoadId)
                            }
                            send(Frame.Text(loadReq.toString()))
                        }

                        var reqPingId = -1
                        if (isFirstPoll && detailUuid != null && pingHours != null) {
                            reqPingId = requestId++
                            expectedCount++
                            val pingReq = buildJsonObject {
                                put("jsonrpc", "2.0")
                                put("method", "common:getRecords")
                                put(
                                        "params",
                                        buildJsonObject {
                                            put("uuid", detailUuid)
                                            put("type", "ping")
                                            put("hours", pingHours)
                                        }
                                )
                                put("id", reqPingId)
                            }
                            send(Frame.Text(pingReq.toString()))
                        }

                        // 接收响应
                        var receivedCount = 0
                        while (receivedCount < expectedCount && isActive) {
                            val frame = withTimeoutOrNull(10_000) { incoming.receive() } ?: break
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                try {
                                    val envelope = decodeRpcEnvelope(text)
                                    val idStr = rpcEnvelopeId(envelope) ?: continue

                                    if (idStr == reqStatusId.toString()) {
                                        val statusMap =
                                                decodeOptionalRpcResult(
                                                        envelope,
                                                        MapSerializer(
                                                                String.serializer(),
                                                                NodeStatusDto.serializer()
                                                        ),
                                                        emptyMap()
                                                )
                                        if (statusMap.isNotEmpty()) {
                                            send(KomariWsEvent.Status(statusMap))
                                        }
                                        receivedCount++
                                    } else if (idStr == reqLoadId.toString()) {
                                        val dto =
                                                decodeRpcResult(
                                                        envelope,
                                                        LoadRecordsResponseDto.serializer()
                                                )
                                        send(KomariWsEvent.LoadRecords(dto))
                                        receivedCount++
                                    } else if (idStr == reqPingId.toString()) {
                                        val dto =
                                                decodeRpcResult(
                                                        envelope,
                                                        PingRecordsResponseDto.serializer()
                                                )
                                        send(KomariWsEvent.PingRecords(dto))
                                        receivedCount++
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Parse error: ${e.message}")
                                }
                            }
                        }

                        isFirstPoll = false
                        delay(WS_POLL_INTERVAL_MS)
                    }
                }

        httpClient.runKomariWebSocketLifecycle(
                endpoint = endpoint,
                sessionToken = sessionToken,
                authType = authType,
                customHeaders = resolvedCustomHeaders,
                loggerTag = TAG,
                reconnectDelayMs = 5_000L,
                maxReconnectDelayMs = 30_000L,
                reconnectJitterMs = 1_000L,
                reconnectOnNormalClose = true,
                block = wsBlock
        )
    }

    // Observe combined records handled in observeNodesLatestStatus

    // ─── 内部方法 ───

    private suspend fun callRpcHttp(
            baseUrl: String,
            sessionToken: String,
            method: String,
            params: JsonObject? = null,
            authType: AuthType = AuthType.PASSWORD,
            customHeaders: List<CustomHeader>? = null
    ): RpcEnvelope {
        val url = baseUrl.trimEnd('/') + "/api/rpc2"
        val requestBody = buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", method)
            if (params != null) put("params", params)
            put("id", 1)
        }

        val response =
                httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    if (customHeaders != null) {
                        skipSessionInterceptor()
                    }
                    applyCustomHeaders(resolveCustomHeaders(customHeaders))
                    applyAuth(sessionToken, authType)
                    setBody(requestBody.toString())
                }

        return decodeRpcEnvelope(response.bodyAsText())
    }

    private fun resolveCustomHeaders(customHeaders: List<CustomHeader>?): List<CustomHeader> {
        return customHeaders ?: sessionManager.getCustomHeaders()
    }

    private fun decodeRpcEnvelope(rawText: String): RpcEnvelope {
        return json.decodeFromString(RpcEnvelope.serializer(), rawText)
    }

    private fun rpcEnvelopeId(envelope: RpcEnvelope): String? {
        return envelope.id?.jsonPrimitive?.contentOrNull
    }

    private fun ensureNoRpcError(envelope: RpcEnvelope) {
        val error = envelope.error ?: return
        throw RpcException(error.code ?: -1, error.message ?: "Unknown RPC error")
    }

    private fun <T> decodeRpcResult(envelope: RpcEnvelope, serializer: KSerializer<T>): T {
        ensureNoRpcError(envelope)
        val result = envelope.result ?: throw RpcException(-1, "Missing RPC result")
        return json.decodeFromJsonElement(serializer, result)
    }

    private fun <T> decodeOptionalRpcResult(
            envelope: RpcEnvelope,
            serializer: KSerializer<T>,
            defaultValue: T
    ): T {
        ensureNoRpcError(envelope)
        val result = envelope.result ?: return defaultValue
        return json.decodeFromJsonElement(serializer, result)
    }
}

@Serializable
private data class RpcEnvelope(
        val jsonrpc: String? = null,
        val id: JsonElement? = null,
        val result: JsonElement? = null,
        val error: RpcErrorPayload? = null
)

@Serializable
private data class RpcErrorPayload(
        val code: Int? = null,
        val message: String? = null
)

@Serializable
private data class RpcVersionResult(val version: String = "")

@Serializable
private data class RecentStatusResponse(val data: List<RecentStatusItemDto> = emptyList())

/** RPC 调用异常 */
class RpcException(val code: Int, override val message: String) :
        Exception("RPC Error [$code]: $message")
