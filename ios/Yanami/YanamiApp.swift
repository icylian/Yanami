import Foundation
import Security
import SwiftUI

@main
struct YanamiApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
        }
    }
}

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

struct CustomHeader: Codable, Equatable, Identifiable {
    var id = UUID()
    var name: String
    var value: String
}

struct ServerProfile: Codable, Equatable {
    var name = "My Komari"
    var baseURL = "https://"
    var authType = AuthType.password
    var username = ""
    var password = ""
    var apiKey = ""
    var sessionToken = ""
    var customHeaders: [CustomHeader] = []

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

struct KomariNode: Identifiable, Equatable {
    let id: String
    let name: String
    let group: String
    let region: String
    let os: String
    let isOnline: Bool
    let cpu: Double
    let ram: Int64
    let ramTotal: Int64
    let disk: Int64
    let diskTotal: Int64
    let netIn: Int64
    let netOut: Int64
}

@MainActor
final class AppState: ObservableObject {
    @Published var profile: ServerProfile {
        didSet {
            persist()
        }
    }
    @Published var nodes: [KomariNode] = []
    @Published var statusMessage: String = ""
    @Published var isBusy = false

    private static let keychainAccount = "serverProfile"
    private static let keychainStore = KeychainStore(service: "com.sekusarisu.yanami.ios")

    init() {
        if let decoded = Self.keychainStore.read(ServerProfile.self, account: Self.keychainAccount) {
            profile = decoded
        } else {
            profile = ServerProfile()
        }
    }

    func addHeader() {
        let existing = Set(profile.customHeaders.map { $0.name.lowercased() })
        let defaultName: String
        if !existing.contains("cf-access-client-id") {
            defaultName = "CF-Access-Client-Id"
        } else if !existing.contains("cf-access-client-secret") {
            defaultName = "CF-Access-Client-Secret"
        } else {
            defaultName = ""
        }
        profile.customHeaders.append(CustomHeader(name: defaultName, value: ""))
    }

    func removeHeader(_ header: CustomHeader) {
        profile.customHeaders.removeAll { $0.id == header.id }
    }

    func testConnection() async {
        await runOperation(successMessage: "Connected") { client in
            let version = try await client.getVersion(token: try await resolveToken(client: client))
            return "Connected. Komari \(version)"
        }
    }

    func loadNodes() async {
        await runOperation(successMessage: "Loaded") { client in
            let token = try await resolveToken(client: client)
            let fetchedNodes = try await client.getNodes(token: token)
            nodes = fetchedNodes
            return "Loaded \(fetchedNodes.count) node(s)"
        }
    }

    private func runOperation(
        successMessage: String,
        operation: (KomariClient) async throws -> String
    ) async {
        isBusy = true
        statusMessage = ""
        defer {
            isBusy = false
        }

        do {
            let client = KomariClient(profile: profile)
            let message = try await operation(client)
            statusMessage = message.isEmpty ? successMessage : message
        } catch {
            statusMessage = error.localizedDescription
        }
    }

    private func resolveToken(client: KomariClient) async throws -> String {
        switch profile.authType {
        case .guest:
            return ""
        case .apiKey:
            let token = profile.apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
            if token.isEmpty {
                throw KomariClientError.invalidConfiguration("API Key is required")
            }
            return token
        case .password:
            if !profile.sessionToken.isEmpty {
                return profile.sessionToken
            }
            let token = try await client.login()
            profile.sessionToken = token
            return token
        }
    }

    private func persist() {
        try? Self.keychainStore.save(profile, account: Self.keychainAccount)
    }
}

struct ContentView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        NavigationStack {
            List {
                Section("Server") {
                    TextField("Name", text: $appState.profile.name)
                    TextField("Server URL", text: $appState.profile.baseURL)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.URL)
                        .autocorrectionDisabled()

                    Picker("Auth", selection: $appState.profile.authType) {
                        ForEach(AuthType.allCases) { authType in
                            Text(authType.title).tag(authType)
                        }
                    }

                    if appState.profile.authType == .password {
                        TextField("Username", text: $appState.profile.username)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                        SecureField("Password", text: $appState.profile.password)
                    }

                    if appState.profile.authType == .apiKey {
                        SecureField("API Key", text: $appState.profile.apiKey)
                    }
                }

                Section("Custom Headers") {
                    ForEach($appState.profile.customHeaders) { $header in
                        VStack(alignment: .leading, spacing: 8) {
                            TextField("Header name", text: $header.name)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled()
                            SecureField("Header value", text: $header.value)
                            Button(role: .destructive) {
                                appState.removeHeader(header)
                            } label: {
                                Text("Remove Header")
                            }
                        }
                        .padding(.vertical, 4)
                    }

                    Button {
                        appState.addHeader()
                    } label: {
                        Label("Add Header", systemImage: "plus")
                    }
                }

                Section("Actions") {
                    Button("Test Connection") {
                        Task {
                            await appState.testConnection()
                        }
                    }
                    Button("Load Nodes") {
                        Task {
                            await appState.loadNodes()
                        }
                    }
                    if appState.isBusy {
                        ProgressView()
                    }
                    if !appState.statusMessage.isEmpty {
                        Text(appState.statusMessage)
                            .foregroundStyle(.secondary)
                    }
                }

                Section("Nodes") {
                    if appState.nodes.isEmpty {
                        Text("No nodes loaded")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(appState.nodes) { node in
                            NodeRow(node: node)
                        }
                    }
                }
            }
            .navigationTitle("Yanami")
        }
    }
}

