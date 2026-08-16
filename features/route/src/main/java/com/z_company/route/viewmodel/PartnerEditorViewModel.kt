package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.partner.Partner
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.PartnerUseCase
import com.z_company.domain.util.generateId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class PartnerEditorState(
    val partnerId: String? = null,
    val fullName: String = "",
    val tabNumber: String = "",
    val notes: String = "",
    val deleted: Boolean = false,
)

/**
 * Редактор записи справочника напарников. По образцу [StationNormEditorViewModel]:
 * id генерируется один раз (защита от дублей при автосохранении), autosave silent.
 */
class PartnerEditorViewModel(
    private val partnerId: String?,
) : ViewModel(), KoinComponent {
    private val useCase: PartnerUseCase by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()

    private val persistentId: String = partnerId ?: generateId()

    private val _state = MutableStateFlow(PartnerEditorState(partnerId = partnerId))
    val state = _state.asStateFlow()

    init {
        if (partnerId != null) {
            viewModelScope.launch {
                useCase.getById(partnerId)?.let { p ->
                    _state.update {
                        it.copy(
                            partnerId = p.partnerId,
                            fullName = p.fullName,
                            tabNumber = p.tabNumber.orEmpty(),
                            notes = p.notes.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    fun setFullName(value: String) = _state.update { it.copy(fullName = value) }
    fun setTabNumber(value: String) = _state.update { it.copy(tabNumber = value) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value) }

    /** Autosave — silent. Не создаёт запись с пустым ФИО. */
    fun save() {
        val s = _state.value
        if (s.fullName.isBlank()) return
        sharedPrefs.setSettingsSyncPending(true)
        viewModelScope.launch {
            useCase.upsert(
                Partner(
                    partnerId = persistentId,
                    fullName = s.fullName.trim(),
                    tabNumber = s.tabNumber.trim().ifBlank { null },
                    notes = s.notes.trim().ifBlank { null },
                )
            ).first()
        }
    }

    fun delete() {
        sharedPrefs.setSettingsSyncPending(true)
        viewModelScope.launch {
            useCase.delete(persistentId).collect {}
            _state.update { it.copy(deleted = true) }
        }
    }
}
