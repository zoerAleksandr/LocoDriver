import SwiftUI

// Макет: design/src/all-routes.jsx → arChip / arIconChip (чипы «Фильтр», «Дата», PDF);
//        design/tokens.js → chipBg / chipBgActive (выбираемые чипы);
//        design/src/shared-ui.jsx → Pill (бейджи-метки);
//        design/src/ios-screens.jsx → TimeRow tag / UnitRow badge (mono 9.5/700, UPPERCASE).

/// Чип-кнопка.
/// - `.assist` — surface + рамка `border` + тень `sm`, высота 34, 13/600 («Фильтр», «Дата»);
/// - `.choice(isSelected:)` — выбираемый: `chipBg` / `chipBgActive`, текст `text` / `accent`.
struct DSChip: View {
    enum Style {
        case assist
        case choice(isSelected: Bool)
    }

    let title: String
    var icon: DSIcon? = nil
    var style: Style = .assist
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if let icon = icon {
                    icon.image.font(.system(size: 13, weight: .semibold))
                }
                Text(title)
                    .font(DSFont.sans(13, .semibold))
                    .lineLimit(1)
            }
            .foregroundColor(foreground)
            .padding(.horizontal, 12)
            .frame(height: 34)
            .background(background)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(isAssist ? DSColor.border : Color.clear, lineWidth: 1))
        }
        .buttonStyle(DSPressableStyle())
        .modifier(DSChipShadow(isAssist: isAssist))
    }

    private var isAssist: Bool {
        if case .assist = style { return true }
        return false
    }

    private var isSelected: Bool {
        if case .choice(let selected) = style { return selected }
        return false
    }

    private var foreground: Color {
        isSelected ? DSColor.accent : DSColor.text
    }

    private var background: Color {
        switch style {
        case .assist: return DSColor.surface
        case .choice(let selected): return selected ? DSColor.chipBgActive : DSColor.chipBg
        }
    }
}

/// Круглый чип только с иконкой (34×34), например «Экспорт в PDF».
struct DSIconChip: View {
    let icon: DSIcon
    var accessibilityLabel: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            icon.image
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(DSColor.text)
                .frame(width: 34, height: 34)
                .background(DSColor.surface)
                .clipShape(Circle())
                .overlay(Circle().stroke(DSColor.border, lineWidth: 1))
        }
        .buttonStyle(DSPressableStyle())
        .dsShadow(.sm)
        .accessibilityLabel(Text(accessibilityLabel))
    }
}

private struct DSChipShadow: ViewModifier {
    let isAssist: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if isAssist {
            content.dsShadow(.sm)
        } else {
            content
        }
    }
}

/// Бейдж-метка (`Pill`): статус, счётчик, метка. Pill-радиус, 12/600.
struct DSPill: View {
    enum Tone { case accent, success, warning, danger, muted, inverse }

    let text: String
    var tone: Tone = .muted
    /// По умолчанию mono — пиллы обычно содержат время/числа/коды.
    var isMono: Bool = true

    var body: some View {
        Text(text)
            .font(isMono ? DSFont.mono(12, .semibold) : DSFont.sans(12, .semibold))
            .tracking(0.2)
            .foregroundColor(foreground)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(background)
            .clipShape(Capsule())
    }

    private var foreground: Color {
        switch tone {
        case .accent: return DSColor.accent
        case .success: return DSColor.success
        case .warning: return DSColor.warning
        case .danger: return DSColor.danger
        case .muted: return DSColor.textMuted
        case .inverse: return DSColor.accentInk
        }
    }

    private var background: Color {
        switch tone {
        case .accent: return DSColor.accentSoft
        case .success: return DSColor.success.opacity(0.12)
        case .warning: return DSColor.warning.opacity(0.14)
        case .danger: return DSColor.danger.opacity(0.12)
        case .muted: return DSColor.bgSubtle
        case .inverse: return DSColor.accent
        }
    }
}

/// Маленький UPPERCASE-тег в строке («ПО ПРИБЫТИЮ ПАССАЖИРОМ», «НАЧАЛО РАБОТЫ»):
/// mono 9.5/700, tracking 0.8, accent на accentSoft, радиус 6.
struct DSTag: View {
    let text: String
    var color: Color = DSColor.accent
    var background: Color = DSColor.accentSoft

    var body: some View {
        Text(text)
            .font(DSFont.mono(9.5, .bold))
            .tracking(0.8)
            .textCase(.uppercase)
            .foregroundColor(color)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(background)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.xs, style: .continuous))
    }
}

#Preview("Чипы — light") {
    DSChipPreview().preferredColorScheme(.light)
}

#Preview("Чипы — dark") {
    DSChipPreview().preferredColorScheme(.dark)
}

private struct DSChipPreview: View {
    var body: some View {
        DSPreviewCanvas {
            HStack(spacing: DSSpacing.sm) {
                DSChip(title: "Фильтр", icon: .filter) {}
                DSChip(title: "Дата", icon: .sort) {}
                DSIconChip(icon: .pdf, accessibilityLabel: "Экспорт в PDF") {}
            }
            HStack(spacing: DSSpacing.sm) {
                DSChip(title: "Месяц", style: .choice(isSelected: true)) {}
                DSChip(title: "Год", style: .choice(isSelected: false)) {}
            }
            HStack(spacing: DSSpacing.sm) {
                DSPill(text: "+30:05", tone: .accent)
                DSPill(text: "Активна", tone: .success, isMono: false)
                DSPill(text: "3 дня", tone: .warning)
                DSPill(text: "Ошибка", tone: .danger, isMono: false)
                DSPill(text: "лок", tone: .muted)
            }
            HStack(spacing: DSSpacing.sm) {
                DSTag(text: "по прибытию пассажиром")
                DSTag(text: "из маршрута", color: DSColor.textMuted, background: DSColor.bgSubtle)
            }
        }
    }
}
