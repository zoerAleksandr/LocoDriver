import SwiftUI
import ComposeApp

// Общие элементы экранов «Пассажиром» (§7) и «Прочая работа» (§8): навбар, поле станции
// с автодополнением, строка даты/времени, бейдж длительности, поле заметок.
// Макет «Пассажиром»: design/src/pass-screen.jsx (PassGroup, StationLegCard, DurationBadge, FlatCard).

/// Какое из двух полей времени редактируется (пикер / удаление значения).
enum PassTimeTarget: String, Identifiable {
    case first, second
    var id: String { rawValue }
}

/// «7 ч 40 мин» (`formatTravelDuration` / `formatOwDuration`).
func passDurationText(_ ms: Int64?) -> String {
    let total = Int((ms ?? 0) / 60_000)
    let h = total / 60
    let m = total % 60
    if h > 0 && m > 0 { return "\(h) ч \(m) мин" }
    if h > 0 { return "\(h) ч" }
    return "\(m) мин"
}

func hideKeyboard() {
    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
}

/// Топ-бар форм (§7.3 п.1, §8.3 п.1): слева «Готово», по центру заголовок.
struct PassNavBar: View {
    let title: String
    let onDone: () -> Void

    var body: some View {
        ZStack {
            Text(title).dsTextStyle(.navTitle).lineLimit(1)
            HStack {
                DSNavPillButton(title: "Готово") {
                    hideKeyboard()
                    onDone()
                }
                Spacer()
            }
        }
        .padding(.horizontal, DSMetrics.screenPadX)
        .padding(.vertical, 12)
        .background(DSColor.bg)
    }
}

/// Бейдж длительности между карточками: «В пути · 7 ч 40 мин» / «Длительность · …».
struct PassDurationBadge: View {
    let text: String

    var body: some View {
        HStack(spacing: 6) {
            DSIcon.clock.image.font(.system(size: 12, weight: .semibold))
            Text(text).font(DSFont.mono(13, .semibold))
        }
        .foregroundColor(DSColor.accent)
        .padding(.horizontal, 12)
        .padding(.vertical, 7)
        .background(DSColor.accentSoft)
        .clipShape(Capsule())
        .frame(maxWidth: .infinity)
        .padding(.vertical, 2)
    }
}

/// Строка даты/времени (§7.3 п.5, §8.3 п.5): слева метка, справа `dd.MM.yy HH:mm` (mono),
/// пусто — «Выбрать». Тап — пикер; долгое нажатие по заполненной — «Удалить значение».
struct PassDateTimeRow: View {
    let label: String
    let valueText: String?
    var showsDivider: Bool = false
    let onTap: () -> Void
    var onLongPress: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 0) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: DSMetrics.rowGap) {
                    Text(label).dsTextStyle(.labelMuted)
                    Spacer(minLength: 8)
                    valueView
                }
                // Крупный системный шрифт: пояснение и значение — в столбец.
                VStack(alignment: .leading, spacing: 4) {
                    Text(label).dsTextStyle(.labelMuted)
                    valueView
                }
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, DSMetrics.rowPadY)
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
            .onTapGesture(perform: onTap)
            .onLongPressGesture {
                if valueText != nil { onLongPress?() }
            }
            if showsDivider { DSDivider() }
        }
    }

    private var valueView: some View {
        Text(valueText ?? "Выбрать")
            .font(DSFont.mono(15, .semibold))
            .foregroundColor(valueText == nil ? DSColor.textFaint : DSColor.text)
            .lineLimit(1)
    }
}

