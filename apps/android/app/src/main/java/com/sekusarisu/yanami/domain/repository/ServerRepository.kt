package com.sekusarisu.yanami.domain.repository

import com.sekusarisu.yanami.domain.model.CustomHeader
import com.sekusarisu.yanami.domain.model.ServerInstance
import kotlinx.coroutines.flow.Flow

/** 服务端实例仓库接口 */
interface ServerRepository {

    /** 获取所有实例（一次性） */
    suspend fun getAll(): List<ServerInstance>

    /** 获取所有实例（Flow，实时更新） */
    fun getAllFlow(): Flow<List<ServerInstance>>

    /** 获取当前激活的实例 */
    suspend fun getActive(): ServerInstance?

    /** 获取当前激活的实例（Flow） */
    fun getActiveFlow(): Flow<ServerInstance?>

    /** 根据 ID 获取实例 */
    suspend fun getById(id: Long): ServerInstance?

    /**
     * 添加新实例
     *
     * @return 新实例的 ID
     */
    suspend fun add(instance: ServerInstance): Long

    /** 更新实例 */
    suspend fun update(instance: ServerInstance)

    /** 删除实例 */
    suspend fun remove(id: Long)

    /** 设置指定实例为活跃 */
    suspend fun setActive(id: Long)

    /**
     * 测试与服务端的连接（登录 + 获取版本号）
     *
     * @return 成功返回版本信息，失败抛出异常
     */
    suspend fun testConnection(
            baseUrl: String,
            username: String,
            password: String,
            twoFaCode: String? = null,
            customHeaders: List<CustomHeader> = emptyList()
    ): String

    /**
     * 使用 API Key 测试与服务端的连接
     *
     * @return 成功返回版本信息，失败抛出异常
     */
    suspend fun testConnectionWithApiKey(
            baseUrl: String,
            apiKey: String,
            customHeaders: List<CustomHeader> = emptyList()
    ): String

    /**
     * 使用游客模式测试与服务端的连接（无认证）
     *
     * @return 成功返回版本信息，失败抛出异常
     */
    suspend fun testConnectionAsGuest(
            baseUrl: String,
            customHeaders: List<CustomHeader> = emptyList()
    ): String

    /**
     * 登录到指定实例
     *
     * 成功时更新 SessionManager 并将 session_token 持久化到 DB。
     * @return 成功返回 true，需要 2FA 抛出 Requires2FAException，其他失败抛异常
     */
    suspend fun login(instance: ServerInstance, twoFaCode: String? = null): Boolean

    /**
     * 尝试恢复缓存的 session
     *
     * 从 DB 读取加密的 session_token → 验证有效性 → 更新 SessionManager。
     * @return true = 恢复成功，false = token 无效或不存在（需要重新登录）
     */
    suspend fun restoreSession(instance: ServerInstance): Boolean

    /**
     * 确保当前 session 有效，返回可用的 session_token
     *
     * 先尝试恢复缓存 token；若失效则自动重新登录。
     * 需要 2FA 时抛出 Requires2FAException，其他认证失败抛 SessionExpiredException。
     */
    suspend fun ensureSessionToken(instance: ServerInstance): String

    /** 更新实例的 requires_2fa 标记 */
    suspend fun updateRequires2fa(id: Long, requires2fa: Boolean)

    /** 更新实例认证信息（用户名/密码/2FA 开关） */
    suspend fun updateAuthInfo(id: Long, username: String, password: String, requires2fa: Boolean)
}

/** 需要 2FA 验证码异常 */
class Requires2FAException(message: String = "需要输入两步验证码") : Exception(message)

/** Session 过期异常 */
class SessionExpiredException(message: String = "Session 已过期，请重新登录") : Exception(message)
