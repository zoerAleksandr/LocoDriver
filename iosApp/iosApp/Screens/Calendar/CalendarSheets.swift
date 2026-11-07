import SwiftUI
import ComposeApp

// Шторки Календаря: «Что добавить?» (§14.3), «Технические занятия» (§14.6),
// продолжительность работы (§14/§14.4), «Своё время явки» (§14.4).
// Макет: calendar-variants-de.jsx (AddEventSheetE), AppBottomSheet (§24.1).

// MARK: - §14.3 AddEventSheet «Что добавить?»

struct AddEventSheetView: View {
    let day: Int
    let monthLabel: String
    let onRoute: () -> Void
    let onAbsence: () -> Void
    let onDayOff: () -> Void
    let onTechStudy: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        DSBottomSheet(
            header: .titleClose(title: "Что добавить?", subtitle: subtitle),
            onClose: { dismiss() }
        ) {
            VStack(spacing: 0) {
                row(title: "Маршрут", color: DSColor.accent) {
                    dismiss()
                    onRoute()
                }
                row(title: "Отвлечение", color: DSColor.warning) {
                    dismiss()
                    onAbsence()
                }
                row(title: "Выходной", color: DSColor.success) {
                    dismiss()
                    onDayOff()
                }
                row(title: "Технические занятия", color: DSColor.text, showsDivider: false) {
                    dismiss()
                    onTechStudy()
                }
            }
            .padding(.top, 8)
        }
    }

    private var subtitle: String {
        let month = monthLabel.split(separator: " ").first.map(String.init) ?? ""
        return "На \(day) \(month.lowercased())"
    }

    private func row(
        title: String,
        color: Color,
        showsDivider: Bool = true,
        action: @escaping () -> Void
    ) -> some View {
        VStack(spacing: 0) {
            Button(action: action) {
                HStack(spacing: 12) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(color)
                        .frame(width: 4, height: 28)
                    Text(title)
                        .font(DSFont.sans(15, .medium))
                        .foregroundColor(DSColor.text)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(DSColor.textFaint)
                }
                .padding(.vertical, 14)
                .contentShape(Rectangle())
            }
            .buttonStyle(DSRowPressStyle())
            if showsDivider {
                DSDivider(inset: 24)
            }
        }
    }
}

// MARK: - §14.6 TechnicalStudyDialog «Технические занятия»

struct TechnicalStudyDialog: View {
    let day: Int
    let monthLabel: String
    let averagePayText: (Double) -> String
    let onSave: (Double) -> Void
    @Environment(\.dismiss) private var dismiss

    @State private var hour: Int = 2
    @State private var minute: Int = 0

    private var hours: Double { Double(hour) + Double(minute) / 60.0 }

    var body: some View {
        DSBottomSheet(
            header: .titleClose(title: "Технические занятия", subtitle: subtitle),
            onClose: { dismiss() }
        ) {
            VStack(spacing: 16) {
                HStack(spacing: 0) {
                    Picker("Часы", selection: $hour) {
                        ForEach(0..<24, id: \.self) { Text("\($0)").tag($0) }
                    }
                    .pickerStyle(.wheel)
                    .frame(maxWidth: .infinity)
                    Picker("Минуты", selection: $minute) {
                        ForEach(0..<60, id: \.self) { Text("\($0)").tag($0) }
                    }
                    .pickerStyle(.wheel)
                    .frame(maxWidth: .infinity)
                }
                .frame(height: 140)

                payBlock

                HStack(spacing: 10) {
                    DSTonalButton(title: "Отмена", tone: .neutral) { dismiss() }
                    DSCTAButton(title: "Сохранить", isEnabled: hours > 0) {
                        onSave(hours)
                        dismiss()
                    }
                }
            }
            .padding(.top, 4)
        }
    }

    private var subtitle: String {
        let month = monthLabel.split(separator: " ").first.map(String.init) ?? ""
        return "На \(day) \(month.lowercased())"
    }

