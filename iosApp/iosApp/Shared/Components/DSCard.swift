import SwiftUI

// Макет: design/src/shared-ui.jsx → Card, Sep, IconAvatar;
//        design/src/ios-screens.jsx → Card (surface, r18, shadow sm).

/// Карточка секции — главный контейнер контента.
/// `surface`, радиус 18 (iOS), тень `sm`, содержимое обрезается по радиусу.
/// Внутреннего padding нет: строки внутри задают свои отступы.
struct DSCard<Content: View>: View {
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        .dsShadow(.sm)
    }
}

/// Разделитель строк внутри карточки (`Sep`).
/// `inset`: 0 — от края до края; 20 — от текста строки; 56 — после колонки иконки.
struct DSDivider: View {
    var inset: CGFloat = DSMetrics.rowPadX

    var body: some View {
        Rectangle()
            .fill(DSColor.border)
            .frame(height: 1)
            .padding(.leading, inset)
    }
}

/// Иконка в круге/скруглённом квадрате в начале строки (`IconAvatar`).
struct DSIconAvatar: View {
    enum Size {
        case sm, md, lg

        var dimension: CGFloat {
            switch self {
            case .sm: return DSMetrics.avatarSm
            case .md: return DSMetrics.avatarMd
            case .lg: return DSMetrics.avatarLg
            }
        }

        var iconSize: CGFloat {
            switch self {
            case .sm: return 14
            case .md: return 16
            case .lg: return 18
            }
        }
    }

    enum Tone {
        /// bgSubtle + text
        case neutral
        /// accentSoft + accent
        case accent
        /// accent + accentInk
        case inverse
        /// warning 15 % + warning
        case warn
    }

    enum AvatarShape { case circle, square }

    let icon: DSIcon
    var size: Size = .md
    var tone: Tone = .neutral
    var shape: AvatarShape = .circle

    var body: some View {
        let dim = size.dimension
        let radius = shape == .circle ? dim / 2 : (dim * 0.3).rounded()
        icon.image
            .font(.system(size: size.iconSize, weight: .medium))
            .foregroundColor(foreground)
            .frame(width: dim, height: dim)
            .background(background)
            .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
    }

    private var foreground: Color {
        switch tone {
        case .neutral: return DSColor.text
        case .accent: return DSColor.accent
        case .inverse: return DSColor.accentInk
        case .warn: return DSColor.warning
        }
    }

    private var background: Color {
        switch tone {
        case .neutral: return DSColor.bgSubtle
        case .accent: return DSColor.accentSoft
        case .inverse: return DSColor.accent
        case .warn: return DSColor.warning.opacity(0.15)
        }
    }
}

#Preview("DSCard — light") {
    DSCardPreview().preferredColorScheme(.light)
}

#Preview("DSCard — dark") {
    DSCardPreview().preferredColorScheme(.dark)
}

private struct DSCardPreview: View {
    var body: some View {
        DSPreviewCanvas {
            DSCard {
                Text("№ 112").dsTextStyle(.valueMono)
                    .padding(.horizontal, DSMetrics.rowPadX)
                    .padding(.vertical, DSMetrics.cardPadY)
                DSDivider()
                Text("Подмена на ст. Лужская.").dsTextStyle(.body)
                    .padding(DSMetrics.rowPadX)
            }
            HStack(spacing: DSSpacing.md) {
                DSIconAvatar(icon: .clock, size: .sm)
                DSIconAvatar(icon: .locomotive, size: .md, tone: .accent)
                DSIconAvatar(icon: .train, size: .lg, tone: .accent, shape: .square)
                DSIconAvatar(icon: .check, size: .md, tone: .inverse)
                DSIconAvatar(icon: .moon, size: .md, tone: .warn)
            }
        }
    }
}
