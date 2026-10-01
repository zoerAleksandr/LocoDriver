import SwiftUI

// Макет: design/src/ios-frame.jsx → IOSList / IOSListRow (геометрия: строка ≥52,
//        разделитель после иконки, шеврон справа);
//        design/src/settings-screens.jsx → SettingsRow (иконка-аватар 32, label 15/500,
//        sub captionMuted, value 14 textMuted, бейдж, шеврон) — цвета/шрифты по токенам;
//        design/src/shared-ui.jsx → FieldRow, ActionRow.

/// Сгруппированный список: необязательный заголовок группы + карточка со строками.
/// Разделители рисуют сами строки (`showsDivider`), последней строке передайте `false`.
struct DSList<Content: View>: View {
    var header: String? = nil
    var headerTopSpacing: CGFloat = DSMetrics.groupHeadMt
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let header = header {
                DSGroupHeader(header, topSpacing: headerTopSpacing)
            }
            DSCard(content: content)
        }
    }
}

/// Строка списка-ссылки: [иконка] заголовок / подзаголовок … значение [бейдж] ›
struct DSListRow: View {
    let title: String
    var subtitle: String? = nil
    var value: String? = nil
    /// true — значение mono (числа, идентификаторы, время), см. «Правила шрифтов.md».
    var valueIsMono: Bool = false
    var icon: DSIcon? = nil
    var iconTone: DSIconAvatar.Tone = .neutral
    var badge: DSPill? = nil
    var showsChevron: Bool = true
    var showsDivider: Bool = true
    var titleColor: Color = DSColor.text
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 0) {
            if let action = action {
                Button(action: action) { rowContent }
                    .buttonStyle(DSRowPressStyle())
            } else {
                rowContent
            }
            if showsDivider {
                DSDivider(inset: icon == nil ? 18 : 18 + DSMetrics.avatarMd + 14)
            }
        }
    }

    private var rowContent: some View {
        HStack(spacing: 14) {
            if let icon = icon {
                DSIconAvatar(icon: icon, size: .md, tone: iconTone, shape: .square)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(DSFont.sans(15, .medium))
                    .foregroundColor(titleColor)
                if let subtitle = subtitle {
                    Text(subtitle).dsTextStyle(.captionMuted)
                }
            }
            Spacer(minLength: DSSpacing.sm)
            if let value = value {
                Text(value)
                    .font(valueIsMono ? DSFont.mono(14) : DSFont.sans(14))
                    .foregroundColor(DSColor.textMuted)
                    .lineLimit(1)
            }
            if let badge = badge {
                badge
            }
            if showsChevron {
                DSIcon.chevronRight.image
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(DSColor.textFaint)
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 14)
        .frame(minHeight: 52)
        .contentShape(Rectangle())
    }
}

/// Строка «поле формы»: label слева … значение справа (`FieldRow`).
struct DSFieldRow: View {
    let label: String
    let value: String
    var valueIsMono: Bool = false
    var isPlaceholder: Bool = false
    var showsChevron: Bool = false
    var showsDivider: Bool = true
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 0) {
            if let action = action {
                Button(action: action) { content }
                    .buttonStyle(DSRowPressStyle())
            } else {
                content
            }
            if showsDivider { DSDivider() }
        }
    }

    private var content: some View {
        HStack(spacing: DSMetrics.rowGap) {
            Text(label).dsTextStyle(.labelMuted)
            Spacer(minLength: DSSpacing.sm)
            Text(value)
                .dsTextStyle(valueIsMono ? .valueMono : .value,
                             color: isPlaceholder ? DSColor.textFaint : DSColor.text)
            if showsChevron {
                DSIcon.chevronRight.image
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(DSColor.textFaint)
            }
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, DSMetrics.rowPadY)
        .contentShape(Rectangle())
    }
}

/// Строка-действие «Добавить …» с акцентной иконкой (`AddRow` / `ActionRow`).
struct DSAddRow: View {
    let title: String
    let icon: DSIcon
    var showsDivider: Bool = false
    let action: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Button(action: action) {
                HStack(spacing: DSMetrics.rowGap) {
                    DSIconAvatar(icon: icon, size: .lg, tone: .accent, shape: .square)
                    Text(title)
                        .font(DSFont.sans(16, .medium))
                        .foregroundColor(DSColor.accent)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, DSMetrics.rowPadX)
                .padding(.vertical, DSMetrics.rowPadYTouch)
                .contentShape(Rectangle())
            }
            .buttonStyle(DSRowPressStyle())
            if showsDivider { DSDivider(inset: 56) }
        }
    }
}

/// Подсветка строки при нажатии (фон `bgSubtle`).
struct DSRowPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? DSColor.bgSubtle : Color.clear)
    }
}

#Preview("Список — light") {
    DSListPreview().preferredColorScheme(.light)
}

#Preview("Список — dark") {
    DSListPreview().preferredColorScheme(.dark)
}

private struct DSListPreview: View {
    var body: some View {
        DSPreviewCanvas {
            DSList(header: "Справочники норм", headerTopSpacing: DSMetrics.groupHeadFirst) {
                DSListRow(title: "Серии локомотивов", value: "5 серий", icon: .locomotive, iconTone: .accent) {}
                DSListRow(title: "Станции", value: "5 станций", icon: .mapPin, iconTone: .accent) {}
                DSListRow(title: "Отдых", subtitle: "Нормы домашнего отдыха и в ПО", icon: .moon,
                          badge: DSPill(text: "Новое", tone: .success, isMono: false), showsDivider: false) {}
            }
            DSList(header: "Локомотив") {
                DSFieldRow(label: "Серия", value: "ВЛ80с", valueIsMono: true)
                DSFieldRow(label: "Тип", value: "Электровоз", showsChevron: true) {}
                DSAddRow(title: "Добавить локомотив", icon: .locomotive) {}
            }
        }
    }
}
