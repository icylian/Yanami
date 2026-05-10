package com.sekusarisu.yanami.data.remote

import android.util.Log
import com.sekusarisu.yanami.domain.model.AuthType
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor — 自动注入认证头
 *
 * 根据认证类型注入不同的认证信息：
 * - PASSWORD 模式 → Cookie: session_token=xxx
 * - API_KEY 模式 → Authorization: Bearer xxx
 */
class SessionCookieInterceptor(private val sessionManager: SessionManager) : Interceptor {

    companion object {
        private const val TAG = "SessionCookie"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val skipSessionInterceptor =
                originalRequest.header(SKIP_SESSION_INTERCEPTOR_HEADER) != null

        val shouldInject = shouldInjectForRequest(originalRequest.url, sessionManager.getBaseUrl())
        val sessionToken = if (shouldInject) sessionManager.getSessionToken() else null
        val authType = sessionManager.getAuthType() ?: AuthType.PASSWORD
        val customHeaders = if (shouldInject) sessionManager.getCustomHeaders() else emptyList()

        var builder =
                if (skipSessionInterceptor) {
                    originalRequest.newBuilder().removeHeader(SKIP_SESSION_INTERCEPTOR_HEADER)
                } else {
                    originalRequest.newBuilder()
                }

        if (skipSessionInterceptor) {
            return chain.proceed(builder.build())
        }

        var changed = false

        customHeaders.forEach { customHeader ->
            val name = customHeader.name.trim()
            val value = customHeader.value.trim()
            if (name.isNotEmpty() && value.isNotEmpty()) {
                builder = builder.header(name, value)
                changed = true
            }
        }

        if (!sessionToken.isNullOrBlank()) {
            builder =
                    when (authType) {
                        AuthType.API_KEY -> {
                            val authHeader = buildAuthHeader(sessionToken, authType)
                            if (authHeader == null) return chain.proceed(originalRequest)
                            Log.d(
                                    TAG,
                                    "Injecting Bearer token for ${originalRequest.url.host}${originalRequest.url.encodedPath}"
                            )
                            builder.header(authHeader.name, authHeader.value)
                        }
                        AuthType.PASSWORD -> {
                            // 构造 Cookie header，保留原有 Cookie（如果有的话）
                            val existingCookie = originalRequest.header("Cookie")
                            val sessionCookie = buildSessionCookie(sessionToken)
                            val fullCookie =
                                    if (existingCookie.isNullOrBlank()) sessionCookie
                                    else "$existingCookie; $sessionCookie"

                            Log.d(
                                    TAG,
                                    "Injecting session cookie for ${originalRequest.url.host}${originalRequest.url.encodedPath}"
                            )
                            builder.header("Cookie", fullCookie)
                        }
                        AuthType.GUEST -> {
                            builder
                        }
                    }
            changed = true
        }

        return chain.proceed(if (changed) builder.build() else originalRequest)
    }

    private fun shouldInjectForRequest(requestUrl: HttpUrl, activeBaseUrl: String?): Boolean {
        val activeUrl = activeBaseUrl?.trim()?.trimEnd('/')?.toHttpUrlOrNull() ?: return false
        if (!requestUrl.scheme.equals(activeUrl.scheme, ignoreCase = true)) return false
        if (!requestUrl.host.equals(activeUrl.host, ignoreCase = true)) return false
        if (requestUrl.port != activeUrl.port) return false

        val activePath = activeUrl.encodedPath.trimEnd('/')
        return activePath.isBlank() ||
                requestUrl.encodedPath == activePath ||
                requestUrl.encodedPath.startsWith("$activePath/")
    }
}
