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

    init() {
        viewModel.watchRoutes { [weak self] list in
            DispatchQueue.main.async {
                self?.routes = list as? [DomainRoute] ?? []
            }
        }
        viewModel.watchSettings { [weak self] s in
            DispatchQueue.main.async { self?.settings = s }
        }
        viewModel.watchIsLoading { [weak self] loading in
            DispatchQueue.main.async { self?.isLoading = loading.boolValue }
        }
        viewModel.watchCurrentMonth { [weak self] month in
            DispatchQueue.main.async { self?.currentMonth = Int(month) }
        }
        viewModel.watchCurrentYear { [weak self] year in
            DispatchQueue.main.async { self?.currentYear = Int(year) }
        }

        // Статистика
        viewModel.watchTotalWorkMs { [weak self] v in
            DispatchQueue.main.async { self?.totalWorkMs = Int64(v) }
        }
        viewModel.watchNightWorkMs { [weak self] v in
            DispatchQueue.main.async { self?.nightWorkMs = Int64(v) }
        }
        viewModel.watchPassengerWorkMs { [weak self] v in
            DispatchQueue.main.async { self?.passengerWorkMs = Int64(v) }
        }
        viewModel.watchReserveWorkMs { [weak self] v in
            DispatchQueue.main.async { self?.reserveWorkMs = Int64(v) }
        }
        viewModel.watchOnePersonMs { [weak self] v in
            DispatchQueue.main.async { self?.onePersonMs = Int64(v) }
        }
        viewModel.watchNormaHoursMonth { [weak self] v in
            DispatchQueue.main.async { self?.normaHoursMonth = Int(v) }
        }
        viewModel.watchNormaHoursToday { [weak self] v in
            DispatchQueue.main.async { self?.normaHoursToday = Int(v) }
        }
        viewModel.watchTodayWorkMs { [weak self] v in
            DispatchQueue.main.async { self?.todayWorkMs = Int64(v) }
        }

        // Snackbar-события синхронизации/шаринга.
        viewModel.watchMessages { msg in
            DispatchQueue.main.async { SyncToastCenter.shared.show(msg) }
        }
        // Публичная ссылка готова — сразу открываем системный share sheet.
        viewModel.watchShareLinks { text in
            DispatchQueue.main.async { ShareSheetPresenter.present(text: text) }
        }
        viewModel.watchIsSyncingRoute { [weak self] loading in
            DispatchQueue.main.async { self?.isSyncingRoute = loading.boolValue }
        }
        viewModel.watchIsCreatingShareLink { [weak self] loading in
            DispatchQueue.main.async { self?.isCreatingShareLink = loading.boolValue }
        }
    }

    @Published var isSyncingRoute: Bool = false
    @Published var isCreatingShareLink: Bool = false

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
        viewModel.syncRoute(routeId: routeId)
    }

    func shareRoute(routeId: String) {
        viewModel.shareRoute(routeId: routeId)
    }

    func setCurrentMonth(month: Int, year: Int) {
        viewModel.setCurrentMonth(month: Int32(month), year: Int32(year))
    }
}
