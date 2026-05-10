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
            AxisMarks(values: .stride(by: .hour)) { value in
                AxisGridLine()
                AxisTick()
                AxisValueLabel(format: .dateTime.hour().minute())
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
