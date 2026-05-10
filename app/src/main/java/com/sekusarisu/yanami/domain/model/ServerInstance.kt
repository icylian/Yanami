package com.sekusarisu.yanami.domain.model

/**
 * 领域模型 — 服务端实例
 *
 * 与 Entity 的区别：此处的 username/password 是解密后的明文，仅在内存中使用。 sessionToken 可为 null（首次登录前或 token 已清除时）。
 */
data class ServerInstance(
        val id: Long = 0,
        val name: String,
        val baseUrl: String,
        val username: String,
        val password: String,
        val sessionToken: String? = null,
        val requires2fa: Boolean = false,
        val isActive: Boolean = false,
        val createdAt: Long = System.currentTimeMillis(),
        val authType: AuthType = AuthType.PASSWORD,
        val apiKey: String? = null,
        val customHeaders: List<CustomHeader> = emptyList()
)
