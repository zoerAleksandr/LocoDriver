import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        // TODO: Sentry KMP 0.23.x скомпилирован с Kotlin 2.1.21 и несовместим с
        // Kotlin 2.2.20 runtime — вызов падает в wrapUnhandledExceptionHook.
        // Включить обратно после выхода Sentry KMP с поддержкой Kotlin 2.2.x.
        // SentryInitKt.doInitSentry(dsn: "https://a0e7493da038dce47d7b82f449bad50b@o4511036722642944.ingest.de.sentry.io/4511036736077904")

        // Инициализируем Koin перед стартом UI.
        // initKoin уже включает iosRepositoryModule (Ktor + Keychain).
        // iosUseCaseModule содержит SQLDelight-драйверы, базы данных,
        // репозитории, UseCases и ViewModels — всё в одном модуле,
        // так как data_local не экспортируется из ComposeApp.framework
        // и его символы недоступны Swift напрямую.
        IosKoinHelperKt.doInitKoin(additionalModules: [
            IosUseCaseModuleKt.iosUseCaseModule,
        ])
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Deep link: locodriver://profile — переход на экран Профиль.
                    if url.scheme == "locodriver" && url.host == "profile" {
                        AppRouter.shared.selectedTab = .profile
                        return
                    }
                    // Deep link: locodriver://share/{id}
                    // Обрабатываем в Kotlin-слое: загрузить Route, сохранить локально,
                    // AppNavHost переключится на FormRoute с новым id.
                    SharedRouteLinkHandler.shared.handle(urlString: url.absoluteString)
                }
        }
    }
}
