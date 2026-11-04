import SwiftUI
import ComposeApp

/// Обёртка Kotlin `LocoScreenIosViewModel` (SCREEN_SPECS §1): по экземпляру на открытый экран.
/// `@Published` меняется на главном потоке — Kotlin присылает данные с фонового.
@MainActor
final class LocoScreenViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createLocoScreenViewModel()

    @Published var state: LocoScreenState? = nil

    init() {
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
    }

    deinit {
        // Финальное сохранение (аналог onCleared, §1.2).
        viewModel.dispose()
    }

    func start(basicId: String, locoId: String?) { viewModel.start(basicId: basicId, locoId: locoId) }
    /// Немедленное сохранение (сворачивание приложения, уход с экрана).
    func flush() { viewModel.flush() }
    func reloadSettings() { viewModel.reloadSettingsFromPrefs() }

    func setSeries(_ v: String) { viewModel.setSeries(value: v) }
    func setNumber(_ v: String) { viewModel.setNumber(value: v) }
    func suggestSeries(_ q: String) -> [String] { (viewModel.suggestSeries(query: q) as? [String]) ?? [] }
    func selectSeries(_ v: String) { viewModel.selectSeries(name: v) }
    func removeSeriesSuggestion(_ v: String) { viewModel.removeSeriesSuggestion(name: v) }
    func changeType(isElectric: Bool) { viewModel.changeType(isElectric: isElectric) }

    func saveAcceptance(_ r: TimeSheetResultUi) {
        viewModel.saveAcceptanceFromSheet(startTime: r.startTime, endTime: r.endTime, barrierOut: r.barrierOut, stationId: r.stationId)
    }
    func saveDelivery(_ r: TimeSheetResultUi) {
        viewModel.saveDeliveryFromSheet(barrierIn: r.barrierIn, startTime: r.startTime, endTime: r.endTime, stationId: r.stationId)
    }
    func setTimeEndWork(_ ms: Int64?) { viewModel.setTimeEndWork(value: ms.map { KotlinLong(value: $0) }) }

    func addSection() { viewModel.addSection() }
    func requestDeleteSection(_ id: String) -> Bool { viewModel.requestDeleteSection(id: id) }
    func deleteSection(_ id: String) { viewModel.deleteSection(id: id) }
    func moveSection(from: Int, to: Int) { viewModel.moveSection(from: Int32(from), to: Int32(to)) }
    func toggleNumbering() { viewModel.toggleNumbering() }
    func toggleKiloMode() { viewModel.toggleKiloMode() }

    func setFuel(_ index: Int, _ field: String, _ text: String) { viewModel.setFuel(index: Int32(index), field: field, text: text) }
    func setRefuel(_ index: Int, _ text: String) { viewModel.setRefuel(index: Int32(index), text: text) }
    func setRefuelKilo(_ index: Int, _ text: String) { viewModel.setRefuelKilo(index: Int32(index), text: text) }
    func openCoefficient(_ index: Int) { viewModel.openCoefficient(index: Int32(index)) }
    func setCoefficient(_ index: Int, _ text: String) { viewModel.setCoefficient(index: Int32(index), text: text) }
    func closeCoefficient(_ index: Int) { viewModel.closeCoefficient(index: Int32(index)) }
    func setRefuelCoefficient(_ index: Int, _ text: String, applyToSection: Bool) {
        viewModel.setRefuelCoefficient(index: Int32(index), text: text, applyToSection: applyToSection)
    }
    func applyRefuelCoefficientToSection(_ index: Int) { viewModel.applyRefuelCoefficientToSection(index: Int32(index)) }
    func closeRefuelCoefficient(_ index: Int) { viewModel.closeRefuelCoefficient(index: Int32(index)) }
    func recentCoefficients() -> [String] { (viewModel.recentCoefficients() as? [String]) ?? [] }
    func stepCoefficient(_ value: String, _ direction: Int) -> String { viewModel.stepCoefficient(value: value, direction: Int32(direction)) }

    func setEnergy(_ index: Int, _ field: String, _ text: String) { viewModel.setEnergy(index: Int32(index), field: field, text: text) }
    func setCounter(_ field: String, _ text: String) { viewModel.setCounter(field: field, text: text) }
    func setNorma1(_ v: String) { viewModel.setNorma1(text: v) }
    func setNorma2(_ v: String) { viewModel.setNorma2(text: v) }
    func setNormaDiesel(_ v: String) { viewModel.setNormaDiesel(text: v) }
    func clearSaveError() { viewModel.clearSaveError() }
}
