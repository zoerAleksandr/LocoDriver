import SwiftUI
import ComposeApp

/// Wrapper мастера «Заполнить месяц» (SCREEN_SPECS §15). VM — factory, `dispose()` в deinit.
@MainActor
final class ScheduleWizardViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createScheduleWizardViewModel()

    @Published var ui: WizardIosUi? = nil

    init() {
        viewModel.watchUi { [weak self] s in
            DispatchQueue.main.async { self?.ui = s }
        }
    }

    deinit {
        viewModel.dispose()
    }

    func prepareScreen() { viewModel.prepareScreen() }

    // Шаг 1
    func selectPattern(_ id: String) { viewModel.selectPattern(id: id) }
    func deletePattern(_ id: String) { viewModel.deletePattern(id: id) }
    func openTypePicker(_ index: Int) { viewModel.openTypePicker(index: Int32(index)) }
    func closeTypePicker() { viewModel.closeTypePicker() }
    func setCycleDayType(_ index: Int, kind: String) {
        viewModel.setCycleDayType(index: Int32(index), kind: kind)
    }
    func addCycleDay() { viewModel.addCycleDay() }
    func removeCycleDay(_ index: Int) { viewModel.removeCycleDay(index: Int32(index)) }
    func setDayStart(hour: Int, minute: Int) { viewModel.setDayStart(hour: Int32(hour), minute: Int32(minute)) }
    func setDayEnd(hour: Int, minute: Int) { viewModel.setDayEnd(hour: Int32(hour), minute: Int32(minute)) }
    func setNightStart(hour: Int, minute: Int) { viewModel.setNightStart(hour: Int32(hour), minute: Int32(minute)) }
    func setNightEnd(hour: Int, minute: Int) { viewModel.setNightEnd(hour: Int32(hour), minute: Int32(minute)) }

    // Шаг 2
    func setFirstDay(_ day: Int) { viewModel.setFirstDay(day: Int32(day)) }
    func setExtendToNextMonth(_ enabled: Bool) { viewModel.setExtendToNextMonth(enabled: enabled) }
    func goToStep(_ step: Int) { viewModel.goToStep(step: Int32(step)) }

    // §15.1
    func continuePreviousSchedule() { viewModel.continuePreviousSchedule() }
    func declineContinuePrevious() { viewModel.declineContinuePrevious() }
    func shiftMonth(_ delta: Int) { viewModel.shiftMonth(delta: Int32(delta)) }

    func apply() { viewModel.apply() }
    func dismissSubscriptionLimit() { viewModel.dismissSubscriptionLimit() }
    func consumeApplyMessage() { viewModel.consumeApplyMessage() }
}
