import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        // Инициализируем Koin перед стартом UI.
        // Передаём модули data_local (SQLDelight) и domain (UseCases).
        // Модуль data_remote (iosRepositoryModule) регистрируется внутри initKoin().
        IosKoinHelperKt.doInitKoin(additionalModules: [
            SqlDelightRouteModuleKt.sqlDelightRouteModule,
            SqlDelightSettingModuleKt.sqlDelightSettingsModule,
        ])
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
