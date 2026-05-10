import Foundation

struct KomariClient {
    private var profile: ServerProfile

    init(profile: ServerProfile) {
        self.profile = profile
    }

    func login(twoFaCode: String? = nil) async throws -> String {
        guard !profile.username.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw KomariClientError.invalidConfiguration("Username is required")
        }
        guard !profile.password.isEmpty else {
            throw KomariClientError.invalidConfiguration("Password is required")
        }

        var body: [String: Any] = [
            "username": profile.username,
            "password": profile.password
        ]
        if let twoFaCode, !twoFaCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            body["2fa_code"] = twoFaCode
        }

        let request = try makeRequest(path: "/api/login", method: "POST", jsonBody: body)
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw KomariClientError.invalidResponse
        }
        guard (200...299).contains(httpResponse.statusCode) else {
            throw KomariClientError.httpStatus(httpResponse.statusCode)
        }
        if let token = extractSessionToken(from: httpResponse) {
            return token
        }
        let text = String(data: data, encoding: .utf8) ?? ""
        if text.localizedCaseInsensitiveContains("2fa") || text.localizedCaseInsensitiveContains("totp") {
            throw KomariClientError.requires2FA
        }
        throw KomariClientError.invalidConfiguration("session_token was not returned")
    }

    func getVersion(token: String) async throws -> String {
        let result: VersionResult = try await rpc(
            method: "common:getVersion",
            token: token,
            responseType: VersionResult.self
        )
        return result.version
    }

    func getNodes(token: String) async throws -> [KomariNode] {
        async let infoTask: [String: NodeInfoPayload] = rpc(
            method: "common:getNodes",
            token: token,
            responseType: [String: NodeInfoPayload].self
        )
        async let statusTask: [String: NodeStatusPayload] = rpc(
            method: "common:getNodesLatestStatus",
            token: token,
            responseType: [String: NodeStatusPayload].self
        )

        let (infos, statuses) = try await (infoTask, statusTask)
        return mergeNodes(infos: infos, statuses: statuses)
    }

    func getLatestStatuses(token: String) async throws -> [String: NodeStatusPayload] {
        try await rpc(
            method: "common:getNodesLatestStatus",
            token: token,
            responseType: [String: NodeStatusPayload].self
        )
    }

    func getLoadRecords(token: String, uuid: String, hours: Int) async throws -> [LoadRecord] {
        let result: LoadRecordsPayload = try await rpc(
            method: "common:getRecords",
            params: ["uuid": uuid, "type": "load", "hours": hours],
            token: token,
            responseType: LoadRecordsPayload.self
        )
        return (result.records[uuid] ?? []).map { $0.toDomain() }
    }

    func getPingRecords(token: String, uuid: String, hours: Int) async throws -> ([PingTask], [PingRecord]) {
        let result: PingRecordsPayload = try await rpc(
            method: "common:getRecords",
            params: ["uuid": uuid, "type": "ping", "hours": hours],
            token: token,
            responseType: PingRecordsPayload.self
        )
        let tasks = result.tasks.map { $0.toDomain() }
        let names = Dictionary(uniqueKeysWithValues: tasks.map { ($0.id, $0.name) })
        let records = result.records
            .filter { $0.client == uuid }
            .map { $0.toDomain(taskName: names[$0.taskId] ?? "Task \($0.taskId)") }
        return (tasks, records)
    }

    func getRecentStatus(token: String, uuid: String) async throws -> [LoadRecord] {
        let request = try makeRequest(path: "/api/recent/\(uuid)", method: "GET", token: token)
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw KomariClientError.invalidResponse
        }
        guard (200...299).contains(httpResponse.statusCode) else {
            throw KomariClientError.httpStatus(httpResponse.statusCode)
        }
        let payload = try Self.decoder.decode(RecentStatusResponse.self, from: data)
        return payload.data.map { $0.toDomain() }
    }

    func mergeNodes(infos: [String: NodeInfoPayload], statuses: [String: NodeStatusPayload]) -> [KomariNode] {
        infos.map { uuid, info in
            let status = statuses[uuid]
            return KomariNode(
                uuid: uuid,
                name: (info.name ?? "").isEmpty ? uuid : info.name ?? uuid,
                region: info.region ?? "",
                group: info.group ?? "",
                isOnline: status?.online ?? false,
                cpuUsage: status?.cpu ?? 0,
                memUsed: status?.ram ?? 0,
                memTotal: nonZero(status?.ramTotal, fallback: info.memTotal),
                swapUsed: status?.swap ?? 0,
                swapTotal: nonZero(status?.swapTotal, fallback: info.swapTotal),
                diskUsed: status?.disk ?? 0,
                diskTotal: nonZero(status?.diskTotal, fallback: info.diskTotal),
                netIn: status?.netIn ?? 0,
                netOut: status?.netOut ?? 0,
                netTotalUp: status?.netTotalUp ?? 0,
                netTotalDown: status?.netTotalDown ?? 0,
                uptime: status?.uptime ?? 0,
                os: info.os ?? "",
                cpuName: info.cpuName ?? "",
                cpuCores: info.cpuCores ?? 0,
                weight: info.weight ?? 0,
                load1: status?.load ?? 0,
                load5: status?.load5 ?? 0,
                load15: status?.load15 ?? 0,
                process: status?.process ?? 0,
                connectionsTcp: status?.connections ?? 0,
                connectionsUdp: status?.connectionsUdp ?? 0,
                kernelVersion: info.kernelVersion ?? "",
                virtualization: info.virtualization ?? "",
                arch: info.arch ?? "",
                gpuName: info.gpuName ?? "",
                trafficLimit: info.trafficLimit ?? 0,
                trafficLimitType: info.trafficLimitType ?? "",
                expiredAt: info.expiredAt
            )
        }
        .sorted {
            if $0.isOnline != $1.isOnline {
                return $0.isOnline && !$1.isOnline
            }
            if $0.weight != $1.weight {
                return $0.weight < $1.weight
            }
            return $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
        }
    }

    func mergeStatuses(nodes: [KomariNode], statuses: [String: NodeStatusPayload]) -> [KomariNode] {
        nodes.map { node in
            guard let status = statuses[node.uuid] else {
                var offline = node
                offline.isOnline = false
                return offline
            }
            var updated = node
            updated.isOnline = status.online ?? false
            updated.cpuUsage = status.cpu ?? 0
            updated.memUsed = status.ram ?? 0
            updated.memTotal = nonZero(status.ramTotal, fallback: node.memTotal)
            updated.swapUsed = status.swap ?? 0
            updated.swapTotal = nonZero(status.swapTotal, fallback: node.swapTotal)
            updated.diskUsed = status.disk ?? 0
            updated.diskTotal = nonZero(status.diskTotal, fallback: node.diskTotal)
            updated.netIn = status.netIn ?? 0
            updated.netOut = status.netOut ?? 0
            updated.netTotalUp = status.netTotalUp ?? 0
            updated.netTotalDown = status.netTotalDown ?? 0
            updated.uptime = status.uptime ?? 0
            updated.load1 = status.load ?? 0
            updated.load5 = status.load5 ?? 0
            updated.load15 = status.load15 ?? 0
            updated.process = status.process ?? 0
            updated.connectionsTcp = status.connections ?? 0
            updated.connectionsUdp = status.connectionsUdp ?? 0
            return updated
        }
        .sorted {
            if $0.isOnline != $1.isOnline {
                return $0.isOnline && !$1.isOnline
            }
            if $0.weight != $1.weight {
                return $0.weight < $1.weight
            }
            return $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
        }
    }

    private func rpc<T: Decodable>(
        method: String,
        params: [String: Any]? = nil,
        token: String,
        responseType: T.Type
    ) async throws -> T {
        var body: [String: Any] = [
            "jsonrpc": "2.0",
            "method": method,
            "id": 1
        ]
        if let params {
            body["params"] = params
        }
        let request = try makeRequest(path: "/api/rpc2", method: "POST", jsonBody: body, token: token)
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw KomariClientError.invalidResponse
        }
        guard (200...299).contains(httpResponse.statusCode) else {
            throw KomariClientError.httpStatus(httpResponse.statusCode)
        }
        let envelope = try Self.decoder.decode(RpcEnvelope<T>.self, from: data)
        if let error = envelope.error {
            throw KomariClientError.rpc(error.message ?? "RPC error")
        }
        guard let result = envelope.result else {
            throw KomariClientError.invalidResponse
        }
        return result
    }

    private func makeRequest(
        path: String,
        method: String,
        jsonBody: [String: Any]? = nil,
        token: String? = nil
    ) throws -> URLRequest {
        guard let url = URL(string: profile.normalizedBaseURL + path) else {
            throw KomariClientError.invalidConfiguration("Server URL is invalid")
        }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("YanamiNext-iPhone/1.0", forHTTPHeaderField: "User-Agent")
        profile.sanitizedCustomHeaders.forEach { header in
            request.setValue(header.value, forHTTPHeaderField: header.name)
        }

        if let token, !token.isEmpty {
            switch profile.authType {
            case .password:
                request.setValue("session_token=\(token)", forHTTPHeaderField: "Cookie")
            case .apiKey:
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            case .guest:
                break
            }
        }

        if let jsonBody {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONSerialization.data(withJSONObject: jsonBody)
        }
        return request
    }

    private func extractSessionToken(from response: HTTPURLResponse) -> String? {
        let cookieHeader = response.value(forHTTPHeaderField: "Set-Cookie") ?? ""
        for part in cookieHeader.components(separatedBy: ";") {
            let trimmed = part.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.hasPrefix("session_token=") {
                let token = String(trimmed.dropFirst("session_token=".count))
                return token.isEmpty ? nil : token
            }
        }
        return nil
    }

    private func nonZero(_ value: Int64?, fallback: Int64?) -> Int64 {
        let candidate = value ?? 0
        return candidate > 0 ? candidate : fallback ?? 0
    }

    private static let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        return decoder
    }()
}

