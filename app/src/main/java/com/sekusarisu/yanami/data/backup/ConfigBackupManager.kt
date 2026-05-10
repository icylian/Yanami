package com.sekusarisu.yanami.data.backup

import android.content.Context
import android.net.Uri
import com.sekusarisu.yanami.data.local.preferences.UserPreferencesRepository
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.CustomHeader
import com.sekusarisu.yanami.domain.model.ServerInstance
import com.sekusarisu.yanami.domain.model.TerminalSnippet
import com.sekusarisu.yanami.domain.repository.ServerRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConfigBackupManager(
        private val context: Context,
        private val serverRepository: ServerRepository,
        private val userPreferencesRepository: UserPreferencesRepository
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportToUri(uri: Uri): ConfigExportSummary {
        val servers = serverRepository.getAll()
        val snippets = userPreferencesRepository.terminalSnippets.first()
        val backup =
                ConfigBackup(
                        servers = servers.map { it.toBackupServer() },
                        snippets = snippets
                )

        val content = json.encodeToString(backup)
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write(content)
        } ?: throw IllegalStateException("Unable to open output stream")

        return ConfigExportSummary(serverCount = servers.size, snippetCount = snippets.size)
    }

    suspend fun importFromUri(uri: Uri): ConfigImportSummary {
        val content =
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    reader.readText()
                } ?: throw IllegalStateException("Unable to open input stream")

        val backup = json.decodeFromString<ConfigBackup>(content)
        require(backup.version == ConfigBackup.CURRENT_VERSION) {
            "Unsupported backup version: ${backup.version}"
        }

        val existingServers = serverRepository.getAll()
        val currentSnippets = userPreferencesRepository.terminalSnippets.first()

        var addedServerCount = 0
        var updatedServerCount = 0
        var skippedServerCount = 0
        var activeServerIdToRestore: Long? = null

        val existingServersByKey = existingServers.associateBy { it.mergeKey() }.toMutableMap()

        backup.servers.forEach { backupServer ->
            val normalizedServer = backupServer.toValidatedServerOrNull()
            if (normalizedServer == null) {
                skippedServerCount++
                return@forEach
            }

            val existing = existingServersByKey[normalizedServer.mergeKey()]
            if (existing != null) {
                val updated =
                        existing.copy(
                                name = normalizedServer.name,
                                baseUrl = normalizedServer.baseUrl,
                                username = normalizedServer.username,
                                password = normalizedServer.password,
                                sessionToken = normalizedServer.sessionToken,
                                requires2fa = normalizedServer.requires2fa,
                                authType = normalizedServer.authType,
                                apiKey = normalizedServer.apiKey,
                                customHeaders = normalizedServer.customHeaders
                        )
                serverRepository.update(updated)
                existingServersByKey[updated.mergeKey()] = updated
                updatedServerCount++
                if (backupServer.isActive) {
                    activeServerIdToRestore = updated.id
                }
            } else {
                val newId = serverRepository.add(normalizedServer.copy(isActive = false))
                val inserted = normalizedServer.copy(id = newId, isActive = false)
                existingServersByKey[inserted.mergeKey()] = inserted
                addedServerCount++
                if (backupServer.isActive) {
                    activeServerIdToRestore = newId
                }
            }
        }

        activeServerIdToRestore?.let { activeServerId ->
            serverRepository.setActive(activeServerId)
        }

        val mergedSnippets = currentSnippets.toMutableList()
        var addedSnippetCount = 0
        var updatedSnippetCount = 0
        var skippedSnippetCount = 0

        backup.snippets.forEach { rawSnippet ->
            val snippet = rawSnippet.normalizedOrNull()
            if (snippet == null) {
                skippedSnippetCount++
                return@forEach
            }

            val sameIdIndex = mergedSnippets.indexOfFirst { it.id == snippet.id }
            if (sameIdIndex >= 0) {
                mergedSnippets[sameIdIndex] = snippet
                updatedSnippetCount++
                return@forEach
            }

            val sameContentIndex =
                    mergedSnippets.indexOfFirst {
                        it.title.trim() == snippet.title &&
                                normalizeSnippetContent(it.content) == snippet.content
                    }
            if (sameContentIndex >= 0) {
                val existing = mergedSnippets[sameContentIndex]
                mergedSnippets[sameContentIndex] =
                        existing.copy(
                                title = snippet.title,
                                content = snippet.content,
                                appendEnter = snippet.appendEnter
                        )
                updatedSnippetCount++
                return@forEach
            }

            mergedSnippets += snippet
            addedSnippetCount++
        }

        userPreferencesRepository.setTerminalSnippets(
                mergedSnippets.sortedBy { it.title.lowercase() }
        )

        return ConfigImportSummary(
                addedServerCount = addedServerCount,
                updatedServerCount = updatedServerCount,
                skippedServerCount = skippedServerCount,
                addedSnippetCount = addedSnippetCount,
                updatedSnippetCount = updatedSnippetCount,
                skippedSnippetCount = skippedSnippetCount,
                restoredActiveServer = activeServerIdToRestore != null
        )
    }

    private fun ServerInstance.toBackupServer(): BackupServer {
        return BackupServer(
                name = name,
                baseUrl = baseUrl.trim().trimEnd('/'),
                username = username.trim(),
                password = password,
                sessionToken = sessionToken,
                requires2fa = requires2fa,
                isActive = isActive,
                createdAt = createdAt,
                authType = authType,
                apiKey = apiKey,
                customHeaders = customHeaders
        )
    }

    private fun BackupServer.toValidatedServerOrNull(): ServerInstance? {
        val normalizedName = name.trim()
        val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
        val normalizedUsername = username.trim()
        val normalizedApiKey = apiKey?.trim()?.ifBlank { null }
        val normalizedCustomHeaders = customHeaders.normalizedCustomHeaders()

        if (normalizedName.isBlank() || normalizedBaseUrl.isBlank()) {
            return null
        }

        return when (authType) {
            AuthType.PASSWORD -> {
                if (normalizedUsername.isBlank() || password.isBlank()) {
                    null
                } else {
                    ServerInstance(
                            name = normalizedName,
                            baseUrl = normalizedBaseUrl,
                            username = normalizedUsername,
                            password = password,
                            sessionToken = sessionToken,
                            requires2fa = requires2fa,
                            isActive = false,
                            createdAt = createdAt,
                            authType = authType,
                            apiKey = null,
                            customHeaders = normalizedCustomHeaders
                    )
                }
            }
            AuthType.API_KEY -> {
                if (normalizedApiKey.isNullOrBlank()) {
                    null
                } else {
                    ServerInstance(
                            name = normalizedName,
                            baseUrl = normalizedBaseUrl,
                            username = normalizedUsername,
                            password = "",
                            sessionToken = sessionToken,
                            requires2fa = false,
                            isActive = false,
                            createdAt = createdAt,
                            authType = authType,
                            apiKey = normalizedApiKey,
                            customHeaders = normalizedCustomHeaders
                    )
                }
            }
            AuthType.GUEST -> {
                ServerInstance(
                        name = normalizedName,
                        baseUrl = normalizedBaseUrl,
                        username = "",
                        password = "",
                        sessionToken = sessionToken,
                        requires2fa = false,
                        isActive = false,
                        createdAt = createdAt,
                        authType = authType,
                        apiKey = null,
                        customHeaders = normalizedCustomHeaders
                )
            }
        }
    }

    private fun ServerInstance.mergeKey(): String {
        return buildString {
            append(baseUrl.trim().trimEnd('/'))
            append('|')
            append(authType.name)
            append('|')
            append(username.trim())
        }
    }

    private fun TerminalSnippet.normalizedOrNull(): TerminalSnippet? {
        val normalizedTitle = title.trim()
        val normalizedContent = normalizeSnippetContent(content)
        if (id.isBlank() || normalizedTitle.isBlank() || normalizedContent.isBlank()) {
            return null
        }
        return copy(title = normalizedTitle, content = normalizedContent)
    }

    private fun normalizeSnippetContent(content: String): String {
        return content.replace("\r\n", "\n").replace('\r', '\n')
    }

    private fun List<CustomHeader>.normalizedCustomHeaders(): List<CustomHeader> {
        return map { CustomHeader(it.name.trim(), it.value.trim()) }
                .filter { it.name.isNotBlank() && it.value.isNotBlank() }
                .distinctBy { it.name.lowercase() }
    }
}
