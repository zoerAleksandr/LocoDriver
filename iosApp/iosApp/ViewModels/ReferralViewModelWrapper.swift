import SwiftUI
import ComposeApp

/// Обёртка Kotlin `ReferralIosViewModel` (SCREEN_SPECS §18.R) — по экземпляру
/// на открытый экран; `deinit` зовёт `dispose()`.
@MainActor
final class ReferralViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createReferralViewModel()

    @Published var status: ReferralStatusResponse? = nil
    @Published var loading: Bool = false
    @Published var error: String? = nil
    @Published var messages: [DSSnackbarItem] = []

    init() {
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async {
                self?.status = s.status
                self?.loading = s.loading
                self?.error = s.error
            }
        }
        viewModel.watchMessages { [weak self] text in
            DispatchQueue.main.async { self?.messages.append(DSSnackbarItem(text: text)) }
        }
    }

    deinit { viewModel.dispose() }

    /// Запрос выполняется при каждом открытии экрана (§18.R).
    func refresh() { viewModel.refresh() }

    func applyCode(_ code: String) { viewModel.applyCode(code: code) }

    func popMessage() {
        if !messages.isEmpty { messages.removeFirst() }
    }

    // MARK: - Поля статуса (§18.R)

    var code: String? { status?.code }
    /// Приглашать может только тот, кто уже хотя бы раз оплачивал Про.
    var canInvite: Bool { status?.canInvite ?? false }
    /// «Приглашено» = rewardedCount (только оплатившие друзья, НЕ invitedCount, §18.R).
    var invitedRewardedCount: Int { Int(status?.rewardedCount ?? 0) }
    /// «Начислено» = awardedDays (исторический счётчик).
    var awardedDays: Int { Int(status?.awardedDays ?? 0) }
}
