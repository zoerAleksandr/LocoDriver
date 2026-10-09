import SwiftUI
import ComposeApp

/// Обёртка `HomeScreenIosViewModel` (SCREEN_SPECS §4) для SwiftUI.
/// Все расчёты и форматирование — в Kotlin; здесь только публикация состояния.
@MainActor
final class HomeScreenViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getHomeScreenViewModel()

    /// nil — Kotlin ещё не прислал первое состояние (показывается скелетон).
    @Published var ui: HomeIosScreenUi? = nil
    @Published var live: HomeIosLive? = nil
    @Published var syncDialog: HomeIosSyncDialog? = nil
    /// Очередь snackbar-сообщений: показываются по одному, не перебивая друг друга (§4.4).
    @Published var messages: [DSSnackbarItem] = []

    init() {
        viewModel.watchUi { [weak self] value in
            DispatchQueue.main.async { self?.ui = value }
        }
        viewModel.watchLive { [weak self] value in
            DispatchQueue.main.async { self?.live = value }
        }
        viewModel.watchSyncDialog { [weak self] value in
            DispatchQueue.main.async { self?.syncDialog = value }
        }
        viewModel.watchMessages { [weak self] message in
            DispatchQueue.main.async { self?.messages.append(DSSnackbarItem(text: message)) }
        }
    }

    func onAppear() { viewModel.onScreenAppear() }
    func onDisappear() { viewModel.onScreenDisappear() }

    func previousMonth() { viewModel.previousMonth() }
    func nextMonth() { viewModel.nextMonth() }

    func setMonth(year: Int, month: Int) {
        viewModel.setCurrentMonth(year: Int32(year), month: Int32(month))
    }

    func dismissNotice(_ key: String) { viewModel.dismissNotice(dismissKey: key) }

    func removeRoute(basicId: String) { viewModel.removeRoute(basicId: basicId) }

    func removeRouteSubtitle(basicId: String) -> String {
        viewModel.removeRouteSubtitle(basicId: basicId)
    }

    func manualSync() { viewModel.manualSync() }
    func resetSyncState() { viewModel.resetSyncState() }

    /// Pull-to-refresh: ждёт окончания синхронизации и ставит итоговое сообщение в очередь.
    func pullToSync() async {
        let message: String? = await withCheckedContinuation { continuation in
            viewModel.pullToSync { text in
                continuation.resume(returning: text)
            }
        }
        if let message = message {
            messages.append(DSSnackbarItem(text: message))
        }
    }

    func popMessage() {
        if !messages.isEmpty { messages.removeFirst() }
    }
}
