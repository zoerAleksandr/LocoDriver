import SwiftUI
import ComposeApp

/// Обёртка Kotlin `ProfileIosViewModel` (SCREEN_SPECS §18) — по экземпляру на
/// открытый экран. Вся логика — в Kotlin; здесь только публикация состояния.
@MainActor
final class ProfileViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createProfileViewModel()
    /// Реферальные данные (§18.R/§19.R). Экран «Покупки» пока (§19 — отдельная
    /// задача) получает их через совместимые свойства ниже.
    private let referral = IosViewModelHelper.shared.createReferralViewModel()

    /// nil — Kotlin ещё не прислал первое состояние (заглушка «Открываем профиль...»).
    @Published var state: ProfileIosState? = nil
    /// Очередь snackbar-сообщений (§24): показываются по одному.
    @Published var messages: [DSSnackbarItem] = []

    // Совместимость с PurchasesView (§19): реферальный статус для «Покупок».
    @Published var referralCode: String? = nil
    @Published var canInvite: Bool = false
    @Published var referralCount: Int = 0
    @Published var referralRewardedCount: Int = 0
    @Published var referralError: String? = nil
    @Published var canApplyReferralCode: Bool = false
    @Published var appliedReferralCode: String? = nil
    @Published var appliedReferralStatus: String? = nil

    init() {
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
        viewModel.watchMessages { [weak self] text in
            DispatchQueue.main.async { self?.messages.append(DSSnackbarItem(text: text)) }
        }
        viewModel.watchEvents { [weak self] event in
            DispatchQueue.main.async { self?.handle(event: event) }
        }
        referral.watchState { [weak self] s in
            DispatchQueue.main.async {
                let status = s.status
                self?.referralCode = status?.code
                self?.canInvite = status?.canInvite ?? false
                self?.referralCount = Int(status?.invitedCount ?? 0)
                self?.referralRewardedCount = Int(status?.rewardedCount ?? 0)
                self?.canApplyReferralCode = status?.canApplyCode ?? false
                self?.appliedReferralCode = status?.appliedCode
                self?.appliedReferralStatus = status?.appliedStatus
                self?.referralError = s.error
            }
        }
    }

    deinit {
        viewModel.dispose()
        referral.dispose()
    }

    /// Одноразовые события Kotlin (закрыть шторку после успеха и т.п.).
    var onEvent: ((ProfileIosEvent) -> Void)?

    private func handle(event: ProfileIosEvent) {
        onEvent?(event)
    }

    func popMessage() {
        if !messages.isEmpty { messages.removeFirst() }
    }

    // MARK: - Вход / регистрация (§18.0)

    func authWithEmail(email: String, password: String) {
        viewModel.authWithEmail(email: email, password: password)
    }

    func registerByEmail(email: String, password: String) {
        viewModel.registerByEmail(email: email, password: password)
    }

    func cancelAuth() { viewModel.cancelAuth() }

    /// VK ID SDK для iOS не реализован (§18.4) — кнопки VK показывают сообщение.
    func vkNotImplemented() { viewModel.vkNotImplementedMessage() }

    func confirmVkRegistration() { viewModel.confirmVkRegistration() }
    func dismissVkRegistration() { viewModel.dismissVkRegistration() }

    // MARK: - Валидация формы (§18.0)

    func isEmailValid(_ email: String) -> Bool { viewModel.isEmailValid(email: email) }
    func isPasswordLongEnough(_ password: String) -> Bool {
        viewModel.isPasswordLongEnough(password: password)
    }
    var minPasswordLength: Int { Int(viewModel.minPasswordLength()) }

    // MARK: - Профиль (§18.1)

    func refresh() { viewModel.refresh() }
    func logOut() { viewModel.logOut() }

    // MARK: - Email / пароль (§18.4)

    func updateEmail(_ newEmail: String) { viewModel.updateEmail(newEmail: newEmail) }
    func clearUpdateEmailError() { viewModel.clearUpdateEmailError() }
    func resetUpdateEmailState() { viewModel.resetUpdateEmailState() }

    func addEmail(_ newEmail: String, password: String) {
        viewModel.addEmail(newEmail: newEmail, password: password)
    }
    func clearAddEmailError() { viewModel.clearAddEmailError() }
    func resetAddEmailState() { viewModel.resetAddEmailState() }

    func forgotRequest(_ email: String) { viewModel.forgotRequest(email: email) }

    // MARK: - VK (§18.2, §18.4)

    func removeUsersVKID() { viewModel.removeUsersVKID() }
    func clearVkLinkMessage() { viewModel.clearVkLinkMessage() }

    // MARK: - Синхронизация (§18.5)

    func startSync() { viewModel.startSync() }
    func cancelSync() { viewModel.cancelSync() }
    func resetSyncState() { viewModel.resetSyncState() }

    /// Pull-to-refresh: ждёт окончания синхронизации и ставит итог в очередь.
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

    // MARK: - Подсказка «Пригласить друга» (§6.9)

    func dismissReferralHint() { viewModel.dismissReferralHint() }

    // MARK: - Реферальные совместимые вызовы (PurchasesView, §19)

    func loadReferrals() { referral.refresh() }
    func applyReferralCode(_ code: String) { referral.applyCode(code: code) }
}
