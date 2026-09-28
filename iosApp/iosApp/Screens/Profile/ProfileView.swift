import SwiftUI
import ComposeApp

struct ProfileView: View {
    @StateObject private var vm = ProfileViewModelWrapper()

    @State private var email: String = ""
    @State private var password: String = ""
    @State private var showPassword: Bool = false
    @State private var showVKAlert: Bool = false

    var body: some View {
        Group {
            if vm.isLoggedIn {
                loggedInView
            } else {
                loginFormView
            }
        }
        .navigationTitle("Профиль")
        .alert("ВКонтакте", isPresented: $showVKAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Вход через ВКонтакте скоро будет доступен")
        }
    }

    // MARK: - Login Form

    private var loginFormView: some View {
        List {
            Section("Вход в аккаунт") {
                HStack {
                    Text("Email")
                    Spacer()
                    TextField("email@example.com", text: $email)
                        .multilineTextAlignment(.trailing)
                        .foregroundColor(.secondary)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                }

                HStack {
                    Text("Пароль")
                    Spacer()
                    if showPassword {
                        TextField("Пароль", text: $password)
                            .multilineTextAlignment(.trailing)
                            .foregroundColor(.secondary)
                    } else {
                        SecureField("Пароль", text: $password)
                            .multilineTextAlignment(.trailing)
                    }
                    Button {
                        showPassword.toggle()
                    } label: {
                        Image(systemName: showPassword ? "eye.slash" : "eye")
                            .foregroundColor(.secondary)
                    }
                }
            }

            if let errorMessage = vm.errorMessage {
                Section {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                        Text(errorMessage)
                            .foregroundColor(.red)
                            .font(.footnote)
                    }
                }
            }

            Section {
                Button {
                    vm.clearError()
                    vm.login(email: email, password: password)
                } label: {
                    HStack {
                        Spacer()
                        if vm.isLoading {
                            ProgressView()
                                .padding(.trailing, 8)
                        }
                        Text("Войти")
                            .bold()
                        Spacer()
                    }
                }
                .disabled(vm.isLoading || email.isEmpty || password.isEmpty)

                Button {
                    showVKAlert = true
                } label: {
                    HStack {
                        Spacer()
                        Image(systemName: "person.badge.plus")
                        Text("Войти через ВКонтакте")
                        Spacer()
                    }
                }
                .foregroundColor(.blue)
            }
        }
    }

    // MARK: - Logged In View

    private var loggedInView: some View {
        List {
            Section("Аккаунт") {
                HStack {
                    Image(systemName: "person.crop.circle.fill")
                        .foregroundColor(.blue)
                        .font(.title2)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Email")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(vm.userEmail ?? "—")
                            .font(.body)
                    }
                }
            }

            Section("Синхронизация") {
                if let syncMessage = vm.syncMessage {
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.green)
                        Text(syncMessage)
                            .font(.footnote)
                            .foregroundColor(.secondary)
                    }
                }

                if let errorMessage = vm.errorMessage {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                        Text(errorMessage)
                            .font(.footnote)
                            .foregroundColor(.red)
                    }
                }

                Button {
                    vm.clearError()
                    vm.syncData()
                } label: {
                    HStack {
                        if vm.isSyncing {
                            ProgressView()
                                .padding(.trailing, 8)
                            Text("Синхронизация...")
                        } else {
                            Image(systemName: "arrow.triangle.2.circlepath")
                            Text("Синхронизировать данные")
                        }
                    }
                }
                .disabled(vm.isSyncing)
            }

            Section {
                NavigationLink {
                    PurchasesView()
                } label: {
                    Label("Машинист Про", systemImage: "star.fill")
                }
                NavigationLink {
                    ReferralView(vm: vm)
                } label: {
                    Label("Реферальная программа", systemImage: "gift.fill")
                }
            }

            Section {
                Button(role: .destructive) {
                    vm.logout()
                } label: {
                    HStack {
                        Spacer()
                        Text("Выйти из аккаунта")
                        Spacer()
                    }
                }
            }
        }
    }
}

private struct ReferralView: View {
    @ObservedObject var vm: ProfileViewModelWrapper

    var body: some View {
        List {
            Section {
                VStack(spacing: 12) {
                    Image(systemName: "gift.fill")
                        .font(.system(size: 34, weight: .semibold))
                        .foregroundStyle(.blue)
                        .frame(width: 64, height: 64)
                        .background(Color.blue.opacity(0.12), in: RoundedRectangle(cornerRadius: 20))
                    Text("Про для вас и друга")
                        .font(.title2.bold())
                    Text("После первой оплаты друга каждый получит половину оплаченного периода дополнительно")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
            }
            Section("Ваш код") {
                if vm.referralCode != nil && !vm.canInvite {
                    Label("Реферальная программа доступна для приглашающих, которые уже хотя бы раз оплачивали Про. После вашей первой оплаты здесь появится код.", systemImage: "exclamationmark.triangle.fill")
                        .foregroundStyle(Color(red: 0.35, green: 0.24, blue: 0))
                        .listRowBackground(Color(red: 1, green: 0.94, blue: 0.74))
                } else if let code = vm.referralCode {
                    Text(code)
                        .font(.system(.title, design: .monospaced, weight: .bold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .textSelection(.enabled)
                    ShareLink(item: "Мой код Машинист Про: \(code)") {
                        Label("Поделиться", systemImage: "square.and.arrow.up")
                    }
                } else if let error = vm.referralError {
                    Text(error).foregroundColor(.red)
                    Button("Повторить") { vm.loadReferrals() }
                } else {
                    ProgressView()
                }
            }
            Section("Приглашения") {
                LabeledContent("Приглашено") {
                    Text("\(vm.referralCount)").fontWeight(.semibold)
                }
                LabeledContent("Бонус начислен") {
                    Text("\(vm.referralRewardedCount)").fontWeight(.semibold).foregroundStyle(.green)
                }
            }
            Section {
                ReferralRuleRow(number: 1, text: "Поделитесь своим кодом с другом")
                ReferralRuleRow(number: 2, text: "Друг вводит код перед первой оплатой Про")
                ReferralRuleRow(number: 3, text: "Половина периода добавится каждому автоматически")
                if let rulesURL = URL(string: "https://locodriver.ru/referral.html") {
                    Link("Подробные правила программы", destination: rulesURL)
                }
            } header: {
                Text("Как это работает")
            } footer: {
                Text("Приглашать может только тот, кто уже хотя бы раз оплачивал Про. Друг может применить код после регистрации, до своей первой оплаты. Повторные покупки бонуса не дают.")
            }
        }
        .navigationTitle("Реферальная программа")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { vm.loadReferrals() }
    }
}

private struct ReferralRuleRow: View {
    let number: Int
    let text: String

    var body: some View {
        HStack(spacing: 12) {
            Text("\(number)")
                .font(.caption.bold())
                .foregroundStyle(.white)
                .frame(width: 28, height: 28)
                .background(Color.blue, in: RoundedRectangle(cornerRadius: 9))
            Text(text).font(.subheadline)
        }
        .padding(.vertical, 2)
    }
}
