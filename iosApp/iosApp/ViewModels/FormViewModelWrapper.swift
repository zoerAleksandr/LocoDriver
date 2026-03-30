import SwiftUI
import ComposeApp

@MainActor
final class FormViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getFormViewModel()

    @Published var route: Route? = nil
    @Published var isLoading: Bool = false
    @Published var isSaved: Bool = false
    @Published var errorMessage: String? = nil

    init() {
        viewModel.watchRoute { [weak self] r in
            DispatchQueue.main.async { self?.route = r }
        }
        viewModel.watchIsLoading { [weak self] loading in
            DispatchQueue.main.async { self?.isLoading = loading.boolValue }
        }
        viewModel.watchIsSaved { [weak self] saved in
            DispatchQueue.main.async { self?.isSaved = saved.boolValue }
        }
        viewModel.watchErrorMessage { [weak self] msg in
            DispatchQueue.main.async { self?.errorMessage = msg }
        }
    }

    func loadRoute(id: String?) { viewModel.loadRoute(routeId: id) }
    func updateNumber(_ value: String) { viewModel.updateNumber(value: value) }
    func updateNotes(_ value: String) { viewModel.updateNotes(value: value) }
    func saveRoute() { viewModel.saveRoute() }
}
