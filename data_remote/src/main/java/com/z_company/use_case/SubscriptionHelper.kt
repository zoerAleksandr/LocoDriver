package com.z_company.use_case

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.domain.repositories.SharedPreferencesRepositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.model.purchase.PurchaseState
import ru.rustore.sdk.pay.RuStorePayClient
import ru.rustore.sdk.pay.model.DeveloperPayload
import ru.rustore.sdk.pay.model.PreferredPurchaseType
import ru.rustore.sdk.pay.model.Product
import ru.rustore.sdk.pay.model.ProductId
import ru.rustore.sdk.pay.model.ProductPurchaseParams
import ru.rustore.sdk.pay.model.Purchase
import ru.rustore.sdk.pay.model.PurchaseAvailabilityResult
import ru.rustore.sdk.pay.model.ProductPurchaseResult
import ru.rustore.sdk.pay.model.PurchaseId
import ru.rustore.sdk.pay.model.RuStorePaymentException.ProductPurchaseCancelled
import ru.rustore.sdk.pay.model.RuStorePaymentException.ProductPurchaseException
import ru.rustore.sdk.pay.model.SubscriptionPurchase
import java.util.Calendar
import java.util.Calendar.getInstance
//import ru.rustore.sdk.pay.model.SubscriptionPurchase
//import ru.rustore.sdk.pay.model.SubscriptionPurchaseStatus
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.getValue
import kotlin.time.Duration.Companion.seconds
import ru.rustore.sdk.billingclient.model.purchase.Purchase as PurchaseBillingClient

class SubscriptionHelper() : KoinComponent {
    private val payClient: RuStorePayClient by inject()
    private val sharedPreferences: SharedPreferencesRepositories by inject()
    private val billingClient: RuStoreBillingClient by inject()
    private val ruStoreUseCase: RuStoreUseCase by inject()

    // 1. Проверка доступности покупок
    suspend fun checkPurchasesAvailabilitySuspend(): ResultState<PurchaseAvailabilityResult> =
        withContext(Dispatchers.IO) {
            try {
                // Используем suspendCoroutine для асинхронной операции
                suspendCoroutine<ResultState<PurchaseAvailabilityResult>> { continuation ->
                    payClient.getPurchaseInteractor().getPurchaseAvailability()
                        .addOnSuccessListener { result ->
                            when (result) {
                                is PurchaseAvailabilityResult.Available -> {
                                    // Process purchases available
                                    continuation.resume(ResultState.Success(result))
                                }

                                is PurchaseAvailabilityResult.Unavailable -> {
                                    // Process purchases unavailable
                                    val exception = result.cause
                                    continuation.resume(ResultState.Error(ErrorEntity(exception)))
                                }
                            }
                        }
                        .addOnFailureListener { throwable ->
                            // Process failure error
                            continuation.resume(ResultState.Error(ErrorEntity(throwable)))
                        }
                }
            } catch (t: Throwable) {
                ResultState.Error(ErrorEntity(t))
            }
        }

    // 2. Получение продуктов и покупок
    suspend fun fetchProductsAndPurchases(productIds: List<String>): ResultState<Pair<List<Product>, List<Purchase>>> =
        withContext(Dispatchers.IO) {
            try {
                // Используем coroutineScope для параллельных асинхронных вызовов
                val prodIds = productIds.map {
                    ProductId(it)
                }
                coroutineScope {
                    val productsDeferred = async {
                        suspendCoroutine<List<Product>> { continuation ->
                            payClient.getProductInteractor().getProducts(prodIds)
                                .addOnSuccessListener { products: List<Product> ->
                                    // Логика работы со списком продуктов
                                    continuation.resume(products)
                                }
                                .addOnFailureListener { throwable: Throwable ->
                                    // Обработка ошибки
                                    continuation.resumeWithException(throwable)
                                }
                        }
                    }

                    val purchasesDeferred = async {
                        suspendCoroutine<List<Purchase>> { continuation ->
                            payClient.getPurchaseInteractor().getPurchases()
                                .addOnSuccessListener { continuation.resume(it) }
                                .addOnFailureListener { continuation.resumeWithException(it) }
                        }
                    }

                    val products = productsDeferred.await()
                    val purchases = purchasesDeferred.await()

                    ResultState.Success(products to purchases)
                }
            } catch (t: Throwable) {
                ResultState.Error(ErrorEntity(t))
            }
        }

