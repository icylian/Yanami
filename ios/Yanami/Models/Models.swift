import Foundation
import SwiftData

enum AuthType: String, Codable {
    case none
    case basic
    case token
}

struct CustomHeader: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var key: String
    var value: String
}

@Model
final class ServerInstance {
    var id: UUID
    var name: String
    var baseURL: String
    var authTypeRaw: String
    var username: String
    var customHeadersData: Data?

    var authType: AuthType {
        get { AuthType(rawValue: authTypeRaw) ?? .none }
        set { authTypeRaw = newValue.rawValue }
    }

    var customHeaders: [CustomHeader] {
        get {
            guard let data = customHeadersData else { return [] }
            return (try? JSONDecoder().decode([CustomHeader].self, from: data)) ?? []
        }
        set {
            customHeadersData = try? JSONEncoder().encode(newValue)
        }
    }

    init(id: UUID = UUID(), name: String, baseURL: String, authType: AuthType = .none, username: String = "", customHeaders: [CustomHeader] = []) {
        self.id = id
        self.name = name
        self.baseURL = baseURL
        self.authTypeRaw = authType.rawValue
        self.username = username
        self.customHeadersData = try? JSONEncoder().encode(customHeaders)
    }
}

struct RpcResponse<T: Codable>: Codable {
    let success: Bool
    let data: T?
    let error: String?
}

struct KomariNode: Codable, Identifiable {
    let id: String
    let name: String
    let os: String?
    let ip: String?
}

struct NodeStatusPayload: Codable {
    let cpuUsage: Double
    let memoryUsed: Int64
    let memoryTotal: Int64
    let networkRx: Int64
    let networkTx: Int64
}

struct NodeInfoPayload: Codable {
    let node: KomariNode
    let status: NodeStatusPayload
}

struct TerminalCommandPayload: Codable {
    let nodeId: String
    let command: String
}
