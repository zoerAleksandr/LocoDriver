import SwiftUI
import ComposeApp

/// Wrapper «Новое отвлечение» (SCREEN_SPECS §16.1, §16.4). VM — factory, `dispose()` в deinit.
@MainActor
final class AbsenceViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createAbsenceViewModel()

    @Published var ui: AbsenceIosUi? = nil

    var typeOptions: [AbsenceIosTypeOption] {
        (viewModel.typeOptions as? [AbsenceIosTypeOption]) ?? []
    }

    init() {
        viewModel.watchUi { [weak self] s in
            DispatchQueue.main.async { self?.ui = s }
        }
    }

    deinit {
        viewModel.dispose()
    }

    func prepareScreen() { viewModel.prepareScreen() }
    func shiftMonth(_ delta: Int) { viewModel.shiftMonth(delta: Int32(delta)) }
    func onDayTap(_ day: Int) { viewModel.onDayTap(day: Int32(day)) }
    func setType(_ kind: String) { viewModel.setType(kind: kind) }
    func toggleTypePicker() { viewModel.toggleTypePicker() }
    func save() { viewModel.save() }
    func consumeError() { viewModel.consumeError() }
}
