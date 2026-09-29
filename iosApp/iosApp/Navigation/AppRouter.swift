import SwiftUI

/// Общий роутер приложения — iOS-аналог `domain.navigation.Router` (SCREEN_SPECS §3.2).
/// Экраны не знают про `NavigationStack`: они вызывают методы роутера.
///
/// Состояние: выбранная вкладка, стек экранов каждой вкладки (сохраняется при
/// переключении — аналог `saveState/restoreState`), текущая шторка и полноэкранный оверлей.
/// ОБЩАЯ ЗОНА: волны только добавляют методы (см. docs/IOS_UI_GUIDE.md).
final class AppRouter: ObservableObject {
    static let shared = AppRouter()
    private init() {}

    @Published var selectedTab: AppTab = .home
    @Published var paths: [AppTab: [AppRoute]] = [:]
    /// Текущая шторка. Модальные состояния взаимоисключающие (§24.1):
    /// новая шторка заменяет текущую.
    @Published var sheet: AppSheet?
    /// Полноэкранный оверлей (Новость при запуске, §23).
    @Published var fullScreen: AppSheet?
    /// Диалог «Нужен вход в аккаунт» гейта покупок (§3.2).
    @Published var isSignInRequiredAlertShown = false

    // MARK: - Базовые операции

    func pathBinding(for tab: AppTab) -> Binding<[AppRoute]> {
        Binding(
            get: { self.paths[tab] ?? [] },
            set: { self.paths[tab] = $0 }
        )
    }

    /// Push экрана в стек текущей вкладки.
    func show(_ route: AppRoute) {
        paths[selectedTab, default: []].append(route)
    }

    /// `back()` / `navigationUp()`.
    func back() {
        guard var path = paths[selectedTab], !path.isEmpty else { return }
        path.removeLast()
        paths[selectedTab] = path
    }

    func popToRoot() {
        paths[selectedTab] = []
    }

    /// Переход на вкладку; повторный выбор текущей ничего не делает (§3.1).
    func selectTab(_ tab: AppTab) {
        guard tab != selectedTab else { return }
        selectedTab = tab
    }

    func present(_ sheet: AppSheet) {
        if sheet.isFullScreen {
            self.sheet = nil
            fullScreen = sheet
        } else {
            self.sheet = sheet
        }
    }

    func dismissSheet() {
        sheet = nil
    }

    func dismissFullScreen() {
        fullScreen = nil
    }

    // MARK: - Методы Router (§3.2)

    func showHome() { selectTab(.home) }
    func showSalaryCalculation() { selectTab(.salary) }
    func showSettings() { selectTab(.settings) }
    /// `showProfile()` — корневая вкладка «Профиль», как нижнее меню (§3.2).
    func showProfile() { selectTab(.profile) }

    func showRouteForm(basicId: String?, isMakeCopy: Bool = false) {
        if isMakeCopy, let basicId = basicId {
            show(.routeFormCopy(basicId: basicId))
        } else {
            show(.routeForm(basicId: basicId))
        }
    }

    func showLocoForm(basicId: String, locoId: String? = nil) { show(.locoForm(basicId: basicId, locoId: locoId)) }
    func showTrainForm(basicId: String, trainId: String? = nil) { show(.trainForm(basicId: basicId, trainId: trainId)) }
    func showPassengerForm(basicId: String, passengerId: String? = nil) {
        show(.passengerForm(basicId: basicId, passengerId: passengerId))
    }
    func showOtherWorkForm(basicId: String, otherWorkId: String? = nil) {
        show(.otherWorkForm(basicId: basicId, otherWorkId: otherWorkId))
    }
    func showAllRoute() { show(.allRoutes) }
    func showSearch() { show(.search) }
    func showSettingSalary() { show(.settingSalary) }
    func showStatistics() { show(.statistics) }
    func showCalendar() { show(.calendar) }
    func showScheduleWizard() { show(.scheduleWizard) }
    func showAbsence() { show(.absence) }
    func showSettingsSection(_ section: SettingsSection) { show(.settingsSection(section)) }

    /// Гейт покупок (§3.2): без авторизации — диалог «Нужен вход в аккаунт»
    /// («Войти» → Профиль). `isAuthorized` — непустой bearer-токен; передаёт вызывающий,
    /// пока нет общего iOS-хелпера авторизации.
    func showPurchases(isAuthorized: Bool) {
        if isAuthorized {
            show(.purchases)
        } else {
            isSignInRequiredAlertShown = true
        }
    }

    /// Центральная «+» нижнего меню — новый маршрут (§3.4).
    /// TODO(волна «Главная/подписка»): проверка подписки и лимита
    /// `FREE_ROUTES_LIMIT = 20` с диалогами «Бесплатный лимит исчерпан» /
    /// «Пробный период» — нужен iOS-аналог `RouteActionsHelper.newRouteClick()`.
    /// Сейчас форма открывается сразу. Как в Android (`popUpTo(start)`), форма
    /// открывается поверх корня «Главной».
    func startNewRoute() {
        selectedTab = .home
        paths[.home] = [.routeForm(basicId: nil)]
    }
}