enum KomariClientError: LocalizedError {
    case invalidConfiguration(String)
    case invalidResponse
    case httpStatus(Int)
    case rpc(String)
    case requires2FA

    var errorDescription: String? {
        switch self {
        case .invalidConfiguration(let message):
            return message
        case .invalidResponse:
            return "Invalid server response"
        case .httpStatus(let status):
            return "HTTP \(status)"
        case .rpc(let message):
            return message
        case .requires2FA:
            return "Two-factor authentication is required"
        }
    }
}

private struct RpcEnvelope<Result: Decodable>: Decodable {
    let result: Result?
    let error: RpcErrorPayload?
}

private struct RpcErrorPayload: Decodable {
    let code: Int?
    let message: String?
}

private struct VersionResult: Decodable {
    let version: String
}

struct NodeInfoPayload: Decodable {
    let name: String?
    let cpuName: String?
    let virtualization: String?
    let arch: String?
    let cpuCores: Int?
    let os: String?
    let kernelVersion: String?
    let gpuName: String?
    let region: String?
    let memTotal: Int64?
    let swapTotal: Int64?
    let diskTotal: Int64?
    let weight: Int?
    let expiredAt: String?
    let group: String?
    let trafficLimit: Int64?
    let trafficLimitType: String?
}

struct NodeStatusPayload: Decodable {
    let cpu: Double?
    let ram: Int64?
    let ramTotal: Int64?
    let swap: Int64?
    let swapTotal: Int64?
    let load: Double?
    let load5: Double?
    let load15: Double?
    let disk: Int64?
    let diskTotal: Int64?
    let netIn: Int64?
    let netOut: Int64?
    let netTotalUp: Int64?
    let netTotalDown: Int64?
    let process: Int?
    let connections: Int?
    let connectionsUdp: Int?
    let uptime: Int64?
    let online: Bool?
}