struct NodeRow: View {
    let node: KomariNode

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(node.name)
                    .font(.headline)
                Spacer()
                Text(node.isOnline ? "Online" : "Offline")
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(node.isOnline ? Color.green.opacity(0.16) : Color.red.opacity(0.16))
                    .clipShape(Capsule())
            }

            Text([node.group, node.region, node.os].filter { !$0.isEmpty }.joined(separator: " / "))
                .font(.subheadline)
                .foregroundStyle(.secondary)

            ProgressView(value: min(max(node.cpu, 0), 100), total: 100) {
                Text("CPU \(node.cpu, specifier: "%.1f")%")
                    .font(.caption)
            }

            HStack {
                Text("RAM \(formatBytes(node.ram)) / \(formatBytes(node.ramTotal))")
                Spacer()
                Text("Net \(formatBytes(node.netIn))/s down")
            }
            .font(.caption)
            .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }

    private func formatBytes(_ value: Int64) -> String {
        ByteCountFormatter.string(fromByteCount: value, countStyle: .binary)
    }
}

struct KomariClient {
    let profile: ServerProfile

    func login() async throws -> String {
        guard !profile.username.isEmpty else {
            throw KomariClientError.invalidConfiguration("Username is required")
        }
        guard !profile.password.isEmpty else {
            throw KomariClientError.invalidConfiguration("Password is required")
        }

        let body: [String: Any] = [
            "username": profile.username,
            "password": profile.password
        ]
        let request = try makeRequest(path: "/api/login", method: "POST", jsonBody: body)
        let (_, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw KomariClientError.invalidResponse
        }
        guard (200...299).contains(httpResponse.statusCode) else {
            throw KomariClientError.httpStatus(httpResponse.statusCode)
        }
        guard let token = extractSessionToken(from: httpResponse) else {
            throw KomariClientError.invalidConfiguration("session_token was not returned")
        }
        return token
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
        return infos.map { uuid, info in
            let status = statuses[uuid]
            let name = info.name ?? ""
            let statusRamTotal = status?.ramTotal ?? 0
            let statusDiskTotal = status?.diskTotal ?? 0
            return KomariNode(
                id: uuid,
                name: name.isEmpty ? uuid : name,
                group: info.group ?? "",
                region: info.region ?? "",
                os: info.os ?? "",
                isOnline: status?.online ?? false,
                cpu: status?.cpu ?? 0,
                ram: status?.ram ?? 0,
                ramTotal: statusRamTotal > 0 ? statusRamTotal : info.memTotal ?? 0,
                disk: status?.disk ?? 0,
                diskTotal: statusDiskTotal > 0 ? statusDiskTotal : info.diskTotal ?? 0,
                netIn: status?.netIn ?? 0,
                netOut: status?.netOut ?? 0
            )
        }
        .sorted {
            if $0.isOnline != $1.isOnline {
                return $0.isOnline && !$1.isOnline
            }
            return $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
        }
    }

    private func rpc<T: Decodable>(
        method: String,
        token: String,
        responseType: T.Type
    ) async throws -> T {
        let request = try makeRequest(
            path: "/api/rpc2",
            method: "POST",
            jsonBody: [
                "jsonrpc": "2.0",
                "method": method,
                "id": 1
            ],
            token: token
        )
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw KomariClientError.invalidResponse
        }
        guard (200...299).contains(httpResponse.statusCode) else {
            throw KomariClientError.httpStatus(httpResponse.statusCode)
        }

        let envelope = try Self.decoder.decode(RpcEnvelope<T>.self, from: data)
        if let error = envelope.error {
            throw KomariClientError.rpc(error.message)
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
        request.setValue("Yanami-iPhone/1.0", forHTTPHeaderField: "User-Agent")
        profile.sanitizedCustomHeaders.forEach { header in
            request.setValue(header.value, forHTTPHeaderField: header.name)
        }

        if let token = token, !token.isEmpty {
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

    private static let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        return decoder
    }()
}

struct RpcEnvelope<Result: Decodable>: Decodable {
    let result: Result?
    let error: RpcErrorPayload?
}

struct RpcErrorPayload: Decodable {
    let message: String
}

struct VersionResult: Decodable {
    let version: String
}

struct NodeInfoPayload: Decodable {
    let name: String?
    let group: String?
    let region: String?
    let os: String?
    let memTotal: Int64?
    let diskTotal: Int64?
}

struct NodeStatusPayload: Decodable {
    let cpu: Double?
    let ram: Int64?
    let ramTotal: Int64?
    let disk: Int64?
    let diskTotal: Int64?
    let netIn: Int64?
    let netOut: Int64?
    let online: Bool?
}

enum KomariClientError: LocalizedError {
    case invalidConfiguration(String)
    case invalidResponse
    case httpStatus(Int)
    case rpc(String)

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
        }
    }
}

extension String {
    func trimmedTrailingSlash() -> String {
        var value = self
        while value.hasSuffix("/") {
            value.removeLast()
        }
        return value
    }
}

struct KeychainStore {
    let service: String

    func read<T: Decodable>(_ type: T.Type, account: String) -> T? {
        var query = baseQuery(account: account)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess,
              let data = item as? Data else {
            return nil
        }
        return try? JSONDecoder().decode(type, from: data)
    }

    func save<T: Encodable>(_ value: T, account: String) throws {
        let data = try JSONEncoder().encode(value)
        let query = baseQuery(account: account)
        SecItemDelete(query as CFDictionary)

        var item = query
        item[kSecValueData as String] = data
        item[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        SecItemAdd(item as CFDictionary, nil)
    }

    private func baseQuery(account: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }
}
