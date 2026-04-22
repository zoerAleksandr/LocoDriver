import SwiftUI
import UIKit

enum AppTab {
    case home, salary, add, settings, profile
}

struct AppCoordinator: View {
    @State private var selectedTab: AppTab = .home

    init() {
        // Единый стиль таб-бара: прозрачный фон, корректный учёт safe area
        // (home indicator), одинаковый stroke для всех иконок.
        let appearance = UITabBarAppearance()
        appearance.configureWithDefaultBackground()
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                HomeView()
            }
            .tabItem { Label("Поездки", systemImage: "doc.text") }
            .tag(AppTab.home)

            NavigationStack {
                SalaryCalculationView()
            }
            .tabItem { Label("Зарплата", systemImage: "rublesign") }
            .tag(AppTab.salary)

            NavigationStack {
                FormView(routeId: nil)
            }
            // Line-style "+" — тот же stroke, что и у doc.text / rublesign /
            // slider.horizontal.3 / person. Без кругового фона (не FAB).
            .tabItem { Label("Добавить", systemImage: "plus") }
            .tag(AppTab.add)

            NavigationStack {
                SettingsView()
            }
            .tabItem { Label("Настройки", systemImage: "slider.horizontal.3") }
            .tag(AppTab.settings)

            NavigationStack {
                ProfileView()
            }
            // person в том же line-стиле, что и slider.horizontal.3.
            .tabItem { Label("Профиль", systemImage: "person") }
            .tag(AppTab.profile)
        }
        .tint(Color.appAccent)
    }
}
