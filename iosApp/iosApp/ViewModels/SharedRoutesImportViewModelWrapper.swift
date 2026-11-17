import Foundation
import Combine
import ComposeApp

@MainActor
final class SharedRoutesImportViewModelWrapper: ObservableObject {
    static let shared = SharedRoutesImportViewModelWrapper()

    private let viewModel = SharedRouteLinkHandler.shared

    @Published private(set) var routes: [DomainRoute] = []
    @Published private(set) var isLoading = false
    @Published private(set) var isSaving = false
    @Published private(set) var errorMessage: String? = nil

    var isPresented: Bool {
        isLoading || isSaving || !routes.isEmpty || errorMessage != nil
    }

    private init() {
        viewModel.watchPendingRoutes { [weak self] list in
            DispatchQueue.main.async {
                self?.routes = list as? [DomainRoute] ?? []
            }
        }
        viewModel.watchBatchLoading { [weak self] loading in
            DispatchQueue.main.async { self?.isLoading = loading.boolValue }
        }
        viewModel.watchBatchSaving { [weak self] saving in
            DispatchQueue.main.async { self?.isSaving = saving.boolValue }
        }
        viewModel.watchBatchErrorMessage { [weak self] message in
            DispatchQueue.main.async { self?.errorMessage = message as? String }
        }
    }

    func save() { viewModel.savePendingRoutes() }
    func cancel() { viewModel.cancelPendingRoutes() }
}
