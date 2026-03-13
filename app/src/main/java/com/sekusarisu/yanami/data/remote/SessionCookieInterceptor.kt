package com.sekusarisu.yanami.data.remote

import android.util.Log
import com.sekusarisu.yanami.domain.model.AuthType
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

        val sessionToken = sessionManager.getSessionToken()
        val authType = sessionManager.getAuthType() ?: AuthType.PASSWORD

        return if (!sessionToken.isNullOrBlank()) {
            val newRequest =
                    when (authType) {
                        AuthType.API_KEY -> {
                            Log.d(
                                    TAG,
                                    "Injecting Bearer token for ${originalRequest.url.host}${originalRequest.url.encodedPath}"
                            )
                            originalRequest
                                    .newBuilder()
                                    .header("Authorization", "Bearer $sessionToken")
                                    .build()
                        }
                        AuthType.PASSWORD -> {
                            // 构造 Cookie header，保留原有 Cookie（如果有的话）
                            val existingCookie = originalRequest.header("Cookie")
                            val sessionCookie = "session_token=$sessionToken"
                            val fullCookie =
                                    if (existingCookie.isNullOrBlank()) sessionCookie
                                    else "$existingCookie; $sessionCookie"

                            Log.d(
                                    TAG,
                                    "Injecting session cookie for ${originalRequest.url.host}${originalRequest.url.encodedPath}"
                            )
                            originalRequest
                                    .newBuilder()
                                    .header("Cookie", fullCookie)
                                    .build()
                        }
                        AuthType.GUEST -> {
                            originalRequest
                        }
                    }
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
