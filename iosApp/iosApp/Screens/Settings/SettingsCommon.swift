import SwiftUI

// SCREEN_SPECS §17: общие строки и шторки Настроек.
// Макет: design/src/settings-subscreens.jsx, design/src/ios-screens.jsx (IOSScreenSettings*).

/// Каркас под-экрана: фон, прокрутка, заголовок по центру и синее «Готово» слева (§17.1).
struct SettingsScaffold<Content: View>: View {
    let title: String
    var showsDone: Bool = true
    var onDisappear: (() -> Void)? = nil
    @ViewBuilder let content: () -> Content
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0, content: content)
                .padding(.horizontal, DSSpacing.lg)
                .padding(.bottom, DSSpacing.xxxl)
        }
        .background(DSColor.bg.ignoresSafeArea())
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(showsDone)
        .toolbar {
            if showsDone {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Готово") { dismiss() }
                        .foregroundColor(DSColor.accent)
                }
            }
        }
        .onDisappear { onDisappear?() }
    }
}

/// Подпись под группой.
struct SettingsHint: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text)
            .dsTextStyle(.captionMuted)
            .padding(.horizontal, DSMetrics.groupHeadPadX)
            .padding(.top, DSSpacing.sm)
            .fixedSize(horizontal: false, vertical: true)
    }
}

/// Строка с переключателем.
struct SettingsToggleRow: View {
    let title: String
    var subtitle: String? = nil
    let isOn: Bool
    var showsDivider: Bool = true
    let onChange: (Bool) -> Void

    var body: some View {
        VStack(spacing: 0) {
            Toggle(isOn: Binding(get: { isOn }, set: onChange)) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(DSFont.sans(15, .medium)).foregroundColor(DSColor.text)
                    if let subtitle = subtitle {
                        Text(subtitle).dsTextStyle(.captionMuted)
                    }
                }
            }
            .tint(DSColor.accent)
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            .frame(minHeight: 52)
            if showsDivider { DSDivider(inset: 18) }
        }
    }
}

/// Строка с радио-кружком.
struct SettingsRadioRow: View {
    let title: String
    var subtitle: String? = nil
    var leading: String? = nil
    let isSelected: Bool
    var showsDivider: Bool = true
    let action: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Button(action: action) {
                HStack(spacing: 12) {
                    if let leading = leading { Text(leading).font(.system(size: 22)) }
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(DSFont.sans(15, isSelected ? .bold : .medium))
                            .foregroundColor(DSColor.text)
                        if let subtitle = subtitle {
                            Text(subtitle).dsTextStyle(.captionMuted)
                        }
                    }
                    Spacer(minLength: 8)
                    ZStack {
                        Circle()
                            .strokeBorder(isSelected ? DSColor.accent : DSColor.borderStrong, lineWidth: 2)
                            .frame(width: 22, height: 22)
                        if isSelected {
                            Circle().fill(DSColor.accent).frame(width: 12, height: 12)
                        }
                    }
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 12)
                .frame(minHeight: 52)
                .contentShape(Rectangle())
            }
            .buttonStyle(DSRowPressStyle())
            if showsDivider { DSDivider(inset: 18) }
        }
    }
}

/// Строка «название — значение» с шевроном (открывает шторку выбора).
struct SettingsValueRow: View {
    let title: String
    let value: String
    var mono: Bool = false
    var showsChevron: Bool = true
    var showsDivider: Bool = true
    var action: (() -> Void)? = nil

    var body: some View {
        DSListRow(title: title, value: value, valueIsMono: mono,
                  showsChevron: showsChevron && action != nil,
                  showsDivider: showsDivider, action: action)
    }
}

/// Один вариант `SettingsPickerSheet`.
struct SettingsPickerOption: Identifiable {
    let id: String
    let title: String
    var flag: String? = nil
}

/// `SettingsPickerSheet(title, hint?, options)` — одиночный выбор; тап по строке выбирает и сразу закрывает (§17.2).
struct SettingsPickerSheet: View {
    let title: String
    var hint: String? = nil
    let options: [SettingsPickerOption]
    let selectedId: String
    let onSelect: (String) -> Void
    let onClose: () -> Void

    var body: some View {
        DSBottomSheet(header: .titleClose(title: title, subtitle: hint), onClose: onClose) {
            DSCard {
                ForEach(Array(options.enumerated()), id: \.element.id) { index, option in
                    SettingsRadioRow(
                        title: option.title, leading: option.flag,
                        isSelected: option.id == selectedId,
                        showsDivider: index < options.count - 1
                    ) {
                        onSelect(option.id)
                        onClose()
                    }
                }
            }
        }
    }
}

/// Шторка выбора длительности «ЧЧ:ММ» (барабаны). Жесты шторки отключены (§17.5 «Отдых»).
struct SettingsDurationSheet: View {
    let title: String
    let maxHours: Int
    let initialMillis: Int64
    let onSave: (Int64) -> Void
    let onClose: () -> Void

    @State private var hours: Int
    @State private var minutes: Int

    init(title: String, maxHours: Int, initialMillis: Int64,
         onSave: @escaping (Int64) -> Void, onClose: @escaping () -> Void) {
        self.title = title
        self.maxHours = maxHours
        self.initialMillis = initialMillis
        self.onSave = onSave
        self.onClose = onClose
        let total = Int(initialMillis / 60_000)
        _hours = State(initialValue: min(total / 60, maxHours))
        _minutes = State(initialValue: total % 60)
    }

    var body: some View {
        DSBottomSheet(
            header: .cancelDone(title: title, cancel: "Отмена", done: "Готово", onDone: {
                onSave((Int64(hours) * 60 + Int64(minutes)) * 60_000)
                onClose()
            }),
            showsGrabber: false,
            onClose: onClose
        ) {
            HStack(spacing: 0) {
                Picker("Часы", selection: $hours) {
                    ForEach(0...maxHours, id: \.self) { Text(String(format: "%02d", $0)).tag($0) }
                }
                .pickerStyle(.wheel)
                Text(":").font(DSFont.mono(22, .semibold))
                Picker("Минуты", selection: $minutes) {
                    ForEach(0..<60, id: \.self) { Text(String(format: "%02d", $0)).tag($0) }
                }
                .pickerStyle(.wheel)
            }
            .frame(height: 180)
        }
        .interactiveDismissDisabled()
    }
}

enum SettingsFormat {
    /// Длительность в мс → «ЧЧ:ММ».
    static func hhmm(_ millis: Int64) -> String {
        let total = Int(millis / 60_000)
        return String(format: "%02d:%02d", total / 60, total % 60)
    }

    /// Склонение (§17): n%10==1 && n%100!=11 → one; n%10 in 2..4 && n%100 not in 12..14 → few; иначе many.
    static func plural(_ n: Int, _ one: String, _ few: String, _ many: String) -> String {
        let m10 = n % 10, m100 = n % 100
        let form = (m10 == 1 && m100 != 11) ? one : ((2...4).contains(m10) && !(12...14).contains(m100)) ? few : many
        return "\(n) \(form)"
    }

    static let months = ["Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                         "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"]
}

/// Системное окно «Поделиться» (экспорт диагностики, §17.6).
struct SettingsShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
