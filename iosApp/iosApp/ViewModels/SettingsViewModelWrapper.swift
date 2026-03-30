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

    func setUsingDefaultWorkTime(_ value: Bool) { viewModel.setUsingDefaultWorkTime(value: value) }
    func setConsiderFutureRoute(_ value: Bool) { viewModel.setConsiderFutureRoute(value: value) }
    func setShowLocoHeating(_ value: Bool) { viewModel.setShowLocoHeating(value: value) }
    func setShowLocoAuxiliary(_ value: Bool) { viewModel.setShowLocoAuxiliary(value: value) }
    func setShowLocoStatistics(_ value: Bool) { viewModel.setShowLocoStatistics(value: value) }
    func setShowLocoNorma(_ value: Bool) { viewModel.setShowLocoNorma(value: value) }
}
