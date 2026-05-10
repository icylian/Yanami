import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var store: AppStore
    @State private var draft = AppSettings()

    var body: some View {
        NavigationStack {
            Form {
                Section("Refresh") {
                    Toggle("Auto refresh", isOn: $draft.autoRefreshEnabled)
                    Stepper(value: $draft.refreshIntervalSeconds, in: 1...60, step: 1) {
                        Text("Interval \(Int(draft.refreshIntervalSeconds))s")
                    }
                }

                Section("Build") {
                    Text("Android & iPhone")
                    Text("Pre-release assets use a unique version tag per CI run.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Settings")
            .onAppear {
                draft = store.settings
            }
            .onChange(of: draft) { newValue in
                store.updateSettings(newValue)
            }
        }
    }
}
