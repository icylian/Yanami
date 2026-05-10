import SwiftUI

struct ServerListView: View {
    @EnvironmentObject private var store: AppStore
    @State private var editingServer: ServerProfile?
    @State private var isAdding = false

    var body: some View {
        NavigationStack {
            List {
                if store.servers.isEmpty {
                    EmptyStateView(title: "No Servers", systemImage: "server.rack", message: "Add a Komari instance to begin.")
                } else {
                    ForEach(store.servers) { server in
                        Button {
                            store.selectServer(server)
                        } label: {
                            ServerRow(server: server, isActive: store.activeServerId == server.id)
                        }
                        .swipeActions {
                            Button {
                                editingServer = server
                            } label: {
                                Label("Edit", systemImage: "pencil")
                            }
                            Button(role: .destructive) {
                                store.deleteServer(server)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
            }
            .navigationTitle("Servers")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        isAdding = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $isAdding) {
                ServerFormView(server: ServerProfile()) { server in
                    store.addServer(server)
                    isAdding = false
                }
            }
            .sheet(item: $editingServer) { server in
                ServerFormView(server: server) { updated in
                    store.updateServer(updated)
                    editingServer = nil
                }
            }
        }
    }
}

private struct ServerRow: View {
    let server: ServerProfile
    let isActive: Bool

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: isActive ? "checkmark.circle.fill" : "circle")
                .foregroundStyle(isActive ? .green : .secondary)
            VStack(alignment: .leading, spacing: 4) {
                Text(server.name)
                    .font(.headline)
                Text(server.normalizedBaseURL)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(server.authType.title)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            if !server.sanitizedCustomHeaders.isEmpty {
                Image(systemName: "lock.shield")
                    .foregroundStyle(.blue)
            }
        }
        .contentShape(Rectangle())
    }
}
