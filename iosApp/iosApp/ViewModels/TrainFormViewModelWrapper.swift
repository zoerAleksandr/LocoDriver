import SwiftUI
import ComposeApp

@MainActor
final class TrainFormViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getTrainFormViewModel()

    @Published var train: Train? = nil
    @Published var isSaved: Bool = false

    init() {
        viewModel.watchTrain { [weak self] t in
            DispatchQueue.main.async { self?.train = t }
        }
        viewModel.watchIsSaved { [weak self] saved in
            DispatchQueue.main.async { self?.isSaved = saved.boolValue }
        }
    }

    func load(routeId: String, trainId: String?) { viewModel.loadTrain(routeId: routeId, trainId: trainId) }
    func setNumber(_ v: String) { viewModel.setNumber(value: v) }
    func setWeight(_ v: String) { viewModel.setWeight(value: v) }
    func setAxle(_ v: String) { viewModel.setAxle(value: v) }
    func setDistance(_ v: String) { viewModel.setDistance(value: v) }
    func setLength(_ v: String) { viewModel.setLength(value: v) }
    func setIsHeavy(_ v: Bool) { viewModel.setIsHeavy(value: v) }
}
