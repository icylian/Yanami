package com.sekusarisu.yanami.data.remote

import com.sekusarisu.yanami.data.remote.dto.AdminEnvelopeDto
import com.sekusarisu.yanami.data.remote.dto.LoadNotificationCreateResultDto
import com.sekusarisu.yanami.data.remote.dto.LoadNotificationDto
import com.sekusarisu.yanami.data.remote.dto.OfflineNotificationDto
import com.sekusarisu.yanami.domain.model.AuthType
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

class KomariAdminNotificationService(private val httpClient: HttpClient) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun listOfflineNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType
    ): List<OfflineNotificationDto> {
        val response =
            httpClient.get(baseUrl.trimEnd('/') + "/api/admin/notification/offline") {
                applyAdminAuth(sessionToken, authType)
            }
        return parseData(response, ListSerializer(OfflineNotificationDto.serializer()))
    }

    suspend fun updateOfflineNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        payload: JsonArray
    ) {
        val response =
            httpClient.post(baseUrl.trimEnd('/') + "/api/admin/notification/offline/edit") {
                applyAdminAuth(sessionToken, authType)
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
        parseNoContent(response)
    }

    suspend fun setOfflineNotificationsEnabled(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        uuids: List<String>,
        enabled: Boolean
    ) {
        val payload =
            JsonArray(uuids.map { uuid -> kotlinx.serialization.json.JsonPrimitive(uuid) })
        val path =
            if (enabled) {
                "/api/admin/notification/offline/enable"
            } else {
                "/api/admin/notification/offline/disable"
            }
        val response =
            httpClient.post(baseUrl.trimEnd('/') + path) {
                applyAdminAuth(sessionToken, authType)
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
        parseNoContent(response)
    }

    suspend fun listLoadNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType
    ): List<LoadNotificationDto> {
        val response =
            httpClient.get(baseUrl.trimEnd('/') + "/api/admin/notification/load/") {
                applyAdminAuth(sessionToken, authType)
            }
        return parseData(response, ListSerializer(LoadNotificationDto.serializer()))
    }

    suspend fun addLoadNotification(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        payload: JsonObject
    ): Int {
        val response =
            httpClient.post(baseUrl.trimEnd('/') + "/api/admin/notification/load/add") {
                applyAdminAuth(sessionToken, authType)
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
        return parseData(response, LoadNotificationCreateResultDto.serializer()).taskId
    }

    suspend fun updateLoadNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        tasks: List<JsonObject>
    ) {
        val payload =
            buildJsonObject {
                putJsonArray("notifications") {
                    tasks.forEach { task -> add(task) }
                }
            }
        val response =
            httpClient.post(baseUrl.trimEnd('/') + "/api/admin/notification/load/edit") {
                applyAdminAuth(sessionToken, authType)
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
        parseNoContent(response)
    }

    suspend fun deleteLoadNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        ids: List<Int>
    ) {
        val payload =
            buildJsonObject {
                putJsonArray("id") {
                    ids.forEach { id -> add(id) }
                }
            }
        val response =
            httpClient.post(baseUrl.trimEnd('/') + "/api/admin/notification/load/delete") {
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
        if (authType == AuthType.GUEST) {
            throw IllegalStateException("游客模式不支持通知管理")
        }
        applyAuth(sessionToken, authType)
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
            val fallbackMessage = responseText.ifBlank { "HTTP $statusCode" }.take(200)
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
