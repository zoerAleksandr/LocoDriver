package com.z_company.shared.platform

import com.z_company.core.ResultState
import com.z_company.domain.entities.Product
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.SettingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Android BillingProvider backed by Robokassa payment SDK.
 *
 * Payment flow:
 * 1. User clicks product → purchase(product) called
 * 2. This class creates PaymentParams and emits to paymentEvent
 * 3. Android PurchasesDestination observes paymentEvent and launches RobokassaPayLauncher
 * 4. On result, Destination calls handlePaymentResult()
 * 5. This class restores subscription from server
 *
 * Note: RobokassaPayLauncher requires Android Activity context,
 * so the actual SDK call stays in the composable layer (PurchasesDestination).
 */
class AndroidBillingProvider(
    private val secureTokenStorage: SecureTokenStorage,
    private val settingsUseCase: SettingsUseCase,
    private val settingManager: SettingManager,
) : BillingProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val MERCHANT_LOGIN = "LOCO_DRIVER_SHOP"
        private const val PASSWORD_1 = "g1hybfLQChf4e508yRIX"
        private const val PASSWORD_2 = "lGOmC8y1iNRbJ9M1fA3w"
        private const val RESULT_URL = "https://www.rustore.ru/catalog/app/com.z_company.loco_driver"
    }

    private val _products = MutableStateFlow(
        listOf(
            Product(name = "1 месяц", desc = "Новичек", sum = 69.0),
            Product(name = "3 месяца", desc = "Эксперт", sum = 179.0),
            Product(name = "1 год", desc = "Профи", sum = 599.0),
        )
    )
    override val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _purchasesEndTime = MutableStateFlow(0L)
    override val purchasesEndTime: StateFlow<Long> = _purchasesEndTime.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Android-specific: event to trigger RobokassaPayLauncher from composable.
     * Contains (product, merchantLogin, password1, password2, resultUrl, userId, email).
     */
    data class PaymentRequest(
        val product: Product,
        val merchantLogin: String,
        val password1: String,
        val password2: String,
        val resultUrl: String,
        val userId: String,
        val email: String,
    )

    private val _paymentEvent = MutableSharedFlow<PaymentRequest>(extraBufferCapacity = 1)
    val paymentEvent: SharedFlow<PaymentRequest> = _paymentEvent.asSharedFlow()

    private val _currentEmail = MutableStateFlow("")

    init {
        // Load subscription end time
        scope.launch {
            settingsUseCase.getUserSettingFlow().onEach { settings ->
                _purchasesEndTime.value = settings.subscriptionPeriod
            }.launchIn(scope)
        }
        // Load email for receipts
        scope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (!token.isNullOrBlank()) {
                try {
                    val state = settingManager.getUserSettingFromRemote("Bearer $token")
                        .first { it !is ResultState.Loading }
                    // Email will be set from profile if available
                } catch (_: Exception) {}
            }
        }
    }

    override fun purchase(product: Product) {
        scope.launch {
            val userId = secureTokenStorage.getUserIdFlow().first()
            if (userId != null) {
                _paymentEvent.emit(
                    PaymentRequest(
                        product = product,
                        merchantLogin = MERCHANT_LOGIN,
                        password1 = PASSWORD_1,
                        password2 = PASSWORD_2,
                        resultUrl = RESULT_URL,
                        userId = userId,
                        email = _currentEmail.value,
                    )
                )
            }
        }
    }

    override fun restorePurchases() {
        scope.launch {
            _isLoading.value = true
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) {
                _isLoading.value = false
                return@launch
            }
            try {
                val settingState = settingManager.getUserSettingFromRemote("Bearer $token")
                    .first { it !is ResultState.Loading }
                if (settingState is ResultState.Success) {
                    val purchaseTimeEnd = settingState.data.subscriptionPeriod
                    if (purchaseTimeEnd > Clock.System.now().toEpochMilliseconds()) {
                        settingsUseCase.updateSubscriptionPeriod(purchaseTimeEnd)
                            .first { it !is ResultState.Loading }
                        _purchasesEndTime.value = purchaseTimeEnd
                    }
                }
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    override fun refresh() {
        restorePurchases()
    }

    /**
     * Called from PurchasesDestination after Robokassa payment result.
     * Restores subscription from server to update local state.
     */
    fun handlePaymentResult(success: Boolean) {
        scope.launch {
            restorePurchases()
        }
    }

    /** Update email for receipts (called from profile screen). */
    fun setEmail(email: String) {
        _currentEmail.value = email
    }
}

actual fun createBillingProvider(): BillingProvider {
    throw IllegalStateException("Use Koin to provide BillingProvider")
}
