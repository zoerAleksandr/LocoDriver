import SwiftUI
import ComposeApp

@MainActor
final class SettingsViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getSettingsViewModel()

    @Published var settings: DomainUserSettings? = nil
    @Published var isLoading: Bool = true

    /// Токены подписок watchX. Отменяются в deinit.
    private var watchHandles: [WatchHandle] = []

    init() {
        watchHandles.append(viewModel.watchSettings { [weak self] s in
            DispatchQueue.main.async { self?.settings = s }
        })
        watchHandles.append(viewModel.watchIsLoading { [weak self] loading in
            DispatchQueue.main.async { self?.isLoading = loading.boolValue }
        })
    }

    deinit {
        watchHandles.forEach { $0.cancel() }
    }

    // MARK: - Route toggles
    func setUsingDefaultWorkTime(_ value: Bool) { viewModel.setUsingDefaultWorkTime(value: value) }
    func setConsiderFutureRoute(_ value: Bool)   { viewModel.setConsiderFutureRoute(value: value) }
    func setShowBreak(_ value: Bool)             { viewModel.setShowBreak(value: value) }

    // MARK: - Default work time (millis)
    func setDefaultWorkTime(_ millis: Int64)     { viewModel.setDefaultWorkTime(millis: millis) }

    // MARK: - Norma hours (whole hours → millis conversion done in VM)
    func setNormaHours(_ hours: Int64)           { viewModel.setNormaHours(hours: hours) }

    // MARK: - Night time
    func setNightStartTime(hour: Int32, minute: Int32) {
        viewModel.setNightStartTime(hour: hour, minute: minute)
    }
    func setNightEndTime(hour: Int32, minute: Int32) {
        viewModel.setNightEndTime(hour: hour, minute: minute)
    }

    // MARK: - Rest times (millis)
    func setMinTimeRest(_ millis: Int64)     { viewModel.setMinTimeRest(millis: millis) }
    func setMinTimeHomeRest(_ millis: Int64) { viewModel.setMinTimeHomeRest(millis: millis) }

    // MARK: - Loco display flags
    func setShowLocoHeating(_ value: Bool)    { viewModel.setShowLocoHeating(value: value) }
    func setShowLocoAuxiliary(_ value: Bool)  { viewModel.setShowLocoAuxiliary(value: value) }
    func setShowLocoStatistics(_ value: Bool) { viewModel.setShowLocoStatistics(value: value) }
    func setShowLocoNorma(_ value: Bool)      { viewModel.setShowLocoNorma(value: value) }
    func setShowOtherCurrent(_ value: Bool)   { viewModel.setShowOtherCurrent(value: value) }
}
