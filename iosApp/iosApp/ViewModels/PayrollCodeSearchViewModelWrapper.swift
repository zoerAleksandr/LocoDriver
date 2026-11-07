import SwiftUI
import ComposeApp

/// Wrapper `PayrollCodeSearchIosViewModel` (§11.8).
@MainActor
final class PayrollCodeSearchViewModelWrapper: ObservableObject {
    let viewModel: PayrollCodeSearchIosViewModel

    @Published var query: String = ""
    @Published var results: [PayrollCodeItemUi] = []

    init(viewModel: PayrollCodeSearchIosViewModel? = nil) {
        let vm = viewModel ?? IosViewModelHelper.shared.createPayrollCodeSearchViewModel()
        self.viewModel = vm
        vm.watchQuery { [weak self] value in
            DispatchQueue.main.async { self?.query = value }
        }
        vm.watchResults { [weak self] list in
            DispatchQueue.main.async {
                self?.results = (list as? [PayrollCodeItemUi]) ?? []
            }
        }
    }

    func setQuery(_ value: String) { viewModel.setQuery(value: value) }
    func clearQuery() { viewModel.clearQuery() }
}
