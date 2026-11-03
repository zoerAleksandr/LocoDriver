import SwiftUI
import ComposeApp

// Вспомогательные элементы экрана «Локомотив» (SCREEN_SPECS §1).

/// Числовое/текстовое поле экрана. Хранит локальный текст, чтобы набор не «прыгал» от
/// обратной записи из Kotlin: внешнее значение подхватывается, только когда поле не в фокусе.
struct LocoField: View {
    let placeholder: String
    let external: String
    var suffix: String? = nil
    var keyboard: UIKeyboardType = .decimalPad
    var isMono: Bool = true
    var alignment: TextAlignment = .leading
    let onChange: (String) -> Void

    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        HStack(spacing: 4) {
            TextField(placeholder, text: $text)
                .keyboardType(keyboard)
                .multilineTextAlignment(alignment)
                .focused($focused)
                .font(isMono ? DSFont.mono(17, .medium) : DSFont.sans(17, .medium))
                .foregroundColor(DSColor.text)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
            if let suffix = suffix {
                Text(suffix).font(DSFont.sans(14)).foregroundColor(DSColor.textFaint)
            }
        }
        .padding(.horizontal, DSMetrics.inputPadX)
        .frame(minHeight: DSMetrics.inputMinH)
        .background(DSColor.bgSubtle)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
        .onAppear { text = external }
        // Не гейтим по `focused`: при первом тапе вставка/ввод может прийти раньше, чем обновится
        // фокус, и значение терялось (в модель не уходило).
        .onChange(of: text) { value in
            if value != external { onChange(value) }
        }
        .onChange(of: external) { value in
            if !focused { text = value }
        }
        .onChange(of: focused) { isFocused in
            if !isFocused && text != external { onChange(text) }
        }
    }
}

/// Подпись над полем («Принял» / «Сдал»).
struct LocoFieldLabel: View {
    let text: String
    var body: some View {
        Text(text).font(DSFont.sans(12)).foregroundColor(DSColor.textMuted)
    }
}

/// Свайп карточки влево открывает красную кнопку удаления (`SwipeToRevealDelete`, §1.3 п.4).
struct LocoSwipeDelete<Content: View>: View {
    let onDelete: () -> Void
    @ViewBuilder let content: () -> Content

    @State private var offset: CGFloat = 0
    private let revealWidth: CGFloat = 84

    var body: some View {
        ZStack(alignment: .trailing) {
            Button(action: {
                withAnimation(.easeOut(duration: 0.2)) { offset = 0 }
                onDelete()
            }) {
                DSIcon.trash.image
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: revealWidth)
                    .frame(maxHeight: .infinity)
                    .background(DSColor.danger)
                    .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
            }
            .buttonStyle(.plain)
            .opacity(offset < 0 ? 1 : 0)
            .accessibilityLabel(Text("Удалить секцию"))

            content()
                .offset(x: offset)
                .simultaneousGesture(
                    DragGesture(minimumDistance: 20, coordinateSpace: .local)
                        .onChanged { value in
                            guard abs(value.translation.width) > abs(value.translation.height) else { return }
                            offset = max(-revealWidth, min(0, value.translation.width + (offset == -revealWidth ? -revealWidth : 0)))
                        }
                        .onEnded { value in
                            withAnimation(.easeOut(duration: 0.2)) {
                                offset = value.translation.width < -revealWidth / 2 ? -revealWidth : 0
                            }
                        }
                )
        }
    }
}

/// Плашка «↓ ПЕРЕРАСХОД −X (−P%)» / «↑ ЭКОНОМИЯ +X (+P%)» (§1.3 п.6).
struct LocoResultCard: View {
    let result: LocoResultUi
    var unit: String = ""

    var body: some View {
        let color = result.isOver ? DSColor.danger : DSColor.success
        let sign = result.isOver ? "−" : "+"
        HStack(spacing: 12) {
            Text(result.isOver ? "↓" : "↑")
                .font(DSFont.sans(18, .bold))
                .foregroundColor(color)
                .frame(width: 34, height: 34)
                .background(color.opacity(0.20))
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(result.isOver ? "ПЕРЕРАСХОД" : "ЭКОНОМИЯ")
                    .dsTextStyle(.group, color: color)
                HStack(alignment: .firstTextBaseline, spacing: 0) {
                    Text("\(sign)\(result.magnitude)")
                        .font(DSFont.mono(20, .bold))
                        .foregroundColor(DSColor.text)
                    Text((unit.isEmpty ? " " : " \(unit)  ") + "(\(sign)\(result.percent)%)")
                        .font(DSFont.mono(13))
                        .foregroundColor(DSColor.textMuted)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(color.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
    }
}

/// Пилюля «k секции 0.83» (тап открывает шторку коэффициента).
struct LocoCoeffPill: View {
    let title: String
    let value: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(title).font(DSFont.sans(12)).foregroundColor(DSColor.textMuted)
                Text(value).font(DSFont.mono(13, .semibold)).foregroundColor(DSColor.text)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(DSColor.bgSubtle)
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

/// Пунктирный разделитель перед итогами секции.
struct LocoDashedDivider: View {
    var body: some View {
        Rectangle()
            .stroke(DSColor.borderStrong, style: StrokeStyle(lineWidth: 1, dash: [3, 3]))
            .frame(height: 1)
            .padding(.vertical, 6)
    }
}
