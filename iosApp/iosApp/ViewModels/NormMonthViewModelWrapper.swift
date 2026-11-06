import SwiftUI
import ComposeApp

/// Wrapper «Норма по месяцам» (SCREEN_SPECS §16.3, §16.3.1). VM — factory, `dispose()` в deinit.
@MainActor
final class NormMonthViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createNormMonthViewModel()

    @Published var ui: NormMonthIosUi? = nil

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

    // Рабочая неделя (§16.3)
    func setWeekMode(_ mode: String) { viewModel.setWeekMode(mode: mode) }
    func toggleWeekTemplate() { viewModel.toggleWeekTemplate() }
    func setWeekdayHours(_ index: Int, hours: Int) {
        viewModel.setWeekdayHours(index: Int32(index), hours: Int32(hours))
    }
    func setWeekdayDayOff(_ index: Int, isDayOff: Bool) {
        viewModel.setWeekdayDayOff(index: Int32(index), isDayOff: isDayOff)
    }
    func applyTemplateToMonth() { viewModel.applyTemplateToMonth() }

    // Правки дня
    func setDayHours(_ day: Int, hours: Int) {
        viewModel.setDayHours(day: Int32(day), hours: Int32(hours))
    }
    func resetDayHours(_ day: Int) { viewModel.resetDayHours(day: Int32(day)) }
    func toggleDayOff(_ day: Int) { viewModel.toggleDayOff(day: Int32(day)) }

    func consumeMessage() { viewModel.consumeMessage() }
}
