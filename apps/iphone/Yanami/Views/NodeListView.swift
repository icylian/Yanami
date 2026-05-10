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
                    Picker("Status", selection: $store.statusFilter) {
                        ForEach(StatusFilter.allCases) { filter in
                            Text(filter.title).tag(filter)
                        }
                    }
                    .pickerStyle(.segmented)
                    .frame(width: 240)
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

            HStack {
                Label(Formatters.rate(store.totalNetIn), systemImage: "arrow.down")
                Spacer()
                Label(Formatters.rate(store.totalNetOut), systemImage: "arrow.up")
            }
            .font(.caption)
            .foregroundStyle(.secondary)

            if !store.statusMessage.isEmpty {
                Text(store.statusMessage)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

private struct NodeRowView: View {
    let node: KomariNode

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(node.name)
                        .font(.headline)
                    Text([node.region, node.group, node.os].filter { !$0.isEmpty }.joined(separator: " / "))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                StatusPill(isOnline: node.isOnline)
            }

            ResourceMeter(title: "CPU", value: node.cpuUsage / 100, label: Formatters.percent(node.cpuUsage))

            HStack {
                Text("RAM \(Formatters.bytes(node.memUsed)) / \(Formatters.bytes(node.memTotal))")
                Spacer()
                Text("Up \(Formatters.rate(node.netOut))")
            }
            .font(.caption)
            .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}
