import SwiftUI
import ComposeApp

@MainActor
final class SalaryCalculationViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getSalaryCalculationViewModel()

    @Published var summary: SalaryCalculationIosViewModel.MonthlySummary? = nil
    @Published var isLoading: Bool = true

    init() {
        viewModel.watchSummary { [weak self] s in
            DispatchQueue.main.async { self?.summary = s }
        }
        viewModel.watchIsLoading { [weak self] loading in
            DispatchQueue.main.async { self?.isLoading = loading.boolValue }
        }
    }
}
