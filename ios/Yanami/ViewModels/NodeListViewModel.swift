import Foundation
import Combine

@MainActor
final class NodeListViewModel: ObservableObject {
    @Published var nodes: [NodeInfoPayload] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let client: KomariClient
    private var timerCancellable: AnyCancellable?

    init(server: ServerInstance) {
        self.client = KomariClient(server: server)
    }

    func startRefreshing() {
        fetchNodes()
        timerCancellable = Timer.publish(every: 3.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                Task {
                    await self?.fetchNodes(isSilent: true)
                }
            }
    }

    func stopRefreshing() {
        timerCancellable?.cancel()
        timerCancellable = nil
    }

    private func fetchNodes(isSilent: Bool = false) {
        Task {
            if !isSilent {
                isLoading = true
            }
            do {
                let fetchedNodes = try await client.getNodes()
                self.nodes = fetchedNodes
                self.errorMessage = nil
            } catch {
                if !isSilent {
                    self.errorMessage = error.localizedDescription
                }
            }
            if !isSilent {
                isLoading = false
            }
        }
    }
}
