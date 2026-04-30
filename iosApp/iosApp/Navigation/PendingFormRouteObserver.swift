import SwiftUI
import ComposeApp

/// Подписчик на `SharedRouteLinkHandler.pendingFormRouteId` (Kotlin singleton).
///
/// После обработки deep-link `https://locodriver.ru/r/{id}` (или legacy
/// `locodriver://share/{id}`) handler сохраняет импортированный маршрут
/// в локальную БД и публикует его `basicData.id` в `pendingFormRouteId`.
///
/// `AppCoordinator` наблюдает это значение и при появлении не-пустого id
/// открывает `FormView(routeId:)` модальным sheet'ом, после чего сразу
/// вызывает [clear] — чтобы повторный deep-link не зациклил подписку.
@MainActor
final class PendingFormRouteObserver: ObservableObject {
    @Published var pendingRouteId: String? = nil

    init() {
        SharedRouteLinkHandler.shared.watchPendingFormRouteId { [weak self] id in
            DispatchQueue.main.async { self?.pendingRouteId = id }
        }
    }

    func clear() {
        SharedRouteLinkHandler.shared.clearPendingFormRouteId()
    }
}
