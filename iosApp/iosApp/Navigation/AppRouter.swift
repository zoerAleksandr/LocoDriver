import SwiftUI

/// Общий роутер приложения. Позволяет менять активную вкладку извне —
/// например, из `onOpenURL` при переходе по ссылке `locodriver://profile`.
final class AppRouter: ObservableObject {
    static let shared = AppRouter()
    private init() {}

    @Published var selectedTab: AppTab = .home
}
