import SwiftUI

struct NodeDetailView: View {
    let nodeInfo: NodeInfoPayload
    let server: ServerInstance
    
    var body: some View {
        List {
            Section("System Info") {
                HStack {
                    Text("OS")
                    Spacer()
                    Text(nodeInfo.node.os ?? "Unknown").foregroundColor(.secondary)
                }
                HStack {
                    Text("IP")
                    Spacer()
                    Text(nodeInfo.node.ip ?? "Unknown").foregroundColor(.secondary)
                }
            }
            
            Section("Status") {
                VStack(alignment: .leading) {
                    Text("CPU Usage")
                    ProgressView(value: min(max(nodeInfo.status.cpuUsage / 100.0, 0.0), 1.0))
                        .progressViewStyle(.linear)
                        .tint(.blue)
                    Text(String(format: "%.1f%%", nodeInfo.status.cpuUsage)).font(.caption)
                }
                
                VStack(alignment: .leading) {
                    Text("Memory Usage")
                    let memUsed = Double(nodeInfo.status.memoryUsed) / 1024 / 1024
                    let memTotal = Double(nodeInfo.status.memoryTotal) / 1024 / 1024
                    let memFraction = nodeInfo.status.memoryTotal > 0 ? Double(nodeInfo.status.memoryUsed) / Double(nodeInfo.status.memoryTotal) : 0
                    
                    ProgressView(value: min(max(memFraction, 0.0), 1.0))
                        .progressViewStyle(.linear)
                        .tint(.green)
                    Text(String(format: "%.0f MB / %.0f MB", memUsed, memTotal)).font(.caption)
                }
                
                HStack {
                    VStack(alignment: .leading) {
                        Text("Network Rx").font(.caption)
                        Text("\(nodeInfo.status.networkRx / 1024) KB").font(.headline)
                    }
                    Spacer()
                    VStack(alignment: .trailing) {
                        Text("Network Tx").font(.caption)
                        Text("\(nodeInfo.status.networkTx / 1024) KB").font(.headline)
                    }
                }
            }
            
            Section {
                NavigationLink(destination: TerminalView(server: server, nodeId: nodeInfo.node.id)) {
                    Label("Open Terminal", systemItem: "terminal")
                }
            }
        }
        .navigationTitle(nodeInfo.node.name)
    }
}
