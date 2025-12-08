package com.z_company.route.viewmodel

import androidx.compose.animation.core.snap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.ui.snackbar.SnackbarManagerImpl
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.route.Const.LOCO_DRIVER_ANNUAL_SUBSCRIPTION
import com.z_company.route.Const.LOCO_DRIVER_MONTHLY_SUBSCRIPTION
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
import ru.rustore.sdk.pay.RuStorePayClient
import ru.rustore.sdk.pay.model.Product
import ru.rustore.sdk.pay.model.ProductId
import ru.rustore.sdk.pay.model.ProductPurchaseParams
import ru.rustore.sdk.pay.model.ProductType
import ru.rustore.sdk.pay.model.SubscriptionPurchase
import ru.rustore.sdk.pay.model.SubscriptionPurchaseStatus

data class BillingState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val activeExpirations: Map<String, Long> = emptyMap(),
    val dateAndTimeConverter: DateAndTimeConverter? = null
)

sealed class BillingEvent {
    data class ShowError(val error: Throwable) : BillingEvent()
}

class PurchasesViewModel : ViewModel(), KoinComponent {

    private val ruStorePayClient: RuStorePayClient by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val subscriptionHelper: SubscriptionHelper by inject()
    private val snackbarManager: ISnackbarManager by inject()

    private val availableProductIds = listOf<ProductId>(
        ProductId(LOCO_DRIVER_MONTHLY_SUBSCRIPTION),
        ProductId(LOCO_DRIVER_ANNUAL_SUBSCRIPTION),
    )

    private val _state = MutableStateFlow(BillingState(isLoading = true))
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<BillingEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val event = _event.asSharedFlow()

    init {
        loadDateConverter()
        refreshProductsAndPurchases()
    }

    private fun loadDateConverter() {
        viewModelScope.launch {
            val setting = settingsUseCase.getUserSettingFlow().first()
            _state.update { it.copy(dateAndTimeConverter = DateAndTimeConverter(setting)) }
        }
    }

    fun refreshProductsAndPurchases() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Продукты
                val products = ruStorePayClient
                    .getProductInteractor()
                    .getProducts(availableProductIds)
                    .await()

                // 2. Все покупки
                val purchases = ruStorePayClient
                    .getPurchaseInteractor()
                    .getPurchases(ProductType.SUBSCRIPTION) // можно добавить productType = ProductType.SUBSCRIPTION если нужно
                    .await()

                // 3. Очистка/конфирм неоконченных покупок
//                purchases.forEach { purchase ->
//                    when (purchase.status) {
//                        PurchaseStatus.CREATED,
//                        PurchaseStatus.INVOICE_CREATED -> {
//                            ruStorePayClient.getPurchaseInteractor()
//                                .deletePurchase(purchase.purchaseId)
//                                .await()
//                        }
//                        PurchaseStatus.PAID -> {
//                            ruStorePayClient.getPurchaseInteractor()
//                                .confirmPurchase(purchase.purchaseId)
//                                .await()
//                        }
//                        else -> Unit
//                    }
//                }

                // 4. Активные подписки
                val activeSubs = purchases
                    .filterIsInstance<SubscriptionPurchase>()
                    .filter { it.status == SubscriptionPurchaseStatus.ACTIVE }

                val expirationMap = activeSubs.associate { sub ->
                    sub.productId.value to sub.expirationDate.time
                }

                // Сохраняем самое позднее окончание подписки (для синхронизации и Profile)
//                val maxExpiration = expirationMap.values.maxOrNull() ?: 0L
//                sharedPrefs.setSubscriptionExpiration(maxExpiration)

                _state.update {
                    it.copy(
                        products = products,
                        activeExpirations = expirationMap,
                        isLoading = false
                    )
                }
            } catch (t: Throwable) {
                _event.tryEmit(BillingEvent.ShowError(t))
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun restoreSubscription() {
        viewModelScope.launch(Dispatchers.IO) {
            subscriptionHelper.restorePurchasesSuspend(snackbarManager = snackbarManager)
        }
    }

    fun onProductClick(product: Product) {
        viewModelScope.launch {
            ruStorePayClient.getPurchaseInteractor()
                .purchase(ProductPurchaseParams(productId = product.productId))
                .addOnSuccessListener {
                    // После покупки сразу обновляем состояние
                    refreshProductsAndPurchases()
                }
                .addOnFailureListener { throwable ->
                    _event.tryEmit(BillingEvent.ShowError(throwable))
                }
        }
    }
}