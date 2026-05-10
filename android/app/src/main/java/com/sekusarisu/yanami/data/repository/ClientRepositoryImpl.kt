package com.sekusarisu.yanami.data.repository

import com.sekusarisu.yanami.data.remote.KomariAdminClientService
import com.sekusarisu.yanami.data.remote.dto.ManagedClientDto
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.domain.model.ManagedClientCreateResult
import com.sekusarisu.yanami.domain.model.ManagedClientDraft
import com.sekusarisu.yanami.domain.model.toUpdatePayload
import com.sekusarisu.yanami.domain.repository.ClientRepository

class ClientRepositoryImpl(private val service: KomariAdminClientService) : ClientRepository {

    override suspend fun listClients(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType
    ): List<ManagedClient> {
        return service.listClients(baseUrl, sessionToken, authType).map { it.toDomain() }.sortClients()
    }

    override suspend fun getClient(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            uuid: String
    ): ManagedClient {
        return service.getClient(baseUrl, sessionToken, authType, uuid).toDomain()
    }

    override suspend fun addClient(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            name: String?
    ): ManagedClientCreateResult {
        val result = service.addClient(baseUrl, sessionToken, authType, name)
        return ManagedClientCreateResult(uuid = result.uuid, token = result.token)
    }

    override suspend fun updateClient(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            uuid: String,
            draft: ManagedClientDraft
    ) {
        service.updateClient(
                baseUrl = baseUrl,
                sessionToken = sessionToken,
                authType = authType,
                uuid = uuid,
                payload = draft.toUpdatePayload()
        )
    }

    override suspend fun deleteClient(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            uuid: String
    ) {
        service.deleteClient(baseUrl, sessionToken, authType, uuid)
    }

    override suspend fun getClientToken(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            uuid: String
    ): String {
        return service.getClientToken(baseUrl, sessionToken, authType, uuid)
    }

    override suspend fun reorderClients(
            baseUrl: String,
            sessionToken: String,
            authType: AuthType,
            weights: Map<String, Int>
    ) {
        service.reorderClients(baseUrl, sessionToken, authType, weights)
    }
}

private fun ManagedClientDto.toDomain(): ManagedClient =
        ManagedClient(
                uuid = uuid,
                token = token,
                name = name,
                cpuName = cpuName,
                virtualization = virtualization,
                arch = arch,
                cpuCores = cpuCores,
                os = os,
                kernelVersion = kernelVersion,
                gpuName = gpuName,
                ipv4 = ipv4,
                ipv6 = ipv6,
                region = region,
                remark = remark,
                publicRemark = publicRemark,
                memTotal = memTotal,
                swapTotal = swapTotal,
                diskTotal = diskTotal,
                version = version,
                weight = weight,
                price = price,
                billingCycle = billingCycle,
                autoRenewal = autoRenewal,
                currency = currency,
                expiredAt = expiredAt,
                group = group,
                tags = tags,
                hidden = hidden,
                trafficLimit = trafficLimit,
                trafficLimitType = trafficLimitType,
                createdAt = createdAt,
                updatedAt = updatedAt
        )

private fun List<ManagedClient>.sortClients(): List<ManagedClient> {
    return sortedWith(compareBy<ManagedClient> { it.weight }.thenBy { it.name })
}
