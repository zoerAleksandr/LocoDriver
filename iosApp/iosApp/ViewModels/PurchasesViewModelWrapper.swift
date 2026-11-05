import SwiftUI
import ComposeApp

/// Модель тарифа для Swift (зеркало `PurchasesPlanUi`).
struct PurchasesPlanModel: Identifiable, Equatable {
    var id: String { code }
    var code: String = ""
    var name: String = ""
    var desc: String = ""
    var sum: Double = 0
    var basePrice: Double = 0
    var discountPercent: Int = 0
    var discountActive: Bool = false
    var discountUntil: Int64 = 0
    var periodDays: Int = 0
    var monthsCount: Int = 1
    var perMonthRub: Int = 0
    var benefitPercent: Int = 0
}

/// Состояние экрана «Покупки» для Swift (зеркало `PurchasesIosState`).
struct PurchasesStateModel: Equatable {
    var isLoadingProducts: Bool = true
    var isSubscriptionLoaded: Bool = false
    var subscriptionKind: String = "loading"
    var subscriptionEndTime: Int64 = 0
    var subscriptionEndText: String = ""
    var daysLeft: Int = 0
    var remainingBonusDays: Int = 0
    var products: [PurchasesPlanModel] = []
    var selectedCode: String = ""
    var isStartingPayment: Bool = false

    var autoRenewEnabled: Bool = false
    var autoRenewAvailable: Bool = false
    var autoRenewChecked: Bool = false
    var autoRenewConsentBefore: String = ""
    var autoRenewConsentLinkWord: String = ""
    var autoRenewConsentAfter: String = ""
    var autoRenewOfferUrl: String = ""
    var autoRenewConsentVersion: String = ""
    var autoRenewNextChargeText: String = ""
    var autoRenewLastChargeFailed: Bool = false
    var autoRenewDisabledReason: String = ""
    var autoRenewTermsAmountText: String = ""
    var autoRenewRenewalAmountText: String = ""
    var isDisablingAutoRenew: Bool = false
    var autoRenewConfirmVisible: Bool = false
    var autoRenewConfirmDateText: String = ""

    var canApplyReferral: Bool = false
    var appliedReferralCode: String = ""
    var appliedReferralStatus: String = ""
    var referralMessage: String = ""
    var referralMessageIsError: Bool = false
    var isApplyingReferral: Bool = false

    var paymentDialogKind: String = ""

    var ctaLabel: String = ""
    var ctaCaption: String = ""
    var renewalPreviewTitle: String = ""
    var renewalPreviewAddText: String = ""
    var renewalPreviewResultText: String = ""

    init() {}

    init(_ state: PurchasesIosState) {
        isLoadingProducts = state.isLoadingProducts
        isSubscriptionLoaded = state.isSubscriptionLoaded
        subscriptionKind = state.subscriptionKind
        subscriptionEndTime = state.subscriptionEndTime
        subscriptionEndText = state.subscriptionEndText
        daysLeft = Int(state.daysLeft)
        remainingBonusDays = Int(state.remainingBonusDays)
        products = state.products.map { plan in
            PurchasesPlanModel(
                code: plan.code,
                name: plan.name,
                desc: plan.desc,
                sum: plan.sum,
                basePrice: plan.basePrice,
                discountPercent: Int(plan.discountPercent),
                discountActive: plan.discountActive,
                discountUntil: plan.discountUntil,
                periodDays: Int(plan.periodDays),
                monthsCount: Int(plan.monthsCount),
                perMonthRub: Int(plan.perMonthRub),
                benefitPercent: Int(plan.benefitPercent)
            )
        }
        selectedCode = state.selectedCode
        isStartingPayment = state.isStartingPayment

        autoRenewEnabled = state.autoRenewEnabled
        autoRenewAvailable = state.autoRenewAvailable
        autoRenewChecked = state.autoRenewChecked
        autoRenewConsentBefore = state.autoRenewConsentBefore
        autoRenewConsentLinkWord = state.autoRenewConsentLinkWord
        autoRenewConsentAfter = state.autoRenewConsentAfter
        autoRenewOfferUrl = state.autoRenewOfferUrl
        autoRenewConsentVersion = state.autoRenewConsentVersion
        autoRenewNextChargeText = state.autoRenewNextChargeText
        autoRenewLastChargeFailed = state.autoRenewLastChargeFailed
        autoRenewDisabledReason = state.autoRenewDisabledReason
        autoRenewTermsAmountText = state.autoRenewTermsAmountText
        autoRenewRenewalAmountText = state.autoRenewRenewalAmountText
        isDisablingAutoRenew = state.isDisablingAutoRenew
        autoRenewConfirmVisible = state.autoRenewConfirmVisible
        autoRenewConfirmDateText = state.autoRenewConfirmDateText

        canApplyReferral = state.canApplyReferral
        appliedReferralCode = state.appliedReferralCode
        appliedReferralStatus = state.appliedReferralStatus
        referralMessage = state.referralMessage
        referralMessageIsError = state.referralMessageIsError
        isApplyingReferral = state.isApplyingReferral

        paymentDialogKind = state.paymentDialogKind

        ctaLabel = state.ctaLabel
        ctaCaption = state.ctaCaption
        renewalPreviewTitle = state.renewalPreviewTitle
        renewalPreviewAddText = state.renewalPreviewAddText
        renewalPreviewResultText = state.renewalPreviewResultText
    }
}

