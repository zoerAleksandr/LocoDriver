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
    }

    func deleteRoute(routeId: String) {
        viewModel.deleteRoute(routeId: routeId)
    }

    func copyRoute(routeId: String) {
        viewModel.doCopyRoute(routeId: routeId)
    }

    func setCurrentMonth(month: Int, year: Int) {
        viewModel.setCurrentMonth(month: Int32(month), year: Int32(year))
    }
}
