import SwiftUI
import ComposeApp

/// Координатор стартовых полноэкранных оверлеев (SCREEN_SPECS §22, §23):
/// сначала очередь «Новости при запуске» (сначала «Обновление», затем «Новость» —
/// порядок отдаёт `AnnouncementUseCase`), затем «Что нового» (если флаг не сброшен).
///
/// Презентация идёт через `AppRouter.fullScreen` (`AppSheet.announcement` /
/// `AppSheet.whatsNew`), содержимое — `AppSheetView`; данные — сюда.
@MainActor
final class LaunchOverlays: ObservableObject {
    static let shared = LaunchOverlays()

    let announcement = AnnouncementViewModelWrapper()
    let whatsNew = WhatsNewViewModelWrapper()

    private var started = false
    /// «Что нового» показывается после закрытия всех новостей (на Android новость
    /// лежит поверх «Что нового» — пользователь взаимодействует с ней первой).
    private var whatsNewDismissed = false

    private init() {}

    /// Запустить показ: вызывается один раз при появлении корня приложения.
    func start() {
        guard !started else { return }
        started = true
        announcement.onChange = { [weak self] in self?.sync() }
        whatsNew.onChange = { [weak self] in self?.sync() }
        announcement.load()
        sync()
    }

    /// Кнопка/крестик текущего оверлея: закрыть его и показать следующий.
    func closeCurrent() {
        switch AppRouter.shared.fullScreen {
        case .announcement:
            // Очередь обновится колбэком → sync() покажет следующее или «Что нового».
            announcement.closeCurrent()
        case .whatsNew:
            whatsNewDismissed = true
            whatsNew.close()
        default:
            break
        }
        // Страховка: состояние придёт асинхронным колбэком; если очередь уже
        // пуста и колбэка не будет — закрыть оверлей на следующем тике.
        DispatchQueue.main.async { [weak self] in self?.sync() }
    }

    private func sync() {
        if announcement.current != nil {
            AppRouter.shared.present(.announcement)
            return
        }
        if !whatsNewDismissed, whatsNew.shouldShow {
            AppRouter.shared.present(.whatsNew)
            return
        }
        if AppRouter.shared.fullScreen == .announcement || AppRouter.shared.fullScreen == .whatsNew {
            AppRouter.shared.dismissFullScreen()
        }
    }
}
