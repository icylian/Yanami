import LocalAuthentication
import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var store: AppStore
    @State private var draft = AppSettings()
    @State private var biometricError: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("General") {
                    Toggle("Auto enter node list", isOn: $draft.autoEnterNodeList)
                }

                Section("Security") {
                    Toggle("Biometric lock", isOn: Binding(
                        get: { draft.biometricEnabled },
                        set: { enabled in
                            if enabled {
                                authenticateBiometric {
                                    draft.biometricEnabled = true
                                }
                            } else {
                                draft.biometricEnabled = false
                            }
                        }
                    ))
                    if let biometricError {
                        Text(biometricError)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Section("Refresh") {
                    Toggle("Auto refresh", isOn: $draft.autoRefreshEnabled)
                    Stepper(value: $draft.refreshIntervalSeconds, in: 1...60, step: 1) {
                        Text("Interval \(Int(draft.refreshIntervalSeconds))s")
                    }
                }

                Section("Display") {
                    Toggle("Mask IP / UUID", isOn: $draft.maskIpEnabled)
                    Toggle("Chart animation", isOn: $draft.chartAnimationEnabled)
                    Picker("Dark mode", selection: $draft.darkMode) {
                        Text("System").tag("system")
                        Text("Light").tag("light")
                        Text("Dark").tag("dark")
                    }
                    Picker("Language", selection: $draft.language) {
                        Text("System").tag("system")
                        Text("简体中文").tag("zh")
                        Text("English").tag("en")
                        Text("日本語").tag("ja")
                    }
                    Slider(value: $draft.fontScale, in: 0.8...1.4, step: 0.05) {
                        Text("Font scale")
                    } minimumValueLabel: {
                        Text("80%")
                    } maximumValueLabel: {
                        Text("140%")
                    }
                    Text("Font scale \(Int(draft.fontScale * 100))%")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Section("Terminal") {
                    Stepper(value: $draft.terminalFontSize, in: 8...32, step: 1) {
                        Text("Font size \(draft.terminalFontSize)")
                    }
                }
                
                Section("Build") {
                    Text("Pre-release builds are generated automatically by CI for each PR.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Settings")
            .environment(\.locale, AppLocalization.locale(for: draft.language))
            .onAppear {
                draft = store.settings
            }
            .onChange(of: draft) { newValue in
                store.updateSettings(newValue)
            }
        }
    }

    private func authenticateBiometric(onSuccess: @escaping () -> Void) {
        let context = LAContext()
        context.localizedFallbackTitle = ""
        context.localizedCancelTitle = AppLocalization.string("Cancel", language: draft.language)
        var error: NSError?
        let policy = LAPolicy.deviceOwnerAuthenticationWithBiometrics
        guard context.canEvaluatePolicy(policy, error: &error) else {
            biometricError = AppLocalization.string("Face ID or Touch ID is unavailable.", language: draft.language)
            return
        }
        context.evaluatePolicy(
            policy,
            localizedReason: AppLocalization.string("Enable biometric lock for YanamiNext", language: draft.language)
        ) { success, _ in
            DispatchQueue.main.async {
                if success {
                    biometricError = nil
                    onSuccess()
                } else {
                    biometricError = AppLocalization.string("Biometric authentication failed.", language: draft.language)
                }
            }
        }
    }
}
