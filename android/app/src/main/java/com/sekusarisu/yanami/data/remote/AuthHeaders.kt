package com.sekusarisu.yanami.data.remote

import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.CustomHeader
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header

internal const val SESSION_COOKIE_NAME = "session_token"
internal const val SKIP_SESSION_INTERCEPTOR_HEADER = "X-Yanami-Skip-Session-Interceptor"

internal data class AuthHeader(val name: String, val value: String)

internal fun buildAuthHeader(sessionToken: String, authType: AuthType): AuthHeader? {
    if (sessionToken.isBlank()) return null
    return when (authType) {
        AuthType.API_KEY -> AuthHeader("Authorization", "Bearer $sessionToken")
        AuthType.PASSWORD -> AuthHeader("Cookie", buildSessionCookie(sessionToken))
        AuthType.GUEST -> null
    }
}

internal fun buildSessionCookie(sessionToken: String): String =
        "$SESSION_COOKIE_NAME=$sessionToken"

internal fun HttpRequestBuilder.applyAuth(sessionToken: String, authType: AuthType) {
    val authHeader = buildAuthHeader(sessionToken, authType) ?: return
    header(authHeader.name, authHeader.value)
}

internal fun HttpRequestBuilder.applyCustomHeaders(customHeaders: List<CustomHeader>) {
    customHeaders.forEach { customHeader ->
        val name = customHeader.name.trim()
        val value = customHeader.value.trim()
        if (name.isNotEmpty() && value.isNotEmpty()) {
            header(name, value)
        }
    }
}

internal fun HttpRequestBuilder.skipSessionInterceptor() {
    header(SKIP_SESSION_INTERCEPTOR_HEADER, "1")
}
