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
import java.util.Calendar
import java.util.Calendar.getInstance
//
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

            // Изменено: Добавил проверку на Success с else, хотя ResultState sealed, но для безопасности
            // Для чего: Чтобы функция всегда возвращала значение, даже если state не Error и не Success (хотя это маловероятно)
            if (settingState is ResultState.Success) {
                val remoteSetting = settingState.data
                val purchaseTimeEnd = remoteSetting.subscriptionPeriod
                val setting = settingsUseCase.getUserSettingFlow().first()
                val dateAndTimeConverter =
                    DateAndTimeConverter(setting) // Предполагаю, что это DateAndTimeConverter, исправьте если опечатка
                val time = dateAndTimeConverter.getDateAndTime(purchaseTimeEnd)

                // Изменено: Исправил getInstance() на Calendar.getInstance()
                // Для чего: Чтобы правильно получить текущее время в миллисекундах
                if (purchaseTimeEnd > getInstance().timeInMillis) {
                    // Изменено: Изменил .collect на .first(), предполагая, что flow эмитирует один ResultState
                    // Для чего: Чтобы дождаться результата обновления и вернуть его как результат функции, а не всегда Success
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
                // Добавлено: Обработка случая, если state не Success и не Error (маловероятно, но для полноты)
                return ResultState.Error(ErrorEntity(message = "Неизвестное состояние загрузки настроек"))
            }
        } catch (e: Exception) {
            Log.e("RestorePurchases", "Exception occurred: ${e.message}", e)
            ResultState.Error(entity = ErrorEntity(e))
        }
    }
}