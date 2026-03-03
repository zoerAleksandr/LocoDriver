package com.z_company.use_case

import android.util.Log
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.remote_rest.SettingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
//
class SubscriptionHelper() : KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()
    private val settingManager: SettingManager by inject()

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
            val settingState = settingManager.getUserSettingFromRemote(bearer)
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

                if (purchaseTimeEnd > Clock.System.now().toEpochMilliseconds()) {
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