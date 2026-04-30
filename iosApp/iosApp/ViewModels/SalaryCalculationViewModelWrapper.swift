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
    /// Голый минимум для consistency с другими Wrapper'ами. На VM-стороне
    /// _error всегда nil (нет сетевых вызовов в этом VM). LastAction/retry
    /// не нужны — Шаг 6 добавит, когда появится сетевой запрос.
    @Published var error: AppError? = nil

    /// Токены подписок watchX. Отменяются в deinit.
    private var watchHandles: [WatchHandle] = []

    init() {
        watchHandles.append(viewModel.watchSummary { [weak self] s in
            DispatchQueue.main.async { self?.summary = s }
        })
        watchHandles.append(viewModel.watchRoutes { [weak self] list in
            DispatchQueue.main.async {
                self?.routes = list as? [SalaryCalculationIosViewModel.RouteRow] ?? []
            }
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
        watchHandles.append(viewModel.watchError { [weak self] e in
            DispatchQueue.main.async { self?.error = e }
        })
    }

    deinit {
        watchHandles.forEach { $0.cancel() }
    }

    func clearError() { viewModel.clearError() }

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
