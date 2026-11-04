import SwiftUI
import ComposeApp

/// Обёртка Kotlin `LocoTimeSheetIosViewModel` (SCREEN_SPECS §2): по экземпляру на открытие шторки.
@MainActor
final class LocoTimeSheetViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createLocoTimeSheetViewModel()

    @Published var state = TimeSheetState(
        ready: false, isAcceptance: true, title: "", seriesName: nil, seriesId: nil, stationName: nil,
        stationId: nil, stationLabel: "", showNormMode: false, handToHand: false, warnings: [],
        isAsking: false, rows: [], saveSeries: nil, saveStation: nil, canCalculate: false,
        hasSequenceError: false, showRecalcDialog: false, recalcVariantText: "", catalogVersion: 0
    )

    init() {
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
    }

    deinit { viewModel.dispose() }

    func setCallbacks(
        onSave: @escaping (TimeSheetResultUi) -> Void,
        onSeriesChanged: @escaping (String) -> Void,
        onTimeEndWork: @escaping (Int64?) -> Void
    ) {
        viewModel.setCallbacks(
            onSave: { r in DispatchQueue.main.async { onSave(r) } },
            onSeriesChanged: { s in DispatchQueue.main.async { onSeriesChanged(s) } },
            onTimeEndWorkChanged: { ms in DispatchQueue.main.async { onTimeEndWork(ms?.int64Value) } }
        )
    }

    func start(kind: String, seriesName: String?, isElectric: Bool, initialStart: Int64?, initialEnd: Int64?,
               initialBarrierOut: Int64?, initialBarrierIn: Int64?, routeStartWork: Int64?, routeEndWork: Int64?,
               initialStationId: String?, offsetSeconds: Int) {
        func k(_ v: Int64?) -> KotlinLong? { v.map { KotlinLong(value: $0) } }
        viewModel.start(
            kind: kind, seriesName: seriesName, isElectric: isElectric,
            initialStartTime: k(initialStart), initialEndTime: k(initialEnd),
            initialBarrierOut: k(initialBarrierOut), initialBarrierIn: k(initialBarrierIn),
            routeStartWork: k(routeStartWork), routeEndWork: k(routeEndWork),
            initialStationId: initialStationId, displayOffsetSeconds: Int32(offsetSeconds)
        )
    }

    var rows: [TimeRowUi] { (state.rows as? [TimeRowUi]) ?? [] }
    var warnings: [TimeWarnUi] { (state.warnings as? [TimeWarnUi]) ?? [] }

    func applyNorms() { viewModel.applyNorms() }
    func tapRow(_ id: String) -> Bool { viewModel.tapRow(id: id) }
    func setRowTime(_ id: String, _ ms: Int64) { viewModel.setRowTime(id: id, ms: ms) }
    func clearRow(_ id: String) { viewModel.clearRow(id: id) }
    func acceptPendingWorkEnd() { viewModel.acceptPendingWorkEnd() }
    func setHandToHand(_ v: Bool) { viewModel.setHandToHand(value: v) }
    func answerRecalc(_ v: Bool) { viewModel.answerRecalc(recalculate: v) }
    func dismissWarning(_ t: String) { viewModel.dismissWarning(text: t) }
    func finish() -> Bool { viewModel.finish() }
    func saveSeriesNorm() { viewModel.saveSeriesNorm() }
    func saveStationNorm() { viewModel.saveStationNorm() }

    func selectSeries(_ id: String) { viewModel.selectSeries(id: id) }
    func clearSeries() { viewModel.clearSeries() }
    func selectStation(_ id: String) { viewModel.selectStation(id: id) }
    func clearStation() { viewModel.clearStation() }
    func seriesRows(_ q: String) -> [SeriesRowUi] { (viewModel.seriesRows(query: q) as? [SeriesRowUi]) ?? [] }
    func stationRows(_ q: String) -> [StationRowUi] { (viewModel.stationRows(query: q) as? [StationRowUi]) ?? [] }
    func seriesRenamed(old: String, new: String) { viewModel.seriesRenamed(oldName: old, newName: new) }
    func seriesRemoved(_ name: String) { viewModel.seriesRemoved(name: name) }
}
