import Foundation
import SwiftData

@MainActor
final class ServerListViewModel: ObservableObject {
    func delete(server: ServerInstance, modelContext: ModelContext) {
        KeychainStore.shared.deleteSecret(for: server.id)
        modelContext.delete(server)
    }
}
