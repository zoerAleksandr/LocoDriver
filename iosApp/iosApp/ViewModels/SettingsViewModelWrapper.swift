import SwiftUI
import ComposeApp

@MainActor
final class SettingsViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getSettingsViewModel()

    @Published var settings: UserSettings? = nil
    @Published var isLoading: Bool = true

    init() {
        viewModel.watchSettings { [weak self] s in
            DispatchQueue.main.async { self?.settings = s }
        }
        viewModel.watchIsLoading { [weak self] loading in
            DispatchQueue.main.async { self?.isLoading = loading.boolValue }
        }
    }
}
