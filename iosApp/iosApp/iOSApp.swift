import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        // Инициализируем Sentry для отслеживания ошибок.
        SentryInitKt.initSentry(dsn: "https://a0e7493da038dce47d7b82f449bad50b@o4511036722642944.ingest.de.sentry.io/4511036736077904")

        // Инициализируем Koin перед стартом UI.
        // Передаём модули data_local (SQLDelight) и domain (UseCases).
        // Модуль data_remote (iosRepositoryModule) регистрируется внутри initKoin().
        IosKoinHelperKt.doInitKoin(additionalModules: [
            SqlDelightRouteModuleKt.sqlDelightRouteModule,       // RouteDatabase, SearchResponseDatabase
            SqlDelightSettingModuleKt.sqlDelightSettingsModule,  // SettingsDatabase, SalarySettingDatabase
            IosUseCaseModuleKt.iosUseCaseModule,                 // Repositories, UseCases, iOS ViewModels
        ])
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
