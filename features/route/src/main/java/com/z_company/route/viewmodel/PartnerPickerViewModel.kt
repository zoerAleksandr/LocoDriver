package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.partner.Partner
import com.z_company.domain.entities.route.RoutePartner
import com.z_company.domain.repositories.SharedPreferencesRepositories
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

/** Запрос подтверждения замены постоянного напарника [current] на [candidate]. */
data class PermanentReplaceRequest(
    val current: Partner,
    val candidate: Partner,
)

/**
 * Экран выбора напарников в маршрут (мультивыбор из справочника).
 * Галочка = в маршруте есть копия [RoutePartner] с этим [Partner.partnerId] в
 * [RoutePartner.sourcePartnerId]. По «Готово» — сверка: для отмеченных без копии
 * создаём копию, для снятых копий удаляем.
 *
 * Отмеченного напарника можно сделать «постоянным» (сразу пишется в справочник,
 * не зависит от «Готово»); если постоянный уже есть — сначала подтверждение замены.
 */
class PartnerPickerViewModel(
    private val basicId: String,
) : ViewModel(), KoinComponent {
    private val partnerUseCase: PartnerUseCase by inject()
    private val routePartnerUseCase: RoutePartnerUseCase by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()

    private val _partners = MutableStateFlow<List<Partner>>(emptyList())
    val partnersFlow = _partners.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    /** Последний отмеченный пользователем напарник — кандидат в «постоянные». */
    private val _lastSelectedId = MutableStateFlow<String?>(null)
    val lastSelectedId = _lastSelectedId.asStateFlow()

    private val _replaceRequest = MutableStateFlow<PermanentReplaceRequest?>(null)
    val replaceRequest = _replaceRequest.asStateFlow()

    init {
        // Изначально отмечены напарники, уже добавленные в маршрут (по sourcePartnerId).
        val existing = routePartnerUseCase.getPartnerListByBasicId(basicId)
        _selectedIds.value = existing.mapNotNull { it.sourcePartnerId }.toSet()

        partnerUseCase.getAllFlow()
            .onEach { _partners.value = it }
            .launchIn(viewModelScope)
    }

    fun toggle(partnerId: String) {
        val wasSelected = partnerId in _selectedIds.value
        _selectedIds.update { set ->
            if (wasSelected) set - partnerId else set + partnerId
        }
        if (wasSelected) {
            if (_lastSelectedId.value == partnerId) _lastSelectedId.value = null
        } else {
            _lastSelectedId.value = partnerId
        }
    }

    /** Кнопка «Сделать постоянным напарником» (с подтверждением замены прежнего). */
    fun makePermanent(partner: Partner) {
        if (partner.isPermanent) return
        val current = _partners.value.firstOrNull { it.isPermanent && it.partnerId != partner.partnerId }
        if (current != null) {
            _replaceRequest.value = PermanentReplaceRequest(current = current, candidate = partner)
        } else {
            setPermanent(partner.partnerId)
        }
    }

    fun confirmReplacePermanent() {
        val request = _replaceRequest.value ?: return
        _replaceRequest.value = null
        setPermanent(request.candidate.partnerId)
    }

    fun dismissReplacePermanent() {
        _replaceRequest.value = null
    }

    private fun setPermanent(partnerId: String?) {
        sharedPrefs.setSettingsSyncPending(true)
        viewModelScope.launch { partnerUseCase.setPermanent(partnerId).collect {} }
    }

    /** Удаление записи справочника (свайп в списке). */
    fun deletePartner(partner: Partner) {
        _selectedIds.update { it - partner.partnerId }
        sharedPrefs.setSettingsSyncPending(true)
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
