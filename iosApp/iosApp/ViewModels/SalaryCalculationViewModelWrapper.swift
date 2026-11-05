import SwiftUI
import ComposeApp

/// Wrapper `SalaryCalculationIosViewModel` (§11). Логика — только Kotlin;
/// SwiftUI отвечает за раскладку.
@MainActor
final class SalaryCalculationViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getSalaryCalculationViewModel()

    @Published var statement: SalaryStatement = SalaryStatementModelsKt.emptySalaryStatement()
    @Published var monthYearList: [MonthYearItem] = []
    @Published var isLoading: Bool = true

    init() {
        viewModel.watchStatement { [weak self] value in
            DispatchQueue.main.async { self?.statement = value }
        }
        viewModel.watchMonthYearList { [weak self] list in
            DispatchQueue.main.async {
                self?.monthYearList = (list as? [MonthYearItem]) ?? []
            }
        }
        viewModel.watchIsLoading { [weak self] loading in
            DispatchQueue.main.async { self?.isLoading = loading.boolValue }
        }
    }

    func nextMonth() { viewModel.nextMonth() }
    func previousMonth() { viewModel.previousMonth() }
    func selectYearAndMonth(year: Int, month: Int) {
        viewModel.selectYearAndMonth(year: Int32(year), month: Int32(month))
    }
    func dismissUnderworkInfoForever() { viewModel.dismissUnderworkInfoForever() }
    func hideUnderworkInfoForSession() { viewModel.hideUnderworkInfoForSession() }
}
