import WebKit
import Combine
import SwiftUI
import Charts

struct NodeDetailView: View {
    @EnvironmentObject private var store: AppStore
    let nodeId: String

    var body: some View {
        List {
            if store.nodeDetail.isLoading && store.nodeDetail.node == nil {
                ProgressView("Loading detail")
            } else if let node = store.nodeDetail.node {
                Section {
                    NodeHeaderView(node: node)
                }

                if let error = store.nodeDetail.error {
                    Section {
                        Label {
                            Text(error)
                        } icon: {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.orange)
                        }
                        .font(.caption)
                    }
                }

                Section("Live Resources") {
                    HStack {
                        CircularUsageIndicator(
                            label: "CPU",
                            percent: node.cpuUsage,
                            detail: node.cpuCores > 0 ? "\(node.cpuCores) Core" : ""
                        )
                        Spacer()
                        CircularUsageIndicator(
                            label: "RAM",
                            percent: node.memTotal > 0 ? Double(node.memUsed) / Double(node.memTotal) * 100 : 0,
                            detail: "\(Formatters.bytes(node.memUsed)) / \(Formatters.bytes(node.memTotal))"
                        )
                        Spacer()
                        CircularUsageIndicator(
                            label: "DISK",
                            percent: node.diskTotal > 0 ? Double(node.diskUsed) / Double(node.diskTotal) * 100 : 0,
                            detail: "\(Formatters.bytes(node.diskUsed)) / \(Formatters.bytes(node.diskTotal))"
                        )
                    }
                    .padding(.vertical, 8)
                    
                    if let traffic = node.trafficUsage {
                        ResourceMeter(
                            title: "Traffic Limit",
                            value: traffic.percent,
                            label: "\(Formatters.bytes(traffic.used)) / \(Formatters.bytes(node.trafficLimit))"
                        )
                    }
                }

                Section("System") {
                    DetailLine("UUID", store.settings.maskIpEnabled ? Formatters.maskIpOrUuid(node.uuid) : node.uuid)
                    DetailLine("CPU", [node.cpuName, node.cpuCores > 0 ? "\(node.cpuCores) cores" : ""].filter { !$0.isEmpty }.joined(separator: " / "))
                    DetailLine("Kernel", node.kernelVersion)
                    DetailLine("Virtualization", node.virtualization)
                    DetailLine("Arch", node.arch)
                    DetailLine("GPU", node.gpuName)
                    DetailLine("Uptime", Formatters.uptime(node.uptime))
                    DetailLine("Load", "\(Formatters.number(node.load1, digits: 2)) / \(Formatters.number(node.load5, digits: 2)) / \(Formatters.number(node.load15, digits: 2))")
                    DetailLine("Connections", "TCP \(node.connectionsTcp), UDP \(node.connectionsUdp)")
                    if let expiredAt = node.expiredAt, !expiredAt.isEmpty {
                        DetailLine("Expires", expiredAt)
                    }
                }

                Section("Load Records") {
                    Picker("Range", selection: Binding(
                        get: { store.nodeDetail.loadHours },
                        set: { store.setLoadHours($0) }
                    )) {
                        Text("Realtime").tag(0)
                        Text("1h").tag(1)
                        Text("6h").tag(6)
                        Text("24h").tag(24)
                    }
                    .pickerStyle(.segmented)
                    
                    if !store.nodeDetail.loadRecords.isEmpty {
                        LoadChart(records: store.nodeDetail.loadRecords)
                            .frame(height: 160)
                            .padding(.vertical, 8)
                    } else {
                        Text("No load records")
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding()
                    }
                }

                Section("Ping Monitoring") {
                    Picker("Range", selection: Binding(
                        get: { store.nodeDetail.pingHours },
                        set: { store.setPingHours($0) }
                    )) {
                        Text("1h").tag(1)
                        Text("6h").tag(6)
                        Text("24h").tag(24)
                    }
                    .pickerStyle(.segmented)
                    
                    if !store.nodeDetail.pingTasks.isEmpty {
                        ForEach(store.nodeDetail.pingTasks) { task in
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(task.name)
                                        .font(.subheadline.bold())
                                    Spacer()
                                    Text("\(Formatters.number(task.latest))ms / \(Formatters.percent(task.loss))")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                
                                let taskRecords = store.nodeDetail.pingRecords.filter { $0.taskId == task.id }
                                if !taskRecords.isEmpty {
                                    PingChart(records: taskRecords)
                                        .frame(height: 100)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    } else {
                        Text("No ping tasks")
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding()
                    }
                }
            } else if let error = store.nodeDetail.error {
                EmptyStateView(title: "Load Failed", systemImage: "exclamationmark.triangle", message: error)
            }
        }
        .navigationTitle(store.nodeDetail.node?.name ?? "Node Detail")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                if let server = store.activeServer, let node = store.nodeDetail.node, node.isOnline, server.authType != .guest {
                    NavigationLink {
                        TerminalNavigationWrapper(uuid: node.uuid, server: server)
                    } label: {
                        Image(systemName: "terminal")
                    }
                }
            }
        }
        .refreshable {
            await store.loadNodeDetail(uuid: nodeId)
        }
        .task {
            await store.loadNodeDetail(uuid: nodeId)
        }
    }
}

