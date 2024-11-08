package com.z_company.route.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.repository.ru_store_api.DTO.SubscriptionAnswerDTO
import com.z_company.route.Const.LOCO_DRIVER_ANNUAL_SUBSCRIPTION
import com.z_company.route.Const.LOCO_DRIVER_MONTHLY_SUBSCRIPTION
import com.z_company.route.R
import com.z_company.route.extention.getEndTimeSubscription
import com.z_company.use_case.RuStoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
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
import ru.rustore.sdk.core.util.RuStoreUtils.isRuStoreInstalled
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
class PurchasesViewModel : ViewModel(), KoinComponent {
    private val billingClient: RuStoreBillingClient by inject()
    private val sharedPreferenceStorage: SharedPreferenceStorage by inject()
    private val ruStoreUseCase: RuStoreUseCase by inject()

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
        // TODO Добавить проверку наличия ruStore isRuStoreInstalled(context)

        val callback = object : Callback<SubscriptionAnswerDTO> {
            override fun onResponse(
                p0: Call<SubscriptionAnswerDTO>,
                p1: Response<SubscriptionAnswerDTO>
            ) {
                Log.d("ZZZ", "p1 onResponse ${p1.body()}")
            }

            override fun onFailure(p0: Call<SubscriptionAnswerDTO>, p1: Throwable) {
                Log.d("ZZZ", "p1 onFailure $p1")
            }
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ruStoreUseCase.getJWE()
                    .onSuccess { answer ->
                        val purchases = billingClient.purchases.getPurchases().await()
                        purchases.forEach { purchase ->
                            Log.d("ZZZ", "purchases - ${purchases}")
                            ruStoreUseCase.test(
                                jweToken = answer.body.jwe,
                                subscriptionId = purchase.productId,
                                subscriptionToken = purchase.subscriptionToken ?: "",
                                callback = callback
                            )
                        }
                    }
                    .onFailure {
                        Log.d("ZZZ", "throwable - $it")
                    }
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    val products = billingClient.products.getProducts(
                        productIds = availableProductIds
                    ).await()

                    val purchases = billingClient.purchases.getPurchases().await()
                    val productIds: MutableList<String> = mutableListOf()
                    purchases.forEach { purchase ->
                        val purchaseId = purchase.purchaseId
                        productIds.add(purchase.productId)

                        purchase.productId
                        if (purchase.developerPayload?.isNotEmpty() == true) {
                            Log.w(
                                "RuStoreBillingClient",
                                "DeveloperPayloadInfo: ${purchase.developerPayload}"
                            )
                        }
                        if (purchaseId != null) {
                            when (purchase.purchaseState) {
                                PurchaseState.CREATED, PurchaseState.INVOICE_CREATED -> {
                                    billingClient.purchases.deletePurchase(purchaseId).await()
                                }

                                PurchaseState.PAID -> {
                                    billingClient.purchases.confirmPurchase(purchaseId).await()
                                }

                                else -> Unit
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                products = products,
                                purchases = purchases,
                                isLoading = false,
                                boughtProductsId = productIds
                            )
                        }
                    }
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
                setSubscriptionExpiration(paymentResult.purchaseId)
            }

            else -> Unit
        }
    }

    private fun setSubscriptionExpiration(purchaseId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val purchase = billingClient.purchases.getPurchaseInfo(purchaseId).await()
                val productId = purchase.productId
                val product = billingClient.products.getProducts(listOf(productId)).await()
                val periodInDays = product.first().subscription?.subscriptionPeriod?.days
                if (periodInDays != null) {
                    val endPeriodInLong =
                        Calendar.getInstance().timeInMillis + (86_400_000L * periodInDays.toLong())
                    withContext(Dispatchers.Main) {
                        val oldEndPeriod = sharedPreferenceStorage.getSubscriptionExpiration()
                        if (endPeriodInLong > oldEndPeriod) {
                            sharedPreferenceStorage.setSubscriptionExpiration(endPeriodInLong)
                        }
                    }
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