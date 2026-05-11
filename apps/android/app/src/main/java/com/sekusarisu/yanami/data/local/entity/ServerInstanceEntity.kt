package com.sekusarisu.yanami.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity — 服务端实例
 *
 * 存储用户添加的 Komari 服务端连接信息。 用户名、密码、session_token 均以加密形式存储，使用 CryptoManager 进行加解密。
 */
@Entity(tableName = "server_instances")
data class ServerInstanceEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        @ColumnInfo(name = "name") val name: String,
        @ColumnInfo(name = "base_url") val baseUrl: String,
        @ColumnInfo(name = "encrypted_username") val encryptedUsername: String,
        @ColumnInfo(name = "encrypted_password") val encryptedPassword: String,
        @ColumnInfo(name = "encrypted_session_token") val encryptedSessionToken: String? = null,
        @ColumnInfo(name = "requires_2fa") val requires2fa: Boolean = false,
        @ColumnInfo(name = "is_active") val isActive: Boolean = false,
        @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
        @ColumnInfo(name = "auth_type") val authType: String = "PASSWORD",
        @ColumnInfo(name = "encrypted_api_key") val encryptedApiKey: String? = null,
        @ColumnInfo(name = "encrypted_custom_headers") val encryptedCustomHeaders: String? = null
)
