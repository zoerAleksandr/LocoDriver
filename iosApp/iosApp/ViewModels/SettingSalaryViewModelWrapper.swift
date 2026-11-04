import SwiftUI
import ComposeApp

/// Wrapper `SettingSalaryIosViewModel` (§12).
@MainActor
final class SettingSalaryViewModelWrapper: ObservableObject {
    let viewModel: SettingSalaryIosViewModel

    @Published var state: SettingSalaryState = SalaryStatementModelsKt.emptySettingSalaryState()

    init(viewModel: SettingSalaryIosViewModel? = nil) {
        let vm = viewModel ?? IosViewModelHelper.shared.createSettingSalaryViewModel()
        self.viewModel = vm
        vm.watchState { [weak self] value in
            DispatchQueue.main.async { self?.state = value }
        }
    }

    /// Wrapper для шторки «Изменилась тарифная ставка» (§12.3) поверх уже
    /// открытого экрана настроек: переиспользует последний созданный VM.
    static func reusingActive() -> SettingSalaryViewModelWrapper {
        if let active = IosViewModelHelper.shared.lastSettingSalaryViewModel {
            return SettingSalaryViewModelWrapper(viewModel: active)
        }
        return SettingSalaryViewModelWrapper()
    }

    // ── Поля (§12.2) ────────────────────────────────────────────────────────
    func setTariffRate(_ v: String) { viewModel.setTariffRate(value: v) }
    func setOldTariffRate(_ v: String) { viewModel.setOldTariffRate(value: v) }
    func setAveragePaymentHour(_ v: String) { viewModel.setAveragePaymentHour(value: v) }
    func setDistrictCoefficient(_ v: String) { viewModel.setDistrictCoefficient(value: v) }
    func setNordicCoefficient(_ v: String) { viewModel.setNordicCoefficient(value: v) }
    func setZonalSurcharge(_ v: String) { viewModel.setZonalSurcharge(value: v) }
    func setSurchargeQualificationClass(_ v: String) { viewModel.setSurchargeQualificationClass(value: v) }
    func setOnePersonOperationPercent(_ v: String) { viewModel.setOnePersonOperationPercent(value: v) }
    func setOnePersonOperationPassengerTrainPercent(_ v: String) {
        viewModel.setOnePersonOperationPassengerTrainPercent(value: v)
    }
    func setHarmfulnessPercent(_ v: String) { viewModel.setHarmfulnessPercent(value: v) }
    func setSurchargeHeavyLongDistanceTrains(_ v: String) {
        viewModel.setSurchargeHeavyLongDistanceTrains(value: v)
    }
    func setOtherSurcharge(_ v: String) { viewModel.setOtherSurcharge(value: v) }
    func setNDFL(_ v: String) { viewModel.setNDFL(value: v) }
    func setUnionistsRetention(_ v: String) { viewModel.setUnionistsRetention(value: v) }
    func setOtherRetention(_ v: String) { viewModel.setOtherRetention(value: v) }
    func setWelfarePercent(_ v: String) { viewModel.setWelfarePercent(value: v) }
    func setAlimonyPercent(_ v: String) { viewModel.setAlimonyPercent(value: v) }
    func setShowUnderworkPayments(_ v: Bool) { viewModel.setShowUnderworkPayments(value: v) }

    // ── Пороговые доплаты (§12.1) ───────────────────────────────────────────
    func addSurchargeHeavyTrain() { viewModel.addSurchargeHeavyTrain() }
    func setSurchargeHeavyTrainWeight(id: String, _ v: String) {
        viewModel.setSurchargeHeavyTrainWeight(id: id, value: v)
    }
    func setSurchargeHeavyTrainPercent(id: String, _ v: String) {
        viewModel.setSurchargeHeavyTrainPercent(id: id, value: v)
    }
    func deleteSurchargeHeavyTrain(id: String) { viewModel.deleteSurchargeHeavyTrain(id: id) }

    func addSurchargeLongTrain() { viewModel.addSurchargeLongTrain() }
    func setSurchargeLongTrainLength(id: String, _ v: String) {
        viewModel.setSurchargeLongTrainLength(id: id, value: v)
    }
    func setSurchargeLongTrainPercent(id: String, _ v: String) {
        viewModel.setSurchargeLongTrainPercent(id: id, value: v)
    }
    func deleteSurchargeLongTrain(id: String) { viewModel.deleteSurchargeLongTrain(id: id) }

    func addSurchargeExtendedServicePhase() { viewModel.addSurchargeExtendedServicePhase() }
    func setSurchargeExtendedServicePhaseDistance(id: String, _ v: String) {
        viewModel.setSurchargeExtendedServicePhaseDistance(id: id, value: v)
    }
    func setSurchargeExtendedServicePhasePercent(id: String, _ v: String) {
        viewModel.setSurchargeExtendedServicePhasePercent(id: id, value: v)
    }
    func deleteSurchargeExtendedServicePhase(id: String) {
        viewModel.deleteSurchargeExtendedServicePhase(id: id)
    }

    // ── Доплаты по плечам ───────────────────────────────────────────────────
    func addPhaseHeavyRow(phaseId: String) { viewModel.addPhaseHeavyRow(phaseId: phaseId) }
    func setPhaseHeavyRowThreshold(phaseId: String, rowId: String, _ v: String) {
        viewModel.setPhaseHeavyRowThreshold(phaseId: phaseId, rowId: rowId, value: v)
    }
    func setPhaseHeavyRowPercent(phaseId: String, rowId: String, _ v: String) {
        viewModel.setPhaseHeavyRowPercent(phaseId: phaseId, rowId: rowId, value: v)
    }
    func deletePhaseHeavyRow(phaseId: String, rowId: String) {
        viewModel.deletePhaseHeavyRow(phaseId: phaseId, rowId: rowId)
    }
    func addPhaseLongRow(phaseId: String) { viewModel.addPhaseLongRow(phaseId: phaseId) }
    func setPhaseLongRowThreshold(phaseId: String, rowId: String, _ v: String) {
        viewModel.setPhaseLongRowThreshold(phaseId: phaseId, rowId: rowId, value: v)
    }
    func setPhaseLongRowPercent(phaseId: String, rowId: String, _ v: String) {
        viewModel.setPhaseLongRowPercent(phaseId: phaseId, rowId: rowId, value: v)
    }
    func deletePhaseLongRow(phaseId: String, rowId: String) {
        viewModel.deletePhaseLongRow(phaseId: phaseId, rowId: rowId)
    }

    // ── Тарифная ставка / выход (§12.3) ─────────────────────────────────────
    func setDateSetTariffRate(year: Int, month: Int, day: Int) {
        viewModel.setDateSetTariffRate(year: Int32(year), month: Int32(month), day: Int32(day))
    }
    func showDialogTariffRate() { viewModel.showDialogTariffRate() }
    func hideDialogTariffRate() { viewModel.hideDialogTariffRate() }
    func isTariffRateChanged() -> Bool { viewModel.isTariffRateChanged() }
    func onBackPressed(onExit: @escaping () -> Void) {
        viewModel.onBackPressed(onExit: onExit)
    }
    func saveSettingAndOnlyMonthTariffRate(onSaved: @escaping () -> Void) {
        viewModel.saveSettingAndOnlyMonthTariffRate(onSaved: onSaved)
    }
    func saveSettingAndTariffRateCurrentAndNextMonth(onSaved: @escaping () -> Void) {
        viewModel.saveSettingAndTariffRateCurrentAndNextMonth(onSaved: onSaved)
    }
    func onScreenExit() { viewModel.onScreenExit() }
}