    // 3. Запуск покупки
    suspend fun purchaseProductSuspend(
        productId: String,
        developerPayload: String = ""
    ): ResultState<ProductPurchaseResult> = withContext(Dispatchers.IO) {
        try {
            suspendCoroutine<ResultState<ProductPurchaseResult>> { continuation ->
                payClient.getPurchaseInteractor().purchase(
                    params = ProductPurchaseParams(
                        productId = ProductId(productId),
                        developerPayload = DeveloperPayload(developerPayload),
                    ),
                    // одностадийная оплата PreferredPurchaseType.ONE_STEP
                    // двухстадийная оплата PreferredPurchaseType.TWO_STEP
                    // при двухстадийной оплате:
                    // RuStorePayClient.getPurchaseInteractor().confirmTwoStepPurchase() - подтверждение
                    // RuStorePayClient.getPurchaseInteractor().cancelTwoStepPurchase() - отмена
                    preferredPurchaseType = PreferredPurchaseType.ONE_STEP
                ).addOnSuccessListener { paymentResult ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val id = paymentResult.productId
                        val expirationResult = setSubscriptionExpirationFromServer(id.toString())

                        when (expirationResult) {
                            is ResultState.Success -> {
                                continuation.resume(ResultState.Success(paymentResult))
                            }

                            is ResultState.Error -> {
                                continuation.resume(
                                    ResultState.Error(
                                        ErrorEntity(Throwable("Failed to set subscription expiration: ${expirationResult.entity.message}"))
                                    )
                                )
                            }

                            else -> {}
                        }
                    }


                    continuation.resume(ResultState.Success(paymentResult))
                }.addOnFailureListener { throwable ->
                    when (throwable) {
                        // общая ошибка
                        is ProductPurchaseException -> {
                            continuation.resume(ResultState.Error(ErrorEntity(throwable)))
                        }
                        // отмена пользователем
                        is ProductPurchaseCancelled -> {
                            continuation.resume(ResultState.Error(ErrorEntity(throwable)))
                        }
                    }
                    continuation.resume(ResultState.Error(ErrorEntity(throwable)))
                }
            }
        } catch (t: Throwable) {
            ResultState.Error(ErrorEntity(t))
        }
    }

    // 4. Удаление покупки
//    suspend fun deletePurchaseSuspend(purchaseId: String): ResultState<Unit> =
//        withContext(Dispatchers.IO) {
//            try {
//                suspendCoroutine { continuation ->
//                    billingClient.purchases.deletePurchase(purchaseId)
//                        .addOnSuccessListener {
//                            sharedPreferences.setSubscriptionExpiration(0L)
//                            continuation.resume(ResultState.Success(Unit))
//                        }
//                        .addOnFailureListener { throwable ->
//                            continuation.resume(ResultState.Error(ErrorEntity(throwable)))
//                        }
//                }
//            } catch (t: Throwable) {
//                ResultState.Error(ErrorEntity(t))
//            }
//        }

    // 5. Восстановление подписок
    suspend fun restorePurchases(snackbarManager: ISnackbarManager? = null): ResultState<Unit> {
        snackbarManager?.show(
            message = "Выполняем поиск активной подписки...",
        )
        val result2 = restoreSubscribeBillingClient(snackbarManager)
        if (result2 is ResultState.Error) {
            return result2
        }

        val result1 = restoreSubscribePaySDK(snackbarManager)
        if (result1 is ResultState.Error) {
            return result1
        }

        val maxExpiration2 = if (result2 is ResultState.Success) result2.data else 0L
        val maxExpiration1 = if (result1 is ResultState.Success) result1.data else 0L

        val overallMaxExpiration = maxOf(maxExpiration1, maxExpiration2)

        val now = getInstance().timeInMillis
        if (overallMaxExpiration > now) {
            sharedPreferences.setSubscriptionExpiration(overallMaxExpiration)
            snackbarManager?.show(message = "Подписки восстановлены")
            return ResultState.Success(Unit)
        } else {
            snackbarManager?.show(message = "Действующих подписок не найдено")
            return ResultState.Success(Unit)
        }
    }

    suspend fun restoreSubscribePaySDK(snackbarManager: ISnackbarManager? = null): ResultState<Long> =
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                var expirationDate = 0L

                // Получаем покупки
                val purchases = suspendCoroutine<List<Purchase>> { continuation ->
                    payClient.getPurchaseInteractor().getPurchases()
                        .addOnSuccessListener {
                            continuation.resume(it)
                        }
                        .addOnFailureListener {
                            snackbarManager?.show(message = "ошибка $it")
                            continuation.resumeWithException(it)
                        }
                }

                Log.d("zzz", "paySDK ${purchases.size} подписок")
                // Обработка каждой покупки
                purchases.forEach { purchase ->
                    val subscriptionPurchase = purchase as SubscriptionPurchase

                    // Получаем время истечения подписки
                    val currentExpiration = subscriptionPurchase.expirationDate.time
                    if (currentExpiration > expirationDate) {
                        expirationDate = currentExpiration
                    }
                }
                ResultState.Success(expirationDate)
                // Обновляем expiration
