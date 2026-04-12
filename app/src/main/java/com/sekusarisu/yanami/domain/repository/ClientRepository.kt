package com.sekusarisu.yanami.domain.repository

import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.domain.model.ManagedClientCreateResult
import com.sekusarisu.yanami.domain.model.ManagedClientDraft

interface ClientRepository {
    suspend fun listClients(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType
    ): List<ManagedClient>

    suspend fun getClient(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            uuid: String
    ): ManagedClient

    suspend fun addClient(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            name: String?
    ): ManagedClientCreateResult

    suspend fun updateClient(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            uuid: String,
            draft: ManagedClientDraft
    )

    suspend fun deleteClient(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            uuid: String
    )

    suspend fun getClientToken(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            uuid: String
    ): String

    suspend fun reorderClients(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            weights: Map<String, Int>
    )
}
