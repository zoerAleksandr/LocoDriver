import SwiftUI
import ComposeApp

@MainActor
final class SalaryCalculationViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getSalaryCalculationViewModel()

    @Published var summary: SalaryCalculationIosViewModel.MonthlySummary? = nil
    @Published var routes: [SalaryCalculationIosViewModel.RouteRow] = []
    @Published var isLoading: Bool = true
    @Published var currentMonth: Int = 0
    @Published var currentYear: Int = 0

    init() {
        viewModel.watchSummary { [weak self] s in
            DispatchQueue.main.async { self?.summary = s }
        }
        viewModel.watchRoutes { [weak self] list in
            DispatchQueue.main.async {
                self?.routes = list as? [SalaryCalculationIosViewModel.RouteRow] ?? []
            }
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

    func nextMonth() {
        viewModel.nextMonth()
    }

    func previousMonth() {
        viewModel.previousMonth()
    }

    func setMonth(month: Int, year: Int) {
        viewModel.setMonth(month: Int32(month), year: Int32(year))
    }
}