private struct LoadChart: View {
    let records: [LoadRecord]
    
    var body: some View {
        Chart {
            ForEach(records) { record in
                LineMark(
                    x: .value("Time", parseISO8601(record.time)),
                    y: .value("CPU", record.cpu),
                    series: .value("Metric", "CPU")
                )
                .foregroundStyle(.blue)
                
                LineMark(
                    x: .value("Time", parseISO8601(record.time)),
                    y: .value("RAM", record.ramPercent),
                    series: .value("Metric", "RAM")
                )
                .foregroundStyle(.green)
            }
        }
        .chartYScale(domain: 0...100)
        .chartXAxis {
            if records.count > 0 {
                AxisMarks(values: .stride(by: .hour)) { value in
                    AxisGridLine()
                    AxisTick()
                    AxisValueLabel(format: .dateTime.hour().minute())
                }
            }
        }
    }
}

private struct PingChart: View {
    let records: [PingRecord]
    
    var body: some View {
        Chart {
            ForEach(records) { record in
                LineMark(
                    x: .value("Time", parseISO8601(record.time)),
                    y: .value("Latency", record.value)
                )
                .foregroundStyle(.orange)
            }
        }
        .chartXAxis(.hidden)
        .chartYAxis {
            AxisMarks(position: .leading)
        }
    }
}

private func parseISO8601(_ string: String) -> Date {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    return formatter.date(from: string) ?? Date()
}


private struct TerminalNavigationWrapper: View {
    let uuid: String
    let server: ServerProfile
    @EnvironmentObject private var store: AppStore
    @State private var token: String?
    @State private var error: String?

    var body: some View {
        Group {
            if let token = token {
                SshTerminalView(viewModel: SshTerminalViewModel(uuid: uuid, server: server, token: token))
            } else if let error = error {
                VStack {
                    Image(systemName: "exclamationmark.triangle")
                        .foregroundColor(.red)
                    Text(error)
                        .padding()
                    Button("Retry") {
                        Task { await resolveToken() }
                    }
                }
            } else {
                ProgressView("Preparing terminal...")
            }
        }
        .task {
            await resolveToken()
        }
    }

    private func resolveToken() async {
        do {
            let client = KomariClient(profile: server)
            switch server.authType {
            case .guest:
                error = "Guest mode not supported"
            case .apiKey:
                token = server.apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
            case .password:
                if !server.sessionToken.isEmpty {
                    token = server.sessionToken
                } else {
                    token = try await client.login()
                }
            }
        } catch {
            self.error = error.localizedDescription
        }
    }
}

private struct NodeHeaderView: View {
    let node: KomariNode

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(node.name)
                    .font(.title3.bold())
                Spacer()
                StatusBadge(isOnline: node.isOnline)
            }
            Text([node.region, node.group, node.os].filter { !$0.isEmpty }.joined(separator: " / "))
                .foregroundStyle(.secondary)
            HStack {
                Label(Formatters.rate(node.netIn), systemImage: "arrow.down")
                Spacer()
                Label(Formatters.rate(node.netOut), systemImage: "arrow.up")
            }
            .font(.caption)
        }
    }
}
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
import SwiftUI
import WebKit

struct SshTerminalView: View {
    @StateObject var viewModel: SshTerminalViewModel
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss
    
    @State private var showingSnippets = false
    
    var body: some View {
        VStack(spacing: 0) {
            if viewModel.isConnecting {
                VStack {
                    ProgressView()
                    Text("Connecting to terminal...")
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.error {
                VStack {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.largeTitle)
                        .foregroundColor(.red)
                    Text(error)
                        .padding()
                    Button("Retry") {
                        viewModel.connect()
                    }
                    .buttonStyle(.bordered)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                TerminalWebView(viewModel: viewModel)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            
            keyboardAccessoryBar
        }
        .navigationTitle("Terminal")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showingSnippets = true
                } label: {
                    Image(systemName: "command")
                }
            }
        }
        .onAppear {
            viewModel.connect()
        }
        .onDisappear {
            viewModel.disconnect()
        }
        .sheet(isPresented: $showingSnippets) {
            SnippetPickerView(snippets: store.settings.snippets) { snippet in
                viewModel.sendText(snippet.content)
                if snippet.appendEnter {
                    viewModel.sendText("\r")
                }
            }
        }
    }
    
