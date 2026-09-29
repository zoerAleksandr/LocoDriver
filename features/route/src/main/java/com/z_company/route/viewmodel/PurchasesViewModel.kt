package com.z_company.route.viewmodel

import android.util.Log
import com.z_company.core.sendToSentry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.Product
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.repository.remote_rest.request.CkassaCheckoutRequest
import com.z_company.repository.remote_rest.request.RobokassaCheckoutRequest
import com.z_company.repository.remote_rest.response.RecurringStatusResponse
import com.z_company.repository.remote_rest.response.RecurringTermsResponse
import com.z_company.repository.remote_rest.RecurringConditionsChangedException
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.route.subscription.PaymentReturnChecker
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.GetUserProfileState
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.use_case.SubscriptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class BillingState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val activeExpirations: Map<String, Long> = emptyMap(),
    val dateAndTimeConverter: DateAndTimeConverter? = null
)

sealed class BillingEvent {
    data class ShowError(val error: Throwable) : BillingEvent()

    // Открыть страницу оплаты (Robokassa/CKassa) в браузере. Ссылку формирует
    // сервер; после возврата в приложение экран сам поллит статус подписки.
    data class OpenPaymentUrl(val url: String) : BillingEvent()

    data class ShowMessage(val message: String) : BillingEvent()
}

class PurchasesViewModel : ViewModel(), KoinComponent {
    private val sharedPrefs: SharedPreferencesRepositories by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val snackbarManager: ISnackbarManager by inject()

    private val subscriptionHelper: SubscriptionHelper by inject()
    private val authManager: AuthManager by inject()
    private val settingManager: SettingManager by inject()
    private val remoteRestApi: RemoteRestApi by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()
    private val paymentReturnChecker: PaymentReturnChecker by inject()

    private val _referralStatus = MutableStateFlow<com.z_company.repository.remote_rest.response.ReferralStatusResponse?>(null)
    val referralStatus = _referralStatus.asStateFlow()
    private val _referralMessage = MutableStateFlow<String?>(null)
    val referralMessage = _referralMessage.asStateFlow()
    private val _isApplyingReferral = MutableStateFlow(false)
    val isApplyingReferral = _isApplyingReferral.asStateFlow()

    // --- Автопродление (рекуррент Robokassa) ---
    // Текст согласия для выбранного тарифа (формирует сервер).
    private val _recurringTerms = MutableStateFlow<RecurringTermsResponse?>(null)
    val recurringTerms = _recurringTerms.asStateFlow()

    // Галочка «Автопродление». По требованию платёжных систем по умолчанию
    // НЕ стоит; снимается при смене тарифа — согласие даётся на его сумму.
    private val _autoRenewChecked = MutableStateFlow(false)
    val autoRenewChecked = _autoRenewChecked.asStateFlow()

    private val _recurringStatus = MutableStateFlow<RecurringStatusResponse?>(null)
    val recurringStatus = _recurringStatus.asStateFlow()

    private val _isDisablingRecurring = MutableStateFlow(false)
    val isDisablingRecurring = _isDisablingRecurring.asStateFlow()

    // Защита от двойного тапа по CTA, пока создаётся платёж.
    private val _isStartingPayment = MutableStateFlow(false)
    val isStartingPayment = _isStartingPayment.asStateFlow()

    private var termsTariffCode: String? = null

    private val _state = MutableStateFlow(BillingState(isLoading = true))
    val state = _state.asStateFlow()

    private val _purchasesEndTime = MutableStateFlow(0L)
    val purchasesEndTime = _purchasesEndTime.asStateFlow()

    // Статус подписки известен (загружен из локальных настроек). Пока false —
    // экран не показывает шапку/блок статуса как «неактивную подписку», а держит
    // нейтральный лоадинг (мы узнаём реальный статус ещё в Профиле, быстро).
    private val _isSubscriptionLoaded = MutableStateFlow(false)
    val isSubscriptionLoaded = _isSubscriptionLoaded.asStateFlow()

    private val _showPaymentLoadingDialog = MutableStateFlow(false)
    val showPaymentLoadingDialog = _showPaymentLoadingDialog.asStateFlow()

    private val _showPaymentFailedDialog = MutableStateFlow(false)
    val showPaymentFailedDialog = _showPaymentFailedDialog.asStateFlow()