/// Обёртка `PurchasesIosViewModel` (SCREEN_SPECS §19): состояние экрана,
/// сообщения, открытие страницы оплаты и глобальные диалоги §19.5–19.6.
/// Shared: VM — single, глобальные диалоги живут на уровне приложения.
@MainActor
final class PurchasesViewModelWrapper: ObservableObject {
    static let shared = PurchasesViewModelWrapper()
    private let viewModel = IosViewModelHelper.shared.getPurchasesViewModel()

    @Published var state = PurchasesStateModel()
    @Published var message: String? = nil
    @Published var paymentUrl: String? = nil
    @Published var globalNotice: PurchasesGlobalNoticeUi? = nil

    init() {
        viewModel.watchState { [weak self] value in
            DispatchQueue.main.async { self?.state = PurchasesStateModel(value) }
        }
        viewModel.watchMessages { [weak self] value in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.message = value
                if value != nil {
                    // Сбрасываем в Kotlin через паузу: иначе одинаковые сообщения
                    // подряд не дойдут (StateFlow дедуплицирует значение).
                    DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                        self.viewModel.consumeMessage()
                    }
                }
            }
        }
        viewModel.watchOpenPaymentUrl { [weak self] value in
            DispatchQueue.main.async { self?.paymentUrl = value }
        }
        viewModel.watchGlobalNotice { [weak self] value in
            DispatchQueue.main.async { self?.globalNotice = value }
        }
    }

    // MARK: - Экран

    func selectPlan(code: String) { viewModel.onPlanSelected(code: code) }
    func setAutoRenewChecked(_ checked: Bool) { viewModel.setAutoRenewChecked(checked: checked) }
    func startPayment() { viewModel.startPayment() }
    func restorePurchases() { viewModel.restorePurchases() }
    func applyReferralCode(_ code: String) { viewModel.applyReferralCode(code: code) }
    func refreshRecurringStatus() { viewModel.refreshRecurringStatus() }

    func pullToRefresh() async {
        viewModel.pullToRefresh()
        // pullToRefresh работает в viewModelScope без callback — даём UI короткую паузу.
        try? await Task.sleep(nanoseconds: 900_000_000)
    }

    /// Закрыли страницу оплаты / вернулись в приложение (§19.6).
    func checkPayment() { viewModel.checkPaymentOnServer() }
    func dismissPaymentDialog() { viewModel.dismissPaymentDialog() }

    func showAutoRenewConfirm() { viewModel.showAutoRenewConfirm() }
    func hideAutoRenewConfirm() { viewModel.hideAutoRenewConfirm() }
    func disableAutoRenew() { viewModel.disableAutoRenew() }

    /// Проверка ожидаемой оплаты и глобальных диалогов при возврате в приложение.
    func onAppResumed() { viewModel.onAppResumed() }

    func dismissGlobalNotice() { viewModel.dismissGlobalNotice() }

    func consumePaymentUrl() {
        paymentUrl = nil
        viewModel.consumeOpenPaymentUrl()
    }

    /// Свободные маршруты для «Пробного периода» и карточек Главной (§19.1).
    static let freeRoutesLimit: Int = 20
}
