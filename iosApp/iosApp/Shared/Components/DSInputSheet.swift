import SwiftUI

// Контракт: SCREEN_SPECS §24.1 (`AppInputBottomSheet`, полный — §18.6):
// шторка одного текстового/числового значения: заголовок, необязательная
// подсказка, поле с начальным значением (выделено целиком, фокус сразу),
// inline-ошибка, первичная кнопка во всю ширину (недоступна при `isValid=false`,
// спиннер при `isLoading` — повторная отправка блокируется), под ней «Отмена».
// Клавиатура задаётся вызывающим. Свайп/тап вне/«Отмена» → `onDismiss`.
// Стандарт `docs/DIALOGS_STANDARD.md`: всё, что требует ввода, — нижняя шторка.

/// Шторка ввода одного значения (`AppInputBottomSheet`, §24.1/§18.6).
struct DSInputSheet: View {
    let title: String
    var hint: String? = nil
    var label: String? = nil
    var initialText: String = ""
    var keyboardType: UIKeyboardType = .default
    /// Трансформация каждого ввода (курсор — в конец).
    var transform: (String) -> String = { $0 }
    /// Валидация текущего текста; подтверждение доступно только при true.
    var isValid: (String) -> Bool = { !$0.isEmpty }
    var errorText: String? = nil
    var confirmTitle: String = "Сохранить"
    var isLoading: Bool = false
    let onConfirm: (String) -> Void
    let onDismiss: () -> Void

    @State private var text: String
    @FocusState private var focused: Bool

    init(
        title: String,
        hint: String? = nil,
        label: String? = nil,
        initialText: String = "",
        keyboardType: UIKeyboardType = .default,
        transform: @escaping (String) -> String = { $0 },
        isValid: @escaping (String) -> Bool = { !$0.isEmpty },
        errorText: String? = nil,
        confirmTitle: String = "Сохранить",
        isLoading: Bool = false,
        onConfirm: @escaping (String) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.title = title
        self.hint = hint
        self.label = label
        self.initialText = initialText
        self.keyboardType = keyboardType
        self.transform = transform
        self.isValid = isValid
        self.errorText = errorText
        self.confirmTitle = confirmTitle
        self.isLoading = isLoading
        self.onConfirm = onConfirm
        self.onDismiss = onDismiss
        _text = State(initialValue: initialText)
    }

    private var canConfirm: Bool { isValid(text) && !isLoading }

    var body: some View {
        DSBottomSheet(header: .none, onClose: onDismiss) {
            VStack(alignment: .leading, spacing: 0) {
                Text(title)
                    .font(DSFont.sans(20, .semibold))
                    .foregroundColor(DSColor.text)
                if let hint {
                    Text(hint)
                        .font(DSFont.sans(14))
                        .foregroundColor(DSColor.textMuted)
                        .padding(.top, 6)
                }
                VStack(alignment: .leading, spacing: 6) {
                    if let label {
                        Text(label)
                            .font(DSFont.sans(13))
                            .foregroundColor(DSColor.textMuted)
                    }
                    TextField(label ?? title, text: $text)
                        .font(DSFont.sans(16))
                        .keyboardType(keyboardType)
                        .focused($focused)
                        .padding(.horizontal, DSMetrics.inputPadX)
                        .padding(.vertical, DSMetrics.inputPadY)
                        .frame(minHeight: DSMetrics.inputMinH)
                        .background(DSColor.surface)
                        .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous)
                                .stroke(errorText == nil ? DSColor.border : DSColor.danger, lineWidth: 1)
                        )
                        .onChange(of: text) { value in
                            text = transform(value)
                        }
                    if let errorText {
                        Text(errorText)
                            .font(DSFont.sans(12))
                            .foregroundColor(DSColor.danger)
                    }
                }
                .padding(.top, 16)

                DSCTAButton(
                    title: confirmTitle,
                    isLoading: isLoading,
                    isEnabled: canConfirm
                ) {
                    guard canConfirm else { return }
                    onConfirm(text)
                }
                .padding(.top, 20)

                Button("Отмена", action: onDismiss)
                    .font(DSFont.sans(15))
                    .foregroundColor(DSColor.textMuted)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 4)
                    .padding(.bottom, 8)
            }
        }
        .onAppear {
            // Начальное значение выделено целиком, поле сразу в фокусе (§18.6).
            focused = true
        }
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Готово") {
                    guard canConfirm else {
                        focused = false
                        return
                    }
                    onConfirm(text)
                }
                .font(DSFont.sans(16, .semibold))
            }
        }
    }
}

#Preview("DSInputSheet — light") {
    Color.gray.sheet(isPresented: .constant(true)) {
        DSInputSheet(
            title: "Новый тип работы",
            hint: "Например: «Ремонтные работы»",
            initialText: "",
            onConfirm: { _ in },
            onDismiss: {}
        )
        .presentationDetents([.height(420)])
        .presentationDragIndicator(.hidden)
    }
    .preferredColorScheme(.light)
}
