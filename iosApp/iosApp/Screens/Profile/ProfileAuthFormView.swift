import SwiftUI
import ComposeApp

// SCREEN_SPECS §18.0: форма входа/регистрации — один экран на оба режима.
// Макет: design/src/login-screens.jsx → IOSAuth.
// Вся форма вертикально прокручивается; верх не «уезжает» из-за центрирования.

struct ProfileAuthFormView: View {
    @ObservedObject var vm: ProfileViewModelWrapper
    @Environment(\.openURL) private var openURL

    @State private var isRegisterMode = false
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var showPassword = false
    @State private var showConfirmPassword = false
    @State private var acceptedLicense = false
    @State private var acceptedPrivacy = false

    private let licenseURL = URL(string: "https://locodriver.ru/license.html")
    private let privacyURL = URL(string: "https://locodriver.ru/privacy.html")
    private let offerURL = URL(string: "https://locodriver.ru/offer.html")

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text("Добро пожаловать!")
                    .font(DSFont.sans(26, .bold))
                    .padding(.top, 18)
                    .padding(.bottom, 18)

                modeSwitcher
                    .padding(.bottom, 18)

                emailField
                    .padding(.bottom, 18)

                passwordField
                if isRegisterMode {
                    confirmPasswordField
                }

                forgotPasswordButton

                primaryButton
                    .padding(.top, 18)

