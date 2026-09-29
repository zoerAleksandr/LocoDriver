import SwiftUI

// Макет: design/src/ios-frame.jsx → IOSGlassPill (Liquid Glass: высота 44,
//        blur 12 + saturate, тинт белый 50 % / серый 28 %, блик-обводка 0.5 pt, тень).
// Реализация через `.ultraThinMaterial` (iOS 15+). Настоящий Liquid Glass
// (`glassEffect`) — только iOS 26, при deployment target 16 не используется.

/// Стеклянная пилюля — контейнер для кнопок поверх контента (навбар, плавающие панели).
struct DSGlassPill<Content: View>: View {
    @Environment(\.colorScheme) private var colorScheme
    @ViewBuilder let content: () -> Content

    var body: some View {
        HStack(spacing: 0) {
            content()
        }
        .padding(.horizontal, 4)
        .frame(minWidth: 44, minHeight: 44)
        .background(
            Capsule()
                .fill(.ultraThinMaterial)
                .overlay(Capsule().fill(tint))
        )
        .overlay(
            Capsule().stroke(
                colorScheme == .dark ? Color.white.opacity(0.15) : Color.black.opacity(0.06),
                lineWidth: 0.5
            )
        )
        .clipShape(Capsule())
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.35 : 0.07), radius: 2, x: 0, y: 1)
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.06), radius: 6, x: 0, y: 3)
    }

    private var tint: Color {
        colorScheme == .dark
            ? Color(red: 120 / 255, green: 120 / 255, blue: 128 / 255).opacity(0.28)
            : Color.white.opacity(0.5)
    }
}

/// Иконка-кнопка внутри стеклянной пилюли (36×36, как в `IOSNavBar`).
struct DSGlassIconButton: View {
    let icon: DSIcon
    var accessibilityLabel: String? = nil
    let action: () -> Void

    var body: some View {
        DSGlassPill {
            Button(action: action) {
                icon.image
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(DSColor.textMuted)
                    .frame(width: 36, height: 36)
            }
            .buttonStyle(DSPressableStyle())
            .accessibilityLabel(Text(accessibilityLabel ?? icon.rawValue))
        }
    }
}

#Preview("Стекло — light") {
    DSGlassPreview().preferredColorScheme(.light)
}

#Preview("Стекло — dark") {
    DSGlassPreview().preferredColorScheme(.dark)
}

private struct DSGlassPreview: View {
    var body: some View {
        DSPreviewCanvas {
            ZStack {
                LinearGradient(colors: [DSColor.accent, DSColor.success], startPoint: .leading, endPoint: .trailing)
                    .frame(height: 120)
                    .clipShape(RoundedRectangle(cornerRadius: DSRadius.card))
                HStack {
                    DSGlassIconButton(icon: .chevronLeft, accessibilityLabel: "Назад") {}
                    Spacer()
                    DSGlassIconButton(icon: .ellipsis, accessibilityLabel: "Ещё") {}
                }
                .padding(.horizontal, DSSpacing.lg)
            }
        }
    }
}
