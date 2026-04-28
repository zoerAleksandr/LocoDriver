import SwiftUI
import ComposeApp

@MainActor
final class TrainFormViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getTrainFormViewModel()

    @Published var train: DomainTrain? = nil
    @Published var isSaved: Bool = false
    @Published var error: AppError? = nil

    // save в TrainFormIosViewModel пока не реализован (только setters);
    // _error на VM-стороне всегда nil. LastAction.load остаётся для
    // консистентности — Шаг 6 добавит save без правки Wrapper'а.
    private var lastAction: LastAction? = nil
    private enum LastAction {
        case load(routeId: String, trainId: String?)
    }

    init() {
        viewModel.watchTrain { [weak self] t in
            DispatchQueue.main.async { self?.train = t }
        }
        viewModel.watchIsSaved { [weak self] saved in
            DispatchQueue.main.async { self?.isSaved = saved.boolValue }
        }
        viewModel.watchError { [weak self] e in
            DispatchQueue.main.async { self?.error = e }
        }
    }

    func load(routeId: String, trainId: String?) {
        lastAction = .load(routeId: routeId, trainId: trainId)
        viewModel.loadTrain(routeId: routeId, trainId: trainId)
    }
    func setNumber(_ v: String) { viewModel.setNumber(value: v) }
    func setWeight(_ v: String) { viewModel.setWeight(value: v) }
    func setAxle(_ v: String) { viewModel.setAxle(value: v) }
    func setDistance(_ v: String) { viewModel.setDistance(value: v) }
    func setLength(_ v: String) { viewModel.setLength(value: v) }
    func setIsHeavy(_ v: Bool) { viewModel.setIsHeavy(value: v) }

    func clearError() { viewModel.clearError(); lastAction = nil }
    func retry() {
        guard let a = lastAction else { return }
        switch a {
        case .load(let r, let t): load(routeId: r, trainId: t)
        }
    }
}
