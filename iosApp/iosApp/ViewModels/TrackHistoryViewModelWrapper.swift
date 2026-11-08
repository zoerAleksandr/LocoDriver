import Foundation
import SwiftUI
import ComposeApp

/// Обёртка Kotlin `TrackHistoryIosViewModel` (SCREEN_SPECS §17.8) — по экземпляру
/// на открытый экран. Список/сводки/сервер — в Kotlin; здесь публикация состояния.
@MainActor
final class TrackHistoryViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createTrackHistoryViewModel()

    /// nil — Kotlin ещё не прислал первое состояние.
    @Published var state: TrackHistoryState? = nil
    /// Очередь snackbar-сообщений (§24): показываются по одному.
    @Published var messages: [DSSnackbarItem] = []

    init() {
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
        viewModel.watchMessages { [weak self] text in
            DispatchQueue.main.async { self?.messages.append(DSSnackbarItem(text: text)) }
        }
    }

    deinit { viewModel.dispose() }

    /// Открытие экрана: сироты, локальный список, серверная часть (§17.8).
    func onScreenOpened() { viewModel.onScreenOpened() }
    func refresh() { viewModel.refresh() }
    func loadMore() { viewModel.loadMore() }
    func toggleFavorite(_ card: TrackHistoryCard) { viewModel.toggleFavorite(card: card) }
    func delete(_ card: TrackHistoryCard) { viewModel.delete(card: card) }
    func deleteAllHistory() { viewModel.deleteAllHistory() }

    func popMessage() {
        if !messages.isEmpty { messages.removeFirst() }
    }
}
