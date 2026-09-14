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
                NavigationLink("Подписка") {
                    PurchasesView()
                }
                NavigationLink("Реферальная программа") {
                    ReferralView(vm: vm)
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
            Section("Условия") {
                Text("Пригласите друга. До своей первой оплаты он вводит ваш код на экране подписки. После подтверждения оплаты каждый из вас получит половину срока оплаченного тарифа дополнительно.")
                Text("Код можно ввести и после регистрации, если оплат ещё не было. Повторные покупки бонуса не дают.")
            }
            Section("Ваш код") {
                if let code = vm.referralCode {
                    Text(code).font(.title2).textSelection(.enabled)
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
                Text("Приглашений: \(vm.referralCount)")
                Text("Начислено бонусов: \(vm.referralRewardedCount)")
            }
        }
        .navigationTitle("Реферальная программа")
        .onAppear { vm.loadReferrals() }
    }
}
