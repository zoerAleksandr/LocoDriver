import SwiftUI

/// Сообщение очереди snackbar'ов. `id` различает одинаковые тексты подряд.
struct DSSnackbarItem: Identifiable, Equatable {
    let id = UUID()
    let text: String
}

/// Snackbar — короткое сообщение внизу экрана (§24: transient UI).
struct DSSnackbar: View {
    let text: String

    var body: some View {
        Text(text)
            .font(DSFont.sans(14, .medium))
            .foregroundColor(DSColor.ctaInk)
            .multilineTextAlignment(.leading)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, DSSpacing.lg)
            .padding(.vertical, 14)
            .background(DSColor.cta)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .dsShadow(.floating)
    }
}

extension View {
    /// Показывает первое сообщение очереди на `duration` секунд (или до тапа),
    /// затем вызывает `onFinished` — экран убирает его из очереди, и показывается
    /// следующее. Сообщения не перебивают друг друга.
    func dsSnackbarQueue(
        _ items: [DSSnackbarItem],
        duration: Double = 3,
        bottomPadding: CGFloat = 96,
        onFinished: @escaping () -> Void
    ) -> some View {
        overlay(alignment: .bottom) {
            DSSnackbarHost(item: items.first, duration: duration, onFinished: onFinished)
                .padding(.horizontal, DSMetrics.screenPadX)
                .padding(.bottom, bottomPadding)
        }
    }
}

private struct DSSnackbarHost: View {
    let item: DSSnackbarItem?
    let duration: Double
    let onFinished: () -> Void

    var body: some View {
        ZStack {
            if let item = item {
                DSSnackbar(text: item.text)
                    .id(item.id)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onTapGesture { onFinished() }
            }
        }
        .animation(.easeOut(duration: 0.2), value: item)
        .task(id: item?.id) {
            guard item != nil else { return }
            try? await Task.sleep(nanoseconds: UInt64(duration * 1_000_000_000))
            if !Task.isCancelled { onFinished() }
        }
    }
}

#Preview("Snackbar — light") {
    DSSnackbar(text: "Маршрут перемещён в корзину")
        .padding()
        .background(DSColor.bg)
        .preferredColorScheme(.light)
}

#Preview("Snackbar — dark") {
    DSSnackbar(text: "Синхронизация завершена")
        .padding()
        .background(DSColor.bg)
        .preferredColorScheme(.dark)
}
