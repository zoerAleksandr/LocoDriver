import SwiftUI
import UIKit
import ComposeApp

// SCREEN_SPECS §18.6: шторки ввода `AppInputBottomSheet` / `AppEmailPasswordBottomSheet`.
// Стандарт docs/DIALOGS_STANDARD.md: всё, что требует ввода, — нижняя шторка.
// Контейнер — DSBottomSheet (grabber, шапка с крестиком, фон surface).

/// `AppInputBottomSheet`: одно поле ввода с заголовком, подсказкой и кнопкой.
/// Начальное значение выделено целиком, поле сразу получает фокус (§18.6).
struct ProfileInputSheet: View {
    let title: String
    var hint: String? = nil
    let initialValue: String
    let confirmTitle: String
    let isLoading: Bool
    let errorText: String?
    let isValid: (String) -> Bool
    var keyboard: UIKeyboardType = .default
    var onValueChange: ((String) -> Void)? = nil
    let onConfirm: (String) -> Void
    let onClose: () -> Void

    @State private var text: String = ""
    @FocusState private var focused: Bool

    private var canConfirm: Bool {
        isValid(text) && !isLoading
    }

    var body: some View {
        DSBottomSheet(header: .titleClose(title: title, subtitle: nil), onClose: onClose) {
            VStack(alignment: .leading, spacing: 12) {
                if let hint = hint {
                    Text(hint).dsTextStyle(.hint)
                }
                SelectAllTextField(
                    text: $text,
                    placeholder: "",
                    keyboard: keyboard,
                    isFocused: $focused
                )
                .frame(minHeight: 52)
                .background(DSColor.surface, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(
                            errorText != nil ? DSColor.danger : (focused ? DSColor.accent : DSColor.border),
                            lineWidth: 1.5
                        )
                )
                if let errorText = errorText {
                    Text(errorText)
                        .font(DSFont.sans(12))
                        .foregroundColor(DSColor.danger)
                }
                DSCTAButton(
                    title: confirmTitle,
                    isLoading: isLoading,
                    isEnabled: canConfirm
                ) {
                    if canConfirm { onConfirm(text) }
                }
                Button("Отмена", action: onClose)
                    .font(DSFont.sans(15))
                    .foregroundColor(DSColor.textMuted)
                    .frame(maxWidth: .infinity, minHeight: 44)
            }
            .padding(.top, 4)
        }
        .onAppear {
            text = initialValue
            focused = true
        }
        .onChange(of: text) { value in
            onValueChange?(value)
        }
    }
}

/// `AppEmailPasswordBottomSheet`: почта + пароль для аккаунта, заведённого через VK.
/// Пробелы по краям почты обрезаются при вводе; под паролем — «Не короче N символов».
struct ProfileEmailPasswordSheet: View {
    let title: String
    var hint: String? = nil
    let confirmTitle: String
    let minPasswordLength: Int
    let isLoading: Bool
    let errorText: String?
    let isEmailValid: (String) -> Bool
    var onValueChange: ((String, String) -> Void)? = nil
    let onConfirm: (String, String) -> Void
    let onClose: () -> Void

    @State private var email: String = ""
    @State private var password: String = ""
    @State private var showPassword = false
    @FocusState private var emailFocused: Bool

    private var canConfirm: Bool {
        isEmailValid(email) && password.count >= minPasswordLength && !isLoading
    }

