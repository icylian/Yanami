import Foundation

enum AuthType: String, CaseIterable, Codable, Identifiable {
    case password
    case apiKey
    case guest

    var id: String { rawValue }

    var title: String {
        switch self {
        case .password:
            return "Password"
        case .apiKey:
            return "API Key"
        case .guest:
            return "Guest"
        }
    }
}

enum StatusFilter: String, CaseIterable, Codable, Identifiable {
    case all
    case online
    case offline

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all:
            return "All"
        case .online:
            return "Online"
        case .offline:
            return "Offline"
        }
    }
}

struct CustomHeader: Codable, Equatable, Identifiable {
    var id = UUID()
    var name: String
    var value: String
}

struct ServerProfile: Codable, Equatable, Identifiable {
    var id = UUID()
    var name = "My Komari"
    var baseURL = "https://"
    var username = ""
    var password = ""
    var apiKey = ""
    var sessionToken = ""
    var requires2FA = false
    var authType = AuthType.password
    var customHeaders: [CustomHeader] = []
    var createdAt = Date()

    var normalizedBaseURL: String {
        baseURL.trimmingCharacters(in: .whitespacesAndNewlines).trimmedTrailingSlash()
    }

    var sanitizedCustomHeaders: [CustomHeader] {
        customHeaders
            .map {
                CustomHeader(
                    id: $0.id,
                    name: $0.name.trimmingCharacters(in: .whitespacesAndNewlines),
                    value: $0.value.trimmingCharacters(in: .whitespacesAndNewlines)
                )
            }
            .filter { !$0.name.isEmpty && !$0.value.isEmpty }
    }
}

struct TerminalSnippet: Codable, Equatable, Identifiable {
    var id = UUID()
    var title: String
    var content: String
    var appendEnter: Bool = true
}

struct AppSettings: Codable, Equatable {
    var autoRefreshEnabled = true
    var refreshIntervalSeconds = 2.0
    var terminalFontSize: Int = 14
    var snippets: [TerminalSnippet] = []
    var maskIpEnabled = false
    var autoEnterNodeList = false
    var chartAnimationEnabled = true
    var biometricEnabled = false
    var darkMode = "system"
    var language = "system"
    var fontScale = 1.0

    enum CodingKeys: String, CodingKey {
        case autoRefreshEnabled, refreshIntervalSeconds, terminalFontSize, snippets, maskIpEnabled
        case autoEnterNodeList, chartAnimationEnabled, biometricEnabled, darkMode, language, fontScale
    }

    init() {}

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        autoRefreshEnabled = try container.decodeIfPresent(Bool.self, forKey: .autoRefreshEnabled) ?? true
        refreshIntervalSeconds = try container.decodeIfPresent(Double.self, forKey: .refreshIntervalSeconds) ?? 2.0
        terminalFontSize = try container.decodeIfPresent(Int.self, forKey: .terminalFontSize) ?? 14
        snippets = try container.decodeIfPresent([TerminalSnippet].self, forKey: .snippets) ?? []
        maskIpEnabled = try container.decodeIfPresent(Bool.self, forKey: .maskIpEnabled) ?? false
        autoEnterNodeList = try container.decodeIfPresent(Bool.self, forKey: .autoEnterNodeList) ?? false
        chartAnimationEnabled = try container.decodeIfPresent(Bool.self, forKey: .chartAnimationEnabled) ?? true
        biometricEnabled = try container.decodeIfPresent(Bool.self, forKey: .biometricEnabled) ?? false
        darkMode = try container.decodeIfPresent(String.self, forKey: .darkMode) ?? "system"
        language = try container.decodeIfPresent(String.self, forKey: .language) ?? "system"
        fontScale = try container.decodeIfPresent(Double.self, forKey: .fontScale) ?? 1.0
    }
}

struct PersistedAppState: Codable {
    var servers: [ServerProfile]
    var activeServerId: UUID?
    var settings: AppSettings
}

struct KomariNode: Identifiable, Equatable {
    var id: String { uuid }

    let uuid: String
    var name: String
    var region: String
    var group: String
    var isOnline: Bool
    var cpuUsage: Double
    var memUsed: Int64
    var memTotal: Int64
    var swapUsed: Int64
    var swapTotal: Int64
    var diskUsed: Int64
    var diskTotal: Int64
    var netIn: Int64
    var netOut: Int64
    var netTotalUp: Int64
    var netTotalDown: Int64
    var uptime: Int64
    var os: String
    var cpuName: String
    var cpuCores: Int
    var weight: Int
    var load1: Double
    var load5: Double
    var load15: Double
    var process: Int
    var connectionsTcp: Int
    var connectionsUdp: Int
    var kernelVersion: String
    var virtualization: String
    var arch: String
    var gpuName: String
    var trafficLimit: Int64
    var trafficLimitType: String
    var expiredAt: String?

    var statusText: String { isOnline ? "Online" : "Offline" }

    var trafficUsage: (used: Int64, percent: Double)? {
        guard trafficLimit > 0 else { return nil }
        let kind = trafficLimitType.lowercased()
        let used: Int64
        switch kind {
        case "max": used = max(netTotalUp, netTotalDown)
        case "min": used = min(netTotalUp, netTotalDown)
        case "up": used = netTotalUp
        case "down": used = netTotalDown
        default: used = netTotalUp + netTotalDown
        }
        return (used, min(Double(used) / Double(trafficLimit), 1))
    }
}

struct LoadRecord: Identifiable, Equatable {
    var id = UUID()
    let time: String
    let cpu: Double
    let ramPercent: Double
    let diskPercent: Double
    let netIn: Int64
    let netOut: Int64
    let load: Double
    let process: Int
    let connections: Int
    let connectionsUdp: Int
}

struct PingTask: Identifiable, Equatable {
    let id: Int
    let name: String
    let interval: Int
    let latest: Double
    let min: Double
    let max: Double
    let avg: Double
    let loss: Double
    let p50: Double
    let p99: Double
}

struct PingRecord: Identifiable, Equatable {
    var id = UUID()
    let taskId: Int
    let taskName: String
    let time: String
    let value: Double
}

struct NodeDetailState: Equatable {
    var node: KomariNode?
    var loadRecords: [LoadRecord] = []
    var pingTasks: [PingTask] = []
    var pingRecords: [PingRecord] = []
    var loadHours = 0
    var pingHours = 1
    var isLoading = false
    var error: String?
}

struct ManagedClient: Codable, Equatable, Identifiable {
    let uuid: String
    var name: String?
    var weight: Int?
    
    var id: String { uuid }
}

struct AdminPingTask: Codable, Equatable, Identifiable {
    let id: Int
    var name: String
    var type: String
    var target: String
    var interval: Int
    var weight: Int?
    
    enum CodingKeys: String, CodingKey {
        case id, name, type, target, interval, weight
    }
}
