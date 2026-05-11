package com.sekusarisu.yanami.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfoDto(
        val versionCode: Int,
        val versionName: String,
        val downloadUrl: String,
        val changelog: String
)
