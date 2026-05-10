import SwiftUI
import SwiftData

struct ServerListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var servers: [ServerInstance]
    @StateObject private var viewModel = ServerListViewModel()
    @State private var showingAddServer = false

    var body: some View {
        List {
            ForEach(servers) { server in
                NavigationLink(value: server) {
                    VStack(alignment: .leading) {
                        Text(server.name).font(.headline)
                        Text(server.baseURL).font(.subheadline).foregroundColor(.secondary)
                    }
                }
            }
            .onDelete(perform: deleteServers)
        }
        .navigationTitle("Servers")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showingAddServer = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .navigationDestination(for: ServerInstance.self) { server in
            NodeListView(server: server)
        }
        .sheet(isPresented: $showingAddServer) {
            AddServerView()
        }
    }

    private func deleteServers(offsets: IndexSet) {
        withAnimation {
            for index in offsets {
                viewModel.delete(server: servers[index], modelContext: modelContext)
            }
        }
    }
}

struct AddServerView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    @State private var name = ""
    @State private var baseURL = "https://"
    @State private var authType: AuthType = .none
    @State private var username = ""
    @State private var passwordOrToken = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Server Details") {
                    TextField("Name", text: $name)
                    TextField("Base URL", text: $baseURL)
                        .keyboardType(.URL)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                }
                
                Section("Authentication") {
                    Picker("Type", selection: $authType) {
                        Text("None").tag(AuthType.none)
                        Text("Basic").tag(AuthType.basic)
                        Text("Token").tag(AuthType.token)
                    }
                    
                    if authType == .basic {
                        TextField("Username", text: $username)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                        SecureField("Password", text: $passwordOrToken)
                    } else if authType == .token {
                        SecureField("Token", text: $passwordOrToken)
                    }
                }
            }
            .navigationTitle("Add Server")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        saveServer()
                    }
                    .disabled(name.isEmpty || baseURL.isEmpty)
                }
            }
        }
    }
    
    private func saveServer() {
        let server = ServerInstance(name: name, baseURL: baseURL, authType: authType, username: username)
        modelContext.insert(server)
        if authType != .none && !passwordOrToken.isEmpty {
            KeychainStore.shared.save(secret: passwordOrToken, for: server.id)
        }
        dismiss()
    }
}
