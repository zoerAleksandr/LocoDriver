import SwiftUI
import ComposeApp

// Обёртки Kotlin iOS-ViewModel экранов «Все маршруты» (§9) и смежных:
// быстрый просмотр (§9.1), корзина (§9.4), поиск (§10).
// Все расчёты/форматирование — в Kotlin; здесь только публикация состояния
// и проброс вызовов. Паттерн — см. AGENTS.md «Паттерн SwiftUI ↔ Kotlin ViewModel».
// Типы: Kotlin Boolean → Swift Bool (без .boolValue), Int → Int32 (обернуть в Int(...)),
// Long? → KotlinLong(value:), List<Int> → [KotlinInt] (через .int32Value).

// MARK: - Все маршруты (§9)

@MainActor
final class AllRoutesViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createAllRoutesViewModel()

    /// nil — Kotlin ещё не прислал первое состояние.
    @Published var ui: AllRoutesUi? = nil
    /// Очередь snackbar-сообщений (показываются по одному).
    @Published var messages: [DSSnackbarItem] = []
    /// Полезная нагрузка share-sheet; `nil` — нет.
    @Published var share: SharePayloadUi? = nil

    init() {
        viewModel.watchUi { [weak self] value in
            DispatchQueue.main.async { self?.ui = value }
        }
        viewModel.watchMessages { [weak self] message in
            DispatchQueue.main.async { self?.messages.append(DSSnackbarItem(text: message)) }
        }
        viewModel.watchShare { [weak self] payload in
            DispatchQueue.main.async { self?.share = payload }
        }
    }

    deinit { viewModel.dispose() }

    // ── Удобные проекции состояния ─────────────────────────────────────────
    var months: [Int] { (ui?.monthOptions ?? []).map { Int($0.int32Value) } }
    var years: [Int] { (ui?.yearOptions ?? []).map { Int($0.int32Value) } }
    /// Имя текущей сортировки (`SortOption.name`, напр. «DATE_DESC») — для чипа и шторки.
    var sortName: String { ui?.sortOption.name ?? "DATE_DESC" }

    private static let monthNames = [
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
    ]
    /// Текущий месяц (0-based) — для шторки выбора месяца.
    var selectedMonth: Int {
        Self.monthNames.firstIndex(of: ui?.monthTitle ?? "") ?? 0
    }
    /// Текущий год — для шторки выбора месяца.
    var selectedYear: Int { Int(ui?.yearTitle ?? "") ?? 2026 }

    // ── Месяц ─────────────────────────────────────────────────────────────
    func prevMonth() { viewModel.prevMonth() }
    func nextMonth() { viewModel.nextMonth() }
    func setCurrentMonth(year: Int, month: Int) {
        viewModel.setCurrentMonth(year: Int32(year), month: Int32(month))
    }

    // ── Фильтры / сортировка / вид ────────────────────────────────────────
    func toggleFilter(name: String) { viewModel.toggleFilter(name: name) }
    func setSort(name: String) { viewModel.setSort(name: name) }
    func toggleExpandedView() { viewModel.toggleExpandedView() }
    @discardableResult
    func toggleShowTurnaroundRest() -> Bool { viewModel.toggleShowTurnaroundRest() }
    func reload() { viewModel.reload() }

    // ── Множественный выбор (§9.2) ────────────────────────────────────────
    func enterSelectionMode() { viewModel.enterSelectionMode() }
    func exitSelectionMode() { viewModel.exitSelectionMode() }
    func toggleRouteSelection(routeId: String) { viewModel.toggleRouteSelection(routeId: routeId) }
    func toggleSelectAll() { viewModel.toggleSelectAll() }
    func toggleFavoriteSelectedRoutes() { viewModel.toggleFavoriteSelectedRoutes() }
    func deleteSelectedRoutes() { viewModel.deleteSelectedRoutes() }
    func shareSelectedRoutes() { viewModel.shareSelectedRoutes() }

    // ── Действия по маршруту ──────────────────────────────────────────────
    func setFavoriteRoute(routeId: String) { viewModel.setFavoriteRoute(routeId: routeId) }
    func deleteRoute(routeId: String) { viewModel.deleteRoute(routeId: routeId) }
    func shareRoute(routeId: String) { viewModel.shareRoute(routeId: routeId) }
    func syncRoute(routeId: String) { viewModel.syncRoute(routeId: routeId) }
    func onCopyRoute(routeId: String, callback: @escaping (AllRoutesFormDecision) -> Void) {
        viewModel.onCopyRoute(routeId: routeId) { decision in
            DispatchQueue.main.async { callback(decision) }
        }
    }
    func onNewRouteClick(callback: @escaping (AllRoutesFormDecision) -> Void) {
        viewModel.onNewRouteClick { decision in
            DispatchQueue.main.async { callback(decision) }
        }
    }
    func hasRecordedTracks(routeId: String) -> Bool { viewModel.hasRecordedTracks(routeId: routeId) }

    // ── Синхронизация ─────────────────────────────────────────────────────
    func syncOnScreenOpen() { viewModel.syncOnScreenOpen() }
    func stopScreenSync() { viewModel.stopScreenSync() }
    func pullToRefresh() { viewModel.pullToRefresh() }

    // ── Сообщения ─────────────────────────────────────────────────────────
    func popMessage() {
        if !messages.isEmpty { messages.removeFirst() }
    }
}

