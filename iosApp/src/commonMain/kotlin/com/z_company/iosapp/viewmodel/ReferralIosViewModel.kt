package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import com.z_company.iosapp.session.AuthSessionHolder
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.response.ApplyReferralCodeRequest
import com.z_company.repository.remote_rest.response.ReferralStatusResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Состояние экрана «Пригласить друга» (SCREEN_SPECS §18.R) для Swift.
 *
 * @param status данные `GET /v1/referrals/me`; null — ещё не загружены.
 * @param loading идёт запрос (с спиннером только когда данных ещё нет).
 * @param error текст ошибки «Не удалось загрузить код»; при повторной ошибке
 *   старые [status] остаются на экране (§18.R, п. 3).
 */
data class ReferralIosState(
    val status: ReferralStatusResponse? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * iOS-аналог Android `ReferralViewModel` (§18.R). Запрос выполняется при каждом
 * открытии экрана — экран зовёт [refresh] из onAppear.
 */
class ReferralIosViewModel(
    private val remoteRestApi: RemoteRestApi,
    private val authSession: AuthSessionHolder,
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(ReferralIosState())
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    init {
        refresh()
    }

    /** Перезапрос статуса (каждое открытие экрана, «Повторить» при ошибке). */
    fun refresh() {
        scope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val bearer = authSession.currentBearer()
                if (bearer == null) {
                    _state.value = _state.value.copy(loading = false, error = ERROR_LOAD)
                    return@launch
                }
                val status = remoteRestApi.getReferralStatus(bearer)
                _state.value = ReferralIosState(status = status, loading = false, error = null)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false, error = ERROR_LOAD)
            }
        }
    }

    /**
     * Применение чужого кода (§19.R, экран «Покупки») — на экране рефералки
     * статус применённого кода не показывается.
     */
    fun applyCode(code: String) {
        scope.launch {
            try {
                val bearer = authSession.currentBearer() ?: return@launch
                remoteRestApi.applyReferralCode(bearer, ApplyReferralCodeRequest(code.trim()))
                refresh()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                _messages.tryEmit(ERROR_APPLY)
            }
        }
    }

    fun watchState(callback: (ReferralIosState) -> Unit) {
        scope.launch { _state.collect { callback(it) } }
    }

    fun watchMessages(callback: (String) -> Unit) {
        scope.launch { _messages.collect { callback(it) } }
    }

    fun dispose() {
        scope.cancel()
    }

    private companion object {
        const val ERROR_LOAD = "Не удалось загрузить код"
        const val ERROR_APPLY = "Не удалось применить код. Проверьте его."
    }
}
