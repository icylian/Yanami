import Foundation

@MainActor
final class AppStore: ObservableObject {
    @Published var servers: [ServerProfile] = []
    @Published var activeServerId: UUID?
    @Published var settings = AppSettings()

    @Published var nodes: [KomariNode] = []
    @Published var searchQuery = ""
    @Published var selectedGroup: String?
    @Published var statusFilter = StatusFilter.all
    @Published var statusMessage = ""
    @Published var isLoadingNodes = false
    @Published var isRefreshingNodes = false
    @Published var selectedNodeId: String?
    @Published var nodeDetail = NodeDetailState()

    private let profileStore = ProfileStore()
    private var refreshTask: Task<Void, Never>?

    var activeServer: ServerProfile? {
        guard let activeServerId else { return nil }
        return servers.first { $0.id == activeServerId }
    }

    var groups: [String] {
        nodes.map(\.group).filter { !$0.isEmpty }.uniqueSorted()
    }

    var filteredNodes: [KomariNode] {
        nodes.filter { node in
            let matchesSearch =
                searchQuery.isEmpty ||
                node.name.localizedCaseInsensitiveContains(searchQuery) ||
                node.uuid.localizedCaseInsensitiveContains(searchQuery) ||
                node.region.localizedCaseInsensitiveContains(searchQuery) ||
                node.group.localizedCaseInsensitiveContains(searchQuery)
            let matchesGroup = selectedGroup == nil || node.group == selectedGroup
            let matchesStatus: Bool
            switch statusFilter {
            case .all: matchesStatus = true
            case .online: matchesStatus = node.isOnline
            case .offline: matchesStatus = !node.isOnline
            }
            return matchesSearch && matchesGroup && matchesStatus
        }
    }

    var onlineCount: Int { nodes.filter(\.isOnline).count }
    var offlineCount: Int { nodes.count - onlineCount }
    var totalNetIn: Int64 { nodes.filter(\.isOnline).reduce(0) { $0 + $1.netIn } }
    var totalNetOut: Int64 { nodes.filter(\.isOnline).reduce(0) { $0 + $1.netOut } }
    var totalTrafficUp: Int64 { nodes.filter(\.isOnline).reduce(0) { $0 + $1.netTotalUp } }
    var totalTrafficDown: Int64 { nodes.filter(\.isOnline).reduce(0) { $0 + $1.netTotalDown } }

    init() {
        let persisted = profileStore.load()
        servers = persisted.servers
        activeServerId = persisted.activeServerId ?? persisted.servers.first?.id
        settings = persisted.settings
        startAutoRefresh()
    }

    deinit {
        refreshTask?.cancel()
    }

    func persist() {
        profileStore.save(
            PersistedAppState(
                servers: servers,
                activeServerId: activeServerId,
                settings: settings
            )
        )
    }

    func addServer(_ server: ServerProfile) {
        var normalized = server
        normalized.baseURL = normalized.normalizedBaseURL
        normalized.customHeaders = normalized.sanitizedCustomHeaders
        servers.append(normalized)
        activeServerId = normalized.id
        persist()
    }

    func updateServer(_ server: ServerProfile) {
        var normalized = server
        normalized.baseURL = normalized.normalizedBaseURL
        normalized.customHeaders = normalized.sanitizedCustomHeaders
        if let index = servers.firstIndex(where: { $0.id == normalized.id }) {
            servers[index] = normalized
        }
        if activeServerId == nil {
            activeServerId = normalized.id
        }
        persist()
    }

    func deleteServer(_ server: ServerProfile) {
        servers.removeAll { $0.id == server.id }
        if activeServerId == server.id {
            activeServerId = servers.first?.id
            nodes = []
            selectedNodeId = nil
            nodeDetail = NodeDetailState()
        }
        persist()
    }

    func selectServer(_ server: ServerProfile) {
        activeServerId = server.id
        nodes = []
        selectedNodeId = nil
        nodeDetail = NodeDetailState()
        persist()
        Task {
            await loadNodes(mode: .initial)
        }
    }

