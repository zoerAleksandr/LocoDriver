import SwiftUI

// Макет: design/src/time-sheet.jsx → TimeSheetIOS (шапка «Отмена · Заголовок · Готово»).
// Контракт: SCREEN_SPECS §24.2 (`AppDateTimePicker`): один момент в поясе отображения,
// результат обрезается до минуты, «Отмена» значение не меняет, шторка не закрывается свайпом.

/// Нативная шторка выбора даты и времени (`AppDateTimePicker`, §24.2): редактирует один
/// момент в поясе отображения; результат обрезается до минуты; «Отмена» ничего не меняет.
struct DSDateTimeSheet: View {
    let title: String
    let timeZone: TimeZone
    let initial: Date
    /// Последние выбранные значения (§24.2): тап по чипу подставляет значение в барабан.
    var recent: [Date] = []
    let onDone: (Int64) -> Void
    let onCancel: () -> Void

    @State private var value: Date

    init(title: String, timeZone: TimeZone, initial: Date, recent: [Date] = [], onDone: @escaping (Int64) -> Void, onCancel: @escaping () -> Void) {
        self.title = title
        self.timeZone = timeZone
        self.initial = initial
        self.recent = recent
        self.onDone = onDone
        self.onCancel = onCancel
        _value = State(initialValue: initial)
    }

    private func chipText(_ date: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = timeZone
        f.dateFormat = "dd.MM HH:mm"
        return f.string(from: date)
    }

    var body: some View {
        VStack(spacing: 0) {
            // Отступ над «Отмена / Готово»: у шторки без грэббера шапка иначе прижата к краю.
            Color.clear.frame(height: 16)
            sheetBody
        }
        .background(DSColor.bg.ignoresSafeArea())
        .interactiveDismissDisabled()
    }

    private var sheetBody: some View {
        DSBottomSheet(
            header: .cancelDone(title: title, cancel: "Отмена", done: "Готово", onDone: {
                let ms = TimeFormatter.dateToMs(value)
                onDone(ms - ms % 60_000)
            }),
            showsGrabber: false,
            onClose: onCancel
        ) {
            if !recent.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(recent.enumerated()), id: \.offset) { _, date in
                            DSChip(title: chipText(date)) { value = date }
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            DatePicker("", selection: $value, displayedComponents: [.date, .hourAndMinute])
                .datePickerStyle(.wheel)
                .labelsHidden()
                .environment(\.timeZone, timeZone)
                .environment(\.locale, Locale(identifier: "ru_RU"))
                .frame(maxWidth: .infinity)
        }
    }
}

#Preview("DSDateTimeSheet — light") {
    DSDateTimeSheet(title: "Явка", timeZone: TimeZone(secondsFromGMT: 3 * 3600)!, initial: Date(), onDone: { _ in }, onCancel: {})
        .preferredColorScheme(.light)
}

#Preview("DSDateTimeSheet — dark") {
    DSDateTimeSheet(title: "Явка", timeZone: TimeZone(secondsFromGMT: 3 * 3600)!, initial: Date(), onDone: { _ in }, onCancel: {})
        .preferredColorScheme(.dark)
}
