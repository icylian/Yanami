import SwiftUI

struct NodeListView: View {
    let server: ServerInstance
    @StateObject private var viewModel: NodeListViewModel
    
    init(server: ServerInstance) {
        self.server = server
        _viewModel = StateObject(wrappedValue: NodeListViewModel(server: server))
    }

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.nodes.isEmpty {
                ProgressView()
            } else if let error = viewModel.errorMessage, viewModel.nodes.isEmpty {
                VStack {
                    Text("Error").font(.headline)
                    Text(error).foregroundColor(.red).multilineTextAlignment(.center).padding()
                }
            } else {
                List(viewModel.nodes, id: \.node.id) { info in
                    NavigationLink(destination: NodeDetailView(nodeInfo: info, server: server)) {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(info.node.name).font(.headline)
                                Text(info.node.ip ?? "Unknown IP").font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing) {
                                Text(String(format: "CPU: %.1f%%", info.status.cpuUsage))
                                    .font(.caption)
                                let memPercent = info.status.memoryTotal > 0 ? Double(info.status.memoryUsed) / Double(info.status.memoryTotal) * 100 : 0
                                Text(String(format: "MEM: %.1f%%", memPercent))
                                    .font(.caption)
                            }
                        }
                    }
                }
                .refreshable {
                    viewModel.startRefreshing()
                }
            }
        }
        .navigationTitle(server.name)
        .onAppear {
            viewModel.startRefreshing()
        }
        .onDisappear {
            viewModel.stopRefreshing()
        }
    }
}