    var body: some View {
        DSBottomSheet(header: .titleClose(title: title, subtitle: nil), onClose: onClose) {
            VStack(alignment: .leading, spacing: 12) {
                if let hint = hint {
                    Text(hint).dsTextStyle(.hint)
                }
                SelectAllTextField(
                    text: $email,
                    placeholder: "Почта",
                    keyboard: .emailAddress,
                    isFocused: $emailFocused
                )
                .frame(minHeight: 52)
                .background(DSColor.surface, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(errorText != nil ? DSColor.danger : DSColor.border, lineWidth: 1.5)
                )
                HStack(spacing: 10) {
                    if showPassword {
                        TextField("Пароль", text: $password)
                            .font(DSFont.sans(16))
                    } else {
                        SecureField("Пароль", text: $password)
                            .font(DSFont.sans(16))
                    }
                    Button { showPassword.toggle() } label: {
                        Image(systemName: showPassword ? "eye.slash" : "eye")
                            .foregroundColor(DSColor.textMuted)
                    }
                    .accessibilityLabel(showPassword ? "Скрыть пароль" : "Показать пароль")
                }
                .padding(.horizontal, 14)
                .frame(minHeight: 52)
                .background(DSColor.surface, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(errorText != nil ? DSColor.danger : DSColor.border, lineWidth: 1.5)
                )
                if let errorText = errorText {
                    Text(errorText)
                        .font(DSFont.sans(12))
                        .foregroundColor(DSColor.danger)
                } else {
                    Text("Не короче \(minPasswordLength) символов")
                        .font(DSFont.sans(12))
                        .foregroundColor(DSColor.textMuted)
                }
                DSCTAButton(
                    title: confirmTitle,
                    isLoading: isLoading,
                    isEnabled: canConfirm
                ) {
                    if canConfirm { onConfirm(email.trimmingCharacters(in: .whitespaces), password) }
                }
                Button("Отмена", action: onClose)
                    .font(DSFont.sans(15))
                    .foregroundColor(DSColor.textMuted)
                    .frame(maxWidth: .infinity, minHeight: 44)
            }
            .padding(.top, 4)
        }
        .onAppear {
            email = ""
            password = ""
            emailFocused = true
        }
        .onChange(of: email) { value in
            // Пробелы по краям обрезаются при вводе (§18.6).
            let trimmed = value.trimmingCharacters(in: .whitespaces)
            if trimmed != value { email = trimmed }
            onValueChange?(email, password)
        }
        .onChange(of: password) { value in
            onValueChange?(email, value)
        }
    }
}

/// UITextField-обёртка: фокус + выделение начального значения целиком (§18.6).
private struct SelectAllTextField: UIViewRepresentable {
    @Binding var text: String
    let placeholder: String
    let keyboard: UIKeyboardType
    var isFocused: FocusState<Bool>.Binding

    func makeUIView(context: Context) -> UITextField {
        let field = UITextField(frame: .zero)
        field.keyboardType = keyboard
        field.autocapitalizationType = .none
        field.autocorrectionType = .no
        field.font = UIFont.systemFont(ofSize: 16)
        field.placeholder = placeholder
        field.delegate = context.coordinator
        field.addTarget(context.coordinator, action: #selector(Coordinator.editingChanged(_:)), for: .editingChanged)
        return field
    }

    func updateUIView(_ field: UITextField, context: Context) {
        if field.text != text {
            field.text = text
        }
        if isFocused.wrappedValue, !field.isFirstResponder {
            DispatchQueue.main.async {
                field.becomeFirstResponder()
                // Начальное значение выделено целиком (§18.6).
                field.selectedTextRange = field.textRange(from: field.beginningOfDocument, to: field.endOfDocument)
            }
        }
        if !isFocused.wrappedValue, field.isFirstResponder {
            field.resignFirstResponder()
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UITextFieldDelegate {
        private let parent: SelectAllTextField
        init(_ parent: SelectAllTextField) { self.parent = parent }

        @objc func editingChanged(_ field: UITextField) {
            parent.text = field.text ?? ""
        }

        func textFieldDidBeginEditing(_ field: UITextField) {
            parent.isFocused.wrappedValue = true
            field.selectedTextRange = field.textRange(from: field.beginningOfDocument, to: field.endOfDocument)
        }

        func textFieldDidEndEditing(_ field: UITextField) {
            parent.isFocused.wrappedValue = false
        }

        func textFieldShouldReturn(_ field: UITextField) -> Bool {
            field.resignFirstResponder()
            return true
        }
    }
}
