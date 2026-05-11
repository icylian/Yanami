package com.sekusarisu.yanami.data.backup

import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.CustomHeader
import com.sekusarisu.yanami.domain.model.TerminalSnippet
import kotlinx.serialization.Serializable

@Serializable
data class ConfigBackup(
        val version: Int = CURRENT_VERSION,
        val exportedAt: Long = System.currentTimeMillis(),
        val servers: List<BackupServer> = emptyList(),
        val snippets: List<TerminalSnippet> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class BackupServer(
        val name: String,
        val baseUrl: String,
        val username: String = "",
        val password: String = "",
        val sessionToken: String? = null,
        val requires2fa: Boolean = false,
        val isActive: Boolean = false,
        val createdAt: Long = System.currentTimeMillis(),
        val authType: AuthType = AuthType.PASSWORD,
        val apiKey: String? = null,
        val customHeaders: List<CustomHeader> = emptyList()
)

data class ConfigExportSummary(
        val serverCount: Int,
        val snippetCount: Int
)

data class ConfigImportSummary(
        val addedServerCount: Int,
        val updatedServerCount: Int,
        val skippedServerCount: Int,
        val addedSnippetCount: Int,
        val updatedSnippetCount: Int,
        val skippedSnippetCount: Int,
        val restoredActiveServer: Boolean
)
