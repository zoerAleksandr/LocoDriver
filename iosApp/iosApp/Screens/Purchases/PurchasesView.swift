import SwiftUI
import ComposeApp

/// Экран «Покупки» / подписка «Машинист Pro» (SCREEN_SPECS §19.2).
/// Макет — `design/src/purchase-screens.jsx` → `IOSPaywallA` (вариант, ближайший
/// к Android: hero + преимущества + список тарифов + закреплённая CTA).
/// Логика — Kotlin `PurchasesIosViewModel`; здесь только показ и открытие оплаты.
struct PurchasesView: View {
    @ObservedObject private var vm = PurchasesViewModelWrapper.shared
    @ObservedObject private var router = AppRouter.shared
    @State private var referralCode = ""
    @State private var safariItem: SafariItem? = nil
    @State private var snackbars: [DSSnackbarItem] = []

    var body: some View {
        VStack(spacing: 0) {
            topBar
            ScrollView {
                VStack(alignment: .leading, spacing: DSSpacing.lg) {
                    heroCard
                    statusSection
                    plansSection
                    autoRenewSection
                    referralSection
                }
                .padding(.horizontal, DSMetrics.screenPadX)
                .padding(.top, DSSpacing.md)
                .padding(.bottom, 24)
            }
            .refreshable { await vm.pullToRefresh() }
            if !ctaLabel.isEmpty {
                ctaBlock
            }
        }
        .navigationBarHidden(true)
        .background(DSColor.bg.ignoresSafeArea())
        .onAppear { vm.refreshRecurringStatus() }
        .onReceive(vm.$message) { message in
            guard let message = message, !message.isEmpty else { return }
            snackbars.append(DSSnackbarItem(text: message))
        }
        .dsSnackbarQueue(snackbars, bottomPadding: 24) {
            if !snackbars.isEmpty { snackbars.removeFirst() }
        }
        // Страница оплаты (§19.3): SFSafariViewController, возврат → проверка (§19.4/§19.6).
        .fullScreenCover(item: $safariItem) { item in
            PurchasesSafariView(url: item.url) {
                safariItem = nil
                vm.checkPayment()
            }
            .ignoresSafeArea()
        }
        .overlay {
            if !vm.state.paymentDialogKind.isEmpty {
                PurchasesPaymentStatusDialog(kind: vm.state.paymentDialogKind) {
                    vm.dismissPaymentDialog()
                }
            }
        }
        .overlay {
            if vm.state.autoRenewConfirmVisible {
                PurchasesAutoRenewConfirm(
                    dateText: vm.state.autoRenewConfirmDateText,
                    isDisabling: vm.state.isDisablingAutoRenew,
                    onKeep: { vm.hideAutoRenewConfirm() },
                    onDisable: { vm.disableAutoRenew() }
                )
            }
        }
        // Глобальные диалоги §19.5–19.6 показываются на уровне приложения
        // (AppCoordinator) — на любом экране, включая этот.
        .onReceive(vm.$paymentUrl) { url in
            guard let url = url, let link = URL(string: url) else { return }
            safariItem = SafariItem(url: link)
            vm.consumePaymentUrl()
        }
    }

    // MARK: - App bar (§19.2)

    private var topBar: some View {
        HStack {
            Button {
                router.back()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(DSColor.text)
                    .frame(width: 40, height: 40)
            }
            Spacer()
            if (vm.state.subscriptionKind == "paywall" || vm.state.subscriptionKind == "expired"),
               vm.state.isSubscriptionLoaded {
                Button("Восстановить") { vm.restorePurchases() }
                    .font(DSFont.sans(15, .medium))
                    .foregroundColor(DSColor.accent)
            }
        }
        .padding(.horizontal, DSMetrics.screenPadX - 8)
        .frame(height: 52)
    }