    // Robokassa подтвердила платёж, но сервер ещё не обработал webhook — подписка скоро обновится
    private val _showPaymentProcessingDialog = MutableStateFlow(false)
    val showPaymentProcessingDialog = _showPaymentProcessingDialog.asStateFlow()

    private val _event = MutableSharedFlow<BillingEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val event = _event.asSharedFlow()
    var currentEmail: String = ""

    init {
        loadDateConverter()
        observeSubscription()
        refreshProductsAndPurchases()
        refreshReferralStatus()
        refreshRecurringStatus()
    }

    /** Выбран тариф: снять галочку и загрузить текст согласия для него. */
    fun onPlanSelected(product: Product?) {
        val code = product?.code?.takeIf { it.isNotBlank() }
        if (code == termsTariffCode && _recurringTerms.value != null) return
        termsTariffCode = code
        _autoRenewChecked.value = false
        _recurringTerms.value = null
        if (code == null) return
        viewModelScope.launch {
            val terms = try {
                remoteRestApi.getRecurringTerms(code)
            } catch (_: Exception) {
                null
            }
            // Пока шёл запрос, пользователь мог выбрать другой тариф.
            if (termsTariffCode == code) _recurringTerms.value = terms
        }
    }

    fun setAutoRenewChecked(checked: Boolean) {
        _autoRenewChecked.value = checked
    }

    fun refreshRecurringStatus() {
        viewModelScope.launch {
            setRecurringStatus(
                try {
                    val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                    remoteRestApi.getRecurringStatus("Bearer $token")
                } catch (_: Exception) {
                    null
                }
            )
        }
    }

    /**
     * Новое состояние автопродления. Если оно включилось или выключилось,
     * галочку согласия снимаем: чекбокс, появившийся снова (например, после
     * отключения), не должен быть отмечен заранее.
     */
    private fun setRecurringStatus(status: RecurringStatusResponse?) {
        if (status?.enabled != _recurringStatus.value?.enabled) _autoRenewChecked.value = false
        _recurringStatus.value = status
    }

    /** Отключить автопродление (после подтверждения в диалоге). */
    fun disableRecurring(onDone: (Boolean) -> Unit) {
        if (_isDisablingRecurring.value) return
        viewModelScope.launch {
            _isDisablingRecurring.value = true
            val ok = try {
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                setRecurringStatus(remoteRestApi.disableRecurring("Bearer $token"))
                true
            } catch (t: Throwable) {
                t.sendToSentry("PurchasesViewModel", "disableRecurring")
                false
            } finally {
                _isDisablingRecurring.value = false
            }
            _event.tryEmit(
                BillingEvent.ShowMessage(
                    if (ok) "Автопродление отключено"
                    else "Не удалось отключить автопродление. Попробуйте ещё раз."
                )
            )
            onDone(ok)
        }
    }

    fun refreshReferralStatus() {
        viewModelScope.launch {
            try {
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                _referralStatus.value = remoteRestApi.getReferralStatus("Bearer $token")
            } catch (_: Exception) {
                _referralStatus.value = null
            }
        }
    }

    fun applyReferralCode(code: String) {
        viewModelScope.launch {
            _isApplyingReferral.value = true
            _referralMessage.value = null
            try {
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                remoteRestApi.applyReferralCode(
                    "Bearer $token",
                    com.z_company.repository.remote_rest.response.ApplyReferralCodeRequest(code.trim()),
                )
                _referralMessage.value = "Код принят. Бонус начислится после первой оплаты."
                refreshReferralStatus()
            } catch (_: Exception) {
                _referralMessage.value = "Не удалось применить код. Проверьте его и попробуйте снова."
            } finally {
                _isApplyingReferral.value = false
            }
        }
    }

