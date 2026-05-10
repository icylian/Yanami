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