//                if (expirationDate > now) {
//                    sharedPreferences.setSubscriptionExpiration(expirationDate)
//                    Log.d("zzz", "Подписки восстановлены paySDK")
//                    snackbarManager?.show(message = "Подписки восстановлены")
//                    ResultState.Success(Unit)
//                } else {
//                    Log.d("zzz", "Действующих подписок не найдено paySDK")
//                    snackbarManager?.show(message = "Действующих подписок не найдено")
//                    ResultState.Success(Unit)
//                }
            } catch (t: Throwable) {
                Log.d("zzz", "message = ${t.message}")
                snackbarManager?.show(message = t.message ?: "Ошибка при восстановлении подписок")
                ResultState.Error(ErrorEntity(t))
            }
        }

    suspend fun restoreSubscribeBillingClient(snackbarManager: ISnackbarManager? = null): ResultState<Long> =
        withContext(Dispatchers.IO) {
            try {
                var maxEndTime = 0L

                // Suspend ждём покупки из колбэка
                val purchases =
                    suspendCancellableCoroutine<List<PurchaseBillingClient>> { continuation ->
                        billingClient.purchases.getPurchases()
                            .addOnSuccessListener { purchasesList ->
                                continuation.resume(purchasesList)
                            }
                            .addOnFailureListener { e ->
                                snackbarManager?.show(message = "Ошибка получения покупок: $e")
                                continuation.resumeWithException(e)
                            }
                    }

                // Последовательно обрабатываем каждую покупку (collect — suspend, так что ждём)
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == PurchaseState.CONFIRMED) {
                        val resultState = ruStoreUseCase.getExpiryTimeMillis(
                            purchase.productId,
                            purchase.subscriptionToken ?: ""
                        ).first { it is ResultState.Success || it is ResultState.Error }

                        if (resultState is ResultState.Success) {
                            if (resultState.data > maxEndTime) {
                                maxEndTime = resultState.data
                                Log.d("zzz", "billing client maxEndTime $maxEndTime")
                            }
                        }
                        if (resultState is ResultState.Error) {
                            snackbarManager?.show(
                                message = "Ошибка ruStoreUseCase.getExpiryTimeMillis ${resultState.entity.message}",
                                showOnceKey = "restore_purchases_none"
                            )
                        }
                    }
                }
                ResultState.Success(maxEndTime)
            } catch (e: Exception) {
                snackbarManager?.show(
                    message = e.message ?: "Ошибка при восстановлении",
                    actionLabel = "Повторить",
                    onAction = {
                        this.cancel()
                        restorePurchases()
                    }
                )
                ResultState.Error(ErrorEntity(e))
            }
        }

    // 6. Установка expiration через получение деталей подписки
    suspend fun setSubscriptionExpirationFromServer(
        purchaseId: String,
    ): ResultState<Unit> = withContext(Dispatchers.IO) {
        try {
            suspendCoroutine<ResultState<Unit>> { continuation ->
                payClient.getPurchaseInteractor().getPurchase(PurchaseId(purchaseId))
                    .addOnSuccessListener { purchase: Purchase ->
                        when (purchase) {
//                            is SubscriptionPurchase -> {
//                                // Логика обработки результата покупки подписки
//                                val expiration = purchase.expirationDate.time
//
//                                val prev = sharedPreferences.getSubscriptionExpiration()
//                                if (expiration > prev) {
//                                    sharedPreferences.setSubscriptionExpiration(expiration)
//                                }
//                                continuation.resume(ResultState.Success(Unit))
//                            }

                            else -> {
                                // Логика обработки результата покупки c базовыми полями
                            }
                        }
                    }
                    .addOnFailureListener { throwable: Throwable ->
                        // Обработка ошибки
                        continuation.resumeWithException(throwable)
                    }
            }

        } catch (t: Throwable) {
            ResultState.Error(ErrorEntity(t))
        }
    }

}