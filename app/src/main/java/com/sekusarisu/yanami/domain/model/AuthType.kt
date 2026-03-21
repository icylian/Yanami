package com.sekusarisu.yanami.domain.model

import kotlinx.serialization.Serializable

/** 认证方式 */
@Serializable
enum class AuthType {
    /** 账号密码认证 */
    PASSWORD,
    /** API Key 认证 */
    API_KEY,
    /** 游客模式（无认证） */
    GUEST
}
