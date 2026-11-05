import SwiftUI
import ComposeApp

/// Файл PDF для презентации после шторки действий (§21 «PDF сформирован»).
struct PdfFileItem: Identifiable {
    let id = UUID()
    let url: URL
}

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
    /// Диалог гейта кнопки «+» (§3.4): «Бесплатный лимит исчерпан» / «Пробный период».
    @Published var newRouteGate: NewRouteGate?
    /// Диалог «Не хватает бесплатных маршрутов» (§19.1a) для пакетного создания.
    @Published var subscriptionLimit: SubscriptionLimitRequest?
    /// §21 PDF: файл для просмотра (QuickLook) / шаринга — после закрытия шторки (§24.1).
    @Published var pdfPreview: PdfFileItem?
    @Published var pdfShare: PdfFileItem?

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

    /// §21 PDF: «Открыть PDF» (QuickLook) после закрытия шторки действий.
    func presentPdfPreview(url: URL) {
        pdfPreview = PdfFileItem(url: url)
    }

    /// §21 PDF: «Поделиться» (share sheet) после закрытия шторки действий.
    func presentPdfShare(url: URL) {
        pdfShare = PdfFileItem(url: url)
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

    /// Центральная «+» нижнего меню — новый маршрут (§3.4). iOS-аналог
    /// `RouteActionsHelper.newRouteClick()`: решение считает `NewRouteIosViewModel`
    /// (подписка с грейсом 1 сутки, лимит `FREE_ROUTES_LIMIT = 20` с учётом корзины).
    /// Активная подписка → форма сразу; иначе — диалог `newRouteGate`; ошибка — ничего.
    func startNewRoute() {
        IosViewModelHelper.shared.getNewRouteViewModel().onNewRouteClick { [weak self] decision in
            DispatchQueue.main.async {
                guard let self = self else { return }
                switch decision.type {
                case "open":
                    self.openNewRouteForm()
                case "limit":
                    self.newRouteGate = .limitReached(isAuthorized: decision.isAuthorized)
                case "trial":
                    self.newRouteGate = .trial(
                        freeRoutesLeft: Int(decision.freeRoutesLeft),
                        isAuthorized: decision.isAuthorized
                    )
                default:
                    break
                }
            }
        }
    }

    /// Открыть новую форму маршрута. Как в Android (`popUpTo(start)`), форма
    /// открывается поверх корня «Главной».
    func openNewRouteForm() {
        selectedTab = .home
        paths[.home] = [.routeForm(basicId: nil)]
    }

    /// «Оформить подписку» из диалога гейта «+»: через гейт покупок (§3.2).
    /// Задержка — чтобы диалог «Нужен вход в аккаунт» не столкнулся с закрытием текущего.
    func showPurchasesFromNewRouteGate(isAuthorized: Bool) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
            self.showPurchases(isAuthorized: isAuthorized)
        }
    }

    /// Пакетное создание маршрутов (Календарь «запланировать», мастер «Применить»,
    /// §19.1): пачка создаётся целиком и только если помещается в остаток
    /// бесплатного лимита; иначе — `SubscriptionLimitDialog` (§19.1a).
    /// Ошибка проверки → сообщение «Не удалось проверить подписку» (§19.1).
    func checkBatchRoutes(count: Int32) {
        IosViewModelHelper.shared.getPurchasesViewModel().checkBatchRoutes(count: count) { [weak self] gate in
            DispatchQueue.main.async {
                guard let self = self else { return }
                switch gate.type {
                case "limit":
                    self.subscriptionLimit = SubscriptionLimitRequest(
                        requested: Int(gate.requested),
                        remaining: Int(gate.remaining),
                        isAuthorized: gate.isAuthorized
                    )
                case "error":
                    self.presentSnackbarMessage("Не удалось проверить подписку")
                default:
                    break
                }
            }
        }
    }

    /// Короткое сообщение внизу экрана без привязки к конкретному View (§19.1).
    /// Показывается через `AppRouter.shared.snackbarMessage`.
    func presentSnackbarMessage(_ text: String) {
        snackbarMessage = text
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            if self.snackbarMessage == text { self.snackbarMessage = nil }
        }
    }

    /// Snackbar-сообщение глобального уровня (пока используется гейтом §19.1).
    @Published var snackbarMessage: String? = nil
}

/// Диалог гейта «+» (§3.4).
enum NewRouteGate: Equatable {
    /// «Бесплатный лимит исчерпан» — «Оформить подписку» / «Отмена».
    case limitReached(isAuthorized: Bool)
    /// «Пробный период» — «Продолжить бесплатно» / «Оформить подписку».
    case trial(freeRoutesLeft: Int, isAuthorized: Bool)

    var title: String {
        switch self {
        case .limitReached: return "Бесплатный лимит исчерпан"
        case .trial: return "Пробный период"
        }
    }

    var message: String {
        switch self {
        case .limitReached:
            return "Для добавления новых маршрутов оформите подписку."
        case let .trial(left, _):
            return "Осталось бесплатных маршрутов: \(left) из 20. Оформите подписку для неограниченного использования или продолжите бесплатно."
        }
    }

    var isAuthorized: Bool {
        switch self {
        case let .limitReached(isAuthorized): return isAuthorized
        case let .trial(_, isAuthorized): return isAuthorized
        }
    }
}

/// Запрос диалога «Не хватает бесплатных маршрутов» (SubscriptionLimitDialog, §19.1a).
struct SubscriptionLimitRequest: Identifiable, Equatable {
    let id = UUID()
    let requested: Int
    let remaining: Int
    let isAuthorized: Bool
}
