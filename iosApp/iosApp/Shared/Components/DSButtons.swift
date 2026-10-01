import SwiftUI

// Макет: design/src/shared-ui.jsx → AddBtn, DangerBtn, TextAction, IconBtn;
//        design/src/all-routes.jsx → IOSFilterSheet (CTA «Показать 18» / «Сбросить»);
//        design/src/stats-screens.jsx → IOSStatsEmpty (CTA «Добавить маршрут»);
//        design/src/ios-screens.jsx → IOSCalcSheet (кнопка-строка «Настройки зарплаты»).

/// Главная кнопка действия (CTA): фон `cta`, текст `ctaInk`, радиус 14, 15/700.
/// Во время async-операции передавайте `isLoading: true` — кнопка блокируется
/// (SCREEN_SPECS §24.4: повторный тап не должен создавать второй запрос).
struct DSCTAButton: View {
    let title: String
    var icon: DSIcon? = nil
    var isLoading: Bool = false
    var isEnabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: DSSpacing.sm) {
                if isLoading {
                    ProgressView().tint(DSColor.ctaInk)
                } else if let icon = icon {
                    icon.image.font(.system(size: 16, weight: .semibold))
                }
                Text(title).font(DSFont.sans(15, .bold))
            }
            .foregroundColor(DSColor.ctaInk)
            .frame(maxWidth: .infinity, minHeight: 50)
            .padding(.horizontal, DSSpacing.lg)
            .background(DSColor.cta)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .opacity(isEnabled ? 1 : 0.4)
        }
        .buttonStyle(DSPressableStyle())
        .disabled(!isEnabled || isLoading)
    }
}

/// Тональная кнопка: вторичное действие рядом с CTA.
/// - `.neutral` — surface + рамка `border` + текст `text` («Сбросить»);
/// - `.accent` — `accentSoft` + текст `accent`;
/// - `.subtle` — `bgSubtle` + текст `accent` (AddBtn «Добавить серию»).
struct DSTonalButton: View {
    enum Tone { case neutral, accent, subtle }

    let title: String
    var icon: DSIcon? = nil
    var tone: Tone = .neutral
    var isEnabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: DSSpacing.sm) {
                if let icon = icon {
                    icon.image.font(.system(size: 15, weight: .semibold))
                }
                Text(title).font(DSFont.sans(15, .semibold))
            }
            .foregroundColor(foreground)
            .frame(maxWidth: .infinity, minHeight: 50)
            .padding(.horizontal, DSSpacing.lg)
            .background(background)
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(tone == .neutral ? DSColor.border : Color.clear, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .opacity(isEnabled ? 1 : 0.4)
        }
        .buttonStyle(DSPressableStyle())
        .disabled(!isEnabled)
    }

    private var foreground: Color {
        tone == .neutral ? DSColor.text : DSColor.accent
    }

    private var background: Color {
        switch tone {
        case .neutral: return DSColor.surface
        case .accent: return DSColor.accentSoft
        case .subtle: return DSColor.bgSubtle
        }
    }
}

/// Деструктивная кнопка внизу экрана-редактора (`DangerBtn`):
/// прозрачная, рамка `border`, текст `danger`, радиус 12, 14/500.
struct DSDangerButton: View {
    let title: String
    var icon: DSIcon? = .trash
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: DSSpacing.sm) {
                if let icon = icon {
                    icon.image.font(.system(size: 14, weight: .medium))
                }
                Text(title).font(DSFont.sans(14, .medium))
            }
            .foregroundColor(DSColor.danger)
            .frame(maxWidth: .infinity)
            .padding(14)
            .overlay(
                RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous)
                    .stroke(DSColor.border, lineWidth: 1)
            )
        }
        .buttonStyle(DSPressableStyle())
    }
}

/// Текстовая ссылка цвета `accent` без фона («Все (23) ›», «+ Рекуперация»).
struct DSTextAction: View {
    let title: String
    var leadingIcon: DSIcon? = nil
    var trailingIcon: DSIcon? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                if let leadingIcon = leadingIcon {
                    leadingIcon.image.font(.system(size: 11, weight: .semibold))
                }
                Text(title).font(DSFont.sans(13, .medium))
                if let trailingIcon = trailingIcon {
                    trailingIcon.image.font(.system(size: 10, weight: .semibold))
                }
            }
            .foregroundColor(DSColor.accent)
            .padding(.vertical, 2)
        }
        .buttonStyle(.plain)
    }
}

