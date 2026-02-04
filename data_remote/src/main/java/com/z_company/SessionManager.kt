package com.z_company

import android.util.Log
import com.parse.ParseUser
import com.z_company.use_case.RemoteRouteUseCase
import com.z_company.work_manager.UserFieldName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class SessionManager @Inject constructor(
    private val remoteRouteUseCase: RemoteRouteUseCase
) {
    private val _isLoggedIn = MutableStateFlow(ParseUser.getCurrentUser() != null)
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var syncJob: Job? = null

    suspend fun updateLoggedIn() {
//        withContext(Dispatchers.IO) {
//            val currentUser = ParseUser.getCurrentUser()
//            val loggedIn = currentUser != null
//            val verified =
//                loggedIn && currentUser?.getBoolean(UserFieldName.EMAIL_VERIFIED_FIELD_NAME_REMOTE) == true
//
//            // ← МГНОВЕННО обновляем состояние логина (это главное!)
//            _isLoggedIn.value = loggedIn
//
//            // Отменяем предыдущую синхронизацию
//            syncJob?.cancel()
//            syncJob = null
//
//            // Запускаем/отменяем синхронизацию в фоне
//            if (verified) {
//                syncJob = scope.launch {
//                    try {
//                        remoteRouteUseCase.syncBasicDataPeriodic().collect {
//                            // если Flow эмитит что-то полезное — можно обработать
//                        }
//                    } catch (e: Exception) {
//                        if (e !is CancellationException) {
//                            // опционально лог ошибки синхронизации
//                            Log.e("SessionManager", "Sync error", e)
//                        }
//                    }
//                }
//            } else {
//                // Если пользователь залогинен, но email не подтверждён — всё равно отменяем синхронизацию
////            remoteRouteUseCase.cancelingSync() // если это suspend — scope.launch { it() }
//                scope.launch { remoteRouteUseCase.cancelingSync() }
//            }
//        }
    }

    fun clear() {
        syncJob?.cancel()
        scope.cancel()
    }
}