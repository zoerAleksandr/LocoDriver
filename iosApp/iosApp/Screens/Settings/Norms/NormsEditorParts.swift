import SwiftUI

// Общие элементы редакторов норм серии/станции (SCREEN_SPECS §17.4).
// Макет: design/src/norms-screens.jsx → Hint, FieldRow, DurationRow, DangerBtn.

/// Подсказка-плашка над группами (`Hint`).
struct NormsHint: View {
    let text: String
    var body: some View {
        Text(text)
            .font(DSFont.sans(13))
            .foregroundColor(DSColor.textMuted)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 14).padding(.vertical, 12)
            .background(DSColor.bgSubtle)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

/// Строка «подпись слева — поле ввода справа» (`FieldRow`). `suffix` — «км», «пк», «т» …
struct NormsInputRow: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default
    var alignRight: Bool = true
    var mono: Bool = false
    var suffix: String? = nil
    var showsDivider: Bool = true
    var isEnabled: Bool = true
    /// Значение в плашке «как время» в шторке (mono 18/600, рамка) — для числовых полей.
    var plate: Bool = false

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Text(label).font(plate ? DSFont.sans(14, .medium) : DSFont.sans(14))
                    .foregroundColor(plate ? DSColor.text : DSColor.textMuted)
                Spacer(minLength: 8)
                if plate {
                    HStack(spacing: 6) {
                        TextField(placeholder, text: $text)
                            .keyboardType(keyboard)
                            .multilineTextAlignment(.trailing)
                            .font(DSFont.mono(18, .semibold))
                            .foregroundColor(DSColor.text)
                            .disabled(!isEnabled)
                            .frame(width: 64)
                        if let suffix = suffix {
                            Text(suffix).font(DSFont.mono(13)).foregroundColor(DSColor.textMuted)
                        }
                    }
                    .padding(.horizontal, 14).padding(.vertical, 8)
                    .background(text.isEmpty ? DSColor.bgSubtle : DSColor.bg)
                    .overlay(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous)
                        .stroke(text.isEmpty ? Color.clear : DSColor.border, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
                } else {
                    TextField(placeholder, text: $text)
                        .keyboardType(keyboard)
                        .multilineTextAlignment(alignRight ? .trailing : .leading)
                        .font(mono ? DSFont.mono(15, .medium) : DSFont.sans(15, .medium))
                        .foregroundColor(DSColor.text)
                        .disabled(!isEnabled)
                        .frame(maxWidth: 220)
                    if let suffix = suffix {
                        Text(suffix).font(DSFont.sans(14)).foregroundColor(DSColor.textMuted)
                    }
                }
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, 12)
            .frame(minHeight: 52)
            if showsDivider { DSDivider() }
        }
    }
}

/// Строка нормы в минутах: [иконка] подпись … [−] [N мин] [+] (`DurationRow`).
/// Пустое поле или 0 = «не задано».
struct NormsStepperRow: View {
    let icon: DSIcon
    let label: String
    @Binding var text: String
    var maxValue: Int = 240
    var showsDivider: Bool = true

    private var value: Int { Int(text.trimmingCharacters(in: .whitespaces)) ?? 0 }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                DSIconAvatar(icon: icon, size: .sm, tone: .neutral)
                Text(label).font(DSFont.sans(14)).foregroundColor(DSColor.text)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: 4)
                stepButton("−", enabled: value > 0) { set(value - 1) }
                HStack(spacing: 4) {
                    TextField("—", text: $text)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .font(DSFont.mono(15, .semibold))
                        .foregroundColor(DSColor.text)
                        .frame(width: 40)
                    Text("мин").font(DSFont.mono(13)).foregroundColor(DSColor.textMuted)
                }
                .padding(.horizontal, 10).padding(.vertical, 6)
                .background(DSColor.surface)
                .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(DSColor.border, lineWidth: 1))
                stepButton("+", enabled: value < maxValue) { set(value + 1) }
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, 10)
            if showsDivider { DSDivider(inset: 56) }
        }
    }

    private func set(_ v: Int) {
        let clamped = max(0, min(maxValue, v))
        text = clamped == 0 ? "" : String(clamped)
    }

    private func stepButton(_ title: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).font(DSFont.sans(16, .semibold))
                .foregroundColor(DSColor.text)
                .frame(width: 28, height: 28)
                .background(DSColor.bgSubtle)
                .clipShape(Circle())
                .opacity(enabled ? 1 : 0.4)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

/// Поля редактора — для фокуса и локальных текстов (курсор не прыгает при нормализации в Kotlin).
enum NormsField: Hashable {
    case name, n1, n2, n3, n4
    case sections, weight, axles, axleLoad, volume
    case km, picket
}

extension View {
    /// Кнопка «Готово» над числовой клавиатурой.
    func normsKeyboardDone(_ focus: FocusState<NormsField?>.Binding) -> some View {
        toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Готово") { focus.wrappedValue = nil }
            }
        }
    }
}

extension View {
    /// Курсор при входе в поле ставится в КОНЕЦ значения (ввод дописывает), а не в точку касания.
    func normsCaretAtEnd() -> some View {
        onReceive(NotificationCenter.default.publisher(for: UITextField.textDidBeginEditingNotification)) { n in
            guard let tf = n.object as? UITextField else { return }
            let move = { tf.selectedTextRange = tf.textRange(from: tf.endOfDocument, to: tf.endOfDocument) }
            DispatchQueue.main.async(execute: move)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05, execute: move)
        }
    }
}
