import Foundation

struct ProfileStore {
    private let account = "appState"
    private let keychain = KeychainStore(service: "com.sekusarisu.yanami.ios")

    func load() -> PersistedAppState {
        keychain.read(PersistedAppState.self, account: account)
            ?? PersistedAppState(servers: [], activeServerId: nil, settings: AppSettings())
    }

    func save(_ state: PersistedAppState) {
        try? keychain.save(state, account: account)
    }
}
