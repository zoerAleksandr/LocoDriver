import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        // Инициализируем Sentry для отслеживания ошибок.
        SentryInitKt.doInitSentry(dsn: "https://a0e7493da038dce47d7b82f449bad50b@o4511036722642944.ingest.de.sentry.io/4511036736077904")

        // Инициализируем Koin перед стартом UI.
        // IosRepositoryModuleKt — data layer (SQLDelight + Ktor).
        // IosUseCaseModuleKt   — domain layer (UseCases + iOS ViewModels).
        IosKoinHelperKt.doInitKoin(additionalModules: [
            IosRepositoryModuleKt.iosRepositoryModule,
            IosUseCaseModuleKt.iosUseCaseModule,
        ])
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
