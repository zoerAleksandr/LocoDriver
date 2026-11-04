import SwiftUI
import ComposeApp

/// Обёртка Kotlin `SettingsScreenIosViewModel` (SCREEN_SPECS §17): по экземпляру на открытый экран.
/// Правки уходят в Kotlin сразу, запись в БД — с дебаунсом 300 мс (§17 «Сохранение»).
@MainActor
final class SettingsScreenViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createSettingsScreenViewModel()

    @Published var state: SettingsScreenState? = nil

    init() {
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
    }

    /// Принудительное сохранение при уходе с экрана (аналог onCleared).
    func flush() { viewModel.flushPending() }

    var settings: DomainUserSettings? { state?.settings }

    // Основные
    func setDefaultLocoType(electric: Bool) { viewModel.setDefaultLocoType(electric: electric) }
    func setDecimalTime(_ v: Bool) { viewModel.setDecimalTime(value: v) }
    func setUsingDefaultWorkTime(_ v: Bool) { viewModel.setUsingDefaultWorkTime(value: v) }
    func setDefaultWorkTime(millis: Int64) { viewModel.setDefaultWorkTime(millis: millis) }
    func setUseStandardTimePicker(_ v: Bool) { viewModel.setUseStandardTimePicker(value: v) }
    func setThemeMode(_ v: String) { viewModel.setThemeMode(mode: v) }
    func setPassenger12h(_ v: String) { viewModel.setPassenger12h(option: v) }

    // Маршрут
    func setShowBreak(_ v: Bool) { viewModel.setShowBreak(v: v) }
    func setShowOnePerson(_ v: Bool) { viewModel.setShowOnePerson(v: v) }
    func setShowPartner(_ v: Bool) { viewModel.setShowPartner(v: v) }
    func setShowLocomotive(_ v: Bool) { viewModel.setShowLocomotive(v: v) }
    func setShowTrain(_ v: Bool) { viewModel.setShowTrain(v: v) }
    func setShowPassenger(_ v: Bool) { viewModel.setShowPassenger(v: v) }
    func setShowOtherWork(_ v: Bool) { viewModel.setShowOtherWork(v: v) }

    // Локомотив
    func setShowLocoHeating(_ v: Bool) { viewModel.setShowLocoHeating(v: v) }
    func setShowLocoAuxiliary(_ v: Bool) { viewModel.setShowLocoAuxiliary(v: v) }
    func setShowOtherCurrent(_ v: Bool) { viewModel.setShowOtherCurrent(v: v) }
    func setConsiderHeatingInTotal(_ v: Bool) { viewModel.setConsiderHeatingInTotal(v: v) }
    func setConsiderAuxiliaryInTotal(_ v: Bool) { viewModel.setConsiderAuxiliaryInTotal(v: v) }

    // Поезд
    func setPassengerWagonLength(_ meters: Double) { viewModel.setPassengerWagonLength(meters: meters) }

    // Учёт
    func setNightTime(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        viewModel.setNightTime(startHour: Int32(startHour), startMinute: Int32(startMinute),
                               endHour: Int32(endHour), endMinute: Int32(endMinute))
    }
    func setConsiderFutureRoute(_ v: Bool) { viewModel.setConsiderFutureRoute(v: v) }

    // Отдых
    func setMinTimeRest(millis: Int64) { viewModel.setMinTimeRest(millis: millis) }
    func setMinTimeRestSecond(millis: Int64) { viewModel.setMinTimeRestSecond(millis: millis) }
    func setMinTimeHomeRest(millis: Int64) { viewModel.setMinTimeHomeRest(millis: millis) }

    // Норма / регион
    func setTimeZone(millis: Int64) { viewModel.setTimeZone(millis: millis) }
    func setCrossMonthLocal(_ local: Bool) { viewModel.setCrossMonthTimezone(local: local) }
    func setCountry(_ code: String) { viewModel.setCountry(country: code) }

    // Плечи
    var phases: [DomainServicePhase] { (settings?.servicePhases as? [DomainServicePhase]) ?? [] }
    func savePhase(_ base: DomainServicePhase?, from: String, to: String, distance: Int, rate: Double, alsoReverse: Bool) {
        let phase: DomainServicePhase
        if let base = base {
            phase = viewModel.editedPhase(base: base, from: from, to: to, distance: Int32(distance), rate: rate)
        } else {
            phase = viewModel.newPhase(from: from, to: to, distance: Int32(distance), rate: rate)
        }
        viewModel.savePhase(phase: phase, alsoReverse: alsoReverse)
    }
    func deletePhase(id: String) { viewModel.deletePhase(id: id) }
}