private struct LoadRecordsPayload: Decodable {
    let records: [String: [LoadRecordPayload]]
}

private struct LoadRecordPayload: Decodable {
    let time: String
    let cpu: Double?
    let ram: Int64?
    let ramTotal: Int64?
    let disk: Int64?
    let diskTotal: Int64?
    let netIn: Int64?
    let netOut: Int64?
    let load: Double?
    let process: Int?
    let connections: Int?
    let connectionsUdp: Int?

    func toDomain() -> LoadRecord {
        LoadRecord(
            time: time,
            cpu: cpu ?? 0,
            ramPercent: percent(used: ram, total: ramTotal),
            diskPercent: percent(used: disk, total: diskTotal),
            netIn: netIn ?? 0,
            netOut: netOut ?? 0,
            load: load ?? 0,
            process: process ?? 0,
            connections: connections ?? 0,
            connectionsUdp: connectionsUdp ?? 0
        )
    }
}

private struct PingRecordsPayload: Decodable {
    let records: [PingRecordPayload]
    let tasks: [PingTaskPayload]
}

private struct PingTaskPayload: Decodable {
    let id: Int
    let name: String
    let interval: Int?
    let latest: Double?
    let min: Double?
    let max: Double?
    let avg: Double?
    let loss: Double?
    let p50: Double?
    let p99: Double?