    @ViewBuilder
    private var payBlock: some View {
        let pay = averagePayText(hours)
        if pay.isEmpty {
            Text("Средний час не задан в настройках зарплаты — сумма не рассчитается. Часы сохранятся.")
                .font(DSFont.sans(13))
                .foregroundColor(DSColor.warning)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(DSColor.warning.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        } else {
            HStack {
                Text("К начислению")
                    .font(DSFont.sans(13))
                    .foregroundColor(DSColor.textMuted)
                Spacer()
                Text("\(pay) ₽")
                    .dsTextStyle(.valueMono)
            }
            .padding(12)
            .background(DSColor.accentSoft)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }
}

// MARK: - Шторка продолжительности работы (§14 / §14.4 «ЧЧ:ММ»)

struct PlanDurationSheet: View {
    let suggestedMillis: Int64
    let onConfirm: (Int64) -> Void
    let onWithoutEnd: () -> Void
    @Environment(\.dismiss) private var dismiss

    /// Четыре цифры ЧЧММ; двоеточие подставляется автоматически (§14).
    @State private var digits: String = ""

    private var hourPart: Int { Int(digits.prefix(2)) ?? 0 }
    private var minutePart: Int { digits.count >= 4 ? Int(digits.suffix(2)) ?? 0 : 0 }
    private var minutesValid: Bool { digits.count < 4 || minutePart <= 59 }
    private var durationMillis: Int64? {
        guard digits.count == 4, minutesValid else { return nil }
        let total = Int64(hourPart) * 3_600_000 + Int64(minutePart) * 60_000
        return total > 0 ? total : nil
    }

    var body: some View {
        DSBottomSheet(
            header: .cancelDone(
                title: "Продолжительность работы",
                cancel: "Отмена",
                done: "Указать",
                onDone: {
                    if let ms = durationMillis {
                        onConfirm(ms)
                        dismiss()
                    }
                }
            ),
            showsGrabber: false,
            onClose: { dismiss() }
        ) {
            VStack(spacing: 20) {
                Text("Окончание рассчитается от времени явки. Введите четыре цифры — часы и минуты.")
                    .font(DSFont.sans(13))
                    .foregroundColor(DSColor.textMuted)
                    .frame(maxWidth: .infinity, alignment: .leading)

                ZStack {
                    Text(formatted)
                        .font(DSFont.mono(24, .semibold))
                        .foregroundColor(minutesValid ? DSColor.text : DSColor.danger)
                    // Прозрачное поле принимает цифры с клавиатуры; двоеточие
                    // подставляется в отображении (§14: 4 цифры).
                    TextField("", text: Binding(
                        get: { digits },
                        set: { newValue in
                            digits = String(newValue.filter { $0.isNumber }.prefix(4))
                        }
                    ))
                    .keyboardType(.numberPad)
                    .opacity(0.02)
                    .foregroundColor(.clear)
                    .accentColor(.clear)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 60)
                .background(DSColor.surfaceAlt)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(minutesValid ? DSColor.border : DSColor.danger, lineWidth: 1)
                )

                if !minutesValid {
                    Text("Минуты — от 00 до 59.")
                        .font(DSFont.sans(12))
                        .foregroundColor(DSColor.danger)
                }

                VStack(spacing: 10) {
                    DSCTAButton(title: "Указать", isEnabled: durationMillis != nil) {
                        if let ms = durationMillis {
                            onConfirm(ms)
                            dismiss()
                        }
                    }
                    DSTonalButton(title: "Без окончания", tone: .neutral) {
                        onWithoutEnd()
                        dismiss()
                    }
                }
            }
            .padding(.top, 8)
        }
        .onAppear {
            let totalMinutes = Int(suggestedMillis / 60_000)
            digits = String(format: "%02d%02d", totalMinutes / 60, totalMinutes % 60)
        }
    }

    private var formatted: String {
        let h = digits.count >= 2 ? String(digits.prefix(2)) : digits.padding(toLength: 2, withPad: "0", startingAt: 0)
        let m = digits.count >= 4 ? String(digits.suffix(2)) : (digits.count > 2 ? String(digits.dropFirst(2)).padding(toLength: 2, withPad: "0", startingAt: 0) : "00")
        return "\(h):\(m)"
    }
}

// MARK: - §14.4 «Своё время явки»

struct PlanCustomTimeSheet: View {
    let onDone: (Int, Int) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var date = Calendar.current.date(
        from: DateComponents(hour: 8, minute: 0)
    ) ?? Date()

    var body: some View {
        DSBottomSheet(
            header: .cancelDone(
                title: "Своё время явки",
                cancel: "Отмена",
                done: "Готово",
                onDone: {
                    let comps = Calendar.current.dateComponents([.hour, .minute], from: date)
                    onDone(comps.hour ?? 8, comps.minute ?? 0)
                    dismiss()
                }
            ),
            showsGrabber: false,
            onClose: { dismiss() }
        ) {
            DatePicker("", selection: $date, displayedComponents: .hourAndMinute)
                .datePickerStyle(.wheel)
                .labelsHidden()
                .environment(\.locale, Locale(identifier: "ru_RU"))
                .frame(maxWidth: .infinity)
        }
    }
}