    // MARK: - Hero (§19.2)

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: DSSpacing.md) {
            HStack(spacing: DSSpacing.md) {
                ZStack {
                    Circle().fill(Color.white.opacity(0.16))
                    Image(systemName: "crown.fill")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.white)
                }
                .frame(width: 48, height: 48)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Машинист Pro")
                        .font(DSFont.sans(20, .bold))
                        .foregroundColor(.white)
                    Text("ПОЛНАЯ ВЕРСИЯ")
                        .font(DSFont.sans(11, .bold))
                        .foregroundColor(.white.opacity(0.7))
                }
                Spacer()
            }
            Text("Облако, экспорт и безлимит истории. Все поездки под рукой и в безопасности.")
                .font(DSFont.sans(14, .regular))
                .foregroundColor(.white.opacity(0.85))
                .fixedSize(horizontal: false, vertical: true)
            if vm.state.subscriptionKind == "paywall" || vm.state.subscriptionKind == "loading" {
                VStack(alignment: .leading, spacing: DSSpacing.sm) {
                    heroFeature("Облачная копия и синхронизация", icon: .sync)
                    heroFeature("Экспорт в PDF", icon: .pdf)
                    heroFeature("Безлимит маршрутов и истории", icon: .document)
                }
                .padding(.top, DSSpacing.xs)
            }
        }
        .padding(DSSpacing.xl)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [Color(red: 0.07, green: 0.09, blue: 0.14), Color(red: 0.11, green: 0.16, blue: 0.26)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.xl, style: .continuous))
    }

    private func heroFeature(_ text: String, icon: DSIcon) -> some View {
        HStack(spacing: DSSpacing.md) {
            icon.image
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 24)
            Text(text)
                .font(DSFont.sans(14, .medium))
                .foregroundColor(.white.opacity(0.92))
        }
    }

    // MARK: - Статус подписки (§19.2)

    @ViewBuilder
    private var statusSection: some View {
        switch vm.state.subscriptionKind {
        case "loading", "paywall":
            if vm.state.subscriptionKind == "loading" {
                HStack { Spacer(); ProgressView(); Spacer() }
                    .frame(height: 60)
            }
        case "active":
            VStack(alignment: .leading, spacing: DSSpacing.md) {
                banner(
                    icon: .check,
                    tone: DSColor.success,
                    title: "Подписка активна",
                    subtitle: "Действует до \(vm.state.subscriptionEndText)"
                )
                if vm.state.remainingBonusDays > 0 {
                    Text("Из них \(vm.state.remainingBonusDays) дн. — бонус по реферальной программе")
                        .font(DSFont.sans(12, .regular))
                        .foregroundColor(DSColor.textMuted)
                        .padding(.leading, DSSpacing.xs)
                }
                if vm.state.daysLeft >= 0 && vm.state.daysLeft <= 7 {
                    banner(
                        icon: .clock,
                        tone: DSColor.warning,
                        title: "Подписка истекает через \(daysWord(Int(vm.state.daysLeft)))",
                        subtitle: "Продлите заранее — новый срок прибавится к текущему, дни не сгорят."
                    )
                }
            }
        case "expired":
            VStack(alignment: .leading, spacing: DSSpacing.md) {
                banner(
                    icon: .warning,
                    tone: DSColor.danger,
                    title: "Подписка истекла",
                    subtitle: "Закончилась \(vm.state.subscriptionEndText)"
                )
                Text("Маршруты и история сохранены. Но добавлять новые и пользоваться синхронизацией нельзя, пока подписка не возобновлена.")
                    .font(DSFont.sans(13, .regular))
                    .foregroundColor(DSColor.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(DSSpacing.lg)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(DSColor.bgSubtle)
                    .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
            }
        default:
            EmptyView()
        }
    }

    private func banner(icon: DSIcon, tone: Color, title: String, subtitle: String) -> some View {
        HStack(alignment: .top, spacing: DSSpacing.md) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(tone.opacity(0.16))
                icon.image.font(.system(size: 17, weight: .semibold)).foregroundColor(tone)
            }
            .frame(width: 40, height: 40)
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(DSFont.sans(15, .bold))
                    .foregroundColor(DSColor.text)
                Text(subtitle)
                    .font(DSFont.sans(12, .regular))
                    .foregroundColor(DSColor.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer()
        }
        .padding(DSSpacing.lg)
        .background(tone.opacity(0.10))
        .overlay(
            RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous)
                .stroke(tone.opacity(0.25), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
    }

    /// «Подписка истекает через N день/дня/дней» (§19.2).
    private func daysWord(_ n: Int) -> String {
        let mod100 = n % 100
        let mod10 = n % 10
        let word: String
        if mod100 >= 11 && mod100 <= 14 { word = "дней" }
        else if mod10 == 1 { word = "день" }
        else if mod10 >= 2 && mod10 <= 4 { word = "дня" }
        else { word = "дней" }
        return "\(n) \(word)"
    }

    // MARK: - Тарифы (§19.0, §19.2)

    @ViewBuilder
    private var plansSection: some View {
        if vm.state.isLoadingProducts {
            HStack { Spacer(); ProgressView().frame(width: 160, height: 160); Spacer() }
        } else {
            if vm.state.subscriptionKind == "active" {
                if !vm.state.renewalPreviewTitle.isEmpty {
                    renewalPreviewCard
                }
                sectionHead("ПЕРИОД ПРОДЛЕНИЯ")
            } else {
                sectionHead("ВЫБЕРИТЕ ТАРИФ")
            }
            VStack(spacing: DSSpacing.sm) {
                ForEach(vm.state.products) { plan in
                    PlanCard(
                        plan: plan,
                        isSelected: plan.code == vm.state.selectedCode,
                        discountUntilText: discountUntilText(plan)
                    ) {
                        vm.selectPlan(code: plan.code)
                    }
                }
            }
        }
    }

    private var renewalPreviewCard: some View {
        VStack(spacing: DSSpacing.sm) {
            Text(vm.state.renewalPreviewTitle)
                .font(DSFont.sans(12, .medium))
                .foregroundColor(DSColor.textMuted)
                .frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: DSSpacing.sm) {
                Text(vm.state.renewalPreviewAddText)
                    .font(DSFont.sans(14, .bold))
                    .foregroundColor(DSColor.accent)
                    .padding(.horizontal, DSSpacing.md)
                    .padding(.vertical, 6)
                    .background(DSColor.accentSoft)
                    .clipShape(Capsule())
                Image(systemName: "arrow.right")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(DSColor.textFaint)
                Text(vm.state.renewalPreviewResultText)
                    .font(DSFont.sans(13, .bold))
                    .foregroundColor(DSColor.text)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(DSSpacing.lg)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        .dsShadow(.sm)
    }

    private func sectionHead(_ text: String) -> some View {
        Text(text)
            .font(DSFont.sans(12, .bold))
            .foregroundColor(DSColor.textMuted)
            .tracking(0.6)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, DSSpacing.xs)
    }

    private func discountUntilText(_ plan: PurchasesPlanModel) -> String? {
        guard plan.discountActive, plan.discountUntil > 0 else { return nil }
        let date = Date(timeIntervalSince1970: Double(plan.discountUntil) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM.yyyy"
        return "Действует до " + formatter.string(from: date)
    }

    // MARK: - Автопродление (§19.A)

    @ViewBuilder
    private var autoRenewSection: some View {
        if vm.state.isSubscriptionLoaded && !vm.state.products.isEmpty && !vm.state.selectedCode.isEmpty {
            if vm.state.autoRenewEnabled {
                DSCard {
                    VStack(alignment: .leading, spacing: DSSpacing.sm) {
                        HStack(spacing: DSSpacing.md) {
                            DSIconAvatar(icon: .sync, tone: .accent)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Автопродление включено")
                                    .font(DSFont.sans(15, .bold))
                                    .foregroundColor(DSColor.text)
                                if !vm.state.autoRenewNextChargeText.isEmpty {
                                    Text(vm.state.autoRenewNextChargeText)
                                        .font(DSFont.sans(13, .regular))
                                        .foregroundColor(DSColor.textMuted)
                                }
                            }
                            Spacer()
                            Button("Отключить") { vm.showAutoRenewConfirm() }
                                .font(DSFont.sans(14, .semibold))
                                .foregroundColor(DSColor.danger)
                        }
                        if vm.state.autoRenewLastChargeFailed {
                            Text("Последнее списание не прошло — повторим через сутки.")
                                .font(DSFont.sans(12, .medium))
                                .foregroundColor(DSColor.danger)
                        }
                    }
                    .padding(DSSpacing.card)
                }
            } else if vm.state.autoRenewAvailable {
                DSCard {
                    VStack(alignment: .leading, spacing: DSSpacing.sm) {
                        Button {
                            vm.setAutoRenewChecked(!vm.state.autoRenewChecked)
                        } label: {
                            HStack(spacing: DSSpacing.md) {
                                Image(systemName: vm.state.autoRenewChecked ? "checkmark.square.fill" : "square")
                                    .font(.system(size: 20, weight: .medium))
                                    .foregroundColor(vm.state.autoRenewChecked ? DSColor.accent : DSColor.textFaint)
                                Text("Автопродление")
                                    .font(DSFont.sans(15, .bold))
                                    .foregroundColor(DSColor.text)
                                Spacer()
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        consentTextView
                    }
                    .padding(DSSpacing.card)
                }
            }
            if vm.state.autoRenewDisabledReason == "charge_failed" {
                Text("Автопродление отключено: несколько списаний подряд не прошли. Продлите подписку вручную — при оплате его можно включить снова.")
                    .font(DSFont.sans(12, .regular))
                    .foregroundColor(DSColor.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    /// Текст согласия сервера без изменений; адрес оферты убран, ссылкой
    /// становится слово перед ним (§19.A).
    private var consentTextView: some View {
        var attributed = AttributedString(vm.state.autoRenewConsentBefore + vm.state.autoRenewConsentLinkWord + vm.state.autoRenewConsentAfter)
        if !vm.state.autoRenewConsentLinkWord.isEmpty,
           !vm.state.autoRenewOfferUrl.isEmpty,
           let url = URL(string: vm.state.autoRenewOfferUrl),
           let range = attributed.range(of: vm.state.autoRenewConsentLinkWord) {
            attributed[range].link = url
            attributed[range].foregroundColor = DSColor.accent
        }
        return Text(attributed)
            .font(DSFont.sans(12, .regular))
            .foregroundColor(DSColor.textMuted)
            .fixedSize(horizontal: false, vertical: true)
    }

    // MARK: - Реферальный код (§19.R)

    @ViewBuilder
    private var referralSection: some View {
        if vm.state.canApplyReferral {
            DSCard {
                VStack(alignment: .leading, spacing: DSSpacing.md) {
                    HStack(spacing: DSSpacing.sm) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(DSColor.accent)
                        Text("Есть код друга?")
                            .font(DSFont.sans(16, .bold))
                            .foregroundColor(DSColor.text)
                    }
                    Text("Введите его до первой оплаты")
                        .font(DSFont.sans(13, .medium))
                        .foregroundColor(DSColor.text)
                    Text("После оплаты каждый получит половину срока выбранного тарифа дополнительно.")
                        .font(DSFont.sans(12, .regular))
                        .foregroundColor(DSColor.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                    TextField("Например, ABCD2345", text: $referralCode)
                        .font(.system(.body, design: .monospaced))
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                        .disabled(vm.state.isApplyingReferral)
                        .padding(DSSpacing.md)
                        .background(DSColor.bgSubtle)
                        .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                        .onChange(of: referralCode) { value in
                            referralCode = String(value.uppercased().filter { $0.isLetter || $0.isNumber }.prefix(16))
                        }
                    Button {
                        vm.applyReferralCode(referralCode)
                    } label: {
                        HStack(spacing: DSSpacing.sm) {
                            if vm.state.isApplyingReferral {
                                ProgressView().tint(DSColor.accentInk)
                                Text("Проверяем код…")
                            } else {
                                Text("Применить код")
                            }
                        }
                        .font(DSFont.sans(15, .bold))
                        .foregroundColor(DSColor.accentInk)
                        .frame(maxWidth: .infinity, minHeight: 50)
                        .background(DSColor.accent)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(DSPressableStyle())
                    .disabled(referralCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || vm.state.isApplyingReferral)
                    if !vm.state.referralMessage.isEmpty {
                        Text(vm.state.referralMessage)
                            .font(DSFont.sans(12, .medium))
                            .foregroundColor(vm.state.referralMessageIsError ? DSColor.danger : DSColor.success)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .padding(DSSpacing.card)
            }
        } else if !vm.state.appliedReferralCode.isEmpty && vm.state.appliedReferralStatus == "pending" {
            DSCard {
                VStack(alignment: .leading, spacing: DSSpacing.xs) {
                    Text("Код \(vm.state.appliedReferralCode) применён")
                        .font(DSFont.sans(15, .bold))
                        .foregroundColor(DSColor.text)
                    Text("При первой оплате вам и другу начислится по половине оплаченного периода. Бонус можно получить только один раз — при первой оплате.")
                        .font(DSFont.sans(12, .regular))
                        .foregroundColor(DSColor.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(DSSpacing.card)
            }
        }
    }

    // MARK: - CTA (§19.2)

    private var ctaLabel: String {
        let loaded = vm.state.isSubscriptionLoaded && !vm.state.products.isEmpty
        return loaded && !vm.state.selectedCode.isEmpty ? vm.state.ctaLabel : ""
    }

    private var ctaBlock: some View {
        VStack(spacing: DSSpacing.sm) {
            Button {
                vm.startPayment()
            } label: {
                HStack(spacing: DSSpacing.sm) {
                    if vm.state.isStartingPayment {
                        ProgressView().tint(DSColor.ctaInk)
                    }
                    Text(ctaLabel)
                        .font(DSFont.sans(16, .bold))
                }
                .foregroundColor(DSColor.ctaInk)
                .frame(maxWidth: .infinity, minHeight: 54)
                .background(DSColor.cta)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .opacity(vm.state.isStartingPayment ? 0.5 : 1)
            }
            .buttonStyle(DSPressableStyle())
            .disabled(vm.state.isStartingPayment)
            if !vm.state.ctaCaption.isEmpty {
                Text(vm.state.ctaCaption)
                    .font(DSFont.sans(12, .regular))
                    .foregroundColor(DSColor.textMuted)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.horizontal, DSMetrics.screenPadX)
        .padding(.top, DSSpacing.sm)
        .padding(.bottom, DSSpacing.lg)
        .background(DSColor.bg)
    }
}

/// Карточка тарифа (`PlanCard`, §19.0).
private struct PlanCard: View {
    let plan: PurchasesPlanModel
    let isSelected: Bool
    let discountUntilText: String?
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .center, spacing: DSSpacing.md) {
                Image(systemName: isSelected ? "circle.inset.filled" : "circle")
                    .font(.system(size: 22, weight: .regular))
                    .foregroundColor(isSelected ? DSColor.accent : DSColor.textFaint)
                VStack(alignment: .leading, spacing: 3) {
                    Text(plan.name)
                        .font(DSFont.sans(15, .bold))
                        .foregroundColor(DSColor.text)
                    HStack(spacing: 6) {
                        Text("\(plan.perMonthRub) ₽/мес")
                            .font(DSFont.sans(12, .medium))
                            .foregroundColor(DSColor.textMuted)
                        if plan.benefitPercent > 0 {
                            Text("−\(plan.benefitPercent)%")
                                .font(DSFont.sans(10, .bold))
                                .foregroundColor(DSColor.success)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(DSColor.success.opacity(0.12))
                                .clipShape(Capsule())
                        }
                    }
                    if let discountUntilText = discountUntilText {
                        Text(discountUntilText)
                            .font(DSFont.sans(11, .medium))
                            .foregroundColor(DSColor.warning)
                    }
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    if plan.discountActive, plan.basePrice > plan.sum {
                        Text("\(Int(plan.basePrice.rounded())) ₽")
                            .font(DSFont.sans(11, .regular))
                            .foregroundColor(DSColor.textFaint)
                            .strikethrough()
                    }
                    Text("\(Int(plan.sum.rounded())) ₽")
                        .font(DSFont.sans(18, .bold))
                        .foregroundColor(DSColor.text)
                    if plan.discountActive && plan.discountPercent > 0 {
                        Text("Акция −\(plan.discountPercent)%")
                            .font(DSFont.sans(10, .bold))
                            .foregroundColor(DSColor.warning)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(DSColor.warning.opacity(0.14))
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(DSSpacing.lg)
            .background(isSelected ? DSColor.accentSoft : DSColor.surface)
            .overlay(
                RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous)
                    .stroke(isSelected ? DSColor.accent : DSColor.border, lineWidth: isSelected ? 1.5 : 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        }
        .buttonStyle(DSPressableStyle())
    }
}

/// Идентификатор для `.fullScreenCover(item:)`.
private struct SafariItem: Identifiable {
    let id = UUID()
    let url: URL
}
