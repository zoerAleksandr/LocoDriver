import SwiftUI

struct ProfileView: View {
    var body: some View {
        List {
            Section("Аккаунт") {
                NavigationLink("Подписка") {
                    PurchasesView()
                }
            }

            Section {
                Button(role: .destructive) {
                    // TODO: logout
                } label: {
                    Text("Выйти из аккаунта")
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .navigationTitle("Профиль")
    }
}
