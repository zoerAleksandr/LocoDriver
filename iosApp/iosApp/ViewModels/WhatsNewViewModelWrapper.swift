import SwiftUI
import ComposeApp

/// Обёртка `WhatsNewIosViewModel` (SCREEN_SPECS §22) для SwiftUI.
/// Показывает легаси-блок «Что нового» один раз после обновления.
@MainActor
final class WhatsNewViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getWhatsNewViewModel()

    /// true — блок нужно показать в этой сессии (флаг читается один раз на старте).
    @Published var shouldShow: Bool = false

    /// Вызывается после каждого изменения состояния.
    var onChange: (() -> Void)? = nil

    init() {
        viewModel.watchShouldShow { [weak self] value in
            DispatchQueue.main.async {
                self?.shouldShow = value.boolValue
                self?.onChange?()
            }
        }
    }

    /// «Пропустить»/«Начать»: закрыть блок и сбросить флаг.
    func close() {
        viewModel.dismissWhatsNew()
    }
}
