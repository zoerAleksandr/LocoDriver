import SwiftUI
import ComposeApp

@MainActor
final class HomeViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getHomeViewModel()

    @Published var routes: [DomainRoute] = []
    @Published var settings: DomainUserSettings? = nil
    @Published var isLoading: Bool = true
    @Published var currentMonth: Int = 0
    @Published var currentYear: Int = 2024

    // Статистика из KMP ViewModel
    @Published var totalWorkMs: Int64 = 0
    @Published var nightWorkMs: Int64 = 0
    @Published var passengerWorkMs: Int64 = 0
    @Published var reserveWorkMs: Int64 = 0
    @Published var onePersonMs: Int64 = 0
    @Published var normaHoursMonth: Int = 165
    @Published var normaHoursToday: Int = 0
    @Published var todayWorkMs: Int64 = 0

    /// Pull-to-refresh индикатор. SwiftUI .refreshable polls этот флаг,
    /// чтобы держать спиннер ровно пока идёт syncFromRemote.
    @Published var isRefreshing: Bool = false

    /// Типизированная ошибка для алерта с кнопкой «Повторить».
    @Published var error: AppError? = nil

    /// Last retry'абельное действие. delete/copy НЕ записываются:
    /// delete — silent recovery by design в HomeIosViewModel; copy —
    /// локальная DB-операция, ошибки маловероятны.
    private var lastAction: LastAction? = nil
    private enum LastAction {
        case sync(id: String)
        case share(id: String)
        case refresh
    }

    /// Токены подписок watchX. Отменяются в deinit, чтобы collect-корутины
    /// не накапливались в viewModelScope singleton-VM при пересоздании Wrapper'а.
    private var watchHandles: [WatchHandle] = []

    init() {
        watchHandles.append(viewModel.watchRoutes { [weak self] list in
            DispatchQueue.main.async {
                self?.routes = list as? [DomainRoute] ?? []
            }
        })
        watchHandles.append(viewModel.watchSettings { [weak self] s in
            DispatchQueue.main.async { self?.settings = s }
        })
        watchHandles.append(viewModel.watchIsLoading { [weak self] loading in
            DispatchQueue.main.async { self?.isLoading = loading.boolValue }
        })
        watchHandles.append(viewModel.watchCurrentMonth { [weak self] month in
            DispatchQueue.main.async { self?.currentMonth = Int(month) }
        })
        watchHandles.append(viewModel.watchCurrentYear { [weak self] year in
            DispatchQueue.main.async { self?.currentYear = Int(year) }
        })

        // Статистика
        watchHandles.append(viewModel.watchTotalWorkMs { [weak self] v in
            DispatchQueue.main.async { self?.totalWorkMs = Int64(v) }
        })
        watchHandles.append(viewModel.watchNightWorkMs { [weak self] v in
            DispatchQueue.main.async { self?.nightWorkMs = Int64(v) }
        })
        watchHandles.append(viewModel.watchPassengerWorkMs { [weak self] v in
            DispatchQueue.main.async { self?.passengerWorkMs = Int64(v) }
        })
        watchHandles.append(viewModel.watchReserveWorkMs { [weak self] v in
            DispatchQueue.main.async { self?.reserveWorkMs = Int64(v) }
        })
        watchHandles.append(viewModel.watchOnePersonMs { [weak self] v in
            DispatchQueue.main.async { self?.onePersonMs = Int64(v) }
        })
        watchHandles.append(viewModel.watchNormaHoursMonth { [weak self] v in
            DispatchQueue.main.async { self?.normaHoursMonth = Int(v) }
        })
        watchHandles.append(viewModel.watchNormaHoursToday { [weak self] v in
            DispatchQueue.main.async { self?.normaHoursToday = Int(v) }
        })
        watchHandles.append(viewModel.watchTodayWorkMs { [weak self] v in
            DispatchQueue.main.async { self?.todayWorkMs = Int64(v) }
        })

        // Snackbar-события синхронизации/шаринга.
        watchHandles.append(viewModel.watchMessages { msg in
            DispatchQueue.main.async { SyncToastCenter.shared.show(msg) }
        })
        // Публичная ссылка готова — сразу открываем системный share sheet.
        watchHandles.append(viewModel.watchShareLinks { text in
            DispatchQueue.main.async { ShareSheetPresenter.present(text: text) }
        })
        watchHandles.append(viewModel.watchIsSyncingRoute { [weak self] loading in
            DispatchQueue.main.async { self?.isSyncingRoute = loading.boolValue }
        })
        watchHandles.append(viewModel.watchIsCreatingShareLink { [weak self] loading in
            DispatchQueue.main.async { self?.isCreatingShareLink = loading.boolValue }
        })
        watchHandles.append(viewModel.watchIsRefreshing { [weak self] loading in
            DispatchQueue.main.async { self?.isRefreshing = loading.boolValue }
        })
        watchHandles.append(viewModel.watchError { [weak self] e in
            DispatchQueue.main.async { self?.error = e }
        })
    }

    deinit {
        watchHandles.forEach { $0.cancel() }
    }

    @Published var isSyncingRoute: Bool = false
    @Published var isCreatingShareLink: Bool = false

    // delete/copy не записывают lastAction — см. enum LastAction.
    func deleteRoute(routeId: String) {
        viewModel.deleteRoute(routeId: routeId)
    }

    func copyRoute(routeId: String) {
        viewModel.doCopyRoute(routeId: routeId)
    }

    func toggleFavorite(routeId: String) {
        viewModel.toggleFavorite(routeId: routeId)
    }

    func syncRoute(routeId: String) {
        lastAction = .sync(id: routeId)
        viewModel.syncRoute(routeId: routeId)
    }

    func shareRoute(routeId: String) {
        lastAction = .share(id: routeId)
        viewModel.shareRoute(routeId: routeId)
    }

    func refresh() {
        lastAction = .refresh
        viewModel.refresh()
    }

    func setCurrentMonth(month: Int, year: Int) {
        viewModel.setCurrentMonth(month: Int32(month), year: Int32(year))
    }

    func clearError() { viewModel.clearError(); lastAction = nil }

    func retry() {
        guard let a = lastAction else { return }
        switch a {
        case .sync(let id):  syncRoute(routeId: id)
        case .share(let id): shareRoute(routeId: id)
        case .refresh:       refresh()
        }
    }
}
