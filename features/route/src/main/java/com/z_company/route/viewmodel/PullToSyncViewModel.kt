package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.sendToSentry
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.SyncManager
import com.z_company.repository.remote_rest.NetworkErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class PullToSyncUiState(
    val isRefreshing: Boolean = false,
    val message: String? = null,
)

/**
 * Единый контроллер ручной синхронизации для pull-to-refresh.
 *
 * Ручной запуск намеренно не проверяет автоматический cooldown: явный жест
 * пользователя всегда запускает полную двустороннюю синхронизацию. Общий mutex
 * в [SyncManager] не допускает параллельной записи данных.
 */
class PullToSyncViewModel : ViewModel(), KoinComponent {
    private val syncManager: SyncManager by inject()
    private val routeActionsHelper: RouteActionsHelper by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()

    private val _uiState = MutableStateFlow(PullToSyncUiState())
    val uiState = _uiState.asStateFlow()

    fun refresh(onFinished: () -> Unit = {}) {
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true, message = null) }
            var completed = false
            var failureMessage: String? = null
            try {
                if (!routeActionsHelper.hasActiveSubscription()) {
                    showMessage("Синхронизация доступна по подписке")
                    return@launch
                }

                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (token.isNullOrBlank()) {
                    showMessage("Необходимо войти в профиль")
                    return@launch
                }

                syncManager.syncBidirectional("Bearer $token").collect { state ->
                    when (state) {
                        is ResultState.Error -> {
                            failureMessage = NetworkErrorMapper.syncFailureMessage(
                                state.entity.message,
                                state.entity.throwable,
                            )
                        }
                        is ResultState.Success -> completed = state.data.routesDone
                        is ResultState.Loading -> Unit
                    }
                }

                showMessage(
                    failureMessage ?: if (completed) {
                        "Синхронизация завершена"
                    } else {
                        "Синхронизация не выполнена: сервер не завершил обработку данных"
                    }
                )
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                t.sendToSentry("PullToSyncViewModel", "refresh")
                showMessage(NetworkErrorMapper.syncFailureMessage(t.message, t))
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
                withContext(Dispatchers.Main) { onFinished() }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}
