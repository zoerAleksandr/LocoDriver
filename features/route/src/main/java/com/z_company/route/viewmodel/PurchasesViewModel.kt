package com.z_company.route.viewmodel

import android.util.Log
import com.z_company.core.sendToSentry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robokassa.library.models.Culture
import com.robokassa.library.models.PaymentMethod
import com.robokassa.library.models.Receipt
import com.robokassa.library.models.ReceiptItem
import com.robokassa.library.models.Tax
import com.robokassa.library.params.PaymentParams
import com.robokassa.library.pay.RobokassaPayLauncher
import com.z_company.core.ResultState
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.Product
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.GetUserProfileState
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.use_case.SubscriptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    data class StartPayment(val params: PaymentParams, val onlyChek: Boolean = false) :
        BillingEvent()
}

class PurchasesViewModel : ViewModel(), KoinComponent {
    private val MERCHANT_LOGIN = "LOCO_DRIVER_SHOP"
    private val PASSWORD_1 = "g1hybfLQChf4e508yRIX"
    private val PASSWORD_2 = "lGOmC8y1iNRbJ9M1fA3w"
    private val sharedPrefs: SharedPreferencesRepositories by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val snackbarManager: ISnackbarManager by inject()

    private val subscriptionHelper: SubscriptionHelper by inject()
    private val authManager: AuthManager by inject()
    private val settingManager: SettingManager by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()

    private val _state = MutableStateFlow(BillingState(isLoading = true))
    val state = _state.asStateFlow()

    private val _purchasesEndTime = MutableStateFlow(0L)
    val purchasesEndTime = _purchasesEndTime.asStateFlow()

    private val _showPaymentSuccessDialog = MutableStateFlow(false)
    val showPaymentSuccessDialog = _showPaymentSuccessDialog.asStateFlow()

    private val _showPaymentLoadingDialog = MutableStateFlow(false)
    val showPaymentLoadingDialog = _showPaymentLoadingDialog.asStateFlow()

    private val _showPaymentFailedDialog = MutableStateFlow(false)
    val showPaymentFailedDialog = _showPaymentFailedDialog.asStateFlow()

    private val _event = MutableSharedFlow<BillingEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val event = _event.asSharedFlow()
    var currentEmail: String = ""

    init {
        loadDateConverter()
        refreshProductsAndPurchases()
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
                val products = listOf(
                    Product(
                        name = "1 месяц",
                        desc = "Новичек",
                        sum = 69.0
                    ),
                    Product(
                        name = "3 месяца",
                        desc = "Эксперт",
                        sum = 179.0
                    ),
                    Product(
                        name = "1 год",
                        desc = "Профи",
                        sum = 599.0
                    )
                )
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

            settingsUseCase.getUserSettingFlow().collect { setting ->
                val purchasesEndTime = setting.subscriptionPeriod
                _purchasesEndTime.value = purchasesEndTime
            }
        }
    }

    fun restoreSubscribe() {
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            subscriptionHelper.restorePurchases(snackbarManager, token)
        }
    }

    fun onProductClick(product: Product) {
        viewModelScope.launch {
//            val opKey = sharedPrefs.getOPKeyRobokassa()
            val userId = secureTokenStorage.getUserIdFlow().first()
            if (userId != null) {
                val paymentParams =
                    createPaymentParams(product = product, opKey = null, userId = userId)
                _event.tryEmit(BillingEvent.StartPayment(paymentParams))
            } else {
                _event.tryEmit(BillingEvent.ShowError(Throwable(message = "Отсутствует User ID")))
            }
        }
    }

    /**
     * Создаёт PaymentParams для Robokassa.
     * @param product Продукт для оплаты.
     * @param opKey Токен сохранённой карты (null для первой оплаты).
     * @return PaymentParams.
     */
    private fun createPaymentParams(
        product: Product,
        opKey: String?,
        userId: String
    ): PaymentParams {
        return PaymentParams().setParams {
            orderParams {
                invoiceId = System.currentTimeMillis().toInt()
                orderSum = product.sum
                description = product.desc
                receipt = Receipt(
                    items = listOf(
                        ReceiptItem(
                            name = product.name,
                            sum = product.sum,
                            quantity = 1,
                            paymentMethod = PaymentMethod.FULL_PAYMENT,
                            tax = Tax.NONE
                        )
                    )
                )
                if (opKey != null) {
                    // Для оплаты сохранённой картой
                    token = opKey
                }
            }
            customerParams {
                culture = Culture.RU
                email = currentEmail
            }
            viewParams {
                toolbarText = "Оплата ${product.name}"
            }
            shp = mapOf("user_id" to userId)
        }.also {
            it.setCredentials(
                MERCHANT_LOGIN,
                PASSWORD_1,
                PASSWORD_2,
                "https://www.rustore.ru/catalog/app/com.z_company.loco_driver"
            )
        }
    }

    fun handlePaymentSuccess(success: RobokassaPayLauncher.Success?) {
        viewModelScope.launch {
            val previousEndTime = _purchasesEndTime.value
            _showPaymentLoadingDialog.value = true
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val result = subscriptionHelper.restorePurchases(null, token)
            _showPaymentLoadingDialog.value = false
            if (result is ResultState.Success) {
                val updatedSetting = settingsUseCase.getUserSettingFlow().first()
                _purchasesEndTime.value = updatedSetting.subscriptionPeriod
                if (updatedSetting.subscriptionPeriod > previousEndTime) {
                    _showPaymentSuccessDialog.value = true
                } else {
                    _showPaymentFailedDialog.value = true
                }
            } else {
                _showPaymentFailedDialog.value = true
            }
        }
    }

    fun dismissPaymentSuccessDialog() {
        _showPaymentSuccessDialog.value = false
    }

    fun dismissPaymentFailedDialog() {
        _showPaymentFailedDialog.value = false
    }

    // Новое: Метод для эмиссии события StartPayment.
// Для чего: Чтобы из MainViewModel (при возврате) или из onProductClick можно было эмитировать событие для запуска launcher в UI с нужным onlyCheck. Это упрощает обработку возврата без дублирования кода.
    fun emitStartPayment(params: PaymentParams, onlyCheck: Boolean) {
        _event.tryEmit(BillingEvent.StartPayment(params, onlyCheck))
    }
}