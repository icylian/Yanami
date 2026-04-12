package com.sekusarisu.yanami.data.remote

import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.data.remote.dto.AdminEnvelopeDto
import com.sekusarisu.yanami.data.remote.dto.ClientCreateResultDto
import com.sekusarisu.yanami.data.remote.dto.ClientTokenDto
import com.sekusarisu.yanami.data.remote.dto.ManagedClientDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class KomariAdminClientService(private val httpClient: HttpClient) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun listClients(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType
    ): List<ManagedClientDto> {
        val response =
                httpClient.get(baseUrl.trimEnd('/') + "/api/admin/client/list") {
                    applyAdminAuth(sessionToken, authType)
                }
        return parseData(response, ListSerializer(ManagedClientDto.serializer()))
    }

    suspend fun getClient(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            uuid: String
    ): ManagedClientDto {
        val response =
                httpClient.get(baseUrl.trimEnd('/') + "/api/admin/client/$uuid") {
                    applyAdminAuth(sessionToken, authType)
                }
        return parseData(response, ManagedClientDto.serializer())
    }

    suspend fun addClient(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            name: String?
    ): ClientCreateResultDto {
        val requestBody =
                buildJsonObject {
                    if (!name.isNullOrBlank()) {
                        put("name", name.trim())
                    }
                }
        val response =
                httpClient.post(baseUrl.trimEnd('/') + "/api/admin/client/add") {
                    applyAdminAuth(sessionToken, authType)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toString())
                }
        return parseData(response, ClientCreateResultDto.serializer())
    }

    suspend fun updateClient(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            uuid: String,
            payload: JsonObject
    ) {
        val response =
                httpClient.post(baseUrl.trimEnd('/') + "/api/admin/client/$uuid/edit") {
                    applyAdminAuth(sessionToken, authType)
                    contentType(ContentType.Application.Json)
                    setBody(payload.toString())
                }
        parseNoContent(response)
    }

    suspend fun deleteClient(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            uuid: String
    ) {
        val response =
                httpClient.post(baseUrl.trimEnd('/') + "/api/admin/client/$uuid/remove") {
                    applyAdminAuth(sessionToken, authType)
                }
        parseNoContent(response)
    }

    suspend fun getClientToken(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            uuid: String
    ): String {
        val response =
                httpClient.get(baseUrl.trimEnd('/') + "/api/admin/client/$uuid/token") {
                    applyAdminAuth(sessionToken, authType)
                }
        return parseData(response, ClientTokenDto.serializer()).token
    }

    suspend fun reorderClients(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            weights: Map<String, Int>
    ) {
        val payload =
                buildJsonObject {
                    weights.forEach { (uuid, weight) -> put(uuid, weight) }
                }
        val response =
                httpClient.post(baseUrl.trimEnd('/') + "/api/admin/client/order") {
                    applyAdminAuth(sessionToken, authType)
                    contentType(ContentType.Application.Json)
                    setBody(payload.toString())
                }
        parseNoContent(response)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAdminAuth(
            sessionToken: String,
            authType: AuthType
    ) {
        when (authType) {
            AuthType.API_KEY -> header("Authorization", "Bearer $sessionToken")
            AuthType.PASSWORD -> header("Cookie", "session_token=$sessionToken")
            AuthType.GUEST -> throw IllegalStateException("游客模式不支持 Client 管理")
        }
    }

    private suspend fun <T> parseData(response: HttpResponse, serializer: KSerializer<T>): T {
        val responseText = response.bodyAsText()
        val rawResult = runCatching { json.decodeFromString(serializer, responseText) }.getOrNull()
        if (rawResult != null && response.status.value in 200..299) {
            return rawResult
        }
        val envelope = parseEnvelope(response.status.value, responseText)
        val data = envelope.data ?: throw AdminApiException(response.status.value, "响应缺少 data")
        return json.decodeFromJsonElement(serializer, data)
    }

    private suspend fun parseNoContent(response: HttpResponse) {
        val responseText = response.bodyAsText()
        if (response.status.value in 200..299 && responseText.isBlank()) {
            return
        }
        parseEnvelope(response.status.value, responseText)
    }

    private fun parseEnvelope(statusCode: Int, responseText: String): AdminEnvelopeDto {
        val envelope =
                runCatching { json.decodeFromString(AdminEnvelopeDto.serializer(), responseText) }
                        .getOrNull()

        if (envelope == null) {
            val fallbackMessage =
                    responseText.ifBlank { "HTTP $statusCode" }.take(200)
            throw AdminApiException(statusCode, fallbackMessage)
        }

        if (statusCode !in 200..299 || envelope.status.equals("error", ignoreCase = true)) {
            throw AdminApiException(
                    statusCode = statusCode,
                    message = envelope.message.ifBlank { "HTTP $statusCode" }
            )
        }

        return envelope
    }
}

class AdminApiException(val statusCode: Int, override val message: String) : Exception(message)
