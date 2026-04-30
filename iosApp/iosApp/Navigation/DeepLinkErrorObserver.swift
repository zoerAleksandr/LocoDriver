import SwiftUI
import ComposeApp

/// Подписчик на ошибки `SharedRouteLinkHandler` (Kotlin singleton).
///
/// Используется в [AppCoordinator] для показа алерта при сбое
/// открытия deep-link `https://locodriver.ru/r/{id}` (или legacy
/// `locodriver://share/{id}`).
///
/// Без кнопки «Повторить» — deep-link одноразовый, повтор не имеет
/// смысла (пользователь должен снова нажать на ссылку извне).
@MainActor
final class DeepLinkErrorObserver: ObservableObject {
    @Published var error: AppError? = nil
    @Published var errorMessage: String? = nil

    /// Токены подписок watchX. Отменяются в deinit.
    private var watchHandles: [WatchHandle] = []

    init() {
        watchHandles.append(SharedRouteLinkHandler.shared.watchAppError { [weak self] e in
            DispatchQueue.main.async { self?.error = e }
        })
        watchHandles.append(SharedRouteLinkHandler.shared.watchErrorMessage { [weak self] msg in
            DispatchQueue.main.async { self?.errorMessage = msg }
        })
    }

    deinit {
        watchHandles.forEach { $0.cancel() }
    }

    func clearError() {
        SharedRouteLinkHandler.shared.clearError()
    }
}
