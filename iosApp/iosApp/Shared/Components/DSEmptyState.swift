import SwiftUI

// Макет: design/src/stats-screens.jsx → IOSStatsEmpty (плитка 76×76 r24 bgSubtle
//        с иконкой textFaint 38, заголовок 19/700 tracking −0.3, текст 14 textMuted
//        line-height 1.5, CTA + вторичная ссылка);
//        design/src/all-routes.jsx → ArEmpty («За май маршрутов нет»).
// Контракт: SCREEN_SPECS §24.4 — пустой список показывает назначение экрана и
// доступное основное действие («Добавить…»), а не бесконечный спиннер.

/// Пустое состояние экрана/списка.
struct DSEmptyState: View {
    let icon: DSIcon
    let title: String
    var message: String? = nil
    var primaryTitle: String? = nil
    var primaryIcon: DSIcon? = .plus
    var onPrimary: (() -> Void)? = nil
    var secondaryTitle: String? = nil
    var onSecondary: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 0) {
            icon.image
                .font(.system(size: 34, weight: .regular))
                .foregroundColor(DSColor.textFaint)
                .frame(width: 76, height: 76)
                .background(DSColor.bgSubtle)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.xl, style: .continuous))
                .padding(.bottom, 22)

            Text(title)
                .font(DSFont.sans(19, .bold))
                .tracking(-0.3)
                .foregroundColor(DSColor.text)
                .multilineTextAlignment(.center)

            if let message = message {
                Text(message)
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.textMuted)
                    .lineSpacing(14 * 0.5)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 280)
                    .padding(.top, 8)
            }

            if let primaryTitle = primaryTitle, let onPrimary = onPrimary {
                Button(action: onPrimary) {
                    HStack(spacing: DSSpacing.sm) {
                        if let primaryIcon = primaryIcon {
                            primaryIcon.image.font(.system(size: 16, weight: .semibold))
                        }
                        Text(primaryTitle).font(DSFont.sans(15, .semibold))
                    }
                    .foregroundColor(DSColor.ctaInk)
                    .padding(.horizontal, 22)
                    .padding(.vertical, 13)
                    .background(DSColor.cta)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(DSPressableStyle())
                .padding(.top, DSSpacing.xxl)
            }

            if let secondaryTitle = secondaryTitle, let onSecondary = onSecondary {
                Button(action: onSecondary) {
                    Text(secondaryTitle)
                        .font(DSFont.sans(14, .semibold))
                        .foregroundColor(DSColor.accent)
                        .padding(.horizontal, DSSpacing.lg)
                        .padding(.vertical, 10)
                }
                .buttonStyle(.plain)
                .padding(.top, 6)
            }
        }
        .padding(.horizontal, DSSpacing.xxl)
        .frame(maxWidth: .infinity)
    }
}

#Preview("Пустое состояние — light") {
    DSEmptyStatePreview().preferredColorScheme(.light)
}

#Preview("Пустое состояние — dark") {
    DSEmptyStatePreview().preferredColorScheme(.dark)
}

private struct DSEmptyStatePreview: View {
    var body: some View {
        DSPreviewCanvas {
            DSEmptyState(
                icon: .calendar,
                title: "За май маршрутов нет",
                message: "Маршруты появятся здесь, когда вы отметите смену.",
                primaryTitle: "Добавить маршрут",
                onPrimary: {},
                secondaryTitle: "Перейти к апрелю →",
                onSecondary: {}
            )
        }
    }
}
