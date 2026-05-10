package com.sekusarisu.yanami.data.remote

import android.util.Log
import com.sekusarisu.yanami.BuildConfig
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.CustomHeader
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Komari 认证服务
 *
 * 负责：
 * - POST /api/login：用户名 + 密码 + 可选 2FA 码 → 获取 session_token
 * - 通过 RPC common:getMe 验证 session_token 有效性
 */
class KomariAuthService(private val httpClient: HttpClient) {

    companion object {
        private const val TAG = "KomariAuth"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 登录
     *
     * POST /api/login 请求体: { "username": "admin", "password": "password", "2fa_code": "123456" }
     * 成功时服务端通过 Set-Cookie 返回 session_token
     */
    suspend fun login(
            baseUrl: String,
            username: String,
            password: String,
            twoFaCode: String? = null,
            customHeaders: List<CustomHeader> = emptyList()
    ): LoginResult {
        val url = baseUrl.trimEnd('/') + "/api/login"

        val requestBody = buildJsonObject {
            put("username", username)
            put("password", password)
            if (!twoFaCode.isNullOrBlank()) {
                put("2fa_code", twoFaCode)
            }
        }

        return try {
            val response =
                    httpClient.post(url) {
                        contentType(ContentType.Application.Json)
                        skipSessionInterceptor()
                        applyCustomHeaders(customHeaders)
                        header("User-Agent", "Yanami/${BuildConfig.VERSION_NAME}")
                        setBody(requestBody.toString())
                    }

            // 从 Set-Cookie 头提取 session_token
            val setCookieHeaders = response.headers.getAll("Set-Cookie") ?: emptyList()
            val sessionToken = extractSessionToken(setCookieHeaders)

            if (sessionToken != null) {
                Log.d(TAG, "Login successful, got session_token")
                LoginResult.Success(sessionToken)
            } else {
                // 尝试解析响应体获取更多信息
                val responseText = response.bodyAsText()
                Log.w(TAG, "Login response (no token): $responseText")

                // 检查是否需要 2FA
                if (responseText.contains("2fa", ignoreCase = true) ||
                                responseText.contains("two-factor", ignoreCase = true) ||
                                responseText.contains("totp", ignoreCase = true)
                ) {
                    LoginResult.Requires2FA("需要输入两步验证码")
                } else {
                    LoginResult.Error("登录失败: $responseText")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            LoginResult.Error("登录失败: ${e.message}")
        }
    }

    /**
     * 验证 API Key 是否有效
     *
     * 通过 HTTP POST RPC2 调用 common:getVersion，使用 Bearer 认证头
     * @return true = API Key 有效
     */
    suspend fun validateApiKey(
            baseUrl: String,
            apiKey: String,
            customHeaders: List<CustomHeader> = emptyList()
    ): Boolean {
        return try {
            val url = baseUrl.trimEnd('/') + "/api/rpc2"
            val rpcRequest = buildJsonObject {
                put("jsonrpc", "2.0")
                put("method", "common:getVersion")
                put("id", 1)
            }

            val response =
                    httpClient.post(url) {
                        contentType(ContentType.Application.Json)
                        skipSessionInterceptor()
                        applyCustomHeaders(customHeaders)
                        applyAuth(apiKey, AuthType.API_KEY)
                        header("User-Agent", "Yanami/${BuildConfig.VERSION_NAME}")
                        setBody(rpcRequest.toString())
                    }

            val responseText = response.bodyAsText()
            val parsed = json.parseToJsonElement(responseText).jsonObject
            val result = parsed["result"]?.jsonObject
            val version = result?.get("version")?.jsonPrimitive?.content

            val isValid = !version.isNullOrBlank()
            Log.d(TAG, "API Key validation: version=$version, valid=$isValid")
            isValid
        } catch (e: Exception) {
            Log.w(TAG, "API Key validation error: ${e.message}")
            false
        }
    }

    /**
     * 验证 session_token 是否有效
     *
     * 通过 HTTP POST RPC2 调用 common:getMe，检查返回的 logged_in 是否为 true
     */
    suspend fun validateSession(
            baseUrl: String,
            sessionToken: String,
            customHeaders: List<CustomHeader> = emptyList()
    ): Boolean {
        return try {
            val url = baseUrl.trimEnd('/') + "/api/rpc2"
            val rpcRequest = buildJsonObject {
                put("jsonrpc", "2.0")
                put("method", "common:getMe")
                put("id", 1)
            }

            val response =
                    httpClient.post(url) {
                        contentType(ContentType.Application.Json)
                        skipSessionInterceptor()
                        applyCustomHeaders(customHeaders)
                        applyAuth(sessionToken, AuthType.PASSWORD)
                        setBody(rpcRequest.toString())
                    }

            val responseText = response.bodyAsText()
            val parsed = json.parseToJsonElement(responseText).jsonObject
            val result = parsed["result"]?.jsonObject

            // 检查 logged_in 字段
            val loggedIn = result?.get("logged_in")?.jsonPrimitive?.content?.toBooleanStrictOrNull()
            val username = result?.get("username")?.jsonPrimitive?.content

            val isValid = loggedIn == true && username != "Guest"
            Log.d(TAG, "Session validation: loggedIn=$loggedIn, username=$username, valid=$isValid")
            isValid
        } catch (e: Exception) {
            Log.w(TAG, "Session validation error: ${e.message}")
            false
        }
    }

    /**
     * 登出
     *
     * 调用 GET /api/logout 使服务端 session 失效
     */
    suspend fun logout(
            baseUrl: String,
            sessionToken: String,
            customHeaders: List<CustomHeader> = emptyList()
    ) {
        try {
            val url = baseUrl.trimEnd('/') + "/api/logout"
            httpClient.post(url) {
                skipSessionInterceptor()
                applyCustomHeaders(customHeaders)
                applyAuth(sessionToken, AuthType.PASSWORD)
            }
            Log.d(TAG, "Logout successful")
        } catch (e: Exception) {
            Log.w(TAG, "Logout error: ${e.message}")
        }
    }

    // ─── 内部方法 ───

    /**
     * 从 Set-Cookie 头列表中提取 session_token 的值
     *
     * Set-Cookie 格式示例: session_token=abc123; Path=/; HttpOnly
     */
    private fun extractSessionToken(setCookieHeaders: List<String>): String? {
        for (header in setCookieHeaders) {
            val parts = header.split(";").map { it.trim() }
            for (part in parts) {
                if (part.startsWith("session_token=")) {
                    val token = part.removePrefix("session_token=")
                    if (token.isNotBlank()) return token
                }
            }
        }
        return null
    }
}

/** 登录结果 */
sealed class LoginResult {
    data class Success(val sessionToken: String) : LoginResult()
    data class Requires2FA(val message: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}
