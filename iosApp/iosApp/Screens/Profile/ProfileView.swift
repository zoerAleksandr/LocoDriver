import SwiftUI
import ComposeApp

struct ProfileView: View {
    @StateObject private var vm = HomeViewModelWrapper()
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var showPassword = false
    @State private var showDeleteAlert = false

    var body: some View {
        List {
            Section("Аккаунт") {
                HStack {
                    Text("Email")
                    Spacer()
                    TextField("email@example.com", text: $email)
                        .multilineTextAlignment(.trailing)
                        .foregroundColor(.secondary)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
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

            Section {
                // VK ID auth — на iOS VK ID SDK требует отдельной интеграции
                // Пока показываем статус из настроек
                if let settings = vm.settings {
                    HStack {
                        Image(systemName: "person.crop.circle.badge.checkmark")
                            .foregroundColor(.blue)
                        Text("ВКонтакте")
                        Spacer()
                        Text(settings.subscriptionPeriod > 0 ? "Подключено" : "Не подключено")
                            .foregroundColor(.secondary)
                            .font(.caption)
                    }
                }
                Button {
                    // TODO: VK ID OAuth на iOS требует vk-id-sdk-ios
                } label: {
                    HStack {
                        Image(systemName: "person.badge.plus")
                        Text("Войти через ВКонтакте")
                    }
                }
            }

            Section {
                NavigationLink("Подписка") {
                    PurchasesView()
                }
            }

            Section {
                Button(role: .destructive) {
                    showDeleteAlert = true
                } label: {
                    Text("Удалить аккаунт")
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .navigationTitle("Профиль")
        .alert("Удалить аккаунт?", isPresented: $showDeleteAlert) {
            Button("Удалить", role: .destructive) { /* TODO */ }
            Button("Отмена", role: .cancel) {}
        } message: {
            Text("Все данные будут удалены безвозвратно.")
        }
    }
}