    func toDomain() -> PingTask {
        PingTask(
            id: id,
            name: name,
            interval: interval ?? 0,
            latest: latest ?? 0,
            min: min ?? 0,
            max: max ?? 0,
            avg: avg ?? 0,
            loss: loss ?? 0,
            p50: p50 ?? 0,
            p99: p99 ?? 0
        )
    }
}

private struct PingRecordPayload: Decodable {
    let taskId: Int
    let time: String
    let value: Double?
    let client: String

    func toDomain(taskName: String) -> PingRecord {
        PingRecord(taskId: taskId, taskName: taskName, time: time, value: value ?? 0)
    }
}

private struct RecentStatusResponse: Decodable {
    let data: [RecentStatusItemPayload]
}

private struct RecentStatusItemPayload: Decodable {
    let cpu: RecentCpuPayload?
    let ram: RecentUsedTotalPayload?
    let load: RecentLoadPayload?
    let disk: RecentUsedTotalPayload?
    let network: RecentNetworkPayload?
    let connections: RecentConnectionsPayload?
    let process: Int?
    let updatedAt: String?

    func toDomain() -> LoadRecord {
        LoadRecord(
            time: updatedAt ?? "",
            cpu: cpu?.usage ?? 0,
            ramPercent: percent(used: ram?.used, total: ram?.total),
            diskPercent: percent(used: disk?.used, total: disk?.total),
            netIn: network?.down ?? 0,
            netOut: network?.up ?? 0,
            load: load?.load1 ?? 0,
            process: process ?? 0,
            connections: connections?.tcp ?? 0,
            connectionsUdp: connections?.udp ?? 0
        )
    }
}

private struct RecentCpuPayload: Decodable { let usage: Double? }
private struct RecentUsedTotalPayload: Decodable { let total: Int64?; let used: Int64? }
private struct RecentLoadPayload: Decodable { let load1: Double? }
private struct RecentNetworkPayload: Decodable { let up: Int64?; let down: Int64? }
private struct RecentConnectionsPayload: Decodable { let tcp: Int?; let udp: Int? }

private func percent(used: Int64?, total: Int64?) -> Double {
    guard let used, let total, total > 0 else { return 0 }
    return Double(used) / Double(total) * 100
}
