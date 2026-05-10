import Foundation
import Combine

@MainActor
final class SshTerminalViewModel: ObservableObject {
    @Published var isConnecting = true
    @Published var isConnected = false
    @Published var error: String?
    @Published var ctrlActive = false
    @Published var altActive = false
    
    private let uuid: String
    private let server: ServerProfile
    private let token: String
    private var webSocketTask: URLSessionWebSocketTask?
    private var heartbeatTimer: Timer?
    
    init(uuid: String, server: ServerProfile, token: String) {
        self.uuid = uuid
        self.server = server
        self.token = token
    }
    
    func connect() {
        guard let url = buildTerminalURL() else {
            self.error = "Invalid terminal URL"
            self.isConnecting = false
            return
        }
        
        var request = URLRequest(url: url)
        request.setValue("session_token=\(token)", forHTTPHeaderField: "Cookie")
        server.sanitizedCustomHeaders.forEach { header in
            request.setValue(header.value, forHTTPHeaderField: header.name)
        }
        
        let session = URLSession(configuration: .default)
        webSocketTask = session.webSocketTask(for: request)
        webSocketTask?.resume()
        
        self.isConnecting = true
        self.isConnected = false
        self.error = nil
        
        receiveMessage()
        startHeartbeat()
    }
    
    func disconnect() {
        heartbeatTimer?.invalidate()
        webSocketTask?.cancel(with: .normalClosure, reason: nil)
        isConnected = false
    }
    
    func sendInput(_ data: Data) {
        let message = URLSessionWebSocketTask.Message.data(data)
        webSocketTask?.send(message) { error in
            if let error = error {
                print("WebSocket send error: \(error)")
            }
        }
    }
    
    func sendText(_ text: String) {
        guard let data = text.data(using: .utf8) else { return }
        sendInput(data)
    }
    
    func sendResize(cols: Int, rows: Int) {
        let json: [String: Any] = [
            "type": "resize",
            "cols": cols,
            "rows": rows
        ]
        if let data = try? JSONSerialization.data(withJSONObject: json),
           let text = String(data: data, encoding: .utf8) {
            webSocketTask?.send(.string(text)) { _ in }
        }
    }
    
    private func receiveMessage() {
        webSocketTask?.receive { [weak self] result in
            guard let self = self else { return }
            Task { @MainActor in
                switch result {
                case .success(let message):
                    switch message {
                    case .string(let text):
                        self.handleTextMessage(text)
                    case .data(let data):
                        self.handleDataMessage(data)
                    @unknown default:
                        break
                    }
                    self.receiveMessage()
                case .failure(let error):
                    self.isConnected = false
                    self.error = error.localizedDescription
                }
            }
        }
    }
    
    private func handleTextMessage(_ text: String) {
        // Handle potential JSON messages from server if any
        if !isConnected {
            isConnected = true
            isConnecting = false
        }
    }
    
    private func handleDataMessage(_ data: Data) {
        if !isConnected {
            isConnected = true
            isConnecting = false
        }
        // This will be forwarded to the WebView
        NotificationCenter.default.post(name: .sshTerminalOutput, object: data)
    }
    
    private func startHeartbeat() {
        heartbeatTimer?.invalidate()
        heartbeatTimer = Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { [weak self] _ in
            Task { @MainActor in
                let json = ["type": "heartbeat"]
                if let data = try? JSONSerialization.data(withJSONObject: json),
                   let text = String(data: data, encoding: .utf8) {
                    self?.webSocketTask?.send(.string(text)) { _ in }
                }
            }
        }
    }
    
    private func buildTerminalURL() -> URL? {
        var base = server.normalizedBaseURL
        if base.hasPrefix("https://") {
            base = "wss://" + base.dropFirst(8)
        } else if base.hasPrefix("http://") {
            base = "ws://" + base.dropFirst(7)
        }
        return URL(string: "\(base)/api/admin/client/\(uuid)/terminal")
    }
}

extension NSNotification.Name {
    static let sshTerminalOutput = NSNotification.Name("sshTerminalOutput")
}
