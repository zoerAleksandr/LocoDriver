import SwiftUI
import ComposeApp

/// Swift-обёртка над KMP AppInitIosViewModel.
/// Выполняет стартовую инициализацию (первый запуск, локаль/таймзона,
/// загрузка производственного календаря) и выставляет `appInitialized = true`,
/// когда всё готово.
@MainActor
final class AppInitViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getAppInitViewModel()

    @Published var appInitialized: Bool = false

    init() {
        viewModel.watchAppInitialized { [weak self] ready in
            DispatchQueue.main.async {
                self?.appInitialized = ready.boolValue
            }
        }
    }
}