    private var keyboardAccessoryBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                TerminalKeyButton(title: "Ctrl", isHighlighted: viewModel.ctrlActive) {
                    viewModel.ctrlActive.toggle()
                }
                TerminalKeyButton(title: "Alt", isHighlighted: viewModel.altActive) {
                    viewModel.altActive.toggle()
                }
                TerminalKeyButton(title: "Esc") { viewModel.sendText("\u{1b}") }
                TerminalKeyButton(title: "Tab") { viewModel.sendText("\t") }
                TerminalKeyButton(title: "↑") { viewModel.sendText("\u{1b}[A") }
                TerminalKeyButton(title: "↓") { viewModel.sendText("\u{1b}[B") }
                TerminalKeyButton(title: "←") { viewModel.sendText("\u{1b}[D") }
                TerminalKeyButton(title: "→") { viewModel.sendText("\u{1b}[C") }
            }
            .padding(8)
        }
        .background(.thinMaterial)
    }
}

struct TerminalKeyButton: View {
    let title: String
    var isHighlighted: Bool = false
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(.subheadline, design: .monospaced))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isHighlighted ? Color.accentColor : Color.secondary.opacity(0.2))
                .foregroundColor(isHighlighted ? .white : .primary)
                .cornerRadius(6)
        }
    }
}

struct TerminalWebView: UIViewRepresentable {
    @ObservedObject var viewModel: SshTerminalViewModel
    
    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.isOpaque = false
        webView.backgroundColor = .black
        webView.scrollView.isScrollEnabled = false
        
        let html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/xterm@5.3.0/css/xterm.css" />
            <script src="https://cdn.jsdelivr.net/npm/xterm@5.3.0/lib/xterm.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/xterm-addon-fit@0.8.0/lib/xterm-addon-fit.js"></script>
            <style>
                body { margin: 0; background: #000; overflow: hidden; }
                #terminal { height: 100vh; width: 100vw; }
            </style>
        </head>
        <body>
            <div id="terminal"></div>
            <script>
                const term = new Terminal({
                    cursorBlink: true,
                    fontSize: 14,
                    fontFamily: 'Menlo, Monaco, "Courier New", monospace',
                    theme: { background: '#000' }
                });
                const fitAddon = new FitAddon.FitAddon();
                term.loadAddon(fitAddon);
                term.open(document.getElementById('terminal'));
                
                // Initial fit and report
                setTimeout(() => {
                    fitAddon.fit();
                    window.webkit.messageHandlers.terminalResize.postMessage({
                        cols: term.cols,
                        rows: term.rows
                    });
                }, 100);

                term.onData(data => {
                    window.webkit.messageHandlers.terminalInput.postMessage(data);
                });

                window.addEventListener('resize', () => {
                    fitAddon.fit();
                    window.webkit.messageHandlers.terminalResize.postMessage({
                        cols: term.cols,
                        rows: term.rows
                    });
                });

                function writeToTerminal(data) {
                    term.write(new Uint8Array(data));
                }
            </script>
        </body>
        </html>
        """
        
        webView.loadHTMLString(html, baseURL: nil)
        
        // Setup message handlers
        let controller = webView.configuration.userContentController
        controller.add(context.coordinator, name: "terminalInput")
        controller.add(context.coordinator, name: "terminalResize")
        
        context.coordinator.webView = webView
        
        return webView
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(viewModel: viewModel)
    }
    
    class Coordinator: NSObject, WKScriptMessageHandler {
        var viewModel: SshTerminalViewModel
        weak var webView: WKWebView?
        private var cancellables = Set<AnyCancellable>()
        
        init(viewModel: SshTerminalViewModel) {
            self.viewModel = viewModel
            super.init()
            
            NotificationCenter.default.publisher(for: .sshTerminalOutput)
                .sink { [weak self] notification in
                    if let data = notification.object as? Data {
                        self?.writeToWebView(data)
                    }
                }
                .store(in: &cancellables)
        }
        
        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            if message.name == "terminalInput", let data = message.body as? String {
                handleInput(data)
            } else if message.name == "terminalResize", let body = message.body as? [String: Int] {
                viewModel.sendResize(cols: body["cols"] ?? 80, rows: body["rows"] ?? 24)
            }
        }
        
        private func handleInput(_ input: String) {
            var finalInput = input
            if viewModel.ctrlActive {
                if let firstChar = input.first, firstChar.isLetter {
                    let ctrlChar = Character(UnicodeScalar(UInt32(firstChar.uppercased().unicodeScalars.first!.value) - 64)!)
                    finalInput = String(ctrlChar)
                }
                viewModel.ctrlActive = false
            } else if viewModel.altActive {
                finalInput = "\u{1b}" + input
                viewModel.altActive = false
            }
            viewModel.sendText(finalInput)
        }
        
        private func writeToWebView(_ data: Data) {
            let bytes = [UInt8](data)
            let script = "writeToTerminal(\(bytes))"
            DispatchQueue.main.async {
                self.webView?.evaluateJavaScript(script)
            }
        }
    }
}

struct SnippetPickerView: View {
    let snippets: [TerminalSnippet]
    let onSelect: (TerminalSnippet) -> Void
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationStack {
            List(snippets) { snippet in
                Button {
                    onSelect(snippet)
                    dismiss()
                } label: {
                    VStack(alignment: .leading) {
                        Text(snippet.title)
                            .font(.headline)
                        Text(snippet.content)
                            .font(.caption)
                            .lineLimit(1)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle("Snippets")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}