/// Круглая кнопка с иконкой (`IconBtn`).
struct DSIconButton: View {
    enum Variant {
        /// прозрачная, цвет `text`
        case ghost
        /// surface + рамка
        case surface
        /// bgSubtle без рамки
        case subtle
        /// surface + рамка + тень floating (плавающий тулбар iOS)
        case floating
        /// фон `cta` (главная плавающая кнопка)
        case cta
        /// фон `accent` (FAB)
        case accent
        /// surface + рамка + текст `danger`
        case danger
    }

    let icon: DSIcon
    var variant: Variant = .ghost
    var size: CGFloat = 40
    var accessibilityLabel: String? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            icon.image
                .font(.system(size: (size * 0.45).rounded(), weight: .medium))
                .foregroundColor(foreground)
                .frame(width: size, height: size)
                .background(background)
                .clipShape(Circle())
                .overlay(Circle().stroke(hasBorder ? DSColor.border : Color.clear, lineWidth: 1))
        }
        .buttonStyle(DSPressableStyle())
        .modifier(DSIconButtonShadow(variant: variant))
        .accessibilityLabel(Text(accessibilityLabel ?? icon.rawValue))
    }

    private var hasBorder: Bool {
        switch variant {
        case .surface, .floating, .danger: return true
        default: return false
        }
    }

    private var foreground: Color {
        switch variant {
        case .cta: return DSColor.ctaInk
        case .accent: return DSColor.accentInk
        case .danger: return DSColor.danger
        default: return DSColor.text
        }
    }

    private var background: Color {
        switch variant {
        case .ghost: return .clear
        case .surface, .floating, .danger: return DSColor.surface
        case .subtle: return DSColor.bgSubtle
        case .cta: return DSColor.cta
        case .accent: return DSColor.accent
        }
    }
}

private struct DSIconButtonShadow: ViewModifier {
    let variant: DSIconButton.Variant

    @ViewBuilder
    func body(content: Content) -> some View {
        switch variant {
        case .floating, .danger:
            content.dsShadow(.floating)
        case .accent:
            content.dsShadow(.fab)
        case .cta:
            // `0 12px 32px rgba(0,0,0,0.28)` — ios-screens.jsx, плавающий тулбар
            content.shadow(color: Color.black.opacity(0.28), radius: 16, x: 0, y: 12)
        default:
            content
        }
    }
}

/// Стиль нажатия для кнопок кита: лёгкое затемнение, без системной подсветки.
struct DSPressableStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.7 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

#Preview("Кнопки — light") {
    DSButtonsPreview().preferredColorScheme(.light)
}

#Preview("Кнопки — dark") {
    DSButtonsPreview().preferredColorScheme(.dark)
}

private struct DSButtonsPreview: View {
    var body: some View {
        DSPreviewCanvas {
            DSCTAButton(title: "Добавить маршрут", icon: .plus) {}
            DSCTAButton(title: "Оплатить", isLoading: true) {}
            HStack(spacing: 10) {
                DSTonalButton(title: "Сбросить") {}
                DSCTAButton(title: "Показать 18") {}
            }
            DSTonalButton(title: "Настроить станцию", tone: .accent) {}
            DSTonalButton(title: "Добавить серию", icon: .plus, tone: .subtle) {}
            DSDangerButton(title: "Удалить серию") {}
            DSTextAction(title: "Все (23)", trailingIcon: .chevronRight) {}
            HStack(spacing: DSSpacing.md) {
                DSIconButton(icon: .sliders, variant: .cta, size: 52) {}
                DSIconButton(icon: .heart, variant: .floating) {}
                DSIconButton(icon: .trash, variant: .danger) {}
                DSIconButton(icon: .plus, variant: .accent) {}
                DSIconButton(icon: .close, variant: .subtle, size: 30) {}
            }
        }
    }
}
