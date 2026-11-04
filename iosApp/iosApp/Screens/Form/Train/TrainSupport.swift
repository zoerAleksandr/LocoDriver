import SwiftUI
import ComposeApp

// Общие вспомогательные вещи экрана «Поезд» (SCREEN_SPECS §6).

enum TrainTime {
    /// «HH:mm» в заданном смещении от GMT; nil → «—».
    static func hm(_ ms: Int64?, offset: Int) -> String {
        guard let ms = ms else { return "—" }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(secondsFromGMT: offset)
        f.dateFormat = "HH:mm"
        return f.string(from: TimeFormatter.msToDate(ms))
    }

    /// «dd.MM.yy» в заданном смещении.
    static func date(_ ms: Int64?, offset: Int) -> String {
        guard let ms = ms else { return "" }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(secondsFromGMT: offset)
        f.dateFormat = "dd.MM.yy"
        return f.string(from: TimeFormatter.msToDate(ms))
    }

    /// Текущее время, обрезанное до минуты.
    static func nowMinute() -> Int64 {
        let ms = TimeFormatter.dateToMs(Date())
        return ms - ms % 60_000
    }
}

/// Название станции со списком подсказок (общий справочник станций, §6.4).
/// Подсказки скрыты, если текст полностью совпадает с одним из вариантов; долгое нажатие по
/// подсказке убирает её из справочника.
struct TrainStationNameField: View {
    let title: String
    @Binding var text: String
    let suggest: (String) -> [String]
    let onRemove: (String) -> Void
    var placeholder: String = "Станция"

    @FocusState private var focused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).dsTextStyle(.captionMuted)
            TextField(placeholder, text: $text)
                .focused($focused)
                .font(DSFont.sans(16))
                .foregroundColor(DSColor.text)
                .autocorrectionDisabled()
                .padding(.horizontal, 14)
                .frame(height: 48)
                .background(DSColor.surface)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous).stroke(DSColor.border, lineWidth: 1))
            let items = focused ? suggest(text) : []
            if !items.isEmpty {
                VStack(spacing: 0) {
                    ForEach(Array(items.prefix(6).enumerated()), id: \.offset) { index, item in
                        Button { text = item; focused = false } label: {
                            Text(item)
                                .font(DSFont.sans(15))
                                .foregroundColor(DSColor.text)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 11)
                        }
                        .buttonStyle(.plain)
                        .contextMenu {
                            Button(role: .destructive) { onRemove(item) } label: { Text("Убрать из справочника") }
                        }
                        if index < min(items.count, 6) - 1 { DSDivider(inset: 14) }
                    }
                }
                .background(DSColor.surface)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous).stroke(DSColor.border, lineWidth: 1))
            }
        }
    }
}

/// Подписанное поле ввода (рамка + заголовок) для шторок.
struct TrainLabeledField: View {
    let title: String
    let placeholder: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default
    var prefix: String? = nil
    var mono: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).dsTextStyle(.captionMuted)
            HStack(spacing: 6) {
                if let prefix = prefix, !text.isEmpty {
                    Text(prefix).font(DSFont.sans(16)).foregroundColor(DSColor.textMuted)
                }
                TextField(placeholder, text: $text)
                    .keyboardType(keyboard)
                    .font(mono ? DSFont.mono(16, .medium) : DSFont.sans(16))
                    .foregroundColor(DSColor.text)
                    .autocorrectionDisabled()
            }
            .padding(.horizontal, 14)
            .frame(height: 48)
            .background(DSColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous).stroke(DSColor.border, lineWidth: 1))
        }
    }
}

/// Строка «добавить / ✕» заголовка раздела в шторках.
struct TrainSectionToggleHeader: View {
    let title: String
    let isPresent: Bool
    let onAdd: () -> Void
    let onRemove: () -> Void

    var body: some View {
        HStack {
            Text(title).dsTextStyle(.group)
            Spacer()
            if isPresent {
                DSIconButton(icon: .close, variant: .subtle, size: 28, accessibilityLabel: "Удалить раздел «\(title)»", action: onRemove)
            } else {
                DSTextAction(title: "Добавить", leadingIcon: .plus, action: onAdd)
            }
        }
        .padding(.top, 8)
    }
}

extension View {
    @ViewBuilder
    func trainScrollDismissesKeyboard() -> some View {
        if #available(iOS 16.0, *) { self.scrollDismissesKeyboard(.interactively) } else { self }
    }
}
