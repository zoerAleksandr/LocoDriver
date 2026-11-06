import SwiftUI
import ComposeApp
import VKID

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

        // VK ID SDK (§18.2/§18.4): конфигурация с client_id 54740437 до первого
        // показа кнопок VK. Без неё `VKID.shared.authorize` падает.
        VKIDService.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Возврат из авторизации VK ID (§18.2): vk54740437://… —
                    // отдаём ссылку SDK, дальше — флоу шторки VK.
                    if url.scheme == "vk\(VKIDService.clientId)" {
                        _ = VKID.shared.open(url: url)
                        return
                    }
                    // Deep link: locodriver://profile — переход на экран Профиль.
                    if url.scheme == "locodriver" && url.host == "profile" {
                        AppRouter.shared.selectedTab = .profile
                        return
                    }
                    // Deep link: locodriver://share/{id}
                    // Обрабатываем в Kotlin-слое: загрузить Route, сохранить локально,
                    // AppNavHost переключится на FormRoute с новым id.
                    if url.host == "share" {
                        SharedRouteLinkHandler.shared.handle(urlString: url.absoluteString)
                        return
                    }
                    // Возврат из оплаты (§19.6): страница возврата открывает
                    // locodriver:// — проверяем ожидаемую оплату.
                    PurchasesViewModelWrapper.shared.onAppResumed()
                }
        }
    }
}
