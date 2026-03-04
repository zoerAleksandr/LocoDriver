package com.z_company.shared.platform

import com.z_company.core.ResultState
import com.z_company.domain.entities.Product
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.SettingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * iOS BillingProvider.
 *
 * Shows the same product list and subscription status.
 * Purchase action opens a web URL (Robokassa web checkout).
 * StoreKit integration can be added later.
 */
class IosBillingProvider(
    private val secureTokenStorage: SecureTokenStorage,
    private val settingsUseCase: SettingsUseCase,
    private val settingManager: SettingManager,
    private val platformActions: PlatformActions,
) : BillingProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    init {
        scope.launch {
            settingsUseCase.getUserSettingFlow().onEach { settings ->
                _purchasesEndTime.value = settings.subscriptionPeriod
            }.launchIn(scope)
        }
    }

    override fun purchase(product: Product) {
        // Open web payment URL for iOS users
        platformActions.openUrl("https://www.rustore.ru/catalog/app/com.z_company.loco_driver")
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
}

actual fun createBillingProvider(): BillingProvider {
    throw IllegalStateException("Use Koin to provide BillingProvider")
}
