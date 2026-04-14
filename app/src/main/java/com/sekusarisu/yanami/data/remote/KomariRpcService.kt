package com.sekusarisu.yanami.data.remote

import android.util.Log
import com.sekusarisu.yanami.data.remote.dto.LoadRecordsResponseDto
import com.sekusarisu.yanami.data.remote.dto.NodeInfoDto
import com.sekusarisu.yanami.data.remote.dto.NodeStatusDto
import com.sekusarisu.yanami.data.remote.dto.PingRecordsResponseDto
import com.sekusarisu.yanami.data.remote.dto.RecentStatusItemDto
import com.sekusarisu.yanami.domain.model.AuthType
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.plugins.websocket.wss
import io.ktor.client.request.get
import io.ktor.client.request.header
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Komari RPC 服务
 *
 * 提供两种调用方式：
 * - HTTP POST `/api/rpc2`：用于一次性请求（如获取节点列表）
 * - WebSocket `/api/rpc2`：通过 HTTP Upgrade 实时流式获取节点状态
 *
 * 所有请求通过 Cookie: session_token=xxx 进行认证。
 */
class KomariRpcService(private val httpClient: HttpClient) {

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
            authType: AuthType = AuthType.PASSWORD
    ): Map<String, NodeInfoDto> {
        val responseText = callRpcHttp(baseUrl, sessionToken, "common:getNodes", authType = authType)
        val parsed = json.parseToJsonElement(responseText).jsonObject
        checkRpcError(parsed)
        val result = parsed["result"]?.jsonObject ?: return emptyMap()
        return result.mapValues { (_, value) ->
            json.decodeFromJsonElement(NodeInfoDto.serializer(), value)
        }
    }

    /**
     * 获取所有节点最新状态（HTTP POST，单次）
     * @return Map<UUID, NodeStatusDto>
     */
    suspend fun getNodesLatestStatus(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType = AuthType.PASSWORD
    ): Map<String, NodeStatusDto> {
        val responseText =
                callRpcHttp(baseUrl, sessionToken, "common:getNodesLatestStatus", authType = authType)
        val parsed = json.parseToJsonElement(responseText).jsonObject
        checkRpcError(parsed)
        val result = parsed["result"]?.jsonObject ?: return emptyMap()
        return result.mapValues { (_, value) ->
            json.decodeFromJsonElement(NodeStatusDto.serializer(), value)
        }
    }

    /**
     * 获取服务端版本（HTTP POST）
     * @return 版本号字符串
     */
    suspend fun getVersion(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType = AuthType.PASSWORD
    ): String {
        val responseText = callRpcHttp(baseUrl, sessionToken, "common:getVersion", authType = authType)
        val parsed = json.parseToJsonElement(responseText).jsonObject
        checkRpcError(parsed)
        val result = parsed["result"]?.jsonObject
        return result?.get("version")?.jsonPrimitive?.content
                ?: throw RpcException(-1, "Invalid version response")
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
            authType: AuthType = AuthType.PASSWORD
    ): List<RecentStatusItemDto> {
        val url = baseUrl.trimEnd('/') + "/api/recent/$uuid"
        val response =
                httpClient.get(url) {
                    applyAuth(sessionToken, authType)
                }
        val responseText = response.bodyAsText()
        val parsed = json.parseToJsonElement(responseText).jsonObject
        val data = parsed["data"] ?: return emptyList()
        return data.jsonArray.map {
            json.decodeFromJsonElement(RecentStatusItemDto.serializer(), it)
        }
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
            authType: AuthType = AuthType.PASSWORD
    ): Flow<KomariWsEvent> = channelFlow {
        val cleanUrl = baseUrl.trimEnd('/')
        // 提取 host、port、path
        val urlWithoutScheme = cleanUrl.removePrefix("https://").removePrefix("http://")
        val host = urlWithoutScheme.substringBefore('/').substringBefore(':')
        val portStr = urlWithoutScheme.substringBefore('/').substringAfter(':', "")
        val isSecure = cleanUrl.startsWith("https")
        val defaultPort = if (isSecure) 443 else 80
        val port = portStr.toIntOrNull() ?: defaultPort
        val pathPrefix = urlWithoutScheme.substringAfter(urlWithoutScheme.substringBefore('/'), "")
        val wsPath = "$pathPrefix/api/rpc2"

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
                                    val parsed = json.parseToJsonElement(text).jsonObject
                                    val idStr = parsed["id"]?.jsonPrimitive?.content ?: continue

                                    if (idStr == reqStatusId.toString()) {
                                        val result = parsed["result"]?.jsonObject
                                        if (result != null) {
                                            val statusMap =
                                                    result.mapValues { (_, v) ->
                                                        json.decodeFromJsonElement(
                                                                NodeStatusDto.serializer(),
                                                                v
                                                        )
                                                    }
                                            send(KomariWsEvent.Status(statusMap))
                                        }
                                        receivedCount++
                                    } else if (idStr == reqLoadId.toString()) {
                                        checkRpcError(parsed)
                                        val result = parsed["result"]
                                        if (result != null) {
                                            val dto =
                                                    json.decodeFromJsonElement(
                                                            LoadRecordsResponseDto.serializer(),
                                                            result
                                                    )
                                            send(KomariWsEvent.LoadRecords(dto))
                                        }
                                        receivedCount++
                                    } else if (idStr == reqPingId.toString()) {
                                        checkRpcError(parsed)
                                        val result = parsed["result"]
                                        if (result != null) {
                                            val dto =
                                                    json.decodeFromJsonElement(
                                                            PingRecordsResponseDto.serializer(),
                                                            result
                                                    )
                                            send(KomariWsEvent.PingRecords(dto))
                                        }
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

        // Origin header — 服务器校验此头，缺失则返回 403
        val origin = if (isSecure) "https://$host" else "http://$host"

        // 外层循环：断开后自动重连
        while (!isClosedForSend) {
            try {
                Log.d(TAG, "WebSocket connecting to $host:$port$wsPath (secure=$isSecure)")

                if (isSecure) {
                    httpClient.wss(
                            host = host,
                            port = port,
                            path = wsPath,
                            request = {
                                applyAuth(sessionToken, authType)
                                header("Origin", origin)
                            },
                            block = wsBlock
                    )
                } else {
                    httpClient.ws(
                            host = host,
                            port = port,
                            path = wsPath,
                            request = {
                                applyAuth(sessionToken, authType)
                                header("Origin", origin)
                            },
                            block = wsBlock
                    )
                }
            } catch (e: Exception) {
                if (isWsAuthException(e)) {
                    Log.w(TAG, "WebSocket auth error: ${e.message}, propagating")
                    throw e
                }
                Log.w(TAG, "WebSocket error: ${e.message}, reconnecting in 5s...")
                delay(5_000)
            }
        }
    }

    // Observe combined records handled in observeNodesLatestStatus

    // ─── 内部方法 ───

    private suspend fun callRpcHttp(
            baseUrl: String,
            sessionToken: String,
            method: String,
            params: JsonObject? = null,
            authType: AuthType = AuthType.PASSWORD
    ): String {
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
                    applyAuth(sessionToken, authType)
                    setBody(requestBody.toString())
                }

        return response.bodyAsText()
    }

    private fun isWsAuthException(e: Exception): Boolean {
        if (e is ClientRequestException) {
            val status = e.response.status.value
            return status == 401 || status == 403
        }
        val msg = (e.message ?: "").lowercase()
        return msg.contains("403") || msg.contains("401") ||
                msg.contains("forbidden") || msg.contains("unauthorized")
    }

    private fun checkRpcError(json: JsonObject) {
        val error = json["error"]
        if (error != null && error.toString() != "null") {
            val errorObj = error.jsonObject
            val message = errorObj["message"]?.jsonPrimitive?.content ?: "Unknown RPC error"
            val code = errorObj["code"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
            throw RpcException(code, message)
        }
    }
}

/** RPC 调用异常 */
class RpcException(val code: Int, override val message: String) :
        Exception("RPC Error [$code]: $message")
