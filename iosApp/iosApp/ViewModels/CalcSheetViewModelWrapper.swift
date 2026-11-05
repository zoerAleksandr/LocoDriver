import SwiftUI
import ComposeApp

/// Wrapper `CalcSheetIosViewModel` (§11.9) для шторки «Расчёт за смену».
@MainActor
final class CalcSheetViewModelWrapper: ObservableObject {
    let viewModel: CalcSheetIosViewModel

    @Published var salary: SalaryUi = SalaryStatementModelsKt.emptySalaryUi()

    init(basicId: String) {
        let vm = IosViewModelHelper.shared.createCalcSheetViewModel(basicId: basicId)
        self.viewModel = vm
        vm.watchSalary { [weak self] value in
            DispatchQueue.main.async { self?.salary = value }
        }
    }
}
