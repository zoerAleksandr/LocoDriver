import SwiftUI
import ComposeApp

/// Обёртка `AnnouncementIosViewModel` (SCREEN_SPECS §23) для SwiftUI.
/// Состояние — очередь сообщений при запуске; показ/закрытие ведёт Kotlin.
@MainActor
final class AnnouncementViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getAnnouncementViewModel()

    /// Текущее сообщение (первое в очереди); nil — показывать нечего.
    @Published var current: AnnouncementUi? = nil

    /// Вызывается после каждого изменения состояния (очереди).
    var onChange: (() -> Void)? = nil

    init() {
        viewModel.watchAnnouncement { [weak self] value in
            DispatchQueue.main.async {
                self?.current = value
                self?.onChange?()
            }
        }
    }

    /// Запросить непросмотренные сообщения (после старта приложения).
    func load() {
        viewModel.loadAnnouncement()
    }

    /// Пометить текущее сообщение просмотренным и перейти к следующему в очереди.
    func closeCurrent() {
        viewModel.dismissAnnouncement()
    }
}
