import SwiftUI

struct NodeDetailView: View {
    @EnvironmentObject private var store: AppStore
    let nodeId: String

    var body: some View {
        List {
            if store.nodeDetail.isLoading && store.nodeDetail.node == nil {
                ProgressView("Loading detail")
            } else if let error = store.nodeDetail.error {
                EmptyStateView(title: "Load Failed", systemImage: "exclamationmark.triangle", message: error)
            } else if let node = store.nodeDetail.node {
                Section {
                    NodeHeaderView(node: node)
                }

                Section("Live Resources") {
                    ResourceMeter(title: "CPU", value: node.cpuUsage / 100, label: Formatters.percent(node.cpuUsage))
                    ResourceMeter(
                        title: "RAM",
                        value: node.memTotal > 0 ? Double(node.memUsed) / Double(node.memTotal) : 0,
                        label: "\(Formatters.bytes(node.memUsed)) / \(Formatters.bytes(node.memTotal))"
                    )
                    ResourceMeter(
                        title: "Disk",
                        value: node.diskTotal > 0 ? Double(node.diskUsed) / Double(node.diskTotal) : 0,
                        label: "\(Formatters.bytes(node.diskUsed)) / \(Formatters.bytes(node.diskTotal))"
                    )
                    if let traffic = node.trafficUsage {
                        ResourceMeter(
                            title: "Traffic Limit",
                            value: traffic.percent,
                            label: "\(Formatters.bytes(traffic.used)) / \(Formatters.bytes(node.trafficLimit))"
                        )
                    }
                }

                Section("System") {
                    DetailLine("UUID", node.uuid)
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
                    RecordsSummaryView(records: store.nodeDetail.loadRecords)
                }

                Section("Ping") {
                    Picker("Range", selection: Binding(
                        get: { store.nodeDetail.pingHours },
                        set: { store.setPingHours($0) }
                    )) {
                        Text("1h").tag(1)
                        Text("6h").tag(6)
                        Text("24h").tag(24)
                    }
                    .pickerStyle(.segmented)
                    PingSummaryView(tasks: store.nodeDetail.pingTasks, records: store.nodeDetail.pingRecords)
                }

                Section("SSH Terminal") {
                    if store.activeServer?.authType == .guest {
                        Text("SSH terminal is disabled in guest mode.")
                            .foregroundStyle(.secondary)
                    } else {
                        TerminalPreviewView(node: node)
                    }
                }
            }
        }
        .navigationTitle(store.nodeDetail.node?.name ?? "Node Detail")
        .refreshable {
            await store.loadNodeDetail(uuid: nodeId)
        }
        .task {
            await store.loadNodeDetail(uuid: nodeId)
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
                StatusPill(isOnline: node.isOnline)
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

private struct RecordsSummaryView: View {
    let records: [LoadRecord]

    var body: some View {
        if records.isEmpty {
            Text("No load records")
                .foregroundStyle(.secondary)
        } else {
            let last = records.last
            VStack(alignment: .leading, spacing: 8) {
                DetailLine("Samples", "\(records.count)")
                DetailLine("Latest CPU", Formatters.percent(last?.cpu ?? 0))
                DetailLine("Latest RAM", Formatters.percent(last?.ramPercent ?? 0))
                DetailLine("Latest Disk", Formatters.percent(last?.diskPercent ?? 0))
            }
        }
    }
}

private struct PingSummaryView: View {
    let tasks: [PingTask]
    let records: [PingRecord]

    var body: some View {
        if tasks.isEmpty {
            Text("No ping tasks")
                .foregroundStyle(.secondary)
        } else {
            ForEach(tasks) { task in
                VStack(alignment: .leading, spacing: 4) {
                    Text(task.name)
                        .font(.headline)
                    Text("Latest \(Formatters.number(task.latest)) ms, avg \(Formatters.number(task.avg)) ms, loss \(Formatters.percent(task.loss))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            if !records.isEmpty {
                Text("\(records.count) ping samples loaded")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

private struct TerminalPreviewView: View {
    let node: KomariNode

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Terminal endpoint is available for \(node.name).")
            Text("Interactive ANSI terminal parity requires a signed device build and the server-side terminal WebSocket token flow.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }
}
