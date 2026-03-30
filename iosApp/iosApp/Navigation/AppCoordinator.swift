import SwiftUI

enum AppTab {
    case home, salary, add, settings, profile
}

struct AppCoordinator: View {
    @State private var selectedTab: AppTab = .home

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                HomeView()
            }
            .tabItem { Label("Поездки", systemImage: "list.bullet") }
            .tag(AppTab.home)

            NavigationStack {
                SalaryCalculationView()
            }
            .tabItem { Label("Зарплата", systemImage: "rublesign.circle") }
            .tag(AppTab.salary)

            NavigationStack {
                FormView(routeId: nil)
            }
            .tabItem { Label("Добавить", systemImage: "plus.circle.fill") }
            .tag(AppTab.add)

            NavigationStack {
                SettingsView()
            }
            .tabItem { Label("Настройки", systemImage: "gearshape") }
            .tag(AppTab.settings)

            NavigationStack {
                ProfileView()
            }
            .tabItem { Label("Профиль", systemImage: "person.circle") }
            .tag(AppTab.profile)
        }
    }
}
