import SwiftUI

struct RootView: View {
    @EnvironmentObject private var store: AppStore

    var body: some View {
        TabView {
            ServerListView()
                .tabItem {
                    Label("Servers", systemImage: "server.rack")
                }

            NodeListView()
                .tabItem {
                    Label("Nodes", systemImage: "list.bullet.rectangle")
                }

            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
        }
        .task {
            if store.activeServer != nil && store.nodes.isEmpty {
                await store.loadNodes(mode: .initial)
            }
        }
    }
}