    func addCloudflareHeaders(to server: inout ServerProfile) {
        let existing = Set(server.customHeaders.map { $0.name.lowercased() })
        if !existing.contains("cf-access-client-id") {
            server.customHeaders.append(CustomHeader(name: "CF-Access-Client-Id", value: ""))
        }
        if !existing.contains("cf-access-client-secret") {
            server.customHeaders.append(CustomHeader(name: "CF-Access-Client-Secret", value: ""))
        }
    }

    func testConnection(_ server: ServerProfile) async throws -> String {
        let client = KomariClient(profile: server)
        let token = try await resolveToken(for: server, client: client)
        return try await client.getVersion(token: token)
    }

    func loadNodes(mode: NodeLoadMode = .refresh) async {
        guard let server = activeServer else {
            statusMessage = "Add or select a Komari instance first"
            return
        }
        if mode == .initial {
            isLoadingNodes = true
        } else {
            isRefreshingNodes = true
        }
        defer {
            isLoadingNodes = false
            isRefreshingNodes = false
        }

        do {
            let client = KomariClient(profile: server)
            let token = try await resolveToken(for: server, client: client)
            let fetched = try await client.getNodes(token: token)
            nodes = fetched
            statusMessage = "Loaded \(fetched.count) node(s)"
            if let selectedNodeId {
                await loadNodeDetail(uuid: selectedNodeId, preserveRecords: true)
            }
        } catch {
            statusMessage = error.localizedDescription
        }
    }

    func refreshStatusesOnly() async {
        guard let server = activeServer, !nodes.isEmpty else { return }
        do {
            let client = KomariClient(profile: server)
            let token = try await resolveToken(for: server, client: client)
            let statuses = try await client.getLatestStatuses(token: token)
            nodes = client.mergeStatuses(nodes: nodes, statuses: statuses)
            if let selectedNodeId, var selected = nodes.first(where: { $0.uuid == selectedNodeId }) {
                if let status = statuses[selectedNodeId] {
                    selected = client.mergeStatuses(nodes: [selected], statuses: [selectedNodeId: status]).first ?? selected
                }
                nodeDetail.node = selected
                if nodeDetail.loadHours == 0 {
                    appendRealtimeLoadRecord(from: selected)
                }
            }
        } catch {
            statusMessage = error.localizedDescription
        }
    }

    func loadNodeDetail(uuid: String, preserveRecords: Bool = false) async {
        let uuid = uuid.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !uuid.isEmpty else {
            nodeDetail.error = "UUID is required"
            nodeDetail.isLoading = false
            return
        }
        selectedNodeId = uuid
        nodeDetail.isLoading = true
        nodeDetail.error = nil
        if !preserveRecords {
            nodeDetail.loadRecords = []
            nodeDetail.pingRecords = []
            nodeDetail.pingTasks = []
        }

        guard let server = activeServer else {
            nodeDetail.error = "No active server"
            nodeDetail.isLoading = false
            return
        }

        do {
            let client = KomariClient(profile: server)
            let token = try await resolveToken(for: server, client: client)
            if nodes.isEmpty {
                nodes = try await client.getNodes(token: token)
            }
            nodeDetail.node = nodes.first { $0.uuid.trimmingCharacters(in: .whitespacesAndNewlines) == uuid }
            nodeDetail.isLoading = false

            // Load records independently
            do {
                if nodeDetail.loadHours == 0 {
                    nodeDetail.loadRecords = try await client.getRecentStatus(token: token, uuid: uuid)
                } else {
                    nodeDetail.loadRecords = try await client.getLoadRecords(token: token, uuid: uuid, hours: nodeDetail.loadHours)
                }
            } catch {
                if nodeDetail.loadHours == 0, let node = nodeDetail.node {
                    appendRealtimeLoadRecord(from: node)
                } else {
                    nodeDetail.error = "Load records: \(error.localizedDescription)"
                }
            }

            do {
                let ping = try await client.getPingRecords(token: token, uuid: uuid, hours: nodeDetail.pingHours)
                nodeDetail.pingTasks = ping.0
                nodeDetail.pingRecords = ping.1
            } catch {
                let current = nodeDetail.error ?? ""
                nodeDetail.error = current.isEmpty ? "Ping records: \(error.localizedDescription)" : "\(current)\nPing records: \(error.localizedDescription)"
            }
        } catch {
            nodeDetail.isLoading = false
            nodeDetail.error = error.localizedDescription
        }
    }

