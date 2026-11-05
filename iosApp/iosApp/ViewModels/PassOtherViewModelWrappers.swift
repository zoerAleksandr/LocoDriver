import SwiftUI
import ComposeApp

// Обёртки Kotlin-VM экранов «Пассажиром» (§7), «Прочая работа» (§8), «Напарники» (§8.5).
// По экземпляру на открытый экран; `deinit` вызывает `dispose()` (аналог `onCleared`).

@MainActor
final class PassengerScreenViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createPassengerScreenViewModel()

    @Published var state: PassengerScreenState? = nil

    init() {
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
    }

    deinit { viewModel.dispose() }

    func start(basicId: String, passengerId: String?) { viewModel.start(basicId: basicId, passengerId: passengerId) }
    func flush() { viewModel.flush() }

    func setTrainNumber(_ v: String) { viewModel.setTrainNumber(value: v) }
    func setStationDeparture(_ v: String) { viewModel.setStationDeparture(value: v) }
    func setStationArrival(_ v: String) { viewModel.setStationArrival(value: v) }
    func setNotes(_ v: String) { viewModel.setNotes(value: v) }
    func setTimeDeparture(_ ms: Int64?) { viewModel.setTimeDeparture(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeArrival(_ ms: Int64?) { viewModel.setTimeArrival(ms: ms.map { KotlinLong(value: $0) }) }
    func setWorkStartByArrival(_ v: Bool) { viewModel.setWorkStartByArrival(enabled: v) }
    func setWorkStartOffsetMinutes(_ v: Int) { viewModel.setWorkStartOffsetMinutes(minutes: Int32(v)) }

    func stationSuggestions(_ q: String) -> [String] { (viewModel.stationSuggestions(query: q) as? [String]) ?? [] }
    func removeStationName(_ v: String) { viewModel.removeStationName(value: v) }
}

@MainActor
final class OtherWorkScreenViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createOtherWorkScreenViewModel()

    @Published var state: OtherWorkScreenState? = nil

    init() {
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
        viewModel.watchMessages { text in
            DispatchQueue.main.async { ToastCenter.shared.show(text) }
        }
    }

    deinit { viewModel.dispose() }

    func start(basicId: String, otherWorkId: String?) { viewModel.start(basicId: basicId, otherWorkId: otherWorkId) }
    func flush() { viewModel.flush() }

    func setWorkType(_ v: String) { viewModel.setWorkType(type: v) }
    func addCustomType(_ v: String) { viewModel.addCustomType(name: v) }
    func deleteCustomType(_ v: String) { viewModel.deleteCustomType(name: v) }
    func setStation(_ v: String) { viewModel.setStation(value: v) }
    func setNotes(_ v: String) { viewModel.setNotes(value: v) }
    func setTimeStart(_ ms: Int64?) { viewModel.setTimeStart(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeEnd(_ ms: Int64?) { viewModel.setTimeEnd(ms: ms.map { KotlinLong(value: $0) }) }
    func applyTimeFromWork() { viewModel.applyTimeFromWork() }
    func applyTimeFromLoco() { viewModel.applyTimeFromLoco() }
    func selectLoco(_ id: String) { viewModel.selectLoco(id: id) }
    func dismissLocoPicker() { viewModel.dismissLocoPicker() }

    func stationSuggestions(_ q: String) -> [String] { (viewModel.stationSuggestions(query: q) as? [String]) ?? [] }
    func removeStationName(_ v: String) { viewModel.removeStationName(value: v) }
}

@MainActor
final class PartnersScreenViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createPartnersScreenViewModel()

    @Published var state: PartnersScreenState? = nil

    init() {
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
    }

    deinit { viewModel.dispose() }

    func start(basicId: String?) { viewModel.start(basicId: basicId) }
    func setQuery(_ v: String) { viewModel.setQuery(value: v) }
    func toggle(_ id: String) { viewModel.toggle(partnerId: id) }
    func deletePartner(_ id: String) { viewModel.deletePartner(partnerId: id) }
    func setPermanent(_ v: Bool) { viewModel.setPermanentForCandidate(value: v) }
    func confirmReplace() { viewModel.confirmReplacePermanent() }
    func dismissReplace() { viewModel.dismissReplacePermanent() }
    func confirm(_ onComplete: @escaping () -> Void) {
        viewModel.confirm { DispatchQueue.main.async { onComplete() } }
    }
}

@MainActor
final class PartnerEditorViewModelWrapper: ObservableObject {
    private let viewModel: PartnerEditorIosViewModel

    @Published var state: PartnerEditorScreenState? = nil

    init(partnerId: String?) {
        viewModel = IosViewModelHelper.shared.createPartnerEditorViewModel(partnerId: partnerId)
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
    }

    deinit { viewModel.dispose() }

    func setFullName(_ v: String) { viewModel.setFullName(value: v) }
    func setTabNumber(_ v: String) { viewModel.setTabNumber(value: v) }
    func setNotes(_ v: String) { viewModel.setNotes(value: v) }
    func setPermanent(_ v: Bool) { viewModel.setPermanent(value: v) }
    func confirmReplace() { viewModel.confirmReplacePermanent() }
    func dismissReplace() { viewModel.dismissReplacePermanent() }
    func saveNow() { viewModel.saveNow() }
    func delete() { viewModel.delete() }
}
