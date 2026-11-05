import Foundation
import ComposeApp

/// Обёртка `StatisticsIosViewModel` (§13). Новый экземпляр VM на каждый экран,
/// `dispose()` в `deinit` (паттерн createX + dispose, AGENTS.md).
@MainActor
final class StatisticsViewModelWrapper: ObservableObject {
    private let viewModel: StatisticsIosViewModel

    @Published var state: StatisticsIosUiState? = nil

    init() {
        viewModel = IosViewModelHelper.shared.createStatisticsViewModel()
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
    }

    deinit {
        viewModel.dispose()
    }

    // Вкладки
    func setTab(_ tab: String) {
        viewModel.setTab(newTab: tab)
    }

    // Периоды
    func prevPeriod() { viewModel.prevPeriod() }
    func nextPeriod() { viewModel.nextPeriod() }

    // Сравнение
    func setCompare(id: String) {
        viewModel.setCompare(id: id)
    }

    func setCompareCustomMonth(year: Int, month0: Int) {
        viewModel.setCompareCustomMonth(year: Int32(year), month0: Int32(month0))
    }

    func setCompareCustomYear(year: Int) {
        viewModel.setCompareCustomYear(year: Int32(year))
    }

    // История
    func selectHistoryMetric(key: String) {
        viewModel.selectHistoryMetric(key: key)
    }

    // Детализация метрики
    func openDetail(key: String) {
        viewModel.openDetail(key: key)
    }

    func selectDetailMonth(index: Int) {
        viewModel.selectDetailMonth(index: Int32(index))
    }

    func closeDetail() {
        viewModel.closeDetail()
    }
}
