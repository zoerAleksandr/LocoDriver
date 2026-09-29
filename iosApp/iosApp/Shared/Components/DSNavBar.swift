import SwiftUI

// Макет: design/src/ios-screens.jsx → навбар IOSScreenRoute (pill-кнопка 40×40
//        «назад», заголовок 17/700 по центру, текстовая pill «Сохранить» accent);
//        design/src/shared-ui.jsx → NavBarIOS (style 'pill' / 'text'), NavPillBtn;
//        design/src/ios-frame.jsx → IOSNavBar (стеклянный вариант — см. DSGlassPill).

/// Кастомный навбар экрана. Экран, который его использует, прячет системный
/// навбар модификатором `.dsHideSystemNavBar()`.
///
/// ⚠️ При скрытом системном навбаре в iOS 16 может перестать работать свайп
/// «назад» от края экрана. Если на экране это критично — используйте системный
/// навбар с `.toolbar { ToolbarItem { DSNavPillButton(...) } }` и
/// `.navigationTitle` (цвета/шрифты — из кита).
struct DSNavBar<Trailing: View>: View {
    enum Style {
        /// pill-кнопки (основные экраны-формы: Маршрут, Локомотив)
        case pill
        /// текстовый «‹ Настройки» + заголовок 17/600 + нижняя граница (справочники)
        case text(backLabel: String)
    }

    let title: String
    var style: Style = .pill
    var onBack: (() -> Void)? = nil
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        switch style {
        case .pill:
            pillBar
        case let .text(backLabel):
            textBar(backLabel: backLabel)
        }
    }

    private var pillBar: some View {
        ZStack {
            Text(title)
                .dsTextStyle(.navTitle)
                .lineLimit(1)
                .padding(.horizontal, 64)
            HStack {
                if let onBack = onBack {
                    DSNavPillButton(icon: .chevronLeft, accessibilityLabel: "Назад", action: onBack)
                } else {
                    Color.clear.frame(width: DSMetrics.navBtnSize, height: DSMetrics.navBtnSize)
                }
                Spacer()
                trailing()
            }
        }
        .padding(.horizontal, DSMetrics.screenPadX)
        .padding(.vertical, 12)
        .background(DSColor.bg)
    }

    private func textBar(backLabel: String) -> some View {
        ZStack {
            Text(title)
                .font(DSFont.sans(17, .semibold))
                .foregroundColor(DSColor.text)
                .lineLimit(1)
                .padding(.horizontal, 88)
            HStack {
                if let onBack = onBack {
                    Button(action: onBack) {
                        HStack(spacing: 2) {
                            DSIcon.chevronLeft.image.font(.system(size: 17, weight: .semibold))
                            Text(backLabel).font(DSFont.sans(16)).lineLimit(1)
                        }
                        .foregroundColor(DSColor.accent)
                        .padding(.vertical, 4)
                        .padding(.horizontal, 6)
                    }
                    .buttonStyle(.plain)
                }
                Spacer()
                trailing()
            }
            .padding(.leading, 8)
            .padding(.trailing, 12)
        }
        .frame(height: 44)
        .background(DSColor.bg)
        .overlay(alignment: .bottom) { DSDivider(inset: 0) }
    }
}

extension DSNavBar where Trailing == EmptyView {
    init(title: String, style: Style = .pill, onBack: (() -> Void)? = nil) {
        self.title = title
        self.style = style
        self.onBack = onBack
        self.trailing = { EmptyView() }
    }
}

/// Кнопка-пилюля навбара (`NavPillBtn`): иконочная 40×40 или текстовая
/// (padding 14, 15/600). surface + тень sm.
struct DSNavPillButton: View {
    private let icon: DSIcon?
    private let title: String?
    private let isAccent: Bool
    private let label: String
    private let action: () -> Void

    init(icon: DSIcon, accessibilityLabel: String, action: @escaping () -> Void) {
        self.icon = icon
        self.title = nil
        self.isAccent = false
        self.label = accessibilityLabel
        self.action = action
    }

    init(title: String, isAccent: Bool = true, action: @escaping () -> Void) {
        self.icon = nil
        self.title = title
        self.isAccent = isAccent
        self.label = title
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Group {
                if let icon = icon {
                    icon.image
                        .font(.system(size: 16, weight: .semibold))
                        .frame(width: DSMetrics.navBtnSize, height: DSMetrics.navBtnSize)
                } else {
                    Text(title ?? "")
                        .font(DSFont.sans(15, .semibold))
                        .padding(.horizontal, DSMetrics.navBtnPadX)
                        .frame(height: DSMetrics.navBtnSize)
                }
            }
            .foregroundColor(isAccent ? DSColor.accent : DSColor.text)
            .background(DSColor.surface)
            .clipShape(Capsule())
        }
        .buttonStyle(DSPressableStyle())
        .dsShadow(.sm)
        .accessibilityLabel(Text(label))
    }
}

extension View {
    /// Прячет системный навбар (для экранов с `DSNavBar`). iOS 16+.
    func dsHideSystemNavBar() -> some View {
        toolbar(.hidden, for: .navigationBar)
    }
}

#Preview("Навбар — light") {
    DSNavBarPreview().preferredColorScheme(.light)
}

#Preview("Навбар — dark") {
    DSNavBarPreview().preferredColorScheme(.dark)
}

private struct DSNavBarPreview: View {
    var body: some View {
        VStack(spacing: 24) {
            DSNavBar(title: "Маршрут", onBack: {}, trailing: {
                DSNavPillButton(title: "Сохранить") {}
            })
            DSNavBar(title: "Серии локомотивов", style: .text(backLabel: "Настройки"), onBack: {}, trailing: {
                Button("Готово") {}
                    .font(DSTextStyle.navActionAccent.font)
                    .foregroundColor(DSColor.accent)
            })
            DSNavBar(title: "Обозначения", onBack: {})
            Spacer()
        }
        .background(DSColor.bg.ignoresSafeArea())
    }
}
