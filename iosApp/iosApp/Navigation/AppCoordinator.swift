import SwiftUI
import UIKit

enum AppTab {
    case home, salary, add, settings, profile
}

/// Идентифицируемая обёртка над routeId для `.sheet(item:)`. SwiftUI
/// требует Identifiable; сам routeId уникален (UUID), используем как `id`.
private struct DeepLinkRoute: Identifiable {
    let id: String
}

struct AppCoordinator: View {
    @State private var selectedTab: AppTab = .home
    @State private var previousTab: AppTab = .home
    @State private var showAddForm: Bool = false
    /// Маршрут, открываемый по deep-link. Установка не-nil показывает sheet
    /// с уже импортированной формой (см. [PendingFormRouteObserver]).
    @State private var deepLinkRoute: DeepLinkRoute? = nil
    /// Подписка на ошибки SharedRouteLinkHandler (deep-link).
    /// AppCoordinator — root, .alert поверх любой активной вкладки.
    @StateObject private var deepLinkError = DeepLinkErrorObserver()
    /// Подписка на `pendingFormRouteId` SharedRouteLinkHandler. При
    /// успешном импорте deep-link открывает форму свежего маршрута sheet'ом.
    @StateObject private var pendingForm = PendingFormRouteObserver()

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
        // Deep-link на существующий импортированный маршрут — открывается
        // sheet'ом аналогично «Добавить», но с заранее заданным routeId.
        .sheet(item: $deepLinkRoute) { route in
            NavigationStack {
                FormView(routeId: route.id)
            }
        }
        .onChange(of: pendingForm.pendingRouteId) { newId in
            guard let id = newId, !id.isEmpty else { return }
            // Закрываем «Добавить»-шторку, если была открыта, чтобы не было
            // конкуренции за модальное окно. Затем сразу очищаем
            // pendingFormRouteId — повторный приход того же id не должен
            // переоткрывать sheet.
            showAddForm = false
            deepLinkRoute = DeepLinkRoute(id: id)
            pendingForm.clear()
        }
        // Deep-link error — без кнопки «Повторить» (одноразовая ссылка,
        // повтор не имеет смысла; пользователь должен снова кликнуть
        // ссылку извне). Объединённый текст: AppError.userMessage или
        // legacy errorMessage из handler'а.
        .alert(
            deepLinkError.error?.userMessage ?? deepLinkError.errorMessage ?? "Не удалось открыть ссылку",
            isPresented: Binding(
                get: { deepLinkError.error != nil || deepLinkError.errorMessage != nil },
                set: { if !$0 { deepLinkError.clearError() } }
            )
        ) {
            Button("OK", role: .cancel) {}
        }
    }
}
