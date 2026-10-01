import SwiftUI

/// Корневой вид приложения — делегирует к AppCoordinator (4 вкладки + кнопка «+»).
struct ContentView: View {
    var body: some View {
        AppCoordinator()
    }
}
