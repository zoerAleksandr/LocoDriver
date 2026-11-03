import SwiftUI

// iOS-аналог Android `SnackbarManager` (SCREEN_SPECS §24.4): короткое сообщение о результате операции.
// Тост живёт выше навигации, поэтому переживает закрытие экрана (Android показывает «Маршрут сохранен»
// через 400 мс уже на предыдущем экране).

/// Глобальный центр тостов. `show(_:)` можно вызывать с любого экрана.
final class ToastCenter: ObservableObject {
    static let shared = ToastCenter()
    private init() {}

    @Published private(set) var message: String?
    private var generation = 0

    @MainActor
    func show(_ text: String) {
        generation += 1
        let current = generation
        message = text
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            if self.generation == current { self.message = nil }
        }
    }
}

private struct DSToastHost: ViewModifier {
    @ObservedObject private var center = ToastCenter.shared

    func body(content: Content) -> some View {
        content.overlay(alignment: .bottom) {
            if let message = center.message {
                Text(message)
                    .font(DSFont.sans(14, .medium))
                    .foregroundColor(DSColor.ctaInk)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 12)
                    .background(DSColor.cta)
                    .clipShape(Capsule())
                    .dsShadow(.sm)
                    .padding(.horizontal, DSMetrics.screenPadX)
                    .padding(.bottom, 96)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .allowsHitTesting(false)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: center.message)
    }
}

extension View {
    /// Подключается один раз в корне приложения (`AppCoordinator`).
    func dsToastHost() -> some View { modifier(DSToastHost()) }
}