// MARK: - Быстрый просмотр маршрута (§9.1)

/// Только данные для отображения (§9.1: «Данные — только чтение»).
/// Действия нижней панели выполняет вызывающий экран через замыкания.
@MainActor
final class RouteQuickViewViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createRouteQuickViewModel()

    @Published var ui: RouteQuickViewUi? = nil

    init() {
        viewModel.watchQuickView { [weak self] value in
            DispatchQueue.main.async { self?.ui = value }
        }
    }

    deinit { viewModel.dispose() }

    func show(routeId: String) {
        ui = nil
        viewModel.show(routeId: routeId)
    }

    func refresh() { viewModel.refresh() }
}

// MARK: - Корзина маршрутов (§9.4)

@MainActor
final class TrashViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createTrashViewModel()

    @Published var ui: TrashUi? = nil
    @Published var messages: [DSSnackbarItem] = []

    init() {
        viewModel.watchUi { [weak self] value in
            DispatchQueue.main.async { self?.ui = value }
        }
        viewModel.watchMessages { [weak self] message in
            DispatchQueue.main.async { self?.messages.append(DSSnackbarItem(text: message)) }
        }
    }

    deinit { viewModel.dispose() }

    func onScreenOpened() { viewModel.onScreenOpened() }
    func refresh() { viewModel.refresh() }
    func restore(routeId: String) { viewModel.restore(routeId: routeId) }
    func restoreAll() { viewModel.restoreAll() }
    func emptyTrash() { viewModel.emptyTrash() }
    func emptyTrashConfirmText(count: Int) -> String {
        viewModel.emptyTrashConfirmText(count: Int32(count))
    }
    func purgeTracksNote() -> String? { viewModel.purgeTracksNote() }
    func hasRecordedTracksForPurgeable() -> Bool { viewModel.hasRecordedTracksForPurgeable() }

    func popMessage() {
        if !messages.isEmpty { messages.removeFirst() }
    }
}

// MARK: - Поиск (§10)

@MainActor
final class SearchViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createSearchViewModel()

    @Published var ui: SearchIosUi? = nil

    init() {
        viewModel.watchUi { [weak self] value in
            DispatchQueue.main.async { self?.ui = value }
        }
    }

    deinit { viewModel.dispose() }

    /// Ввод с дебаунсом 200 мс (§10.2) — дебаунс внутри Kotlin-VM.
    func setQueryValue(_ value: String) { viewModel.setQueryValue(value: value) }
    /// Немедленный полный поиск («Поиск», тап по подсказке/истории).
    func onSearch() { viewModel.onSearch() }
    func applySuggestion(_ suggestion: String) { viewModel.applySuggestion(suggestion: suggestion) }
    func searchHistoryItem(_ text: String) { viewModel.searchHistoryItem(text: text) }
    func removeHistoryItem(_ text: String) { viewModel.removeHistoryItem(text: text) }
    func setFilterEnabled(key: String, enabled: Bool) {
        viewModel.setFilterEnabled(key: key, enabled: enabled)
    }
    func setPeriodStart(_ ms: Int64?) {
        viewModel.setPeriodStart(ms: ms.map { KotlinLong(value: $0) })
    }
    func setPeriodEnd(_ ms: Int64?) {
        viewModel.setPeriodEnd(ms: ms.map { KotlinLong(value: $0) })
    }
    func clearFilter() { viewModel.clearFilter() }
    func closeSettings() { viewModel.closeSettings() }
}
