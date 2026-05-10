package com.sekusarisu.yanami.data.repository

import android.util.Log
import com.sekusarisu.yanami.data.local.crypto.CryptoManager
import com.sekusarisu.yanami.data.local.dao.ServerInstanceDao
import com.sekusarisu.yanami.data.local.entity.ServerInstanceEntity
import com.sekusarisu.yanami.data.remote.KomariAuthService
import com.sekusarisu.yanami.data.remote.KomariRpcService
import com.sekusarisu.yanami.data.remote.LoginResult
import com.sekusarisu.yanami.data.remote.SessionManager
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.CustomHeader
import com.sekusarisu.yanami.domain.model.ServerInstance
import com.sekusarisu.yanami.domain.repository.Requires2FAException
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.domain.repository.SessionExpiredException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 服务端实例仓库实现
 *
 * 负责：
 * - Entity ↔ Domain Model 转换
 * - 用户名/密码加解密
 * - session_token 持久化与恢复
 * - 通过 KomariAuthService 登录获取 session_token
 * - 通过 KomariRpcService 验证连接
 */
class ServerRepositoryImpl(
        private val dao: ServerInstanceDao,
        private val cryptoManager: CryptoManager,
        private val authService: KomariAuthService,
        private val rpcService: KomariRpcService,
        private val sessionManager: SessionManager
) : ServerRepository {

    companion object {
        private const val TAG = "ServerRepo"
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAll(): List<ServerInstance> {
        return dao.getAll().map { it.toDomain() }
    }

    override fun getAllFlow(): Flow<List<ServerInstance>> {
        return dao.getAllFlow().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getActive(): ServerInstance? {
        return dao.getActive()?.toDomain()
    }

    override fun getActiveFlow(): Flow<ServerInstance?> {
        return dao.getActiveFlow().map { it?.toDomain() }
    }

    override suspend fun getById(id: Long): ServerInstance? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun add(instance: ServerInstance): Long {
        val entity = instance.toEntity()
        return dao.insert(entity)
    }

    override suspend fun update(instance: ServerInstance) {
        dao.update(instance.toEntity())
    }

    override suspend fun remove(id: Long) {
        val entity = dao.getById(id) ?: return
        dao.delete(entity)
    }

    override suspend fun setActive(id: Long) {
        dao.deactivateAll()
        dao.activateById(id)
    }

    override suspend fun testConnection(
            baseUrl: String,
            username: String,
            password: String,
            twoFaCode: String?,
            customHeaders: List<CustomHeader>
    ): String {
        // 1. 登录获取 session_token
        val loginResult = authService.login(baseUrl, username, password, twoFaCode, customHeaders)
        val sessionToken =
                when (loginResult) {
                    is LoginResult.Success -> loginResult.sessionToken
                    is LoginResult.Requires2FA -> throw Requires2FAException(loginResult.message)
                    is LoginResult.Error -> throw Exception(loginResult.message)
                }

        // 2. 用 session_token 调用 RPC 获取版本号验证
        val version =
                rpcService.getVersion(
                        baseUrl,
                        sessionToken,
                        customHeaders = customHeaders
                )
        Log.d(TAG, "Test connection ok, version=$version")
        return version
    }

    override suspend fun testConnectionWithApiKey(
            baseUrl: String,
            apiKey: String,
            customHeaders: List<CustomHeader>
    ): String {
        // 使用 Bearer 认证调用 getVersion 验证 API Key
        val version =
                rpcService.getVersion(
                        baseUrl,
                        apiKey,
                        AuthType.API_KEY,
                        customHeaders = customHeaders
                )
        Log.d(TAG, "Test connection with API Key ok, version=$version")
        return version
    }

    override suspend fun testConnectionAsGuest(
            baseUrl: String,
            customHeaders: List<CustomHeader>
    ): String {
        val version =
                rpcService.getVersion(
                        baseUrl,
                        "",
                        AuthType.GUEST,
                        customHeaders = customHeaders
                )
        Log.d(TAG, "Test connection as guest ok, version=$version")
        return version
    }

    override suspend fun login(instance: ServerInstance, twoFaCode: String?): Boolean {
        // GUEST 模式：直接设置空 session，无需登录
        if (instance.authType == AuthType.GUEST) {
            sessionManager.setSession(
                    instance.id,
                    instance.baseUrl,
                    "",
                    AuthType.GUEST,
                    instance.customHeaders
            )
            Log.d(TAG, "Guest session set for server ${instance.name}")
            return true
        }

        // API_KEY 模式：直接设置 session，无需登录
        if (instance.authType == AuthType.API_KEY) {
            val apiKey = instance.apiKey
                    ?: throw Exception("API Key is missing")
            sessionManager.setSession(
                    instance.id,
                    instance.baseUrl,
                    apiKey,
                    AuthType.API_KEY,
                    instance.customHeaders
            )
            Log.d(TAG, "API Key session set for server ${instance.name}")
            return true
        }

        val loginResult =
                authService.login(
                        instance.baseUrl,
                        instance.username,
                        instance.password,
                        twoFaCode,
                        instance.customHeaders
                )

        return when (loginResult) {
            is LoginResult.Success -> {
                // 更新内存 session
                sessionManager.setSession(
                        instance.id,
                        instance.baseUrl,
                        loginResult.sessionToken,
                        customHeaders = instance.customHeaders
                )

                // 持久化 session_token（加密）
                val encryptedToken = cryptoManager.encrypt(loginResult.sessionToken)
                dao.updateSessionToken(instance.id, encryptedToken)

                Log.d(TAG, "Login successful for server ${instance.name}")
                true
            }
            is LoginResult.Requires2FA -> {
                throw Requires2FAException(loginResult.message)
            }
            is LoginResult.Error -> {
                throw Exception(loginResult.message)
            }
        }
    }

    override suspend fun ensureSessionToken(instance: ServerInstance): String {
        // GUEST 模式：直接返回空 token
        if (instance.authType == AuthType.GUEST) {
            sessionManager.setSession(
                    instance.id,
                    instance.baseUrl,
                    "",
                    AuthType.GUEST,
                    instance.customHeaders
            )
            return ""
        }

        // API_KEY 模式：直接使用 API Key，无需恢复/验证 session
        if (instance.authType == AuthType.API_KEY) {
            val apiKey = instance.apiKey
                    ?: throw SessionExpiredException("API Key is missing")
            sessionManager.setSession(
                    instance.id,
                    instance.baseUrl,
                    apiKey,
                    AuthType.API_KEY,
                    instance.customHeaders
            )
            return apiKey
        }

        val restored = restoreSession(instance)
        if (!restored) {
            login(instance) // 成功时内部已更新 SessionManager；失败时抛出 Requires2FAException 或其他异常
        }
        return sessionManager.getSessionToken()
                ?: throw SessionExpiredException()
    }

    override suspend fun restoreSession(instance: ServerInstance): Boolean {
        // GUEST 模式：直接设置空 session
        if (instance.authType == AuthType.GUEST) {
            sessionManager.setSession(
                    instance.id,
                    instance.baseUrl,
                    "",
                    AuthType.GUEST,
                    instance.customHeaders
            )
            Log.d(TAG, "Guest session restored for server ${instance.name}")
            return true
        }

        // API_KEY 模式：直接从存储的 apiKey 设置 session
        if (instance.authType == AuthType.API_KEY) {
            val apiKey = instance.apiKey
            if (apiKey.isNullOrBlank()) {
                Log.d(TAG, "No API Key for server ${instance.name}")
                sessionManager.clearSession()
                return false
            }
            sessionManager.setSession(
                    instance.id,
                    instance.baseUrl,
                    apiKey,
                    AuthType.API_KEY,
                    instance.customHeaders
            )
            Log.d(TAG, "API Key session restored for server ${instance.name}")
            return true
        }

        val cachedToken = instance.sessionToken
        if (cachedToken.isNullOrBlank()) {
            Log.d(TAG, "No cached session_token for server ${instance.name}")
            sessionManager.clearSession()
            return false
        }

        // 验证 token 有效性
        val isValid =
                authService.validateSession(
                        instance.baseUrl,
                        cachedToken,
                        instance.customHeaders
                )
        if (isValid) {
            sessionManager.setSession(
                    instance.id,
                    instance.baseUrl,
                    cachedToken,
                    customHeaders = instance.customHeaders
            )
            Log.d(TAG, "Session restored for server ${instance.name}")
            return true
        }

        // Token 已失效，清除缓存
        Log.d(TAG, "Cached session_token expired for server ${instance.name}")
        sessionManager.clearSession()
        dao.updateSessionToken(instance.id, null)
        return false
    }

    override suspend fun updateRequires2fa(id: Long, requires2fa: Boolean) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(requires2fa = requires2fa))
    }

    override suspend fun updateAuthInfo(
            id: Long,
            username: String,
            password: String,
            requires2fa: Boolean
    ) {
        val entity = dao.getById(id) ?: return
        dao.update(
                entity.copy(
                        encryptedUsername = cryptoManager.encrypt(username),
                        encryptedPassword = cryptoManager.encrypt(password),
                        requires2fa = requires2fa
                )
        )
    }

    // ─── Entity ↔ Domain 转换 ───

    private fun ServerInstanceEntity.toDomain(): ServerInstance {
        return ServerInstance(
                id = id,
                name = name,
                baseUrl = baseUrl,
                username =
                        try {
                            cryptoManager.decrypt(encryptedUsername)
                        } catch (e: Exception) {
                            ""
                        },
                password =
                        try {
                            cryptoManager.decrypt(encryptedPassword)
                        } catch (e: Exception) {
                            ""
                        },
                sessionToken =
                        encryptedSessionToken?.let {
                            try {
                                cryptoManager.decrypt(it)
                            } catch (e: Exception) {
                                null
                            }
                        },
                requires2fa = requires2fa,
                isActive = isActive,
                createdAt = createdAt,
                authType =
                        try {
                            AuthType.valueOf(authType)
                        } catch (e: Exception) {
                            AuthType.PASSWORD
                        },
                apiKey =
                        encryptedApiKey?.let {
                            try {
                                cryptoManager.decrypt(it)
                            } catch (e: Exception) {
                                null
                            }
                        },
                customHeaders = decryptCustomHeaders(encryptedCustomHeaders)
        )
    }

    private fun ServerInstance.toEntity(): ServerInstanceEntity {
        return ServerInstanceEntity(
                id = if (id == 0L) 0 else id,
                name = name,
                baseUrl = baseUrl,
                encryptedUsername = cryptoManager.encrypt(username),
                encryptedPassword = cryptoManager.encrypt(password),
                encryptedSessionToken =
                        sessionToken?.let {
                            try {
                                cryptoManager.encrypt(it)
                            } catch (e: Exception) {
                                null
                            }
                        },
                requires2fa = requires2fa,
                isActive = isActive,
                createdAt = createdAt,
                authType = authType.name,
                encryptedApiKey =
                        apiKey?.let {
                            try {
                                cryptoManager.encrypt(it)
                            } catch (e: Exception) {
                                null
                            }
                        },
                encryptedCustomHeaders = encryptCustomHeaders(customHeaders)
        )
    }

    private fun decryptCustomHeaders(encryptedCustomHeaders: String?): List<CustomHeader> {
        if (encryptedCustomHeaders.isNullOrBlank()) return emptyList()
        return try {
            val rawJson = cryptoManager.decrypt(encryptedCustomHeaders)
            json.decodeFromString(ListSerializer(CustomHeader.serializer()), rawJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun encryptCustomHeaders(customHeaders: List<CustomHeader>): String? {
        val sanitized =
                customHeaders
                        .map { CustomHeader(it.name.trim(), it.value.trim()) }
                        .filter { it.name.isNotBlank() && it.value.isNotBlank() }
        if (sanitized.isEmpty()) return null
        return try {
            cryptoManager.encrypt(
                    json.encodeToString(
                            ListSerializer(CustomHeader.serializer()),
                            sanitized
                    )
            )
        } catch (e: Exception) {
            null
        }
    }
}
