import SwiftUI

/// Корень навигации (SCREEN_SPECS §3.1): 4 корневые вкладки, у каждой свой
/// `NavigationStack` со стеком из `AppRouter.paths`, плавающий `DSTabBar` с
/// отдельной кнопкой «+» (новый маршрут, §3.4).
///
/// Почему не системный `TabView`: нижнее меню показывается ТОЛЬКО на корневых
/// экранах. Системный таб-бар на iOS 16 нельзя надёжно скрыть на экранах,
/// открытых через `NavigationLink(destination:)` из существующих View.
/// Поэтому вкладки — это стопка `NavigationStack` (неактивные скрыты, но
/// сохраняют состояние — аналог `saveState/restoreState`), а `DSTabBar`
/// прикреплён к корневому экрану каждой вкладки через `safeAreaInset` и
/// автоматически пропадает на вложенных экранах.
struct AppCoordinator: View {
    @ObservedObject private var router = AppRouter.shared
    /// Вкладка создаётся при первом открытии (не грузим VM всех вкладок на старте).
    @State private var loadedTabs: Set<AppTab> = []

    var body: some View {
        ZStack {
            ForEach(AppTab.allCases, id: \.self) { tab in
                if loadedTabs.contains(tab) || router.selectedTab == tab {
                    let isSelected = router.selectedTab == tab
                    tabStack(tab)
                        .opacity(isSelected ? 1 : 0)
                        .allowsHitTesting(isSelected)
                        .accessibilityHidden(!isSelected)
                }
            }
        }
        .onAppear { loadedTabs.insert(router.selectedTab) }
        .onChange(of: router.selectedTab) { tab in loadedTabs.insert(tab) }
        .sheet(item: $router.sheet) { sheet in
            AppSheetView(sheet: sheet, onClose: { router.dismissSheet() })
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.hidden)
        }
        .fullScreenCover(item: $router.fullScreen) { sheet in
            AppSheetView(sheet: sheet, onClose: { router.dismissFullScreen() })
        }
        .alert("Нужен вход в аккаунт", isPresented: $router.isSignInRequiredAlertShown) {
            Button("Войти") { router.showProfile() }
            Button("Отмена", role: .cancel) {}
        }
        // Гейт «+» (§3.4). Отдельный фоновый носитель — чтобы не делить view с алертом выше.
        .background(newRouteGateAlertHost)
    }

    private var newRouteGateAlertHost: some View {
        Color.clear
            .alert(
                router.newRouteGate?.title ?? "",
                isPresented: Binding(
                    get: { router.newRouteGate != nil },
                    set: { if !$0 { router.newRouteGate = nil } }
                ),
                presenting: router.newRouteGate
            ) { gate in
                switch gate {
                case .limitReached:
                    Button("Оформить подписку") {
                        router.showPurchasesFromNewRouteGate(isAuthorized: gate.isAuthorized)
                    }
                    Button("Отмена", role: .cancel) {}
                case .trial:
                    Button("Продолжить бесплатно") { router.openNewRouteForm() }
                    Button("Оформить подписку") {
                        router.showPurchasesFromNewRouteGate(isAuthorized: gate.isAuthorized)
                    }
                }
            } message: { gate in
                Text(gate.message)
            }
    }

    private func tabStack(_ tab: AppTab) -> some View {
        NavigationStack(path: router.pathBinding(for: tab)) {
            rootView(for: tab)
                .safeAreaInset(edge: .bottom, spacing: 0) {
                    DSTabBar(
                        selection: Binding(
                            get: { router.selectedTab },
                            set: { router.selectTab($0) }
                        ),
                        onAdd: { router.startNewRoute() }
                    )
                    .padding(.bottom, 4)
                }
                .navigationDestination(for: AppRoute.self) { route in
                    AppDestinationView(route: route)
                }
        }
    }

    @ViewBuilder
    private func rootView(for tab: AppTab) -> some View {
        switch tab {
        case .home: HomeView()
        case .salary: SalaryCalculationView()
        case .settings: SettingsView()
        case .profile: ProfileView()
        }
    }
}
