@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.Product
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.util.displayTimeZone
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.GetUserProfileState
import com.z_company.repository.remote_rest.RecurringConditionsChangedException
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.repository.remote_rest.SyncManager
import com.z_company.repository.remote_rest.request.RobokassaCheckoutRequest
import com.z_company.repository.remote_rest.response.ApplyReferralCodeRequest
import com.z_company.repository.remote_rest.response.RecurringStatusResponse
import com.z_company.repository.remote_rest.response.RecurringTermsResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock

/**
 * iOS-аналог `PurchasesViewModel` + `PaymentReturnChecker` + `SubscriptionNoticesViewModel`
 * (SCREEN_SPECS §19): цены/скидки §19.0, состав экрана §19.2, оплата §19.3
 * (серверный checkout Robokassa, `platform: "ios"`), срок по тарифу §19.05,
 * лимит бесплатных маршрутов §19.1/19.1a, автопродление §19.A, реферальный код
 * §19.R, диалоги §19.4–19.6.
 *
 * Логика — Kotlin (data_remote/domain); Swift только показывает и открывает
 * `payment_url` в SFSafariViewController. Срок подписки начисляет только сервер.
 *
 * single в Koin: ожидаемая оплата и глобальные диалоги живут дольше экрана (§19.5–19.6).
 */
