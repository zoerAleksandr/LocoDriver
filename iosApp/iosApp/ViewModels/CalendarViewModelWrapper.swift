import SwiftUI
import ComposeApp

/// Wrapper Календаря (SCREEN_SPECS §14). VM — single (Koin), месяц глобален.
@MainActor
final class CalendarViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getCalendarViewModel()

    @Published var ui: CalendarIosUi? = nil
    @Published var plan: CalendarIosPlanUi? = nil
    @Published var limit: CalendarIosLimit? = nil
    @Published var duplicate: CalendarIosDuplicate? = nil
    @Published var message: String? = nil
    @Published var shareLink: String? = nil
    @Published var openRouteFormId: String? = nil

    init() {
        viewModel.watchUi { [weak self] s in
            DispatchQueue.main.async { self?.ui = s }
        }
        viewModel.watchPlan { [weak self] s in
            DispatchQueue.main.async { self?.plan = s }
        }
        viewModel.watchLimit { [weak self] s in
            DispatchQueue.main.async { self?.limit = s }
        }
        viewModel.watchDuplicate { [weak self] s in
            DispatchQueue.main.async { self?.duplicate = s }
        }
        viewModel.watchMessage { [weak self] text in
            DispatchQueue.main.async { self?.message = text }
        }
        viewModel.watchShareLink { [weak self] url in
            DispatchQueue.main.async { self?.shareLink = url }
        }
        viewModel.watchOpenRouteForm { [weak self] basicId in
            DispatchQueue.main.async { self?.openRouteFormId = basicId }
        }
    }

    func prepareScreen() { viewModel.prepareScreen() }
    func reload() { viewModel.reload() }
    func shiftMonth(_ delta: Int) { viewModel.shiftMonth(delta: Int32(delta)) }

    // Планирование (§14.4)
    func enterRoutePlan(initialDay: Int) { viewModel.enterRoutePlan(initialDay: Int32(initialDay)) }
    func exitRoutePlan() { viewModel.exitRoutePlan() }
    func pickPlanTime(millis: Int64) { viewModel.pickPlanTime(millis: millis) }
    func addCustomPlanTime(hour: Int, minute: Int) {
        viewModel.addCustomPlanTime(hour: Int32(hour), minute: Int32(minute))
    }
    func toggleDayPlan(_ day: Int) { viewModel.toggleDayPlan(day: Int32(day)) }
    func createPlannedRoutes(durationMillis: Int64?) {
        viewModel.createPlannedRoutes(workDurationMillis: durationMillis.map { KotlinLong(value: $0) })
    }
    func replaceDuplicateAndCreate() { viewModel.replaceDuplicateAndCreate() }
    func keepDuplicateAndCreate() { viewModel.keepDuplicateAndCreate() }
    func dismissDuplicate() { viewModel.dismissDuplicate() }
    func dismissLimit() { viewModel.dismissLimit() }

    // Отвлечения (§14.3, §14.5)
    func addDayOff(_ day: Int) { viewModel.addDayOff(day: Int32(day)) }
    func addTechnicalStudy(_ day: Int, hours: Double) {
        viewModel.addTechnicalStudy(day: Int32(day), hours: hours)
    }
    func deleteAbsenceDay(_ day: Int) { viewModel.deleteAbsenceDay(day: Int32(day)) }
    func deleteAbsencePeriod(start: Int, end: Int) {
        viewModel.deleteAbsencePeriod(startDay: Int32(start), endDay: Int32(end))
    }
    func techStudyPayText(hours: Double) -> String { viewModel.techStudyPayText(hours: hours) }

    // Маршруты
    func deleteRoute(_ basicId: String) { viewModel.deleteRoute(basicId: basicId) }
    func setFavoriteRoute(_ basicId: String) { viewModel.setFavoriteRoute(basicId: basicId) }
    func syncRoute(_ basicId: String) { viewModel.syncRoute(basicId: basicId) }
    func makeCopyRoute(_ basicId: String) { viewModel.makeCopyRoute(basicId: basicId) }
    func shareRoute(_ basicId: String) { viewModel.shareRoute(basicId: basicId) }

    func consumeMessage() { message = nil }
    func consumeShareLink() { shareLink = nil }
    func consumeOpenRouteForm() { openRouteFormId = nil }
}
