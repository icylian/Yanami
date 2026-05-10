package com.sekusarisu.yanami.data.remote

import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.CustomHeader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Session 管理器
 *
 * 维护当前活跃实例的 session_token（内存缓存）。 持久化由 ServerRepositoryImpl 负责（加密写入 Room DB）。
 */
class SessionManager {

    private val _activeSession = MutableStateFlow<ActiveSession?>(null)
    val activeSession: StateFlow<ActiveSession?> = _activeSession

    /** 设置当前活跃的 session */
    fun setSession(
            serverId: Long,
            baseUrl: String,
            sessionToken: String,
            authType: AuthType = AuthType.PASSWORD,
            customHeaders: List<CustomHeader> = emptyList()
    ) {
        _activeSession.value =
                ActiveSession(serverId, baseUrl, sessionToken, authType, customHeaders)
    }

    /** 清除当前 session（登出或 token 失效时调用） */
    fun clearSession() {
        _activeSession.value = null
    }

    /** 快捷获取当前 session_token（API_KEY 模式下存储的是 API Key） */
    fun getSessionToken(): String? = _activeSession.value?.sessionToken

    /** 快捷获取当前 baseUrl */
    fun getBaseUrl(): String? = _activeSession.value?.baseUrl

    /** 快捷获取当前活跃实例 ID */
    fun getServerId(): Long? = _activeSession.value?.serverId

    /** 快捷获取当前认证类型 */
    fun getAuthType(): AuthType? = _activeSession.value?.authType

    /** 快捷获取当前自定义请求头 */
    fun getCustomHeaders(): List<CustomHeader> = _activeSession.value?.customHeaders.orEmpty()
}

/** 活跃 session 信息 */
data class ActiveSession(
        val serverId: Long,
        val baseUrl: String,
        val sessionToken: String,
        val authType: AuthType = AuthType.PASSWORD,
        val customHeaders: List<CustomHeader> = emptyList()
)
