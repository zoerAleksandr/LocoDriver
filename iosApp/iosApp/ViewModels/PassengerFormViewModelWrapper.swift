import SwiftUI
import ComposeApp

@MainActor
final class PassengerFormViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getPassengerFormViewModel()

    @Published var passenger: DomainPassenger? = nil
    @Published var isSaved: Bool = false
    @Published var isLoading: Bool = false
    @Published var error: AppError? = nil

    private var lastAction: LastAction? = nil
    private enum LastAction {
        case load(routeId: String, passengerId: String?)
        case save
    }

    init() {
        viewModel.watchPassenger { [weak self] p in
            DispatchQueue.main.async { self?.passenger = p }
        }
        viewModel.watchIsSaved { [weak self] saved in
            DispatchQueue.main.async { self?.isSaved = saved.boolValue }
        }
        viewModel.watchIsLoading { [weak self] loading in
            DispatchQueue.main.async { self?.isLoading = loading.boolValue }
        }
        viewModel.watchError { [weak self] e in
            DispatchQueue.main.async { self?.error = e }
        }
    }

    func load(routeId: String, passengerId: String?) {
        lastAction = .load(routeId: routeId, passengerId: passengerId)
        viewModel.loadPassenger(routeId: routeId, passengerId: passengerId)
    }

    func setTrainNumber(_ v: String) { viewModel.setTrainNumber(value: v) }
    func setDepartureStation(_ v: String) { viewModel.setDepartureStation(value: v) }
    func setArrivalStation(_ v: String) { viewModel.setArrivalStation(value: v) }
    func setTimeDeparture(_ ms: Int64?) { viewModel.setTimeDeparture(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeArrival(_ ms: Int64?) { viewModel.setTimeArrival(ms: ms.map { KotlinLong(value: $0) }) }
    func setNotes(_ v: String) { viewModel.setNotes(value: v) }
    func savePassenger() {
        lastAction = .save
        viewModel.savePassenger()
    }

    func clearError() { viewModel.clearError(); lastAction = nil }
    func retry() {
        guard let a = lastAction else { return }
        switch a {
        case .load(let r, let p): load(routeId: r, passengerId: p)
        case .save: savePassenger()
        }
    }
}
