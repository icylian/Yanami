import SwiftUI

struct NodeListView: View {
    @EnvironmentObject private var store: AppStore

    var body: some View {
        NavigationStack {
            List {
                if store.activeServer == nil {
                    EmptyStateView(title: "No Active Server", systemImage: "server.rack", message: "Select or add a Komari instance first.")
                } else {
                    Section {
                        NodeSummaryView()
                    }

                    if !store.groups.isEmpty {
                        Section("Groups") {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack {
                                    FilterChip(title: "All", isSelected: store.selectedGroup == nil) {
                                        store.selectedGroup = nil
                                    }
                                    ForEach(store.groups, id: \.self) { group in
                                        FilterChip(title: group, isSelected: store.selectedGroup == group) {
                                            store.selectedGroup = group
                                        }
                                    }
                                }
                                .padding(.vertical, 4)
                            }
                        }
                    }

                    Section("Nodes") {
                        if store.isLoadingNodes {
                            ProgressView("Loading nodes")
                        } else if store.filteredNodes.isEmpty {
                            EmptyStateView(title: "No Nodes", systemImage: "desktopcomputer", message: "No nodes match the current filters.")
                        } else {
                            ForEach(store.filteredNodes) { node in
                                NavigationLink {
                                    NodeDetailView(nodeId: node.uuid)
                                } label: {
                                    NodeRowView(node: node)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle(store.activeServer?.name ?? "Nodes")
            .searchable(text: $store.searchQuery, prompt: "Search node, UUID, group")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    HStack {
                        if let server = store.activeServer, server.authType != .guest {
                            NavigationLink {
                                AdminNavigationWrapper(server: server)
                            } label: {
                                Image(systemName: "server.rack")
                            }
                        }
                        
                        Picker("Status", selection: $store.statusFilter) {
                            ForEach(StatusFilter.allCases) { filter in
                                Text(filter.title).tag(filter)
                            }
                        }
                        .pickerStyle(.segmented)
                        .frame(width: 180)
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        Task { await store.loadNodes(mode: .refresh) }
                    } label: {
                        if store.isRefreshingNodes {
                            ProgressView()
                        } else {
                            Image(systemName: "arrow.clockwise")
                        }
                    }
                }
            }
            .refreshable {
                await store.loadNodes(mode: .refresh)
            }
            .task {
                if store.activeServer != nil && store.nodes.isEmpty {
                    await store.loadNodes(mode: .initial)
                }
            }
        }
    }
}

private struct NodeSummaryView: View {
    @EnvironmentObject private var store: AppStore

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                MetricView(title: "Total", value: "\(store.nodes.count)")
                MetricView(title: "Online", value: "\(store.onlineCount)")
                MetricView(title: "Offline", value: "\(store.offlineCount)")
            }

            LazyVGrid(
                columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 3),
                alignment: .leading,
                spacing: 10
            ) {
                SummaryMetricView(
                    title: "Realtime Speed",
                    value: Formatters.rate(store.totalNetIn + store.totalNetOut),
                    systemImage: "arrow.up.arrow.down"
                )
                SummaryMetricView(
                    title: "Upload Speed",
                    value: Formatters.rate(store.totalNetOut),
                    systemImage: "arrow.up"
                )
                SummaryMetricView(
                    title: "Download Speed",
                    value: Formatters.rate(store.totalNetIn),
                    systemImage: "arrow.down"
                )
                SummaryMetricView(
                    title: "Total Traffic",
                    value: Formatters.bytes(store.totalTrafficUp + store.totalTrafficDown),
                    systemImage: "chart.bar"
                )
                SummaryMetricView(
                    title: "Upload Traffic",
                    value: Formatters.bytes(store.totalTrafficUp),
                    systemImage: "arrow.up.circle"
                )
                SummaryMetricView(
                    title: "Download Traffic",
                    value: Formatters.bytes(store.totalTrafficDown),
                    systemImage: "arrow.down.circle"
                )
            }

            if !store.statusMessage.isEmpty {
                Text(store.statusMessage)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

private struct SummaryMetricView: View {
    let title: LocalizedStringKey
    let value: String
    let systemImage: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Label {
                Text(title)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
            } icon: {
                Image(systemName: systemImage)
                    .imageScale(.small)
            }
            .font(.caption2)
            .foregroundStyle(.secondary)

            Text(value)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .monospacedDigit()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct NodeRowView: View {
    let node: KomariNode

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header: Region, Name, Badges
            HStack(spacing: 8) {
                Text(node.region.isEmpty ? "🌐" : node.region)
                    .font(.subheadline)
                
                Text(node.name)
                    .font(.subheadline.bold())
                    .lineLimit(1)
                
                Spacer()
                
                if node.isOnline {
                    UptimeBadge(uptime: node.uptime)
                }
                
                if let expiredAt = node.expiredAt, !expiredAt.isEmpty {
                    ExpiryBadge(dateString: expiredAt)
                }
                
                StatusBadge(isOnline: node.isOnline)
            }

            if node.isOnline {
                // Circular Indicators
                HStack(alignment: .top) {
                    CircularUsageIndicator(
                        label: "CPU",
                        percent: node.cpuUsage,
                        detail: node.cpuCores > 0 ? "\(node.cpuCores) Core" : ""
                    )
                    Spacer()
                    CircularUsageIndicator(
                        label: "RAM",
                        percent: node.memTotal > 0 ? Double(node.memUsed) / Double(node.memTotal) * 100 : 0,
                        detail: Formatters.bytes(node.memTotal)
                    )
                    Spacer()
                    CircularUsageIndicator(
                        label: "DISK",
                        percent: node.diskTotal > 0 ? Double(node.diskUsed) / Double(node.diskTotal) * 100 : 0,
                        detail: Formatters.bytes(node.diskTotal)
                    )
                    
                    if let traffic = node.trafficUsage {
                        Spacer()
                        VStack(spacing: 4) {
                            ZStack {
                                Circle()
                                    .stroke(Color.gray.opacity(0.2), lineWidth: 5)
                                Circle()
                                    .trim(from: 0, to: CGFloat(traffic.percent))
                                    .stroke(Color.purple, style: StrokeStyle(lineWidth: 5, lineCap: .round))
                                    .rotationEffect(.degrees(-90))
                                
                                Text("\(Int(traffic.percent * 100))%")
                                    .font(.system(size: 10, weight: .bold))
                            }
                            .frame(width: 44, height: 44)
                            Text("TRAFFIC")
                                .font(.system(size: 8, weight: .medium))
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                .padding(.vertical, 4)

                // Traffic speeds and total
                HStack {
                    HStack(spacing: 4) {
                        Image(systemName: "arrow.up")
                        Text(Formatters.rate(node.netOut))
                        Text("(\(Formatters.bytes(node.netTotalUp)))")
                            .font(.system(size: 10))
                    }
                    Spacer()
                    HStack(spacing: 4) {
                        Image(systemName: "arrow.down")
                        Text(Formatters.rate(node.netIn))
                        Text("(\(Formatters.bytes(node.netTotalDown)))")
                            .font(.system(size: 10))
                    }
                }
                .font(.caption)
                .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 8)
    }
}

private struct AdminNavigationWrapper: View {
    let server: ServerProfile
    @State private var token: String?
    @State private var error: String?

    var body: some View {
        Group {
            if let token = token {
                AdminView(store: AdminStore(server: server, token: token))
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
                ProgressView("Preparing admin panel...")
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
