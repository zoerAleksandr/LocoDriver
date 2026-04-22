import SwiftUI
import Combine

/// Простой toast-менеджер для отображения сообщений от KMP ViewModel
/// (аналог Android SnackbarManager). Показывает плавающее сообщение
/// поверх текущего экрана на ~2.5 сек.
///
/// Использование:
///   1. В корне приложения навешать `.syncToastOverlay()`.
///   2. Из любого места вызывать `SyncToastCenter.shared.show("...")`.
final class SyncToastCenter: ObservableObject {
    static let shared = SyncToastCenter()

    @Published var message: String? = nil
    private var clearWork: DispatchWorkItem?

    private init() {}

    func show(_ text: String) {
        guard !text.isEmpty else { return }
        clearWork?.cancel()
        withAnimation(.easeOut(duration: 0.2)) {
            self.message = text
        }
        let work = DispatchWorkItem { [weak self] in
            withAnimation(.easeIn(duration: 0.25)) {
                self?.message = nil
            }
        }
        clearWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5, execute: work)
    }
}

struct SyncToastOverlay: ViewModifier {
    @ObservedObject private var center = SyncToastCenter.shared

    func body(content: Content) -> some View {
        ZStack {
            content
            if let msg = center.message {
                VStack {
                    Spacer()
                    Text(msg)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color.black.opacity(0.85))
                        .clipShape(Capsule())
                        .padding(.bottom, 40)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                .ignoresSafeArea(edges: .bottom)
                .allowsHitTesting(false)
            }
        }
    }
}

extension View {
    func syncToastOverlay() -> some View { modifier(SyncToastOverlay()) }
}
