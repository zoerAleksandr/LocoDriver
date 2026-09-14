package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.response.ReferralStatusResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReferralViewModel : ViewModel(), KoinComponent {
    private val api: RemoteRestApi by inject()
    private val tokens: SecureTokenStorage by inject()
    private val _status = MutableStateFlow<ReferralStatusResponse?>(null)
    val status = _status.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val token = tokens.getAuthBearerTokenFlow().first()
                _status.value = api.getReferralStatus("Bearer $token")
                _error.value = null
            } catch (_: Exception) {
                _error.value = "Не удалось загрузить реферальную программу"
            } finally {
                _loading.value = false
            }
        }
    }
}
