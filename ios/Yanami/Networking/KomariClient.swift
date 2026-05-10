import Foundation

final class KomariClient {
    let server: ServerInstance
    private let urlSession: URLSession

    init(server: ServerInstance) {
        self.server = server
        let config = URLSessionConfiguration.default
        self.urlSession = URLSession(configuration: config)
    }

    private func makeRequest<T: Encodable>(method: String, payload: T? = nil) throws -> URLRequest {
        guard let url = URL(string: server.baseURL)?.appendingPathComponent("rpc") else {
            throw URLError(.badURL)
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        for header in server.customHeaders {
            request.setValue(header.value, forHTTPHeaderField: header.key)
        }

        if server.authType == .basic {
            let password = KeychainStore.shared.loadSecret(for: server.id) ?? ""
            let authString = "\(server.username):\(password)"
            if let authData = authString.data(using: .utf8) {
                let base64AuthString = authData.base64EncodedString()
                request.setValue("Basic \(base64AuthString)", forHTTPHeaderField: "Authorization")
            }
        } else if server.authType == .token {
            let token = KeychainStore.shared.loadSecret(for: server.id) ?? ""
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        struct Body<P: Encodable>: Encodable {
            let method: String
            let payload: P?
        }
        let body = Body(method: method, payload: payload)
        request.httpBody = try? JSONEncoder().encode(body)
        
        return request
    }

    private func makeRequest(method: String) throws -> URLRequest {
        return try makeRequest<String>(method: method, payload: nil)
    }

    func getVersion() async throws -> String {
        let request = try makeRequest(method: "common:getVersion")
        let (data, _) = try await urlSession.data(for: request)
        let response = try JSONDecoder().decode(RpcResponse<String>.self, from: data)
        return response.data ?? "Unknown"
    }

    func getNodes() async throws -> [NodeInfoPayload] {
        let request = try makeRequest(method: "common:getNodes")
        let (data, _) = try await urlSession.data(for: request)
        let response = try JSONDecoder().decode(RpcResponse<[NodeInfoPayload]>.self, from: data)
        return response.data ?? []
    }

    func executeTerminal(nodeId: String, command: String) async throws -> String {
        let payload = TerminalCommandPayload(nodeId: nodeId, command: command)
        let request = try makeRequest(method: "terminal:execute", payload: payload)
        let (data, _) = try await urlSession.data(for: request)
        let response = try JSONDecoder().decode(RpcResponse<String>.self, from: data)
        return response.data ?? ""
    }
}