    func setLoadHours(_ hours: Int) {
        nodeDetail.loadHours = hours
        guard let selectedNodeId else { return }
        Task { await loadNodeDetail(uuid: selectedNodeId) }
    }

    func setPingHours(_ hours: Int) {
        nodeDetail.pingHours = hours
        guard let selectedNodeId else { return }
        Task { await loadNodeDetail(uuid: selectedNodeId) }
    }

    func refreshNodeDetailRecords() async {
        guard let selectedNodeId else { return }
        if nodeDetail.loadHours == 0 {
            await refreshStatusesOnly()
        } else {
            await loadNodeDetail(uuid: selectedNodeId, preserveRecords: true)
        }
    }

    func updateSettings(_ settings: AppSettings) {
        self.settings = settings
        persist()
        startAutoRefresh()
    }

    func startAutoRefresh() {
        refreshTask?.cancel()
        guard settings.autoRefreshEnabled else { return }
        refreshTask = Task { [weak self] in
            while !Task.isCancelled {
                let interval = self?.settings.refreshIntervalSeconds ?? 2
                try? await Task.sleep(nanoseconds: UInt64(max(interval, 1) * 1_000_000_000))
                await self?.refreshStatusesOnly()
            }
        }
    }

    private func resolveToken(for server: ServerProfile, client: KomariClient) async throws -> String {
        switch server.authType {
        case .guest:
            return ""
        case .apiKey:
            let token = server.apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
            if token.isEmpty {
                throw KomariClientError.invalidConfiguration("API Key is required")
            }
            return token
        case .password:
            if !server.sessionToken.isEmpty {
                return server.sessionToken
            }
            let token = try await client.login()
            updateSessionToken(token, for: server.id)
            return token
        }
    }

    private func updateSessionToken(_ token: String, for serverId: UUID) {
        guard let index = servers.firstIndex(where: { $0.id == serverId }) else { return }
        servers[index].sessionToken = token
        servers[index].requires2FA = false
        persist()
    }

    private func appendRealtimeLoadRecord(from node: KomariNode) {
        let now = Date()
        if let last = nodeDetail.loadRecords.last,
           now.timeIntervalSince(parseLoadRecordDate(last.time)) < max(settings.refreshIntervalSeconds * 0.8, 0.8) {
            return
        }
        let record = LoadRecord(
            time: ISO8601DateFormatter().string(from: now),
            cpu: node.cpuUsage,
            ramPercent: node.memTotal > 0 ? Double(node.memUsed) / Double(node.memTotal) * 100 : 0,
            diskPercent: node.diskTotal > 0 ? Double(node.diskUsed) / Double(node.diskTotal) * 100 : 0,
            netIn: node.netIn,
            netOut: node.netOut,
            load: node.load1,
            process: node.process,
            connections: node.connectionsTcp,
            connectionsUdp: node.connectionsUdp
        )
        nodeDetail.loadRecords.append(record)
        if nodeDetail.loadRecords.count > 120 {
            nodeDetail.loadRecords.removeFirst(nodeDetail.loadRecords.count - 120)
        }
    }

    private func parseLoadRecordDate(_ string: String) -> Date {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: string) { return date }
        formatter.formatOptions = [.withInternetDateTime]
        return formatter.date(from: string) ?? .distantPast
    }
}

enum NodeLoadMode {
    case initial
    case refresh
}

private extension Array where Element == String {
    func uniqueSorted() -> [String] {
        Array(Set(self)).sorted()
    }
}