    /**
     * Статус подписки (endTime) грузим отдельно от тарифов — из локальных
     * настроек, быстро и параллельно. Так шапка/блок статуса не мигают
     * «неактивной подпиской», пока тарифы тянутся из сети.
     */
    private fun observeSubscription() {
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collect { setting ->
                _purchasesEndTime.value = setting.subscriptionPeriod
                _isSubscriptionLoaded.value = true
            }
        }
    }

    private fun loadDateConverter() {
        viewModelScope.launch {
            val setting = settingsUseCase.getUserSettingFlow().first()
            _state.update { it.copy(dateAndTimeConverter = DateAndTimeConverter(setting)) }
        }
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val fullToken = "Bearer $token"
            authManager.getUserProfile(fullToken).collect { state ->
                if (state is GetUserProfileState.Success) {
                    currentEmail = state.user.email
                }
            }
        }
    }

    fun refreshProductsAndPurchases() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val products = fetchTariffsOrDefault()
                _state.update {
                    it.copy(
                        products = products,
                        isLoading = false
                    )
                }
            } catch (t: Throwable) {
                t.sendToSentry("PurchasesViewModel", "refreshProductsAndPurchases")

                _event.tryEmit(BillingEvent.ShowError(t))
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Тарифы с сервера (`GET /v1/tariffs`) → доменные [Product]. При офлайне/
     * ошибке или пустом ответе — дефолтный набор, чтобы экран не был пустым.
     * Дефолты несут `code`, поэтому оплата корректно начислит срок и офлайн-путём.
     */
    private suspend fun fetchTariffsOrDefault(): List<Product> {
        return try {
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
            t.sendToSentry("PurchasesViewModel", "fetchTariffsOrDefault")
            defaultProducts()
        }
    }

    /** Резервные тарифы (совпадают с серверным сидом), если сеть недоступна. */
    private fun defaultProducts(): List<Product> = listOf(
        Product(name = "1 месяц", desc = "Новичок", sum = 69.0, code = "month", periodDays = 31, basePrice = 69.0),
        Product(name = "3 месяца", desc = "Эксперт", sum = 179.0, code = "quarter", periodDays = 93, basePrice = 179.0),
        Product(name = "1 год", desc = "Профи", sum = 599.0, code = "year", periodDays = 365, basePrice = 599.0),
    )

    fun restoreSubscribe() {
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            subscriptionHelper.restorePurchases(snackbarManager, token)
        }
    }

    fun onProductClick(product: Product) {
        // Переходный период: обе платёжки работают параллельно. USE_CKASSA
        // выбирает провайдера. Robokassa (прод) не трогаем.
        if (USE_CKASSA) {
            startCkassaCheckout(product)
            return
        }
        startRobokassaCheckout(product)
    }

    /**
     * Robokassa через сервер: сервер берёт цену/срок по коду тарифа, подписывает
     * платёж и (с галочкой) фиксирует согласие на автопродление. Пароли
     * мерчанта на клиенте не нужны. Ссылку открываем в браузере; после возврата
     * экран поллит статус подписки ([PaymentReturnChecker]).
     */
    private fun startRobokassaCheckout(product: Product) {
        if (product.code.isBlank()) {
            _event.tryEmit(BillingEvent.ShowError(Throwable("Тариф недоступен для оплаты")))
            return
        }
        if (_isStartingPayment.value) return
        val terms = _recurringTerms.value
        val withAutoRenew = _autoRenewChecked.value &&
            terms?.available == true && terms.tariffCode == product.code &&
            _recurringStatus.value?.enabled != true
        viewModelScope.launch {
            _isStartingPayment.value = true
            try {
                val userId = currentUserId()
                if (userId == null) {
                    _event.tryEmit(BillingEvent.ShowError(Throwable(message = "Отсутствует User ID")))
                    return@launch
                }
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
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
                _event.tryEmit(BillingEvent.OpenPaymentUrl(response.paymentUrl))
            } catch (e: RecurringConditionsChangedException) {
                // Условия автопродления обновились или услугу выключили —
                // показываем актуальный текст и просим отметить заново.
                termsTariffCode = null
                onPlanSelected(product)
                _event.tryEmit(
                    BillingEvent.ShowMessage("Условия автопродления обновились. Проверьте их и нажмите оплату ещё раз.")
                )
            } catch (t: Throwable) {
                t.sendToSentry("PurchasesViewModel", "startRobokassaCheckout")
                _event.tryEmit(BillingEvent.ShowError(t))
            } finally {
                _isStartingPayment.value = false
            }
        }
    }

    /**
     * user_id в оплате Robokassa — по нему вебхук начисляет срок. Сохранённый
     * id пуст, пока профиль нового аккаунта не загрузился; тогда берём его с
     * сервера, а не оплачиваем «в никуда».
     */
    private suspend fun currentUserId(): String? {
        secureTokenStorage.getUserIdFlow().first()?.takeIf { it.isNotBlank() }?.let { return it }
        val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            ?.takeIf { it.isNotBlank() } ?: return null
        val state = authManager.getUserProfile("Bearer $token")
            .first { it !is GetUserProfileState.Loading }
        val userId = (state as? GetUserProfileState.Success)?.user?.id
            ?.takeIf { it.isNotBlank() } ?: return null
        secureTokenStorage.saveUserId(userId)
        return userId
    }

    /**
     * CKassa: создаём инвойс на сервере (цена/срок берутся там по коду тарифа,
     * со скидками) и открываем ссылку оплаты. Платформу передаём, чтобы в
     * платежах был виден источник. После возврата экран поллит подписку.
     */
    private fun startCkassaCheckout(product: Product) {
        if (product.code.isBlank()) {
            _event.tryEmit(BillingEvent.ShowError(Throwable("Тариф недоступен для оплаты")))
            return
        }
        viewModelScope.launch {
            try {
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                val response = remoteRestApi.createCkassaCheckout(
                    token = "Bearer $token",
                    request = CkassaCheckoutRequest(
                        tariffCode = product.code,
                        platform = PAYMENT_PLATFORM,
                    ),
                )
                currentUserId()?.let { markPaymentStarted(it, product) }
                _event.tryEmit(BillingEvent.OpenPaymentUrl(response.paymentUrl))
            } catch (t: Throwable) {
                t.sendToSentry("PurchasesViewModel", "startCkassaCheckout")
                _event.tryEmit(BillingEvent.ShowError(t))
            }
        }
    }

    /**
     * Запомнить переход в оплату — [PaymentReturnChecker] подтвердит её при
     * любом возврате в приложение, даже если экран Покупок не доживёт.
     */
    private fun markPaymentStarted(userId: String, product: Product) {
        paymentReturnChecker.markStarted(
            userId = userId,
            periodBefore = _purchasesEndTime.value,
            periodDays = product.periodDays,
        )
    }

    /**
     * Проверка оплаты после возврата со страницы оплаты (Robokassa/CKassa). Сервер обновляет подписку асинхронным вебхуком, поэтому —
     * поллинг: `sdkConfirmed=true` → 10 попыток × 3 с, иначе 5 × 3 с. Сам
     * поллинг и «Платёж принят!» — в [PaymentReturnChecker] (глобально, на
     * любом экране); проверка по возврату в приложение идёт тем же заданием,
     * второй раз не запускается.
     *
     * Здесь — только диалоги экрана: «Получаем данные…» на время проверки;
     * не подтвердилось + SDK подтвердила → «Платёж обрабатывается»; не
     * подтвердилось без подтверждения SDK → «Оплата не завершена».
     */
    fun checkPaymentOnServer(sdkConfirmed: Boolean) {
        if (paymentCheckJob?.isActive == true) return
        paymentCheckJob = viewModelScope.launch {
            _showPaymentLoadingDialog.value = true
            val result = try {
                paymentReturnChecker.checkNow(attempts = if (sdkConfirmed) 10 else 5)
            } finally {
                _showPaymentLoadingDialog.value = false
            }
            when (result) {
                // «Платёж принят!» показан глобально; NO_PENDING — оплату уже
                // подтвердила проверка по возврату в приложение.
                PaymentReturnChecker.Result.PAID,
                PaymentReturnChecker.Result.NO_PENDING -> Unit
                PaymentReturnChecker.Result.NOT_CONFIRMED ->
                    if (sdkConfirmed) _showPaymentProcessingDialog.value = true
                    else _showPaymentFailedDialog.value = true
            }
        }
    }

    private var paymentCheckJob: Job? = null

    fun dismissPaymentFailedDialog() {
        _showPaymentFailedDialog.value = false
    }

    fun dismissPaymentProcessingDialog() {
        _showPaymentProcessingDialog.value = false
    }

    companion object {
        // Переключатель платёжного провайдера на переходный период.
        //  false — рабочая Robokassa (прод, старые клиенты).
        //  true  — новая CKassa (для теста/раскатки; сервер должен иметь
        //          CKASSA_ENABLED=true и ключи в .env).
        // Позже заменить на удалённый конфиг. Robokassa НЕ удаляем — обе
        // системы работают параллельно.
        const val USE_CKASSA = false

        // Источник оплаты: журнал платежей на сервере и страница возврата
        // (для приложения — «Вернуться в приложение», а не переход в PWA).
        const val PAYMENT_PLATFORM = "android"
    }
}
