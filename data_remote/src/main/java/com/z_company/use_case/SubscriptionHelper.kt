package com.z_company.use_case

import android.util.Log
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.domain.repositories.SharedPreferencesRepositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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
import ru.rustore.sdk.pay.model.PurchaseStatus
import ru.rustore.sdk.pay.model.RuStorePaymentException.ProductPurchaseCancelled
import ru.rustore.sdk.pay.model.RuStorePaymentException.ProductPurchaseException
import ru.rustore.sdk.pay.model.SubscriptionPurchase
import ru.rustore.sdk.pay.model.SubscriptionPurchaseStatus
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.getValue

class SubscriptionHelper() : KoinComponent {
    private val payClient: RuStorePayClient by inject()
    private val sharedPreferences: SharedPreferencesRepositories by inject()

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
    suspend fun restorePurchasesSuspend(snackbarManager: ISnackbarManager? = null): ResultState<Unit> =
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
                            snackbarManager?.show(message = "message ${it.message}")
                            continuation.resumeWithException(it)
                        }
                }

                snackbarManager?.show(message = "${purchases.size} подписок")
                Log.d("zzz", "${purchases.size} подписок")
                // Обработка каждой покупки
                purchases.forEach { purchase ->
                    snackbarManager?.show(message = "purchase.javaClass = ${purchase.javaClass}")
                    val subscriptionPurchase = purchase as SubscriptionPurchase

                    // Получаем время истечения подписки
                    val currentExpiration = subscriptionPurchase.expirationDate.time
                    snackbarManager?.show(message = "status = ${purchase.status}")
                    snackbarManager?.show(message = "purchaseTime = ${subscriptionPurchase.purchaseTime?.time}")
                    snackbarManager?.show(message = "expirationDate = $currentExpiration")
                    Log.d("zzz", "purchaseTime = ${subscriptionPurchase.purchaseTime?.time}")
                    Log.d("zzz", "expirationDate = $currentExpiration")
                    if (currentExpiration > expirationDate) {
                        expirationDate = currentExpiration
                    }
                }

                // Обновляем expiration
                if (expirationDate > now) {
                    sharedPreferences.setSubscriptionExpiration(expirationDate)
                    snackbarManager?.show(message = "Подписки восстановлены")
                    ResultState.Success(Unit)
                } else {
                    snackbarManager?.show(message = "Действующих подписок не найдено")
                    ResultState.Success(Unit)
                }
            } catch (t: Throwable) {
                snackbarManager?.show(message = t.message ?: "Ошибка при восстановлении подписок")
                ResultState.Error(ErrorEntity(t))
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
                            is SubscriptionPurchase -> {
                                // Логика обработки результата покупки подписки
                                val expiration = purchase.expirationDate.time

                                val prev = sharedPreferences.getSubscriptionExpiration()
                                if (expiration > prev) {
                                    sharedPreferences.setSubscriptionExpiration(expiration)
                                }
                                continuation.resume(ResultState.Success(Unit))
                            }

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