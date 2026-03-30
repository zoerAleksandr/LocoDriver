import SwiftUI
import ComposeApp

@MainActor
final class HomeViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getHomeViewModel()

    @Published var routes: [DomainRoute] = []
    @Published var settings: DomainUserSettings? = nil
    @Published var isLoading: Bool = true

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
    }
}
