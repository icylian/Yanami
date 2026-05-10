import SwiftUI

struct TerminalView: View {
    let server: ServerInstance
    let nodeId: String
    
    @State private var command: String = ""
    @State private var output: String = ""
    @State private var isExecuting: Bool = false
    
    private var client: KomariClient {
        KomariClient(server: server)
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                Text(output)
                    .font(.system(.body, design: .monospaced))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
            }
            .background(Color.black)
            .foregroundColor(.green)
            
            Divider()
            
            HStack {
                Text("$")
                    .font(.system(.body, design: .monospaced))
                    .foregroundColor(.secondary)
                TextField("Enter command...", text: $command)
                    .font(.system(.body, design: .monospaced))
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
                    .onSubmit {
                        executeCommand()
                    }
                
                Button(action: executeCommand) {
                    if isExecuting {
                        ProgressView()
                    } else {
                        Image(systemName: "return")
                    }
                }
                .disabled(command.isEmpty || isExecuting)
            }
            .padding()
            .background(Color(UIColor.secondarySystemBackground))
        }
        .navigationTitle("Terminal")
        .navigationBarTitleDisplayMode(.inline)
    }
    
    private func executeCommand() {
        guard !command.isEmpty else { return }
        let cmdToRun = command
        command = ""
        output += "\n$ \(cmdToRun)\n"
        isExecuting = true
        
        Task {
            do {
                let result = try await client.executeTerminal(nodeId: nodeId, command: cmdToRun)
                await MainActor.run {
                    output += result
                    isExecuting = false
                }
            } catch {
                await MainActor.run {
                    output += "Error: \(error.localizedDescription)\n"
                    isExecuting = false
                }
            }
        }
    }
}
