import SwiftUI

/// Корневой вид приложения — делегирует к AppCoordinator (TabView с 5 вкладками).
struct ContentView: View {
    var body: some View {
        AppCoordinator()
    }
}
