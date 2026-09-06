import SwiftUI
import ComposeApp

@MainActor
final class TrainFormViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getTrainFormViewModel()

    @Published var train: DomainTrain? = nil
    @Published var isSaved: Bool = false
    @Published var errorMessage: String? = nil

    init() {
        viewModel.watchTrain { [weak self] t in
            DispatchQueue.main.async { self?.train = t }
        }
        viewModel.watchIsSaved { [weak self] saved in
            DispatchQueue.main.async { self?.isSaved = saved.boolValue }
        }
        viewModel.watchErrorMessage { [weak self] message in
            DispatchQueue.main.async { self?.errorMessage = message }
        }
    }

    func load(routeId: String, trainId: String?) { viewModel.loadTrain(routeId: routeId, trainId: trainId) }
    func save() { viewModel.saveTrain() }
    func clearError() { viewModel.clearError() }

    func setNumber(_ v: String) { viewModel.setNumber(value: v) }
    func setWeight(_ v: String) { viewModel.setWeight(value: v) }
    func setAxle(_ v: String) { viewModel.setAxle(value: v) }
    func setDistance(_ v: String) { viewModel.setDistance(value: v) }
    func setLength(_ v: String) { viewModel.setLength(value: v) }

    // ── Вагонник ──────────────────────────────────────────────────────────────

    var carInspector: DomainCarInspector? { train?.carInspector }

    func addCarInspector() { viewModel.addCarInspector() }
    func removeCarInspector() { viewModel.removeCarInspector() }
    func setCarInspectorFullName(_ v: String) { viewModel.setCarInspectorFullName(value: v) }
    func setCarInspectorTabNumber(_ v: String) { viewModel.setCarInspectorTabNumber(value: v) }

    /// Время прицепки: Date ↔ миллисекунды epoch (0 = не задано).
    /// Конвертация через общий TimeFormatter — как в FormLocoView.
    var couplingTimeMs: Int64 { carInspector?.couplingTime?.int64Value ?? 0 }
    var hasCouplingTime: Bool { couplingTimeMs > 0 }

    func setCouplingTime(_ date: Date) {
        viewModel.setCarInspectorCouplingTime(ms: TimeFormatter.dateToMs(date))
    }

    func clearCouplingTime() {
        viewModel.setCarInspectorCouplingTime(ms: 0)
    }
}
