package com.z_company.route.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.ui.snackbar.ISnackbarManager
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
import ru.rustore.sdk.pay.model.RuStorePaymentException.ProductPurchaseCancelled
import ru.rustore.sdk.pay.model.RuStorePaymentException.ProductPurchaseException
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
                    .getPurchases()
                    .await()

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

    fun restoreSubscribe() {
        viewModelScope.launch(Dispatchers.IO) {
            subscriptionHelper.restorePurchases(snackbarManager)
        }
    }

    fun onProductClick(product: Product) {
        viewModelScope.launch {
            val purchaseResult =
                subscriptionHelper.purchaseProductSuspend(productId = product.productId.value)
            when (purchaseResult) {
                is ResultState.Success -> {
                    if (purchaseResult.data == -1L) {
                        snackbarManager.show("Подписка успешно оформлена!")
                        subscriptionHelper.restorePurchases()
                    } else {
                        val text =
                            state.value.dateAndTimeConverter?.getDateAndTime(purchaseResult.data)
                                ?: ""
                        snackbarManager.show("Подписка успешно оформлена! Срок до $text")
                        Log.d("zzz", "Подписка успешно оформлена! Срок до $text")
                    }
                }

                is ResultState.Error -> {
                    val error = purchaseResult.entity.throwable
                    when (error) {
                        is ProductPurchaseCancelled -> {
                            snackbarManager.show("Покупка отменена пользователем.")
                            Log.d("zzz", "ProductPurchaseCancelled ${purchaseResult.entity}")
                        }

                        is ProductPurchaseException -> {
                            Log.d("zzz", "ProductPurchaseException ${purchaseResult.entity}")
                        }

                        else -> {
                            snackbarManager.show("Ошибка ${purchaseResult.entity}")
                            Log.d("zzz", "Ошибка ${purchaseResult.entity}")
                        }
                    }
                }

                is ResultState.Loading -> {

                }
            }
//            val result = ruStorePayClient.getPurchaseInteractor()
//                .purchase(ProductPurchaseParams(productId = product.productId))
//                .toSuspendResult()
//
//            result.fold(
//                onSuccess = { purchaseResult ->
//                    val purchaseId = purchaseResult.purchaseId
//                    viewModelScope.launch {
//                        val saveTimeResult =
//                            subscriptionHelper.setSubscriptionExpirationFromServer(purchaseId.value)
//                        when (saveTimeResult) {
//                            is ResultState.Success -> {
//
//                            }
//                            is ResultState.Error -> {
//
//                            }
//                            is ResultState.Loading -> {
//
//                            }
//                        }
//                    }
////                    val purchase = ruStorePayClient
////                        .getPurchaseInteractor()
////                        .getPurchase(purchaseId)
////                        .await()
//
////                    val subscriptionPurchase = purchase as SubscriptionPurchase
//
////                    val time = subscriptionPurchase.expirationDate.time
////                    sharedPrefs.setSubscriptionExpiration(time)
////                    val text = state.value.dateAndTimeConverter?.getDateAndTime(time) ?: ""
//
//                    snackbarManager.show("Подписка успешно оформлена! Срок до $purchaseId")
//
//                    Log.d("zzz", "Подписка успешно оформлена! Срок до $purchaseId")
//                },
//                onFailure = { throwable ->
//                    when (throwable) {
//                        is RuStorePaymentException.ProductPurchaseException -> {
//                            Log.d("zzz", "ProductPurchaseException $throwable")
//                            _event.tryEmit(BillingEvent.ShowError(throwable))
//                        }
//
//                        is RuStorePaymentException.ProductPurchaseCancelled -> {
//                            Log.d("zzz", "ProductPurchaseCancelled $throwable")
//                            snackbarManager.show("Вы не закончили оформление подписки.")
//                            _event.tryEmit(BillingEvent.ShowError(throwable))
//                        }
//
//                        else -> {
//                            Log.d("zzz", "Other Error $throwable")
//                            _event.tryEmit(BillingEvent.ShowError(throwable))
//                        }
//                    }
//                }
//            )
//                .addOnSuccessListener {
//                    Log.d("zzz", "addOnSuccessListener $it")
//                    // После покупки сразу обновляем состояние
//                    refreshProductsAndPurchases()
//                }
//                .addOnFailureListener { throwable ->
//                    Log.d("zzz", "addOnFailureListener $throwable")
//                    _event.tryEmit(BillingEvent.ShowError(throwable))
//                }
        }
    }
}