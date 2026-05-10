package com.sekusarisu.yanami.data.remote

import android.util.Log
import com.sekusarisu.yanami.data.remote.dto.UpdateInfoDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class UpdateCheckService {

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    companion object {
        private const val TAG = "UpdateCheckService"
        private const val UPDATE_URL =
                "https://raw.githubusercontent.com/icylian/Yanami/main/update.json"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _latestUpdate = MutableStateFlow<UpdateInfoDto?>(null)
    val latestUpdate: StateFlow<UpdateInfoDto?> = _latestUpdate.asStateFlow()

    suspend fun checkForUpdate(): UpdateInfoDto? {
        return try {
            val response = httpClient.get(UPDATE_URL)
            val body = response.bodyAsText()
            json.decodeFromString<UpdateInfoDto>(body)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for update", e)
            null
        }
    }

    suspend fun checkForUpdateSilent(currentVersionCode: Int) {
        val info = checkForUpdate()
        if (info != null && info.versionCode > currentVersionCode) {
            _latestUpdate.value = info
        }
    }
}
