import SwiftUI

class AdminStore: ObservableObject {
    @Published var clients: [ManagedClient] = []
    @Published var pingTasks: [AdminPingTask] = []
    @Published var isLoading = false
    @Published var error: String?
    
    let server: ServerProfile
    private let token: String
    private let client: KomariClient
    
    init(server: ServerProfile, token: String) {
        self.server = server
        self.token = token
        self.client = KomariClient(profile: server)
    }
    
    @MainActor
    func loadData() async {
        isLoading = true
        error = nil
        do {
            async let fetchClients = client.getClients(token: token)
            async let fetchPingTasks = client.getPingTasks(token: token)
            
            let (fetchedClients, fetchedPingTasks) = try await (fetchClients, fetchPingTasks)
            clients = fetchedClients
            pingTasks = fetchedPingTasks
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
    
    @MainActor
    func deleteClient(uuid: String) async {
        do {
            try await client.deleteClient(token: token, uuid: uuid)
            clients.removeAll { $0.uuid == uuid }
        } catch {
            self.error = error.localizedDescription
        }
    }
    
    @MainActor
    func deletePingTask(id: Int) async {
        do {
            try await client.deletePingTask(token: token, ids: [id])
            pingTasks.removeAll { $0.id == id }
        } catch {
            self.error = error.localizedDescription
        }
    }
    
    @MainActor
    func fetchClientToken(uuid: String) async -> String? {
        do {
            return try await client.getClientToken(token: token, uuid: uuid)
        } catch {
            self.error = error.localizedDescription
            return nil
        }
    }
}

struct AdminView: View {
    @StateObject var store: AdminStore
    @State private var showingTokenAlert = false
    @State private var selectedToken = ""
    
    var body: some View {
        List {
            if store.isLoading {
                ProgressView("Loading admin data...")
            } else if let error = store.error {
                EmptyStateView(title: "Error", systemImage: "exclamationmark.triangle", message: error)
            } else {
                Section("Clients") {
                    if store.clients.isEmpty {
                        Text("No clients found")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(store.clients) { client in
                            VStack(alignment: .leading) {
                                Text(client.name ?? "Unnamed")
                                    .font(.headline)
                                Text(client.uuid)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            .swipeActions {
                                Button("Delete", role: .destructive) {
                                    Task { await store.deleteClient(uuid: client.uuid) }
                                }
                                Button("Token") {
                                    Task {
                                        if let token = await store.fetchClientToken(uuid: client.uuid) {
                                            selectedToken = token
                                            showingTokenAlert = true
                                        }
                                    }
                                }
                                .tint(.blue)
                            }
                        }
                    }
                }
                
                Section("Ping Tasks") {
                    if store.pingTasks.isEmpty {
                        Text("No ping tasks found")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(store.pingTasks) { task in
                            VStack(alignment: .leading) {
                                Text(task.name)
                                    .font(.headline)
                                Text("\(task.type): \(task.target)")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            .swipeActions {
                                Button("Delete", role: .destructive) {
                                    Task { await store.deletePingTask(id: task.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Server Admin")
        .refreshable {
            await store.loadData()
        }
        .task {
            if store.clients.isEmpty && store.pingTasks.isEmpty {
                await store.loadData()
            }
        }
        .alert("Client Token", isPresented: $showingTokenAlert) {
            Button("Copy", role: .cancel) {
                #if os(iOS)
                UIPasteboard.general.string = selectedToken
                #endif
            }
        } message: {
            Text(selectedToken)
        }
    }
}
