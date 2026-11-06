import SwiftUI
import ComposeApp

// Диалоги «Покупок» (SCREEN_SPECS §19.4–19.6, §19.1a, §19.A).
// Вид PaymentDialog — по §19.4: карточка 22pt, круг-иконка 56pt, кнопки 50/48pt.

/// Общий вид диалогов оплаты и подписки (§19.4).
struct PaymentDialog: View {
    enum Tone {
        case neutral, error, clock, success, gift

        var color: Color {
            switch self {
            case .neutral: return DSColor.accent
            case .error: return DSColor.danger
            case .clock: return DSColor.warning
            case .success: return DSColor.success
            case .gift: return Color(red: 0.54, green: 0.38, blue: 0.0) // #8A6200
            }
        }
    }

    let title: String
    /// Текст под заголовком (в спеке — `body`; имя занято `View.body`).
    var bodyText: String? = nil
    var icon: DSIcon? = nil
    var tone: Tone = .neutral
    var dismissible: Bool = true
    var primaryLabel: String? = nil
    var secondaryLabel: String? = nil
    var showSpinner: Bool = false
    var onPrimary: () -> Void = {}
    var onSecondary: (() -> Void)? = nil
    var onDismiss: () -> Void = {}

    var body: some View {
        ZStack {
            DSColor.scrim.ignoresSafeArea()
                .onTapGesture { if dismissible { onDismiss() } }
            VStack(spacing: DSSpacing.lg) {
                if let icon = icon {
                    ZStack {
                        Circle().fill(tone.color.opacity(0.12))
                        icon.image.font(.system(size: 22, weight: .semibold))
                            .foregroundColor(tone.color)
                    }
                    .frame(width: 56, height: 56)
                }
                Text(title)
                    .font(DSFont.sans(18, .bold))
                    .foregroundColor(DSColor.text)
                    .multilineTextAlignment(.center)
                if let bodyText = bodyText, !bodyText.isEmpty {
                    Text(bodyText)
                        .font(DSFont.sans(14, .regular))
                        .foregroundColor(DSColor.textMuted)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                }
                if showSpinner {
                    ProgressView().tint(DSColor.accent)
                }
                if let primaryLabel = primaryLabel {
                    Button(action: onPrimary) {
                        Text(primaryLabel)
                            .font(DSFont.sans(15, .bold))
                            .foregroundColor(DSColor.surface)
                            .frame(maxWidth: .infinity, minHeight: 50)
                            .background(DSColor.accent)
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(DSPressableStyle())
                }
                if let secondaryLabel = secondaryLabel, let onSecondary = onSecondary {
                    Button(action: onSecondary) {
                        Text(secondaryLabel)
                            .font(DSFont.sans(15, .semibold))
                            .foregroundColor(DSColor.text)
                            .frame(maxWidth: .infinity, minHeight: 48)
                            .overlay(
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .stroke(DSColor.border, lineWidth: 1)
                            )
                    }
                    .buttonStyle(DSPressableStyle())
                }
            }
            .padding(22)
            .background(DSColor.surface)
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(DSColor.border, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .padding(.horizontal, DSSpacing.xxl)
        }
    }
}

/// «Получаем данные…» / «Оплата не завершена» / «Платёж обрабатывается» (§19.4).
struct PurchasesPaymentStatusDialog: View {
    let kind: String
    let onDismiss: () -> Void

    var body: some View {
        switch kind {
        case "loading":
            PaymentDialog(
                title: "Получаем данные…",
                bodyText: "Проверяем оплату на сервере — это займёт несколько секунд.",
                dismissible: false,
                showSpinner: true
            )
        case "failed":
            PaymentDialog(
                title: "Оплата не завершена",
                bodyText: "Данные об оплате не получены. Если у вас есть вопросы, напишите в поддержку.",
                icon: .warning,
                tone: .error,
                primaryLabel: "Написать в поддержку",
                secondaryLabel: "Закрыть",
                onPrimary: {
                    let to = "locodriver.app@yandex.ru"
                    let subject = "Вопрос по оплате".addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
                    if let url = URL(string: "mailto:\(to)?subject=\(subject)") {
                        UIApplication.shared.open(url)
                    }
                    onDismiss()
                },
                onSecondary: onDismiss
            )
        case "processing":
            PaymentDialog(
                title: "Платёж обрабатывается",
                bodyText: "Robokassa подтвердила платёж, но сервер ещё не обновил подписку. Нажмите «Восстановить» через несколько минут.",
                icon: .clock,
                tone: .clock,
                primaryLabel: "Понятно",
                onPrimary: onDismiss
            )
        default:
            EmptyView()
        }
    }
}

/// Глобальные диалоги «Платёж принят!» / «Бонус начислен!» / «Подписка продлена»
/// (§19.5–19.6) — вид PaymentDialog.
struct PurchasesGlobalNoticeDialog: View {
    let notice: PurchasesGlobalNoticeUi
    let onDismiss: () -> Void

    var body: some View {
        PaymentDialog(
            title: notice.title,
            bodyText: notice.body,
            icon: notice.kind == "bonus" ? .heart : .check,
            tone: notice.kind == "bonus" ? .gift : .success,
            primaryLabel: notice.button,
            onPrimary: onDismiss,
            onDismiss: onDismiss
        )
    }
}

/// Подтверждение «Отключить автопродление?» (§19.A, деструктивное).
struct PurchasesAutoRenewConfirm: View {
    let dateText: String
    let isDisabling: Bool
    let onKeep: () -> Void
    let onDisable: () -> Void

    private var message: String {
        let tail = "Дальше деньги списываться не будут — продлить можно будет вручную."
        return dateText.isEmpty ? tail : "Подписка продолжит действовать до \(dateText). \(tail)"
    }

    var body: some View {
        ZStack {
            DSColor.scrim.ignoresSafeArea()
            VStack(spacing: DSSpacing.lg) {
                Text("Отключить автопродление?")
                    .font(DSFont.sans(18, .bold))
                    .foregroundColor(DSColor.text)
                    .multilineTextAlignment(.center)
                Text(message)
                    .font(DSFont.sans(14, .regular))
                    .foregroundColor(DSColor.textMuted)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                Button(action: onDisable) {
                    HStack(spacing: DSSpacing.sm) {
                        if isDisabling { ProgressView().tint(DSColor.surface) }
                        Text(isDisabling ? "Отключаем…" : "Отключить")
                            .font(DSFont.sans(15, .bold))
                    }
                    .foregroundColor(DSColor.surface)
                    .frame(maxWidth: .infinity, minHeight: 50)
                    .background(DSColor.danger)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(DSPressableStyle())
                .disabled(isDisabling)
                Button(action: onKeep) {
                    Text("Оставить")
                        .font(DSFont.sans(15, .semibold))
                        .foregroundColor(DSColor.text)
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .stroke(DSColor.border, lineWidth: 1)
                        )
                }
                .buttonStyle(DSPressableStyle())
            }
            .padding(22)
            .background(DSColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .padding(.horizontal, DSSpacing.xxl)
        }
    }
}

/// «Не хватает бесплатных маршрутов» (SubscriptionLimitDialog, §19.1a).
/// Показывается, когда пачка маршрутов не помещается в остаток бесплатного лимита.
struct SubscriptionLimitDialog: View {
    let requested: Int
    let remaining: Int
    let isAuthorized: Bool
    let onSubscribe: () -> Void
    let onCancel: () -> Void

    private static func routesWord(_ n: Int) -> String {
        let mod100 = n % 100
        let mod10 = n % 10
        if mod100 >= 11 && mod100 <= 14 { return "маршрутов" }
        if mod10 == 1 { return "маршрут" }
        if mod10 >= 2 && mod10 <= 4 { return "маршрута" }
        return "маршрутов"
    }

    private var message: String {
        if remaining == 0 {
            return "Бесплатный лимит исчерпан: использовано 20 из 20. Сейчас создаётся \(requested) \(Self.routesWord(requested)). Оформите подписку — лимит снимется и включится синхронизация."
        }
        return "В бесплатном периоде доступно 20 маршрутов, у вас осталось \(remaining). Сейчас создаётся \(requested) \(Self.routesWord(requested)). Оформите подписку — лимит снимется и включится синхронизация."
    }

    var body: some View {
        ZStack {
            DSColor.scrim.ignoresSafeArea()
            VStack(spacing: DSSpacing.lg) {
                Text("Не хватает бесплатных маршрутов")
                    .font(DSFont.sans(18, .bold))
                    .foregroundColor(DSColor.text)
                    .multilineTextAlignment(.center)
                Text(message)
                    .font(DSFont.sans(14, .regular))
                    .foregroundColor(DSColor.textMuted)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                Button(action: onSubscribe) {
                    Text("Оформить подписку")
                        .font(DSFont.sans(15, .bold))
                        .foregroundColor(DSColor.surface)
                        .frame(maxWidth: .infinity, minHeight: 50)
                        .background(DSColor.accent)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(DSPressableStyle())
                Button(action: onCancel) {
                    Text("Отмена")
                        .font(DSFont.sans(15, .semibold))
                        .foregroundColor(DSColor.text)
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .stroke(DSColor.border, lineWidth: 1)
                        )
                }
                .buttonStyle(DSPressableStyle())
            }
            .padding(22)
            .background(DSColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .padding(.horizontal, DSSpacing.xxl)
        }
    }
}
