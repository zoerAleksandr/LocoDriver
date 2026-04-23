import SwiftUI
import ComposeApp

@MainActor
final class FormViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getFormViewModel()

    @Published var route: DomainRoute? = nil
    @Published var isRestAtPOPublished: Bool = false
    @Published var isFavoritePublished: Bool = false
    @Published var isOnePersonOperationPublished: Bool = false
    @Published var isLoading: Bool = false
    @Published var isSaved: Bool = false
    @Published var isDeleted: Bool = false
    @Published var errorMessage: String? = nil
    @Published var shareLink: String? = nil

    // Salary (плоские поля — не экспортируем Kotlin-nested data class напрямую)
    @Published var salaryTotal: Double? = nil
    @Published var salaryTariff: Double? = nil
    @Published var salaryNight: Double? = nil
    @Published var salaryZonal: Double? = nil
    @Published var salaryPassenger: Double? = nil
    @Published var salaryHoliday: Double? = nil
    @Published var salaryTrains: Double? = nil
    @Published var salaryOnePerson: Double? = nil
    @Published var salaryOther: Double? = nil
    @Published var salaryOverRest: Double? = nil

    // Rest
    @Published var homeRestDuration: Int64? = nil
    @Published var homeRestEnd: Int64? = nil
    @Published var shortRestDuration: Int64? = nil
    @Published var shortRestEnd: Int64? = nil
    @Published var fullRestDuration: Int64? = nil
    @Published var fullRestEnd: Int64? = nil
    @Published var currentWorkDuration: Int64? = nil
    @Published var chainWorkTotal: Int64? = nil
    @Published var chainPORestTotal: Int64? = nil
    @Published var chainSize: Int = 0
    @Published var restErrorMessage: String? = nil

    init() {
        // ViewModel — Koin single, поэтому между показами формы в нём может
        // остаться isSaved = true / isDeleted = true / shareLink от прошлой
        // сессии. Сбрасываем ДО подписки, иначе первое же значение из Flow
        // прилетит как `true` и SwiftUI onChange триггернёт dismiss().
        viewModel.resetTransientState()

        viewModel.watchRoute { [weak self] r in
            DispatchQueue.main.async {
                self?.route = r
                // Явно публикуем «вычисляемые» булевы поля, потому что SwiftUI
                // не всегда перевычисляет computed vars, зависящие от
                // reference-типа (Kotlin Route) в @Published.
                self?.isRestAtPOPublished = r?.basicData.restPointOfTurnover ?? false
                self?.isFavoritePublished = r?.basicData.isFavorite ?? false
                self?.isOnePersonOperationPublished = r?.basicData.isOnePersonOperation ?? false
            }
        }
        viewModel.watchIsLoading { [weak self] loading in
            DispatchQueue.main.async { self?.isLoading = loading.boolValue }
        }
        viewModel.watchIsSaved { [weak self] saved in
            DispatchQueue.main.async { self?.isSaved = saved.boolValue }
        }
        viewModel.watchIsDeleted { [weak self] deleted in
            DispatchQueue.main.async { self?.isDeleted = deleted.boolValue }
        }
        viewModel.watchErrorMessage { [weak self] msg in
            DispatchQueue.main.async { self?.errorMessage = msg }
        }
        viewModel.watchShareLink { [weak self] link in
            DispatchQueue.main.async { self?.shareLink = link }
        }
        viewModel.watchSalary { [weak self] s in
            DispatchQueue.main.async {
                self?.salaryTotal = s.totalPayment?.doubleValue
                self?.salaryTariff = s.paymentAtTariffRate?.doubleValue
                self?.salaryNight = s.paymentAtNightTime?.doubleValue
                self?.salaryZonal = s.zonalSurchargeMoney?.doubleValue
                self?.salaryPassenger = s.paymentAtPassengerTime?.doubleValue
                self?.salaryHoliday = s.paymentHolidayMoney?.doubleValue
                self?.salaryTrains = s.surchargesAtTrain?.doubleValue
                self?.salaryOnePerson = s.paymentAtOnePerson?.doubleValue
                self?.salaryOther = s.otherSurcharge?.doubleValue
                self?.salaryOverRest = s.overRestMoney?.doubleValue
            }
        }
        viewModel.watchRest { [weak self] r in
            DispatchQueue.main.async {
                self?.homeRestDuration = r.homeDuration?.int64Value
                self?.homeRestEnd = r.homeEndTime?.int64Value
                self?.shortRestDuration = r.shortDuration?.int64Value
                self?.shortRestEnd = r.shortEndTime?.int64Value
                self?.fullRestDuration = r.fullDuration?.int64Value
                self?.fullRestEnd = r.fullEndTime?.int64Value
                self?.currentWorkDuration = r.currentWorkDuration?.int64Value
                self?.chainWorkTotal = r.chainWorkTotal?.int64Value
                self?.chainPORestTotal = r.chainPORestTotal?.int64Value
                self?.chainSize = Int(r.chainSize)
                self?.restErrorMessage = r.errorMessage
            }
        }
    }

    // Getters — проксируют на @Published-поля, чтобы SwiftUI
    // гарантированно перечитывал их при изменении route.
    var isFavorite: Bool { isFavoritePublished }
    var isOnePersonOperation: Bool { isOnePersonOperationPublished }
    /// true — «Отдых в ПО», false — «Домашний».
    var isRestAtPO: Bool { isRestAtPOPublished }

    // Actions
    func loadRoute(id: String?) { viewModel.loadRoute(routeId: id) }
    func updateNumber(_ value: String) { viewModel.updateNumber(value: value) }
    func updateNotes(_ value: String) { viewModel.updateNotes(value: value) }
    func saveRoute() { viewModel.saveRoute() }
    func setTimeStartWork(_ ms: Int64?) { viewModel.setTimeStartWork(ms: ms.map { KotlinLong(value: $0) }) }
    func setTimeEndWork(_ ms: Int64?) { viewModel.setTimeEndWork(ms: ms.map { KotlinLong(value: $0) }) }
    func setOnePersonOperation(_ checked: Bool) { viewModel.setOnePersonOperation(checked: checked) }
    func toggleRestType() { viewModel.toggleRestType() }
    func toggleFavorite() { viewModel.toggleFavorite() }
    func shareRoute() { viewModel.shareRoute() }
    func deleteRoute() { viewModel.deleteRoute() }
    func consumeShareLink() { viewModel.consumeShareLink() }
    func consumeError() { viewModel.consumeError() }
}