/// Текстовое поле станции с автодополнением (§7.3 п.5, §8.3 п.4): список раскрывается по фокусу
/// (весь список), при вводе фильтруется; тап по подсказке подставляет значение, «✕» убирает станцию
/// из общего списка.
struct PassStationField: View {
    let placeholder: String
    let external: String
    let suggestions: (String) -> [String]
    let onChange: (String) -> Void
    let onRemoveSuggestion: (String) -> Void

    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            TextField(placeholder, text: $text)
                .focused($focused)
                .font(DSFont.sans(17, .medium))
                .foregroundColor(DSColor.text)
                .submitLabel(.done)
                .autocorrectionDisabled()
                .padding(.horizontal, DSMetrics.rowPadX)
                .padding(.vertical, DSMetrics.rowPadY)
            if focused {
                let items = suggestions(text)
                if !items.isEmpty {
                    DSDivider()
                    ScrollView {
                        VStack(spacing: 0) {
                            ForEach(Array(items.enumerated()), id: \.element) { i, name in
                                HStack {
                                    Button {
                                        text = name
                                        onChange(name)
                                        focused = false
                                    } label: {
                                        Text(name).font(DSFont.sans(16)).foregroundColor(DSColor.text)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            .contentShape(Rectangle())
                                    }
                                    .buttonStyle(.plain)
                                    Button { onRemoveSuggestion(name) } label: {
                                        DSIcon.close.image.font(.system(size: 12, weight: .bold)).foregroundColor(DSColor.textFaint)
                                            .padding(8)
                                    }
                                    .buttonStyle(.plain)
                                    .accessibilityLabel(Text("Убрать из подсказок"))
                                }
                                .padding(.horizontal, DSMetrics.rowPadX)
                                .padding(.vertical, 8)
                                if i < items.count - 1 { DSDivider() }
                            }
                        }
                    }
                    .frame(maxHeight: 220)
                }
            }
        }
        .onAppear { text = external }
        .onChange(of: text) { value in
            if value != external { onChange(value) }
        }
        .onChange(of: external) { value in
            if !focused { text = value }
        }
    }
}

/// Многострочное поле заметок (§7.3 п.8, §8.3 п.6).
struct PassNotesField: View {
    let placeholder: String
    let external: String
    let onChange: (String) -> Void

    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        TextField(placeholder, text: $text, axis: .vertical)
            .lineLimit(3...8)
            .focused($focused)
            .font(DSFont.sans(16))
            .foregroundColor(DSColor.text)
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, DSMetrics.rowPadY)
            .frame(maxWidth: .infinity, alignment: .leading)
            .onAppear { text = external }
            .onChange(of: text) { value in
                if value != external { onChange(value) }
            }
            .onChange(of: external) { value in
                if !focused { text = value }
            }
    }
}

/// Пикер даты/времени с «Удалить значение» (§24.2): шторка пикера и центральный диалог удаления.
struct PassPickerHost: ViewModifier {
    @Binding var picker: PassTimeTarget?
    @Binding var clear: PassTimeTarget?
    let offsetSeconds: Int
    let title: (PassTimeTarget) -> String
    let deleteTitle: (PassTimeTarget) -> String
    let current: (PassTimeTarget) -> Int64?
    let onPick: (PassTimeTarget, Int64) -> Void
    let onClear: (PassTimeTarget) -> Void

    private var format: RouteTimeFormat { RouteTimeFormat(offsetSeconds: offsetSeconds) }

    func body(content: Content) -> some View {
        content
            .sheet(item: $picker) { target in
                DSDateTimeSheet(
                    title: title(target),
                    timeZone: format.timeZone,
                    initial: current(target).map(TimeFormatter.msToDate) ?? Date(),
                    onDone: { ms in
                        onPick(target, ms)
                        picker = nil
                    },
                    onCancel: { picker = nil }
                )
                .presentationDetents([.height(380)])
                .presentationDragIndicator(.hidden)
            }
            .overlay {
                if let target = clear {
                    RouteConfirmDialog(
                        model: RouteConfirmModel(
                            title: deleteTitle(target),
                            message: current(target).map { format.dateTime($0) },
                            actions: [.init(title: "Удалить значение", role: .destructive) { onClear(target) }]
                        ),
                        onCancel: { clear = nil },
                        onAction: { action in
                            action.perform()
                            clear = nil
                        }
                    )
                }
            }
    }
}

extension View {
    /// Прокрутка скрывает клавиатуру (iOS 16+).
    @ViewBuilder
    func passScrollDismissesKeyboard() -> some View {
        if #available(iOS 16.0, *) { self.scrollDismissesKeyboard(.interactively) } else { self }
    }
}
