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

    init() {
        SharedRouteLinkHandler.shared.watchAppError { [weak self] e in
            DispatchQueue.main.async { self?.error = e }
        }
        SharedRouteLinkHandler.shared.watchErrorMessage { [weak self] msg in
            DispatchQueue.main.async { self?.errorMessage = msg }
        }
    }

    func clearError() {
        SharedRouteLinkHandler.shared.clearError()
    }
}
