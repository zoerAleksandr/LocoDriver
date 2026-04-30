import SwiftUI
import ComposeApp

@MainActor
final class LocoFormViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getLocoFormViewModel()

    @Published var loco: DomainLocomotive? = nil
    @Published var isSaved: Bool = false
    @Published var error: AppError? = nil

    private var lastAction: LastAction? = nil
    private enum LastAction {
        case load(routeId: String, locoId: String?)
        case save
    }

    /// Токены подписок watchX. Отменяются в deinit.
    private var watchHandles: [WatchHandle] = []

    init() {
        watchHandles.append(viewModel.watchLoco { [weak self] l in
            DispatchQueue.main.async { self?.loco = l }
        })
        watchHandles.append(viewModel.watchIsSaved { [weak self] saved in
            DispatchQueue.main.async { self?.isSaved = saved.boolValue }
        })
        watchHandles.append(viewModel.watchError { [weak self] e in
            DispatchQueue.main.async { self?.error = e }
        })
    }

    deinit {
        watchHandles.forEach { $0.cancel() }
    }

    func load(routeId: String, locoId: String?) {
        lastAction = .load(routeId: routeId, locoId: locoId)
        viewModel.loadLoco(routeId: routeId, locoId: locoId)
    }
    func setSeries(_ v: String) { viewModel.setSeries(value: v) }
    func setNumber(_ v: String) { viewModel.setNumber(value: v) }
    func setType(_ t: DomainLocoType) { viewModel.setType(type: t) }
    func setTimeStartAcceptance(_ ms: Int64?) { viewModel.setTimeStartAcceptance(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeEndAcceptance(_ ms: Int64?) { viewModel.setTimeEndAcceptance(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeStartDelivery(_ ms: Int64?) { viewModel.setTimeStartDelivery(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeEndDelivery(_ ms: Int64?) { viewModel.setTimeEndDelivery(ms: ms.map { KotlinLong(value: $0) }) }
    func addSection() { viewModel.addSection() }
    func saveLoco() {
        lastAction = .save
        viewModel.saveLoco()
    }

    func clearError() { viewModel.clearError(); lastAction = nil }
    func retry() {
        guard let a = lastAction else { return }
        switch a {
        case .load(let r, let l): load(routeId: r, locoId: l)
        case .save: saveLoco()
        }
    }
}
