package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.partner.Partner
import com.z_company.domain.use_cases.PartnerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Список справочника напарников (Настройки → Справочники → Напарники).
 * По образцу [StationNormListViewModel].
 */
class PartnerListViewModel : ViewModel(), KoinComponent {
    private val useCase: PartnerUseCase by inject()

    private val _partners = MutableStateFlow<List<Partner>>(emptyList())
    val partnersFlow = _partners.asStateFlow()

    init {
        useCase.getAllFlow()
            .onEach { _partners.value = it }
            .launchIn(viewModelScope)
    }

    fun delete(partner: Partner) {
        viewModelScope.launch {
            useCase.delete(partner.partnerId).collect {}
        }
    }
}
