package com.z_company.route.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.SubscriptionDetails
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.ru_store_api.DTO.JWEAnswerDTO
import com.z_company.repository.ru_store_api.DTO.SubscriptionAnswerDTO
import com.z_company.route.Const.LOCO_DRIVER_ANNUAL_SUBSCRIPTION
import com.z_company.route.Const.LOCO_DRIVER_MONTHLY_SUBSCRIPTION
import com.z_company.route.R
import com.z_company.use_case.RuStoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.model.product.Product
import ru.rustore.sdk.billingclient.model.purchase.PaymentResult
import ru.rustore.sdk.billingclient.model.purchase.PurchaseState

class PurchasesViewModel : ViewModel(), KoinComponent {
    private val billingClient: RuStoreBillingClient by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val ruStoreUseCase: RuStoreUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private val availableProductIds = listOf(
        LOCO_DRIVER_MONTHLY_SUBSCRIPTION,
        LOCO_DRIVER_ANNUAL_SUBSCRIPTION,
    )

    private val _state = MutableStateFlow(BillingState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<BillingEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val event = _event.asSharedFlow()

    init {
        getProducts()
    }

    fun onProductClick(product: Product) {
        purchaseProduct(product)
    }

    private fun getProducts() {
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch(Dispatchers.IO) {
            val setting = settingsUseCase.getUserSettingFlow().first()
            _state.update {
                it.copy(
                    dateAndTimeConverter = DateAndTimeConverter(setting)
                )
            }

            runCatching {
                // получил данные по доступным продуктам
                val products: MutableList<Product> = billingClient.products.getProducts(
                    productIds = availableProductIds
                ).await().toMutableList()
                val purchases = billingClient.purchases.getPurchases().await()

                val subscriptions = mutableListOf<SubscriptionDetails>()
                // получил данные по активным подпискам
                ruStoreUseCase.getJWE()
                    .onSuccess { answer ->
                        purchases.forEachIndexed { index, purchase ->
                            val purchaseId = purchase.purchaseId

                            if (purchase.purchaseState == PurchaseState.CONFIRMED) {
                                // получил детали по активным подпискам
                                val callback = object : Callback<SubscriptionAnswerDTO> {
                                    override fun onResponse(
                                        p0: Call<SubscriptionAnswerDTO>,
                                        p1: Response<SubscriptionAnswerDTO>
                                    ) {
                                        subscriptions.add(
                                            SubscriptionDetails(
                                                productId = purchaseId ?: "",
                                                title = products.find { it.productId == purchase.productId }?.title
                                                    ?: "",
                                                description = products.find { it.productId == purchase.productId }?.description
                                                    ?: "",
                                                expiryTime = p1.body()?.expiryTimeMillis
                                                    ?: "",
                                                priceLabel = products.find { it.productId == purchase.productId }?.priceLabel
                                                    ?: ""
                                            )
                                        )

                                        // удаляем из списка продуктов те, которые уже куплены
                                        products.removeIf { product ->
                                            product.productId == purchase.productId
                                        }

                                        if (index + 1 == products.size) {
                                            // обновляем UI
                                            _state.update {
                                                it.copy(
                                                    products = products,
                                                    subscriptions = subscriptions,
                                                    isLoading = false,
                                                )
                                            }
                                        }
                                    }

                                    override fun onFailure(
                                        p0: Call<SubscriptionAnswerDTO>,
                                        p1: Throwable
                                    ) {
                                        _event.tryEmit(BillingEvent.ShowError(p1))
                                        _state.value = _state.value.copy(isLoading = false)
                                    }
                                }

                                ruStoreUseCase.getSubscriptionDetails(
                                    jweToken = answer.body.jwe,
                                    subscriptionId = purchase.productId,
                                    subscriptionToken = purchase.subscriptionToken ?: "",
                                    callback = callback
                                )
                            }

                            if (purchaseId != null) {
                                when (purchase.purchaseState) {
                                    PurchaseState.CREATED, PurchaseState.INVOICE_CREATED -> {
                                        billingClient.purchases.deletePurchase(purchaseId)
                                            .await()
                                    }

                                    PurchaseState.PAID -> {
                                        billingClient.purchases.confirmPurchase(purchaseId)
                                            .await()
                                    }

                                    else -> Unit
                                }
                            }

                            if (purchase.developerPayload?.isNotEmpty() == true) {
                                Log.w(
                                    "RuStoreBillingClient",
                                    "DeveloperPayloadInfo: ${purchase.developerPayload}"
                                )
                            }
                        }
                        if (purchases.isEmpty()) {
                            _state.update {
                                it.copy(
                                    products = products,
                                    subscriptions = subscriptions,
                                    isLoading = false,
                                )
                            }
                        }
                    }
                    .onFailure { throwable ->
                        _event.tryEmit(BillingEvent.ShowError(throwable))
                        _state.value = _state.value.copy(isLoading = false)
                    }
            }.onFailure { throwable ->
                _event.tryEmit(BillingEvent.ShowError(throwable))
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }


    private fun purchaseProduct(product: Product) {
        val developerPayload = "your_developer_payload"
        billingClient.purchases.purchaseProduct(
            productId = product.productId,
            developerPayload = developerPayload
        )
            .addOnSuccessListener { paymentResult ->
                handlePaymentResult(paymentResult)
            }
            .addOnFailureListener { throwable ->
                _event.tryEmit(BillingEvent.ShowError(throwable))
                setErrorStateOnFailure(throwable)
            }
    }

    private fun handlePaymentResult(paymentResult: PaymentResult) {
        when (paymentResult) {
            is PaymentResult.Failure -> {
                paymentResult.purchaseId?.let { deletePurchase(it) }
            }

            is PaymentResult.Success -> {
                setSubscriptionExpiration(paymentResult.productId, paymentResult.subscriptionToken)
            }

            else -> Unit
        }
    }

    private fun setSubscriptionExpiration(productId: String, subscriptionToken: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ruStoreUseCase.getJWE()
                    .onSuccess { value: JWEAnswerDTO ->
                        val callback = object : Callback<SubscriptionAnswerDTO> {
                            override fun onResponse(
                                p0: Call<SubscriptionAnswerDTO>,
                                p1: Response<SubscriptionAnswerDTO>
                            ) {
                                val oldEndPeriod =
                                    sharedPreferenceStorage.getSubscriptionExpiration()
                                p1.body()?.expiryTimeMillis?.toLongOrNull()?.let { expiryTime ->
                                    if (expiryTime > oldEndPeriod) {
                                        sharedPreferenceStorage.setSubscriptionExpiration(expiryTime)
                                    }
                                }
                            }

                            override fun onFailure(p0: Call<SubscriptionAnswerDTO>, p1: Throwable) {
                                _event.tryEmit(BillingEvent.ShowError(p1))
                            }

                        }
                        ruStoreUseCase.getSubscriptionDetails(
                            jweToken = value.body.jwe,
                            subscriptionId = productId,
                            subscriptionToken = subscriptionToken ?: "",
                            callback = callback
                        )
                    }
                    .onFailure {
                        _event.tryEmit(BillingEvent.ShowError(it))
                    }
            }
        }
    }


    private fun deletePurchase(purchaseId: String) {
        _state.value = _state.value.copy(
            isLoading = true,
            snackbarResId = R.string.billing_purchase_delete_in_progress
        )
        billingClient.purchases.deletePurchase(purchaseId)
            .addOnSuccessListener { response ->
                sharedPreferenceStorage.setSubscriptionExpiration(0L)
                _event.tryEmit(
                    BillingEvent.ShowDialog(
                        InfoDialogState(
                            titleRes = R.string.billing_product_deleted,
                            message = response.toString()
                        )
                    )
                )
                _state.value = _state.value.copy(isLoading = false)
            }
            .addOnFailureListener {
                setErrorStateOnFailure(it)
            }
    }

    private fun setErrorStateOnFailure(error: Throwable) {
        _event.tryEmit(BillingEvent.ShowError(error))
        _state.value = _state.value.copy(isLoading = false)
    }
}