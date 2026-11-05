package com.z_company.iosapp.viewmodel

/**
 * Модели экрана «Покупки» / подписка (SCREEN_SPECS §19) для Swift.
 *
 * Плоские data class со строковыми дискриминаторами (`type`, `kind`) —
 * sealed-иерархии в ObjC-экспорт неудобны (см. NewRouteIosDecision).
 * Имена `new`/`init`/`copy` не объявляем (AGENTS.md).
 */

/** Тариф для `PlanCard` (§19.0). */
data class PurchasesPlanUi(
    /** Стабильный код тарифа (`month`|`quarter`|`year`) — уходит в checkout. */
    val code: String,
    val name: String,
    val desc: String,
    /** Итоговая цена к оплате (со скидкой, если активна). */
    val sum: Double,
    val basePrice: Double,
    val discountPercent: Int,
    val discountActive: Boolean,
    /** ms epoch окончания акции; 0 — не задано. */
    val discountUntil: Long,
    val periodDays: Int,
    /** 1/3/12 — по названию тарифа («год» → 12, начинается с «3» → 3, иначе 1). */
    val monthsCount: Int,
    /** round(sum / месяцев) — строка «{N} ₽/мес». */
    val perMonthRub: Int,
    /** «Выгода тарифа» −N% (0 — не показывать; только когда ни у кого нет акции). */
    val benefitPercent: Int,
)

/** Итог проверки пакетного создания маршрутов (§19.1/19.1a). */
data class PurchasesLimitGateUi(
    /** "allowed" | "limit" | "error". */
    val type: String,
    val requested: Int,
    /** Сколько свободных маршрутов осталось (0 при ошибке/лимите). */
    val remaining: Int,
    /** Непустой bearer-токен — для гейта покупок из диалога (§3.2). */
    val isAuthorized: Boolean = true,
)

/** Глобальный диалог «Платёж принят!» / «Бонус начислен!» / «Подписка продлена» (§19.5–19.6). */
data class PurchasesGlobalNoticeUi(
    /** "paid" | "bonus" | "renewed". */
    val kind: String,
    val title: String,
    val body: String,
    val button: String,
)

/** Состояние экрана «Покупки» (§19.2, §19.A, §19.R, §19.4). */
data class PurchasesIosState(
    val isLoadingProducts: Boolean = true,
    /** Локальный срок прочитан из UserSettings (пока false — нейтральный лоадинг). */
    val isSubscriptionLoaded: Boolean = false,
    /** "loading" | "paywall" | "active" | "expired". */
    val subscriptionKind: String = "loading",
    val subscriptionEndTime: Long = 0,
    /** dd.MM.yy в поясе отображения. */
    val subscriptionEndText: String = "",
    val daysLeft: Int = 0,
    /** Из них N дн. — бонус по реферальной программе (§19.2). */
    val remainingBonusDays: Int = 0,
    val products: List<PurchasesPlanUi> = emptyList(),
    val selectedCode: String = "",
    /** Пока сервер создаёт платёж — CTA неактивна (закрывает BUG-58). */
    val isStartingPayment: Boolean = false,

    // ── Автопродление (§19.A) ──
    /** enabled из GET /v1/payment/recurring. */
    val autoRenewEnabled: Boolean = false,
    /** terms.available и статус загружен — показывать блок согласия. */
    val autoRenewAvailable: Boolean = false,
    val autoRenewChecked: Boolean = false,
    /** Текст согласия сервера без адреса оферты (см. consentBefore/LinkWord/After). */
    val autoRenewConsentBefore: String = "",
    val autoRenewConsentLinkWord: String = "",
    val autoRenewConsentAfter: String = "",
    val autoRenewOfferUrl: String = "",
    val autoRenewConsentVersion: String = "",
    /** «Следующее списание dd.MM.yy · {amount} ₽» / «{amount} ₽ за период» / "". */
    val autoRenewNextChargeText: String = "",
    val autoRenewLastChargeFailed: Boolean = false,
    /** "" | "user" | "charge_failed". */
    val autoRenewDisabledReason: String = "",
    val autoRenewTermsAmountText: String = "",
    val autoRenewRenewalAmountText: String = "",
    val isDisablingAutoRenew: Boolean = false,
    /** Показать подтверждение «Отключить автопродление?». */
    val autoRenewConfirmVisible: Boolean = false,
    /** Дата из подтверждения отключения («Подписка продолжит действовать до dd.MM.yy»). */
    val autoRenewConfirmDateText: String = "",

    // ── Реферальный код (§19.R) ──
    val canApplyReferral: Boolean = false,
    val appliedReferralCode: String = "",
    /** "" | "pending" | "rewarded" | "reversed". */
    val appliedReferralStatus: String = "",
    val referralMessage: String = "",
    val referralMessageIsError: Boolean = false,
    val isApplyingReferral: Boolean = false,

    // ── Диалоги оплаты экрана (§19.4) ──
    /** "" | "loading" | "failed" | "processing". */
    val paymentDialogKind: String = "",

    // ── Закреплённая CTA (§19.2) ──
    val ctaLabel: String = "",
    val ctaCaption: String = "",
    /** Карточка перехода дат «СЕЙЧАС → +N месяцев → СТАНЕТ ДО» ("" — не показывать). */
    val renewalPreviewTitle: String = "",
    val renewalPreviewAddText: String = "",
    val renewalPreviewResultText: String = "",
)
