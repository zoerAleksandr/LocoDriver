import SwiftUI
import ComposeApp

/// Обёртка Kotlin `TrainScreenIosViewModel` (SCREEN_SPECS §6): по экземпляру на открытый экран.
/// `@Published` меняется на главном потоке — Kotlin присылает данные с фонового.
@MainActor
final class TrainScreenViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createTrainScreenViewModel()

    @Published var state: TrainScreenState? = nil

    init() {
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
    }

    deinit {
        // Финальное сохранение (аналог onCleared, §6.1).
        viewModel.dispose()
    }

    func start(basicId: String, trainId: String?) { viewModel.start(basicId: basicId, trainId: trainId) }
    func flush() { viewModel.flush() }
    func clearSaveError() { viewModel.clearSaveError() }

    // Номера и характеристики
    func setNumber(_ v: String) { viewModel.setNumber(value: v) }
    func addCurrentNumberToAdditional() { viewModel.addCurrentNumberToAdditional() }
    func removeAdditionalNumber(_ i: Int) { viewModel.removeAdditionalNumber(index: Int32(i)) }
    func setDistance(_ v: String) { viewModel.setDistance(value: v) }
    func setWeight(_ v: String) { viewModel.setWeight(value: v) }
    func setAxle(_ v: String) { viewModel.setAxle(value: v) }
    func setLength(_ v: String) { viewModel.setConditionalLength(value: v) }

    // История данных
    func dataPrefill(stationId: String?) -> TrainVersionUi { viewModel.dataPrefill(stationId: stationId) }
    func addDataVersion(stationId: String, stationName: String?, weight: String, axle: String, length: String) {
        viewModel.addTrainDataVersion(stationId: stationId, stationName: stationName, weight: weight, axle: axle, conditionalLength: length)
    }
    func updateDataVersion(_ i: Int, weight: String, axle: String, length: String) {
        viewModel.updateTrainDataVersion(index: Int32(i), weight: weight, axle: axle, conditionalLength: length)
    }
    func deleteDataVersion(_ i: Int) { viewModel.deleteTrainDataVersion(index: Int32(i)) }

    // Вспомогательная тяга: kind 0 толкач, 1 двойная тяга, 2 сдвоенный
    func addAssist(_ kind: Int) { viewModel.addAssist(kind: Int32(kind)) }
    func removeAssist(_ kind: Int) { viewModel.removeAssist(kind: Int32(kind)) }
    func setAssistField(_ kind: Int, _ field: String, _ value: String) {
        viewModel.setAssistField(kind: Int32(kind), field: field, value: value)
    }
    func setAssistDirection(_ kind: Int, isFirst: Bool) { viewModel.setAssistDirection(kind: Int32(kind), isFirst: isFirst) }
    func saveAssistSeries() { viewModel.saveAssistSeries() }
    func suggestSeries(_ q: String) -> [String] { (viewModel.suggestSeries(query: q) as? [String]) ?? [] }
    func removeSeriesSuggestion(_ v: String) { viewModel.removeSeriesSuggestion(name: v) }

    // Вагонник
    func addCarInspector() { viewModel.addCarInspector() }
    func removeCarInspector() { viewModel.removeCarInspector() }
    func setCarInspectorFullName(_ v: String) { viewModel.setCarInspectorFullName(value: v) }
    func setCarInspectorTabNumber(_ v: String) { viewModel.setCarInspectorTabNumber(value: v) }
    func setCouplingTime(_ ms: Int64) { viewModel.setCarInspectorCouplingTime(ms: ms) }

    // Плечо
    func selectPhase(_ id: String?) { viewModel.selectPhase(id: id) }
    func savePhase(id: String?, from: String, to: String, distance: Int, rate: Double, alsoReverse: Bool) {
        viewModel.savePhase(id: id, from: from, to: to, distance: Int32(distance), rate: rate, alsoReverse: alsoReverse)
    }
    func deletePhase(_ id: String) { viewModel.deletePhase(id: id) }

    // Станции
    func makeStationId() -> String { viewModel.makeStationId() }
    func saveStation(index: Int, newStationId: String?, name: String, track: String,
                     arrival: Int64?, departure: Int64?, isFinal: Bool, isPassing: Bool) {
        viewModel.saveStation(
            index: Int32(index), newStationId: newStationId, name: name, track: track,
            arrival: arrival.map { KotlinLong(value: $0) }, departure: departure.map { KotlinLong(value: $0) },
            isFinal: isFinal, isPassing: isPassing
        )
    }
    func deleteStation(_ index: Int) { viewModel.deleteStation(index: Int32(index)) }
    func moveStation(from: Int, to: Int) { viewModel.moveStation(from: Int32(from), to: Int32(to)) }
    func saveSegment(afterIndex: Int, from: String, to: String, track: String, notes: String) {
        viewModel.saveSegment(afterIndex: Int32(afterIndex), fromName: from, toName: to, track: track, notes: notes)
    }
    func toggleSortOrder() { viewModel.toggleSortOrder() }
    func toggleSegmentsVisibility() -> Bool { viewModel.toggleSegmentsVisibility() }
    func suggestStations(_ q: String) -> [String] { (viewModel.suggestStations(query: q) as? [String]) ?? [] }
    func removeStationSuggestion(_ v: String) { viewModel.removeStationSuggestion(name: v) }

    func go() { viewModel.go() }
    func stopwatch(now: Date) -> TrainStopwatchUi { viewModel.stopwatch(nowMs: TimeFormatter.dateToMs(now)) }
}