class PurchasesIosViewModel(
    private val remoteRestApi: RemoteRestApi,
    private val settingManager: SettingManager,
    private val settingsUseCase: SettingsUseCase,
    private val secureTokenStorage: SecureTokenStorage,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val authManager: AuthManager,
    private val syncManager: SyncManager,
    private val routeUseCase: RouteUseCase,
) : ViewModel() {

    companion object {
        /** Лимит бесплатных маршрутов (как `RouteActionsHelper.FREE_ROUTES_LIMIT`, §19.1). */
        const val FREE_ROUTES_LIMIT = 20
        private const val GRACE_PERIOD_MS = 24L * 3_600_000L
        private const val DAY_MS = 86_400_000L
        /** «Истекает через N дней» — порог §19.2. */
        private const val EXPIRING_SOON_DAYS = 7
        // §19.6: TTL записи ожидаемой оплаты, «свежая оплата», попытки, пауза.
        private const val PENDING_TTL_MS = DAY_MS
        private const val FRESH_PAYMENT_MS = 10L * 60_000L
        private const val FRESH_ATTEMPTS = 10
        private const val RETRY_DELAY_MS = 3_000L
        private const val RESUME_THROTTLE_MS = 30_000L
        private const val PAYMENT_PLATFORM = "ios"
    }

    private val _state = MutableStateFlow(PurchasesIosState())
    val state = _state.asStateFlow()

    private val _messages = MutableStateFlow<String?>(null)
    val messages = _messages.asStateFlow()

    private val _openPaymentUrl = MutableStateFlow<String?>(null)
    val openPaymentUrl = _openPaymentUrl.asStateFlow()

    private val _globalNotice = MutableStateFlow<PurchasesGlobalNoticeUi?>(null)
    val globalNotice = _globalNotice.asStateFlow()

    private var settingsCache: UserSettings? = null
    private var selectedProduct: PurchasesPlanUi? = null
    private var recurringTerms: RecurringTermsResponse? = null
    private var termsTariffCode: String? = null
    private var paymentCheckJob: Job? = null
    private var lastResumeCheckAt = 0L
    private var periodUpdatesInProgress = 0
    private var noticesEvaluating = false
    private val noticeQueue = ArrayDeque<PurchasesGlobalNoticeUi>()
    private var pendingBonusDays = 0

    init {
        observeSubscription()
        refreshProducts()
        refreshReferralStatus()
        refreshRecurringStatus()
    }

    // ── Наблюдение и пересборка состояния ─────────────────────────────────

    private fun observeSubscription() {
        viewModelScope.launch {
            combine(
                settingsUseCase.getUserSettingFlow(),
                secureTokenStorage.getAuthBearerTokenFlow(),
                secureTokenStorage.getUserIdFlow(),
            ) { setting, token, userId -> Triple(setting, token, userId) }
                .collect { (setting, token, userId) ->
                    settingsCache = setting
                    rebuildState()
                    evaluateNotices(setting, token, userId)
                }
        }
    }

    private fun rebuildState() {
        val setting = settingsCache
        val tz = displayTz()
        val now = nowMs()
        val end = setting?.subscriptionPeriod ?: 0L
        val loaded = setting != null
        val kind = when {
            !loaded -> "loading"
            end == 0L -> "paywall"
            end > now -> "active"
            else -> "expired"
        }
        val daysLeft = if (kind == "active") {
            ((end - now + DAY_MS - 1) / DAY_MS).toInt().coerceAtLeast(0)
        } else 0
        val products = _state.value.products
        val plan = products.firstOrNull { it.code == _state.value.selectedCode }
        _state.value = _state.value.copy(
            isSubscriptionLoaded = loaded,
            subscriptionKind = kind,
            subscriptionEndTime = end,
            subscriptionEndText = if (end > 0) fmtDate(end, tz) else "",
            daysLeft = daysLeft,
            ctaLabel = ctaLabel(kind, plan),
            ctaCaption = ctaCaption(kind),
            renewalPreviewTitle = renewalPreviewTitle(kind, end, plan, tz),
            renewalPreviewAddText = renewalPreviewAddText(kind, plan),
            renewalPreviewResultText = renewalPreviewResultText(kind, end, plan, tz),
        )
    }

    private fun ctaLabel(kind: String, plan: PurchasesPlanUi?): String {
        val sum = plan?.sum?.roundToInt() ?: return ""
        return when (kind) {
            "paywall" -> when (plan.monthsCount) {
                1 -> "Оформить за $sum ₽/мес"
                12 -> "Оформить за $sum ₽/год"
                else -> "Оформить за $sum ₽"
            }
            "active" -> "Продлить · $sum ₽"
            "expired" -> "Возобновить подписку"
            else -> ""
        }
    }

    private fun ctaCaption(kind: String): String = when (kind) {
        "paywall" -> "Первые 20 маршрутов — бесплатно. Дальше добавление маршрутов — только по подписке."
        "active" -> "Новый период прибавится к текущему сроку."
        "expired" -> "Возобновление начнётся с сегодняшнего дня."
        else -> ""
    }

    // «ПЕРИОД ПРОДЛЕНИЯ»: СЕЙЧАС ДО X → «+ N месяцев» → СТАНЕТ ДО Y (только Active).
    // BUG-57 (паритет с Android): превью прибавляет календарные месяцы, а сервер
    // начисляет periodDays (31/93/365) — даты могут расходиться на 1–2 дня.
    private fun renewalPreviewTitle(kind: String, end: Long, plan: PurchasesPlanUi?, tz: TimeZone): String {
        if (kind != "active" || plan == null || end <= 0) return ""
        return "СЕЙЧАС ДО ${fmtDate(end, tz)}"
    }

    private fun renewalPreviewAddText(kind: String, plan: PurchasesPlanUi?): String {
        if (kind != "active" || plan == null) return ""
        return when (plan.monthsCount) {
            1 -> "+ 1 месяц"
            3 -> "+ 3 месяца"
            12 -> "+ 1 год"
            else -> "+ ${plan.monthsCount} мес."
        }
    }

    private fun renewalPreviewResultText(kind: String, end: Long, plan: PurchasesPlanUi?, tz: TimeZone): String {
        if (kind != "active" || plan == null || end <= 0) return ""
        val t = Instant.fromEpochMilliseconds(end).toLocalDateTime(tz)
        val shifted = addCalendarMonths(t.year, t.monthNumber, t.dayOfMonth, plan.monthsCount)
        return "СТАНЕТ ДО ${fmtDate(shifted)}"
    }

    private fun addCalendarMonths(year: Int, month: Int, day: Int, months: Int): kotlinx.datetime.LocalDate {
        val total = year * 12 + (month - 1) + months
        val ny = total / 12
        val nm = total % 12 + 1
        val maxDay = when (nm) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            2 -> if (ny % 4 == 0 && (ny % 100 != 0 || ny % 400 == 0)) 29 else 28
            else -> 30
        }
        return kotlinx.datetime.LocalDate(ny, nm, minOf(day, maxDay))
    }

    // ── Тарифы (§19.0) ────────────────────────────────────────────────────

    fun refreshProducts() {
        _state.value = _state.value.copy(isLoadingProducts = true)
        viewModelScope.launch(Dispatchers.Default) {
            val products = fetchTariffsOrDefault()
            val selected = _state.value.selectedCode.ifBlank {
                products.maxByOrNull { it.sum }?.code ?: ""
            }
            _state.value = _state.value.copy(
                isLoadingProducts = false,
                products = products,
                selectedCode = selected,
            )
            selectedProduct = products.firstOrNull { it.code == selected }
            rebuildState()
            if (selected.isNotBlank()) onPlanSelected(selected)
        }
    }

    /**
     * `GET /v1/tariffs` → `Product` → `PurchasesPlanUi`. Офлайн/ошибка/пусто —
     * дефолтный набор (месяц/квартал/год = 69/179/599 ₽, 31/93/365 дней);
     * ошибка пользователю не показывается (§19.0).
     */
    private suspend fun fetchTariffsOrDefault(): List<PurchasesPlanUi> {
        val products: List<Product> = try {
            val tariffs = remoteRestApi.getTariffs().tariffs
            if (tariffs.isEmpty()) defaultProducts()
            else tariffs.map { t ->
                Product(
                    name = t.title,
                    desc = t.desc,
                    sum = t.price,
                    code = t.code,
                    periodDays = t.periodDays,
                    basePrice = t.basePrice,
                    discountPercent = if (t.discountActive) t.discountPercent else 0,
                    discountActive = t.discountActive,
                    discountUntil = t.discountUntil,
                )
            }
        } catch (t: Throwable) {
            defaultProducts()
        }
        return mapPlans(products)
    }

    private fun defaultProducts(): List<Product> = listOf(
        Product(name = "1 месяц", desc = "Новичок", sum = 69.0, code = "month", periodDays = 31, basePrice = 69.0),
        Product(name = "3 месяца", desc = "Эксперт", sum = 179.0, code = "quarter", periodDays = 93, basePrice = 179.0),
        Product(name = "1 год", desc = "Профи", sum = 599.0, code = "year", periodDays = 365, basePrice = 599.0),
    )

    private fun mapPlans(products: List<Product>): List<PurchasesPlanUi> {
        val anyDiscount = products.any { it.discountActive && it.discountPercent > 0 }
        // «Выгода тарифа» — база: ₽/мес тарифа с наименьшим числом месяцев.
        val monthlyBase = products
            .map { it.sum / monthsCount(it.name).coerceAtLeast(1) }
            .minOrNull() ?: 0.0
        return products
            .sortedByDescending { it.sum }
            .map { p ->
                val months = monthsCount(p.name)
                val perMonth = (p.sum / months.coerceAtLeast(1)).roundToInt()
                val benefit = if (!anyDiscount && monthlyBase > 0) {
                    (100.0 * (1.0 - perMonth / monthlyBase)).roundToInt()
                } else 0
                PurchasesPlanUi(
                    code = p.code,
                    name = p.name,
                    desc = p.desc,
                    sum = p.sum,
                    basePrice = p.basePrice,
                    discountPercent = p.discountPercent,
                    discountActive = p.discountActive && p.discountPercent > 0,
                    discountUntil = p.discountUntil ?: 0L,
                    periodDays = p.periodDays,
                    monthsCount = months,
                    perMonthRub = perMonth,
                    benefitPercent = if (benefit > 0) benefit else 0,
                )
            }
    }

    /** Число месяцев по названию тарифа: «год» → 12, начинается с «3» → 3, иначе 1 (§19.2). */
    private fun monthsCount(name: String): Int = when {
        name.contains("год", ignoreCase = true) -> 12
        name.trimStart().startsWith("3") -> 3
        else -> 1
    }

    /** Выбран тариф: снять галочку автопродления и загрузить текст согласия (§19.A). */
    fun onPlanSelected(code: String) {
        val products = _state.value.products
        val plan = products.firstOrNull { it.code == code }
        selectedProduct = plan
        _state.value = _state.value.copy(selectedCode = code, autoRenewChecked = false)
        rebuildState()
        if (code == termsTariffCode && recurringTerms != null) return
        termsTariffCode = code
        recurringTerms = null
        applyTermsToState()
        if (code.isBlank()) return
        viewModelScope.launch {
            val terms = try {
                remoteRestApi.getRecurringTerms(code)
            } catch (_: Exception) {
                null
            }
            if (termsTariffCode == code) {
                recurringTerms = terms
                applyTermsToState()
            }
        }
    }

    // ── Автопродление (§19.A) ─────────────────────────────────────────────

    fun setAutoRenewChecked(checked: Boolean) {
        _state.value = _state.value.copy(autoRenewChecked = checked)
    }

    fun refreshRecurringStatus() {
        viewModelScope.launch {
            val status = try {
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (token.isNullOrBlank()) null
                else remoteRestApi.getRecurringStatus("Bearer $token")
            } catch (_: Exception) {
                null
            }
            setRecurringStatus(status)
        }
    }

    /**
     * Новое состояние автопродления: при включении/выключении галочку снимаем
     * (чекбокс, появившийся снова, не должен быть отмечен заранее).
     */
    private fun setRecurringStatus(status: RecurringStatusResponse?) {
        val prev = _state.value
        val enabledChanged = status?.enabled != prev.autoRenewEnabled
        val tz = displayTz()
        val nextCharge = if (status?.enabled == true) {
            val amount = status.amount?.roundToInt()
            val date = status.nextChargeAt?.let { fmtDate(it, tz) }
            when {
                date != null && amount != null -> "Следующее списание $date · $amount ₽"
                amount != null -> "$amount ₽ за период"
                else -> ""
            }
        } else ""
        _state.value = prev.copy(
            autoRenewEnabled = status?.enabled == true,
            autoRenewChecked = if (enabledChanged) false else prev.autoRenewChecked,
            autoRenewNextChargeText = nextCharge,
            autoRenewLastChargeFailed = status?.lastChargeStatus == "failed",
            autoRenewDisabledReason = status?.disabledReason ?: "",
            isDisablingAutoRenew = false,
        )
        applyTermsToState()
    }

    private fun applyTermsToState() {
        val terms = recurringTerms
        val status = _state.value
        val available = terms?.available == true &&
            (terms.tariffCode.isBlank() || terms.tariffCode == status.selectedCode) &&
            !status.autoRenewEnabled
        val split = splitConsentText(terms?.consentText ?: "")
        val amount = terms?.amount?.roundToInt()?.let { "$it ₽" } ?: ""
        val renewal = terms?.renewalAmount?.roundToInt()?.let { "$it ₽" } ?: ""
        _state.value = status.copy(
            autoRenewAvailable = available,
            autoRenewConsentBefore = split.first,
            autoRenewConsentLinkWord = split.second,
            autoRenewConsentAfter = split.third,
            autoRenewOfferUrl = terms?.offerUrl ?: "",
            autoRenewConsentVersion = terms?.consentVersion ?: "",
            autoRenewTermsAmountText = amount,
            autoRenewRenewalAmountText = renewal,
        )
    }

    /**
     * Адрес оферты `(https://…)` убирается из текста; ссылкой на оферту становится
     * слово перед скобками («оферты») — §19.A.
     */
    private fun splitConsentText(text: String): Triple<String, String, String> {
        val idx = text.indexOf(" (http")
        if (idx < 0) return Triple(text.trim(), "", "")
        val close = text.indexOf(')', idx)
        val before = text.substring(0, idx).trimEnd()
        val after = if (close >= 0) text.substring(close + 1) else ""
        val wordStart = before.lastIndexOf(' ').let { if (it < 0) 0 else it + 1 }
        val word = before.substring(wordStart)
        return Triple(before.substring(0, wordStart), word, after)
    }

    fun showAutoRenewConfirm() {
        val tz = displayTz()
        val end = _state.value.subscriptionEndTime
        _state.value = _state.value.copy(
            autoRenewConfirmVisible = true,
            autoRenewConfirmDateText = if (end > nowMs()) fmtDate(end, tz) else "",
        )
    }

    fun hideAutoRenewConfirm() {
        _state.value = _state.value.copy(autoRenewConfirmVisible = false)
    }

    /** Отключить автопродление (после подтверждения). Оплаченный период сохраняется. */
    fun disableAutoRenew() {
        if (_state.value.isDisablingAutoRenew) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isDisablingAutoRenew = true)
            val ok = try {
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (token.isNullOrBlank()) false
                else {
                    setRecurringStatus(remoteRestApi.disableRecurring("Bearer $token"))
                    true
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(isDisablingAutoRenew = false)
                false
            }
            _state.value = _state.value.copy(autoRenewConfirmVisible = false)
            _messages.value = if (ok) "Автопродление отключено"
            else "Не удалось отключить автопродление. Попробуйте ещё раз."
        }
    }

    // ── Реферальный код (§19.R) ───────────────────────────────────────────

    fun refreshReferralStatus() {
        viewModelScope.launch {
            try {
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (token.isNullOrBlank()) return@launch
                val status = remoteRestApi.getReferralStatus("Bearer $token")
                _state.value = _state.value.copy(
                    canApplyReferral = status.canApplyCode,
                    appliedReferralCode = status.appliedCode ?: "",
                    appliedReferralStatus = status.appliedStatus ?: "",
                    remainingBonusDays = status.remainingBonusDays,
                )
            } catch (_: Exception) {
                // Ошибка запроса молча скрывает блок (§19.R).
                _state.value = _state.value.copy(
                    canApplyReferral = false,
                    appliedReferralCode = "",
                    appliedReferralStatus = "",
                )
            }
        }
    }

    fun applyReferralCode(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isApplyingReferral = true,
                referralMessage = "",
                referralMessageIsError = false,
            )
            try {
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (token.isNullOrBlank()) throw IllegalStateException("no token")
                remoteRestApi.applyReferralCode("Bearer $token", ApplyReferralCodeRequest(trimmed))
                _state.value = _state.value.copy(
                    isApplyingReferral = false,
                    referralMessage = "Код принят. Бонус начислится после первой оплаты.",
                    referralMessageIsError = false,
                )
                refreshReferralStatus()
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isApplyingReferral = false,
                    referralMessage = "Не удалось применить код. Проверьте его и попробуйте снова.",
                    referralMessageIsError = true,
                )
            }
        }
    }

    // ── Оплата (§19.3) ────────────────────────────────────────────────────

    /** CTA: серверный checkout Robokassa (`platform: "ios"`), затем открытие payment_url. */
    fun startPayment() {
        val product = selectedProduct ?: return
        if (product.code.isBlank()) {
            _messages.value = "Ошибка: Тариф недоступен для оплаты"
            return
        }
        if (_state.value.isStartingPayment) return
        val terms = recurringTerms
        val withAutoRenew = _state.value.autoRenewChecked &&
            terms?.available == true && terms.tariffCode == product.code &&
            !_state.value.autoRenewEnabled
        viewModelScope.launch {
            _state.value = _state.value.copy(isStartingPayment = true)
            try {
                val userId = currentUserId()
                if (userId == null) {
                    _messages.value = "Ошибка: Отсутствует User ID"
                    return@launch
                }
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (token.isNullOrBlank()) {
                    _messages.value = "Неавторизованный пользователь"
                    return@launch
                }
                val response = remoteRestApi.createRobokassaCheckout(
                    token = "Bearer $token",
                    request = RobokassaCheckoutRequest(
                        tariffCode = product.code,
                        autoRenew = withAutoRenew,
                        consentVersion = if (withAutoRenew) terms?.consentVersion else null,
                        platform = PAYMENT_PLATFORM,
                    ),
                )
                markPaymentStarted(userId, product)
                val url = response.paymentUrl
                if (url.isBlank()) {
                    _messages.value = "Не удалось открыть страницу оплаты"
                } else {
                    _openPaymentUrl.value = url
                }
            } catch (e: RecurringConditionsChangedException) {
                // Условия автопродления обновились — перечитать и снять галочку.
                termsTariffCode = null
                onPlanSelected(product.code)
                _messages.value =
                    "Условия автопродления обновились. Проверьте их и нажмите оплату ещё раз."
            } catch (t: Throwable) {
                _messages.value = "Ошибка: ${t.message ?: "не удалось создать платёж"}"
            } finally {
                _state.value = _state.value.copy(isStartingPayment = false)
            }
        }
    }

    /** Swift после закрытия страницы оплаты: освободить событие открытия URL. */
    fun consumeOpenPaymentUrl() {
        _openPaymentUrl.value = null
    }

    /**
     * user_id оплаты — серверный `user.id`; пустой id дочитывается из профиля
     * (`GET /v1/users/me`), без него оплата не запускается (§19.3).
     */
    private suspend fun currentUserId(): String? {
        secureTokenStorage.getUserIdFlow().first()?.takeIf { it.isNotBlank() }?.let { return it }
        val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            ?.takeIf { it.isNotBlank() } ?: return null
        val state = try {
            authManager.getUserProfile("Bearer $token")
                .first { it !is GetUserProfileState.Loading }
        } catch (_: Exception) {
            null
        }
        val userId = (state as? GetUserProfileState.Success)?.user?.id
            ?.takeIf { it.isNotBlank() } ?: return null
        secureTokenStorage.saveUserId(userId)
        return userId
    }

    /** Запомнить ожидаемую оплату — переживает выгрузку приложения (§19.6). */
    private fun markPaymentStarted(userId: String, product: PurchasesPlanUi) {
        val pending = PendingPaymentRecord(
            userId = userId,
            startedAt = nowMs(),
            periodBefore = _state.value.subscriptionEndTime.coerceAtLeast(0L),
            periodDays = product.periodDays.coerceAtLeast(0),
        )
        sharedPrefs.setPendingPayment(pending.encode())
    }

    // ── Восстановление покупок (§19.3) ────────────────────────────────────

    fun restorePurchases() {
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) {
                _messages.value = "Неавторизованный пользователь"
                return@launch
            }
            _messages.value = "Начинаем поиск..."
            restorePeriodFromServer(showMessages = true)
        }
    }

    /**
     * Срок с сервера (`GET /v1/user_settings/`) в локальные настройки.
     * Возвращает серверный срок или null при ошибке сети.
     */
    private suspend fun restorePeriodFromServer(showMessages: Boolean): Long? {
        val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            ?.takeIf { it.isNotBlank() } ?: return null
        val remote = try {
            settingManager.getAccountSubscriptionPeriod("Bearer $token")
        } catch (_: Exception) {
            null
        }
        if (remote == null) {
            if (showMessages) {
                _messages.value = "Ошибка связи с сервером. Напишите в поддержку."
            }
            return null
        }
        val tz = displayTz()
        if (remote > nowMs()) {
            periodUpdatesInProgress++
            try {
                settingsUseCase.updateSubscriptionPeriod(remote).collect { }
            } finally {
                periodUpdatesInProgress--
            }
            if (showMessages) {
                _messages.value = "Данные об оплате обновлены. До ${fmtDateTime(remote, tz)}"
            }
        } else if (remote == 0L) {
            if (showMessages) _messages.value = "Не найдено информации об оплате"
        } else {
            if (showMessages) _messages.value = "Срок оплаты истек ${fmtDateTime(remote, tz)}"
        }
        return remote
    }

    /** Pull-to-refresh: полная синхронизация (по подписке) + перезагрузка тарифов (§19.2). */
    fun pullToRefresh() {
        viewModelScope.launch {
            try {
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (!token.isNullOrBlank() && hasActiveSubscriptionNow()) {
                    syncManager.syncBidirectional("Bearer $token").collect { }
                } else if (token != null) {
                    _messages.value = "Синхронизация доступна по подписке"
                }
            } catch (_: Exception) {
                // Ошибка сети — тарифы всё равно перечитаем ниже.
            }
            refreshProducts()
            refreshRecurringStatus()
            refreshReferralStatus()
            restorePeriodFromServer(showMessages = false)
        }
    }

    // ── Проверка оплаты и глобальные диалоги (§19.4–19.6) ─────────────────

    /**
     * Проверка после возврата со страницы оплаты (SFSafariViewController или
     * `locodriver://`). Второй запуск, пока идёт первый, игнорируется.
     * На всё время поллинга — недисмиссируемый «Получаем данные…» (§19.4).
     */
    fun checkPaymentOnServer() {
        if (paymentCheckJob?.isActive == true) return
        paymentCheckJob = viewModelScope.launch {
            _state.value = _state.value.copy(paymentDialogKind = "loading")
            val result = try {
                runPaymentCheck(forcedAttempts = 5)
            } finally {
                if (_state.value.paymentDialogKind == "loading") {
                    _state.value = _state.value.copy(paymentDialogKind = "")
                }
            }
            when (result) {
                // «Платёж принят!» показан глобально; no_pending — уже подтверждено.
                "paid", "no_pending" -> Unit
                else -> _state.value = _state.value.copy(paymentDialogKind = "failed")
            }
        }
    }

    fun dismissPaymentDialog() {
        _state.value = _state.value.copy(paymentDialogKind = "")
    }

    /**
     * Возврат приложения на передний план (§19.6): проверка ожидаемой оплаты
     * и глобальных диалогов. Throttle 30 с.
     */
    fun onAppResumed() {
        val now = nowMs()
        if (sharedPrefs.getPendingPayment() != null && now - lastResumeCheckAt >= RESUME_THROTTLE_MS) {
            lastResumeCheckAt = now
            if (paymentCheckJob?.isActive != true) {
                paymentCheckJob = viewModelScope.launch {
                    runPaymentCheck(forcedAttempts = null)
                }
            }
        }
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val userId = if (token.isNullOrBlank()) null else currentUserId()
            evaluateNotices(settingsUseCase.getUserSettingFlow().first(), token, userId)
        }
    }

    /**
     * Поллинг срока с сервера в обход гейта подписки (§19.6). Подтверждение —
     * рост срока минимум на длину тарифа от `max(срок до оплаты, момент оплаты)`;
     * бонус рефералки оплатой не считается.
     */
    private suspend fun runPaymentCheck(forcedAttempts: Int?): String {
        val raw = sharedPrefs.getPendingPayment() ?: return "no_pending"
        val pending = PendingPaymentRecord.decode(raw) ?: run {
            sharedPrefs.setPendingPayment(null)
            return "no_pending"
        }
        val now = nowMs()
        if (now - pending.startedAt > PENDING_TTL_MS) {
            sharedPrefs.setPendingPayment(null)
            return "no_pending"
        }
        val userId = currentUserId()
        if (userId.isNullOrBlank() || userId != pending.userId) {
            sharedPrefs.setPendingPayment(null)
            return "no_pending"
        }
        val attempts = forcedAttempts
            ?: if (now - pending.startedAt < FRESH_PAYMENT_MS) FRESH_ATTEMPTS else 1
        periodUpdatesInProgress++
        try {
            repeat(attempts) { attempt ->
                val period = restorePeriodFromServer(showMessages = false)
                if (period != null && isPaymentConfirmed(pending, period)) {
                    sharedPrefs.setPendingPayment(null)
                    acknowledgeSeenPeriod(userId, period)
                    enqueueNotice(
                        PurchasesGlobalNoticeUi(
                            kind = "paid",
                            title = "Платёж принят!",
                            body = "Подписка активна до ${fmtDateFull(period)}. Спасибо за поддержку приложения!",
                            button = "Отлично!",
                        )
                    )
                    return "paid"
                }
                if (attempt < attempts - 1) delay(RETRY_DELAY_MS)
            }
            return "not_confirmed"
        } finally {
            periodUpdatesInProgress--
        }
    }

    /** Сервер продлевает от `max(прежний срок, момент оплаты)` (§19.05/§19.6). */
    private fun isPaymentConfirmed(pending: PendingPaymentRecord, period: Long): Boolean {
        if (period <= pending.periodBefore || period <= nowMs()) return false
        if (pending.periodDays <= 0) return true
        val base = maxOf(pending.periodBefore, pending.startedAt)
        return period - base >= pending.periodDays * DAY_MS - DAY_MS
    }

    private fun acknowledgeSeenPeriod(userId: String, period: Long) {
        sharedPrefs.setLastSeenSubscriptionPeriod(userId, period)
        sharedPrefs.setSubscriptionPeriodReferralDays(userId, -1)
    }

    // ── «Бонус начислен!» / «Подписка продлена» (§19.5) ───────────────────

    /**
     * Правила §19.5: ключ — серверный `user.id`; проверка при старте и при
     * изменении срока/токена/userId; сначала бонус, затем срок; пока есть
     * неподтверждённая оплата — срок не оцениваем.
     */
    private suspend fun evaluateNotices(
        setting: UserSettings?,
        token: String?,
        userIdFromStore: String?,
    ) {
        if (noticesEvaluating) return
        noticesEvaluating = true
        try {
            if (token.isNullOrBlank()) return
            val userId = userIdFromStore?.takeIf { it.isNotBlank() } ?: currentUserId() ?: return
            val period = setting?.subscriptionPeriod ?: 0L
            // awardedDays нужен всегда (бонус); не удалось — ничего не запоминаем.
            val awardedDays = try {
                remoteRestApi.getReferralStatus("Bearer $token").awardedDays
            } catch (_: Exception) {
                return
            }
            // Бонус (всегда).
            val lastSeenBonus = sharedPrefs.getLastSeenReferralAwardedDays(userId)
            sharedPrefs.setLastSeenReferralAwardedDays(userId, awardedDays)
            if (lastSeenBonus in 0 until awardedDays) {
                pendingBonusDays += awardedDays - lastSeenBonus
                refreshBonusNotice()
            }
            // Срок — только вне поллинга оплаты (§19.5, SubscriptionPeriodTracker).
            if (sharedPrefs.getPendingPayment() != null || periodUpdatesInProgress > 0) return
            if (period <= 0L) return
            val lastSeen = sharedPrefs.getLastSeenSubscriptionPeriod(userId)
            val referralDaysInLastSeen = sharedPrefs.getSubscriptionPeriodReferralDays(userId)
            if (period == lastSeen) {
                if (referralDaysInLastSeen < 0) {
                    sharedPrefs.setSubscriptionPeriodReferralDays(userId, awardedDays)
                }
                return
            }
            sharedPrefs.setLastSeenSubscriptionPeriod(userId, period)
            sharedPrefs.setSubscriptionPeriodReferralDays(userId, awardedDays)
            if (lastSeen <= 0L) return // первое отслеживание — только база
            val bonusDays = if (referralDaysInLastSeen >= 0) {
                (awardedDays - referralDaysInLastSeen).coerceAtLeast(0)
            } else 0
            if (!isExtendedBeyondBonus(lastSeen, period, bonusDays)) return
            enqueueNotice(
                PurchasesGlobalNoticeUi(
                    kind = "renewed",
                    title = "Подписка продлена",
                    body = "Срок подписки продлён до ${fmtDateFull(period)}. Спасибо за поддержку приложения!",
                    button = "Отлично!",
                )
            )
        } finally {
            noticesEvaluating = false
        }
    }

    private fun isExtendedBeyondBonus(lastSeen: Long, period: Long, bonusDays: Int): Boolean {
        val now = nowMs()
        if (period <= lastSeen || period <= now) return false
        if (bonusDays == 0) return true
        val bonusBase = maxOf(lastSeen, now)
        return period - bonusDays * DAY_MS > bonusBase + DAY_MS
    }

    private fun bonusBody(): String =
        "Вам добавлено $pendingBonusDays дн. подписки по реферальной программе."

    /** Несколько бонусов до закрытия диалога суммируются в одном (§19.5). */
    private fun refreshBonusNotice() {
        if (pendingBonusDays <= 0) return
        val showing = _globalNotice.value
        if (showing?.kind == "bonus") {
            _globalNotice.value = showing.copy(body = bonusBody())
            return
        }
        val idx = noticeQueue.indexOfFirst { it.kind == "bonus" }
        if (idx >= 0) {
            noticeQueue[idx] = noticeQueue[idx].copy(body = bonusBody())
        } else {
            enqueueNotice(
                PurchasesGlobalNoticeUi(
                    kind = "bonus",
                    title = "Бонус начислен! 🎉",
                    body = bonusBody(),
                    button = "Ура!",
                )
            )
        }
    }

    /** Порядок: «Платёж принят!» → «Бонус начислен!» → «Подписка продлена» (один на экране). */
    private fun enqueueNotice(notice: PurchasesGlobalNoticeUi) {
        if (_globalNotice.value == null) {
            _globalNotice.value = notice
        } else {
            noticeQueue.addLast(notice)
        }
    }

    fun dismissGlobalNotice() {
        if (_globalNotice.value?.kind == "bonus") pendingBonusDays = 0
        _globalNotice.value = noticeQueue.removeFirstOrNull()
    }

    // ── Лимит бесплатных маршрутов (§19.1/19.1a) ─────────────────────────

    /** Активна ли подписка для гейта синхронизации (без грейса, §19.1). */
    suspend fun hasActiveSubscriptionNow(): Boolean {
        val end = settingsUseCase.getUserSettingFlow().first().subscriptionPeriod
        return end != 0L && end > nowMs()
    }

    fun hasActiveSubscription(callback: (Boolean) -> Unit) {
        viewModelScope.launch { callback(hasActiveSubscriptionNow()) }
    }

    /**
     * Пакетное создание маршрутов (§19.1): подписка активна (с грейсом 1 сутки)
     * → можно без лимита; иначе пачка целиком помещается в остаток лимита или
     * `limit` (SubscriptionLimitDialog §19.1a). Ошибка → `error`.
     */
    fun checkBatchRoutes(count: Int, callback: (PurchasesLimitGateUi) -> Unit) {
        viewModelScope.launch {
            val authorized = !secureTokenStorage.getAuthBearerTokenFlow().first().isNullOrBlank()
            val gate = try {
                val now = nowMs()
                val end = settingsUseCase.getUserSettingFlow().first().subscriptionPeriod
                val active = end != 0L && end + GRACE_PERIOD_MS >= now
                if (active) {
                    PurchasesLimitGateUi(type = "allowed", requested = count, remaining = -1, isAuthorized = authorized)
                } else {
                    val used = withContext(Dispatchers.Default) {
                        routeUseCase.listRouteWithDeleting().size
                    }
                    val left = (FREE_ROUTES_LIMIT - used).coerceAtLeast(0)
                    if (count <= left) {
                        PurchasesLimitGateUi(type = "allowed", requested = count, remaining = left - count, isAuthorized = authorized)
                    } else {
                        PurchasesLimitGateUi(type = "limit", requested = count, remaining = left, isAuthorized = authorized)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                PurchasesLimitGateUi(type = "error", requested = count, remaining = 0, isAuthorized = false)
            }
            callback(gate)
        }
    }

    // ── Утилиты ───────────────────────────────────────────────────────────

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun displayTz(): TimeZone {
        val us = settingsCache ?: settingsUseCase.getUserSetting()
        return runCatching { TimeZone.of(us.displayTimeZone()) }.getOrElse { TimeZone.of("GMT+3") }
    }

    private fun two(n: Int): String = if (n < 10) "0$n" else n.toString()

    /** `dd.MM.yy` (§19.2). */
    private fun fmtDate(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return fmtDate(t.date)
    }

    private fun fmtDate(date: kotlinx.datetime.LocalDate): String =
        "${two(date.dayOfMonth)}.${two(date.monthNumber)}.${two(date.year % 100)}"

    /** `dd.MM.yyyy` (§19.5–19.6 — системный пояс устройства по спеке). */
    private fun fmtDateFull(ms: Long): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
        return "${two(t.dayOfMonth)}.${two(t.monthNumber)}.${t.year}"
    }

    /** `dd.MM.yy HH:mm` (§19.3 restore). */
    private fun fmtDateTime(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return "${two(t.dayOfMonth)}.${two(t.monthNumber)}.${two(t.year % 100)} ${two(t.hour)}:${two(t.minute)}"
    }

    fun consumeMessage() {
        _messages.value = null
    }

    // ── watchX(callback) для Swift (AGENTS.md) ────────────────────────────

    fun watchState(callback: (PurchasesIosState) -> Unit) {
        viewModelScope.launch { _state.collect { callback(it) } }
    }

    fun watchMessages(callback: (String?) -> Unit) {
        viewModelScope.launch { _messages.collect { callback(it) } }
    }

    fun watchOpenPaymentUrl(callback: (String?) -> Unit) {
        viewModelScope.launch { _openPaymentUrl.collect { callback(it) } }
    }

    fun watchGlobalNotice(callback: (PurchasesGlobalNoticeUi?) -> Unit) {
        viewModelScope.launch { _globalNotice.collect { callback(it) } }
    }
}

/** Ожидаемая оплата (§19.6): хранится строкой `userId|startedAt|periodBefore|periodDays`. */
internal data class PendingPaymentRecord(
    val userId: String,
    val startedAt: Long,
    val periodBefore: Long,
    val periodDays: Int,
) {
    fun encode(): String = listOf(userId, startedAt, periodBefore, periodDays).joinToString("|")

    companion object {
        fun decode(raw: String?): PendingPaymentRecord? {
            val parts = raw?.split("|") ?: return null
            if (parts.size != 4 || parts[0].isBlank()) return null
            return PendingPaymentRecord(
                userId = parts[0],
                startedAt = parts[1].toLongOrNull() ?: return null,
                periodBefore = parts[2].toLongOrNull() ?: return null,
                periodDays = parts[3].toIntOrNull() ?: return null,
            )
        }
    }
}
