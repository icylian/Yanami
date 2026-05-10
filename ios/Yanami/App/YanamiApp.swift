import SwiftUI
import SwiftData

@main
struct YanamiApp: App {
    var body: some Scene {
        WindowGroup {
            NavigationStack {
                ServerListView()
            }
        }
        .modelContainer(for: ServerInstance.self)
    }
}
