import SwiftUI

/// Корневой вид приложения — делегирует к AppCoordinator (4 вкладки + кнопка «+»).
struct ContentView: View {
    /// Тема оформления (§17.5): пишет Kotlin (`ld.themeMode`), хранится на устройстве.
    @AppStorage("ld.themeMode") private var themeMode: String = "MODE_SYSTEM"
    /// С1: загрузка производственного календаря и миграции при старте — один раз за процесс.
    @StateObject private var appInit = AppInitViewModelWrapper()

    private var scheme: ColorScheme? {
        switch themeMode {
        case "MODE_LIGHT": return .light
        case "MODE_DARK": return .dark
        default: return nil
        }
    }

    var body: some View {
        AppCoordinator()
            .preferredColorScheme(scheme)
            .task { appInit.startIfNeeded() }
    }
}
