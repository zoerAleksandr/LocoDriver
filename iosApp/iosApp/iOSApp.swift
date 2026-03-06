import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        // Инициализируем Koin перед стартом UI.
        // allIosKoinModules объединяет:
        //   - sqlDelightRouteModule (RouteDatabase, SearchResponseDatabase)
        //   - sqlDelightSettingsModule (SettingsDatabase, SalarySettingDatabase)
        //   - iosUseCaseModule (Repositories, UseCases, iOS ViewModels)
        IosKoinHelperKt.doInitKoin(additionalModules: IosKoinModulesKt.allIosKoinModules)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
