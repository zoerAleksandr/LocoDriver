package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.partner.Partner
import com.z_company.domain.entities.route.RoutePartner
import com.z_company.domain.use_cases.PartnerUseCase
import com.z_company.domain.use_cases.RoutePartnerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Экран выбора напарников в маршрут (мультивыбор из справочника).
 * Галочка = в маршруте есть копия [RoutePartner] с этим [Partner.partnerId] в
 * [RoutePartner.sourcePartnerId]. По «Готово» — сверка: для отмеченных без копии
 * создаём копию, для снятых копий удаляем.
 */
class PartnerPickerViewModel(
    private val basicId: String,
) : ViewModel(), KoinComponent {
    private val partnerUseCase: PartnerUseCase by inject()
    private val routePartnerUseCase: RoutePartnerUseCase by inject()

    private val _partners = MutableStateFlow<List<Partner>>(emptyList())
    val partnersFlow = _partners.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    init {
        // Изначально отмечены напарники, уже добавленные в маршрут (по sourcePartnerId).
        val existing = routePartnerUseCase.getPartnerListByBasicId(basicId)
        _selectedIds.value = existing.mapNotNull { it.sourcePartnerId }.toSet()

        partnerUseCase.getAllFlow()
            .onEach { _partners.value = it }
            .launchIn(viewModelScope)
    }

    fun toggle(partnerId: String) {
        _selectedIds.update { set ->
            if (set.contains(partnerId)) set - partnerId else set + partnerId
        }
    }

    /** Удаление записи справочника (свайп в списке). */
    fun deletePartner(partner: Partner) {
        _selectedIds.update { it - partner.partnerId }
        viewModelScope.launch { partnerUseCase.delete(partner.partnerId).collect {} }
    }

    /** Применяет выбор к маршруту и вызывает [onComplete] по завершении. */
    fun confirm(onComplete: () -> Unit) {
        viewModelScope.launch {
            val selected = _selectedIds.value
            val existing = routePartnerUseCase.getPartnerListByBasicId(basicId)
            val existingBySource = existing.mapNotNull { rp ->
                rp.sourcePartnerId?.let { it to rp }
            }.toMap()

            // Удаляем снятые.
            for (rp in existing) {
                val src = rp.sourcePartnerId
                if (src != null && src !in selected) {
                    routePartnerUseCase.removePartner(rp).collect {}
                }
            }
            // Добавляем отмеченные, которых ещё нет в маршруте.
            for (partnerId in selected) {
                if (!existingBySource.containsKey(partnerId)) {
                    val partner = _partners.value.find { it.partnerId == partnerId } ?: continue
                    routePartnerUseCase.savePartner(
                        RoutePartner(
                            basicId = basicId,
                            sourcePartnerId = partner.partnerId,
                            fullName = partner.fullName,
                            tabNumber = partner.tabNumber,
                            notes = partner.notes,
                        )
                    ).collect {}
                }
            }
            onComplete()
        }
    }
}
