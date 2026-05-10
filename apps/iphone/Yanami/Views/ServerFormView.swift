import SwiftUI

struct ServerFormView: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss
    @State private var draft: ServerProfile
    @State private var isTesting = false
    @State private var testMessage = ""
    let onSave: (ServerProfile) -> Void

    init(server: ServerProfile, onSave: @escaping (ServerProfile) -> Void) {
        _draft = State(initialValue: server)
        self.onSave = onSave
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Server") {
                    TextField("Name", text: $draft.name)
                    TextField("Server URL", text: $draft.baseURL)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.URL)
                        .autocorrectionDisabled()
                }

                Section("Authentication") {
                    Picker("Mode", selection: $draft.authType) {
                        ForEach(AuthType.allCases) { authType in
                            Text(authType.title).tag(authType)
                        }
                    }

                    if draft.authType == .password {
                        TextField("Username", text: $draft.username)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                        SecureField("Password", text: $draft.password)
                    }

                    if draft.authType == .apiKey {
                        SecureField("API Key", text: $draft.apiKey)
                    }
                }

                Section("Custom Headers") {
                    ForEach($draft.customHeaders) { $header in
                        VStack(alignment: .leading, spacing: 8) {
                            TextField("Header name", text: $header.name)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled()
                            SecureField("Header value", text: $header.value)
                        }
                    }
                    .onDelete { offsets in
                        draft.customHeaders.remove(atOffsets: offsets)
                    }

                    Button {
                        draft.customHeaders.append(CustomHeader(name: "", value: ""))
                    } label: {
                        Label("Add Header", systemImage: "plus")
                    }

                    Button {
                        store.addCloudflareHeaders(to: &draft)
                    } label: {
                        Label("Add Cloudflare Access Headers", systemImage: "lock.shield")
                    }
                }

                Section("Validation") {
                    Button {
                        Task { await testConnection() }
                    } label: {
                        if isTesting {
                            ProgressView()
                        } else {
                            Label("Test Connection", systemImage: "network")
                        }
                    }
                    if !testMessage.isEmpty {
                        Text(testMessage)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle("Komari Instance")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        draft.sessionToken = ""
                        onSave(draft)
                    }
                    .disabled(!canSave)
                }
            }
        }
    }

    private var canSave: Bool {
        !draft.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        URL(string: draft.normalizedBaseURL) != nil &&
        (draft.authType == .guest ||
         draft.authType == .apiKey && !draft.apiKey.isEmpty ||
         draft.authType == .password && !draft.username.isEmpty && !draft.password.isEmpty)
    }

    private func testConnection() async {
        isTesting = true
        defer { isTesting = false }
        do {
            let version = try await store.testConnection(draft)
            testMessage = "Connected. Komari \(version)"
        } catch {
            testMessage = error.localizedDescription
        }
    }
}
