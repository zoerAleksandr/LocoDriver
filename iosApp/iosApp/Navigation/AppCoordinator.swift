import SwiftUI
import UIKit

enum AppTab {
    case home, salary, add, settings, profile
}

struct AppCoordinator: View {
    @State private var selectedTab: AppTab = .home
    @State private var previousTab: AppTab = .home
    @State private var showAddForm: Bool = false

    init() {
        // Единый стиль таб-бара: прозрачный фон, корректный учёт safe area
        // (home indicator), одинаковый stroke для всех иконок.
        let appearance = UITabBarAppearance()
        appearance.configureWithDefaultBackground()
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some View {
        // Перехватываем тап по вкладке «Добавить»: не делаем её активной, а
        // открываем форму нового маршрута модальным sheet'ом поверх предыдущей
        // вкладки. Это даёт работающий dismiss() после сохранения и
        // естественную кнопку «Отмена» для возврата без сохранения.
        let tabBinding = Binding<AppTab>(
            get: { selectedTab },
            set: { newValue in
                if newValue == .add {
                    showAddForm = true
                } else {
                    previousTab = newValue
                    selectedTab = newValue
                }
            }
        )

        TabView(selection: tabBinding) {
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

            // Dummy-вкладка: никогда не активируется, при тапе срабатывает
            // перехватчик в tabBinding.set и открывается sheet.
            Color.clear
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
        .sheet(isPresented: $showAddForm) {
            NavigationStack {
                FormView(routeId: nil)
            }
        }
    }
}
