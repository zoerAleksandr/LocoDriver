import SwiftUI
import ComposeApp

/// Обёртка Kotlin `AppInitIosViewModel` (SCREEN_SPECS §20, сквозная задача С1):
/// первый запуск (страна/пояс), миграции и загрузка производственного календаря.
///
/// Запускается ОДИН раз за процесс из `ContentView` (`.task` + guard `didStart`),
/// UI не блокирует. Статус готовности — `appInitialized`.
@MainActor
final class AppInitViewModelWrapper: ObservableObject {
    private static var didStart = false

    private var viewModel: AppInitIosViewModel? = nil

    @Published var appInitialized: Bool = false

    /// Создаёт Kotlin-ViewModel и подписывается на готовность; повторный вызов — no-op.
    func startIfNeeded() {
        guard !Self.didStart else { return }
        Self.didStart = true
        let vm = IosViewModelHelper.shared.createAppInitViewModel()
        viewModel = vm
        // Параметр колбэка `(Boolean) -> Unit` экспортируется как KotlinBoolean (boxed),
        // в отличие от Bool в сигнатурах методов — поэтому .boolValue.
        vm.watchAppInitialized { [weak self] initialized in
            DispatchQueue.main.async { self?.appInitialized = initialized.boolValue }
        }
    }

    deinit {
        viewModel?.dispose()
    }
}