                if let errorMessage = isRegisterMode
                    ? vm.state?.registrationErrorMessage
                    : vm.state?.authErrorMessage {
                    Text(errorMessage)
                        .font(DSFont.sans(13))
                        .foregroundColor(DSColor.danger)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 10)
                }

                if isRegisterMode {
                    consentCheckboxes
                        .padding(.top, 16)
                }

                orDivider
                    .padding(.top, 22)

                vkButton
                    .padding(.top, 18)

                vkLegalNote
                    .padding(.top, 12)
                    .padding(.bottom, 32)
            }
            .padding(.horizontal, DSMetrics.screenPadX)
        }
        .background(DSColor.bg.ignoresSafeArea())
    }

    // MARK: - Переключатель «Вход | Регистрация» (п. 2)

    private var modeSwitcher: some View {
        HStack(spacing: 4) {
            modeButton(title: "Вход", active: !isRegisterMode) { isRegisterMode = false }
            modeButton(title: "Регистрация", active: isRegisterMode) { isRegisterMode = true }
        }
        .padding(4)
        .background(DSColor.bgSubtle, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func modeButton(title: String, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(DSFont.sans(15, .semibold))
                .foregroundColor(active ? DSColor.text : DSColor.textMuted)
                .frame(maxWidth: .infinity, minHeight: 38)
                .background(
                    active ? DSColor.surface : Color.clear,
                    in: RoundedRectangle(cornerRadius: 9, style: .continuous)
                )
        }
        .buttonStyle(DSPressableStyle())
    }

    // MARK: - Поля (п. 3–5)

    private var emailField: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text("email").dsTextStyle(.labelMuted)
            HStack(spacing: 10) {
                Image(systemName: "envelope")
                    .foregroundColor(DSColor.textMuted)
                TextField("email", text: $email)
                    .font(DSFont.sans(16))
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
            .padding(.horizontal, 14)
            .frame(minHeight: 52)
            .background(DSColor.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(DSColor.border, lineWidth: 1.5)
            )
        }
    }

    private var passwordField: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text("пароль").dsTextStyle(.labelMuted)
            HStack(spacing: 10) {
                Image(systemName: "lock").foregroundColor(DSColor.textMuted)
                if showPassword {
                    TextField("пароль", text: $password)
                        .font(DSFont.sans(16))
                } else {
                    SecureField("пароль", text: $password)
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
            .background(DSColor.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(showPasswordError ? DSColor.danger : DSColor.border, lineWidth: 1.5)
            )
            if showPasswordError {
                Text("Минимум \(vm.minPasswordLength) символа")
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.danger)
            }
        }
    }

    private var confirmPasswordField: some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack(spacing: 10) {
                Image(systemName: "lock").foregroundColor(DSColor.textMuted)
                if showConfirmPassword {
                    TextField("подтвердите пароль", text: $confirmPassword)
                        .font(DSFont.sans(16))
                } else {
                    SecureField("подтвердите пароль", text: $confirmPassword)
                        .font(DSFont.sans(16))
                }
                Button { showConfirmPassword.toggle() } label: {
                    Image(systemName: showConfirmPassword ? "eye.slash" : "eye")
                        .foregroundColor(DSColor.textMuted)
                }
                .accessibilityLabel(showConfirmPassword ? "Скрыть пароль" : "Показать пароль")
            }
            .padding(.horizontal, 14)
            .frame(minHeight: 52)
            .background(DSColor.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(showMismatchError ? DSColor.danger : DSColor.border, lineWidth: 1.5)
            )
            if showMismatchError {
                Text("Пароли не совпадают")
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.danger)
            }
        }
        .padding(.top, 8)
    }

    private var showPasswordError: Bool {
        !password.isEmpty && !vm.isPasswordLongEnough(password)
    }

    private var showMismatchError: Bool {
        isRegisterMode && !confirmPassword.isEmpty && confirmPassword != password
    }

    // MARK: - Сброс пароля (п. 4)

    @ViewBuilder
    private var forgotPasswordButton: some View {
        let cooldown = Int(vm.state?.forgotCooldownSeconds ?? 0)
        HStack {
            Spacer()
            if cooldown > 0 {
                Text("Повторить через \(cooldown) с")
                    .font(DSFont.sans(14, .medium))
                    .foregroundColor(DSColor.textFaint)
            } else {
                Button("Создать новый пароль") {
                    vm.forgotRequest(email)
                }
                .font(DSFont.sans(14, .medium))
                .foregroundColor(DSColor.accent)
                .disabled(vm.state?.forgotLoading == true)
            }
        }
        .padding(.top, 10)
    }

    // MARK: - Кнопка (п. 6)

    private var primaryButton: some View {
        DSCTAButton(
            title: isRegisterMode ? "Зарегистрировать" : "Войти",
            isLoading: false,
            isEnabled: canSubmit
        ) {
            if isRegisterMode {
                vm.registerByEmail(email: email, password: password)
            } else {
                vm.authWithEmail(email: email, password: password)
            }
        }
    }

    private var canSubmit: Bool {
        guard vm.isEmailValid(email), vm.isPasswordLongEnough(password) else { return false }
        if isRegisterMode {
            return !confirmPassword.isEmpty
                && confirmPassword == password
                && acceptedLicense
                && acceptedPrivacy
        }
        return true
    }

    // MARK: - Чекбоксы согласий (п. 7)

    private var consentCheckboxes: some View {
        VStack(alignment: .leading, spacing: 12) {
            consentRow(
                isOn: $acceptedLicense,
                prefix: "Я принимаю условия ",
                linkTitle: "Лицензионного соглашения",
                url: licenseURL
            )
            consentRow(
                isOn: $acceptedPrivacy,
                prefix: "Я ознакомлен с ",
                linkTitle: "политикой обработки персональных данных",
                url: privacyURL
            )
        }
    }

    /// Тап по чекбоксу переключает его; тап по тексту открывает документ
    /// во внешнем браузере и чекбокс не переключает (§18.0, п. 7).
    private func consentRow(isOn: Binding<Bool>, prefix: String, linkTitle: String, url: URL?) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Button {
                isOn.wrappedValue.toggle()
            } label: {
                Image(systemName: isOn.wrappedValue ? "checkmark.square.fill" : "square")
                    .font(.system(size: 20))
                    .foregroundColor(isOn.wrappedValue ? DSColor.accent : DSColor.textFaint)
            }
            .accessibilityLabel(linkTitle)
            (Text(prefix).foregroundColor(DSColor.text)
                + Text(linkTitle)
                    .foregroundColor(DSColor.accent)
                    .underline()
            )
            .font(DSFont.sans(14))
            .fixedSize(horizontal: false, vertical: true)
            .onTapGesture {
                if let url = url { openURL(url) }
            }
        }
    }

    // MARK: - VK (п. 8–10)

    private var orDivider: some View {
        HStack(spacing: 12) {
            DSDivider(inset: 0)
            Text("или").dsTextStyle(.captionMuted)
            DSDivider(inset: 0)
        }
    }

    private var vkButton: some View {
        Button {
            // One-tap VK ID (§18.2): SDK → access token → authWithVKID;
            // «аккаунта нет» — диалог регистрации, отмена/ошибка SDK — snackbar.
            vm.signInWithVKID()
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "person.crop.circle.badge.checkmark")
                Text("Войти через VK ID")
            }
            .font(DSFont.sans(15, .semibold))
            .foregroundColor(.white)
            .frame(maxWidth: .infinity, minHeight: 52)
            .background(Color(red: 0, green: 0.47, blue: 1))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(DSPressableStyle())
    }

    /// Подпись под кнопкой VK (п. 10): «Лицензионное соглашение» и «политику …
    /// данных» кликабельны (markdown-ссылки).
    private var vkLegalNote: some View {
        Text(.init(
            "Если аккаунта ещё нет, он будет создан автоматически. Продолжая, вы принимаете "
            + "[Лицензионное соглашение](https://locodriver.ru/license.html) и "
            + "[политику обработки персональных данных](https://locodriver.ru/privacy.html)."
        ))
        .font(DSFont.sans(13))
        .tint(DSColor.accent)
        .foregroundColor(DSColor.textMuted)
        .fixedSize(horizontal: false, vertical: true)
    }
}
