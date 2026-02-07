package com.z_company.use_case

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureDataStore
import com.z_company.repository.remote_rest.SettingManager
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
    private val settingsUseCase: SettingsUseCase by inject()

    suspend fun restorePurchases(
        snackbarManager: ISnackbarManager? = null,
        token: String?
    ): ResultState<Unit> {
        return try {
            if (token == null) {
                snackbarManager?.show(message = "Неавторизованный пользователь")
                return ResultState.Error(ErrorEntity(message = "Неавторизованный пользователь"))
            }
            snackbarManager?.show("Начинаем поиск...")
            val bearer = "Bearer $token"
            val settingState = SettingManager.getUserSettingFromRemote(bearer)
                .first { it !is ResultState.Loading }

            if (settingState is ResultState.Error) {
                snackbarManager?.show("Ошибка связи с сервером. Напишите в поддержку.")
                return ResultState.Error(settingState.entity)
            }

            if (settingState is ResultState.Success) {
                val remoteSetting = settingState.data
                val purchaseTimeEnd = remoteSetting.subscriptionPeriod
                val setting =
                    settingsUseCase.getUserSettingFlow().first { it != ResultState.Loading() }
                val dateAndTimeConverter = DateAndTimeConverter(setting)
                val time = dateAndTimeConverter.getDateAndTime(purchaseTimeEnd)

                if (purchaseTimeEnd > getInstance().timeInMillis) {
                    val updateResult = settingsUseCase.updateSubscriptionPeriod(purchaseTimeEnd)
                        .first { it !is ResultState.Loading }
                    if (updateResult is ResultState.Success) {
                        snackbarManager?.show("Данные об оплате обновлены. До $time")
                    }
                } else if (purchaseTimeEnd == 0L) {
                    snackbarManager?.show("Не найдено информации об оплате")
                } else {
                    snackbarManager?.show("Срок оплаты истек $time")
                }
                return ResultState.Success(Unit)
            } else {
                return ResultState.Error(ErrorEntity(message = "Неизвестное состояние загрузки настроек"))
            }
        } catch (e: Exception) {
            Log.e("RestorePurchases", "Exception occurred: ${e.message}", e)
            ResultState.Error(entity = ErrorEntity(e))
        }
    }
}