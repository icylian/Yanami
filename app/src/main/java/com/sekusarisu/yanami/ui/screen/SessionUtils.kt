package com.sekusarisu.yanami.ui.screen

import com.sekusarisu.yanami.data.remote.AdminApiException
import com.sekusarisu.yanami.data.remote.RpcException
import com.sekusarisu.yanami.domain.repository.Requires2FAException
import com.sekusarisu.yanami.domain.repository.SessionExpiredException

fun Throwable.isSessionAuthError(): Boolean {
    if (this is SessionExpiredException || this is Requires2FAException) return true
    if (this is RpcException && (code == 401 || code == 403)) return true
    if (this is AdminApiException && (statusCode == 401 || statusCode == 403)) return true
    val msg = (message ?: "").lowercase()
    return msg.contains("session") ||
            msg.contains("not logged") ||
            msg.contains("unauthorized") ||
            msg.contains("forbidden") ||
            msg.contains("401") ||
            msg.contains("403") ||
            msg.contains("2fa") ||
            msg.contains("two-factor") ||
            msg.contains("totp") ||
            msg.contains("重新登录")
}

fun Throwable.isTwoFaHint(): Boolean {
    val msg = (message ?: "").lowercase()
    return msg.contains("2fa") ||
            msg.contains("two-factor") ||
            msg.contains("totp") ||
            msg.contains("两步")
}
