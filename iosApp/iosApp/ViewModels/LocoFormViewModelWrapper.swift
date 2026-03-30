import SwiftUI
import ComposeApp

@MainActor
final class LocoFormViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getLocoFormViewModel()

    @Published var loco: Locomotive? = nil
    @Published var isSaved: Bool = false

    init() {
        viewModel.watchLoco { [weak self] l in
            DispatchQueue.main.async { self?.loco = l }
        }
        viewModel.watchIsSaved { [weak self] saved in
            DispatchQueue.main.async { self?.isSaved = saved.boolValue }
        }
    }

    func load(routeId: String, locoId: String?) { viewModel.loadLoco(routeId: routeId, locoId: locoId) }
    func setSeries(_ v: String) { viewModel.setSeries(value: v) }
    func setNumber(_ v: String) { viewModel.setNumber(value: v) }
    func setType(_ t: LocoType) { viewModel.setType(type: t) }
    func setTimeStartAcceptance(_ ms: Int64?) { viewModel.setTimeStartAcceptance(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeEndAcceptance(_ ms: Int64?) { viewModel.setTimeEndAcceptance(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeStartDelivery(_ ms: Int64?) { viewModel.setTimeStartDelivery(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeEndDelivery(_ ms: Int64?) { viewModel.setTimeEndDelivery(ms: ms.map { KotlinLong(value: $0) }) }
}
