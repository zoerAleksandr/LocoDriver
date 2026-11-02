import SwiftUI
import ComposeApp

/// Обёртка Kotlin `RouteFormIosViewModel` (SCREEN_SPECS §5): по экземпляру на открытую форму.
/// Все изменения `@Published` — на главном потоке; Kotlin присылает данные с фонового.
@MainActor
final class RouteFormViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createRouteFormViewModel()

    @Published var state: RouteFormState? = nil
    @Published var salary: SalaryUi? = nil
    @Published var rest: RestUi? = nil

    /// События (закрыть экран, тост, шаринг). Если обработчик ещё не задан — копятся.
    private var pendingEvents: [RouteFormEvent] = []
    var eventHandler: ((RouteFormEvent) -> Void)? {
        didSet {
            guard let handler = eventHandler else { return }
            let queued = pendingEvents
            pendingEvents.removeAll()
            queued.forEach(handler)
        }
    }

    init() {
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
        viewModel.watchSalary { [weak self] s in
            DispatchQueue.main.async { self?.salary = s }
        }
        viewModel.watchRest { [weak self] r in
            DispatchQueue.main.async { self?.rest = r }
        }
        viewModel.watchEvents { [weak self] e in
            DispatchQueue.main.async {
                guard let self = self else { return }
                if let handler = self.eventHandler { handler(e) } else { self.pendingEvents.append(e) }
            }
        }
    }

    deinit {
        // Финальное сохранение несохранённых изменений (аналог onCleared, §5.2).
        viewModel.dispose()
    }

    func start(routeId: String?, isCopy: Bool, sharedPreview: Bool) {
        viewModel.start(routeId: routeId, isCopy: isCopy, sharedPreview: sharedPreview)
    }

    func setNumber(_ v: String) { viewModel.setNumber(value: v) }
    func setNotes(_ v: String) { viewModel.setNotes(value: v) }
    func setOnePerson(_ v: Bool) { viewModel.setOnePerson(value: v) }
    func toggleFavorite() { viewModel.setFavorite() }
    func setRestPointOfTurnover(_ v: Bool) { viewModel.setRestPointOfTurnover(value: v) }

    func setTimeStartWork(_ ms: Int64?) { viewModel.setTimeStartWork(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeEndWork(_ ms: Int64?) { viewModel.setTimeEndWork(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeStartBreak(_ ms: Int64?) { viewModel.setTimeStartBreak(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeEndBreak(_ ms: Int64?) { viewModel.setTimeEndBreak(ms: ms.map { KotlinLong(value: $0) }) }
    func disableWorkStartByArrival() { viewModel.disableWorkStartByArrival() }

    func deleteLoco(_ id: String) { viewModel.deleteLoco(id: id) }
    func deleteTrain(_ id: String) { viewModel.deleteTrain(id: id) }
    func deletePassenger(_ id: String) { viewModel.deletePassenger(id: id) }
    func deleteOtherWork(_ id: String) { viewModel.deleteOtherWork(id: id) }
    func deletePartner(_ id: String) { viewModel.deletePartner(id: id) }

    /// Сохранить маршрут (если нужно) и затем выполнить переход в дочерний раздел.
    func preSave(_ then: @escaping () -> Void) {
        viewModel.preSave { DispatchQueue.main.async { then() } }
    }

    func onSaveClick() { viewModel.onSaveClick() }
    func saveSharedRouteAndExit() { viewModel.saveSharedRouteAndExit() }
    func cancelSharedPreview() { viewModel.cancelSharedPreview() }
    func hideSharedSheet() { viewModel.hideSharedSheet() }
    func dismissDuplicate() { viewModel.dismissDuplicate() }
    func confirmReplaceDuplicate() { viewModel.confirmReplaceDuplicate() }
    func confirmKeepBothDuplicates() { viewModel.confirmKeepBothDuplicates() }
    func deleteRoute() { viewModel.deleteRoute() }
    func share() { viewModel.share() }

    func dismissPassenger12h() { viewModel.dismissPassenger12h() }
    func savePassenger12h(departure: String?, arrival: String?, timeDeparture: Int64, timeArrival: Int64) {
        viewModel.savePassengerFromSheet(
            stationDeparture: departure,
            stationArrival: arrival,
            timeDeparture: timeDeparture,
            timeArrival: timeArrival
        )
    }
    func suggestStations(_ query: String) -> [String] {
        (viewModel.suggestStations(query: query) as? [String]) ?? []
    }
}
